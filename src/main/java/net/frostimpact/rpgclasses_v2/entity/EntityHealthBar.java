package net.frostimpact.rpgclasses_v2.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = RpgClassesMod.MOD_ID, value = Dist.CLIENT)
public class EntityHealthBar {

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        // Only show for actual living entities (exclude armor stands, item frames, etc.)
        if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player || (mc.player != null && mc.player.distanceTo(entity) > 32.0)) {
            return;
        }

        renderHealthBar(event.getPoseStack(), event.getMultiBufferSource(), entity, event.getPackedLight());
    }

    private static void renderHealthBar(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity, int light) {
        poseStack.pushPose();

        // 1. Move UP
        poseStack.translate(0.0f, 2.3f, 0.0f);

        // 2. Billboard Effect - Always face the camera
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer.getMainCamera() != null) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(mc.gameRenderer.getMainCamera().getXRot()));
        }

        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float healthPercent = Math.max(0.0f, Math.min(1.0f, health / maxHealth));

        // Render entity name above health bar using a different system
        String entityName = entity.getDisplayName().getString();

        poseStack.pushPose();
        // Move above the health bar
        poseStack.translate(0.0f, 0.4f, 0.0f);
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        Matrix4f matrix4f = poseStack.last().pose();
        float bgOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int alphaChannel = (int)(bgOpacity * 255.0F) << 24;

        net.minecraft.client.gui.Font font = mc.font;
        float xOffset = (float)(-font.width(entityName) / 2);

        // Draw background
        font.drawInBatch(
                entityName,
                xOffset,
                0,
                0x20FFFFFF,
                false,
                matrix4f,
                buffer,
                net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,
                alphaChannel,
                light
        );

        // Draw foreground text
        font.drawInBatch(
                entityName,
                xOffset,
                0,
                -1,
                false,
                matrix4f,
                buffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                0,
                light
        );

        poseStack.popPose();

        // Dimensions (Small size)
        float barWidth = 2f;
        float barHeight = 0.125f;
        float startX = -barWidth / 2.0f;
        float startY = 0.0f;

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.gui());

        // 3. Draw Background (Black)
        drawQuad(vc, matrix, (float) (startX - 0.03), (float) (startY - 0.03), (float) (barWidth + 0.06), (float) (barHeight + 0.06), 0.0f, 0, 0, 0, 200, light);

        // Determine Color
        int r = 255, g = 50, b = 50;
        if (healthPercent > 0.66f) { r = 50; g = 255; b = 50; }
        else if (healthPercent > 0.33f) { r = 255; g = 255; b = 0; }

        // Draw Foreground (Health)
        drawQuad(vc, matrix, startX, startY, barWidth * healthPercent, barHeight, -0.01f, r, g, b, 255, light);

        // Draw HP text inside the health bar
        String hpText = String.format("%.0f / %.0f", health, maxHealth);

        poseStack.pushPose();
        poseStack.translate(0.0f, 0.13f, -0.02f); // Center inside bar, slightly forward
        poseStack.scale(-0.018f, -0.018f, 0.018f);

        Matrix4f textMatrix = poseStack.last().pose();
        float textXOffset = (float)(-font.width(hpText) / 2);


        // Draw HP text with shadow for readability
        font.drawInBatch(
                hpText,
                textXOffset,
                0,
                0xFFFFFF, // White color
                true, // Enable shadow
                textMatrix,
                buffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                0,
                light
        );

        poseStack.popPose();

        poseStack.popPose();
    }

    private static void drawQuad(VertexConsumer vc, Matrix4f matrix, float x, float y, float w, float h, float z, int r, int g, int b, int a, int light) {
        // Bottom Left
        vc.addVertex(matrix, x, y + h, z).setColor(r, g, b, a).setLight(light);
        // Bottom Right
        vc.addVertex(matrix, x + w, y + h, z).setColor(r, g, b, a).setLight(light);
        // Top Right
        vc.addVertex(matrix, x + w, y, z).setColor(r, g, b, a).setLight(light);
        // Top Left
        vc.addVertex(matrix, x, y, z).setColor(r, g, b, a).setLight(light);
    }
}