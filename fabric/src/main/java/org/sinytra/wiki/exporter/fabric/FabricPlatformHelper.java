package org.sinytra.wiki.exporter.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.impl.resource.pack.ModPackResourcesUtil;
import net.fabricmc.fabric.impl.resource.pack.ModResourcePackCreator;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.sinytra.wiki.exporter.platform.services.IPlatformHelper;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class FabricPlatformHelper implements IPlatformHelper {
    private static final String BUILTIN_MOD_TYPE = "builtin";

    @Override
    public Set<String> getInDevModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
            .filter(m -> !BUILTIN_MOD_TYPE.equals(m.getMetadata().getType()))
            .filter(m -> m.getOrigin().getKind() == ModOrigin.Kind.PATH && IPlatformHelper.isLoadedFromDir(m.getOrigin().getPaths()))
            .map(m -> m.getMetadata().getId())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Path getGameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public WorldLoader.PackConfig createServerPackConfig() {
        PackRepository repository = new PackRepository(
            new ServerPacksSource(new DirectoryValidator(path -> true)),
            new ModResourcePackCreator(PackType.SERVER_DATA)
        );
        return new WorldLoader.PackConfig(repository, ModPackResourcesUtil.createDefaultDataConfiguration(), false, true);
    }
}
