package com.example.spiderlord.client;

import com.example.spiderlord.ModEffects;
import com.example.spiderlord.SpiderLordMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Облик «паучьей владычицы»: тёмный скин поверх игрока, бюст, хвост волос,
 * светящиеся глаза на лбу, брюшко и четыре паучьи лапы (вместе с руками это 8 конечностей).
 */
public class SpiderLordLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation SKIN =
            new ResourceLocation(SpiderLordMod.MODID, "textures/entity/spider_lord_skin.png");
    private static final ResourceLocation EXTRA =
            new ResourceLocation(SpiderLordMod.MODID, "textures/entity/spider_lord_extra.png");

    private final ModelPart abdomen;
    private final ModelPart bust;
    private final ModelPart hair;
    private final ModelPart eyes;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] lowers = new ModelPart[4];

    public SpiderLordLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                           EntityModelSet models) {
        super(parent);
        ModelPart root = models.bakeLayer(ClientModEvents.EXTRA_LAYER);
        this.abdomen = root.getChild("abdomen");
        this.bust = root.getChild("bust");
        this.hair = root.getChild("hair");
        this.eyes = root.getChild("eyes");
        for (int i = 0; i < 4; i++) {
            this.legs[i] = root.getChild("leg" + i);
            this.lowers[i] = this.legs[i].getChild("lower");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Брюшко сзади (координаты относительно тела)
        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 0F, 0F, 8F, 8F, 10F),
                PartPose.offset(0F, 8F, 2F));

        // Бюст спереди
        root.addOrReplaceChild("bust",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-3.5F, 2F, -3.5F, 3F, 3F, 1.5F)
                        .addBox(0.5F, 2F, -3.5F, 3F, 3F, 1.5F),
                PartPose.ZERO);

        // Хвост волос (относительно головы)
        root.addOrReplaceChild("hair",
                CubeListBuilder.create().texOffs(0, 40).addBox(-1.5F, -6.5F, 4F, 3F, 14F, 2F),
                PartPose.ZERO);

        // Четыре светящихся глаза на лбу
        root.addOrReplaceChild("eyes",
                CubeListBuilder.create().texOffs(60, 0)
                        .addBox(-3.5F, -7F, -4.6F, 1F, 1F, 1F)
                        .addBox(2.5F, -7F, -4.6F, 1F, 1F, 1F)
                        .addBox(-1.5F, -7.8F, -4.6F, 1F, 1F, 1F)
                        .addBox(0.5F, -7.8F, -4.6F, 1F, 1F, 1F),
                PartPose.ZERO);

        // Лапы: 0,1 = левые (передняя/задняя), 2,3 = правые
        for (int i = 0; i < 4; i++) {
            boolean left = i < 2;
            float s = left ? 1F : -1F;
            PartDefinition leg = root.addOrReplaceChild("leg" + i,
                    CubeListBuilder.create().texOffs(0, 20).addBox(left ? 0F : -10F, -1F, -1F, 10F, 2F, 2F),
                    PartPose.offset(s * 3F, 2F, 2.5F));
            leg.addOrReplaceChild("lower",
                    CubeListBuilder.create().texOffs(0, 26).addBox(left ? 0F : -12F, -1F, -1F, 12F, 2F, 2F),
                    PartPose.offset(s * 10F, 0F, 0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!ModEffects.isLord(player) || player.isInvisible()) return;

        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);

        // 1) Тёмный скин поверх модели игрока
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(SKIN)),
                light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        VertexConsumer extra = buffers.getBuffer(RenderType.entityCutoutNoCull(EXTRA));

        // 2) Тело: брюшко, бюст, лапы
        pose.pushPose();
        model.body.translateAndRotate(pose);

        this.abdomen.xRot = 0.35F + Mth.sin(ageInTicks * 0.08F) * 0.03F;
        this.abdomen.render(pose, extra, light, overlay);
        this.bust.render(pose, extra, light, overlay);

        for (int i = 0; i < 4; i++) {
            float s = i < 2 ? 1F : -1F;
            int row = i % 2;
            float fan = row == 0 ? 0.55F : -0.55F;
            float breathe = Mth.sin(ageInTicks * 0.1F + i * 1.3F) * 0.05F;
            float step = Mth.cos(limbSwing * 0.6662F + i * Mth.HALF_PI) * 0.35F * limbSwingAmount;

            this.legs[i].zRot = -s * (0.8F + breathe + step * 0.5F);
            this.legs[i].yRot = s * fan;
            this.lowers[i].zRot = s * (1.9F + step);
            this.legs[i].render(pose, extra, light, overlay);
        }
        pose.popPose();

        // 3) Голова: волосы и светящиеся глаза
        pose.pushPose();
        model.head.translateAndRotate(pose);
        this.hair.render(pose, extra, light, overlay);
        this.eyes.render(pose, buffers.getBuffer(RenderType.eyes(EXTRA)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
