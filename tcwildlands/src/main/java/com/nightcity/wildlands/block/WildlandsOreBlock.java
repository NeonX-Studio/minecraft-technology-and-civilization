package com.nightcity.wildlands.block;

import com.nightcity.wildlands.planet.WildlandsCatalog;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Grade-bearing geological ore or coal-seam block with vanilla-equivalent XP tiers (10 §7.2, §16.7).
 */
public final class WildlandsOreBlock extends DropExperienceBlock {
    public static final EnumProperty<OreGrade> GRADE = EnumProperty.create("grade", OreGrade.class);

    private final String mineralKey;

    public WildlandsOreBlock(String mineralKey, IntProvider xpRange, BlockBehaviour.Properties properties) {
        super(xpRange, properties);
        this.mineralKey = mineralKey;
        this.registerDefaultState(this.stateDefinition.any().setValue(GRADE, OreGrade.NORMAL));
    }

    public static IntProvider xpProviderForPath(String blockPath) {
        String key = mineralKeyFromPath(blockPath);
        WildlandsCatalog.MineralSpec spec = WildlandsCatalog.MINERAL_BY_KEY.get(key);
        if (spec == null) {
            return UniformInt.of(0, 0);
        }
        WildlandsCatalog.XpTier tier = spec.xpTier();
        return UniformInt.of(tier.minXp(), tier.maxXp());
    }

    public static String mineralKeyFromPath(String blockPath) {
        if (blockPath.endsWith("_seam")) {
            return blockPath.substring(0, blockPath.length() - "_seam".length());
        }
        if (blockPath.endsWith("_ore")) {
            String stem = blockPath.substring(0, blockPath.length() - "_ore".length());
            for (WildlandsCatalog.MineralSpec spec : WildlandsCatalog.MINERALS) {
                if (stem.endsWith("_" + spec.key())) {
                    return spec.key();
                }
            }
        }
        return blockPath;
    }

    public String mineralKey() {
        return mineralKey;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRADE);
    }
}
