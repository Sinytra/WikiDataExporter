package org.sinytra.wiki.exporter.lang;

import org.sinytra.wiki.exporter.NamespacedModuleConfig;

import java.util.Set;

public record WikiLangModuleConfig(
    Set<String> namespaces
) implements NamespacedModuleConfig {}
