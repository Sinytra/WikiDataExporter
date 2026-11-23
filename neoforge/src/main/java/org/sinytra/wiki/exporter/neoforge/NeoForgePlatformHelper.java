package org.sinytra.wiki.exporter.neoforge;

import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.WorldDataConfiguration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import org.sinytra.wiki.exporter.platform.services.IPlatformHelper;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public Set<String> getInDevModIds() {
        return ModList.get().getMods().stream()
            .filter(m -> IPlatformHelper.isLoadedFromDir(List.of(m.getOwningFile().getFile().getFilePath())))
            .map(IModInfo::getModId)
            .collect(Collectors.toSet());
    }

    @Override
    public Path getGameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public WorldLoader.PackConfig createServerPackConfig() {
        return new WorldLoader.PackConfig(ServerPacksSource.createVanillaTrustedRepository(), WorldDataConfiguration.DEFAULT, false, true);
    }
}