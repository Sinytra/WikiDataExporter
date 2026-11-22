package org.sinytra.wiki.exporter.platform.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public interface IPlatformHelper {
    Path getGameDirectory();

    boolean isClient();

    Set<String> getInDevModIds();

    static boolean isLoadedFromDir(Collection<Path> paths) {
        return !paths.isEmpty() && paths.stream().allMatch(Files::isDirectory);
    }
}