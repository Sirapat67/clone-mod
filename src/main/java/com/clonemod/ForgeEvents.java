package com.clonemod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = CloneMod.MODID)
public class ForgeEvents {
    /** Maximum clones a single player can have loaded at once. */
    public static final int MAX_CLONES_PER_PLAYER = 20;

    @SubscribeEvent
    public static void onLevelChange(PlayerXpEvent.LevelChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int gained = event.getLevels();
        if (gained <= 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        List<? extends CloneEntity> existing = level.getEntities(
                EntityTypeTest.forClass(CloneEntity.class),
                c -> player.getUUID().equals(c.getOwnerUUID()));

        int room = MAX_CLONES_PER_PLAYER - existing.size();
        if (room <= 0) {
            player.sendSystemMessage(Component.literal("Level up! But you already have the maximum number of clones."));
            return;
        }

        int count = Math.min(gained, room);
        for (int i = 0; i < count; i++) {
            CloneEntity clone = ModEntities.CLONE.get().create(level);
            if (clone == null) {
                continue;
            }
            double ox = (level.random.nextDouble() - 0.5D) * 2.0D;
            double oz = (level.random.nextDouble() - 0.5D) * 2.0D;
            clone.moveTo(player.getX() + ox, player.getY(), player.getZ() + oz, player.getYRot(), 0.0F);
            clone.setOwnerUUID(player.getUUID());
            int number = existing.size() + i + 1;
            clone.setBaseName(player.getName().getString() + " Clone #" + number);
            clone.setJob(CloneJob.FOLLOW);
            level.addFreshEntity(clone);
        }

        player.sendSystemMessage(Component.literal(
                "Level up! A new clone of you appeared. Right-click it to change its job (Shift + right-click = follow me)."));
    }
}
