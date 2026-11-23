package org.sinytra.wiki.exporter.neoforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.sinytra.wiki.exporter.Constants;
import org.sinytra.wiki.exporter.WikiDataExporter;

@Mod(Constants.MOD_ID)
public class WikiExporterMod {

    public WikiExporterMod() {
        WikiDataExporter.initialize();

        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onServerStarted(ServerStartedEvent event) {
        WikiDataExporter.onServerStarted(event.getServer());
    }
}
