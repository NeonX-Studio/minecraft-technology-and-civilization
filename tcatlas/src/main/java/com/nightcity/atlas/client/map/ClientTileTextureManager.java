package com.nightcity.atlas.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.map.MapTileCache;
import com.nightcity.atlas.map.MapTileRasterizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages GPU DynamicTexture uploads for 256x256 map tiles, strictly enforcing <= tile_uploads_per_frame
 * (default 4 uploads/frame <= 0.3 ms, 22 §5.9, §14).
 */
public final class ClientTileTextureManager implements AutoCloseable {
    private record GpuEntry(Identifier textureId, DynamicTexture texture, long uploadedGeneration) {
    }

    private final Map<MapTileCache.TileKey, GpuEntry> gpuTiles = new HashMap<>();
    private final ConcurrentLinkedQueue<MapTileCache.TileKey> pendingEvictions = new ConcurrentLinkedQueue<>();
    private int uploadsThisFrame = 0;

    public void onCacheEvict(MapTileCache.TileKey evictedKey) {
        pendingEvictions.add(evictedKey);
    }

    public void beginFrame() {
        uploadsThisFrame = 0;
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        MapTileCache.TileKey key;
        while ((key = pendingEvictions.poll()) != null) {
            GpuEntry removed = gpuTiles.remove(key);
            if (removed != null) {
                tm.release(removed.textureId);
                removed.texture.close();
            }
        }
    }

    @Nullable
    public Identifier resolveOrUploadTexture(MapTileCache.TileSnapshot snapshot, int maxUploadsPerFrame) {
        if (snapshot.placeholder()) {
            return null;
        }
        GpuEntry existing = gpuTiles.get(snapshot.key());
        if (existing != null && existing.uploadedGeneration == snapshot.generation()) {
            return existing.textureId;
        }
        if (uploadsThisFrame >= maxUploadsPerFrame) {
            return existing != null ? existing.textureId : null;
        }

        uploadsThisFrame++;
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        DynamicTexture dyn;
        Identifier texId;
        if (existing != null) {
            dyn = existing.texture;
            texId = existing.textureId;
        } else {
            String path = "dynamic/tile_" + snapshot.key().dimension().getNamespace() + "_"
                    + snapshot.key().dimension().getPath().replace('/', '_')
                    + "_z" + snapshot.key().zoom()
                    + "_" + (snapshot.key().tileX() < 0 ? "m" + (-snapshot.key().tileX()) : snapshot.key().tileX())
                    + "_" + (snapshot.key().tileZ() < 0 ? "m" + (-snapshot.key().tileZ()) : snapshot.key().tileZ());
            texId = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, path);
            dyn = new DynamicTexture(() -> "tcatlas_tile", MapTileRasterizer.TILE_SIZE, MapTileRasterizer.TILE_SIZE, false);
            tm.register(texId, dyn);
        }

        NativeImage img = dyn.getPixels();
        if (img != null) {
            int[] src = snapshot.argbPixels();
            for (int y = 0; y < MapTileRasterizer.TILE_SIZE; y++) {
                int row = y << 8;
                for (int x = 0; x < MapTileRasterizer.TILE_SIZE; x++) {
                    img.setPixel(x, y, src[row | x]);
                }
            }
            dyn.upload();
        }
        gpuTiles.put(snapshot.key(), new GpuEntry(texId, dyn, snapshot.generation()));
        return texId;
    }

    @Override
    public void close() {
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc != null ? mc.getTextureManager() : null;
        for (GpuEntry entry : gpuTiles.values()) {
            if (tm != null) {
                tm.release(entry.textureId);
            }
            entry.texture.close();
        }
        gpuTiles.clear();
        pendingEvictions.clear();
    }
}
