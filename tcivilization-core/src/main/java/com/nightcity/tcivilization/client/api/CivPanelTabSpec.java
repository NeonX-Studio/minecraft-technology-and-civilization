package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;

/**
 * Registration descriptor for a Civilization Panel tab (03 §5.10.1, §12.2, 31 §4.3).
 */
public record CivPanelTabSpec(
        Identifier id,
        CivTabGroup group,
        int order,
        Identifier icon,
        Optional<Identifier> unlockTech,
        Predicate<CivTabContext> visible,
        Optional<OverviewCard> overviewCard,
        Function<CivTabContext, CivTabPage> factory
) {
    public CivPanelTabSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(icon, "icon");
        unlockTech = unlockTech == null ? Optional.empty() : unlockTech;
        visible = visible == null ? ctx -> true : visible;
        overviewCard = overviewCard == null ? Optional.empty() : overviewCard;
        Objects.requireNonNull(factory, "factory");
    }

    public String translationKey() {
        return "civtab." + id.getNamespace() + "." + id.getPath();
    }
}
