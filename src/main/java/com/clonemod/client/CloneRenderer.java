package com.clonemod.client;

import com.clonemod.CloneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Draws the clone like a player, using the owner's skin. */
public class CloneRenderer extends MobRenderer<CloneEntity, PlayerModel<CloneEntity>> {
    private final PlayerModel<CloneEntity> wideModel;
    private final PlayerModel<CloneEntity> slimModel;

    public CloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = this.model;
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(CloneEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        PlayerInfo info = this.getInfo(entity);
        this.model = (info != null && "slim".equals(info.getModelName())) ? this.slimModel : this.wideModel;
        boolean holding = !entity.getMainHandItem().isEmpty();
        this.model.rightArmPose = holding ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
        this.model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CloneEntity entity) {
        UUID id = entity.getOwnerUUID();
        if (id == null) {
            return DefaultPlayerSkin.getDefaultSkin();
        }
        PlayerInfo info = this.getInfo(entity);
        return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
    }

    private PlayerInfo getInfo(CloneEntity entity) {
        UUID id = entity.getOwnerUUID();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (id == null || connection == null) {
            return null;
        }
        return connection.getPlayerInfo(id);
    }
}
