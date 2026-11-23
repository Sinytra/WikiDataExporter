package org.sinytra.wiki.exporter;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import org.sinytra.wiki.exporter.platform.Services;
import org.slf4j.Logger;

public class WikiDataExporterClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean started;

    public static boolean runInOverlay(Minecraft minecraft, ReloadInstance reload) {
        if (!WikiDataExporter.isEnabled()) {
            return false;
        }

        if (!started) {
            if (!reload.isDone() || reload.done().isCompletedExceptionally()) {
                return false;
            }

            started = true;
            start(minecraft);
        }

        return true;
    }

    private static void start(Minecraft minecraft) {
        Thread thread = new Thread(() -> run(minecraft), "Wiki Exporter");
        thread.setDaemon(true);
        thread.start();
    }

    private static void run(Minecraft minecraft) {
        try {
            loadServerData(minecraft);

            WikiDataExporter.runModules();
        } catch (Exception e) {
            LOGGER.error("Error running wiki exporter", e);
        }

        LOGGER.info("Shutting down");
        minecraft.execute(() -> {
            minecraft.options.startedCleanly = true;
            minecraft.options.save();
            minecraft.stop();
        });
    }

    private static void loadServerData(Minecraft minecraft) {
        LOGGER.info("Loading server data");

        WorldLoader.InitConfig config = new WorldLoader.InitConfig(
            Services.PLATFORM.createServerPackConfig(),
            Commands.CommandSelection.INTEGRATED,
            LevelBasedPermissionSet.GAMEMASTER
        );
        WorldLoader.load(
            config,
            context -> new WorldLoader.DataLoadOutput<>(Unit.INSTANCE, context.datapackDimensions()),
            (resources, managers, registries, cookie) -> {
                resources.close();
                return managers;
            },
            Util.backgroundExecutor(),
            minecraft
        ).join();
    }
}
