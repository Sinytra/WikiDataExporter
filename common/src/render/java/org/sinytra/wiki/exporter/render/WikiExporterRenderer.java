package org.sinytra.wiki.exporter.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

        List<Pair<ResourceLocation, Item>> renderable = getRenderableItems();
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

    private List<Pair<ResourceLocation, Item>> getRenderableItems() {
        if (this.namespaces.isEmpty()) {
            return List.of();
        }

        Constants.LOG.info("Rendering items for namespaces {}", this.namespaces);

        List<Pair<ResourceLocation, Item>> list = new ArrayList<>();
        for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation name = entry.getKey().location();
            if (this.namespaces.contains(name.getNamespace())) {
                list.add(Pair.of(name, entry.getValue()));
            }
        }

        return list;
    }

    private void renderItems(List<Pair<ResourceLocation, Item>> renderable, Path root, @Nullable String subDir) {
        Minecraft minecraft = Minecraft.getInstance();
        int resolution = this.config.resolution();
        RenderTarget target = minecraft.submit(() -> new TextureTarget(resolution, resolution, true, Minecraft.ON_OSX)).join();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        SimpleItemRenderer renderer = new SimpleItemRenderer(bufferSource, 32);
        List<CompletableFuture<Void>> writes = minecraft.submit(() -> renderable.stream()
            .map(p -> exportRenderItem(getOutputDir(root, p.getFirst().getNamespace(), subDir), target, renderer, new ItemStack(p.getSecond())))
            .toList()
        ).join();

        CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).join();
        minecraft.submit(target::destroyBuffers).join();
    }

    private static CompletableFuture<Void> exportRenderItem(Path root, RenderTarget target, SimpleItemRenderer renderer, ItemStack stack) {
        ResourceLocation name = stack.getItem().builtInRegistryHolder().key().location();

        RenderSystem.enableBlend();
        RenderSystem.clear(16640, Minecraft.ON_OSX);

        target.setClearColor(0, 0, 0, 0);
        target.clear(Minecraft.ON_OSX);

        target.bindWrite(true);
        renderer.renderItem(stack);
        target.unbindWrite();

        return ImageWriter.writeAsPNG(root, name.getPath(), target, true);
    }

    private static Path getOutputDir(Path root, String namespace, @Nullable String group) {
        Path base = root.resolve(namespace);
        return group != null ? base.resolve(group) : base;
    }
}
