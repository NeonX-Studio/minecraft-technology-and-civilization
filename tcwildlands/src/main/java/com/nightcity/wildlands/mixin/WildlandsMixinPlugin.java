package com.nightcity.wildlands.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class WildlandsMixinPlugin implements IMixinConfigPlugin {
    private boolean hasPortalSpawnEvent = true;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            Class.forName("net.neoforged.neoforge.event.level.BlockEvent$PortalSpawnEvent", false, getClass().getClassLoader());
            hasPortalSpawnEvent = true;
        } catch (ClassNotFoundException ex) {
            hasPortalSpawnEvent = false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("PortalFallbackMixin") && hasPortalSpawnEvent) {
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
