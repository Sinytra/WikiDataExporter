package org.sinytra.wiki.exporter.lang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.sinytra.wiki.exporter.Constants;
import org.sinytra.wiki.exporter.platform.services.ExporterModule;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class WikiExporterLang implements ExporterModule {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private final Set<String> namespaces;

    public WikiExporterLang(Set<String> namespaces) {
        this.namespaces = namespaces;
    }

    @Override
    public void run(Path output) throws Exception {
        if (this.namespaces.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resources = minecraft.getResourceManager();
        Set<String> languages = minecraft.getLanguageManager().getLanguages().keySet();
        Set<String> availableNamespaces = resources.getNamespaces();

        Constants.LOG.info("Exporting {} languages for namespaces {}", languages.size(), this.namespaces);

        for (String namespace : this.namespaces) {
            if (!availableNamespaces.contains(namespace)) {
                Constants.LOG.warn("Namespace {} has no loaded resources, skipping language export", namespace);
                continue;
            }

            int exported = 0;
            for (String languageCode : languages) {
                Map<String, String> translations = loadTranslations(resources, namespace, languageCode);
                if (translations.isEmpty()) {
                    continue;
                }

                Path outputPath = output.resolve(namespace).resolve("lang").resolve(languageCode + ".json");
                Files.createDirectories(outputPath.getParent());
                Files.writeString(outputPath, GSON.toJson(translations), StandardCharsets.UTF_8);
                exported++;
            }

            Constants.LOG.info("Exported {} languages for namespace {}", exported, namespace);
        }
    }

    private static Map<String, String> loadTranslations(ResourceManager resourceManager, String namespace, String languageCode) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, "lang/%s.json".formatted(languageCode));
        List<Resource> resourceStack = resourceManager.getResourceStack(location);

        Map<String, String> translations = new TreeMap<>();
        for (Resource resource : resourceStack) {
            try (InputStream inputStream = resource.open()) {
                Language.loadFromJson(inputStream, translations::put);
            } catch (Exception e) {
                Constants.LOG.warn("Failed to load translations for {} from pack {}", location, resource.sourcePackId(), e);
            }
        }

        return translations;
    }
}
