package com.nightcity.atlas.api;

import com.nightcity.atlas.TAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface HudApi {
    enum ToastPriority {
        INFO, SUCCESS, WARN, ALERT
    }

    record Toast(
            Identifier category,
            Component title,
            @Nullable Component body,
            @Nullable Identifier icon,
            ToastPriority priority,
            int durationTicks,
            @Nullable String dedupKey
    ) {
    }

    static HudApi get() {
        return TAtlas.hud();
    }

    void registerWidget(HudWidget widget);

    Collection<HudWidget> widgets();

    void pushToast(Toast toast);

    HudLevel currentLevel();

    void setLevel(HudLevel level);

    default HudLevel cycleLevel() {
        HudLevel next = currentLevel().next();
        setLevel(next);
        return next;
    }

    HudSkin currentSkin();

    void setActiveEraSkin(String eraId);
}
