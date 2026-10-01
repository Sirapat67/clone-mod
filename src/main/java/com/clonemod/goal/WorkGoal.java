package com.clonemod.goal;

import com.clonemod.CloneEntity;
import com.clonemod.CloneJob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared logic: find a block near home, walk to it, "break" it for a while, then harvest it. */
public abstract class WorkGoal extends Goal {
    protected final CloneEntity clone;
    protected BlockPos target;

    private int scanDelay = 0;
    private int breakTicks = 0;
    private int repathTicks = 0;
    private int stuck = 0;
    private double lastDist = Double.MAX_VALUE;
    private final Set<Long> blacklist = new HashSet<>();

    protected WorkGoal(CloneEntity clone) {
        this.clone = clone;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    protected abstract CloneJob job();

    /** Cheap check: is this block something we want to work on? */
    protected abstract boolean isTarget(Level level, BlockPos pos, BlockState state);

    /** Slower check, only run on the closest candidates. */
    protected boolean isValid(Level level, BlockPos pos) {
        return true;
    }

    /** Ticks needed to "break" the target. */
    protected abstract int breakTime();

    protected abstract void harvest(ServerLevel level, BlockPos pos, BlockState state);

    protected int horizontalRadius() {
        return 20;
    }

    protected int verticalRadius() {
        return 8;
    }

    /** Called when no walking path exists. Return true if it did something useful (e.g. dug a step). */
    protected boolean onUnreachable(ServerLevel level) {
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (this.clone.getJob() != this.job()) {
            if (!this.blacklist.isEmpty()) {
                this.blacklist.clear();
            }
            return false;
        }
        if (this.clone.getTarget() != null) {
            return false;
        }
        if (--this.scanDelay > 0) {
            return false;
        }
        this.scanDelay = 30 + this.clone.getRandom().nextInt(20);
        this.target = this.findTarget();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.clone.getJob() != this.job() || this.target == null || this.clone.getTarget() != null) {
            return false;
        }
        Level level = this.clone.level();
        return this.isTarget(level, this.target, level.getBlockState(this.target));
    }

    @Override
    public void start() {
        this.breakTicks = 0;
        this.repathTicks = 0;
        this.stuck = 0;
        this.lastDist = Double.MAX_VALUE;
    }

    @Override
    public void stop() {
        if (this.target != null) {
            this.clone.level().destroyBlockProgress(this.clone.getId(), this.target, -1);
        }
        this.target = null;
        this.clone.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        Level level = this.clone.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(this.target);
        this.clone.getLookControl().setLookAt(center.x, center.y, center.z);
        double distSq = this.clone.getEyePosition().distanceToSqr(center);

        if (distSq <= 16.0D) {
            this.clone.getNavigation().stop();
            this.stuck = 0;
            this.breakTicks++;
            if (this.breakTicks % 6 == 0) {
                this.clone.swing(InteractionHand.MAIN_HAND);
            }
            int total = this.breakTime();
            level.destroyBlockProgress(this.clone.getId(), this.target, Math.min(9, this.breakTicks * 10 / total));
            if (this.breakTicks >= total) {
                level.destroyBlockProgress(this.clone.getId(), this.target, -1);
                BlockState state = level.getBlockState(this.target);
                this.harvest(server, this.target, state);
                this.breakTicks = 0;
                this.target = null;
                this.scanDelay = 4;
            }
            return;
        }

        this.breakTicks = 0;
        if (--this.repathTicks > 0) {
            return;
        }
        this.repathTicks = 10;
        Path path = this.clone.getNavigation().createPath(this.target, 1);
        if (path != null && path.canReach()) {
            this.clone.getNavigation().moveTo(path, 1.0D);
            if (distSq >= this.lastDist - 0.5D) {
                this.stuck++;
            } else {
                this.stuck = 0;
            }
            this.lastDist = distSq;
            if (this.stuck >= 8) {
                this.giveUp();
            }
        } else if (!this.onUnreachable(server)) {
            this.giveUp();
        }
    }

    private void giveUp() {
        if (this.target != null) {
            this.blacklist.add(this.target.asLong());
            this.clone.level().destroyBlockProgress(this.clone.getId(), this.target, -1);
        }
        this.target = null;
        this.stuck = 0;
        this.scanDelay = 10;
        this.clone.getNavigation().stop();
    }

    private BlockPos findTarget() {
        Level level = this.clone.level();
        BlockPos home = this.clone.getHome();
        int r = this.horizontalRadius();
        int v = this.verticalRadius();
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -v; dy <= v; dy++) {
                    m.set(home.getX() + dx, home.getY() + dy, home.getZ() + dz);
                    if (!level.hasChunkAt(m) || this.blacklist.contains(m.asLong())) {
                        continue;
                    }
                    BlockState s = level.getBlockState(m);
                    if (!s.isAir() && this.isTarget(level, m, s)) {
                        found.add(m.immutable());
                    }
                }
            }
        }
        BlockPos from = this.clone.blockPosition();
        found.sort(Comparator.comparingDouble(p -> p.distSqr(from)));
        int checks = 0;
        for (BlockPos p : found) {
            if (checks++ >= 8) {
                break;
            }
            if (this.isValid(level, p)) {
                return p;
            }
            this.blacklist.add(p.asLong());
        }
        if (this.blacklist.size() > 500) {
            this.blacklist.clear();
        }
        return null;
    }
}
