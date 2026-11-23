package org.sinytra.wiki.exporter.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.sinytra.wiki.exporter.Constants;
import org.sinytra.wiki.exporter.platform.services.ExporterModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class WikiExporterRenderer implements ExporterModule {
    private final Set<String> namespaces;
    private final WikiRenderModuleConfig config;

    public WikiExporterRenderer(Set<String> namespaces, WikiRenderModuleConfig config) {
        this.namespaces = namespaces;
        this.config = config;
    }

    @Override
    public void run(Path output) throws Exception {
        if (!this.config.png()) {
            return;
        }

        List<Pair<Identifier, Item>> renderable = getRenderableItems();
        if (renderable.isEmpty()) {
            return;
        }

        String subDir = System.getProperty("wiki_exporter.module.%s.output.group.items".formatted(WikiRenderModuleFactory.NAME));
        for (String namespace : renderable.stream().map(p -> p.getFirst().getNamespace()).distinct().toList()) {
            Files.createDirectories(getOutputDir(output, namespace, subDir));
        }

        Constants.LOG.info("Rendering {} items", renderable.size());
        renderItems(renderable, output, subDir);
    }

    private List<Pair<Identifier, Item>> getRenderableItems() {
        if (this.namespaces.isEmpty()) {
            return List.of();
        }

        Constants.LOG.info("Rendering items for namespaces {}", this.namespaces);

        List<Pair<Identifier, Item>> list = new ArrayList<>();
        for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            Identifier name = entry.getKey().identifier();
            if (this.namespaces.contains(name.getNamespace())) {
                list.add(Pair.of(name, entry.getValue()));
            }
        }

        return list;
    }

    private void renderItems(List<Pair<Identifier, Item>> renderable, Path root, @Nullable String subDir) {
        Minecraft minecraft = Minecraft.getInstance();
        int resolution = this.config.resolution();
        RenderTarget target = minecraft.submit(() -> new TextureTarget("Wiki Exporter", resolution, resolution, true, GpuFormat.RGBA8_UNORM)).join();
        SimpleItemRenderer renderer = minecraft.submit(() -> new SimpleItemRenderer(target, 32)).join();
        List<CompletableFuture<Void>> writes = minecraft.submit(() -> renderable.stream()
            .map(p -> exportRenderItem(getOutputDir(root, p.getFirst().getNamespace(), subDir), target, renderer, new ItemStack(p.getSecond())))
            .toList()
        ).join();

        CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).join();
        minecraft.submit(target::destroyBuffers).join();
    }

    private static CompletableFuture<Void> exportRenderItem(Path root, RenderTarget target, SimpleItemRenderer renderer, ItemStack stack) {
        Identifier name = stack.getItem().builtInRegistryHolder().key().identifier();

        RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(target.getColorTexture(), GuiRenderer.CLEAR_COLOR, target.getDepthTexture(), 0.0);

        renderer.renderItem(stack);

        return ImageWriter.writeAsPNG(root, name.getPath(), target.getColorTexture(), true);
    }

    private static Path getOutputDir(Path root, String namespace, @Nullable String group) {
        Path base = root.resolve(namespace);
        return group != null ? base.resolve(group) : base;
    }
}
