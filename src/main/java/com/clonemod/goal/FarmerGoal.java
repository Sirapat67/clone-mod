package com.clonemod.goal;

import com.clonemod.CloneEntity;
import com.clonemod.CloneJob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Harvests fully grown crops (wheat, carrots, potatoes, beetroot) and replants them. */
public class FarmerGoal extends WorkGoal {
    public FarmerGoal(CloneEntity clone) {
        super(clone);
    }

    @Override
    protected CloneJob job() {
        return CloneJob.FARMER;
    }

    @Override
    protected boolean isTarget(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    @Override
    protected int breakTime() {
        return 8;
    }

    @Override
    protected int verticalRadius() {
        return 4;
    }

    @Override
    protected void harvest(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof CropBlock crop)) {
            return;
        }
        Block.dropResources(state, level, pos, null, this.clone, this.clone.getMainHandItem());
        level.levelEvent(2001, pos, Block.getId(state));
        level.setBlock(pos, crop.getStateForAge(0), 3);
    }
}
