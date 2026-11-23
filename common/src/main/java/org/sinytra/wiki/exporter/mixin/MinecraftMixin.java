package org.sinytra.wiki.exporter.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.sinytra.wiki.exporter.WikiDataExporterClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @ModifyVariable(method = "<init>", at = @At("STORE"))
    private ReloadInstance wrapInitialReload(ReloadInstance reload) {
        return WikiDataExporterClient.wrapInitialReload((Minecraft) (Object) this, reload);
    }
}
