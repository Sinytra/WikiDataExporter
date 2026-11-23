package org.sinytra.wiki.exporter.mixin;

import net.minecraft.client.Minecraft;
import org.sinytra.wiki.exporter.WikiDataExporter;
import org.sinytra.wiki.exporter.WikiDataExporterClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "onGameLoadFinished", at = @At("TAIL"))
    private void onGameLoadFinished(CallbackInfo ci) {
        if (WikiDataExporter.isEnabled()) {
            WikiDataExporterClient.start((Minecraft) (Object) this);
        }
    }
}
