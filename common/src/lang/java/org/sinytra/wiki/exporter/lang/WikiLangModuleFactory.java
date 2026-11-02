package org.sinytra.wiki.exporter.lang;

import org.jetbrains.annotations.Nullable;
import org.sinytra.wiki.exporter.ExportContext;
import org.sinytra.wiki.exporter.platform.services.ExporterModule;
import org.sinytra.wiki.exporter.platform.services.ExporterModuleFactory;

public class WikiLangModuleFactory implements ExporterModuleFactory<WikiLangModuleConfig> {
    public static final String NAME = "lang";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isEnabled(boolean client) {
        return client;
    }

    @Nullable
    @Override
    public Class<WikiLangModuleConfig> getConfigClass() {
        return WikiLangModuleConfig.class;
    }

    @Override
    public boolean requiresConfig() {
        return false;
    }

    @Override
    public ExporterModule create(ExportContext context, WikiLangModuleConfig config) {
        return new WikiExporterLang(context.namespaces());
    }
}
