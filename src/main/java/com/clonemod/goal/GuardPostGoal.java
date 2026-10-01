package com.clonemod.goal;

import com.clonemod.CloneEntity;
import com.clonemod.CloneJob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** A guard returns to its post when it wanders away and has nothing to fight. */
public class GuardPostGoal extends Goal {
    private final CloneEntity clone;

    public GuardPostGoal(CloneEntity clone) {
        this.clone = clone;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.clone.getJob() == CloneJob.GUARD
                && this.clone.getTarget() == null
                && this.clone.blockPosition().distSqr(this.clone.getHome()) > 64.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return this.clone.getJob() == CloneJob.GUARD
                && this.clone.getTarget() == null
                && !this.clone.getNavigation().isDone();
    }

    @Override
    public void start() {
        BlockPos h = this.clone.getHome();
        this.clone.getNavigation().moveTo(h.getX() + 0.5D, h.getY(), h.getZ() + 0.5D, 1.0D);
    }

    @Override
    public void stop() {
        this.clone.getNavigation().stop();
    }
}
