package org.sinytra.wiki.exporter;

import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Unit;
import org.sinytra.wiki.exporter.platform.Services;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class WikiDataExporterClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int GAMEMASTER_PERMISSION_LEVEL = 2;

    public static ReloadInstance wrapInitialReload(Minecraft minecraft, ReloadInstance reload) {
        if (!WikiDataExporter.isEnabled()) {
            return reload;
        }
        return new ExportReloadInstance(reload, () -> start(minecraft));
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
        minecraft.execute(minecraft::stop);
    }

    private static void loadServerData(Minecraft minecraft) {
        LOGGER.info("Loading server data");

        WorldLoader.InitConfig config = new WorldLoader.InitConfig(
            Services.PLATFORM.createServerPackConfig(),
            Commands.CommandSelection.INTEGRATED,
            GAMEMASTER_PERMISSION_LEVEL
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

    private static final class ExportReloadInstance implements ReloadInstance {
        private final ReloadInstance delegate;
        private final CompletableFuture<Void> done = new CompletableFuture<>();

        private ExportReloadInstance(ReloadInstance delegate, Runnable onReloaded) {
            this.delegate = delegate;
            delegate.done().whenComplete((result, error) -> {
                if (error != null) {
                    this.done.completeExceptionally(error);
                } else {
                    onReloaded.run();
                }
            });
        }

        @Override
        public CompletableFuture<?> done() {
            return this.done;
        }

        @Override
        public float getActualProgress() {
            return this.delegate.getActualProgress();
        }
    }
}
