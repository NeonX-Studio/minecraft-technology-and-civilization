package com.nightcity.skills.compat.tcivilization;

import com.nightcity.skills.TSkills;
import com.nightcity.skills.api.Skill;
import com.nightcity.skills.api.SkillsApi;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;

public final class TcivilizationBridge {
    private static volatile boolean bound;

    private TcivilizationBridge() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("tcivilization")) {
            return;
        }
        try {
            Class<?> bridgeClass = Class.forName("com.nightcity.tcivilization.api.ProficiencyBridge");
            Class<?> sinkInterface = Class.forName("com.nightcity.tcivilization.api.ProficiencyBridge$ProficiencySink");
            Object sinkProxy = Proxy.newProxyInstance(
                    sinkInterface.getClassLoader(),
                    new Class<?>[]{sinkInterface},
                    (proxy, method, args) -> handleSinkCall(method.getName(), args)
            );
            Method reg = bridgeClass.getMethod("registerSink", sinkInterface);
            reg.invoke(null, sinkProxy);
            bound = true;
        } catch (ReflectiveOperationException ignored) {
            bound = false;
        }
    }

    private static Object handleSinkCall(String methodName, Object[] args) {
        if (("add".equals(methodName) || "grant".equals(methodName)) && args != null && args.length >= 3) {
            Identifier source = args.length >= 4 && args[3] instanceof Identifier s
                    ? s : Identifier.fromNamespaceAndPath(TSkills.MOD_ID, "bridge");
            if (args[0] instanceof ServerPlayer player) {
                String skillRaw = String.valueOf(args[1]);
                int amount = args[2] instanceof Number n ? n.intValue() : 0;
                Skill.resolve(skillRaw).ifPresent(skill -> SkillsApi.get().grant(player, skill, amount, source, Set.of()));
            } else if (args[0] instanceof UUID playerId) {
                String skillRaw = String.valueOf(args[1]);
                int amount = args[2] instanceof Number n ? n.intValue() : 0;
                Skill.resolve(skillRaw).ifPresent(skill -> SkillsApi.get().grantOffline(playerId, skill, amount, source));
            }
            return null;
        }
        if ("multiplier".equals(methodName) && args != null && args.length >= 3) {
            UUID playerId = args[0] instanceof net.minecraft.world.entity.player.Player pl ? pl.getUUID() : (args[0] instanceof UUID id ? id : null);
            if (playerId != null) {
                var skillOpt = Skill.resolve(String.valueOf(args[1]));
                if (skillOpt.isPresent()) {
                    Identifier eff = args[2] instanceof Identifier i ? i : Identifier.parse(String.valueOf(args[2]));
                    return SkillsApi.get().multiplier(playerId, skillOpt.get(), eff);
                }
            }
            return 1.0D;
        }
        return null;
    }

    public static boolean tryOpenSkillsTab() {
        if (!ModList.get().isLoaded("tcivilization")) {
            return false;
        }
        try {
            Class<?> panelCls = Class.forName("com.nightcity.tcivilization.client.api.CivPanel");
            Method openMethod = panelCls.getMethod("open", Identifier.class);
            openMethod.invoke(null, Identifier.fromNamespaceAndPath(TSkills.MOD_ID, "skills"));
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean isBound() {
        return bound;
    }
}
