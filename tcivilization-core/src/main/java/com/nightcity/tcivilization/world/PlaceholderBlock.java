package com.nightcity.tcivilization.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Fallback placeholder block replacing removed content IDs; right-click reports original ID (03 §7, §9).
 */
public class PlaceholderBlock extends Block implements EntityBlock {
    public PlaceholderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlaceholderBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide()) {
            Identifier orig = PlaceholderBlockEntity.UNKNOWN_ID;
            if (level.getBlockEntity(pos) instanceof PlaceholderBlockEntity be) {
                orig = be.originalId();
            }
            player.sendSystemMessage(Component.translatable("block.tcivilization.placeholder.inspect", orig.toString()));
        }
        return InteractionResult.SUCCESS;
    }
}
