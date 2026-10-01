package com.clonemod.goal;

import com.clonemod.CloneEntity;
import com.clonemod.CloneJob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class FollowOwnerGoal extends Goal {
    private final CloneEntity clone;
    private Player owner;
    private int timer;

    public FollowOwnerGoal(CloneEntity clone) {
        this.clone = clone;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.clone.getJob() != CloneJob.FOLLOW) {
            return false;
        }
        Player o = this.clone.getOwner();
        if (o == null || o.isSpectator() || o.level() != this.clone.level()) {
            return false;
        }
        if (this.clone.distanceToSqr(o) < 16.0D) {
            return false;
        }
        this.owner = o;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.clone.getJob() == CloneJob.FOLLOW
                && this.owner != null
                && this.owner.isAlive()
                && this.owner.level() == this.clone.level()
                && this.clone.distanceToSqr(this.owner) > 9.0D;
    }

    @Override
    public void start() {
        this.timer = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.clone.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.clone.getLookControl().setLookAt(this.owner, 10.0F, this.clone.getMaxHeadXRot());
        if (--this.timer > 0) {
            return;
        }
        this.timer = 10;
        if (this.clone.distanceToSqr(this.owner) >= 1024.0D) {
            this.clone.teleportTo(this.owner.getX() + 1.0D, this.owner.getY(), this.owner.getZ() + 1.0D);
            this.clone.getNavigation().stop();
        } else {
            this.clone.getNavigation().moveTo(this.owner, 1.1D);
        }
    }
}
