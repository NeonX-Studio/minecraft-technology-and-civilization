package com.nightcity.tcivilization.api;

import com.nightcity.tcivilization.CivilizationSavedData;
import com.nightcity.tcivilization.TCivilization;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Server-authoritative permission resolver chain with fallback role rules (03 §5.1.2, §12.1).
 */
public final class CivPermissions {
    public static final Identifier REASON_PERMISSION = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "permission");
    public static final Identifier REASON_OWNER = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "owner");
    public static final Identifier REASON_SERVER = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "server");

    private static final List<PermissionResolver> RESOLVERS = new ArrayList<>();
    private static final Set<Identifier> DISABLED_FEATURES = ConcurrentHashMap.newKeySet();

    private CivPermissions() {
    }

    public static synchronized void registerResolver(PermissionResolver resolver) {
        Objects.requireNonNull(resolver, "resolver");
        RESOLVERS.add(resolver);
        RESOLVERS.sort(Comparator.comparingInt(PermissionResolver::priority).reversed());
    }

    public static void setFeatureDisabled(Identifier featureId, boolean disabled) {
        if (disabled) {
            DISABLED_FEATURES.add(featureId);
        } else {
            DISABLED_FEATURES.remove(featureId);
        }
    }

    public static synchronized ActionCheck check(ServerPlayer player, Identifier action, @Nullable ActionScope scope) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(action, "action");
        if (DISABLED_FEATURES.contains(action)) {
            return new ActionCheck.Deny(REASON_SERVER, Component.translatable("gui.tcivilization.reason.server"), Optional.empty());
        }
        for (PermissionResolver resolver : RESOLVERS) {
            PermissionResolver.Result result = resolver.resolve(player, action, scope);
            if (result instanceof PermissionResolver.Decided decided) {
                return decided.check();
            }
        }
        if (scope != null && scope.civ() != null && !Civilization.PLAYER_CIV_ID.equals(scope.civ())) {
            return new ActionCheck.Deny(REASON_OWNER, Component.translatable("gui.tcivilization.reason.owner"), Optional.empty());
        }
        ServerLevel overworld = ((ServerLevel) player.level()).getServer().overworld();
        CivilizationSavedData data = CivilizationSavedData.get(overworld);
        CivMember member = data.playerCiv().members().get(player.getUUID());
        if (member == null || CivMember.ROLE_GUEST.equals(member.role())) {
            if (Commands.LEVEL_GAMEMASTERS.check(player.permissions())) {
                return new ActionCheck.Allow();
            }
            return new ActionCheck.Deny(REASON_PERMISSION, Component.translatable("gui.tcivilization.reason.permission"), Optional.empty());
        }
        return new ActionCheck.Allow();
    }

    public static synchronized void clearForTests() {
        RESOLVERS.clear();
        DISABLED_FEATURES.clear();
    }
}
