package com.clonemod;

import com.clonemod.goal.FarmerGoal;
import com.clonemod.goal.FollowOwnerGoal;
import com.clonemod.goal.GuardPostGoal;
import com.clonemod.goal.LumberjackGoal;
import com.clonemod.goal.MinerGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CloneEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> JOB =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.INT);

    private BlockPos home = BlockPos.ZERO;
    private String baseName = "Clone";

    public CloneEntity(EntityType<? extends CloneEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new LumberjackGoal(this));
        this.goalSelector.addGoal(2, new MinerGoal(this));
        this.goalSelector.addGoal(2, new FarmerGoal(this));
        this.goalSelector.addGoal(3, new GuardPostGoal(this));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> this.getJob() == CloneJob.GUARD && e.blockPosition().distSqr(this.home) < 576.0D));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(JOB, 0);
    }

    // ---------- owner / job ----------

    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER).orElse(null);
    }

    public void setOwnerUUID(UUID id) {
        this.entityData.set(OWNER, Optional.ofNullable(id));
    }

    public Player getOwner() {
        UUID id = this.getOwnerUUID();
        return id == null ? null : this.level().getPlayerByUUID(id);
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(this.getOwnerUUID());
    }

    public CloneJob getJob() {
        return CloneJob.byId(this.entityData.get(JOB));
    }

    public BlockPos getHome() {
        return this.home;
    }

    public void setBaseName(String name) {
        this.baseName = name;
        this.refreshName();
    }

    public String getBaseName() {
        return this.baseName;
    }

    /** Sets the job and remembers the current spot as the work area. */
    public void setJob(CloneJob job) {
        this.entityData.set(JOB, job.ordinal());
        this.home = this.blockPosition();
        this.getNavigation().stop();
        this.setTarget(null);
        this.equipFor(job);
        this.refreshName();
    }

    private void equipFor(CloneJob job) {
        ItemStack tool;
        switch (job) {
            case WOODCUTTER -> tool = new ItemStack(Items.IRON_AXE);
            case MINER -> tool = new ItemStack(Items.DIAMOND_PICKAXE);
            case FARMER -> tool = new ItemStack(Items.IRON_HOE);
            case GUARD -> tool = new ItemStack(Items.IRON_SWORD);
            default -> tool = ItemStack.EMPTY;
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, tool);
    }

    private void refreshName() {
        this.setCustomName(Component.literal(this.baseName + " [" + this.getJob().label + "]"));
        this.setCustomNameVisible(true);
    }

    // ---------- interaction ----------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !this.isOwner(player)) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        CloneJob next = player.isShiftKeyDown() ? CloneJob.FOLLOW : this.getJob().next();
        this.setJob(next);
        player.displayClientMessage(Component.literal(this.baseName + ": " + next.label), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity().getUUID().equals(this.getOwnerUUID())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player p && p.getUUID().equals(this.getOwnerUUID())) {
            return false;
        }
        if (target instanceof CloneEntity c && Objects.equals(c.getOwnerUUID(), this.getOwnerUUID())) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) {
            Player owner = this.getOwner();
            if (owner != null) {
                owner.sendSystemMessage(Component.literal(this.baseName + " has died!"));
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    // ---------- saving ----------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = this.getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("Job", this.getJob().ordinal());
        tag.putInt("HomeX", this.home.getX());
        tag.putInt("HomeY", this.home.getY());
        tag.putInt("HomeZ", this.home.getZ());
        tag.putString("BaseName", this.baseName);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.setOwnerUUID(tag.getUUID("Owner"));
        }
        this.entityData.set(JOB, tag.getInt("Job"));
        this.home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        if (tag.contains("BaseName")) {
            this.baseName = tag.getString("BaseName");
        }
        this.refreshName();
    }
}
