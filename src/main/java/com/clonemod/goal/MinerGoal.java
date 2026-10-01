package com.clonemod.goal;

import com.clonemod.CloneEntity;
import com.clonemod.CloneJob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import java.util.List;

/** Mines ore blocks near home. If an ore can't be walked to, it digs a small stair tunnel toward it. */
public class MinerGoal extends WorkGoal {
    public MinerGoal(CloneEntity clone) {
        super(clone);
    }

    @Override
    protected CloneJob job() {
        return CloneJob.MINER;
    }

    @Override
    protected boolean isTarget(Level level, BlockPos pos, BlockState state) {
        return state.is(Tags.Blocks.ORES);
    }

    @Override
    protected int breakTime() {
        return 30;
    }

    @Override
    protected int horizontalRadius() {
        return 16;
    }

    @Override
    protected int verticalRadius() {
        return 14;
    }

    @Override
    protected void harvest(ServerLevel level, BlockPos pos, BlockState state) {
        Block.dropResources(state, level, pos, level.getBlockEntity(pos), this.clone, this.clone.getMainHandItem());
        level.destroyBlock(pos, false, this.clone);
    }

    @Override
    protected boolean onUnreachable(ServerLevel level) {
        BlockPos feet = this.clone.blockPosition();
        int dx = this.target.getX() - feet.getX();
        int dy = this.target.getY() - feet.getY();
        int dz = this.target.getZ() - feet.getZ();

        Direction dir;
        if (dx == 0 && dz == 0) {
            dir = this.clone.getDirection();
        } else if (Math.abs(dx) >= Math.abs(dz)) {
            dir = dx >= 0 ? Direction.EAST : Direction.WEST;
        } else {
            dir = dz >= 0 ? Direction.SOUTH : Direction.NORTH;
        }
        int v = dy < -1 ? -1 : (dy > 1 ? 1 : 0);

        BlockPos ahead = feet.relative(dir);
        List<BlockPos> cells;
        BlockPos newFeet;
        if (v < 0) {
            cells = List.of(ahead, ahead.above(), ahead.below());
            newFeet = ahead.below();
        } else if (v > 0) {
            cells = List.of(feet.above(2), ahead.above(), ahead.above(2));
            newFeet = ahead.above();
        } else {
            cells = List.of(ahead, ahead.above());
            newFeet = ahead;
        }

        // Safety checks first: never dig next to lava, never dig things we shouldn't.
        for (BlockPos p : cells) {
            for (Direction d : Direction.values()) {
                if (level.getFluidState(p.relative(d)).is(FluidTags.LAVA)) {
                    return false;
                }
            }
            BlockState s = level.getBlockState(p);
            if (s.isAir()) {
                continue;
            }
            if (s.getFluidState().isEmpty() && s.getCollisionShape(level, p).isEmpty()) {
                continue; // grass, torches, etc. don't block walking
            }
            if (!canDig(level, p, s)) {
                return false;
            }
        }
        if (!level.getFluidState(newFeet.below()).isEmpty()) {
            return false;
        }

        for (BlockPos p : cells) {
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.getCollisionShape(level, p).isEmpty()) {
                continue;
            }
            Block.dropResources(s, level, p, null, this.clone, this.clone.getMainHandItem());
            level.destroyBlock(p, false, this.clone);
        }
        this.clone.swing(InteractionHand.MAIN_HAND);
        this.clone.getNavigation().moveTo(newFeet.getX() + 0.5D, newFeet.getY(), newFeet.getZ() + 0.5D, 1.0D);
        return true;
    }

    private static boolean canDig(Level level, BlockPos pos, BlockState s) {
        if (!s.getFluidState().isEmpty()) {
            return false;
        }
        if (s.getDestroySpeed(level, pos) < 0.0F || level.getBlockEntity(pos) != null) {
            return false;
        }
        return s.is(BlockTags.MINEABLE_WITH_PICKAXE) || s.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }
}
