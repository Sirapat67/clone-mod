package com.clonemod.goal;

import com.clonemod.CloneEntity;
import com.clonemod.CloneJob;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Fells whole natural trees (only trees that have natural leaves, so player-built log houses are safe). */
public class LumberjackGoal extends WorkGoal {
    public LumberjackGoal(CloneEntity clone) {
        super(clone);
    }

    @Override
    protected CloneJob job() {
        return CloneJob.WOODCUTTER;
    }

    @Override
    protected boolean isTarget(Level level, BlockPos pos, BlockState state) {
        return state.is(BlockTags.LOGS) && !level.getBlockState(pos.below()).is(BlockTags.LOGS);
    }

    @Override
    protected boolean isValid(Level level, BlockPos pos) {
        return collectTree(level, pos, 250) != null;
    }

    @Override
    protected int breakTime() {
        return 40;
    }

    @Override
    protected void harvest(ServerLevel level, BlockPos pos, BlockState state) {
        List<BlockPos> tree = collectTree(level, pos, 250);
        if (tree == null) {
            return;
        }
        boolean first = true;
        for (BlockPos p : tree) {
            BlockState s = level.getBlockState(p);
            Block.dropResources(s, level, p, level.getBlockEntity(p), this.clone, this.clone.getMainHandItem());
            if (first) {
                level.destroyBlock(p, false, this.clone);
                first = false;
            } else {
                level.removeBlock(p, false);
            }
        }
        this.replant(level, pos, state);
    }

    private void replant(ServerLevel level, BlockPos base, BlockState logState) {
        if (!level.getBlockState(base.below()).is(BlockTags.DIRT)) {
            return;
        }
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(logState.getBlock());
        if (key == null) {
            return;
        }
        Block sapling = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation(key.getNamespace(), key.getPath().replace("_log", "_sapling")));
        if (sapling == null || sapling == Blocks.AIR || !(sapling instanceof SaplingBlock)) {
            return;
        }
        BlockState s = sapling.defaultBlockState();
        if (level.getBlockState(base).isAir() && s.canSurvive(level, base)) {
            level.setBlock(base, s, 3);
        }
    }

    /** Returns all logs connected to the base, or null if it does not look like a natural tree. */
    private static List<BlockPos> collectTree(Level level, BlockPos base, int max) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(base);
        seen.add(base);
        boolean natural = false;
        while (!queue.isEmpty() && result.size() < max) {
            BlockPos p = queue.poll();
            result.add(p);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos n = p.offset(dx, dy, dz);
                        if (seen.contains(n) || !level.hasChunkAt(n)) {
                            continue;
                        }
                        BlockState s = level.getBlockState(n);
                        if (s.is(BlockTags.LOGS)) {
                            seen.add(n);
                            queue.add(n);
                        } else if (!natural && s.getBlock() instanceof LeavesBlock
                                && !s.getValue(LeavesBlock.PERSISTENT)) {
                            natural = true;
                        }
                    }
                }
            }
        }
        return natural ? result : null;
    }
}
