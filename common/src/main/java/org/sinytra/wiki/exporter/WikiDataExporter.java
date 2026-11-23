package org.sinytra.wiki.exporter;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.sinytra.wiki.exporter.platform.Services;
import org.sinytra.wiki.exporter.platform.services.ExporterModule;
import org.sinytra.wiki.exporter.platform.services.ExporterModuleFactory;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WikiDataExporter {
    private static final String EXPORTER_ENABLED_PROPERTY = "wiki_exporter.enabled";
    private static final String EXPORTER_CONFIG_PROPERTY = "wiki_exporter.config.path";
    private static final String EXPORTER_OUTPUT_PROPERTY = "wiki_exporter.output.path";
    private static final String BASE_DIR = "wiki_exporter";
    private static final String DEFAULT_OUTPUT = BASE_DIR + "/output";
    private static final String DEFAULT_PATH = BASE_DIR + "/config.json";
    private static final List<ModuleInstance> MODULES = new ArrayList<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled;
    private static Path outputPath;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void initialize() {
        if (!Boolean.getBoolean(EXPORTER_ENABLED_PROPERTY)) {
            return;
        }

        LOGGER.info("Initializing Wiki Exporter");
        boolean isClient = Services.PLATFORM.isClient();
        Path configPath = Optional.ofNullable(System.getProperty(EXPORTER_CONFIG_PROPERTY))
            .map(Path::of)
            .orElseGet(() -> Services.PLATFORM.getGameDirectory().resolve(DEFAULT_PATH));

        ExporterConfig.MainConfig mainConfig = Files.exists(configPath) ? ExporterConfig.preloadConfig(configPath) : createDefaultConfig(configPath);
        Set<String> candidateModules = mainConfig.enabled();
        LOGGER.info("Candidate modules: {}", candidateModules);

        Map<String, ExporterModuleFactory<?>> enabledFactories = new HashMap<>();
        List<ExporterModuleFactory<?>> sortedFactories = new ArrayList<>();
        Services.MODULE_FACTORIES.stream()
            .filter(m -> candidateModules.contains(m.name()) && m.isEnabled(isClient))
            .forEach(factory -> {
                enabledFactories.put(factory.name(), factory);
                sortedFactories.add(factory);
            });
        ExporterConfig.LoadedConfig config = ExporterConfig.load(mainConfig, enabledFactories);

        outputPath = Optional.ofNullable(mainConfig.outputPath())
            .or(() -> Optional.ofNullable(System.getProperty(EXPORTER_OUTPUT_PROPERTY)))
            .map(Path::of)
            .orElseGet(() -> Services.PLATFORM.getGameDirectory().resolve(DEFAULT_OUTPUT));
        try {
            Files.createDirectories(outputPath);
        } catch (Exception e) {
            LOGGER.error("Error preparing output directory", e);
        }

        Set<String> globalNamespaces = config.global().namespaces();

        sortedFactories.forEach(factory -> {
            Object moduleConfig = config.getForModule(factory.name());
            Set<String> moduleNamespaces = moduleConfig instanceof NamespacedModuleConfig nc
                ? Objects.requireNonNullElse(nc.namespaces(), globalNamespaces) : globalNamespaces;
            ExportContext context = new ExportContext(moduleNamespaces);

            MODULES.add(new ModuleInstance(factory, ((ExporterModuleFactory) factory).create(context, moduleConfig)));
        });

        LOGGER.info("Found modules: {}", MODULES.stream().map(m -> m.factory().name()).toList());
        enabled = true;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ExporterConfig.MainConfig createDefaultConfig(Path configPath) {
        LOGGER.info("No exporter config found at {}, using default configuration", configPath);

        Set<String> namespaces = new HashSet<>(Services.PLATFORM.getInDevModIds());
        namespaces.remove(Constants.MOD_ID);
        if (namespaces.isEmpty()) {
            LOGGER.warn("Could not detect any mods in development, nothing will be exported");
        } else {
            LOGGER.info("Detected mods in development: {}", namespaces);
        }

        return ExporterConfig.createDefault((List) Services.MODULE_FACTORIES, namespaces);
    }

    public static void runModules() {
        for (ModuleInstance module : MODULES) {
            runModule(module);
        }
        LOGGER.info("Finished running all modules");
    }

    private static void runModule(ModuleInstance module) {
        String name = module.factory().name();
        try {
            Path moduleOutputPath = Optional.ofNullable(System.getProperty("wiki_exporter.module." + name + ".output.path"))
                .map(Path::of)
                .orElseGet(() -> outputPath.resolve(name));
            Files.createDirectories(moduleOutputPath);

            LOGGER.info("Running module {}", name);
            module.instance().run(moduleOutputPath);

            LOGGER.info("Finished running module {}", name);
        } catch (Exception e) {
            LOGGER.error("Error running module {}", name, e);
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        if (!enabled || Services.PLATFORM.isClient()) {
            return;
        }

        runModules();
        LOGGER.info("Shutting down");
        server.halt(false);
    }

    private record ModuleInstance(ExporterModuleFactory<?> factory, ExporterModule instance) {
    }
}
