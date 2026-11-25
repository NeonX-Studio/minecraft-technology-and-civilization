package com.nightcity.tcivilization.world;

import com.nightcity.tcivilization.TCivilization;
import com.nightcity.tcivilization.registry.TcRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity preserving original block ID and NBT for removed content (03 §5.9, §7, RM-P8).
 */
public class PlaceholderBlockEntity extends BlockEntity {
    public static final Identifier UNKNOWN_ID = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "unknown");

    private Identifier originalId = UNKNOWN_ID;
    private CompoundTag originalNbt = new CompoundTag();

    public PlaceholderBlockEntity(BlockPos pos, BlockState state) {
        super(TcRegistries.PLACEHOLDER_BLOCK_ENTITY.get(), pos, state);
    }

    public Identifier originalId() {
        return originalId;
    }

    public CompoundTag originalNbt() {
        return originalNbt.copy();
    }

    public void setOriginalContent(Identifier originalId, CompoundTag originalNbt) {
        this.originalId = originalId == null ? UNKNOWN_ID : originalId;
        this.originalNbt = originalNbt == null ? new CompoundTag() : originalNbt.copy();
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("original_id", Identifier.CODEC, this.originalId);
        output.store("original_nbt", CompoundTag.CODEC, this.originalNbt);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.originalId = input.read("original_id", Identifier.CODEC).orElse(UNKNOWN_ID);
        this.originalNbt = input.read("original_nbt", CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }
}
