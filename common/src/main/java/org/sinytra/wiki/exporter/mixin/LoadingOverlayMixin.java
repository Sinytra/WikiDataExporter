package org.sinytra.wiki.exporter.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.sinytra.wiki.exporter.WikiDataExporterClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    @Final
    private ReloadInstance reload;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void holdForExport(CallbackInfo ci) {
        if (WikiDataExporterClient.runInOverlay(this.minecraft, this.reload)) {
            ci.cancel();
        }
    }
}
