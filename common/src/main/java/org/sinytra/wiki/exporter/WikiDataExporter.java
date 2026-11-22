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
import java.util.concurrent.CompletableFuture;

public class WikiDataExporter {
    private static final String EXPORTER_ENABLED_PROPERTY = "wiki_exporter.enabled";
    private static final String EXPORTER_CONFIG_PROPERTY = "wiki_exporter.config.path";
    private static final String EXPORTER_OUTPUT_PROPERTY = "wiki_exporter.output.path";
    private static final String BASE_DIR = "wiki_exporter";
    private static final String DEFAULT_OUTPUT = BASE_DIR + "/output";
    private static final String DEFAULT_PATH = BASE_DIR + "/config.json";
    private static final Map<String, ModuleInstance> MODULES = new HashMap<>();
    private static final Map<String, CompletableFuture<Void>> FUTURES = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static MinecraftServer server;
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
        Services.MODULE_FACTORIES.stream()
            .filter(m -> candidateModules.contains(m.name()) && m.isEnabled(isClient))
            .forEach(factory -> enabledFactories.put(factory.name(), factory));
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

        enabledFactories.forEach((name, factory) -> {
            Object moduleConfig = config.getForModule(name);
            Set<String> moduleNamespaces = moduleConfig instanceof NamespacedModuleConfig nc
                ? Objects.requireNonNullElse(nc.namespaces(), globalNamespaces) : globalNamespaces;
            ExportContext context = new ExportContext(moduleNamespaces);

            ModuleInstance instance = new ModuleInstance(factory, ((ExporterModuleFactory) factory).create(context, moduleConfig));
            MODULES.put(factory.name(), instance);
        });

        LOGGER.info("Found modules: {}", MODULES.keySet());

        MODULES.forEach((name, module) -> FUTURES.put(
            name,
            new CompletableFuture<>()
        ));

        CompletableFuture.allOf(FUTURES.values().toArray(new CompletableFuture[0]))
            .thenRunAsync(() -> exit(isClient));

        if (FUTURES.isEmpty()) {
            exit(isClient);
        }
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

    private static void exit(boolean isClient) {
        LOGGER.info("Finished running all modules, shutting down");

        if (isClient) {
            WikiDataExporterClient.finishRunning();
        } else if (server != null) {
            server.halt(false);
        } else {
            System.exit(0);
        }
    }

    public static void runModule(String name) {
        Optional.ofNullable(MODULES.get(name))
            .ifPresent(module -> {
                CompletableFuture<Void> future = FUTURES.get(name);

                try {
                    Path moduleOutputPath = Optional.ofNullable(System.getProperty("wiki_exporter.module." + name + ".output.path"))
                        .map(Path::of)
                        .orElseGet(() -> outputPath.resolve(name));
                    Files.createDirectories(moduleOutputPath);

                    module.instance().run(moduleOutputPath);

                    LOGGER.info("Finished running module {}", module.factory().name());
                    if (future != null) {
                        future.complete(null);
                    }
                } catch (Exception e) {
                    LOGGER.info("Error running module {}", module.factory().name(), e);
                    if (future != null) {
                        future.completeExceptionally(e);
                    }
                }
            });
    }

    public static void setServer(MinecraftServer server) {
        WikiDataExporter.server = server;
    }

    record ModuleInstance(ExporterModuleFactory<?> factory, ExporterModule instance) {
    }
}
