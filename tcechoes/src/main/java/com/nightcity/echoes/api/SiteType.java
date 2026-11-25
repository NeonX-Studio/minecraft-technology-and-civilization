package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.resources.Identifier;

/**
 * All 25 Echoes ruin/site types matching Appendix A (18 §5.4.1, §6.1, Appendix A).
 */
public enum SiteType {
    CAMP_REMAINS("camp_remains", ArchStyle.TIMBER, 1),
    ROCK_SHELTER("rock_shelter", ArchStyle.CAVE_CUT, 1),
    FROZEN_CAMP("frozen_camp", ArchStyle.TIMBER, 1),
    MEGALITH("megalith", ArchStyle.MEGALITHIC, 2),
    TELL("tell", ArchStyle.MUDBRICK, 3),
    VILLAGE_RUIN("village_ruin", ArchStyle.RAMMED_EARTH, 2),
    CITY_RUIN("city_ruin", ArchStyle.ASHLAR, 6),
    WAYSTATION_RUIN("waystation_ruin", ArchStyle.DRYSTONE, 1),
    BARROW("barrow", ArchStyle.DRYSTONE, 1),
    NECROPOLIS("necropolis", ArchStyle.ASHLAR, 3),
    TEMPLE_RUIN("temple_ruin", ArchStyle.ASHLAR, 3),
    TERRACE_TEMPLE("terrace_temple", ArchStyle.TERRACED, 4),
    CULT_SANCTUM("cult_sanctum", ArchStyle.CAVE_CUT, 2),
    FORTRESS_RUIN("fortress_ruin", ArchStyle.DRYSTONE, 3),
    BATTLEFIELD("battlefield", ArchStyle.DRYSTONE, 3),
    ANCIENT_MINE("ancient_mine", ArchStyle.DRYSTONE, 2),
    ANCIENT_QUARRY("ancient_quarry", ArchStyle.DRYSTONE, 2),
    FURNACE_SITE("furnace_site", ArchStyle.MUDBRICK, 2),
    ANCIENT_ROAD("ancient_road", ArchStyle.DRYSTONE, 1),
    ANCIENT_HARBOR("ancient_harbor", ArchStyle.ASHLAR, 3),
    ANCIENT_WRECK("ancient_wreck", ArchStyle.TIMBER, 1),
    AQUEDUCT_RUIN("aqueduct_ruin", ArchStyle.ASHLAR, 2),
    OBSERVATORY_RUIN("observatory_ruin", ArchStyle.ASHLAR, 2),
    LIBRARY_RUIN("library_ruin", ArchStyle.ASHLAR, 3),
    SUNKEN_CITY("sunken_city", ArchStyle.ASHLAR, 4);

    public static final Codec<SiteType> CODEC = Codec.STRING.xmap(
            s -> SiteType.valueOf(s.toUpperCase(Locale.ROOT)),
            t -> t.path
    );

    private final String path;
    private final ArchStyle defaultStyle;
    private final int chunkSpan;

    SiteType(String path, ArchStyle defaultStyle, int chunkSpan) {
        this.path = path;
        this.defaultStyle = defaultStyle;
        this.chunkSpan = chunkSpan;
    }

    public String path() {
        return path;
    }

    public Identifier id() {
        return Identifier.fromNamespaceAndPath("tcechoes", path);
    }

    public ArchStyle defaultStyle() {
        return defaultStyle;
    }

    public int chunkSpan() {
        return chunkSpan;
    }

    public static SiteType fromOrdinal(int ord) {
        SiteType[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : VILLAGE_RUIN;
    }
}
