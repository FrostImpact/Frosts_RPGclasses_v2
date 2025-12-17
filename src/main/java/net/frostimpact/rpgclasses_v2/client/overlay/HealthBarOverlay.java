package net.frostimpact.rpgclasses_v2.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

public class HealthBarOverlay implements LayeredDraw.Layer {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 7;
    private static final int BORDER_WIDTH = 1;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = screenWidth / 2 - BAR_WIDTH - 5;
        int y = screenHeight - 50;

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthPercent = health / maxHealth;

        RenderSystem.enableBlend();

        // Draw frame
        drawOrnateFrame(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT, 0xFF8b7355, 0xFFd4af37);

        // Draw health bar with gradient based on health percentage
        int fillWidth = (int) (BAR_WIDTH * healthPercent);
        int topColor, bottomColor;

        if (healthPercent > 0.6f) {
            topColor = 0xFF2eb82e;
            bottomColor = 0xFF1a8a1a;
        } else if (healthPercent > 0.3f) {
            topColor = 0xFFe6b800;
            bottomColor = 0xFFb38f00;
        } else {
            topColor = 0xFFcc0000;
            bottomColor = 0xFF800000;
        }

        drawGradientBar(guiGraphics, x + BORDER_WIDTH, y + BORDER_WIDTH, 
            fillWidth - BORDER_WIDTH * 2, BAR_HEIGHT - BORDER_WIDTH * 2, topColor, bottomColor);

        // Low health effect
        if (healthPercent < 0.25f) {
            long currentTime = System.currentTimeMillis();
            float pulse = (float) Math.sin(currentTime / 200.0) * 0.5f + 0.5f;
            int pulseAlpha = (int) (pulse * 128) << 24;
            guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, pulseAlpha | 0xFF0000);
        }

        RenderSystem.disableBlend();

        // Draw health text
        String healthText = String.format("%.0f/%.0f", health, maxHealth);
        int textX = x + BAR_WIDTH / 2 - mc.font.width(healthText) / 2;
        int textY = y + (BAR_HEIGHT - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, healthText, textX, textY, 0xFFFFFFFF);
    }

    private void drawOrnateFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, int borderColor, int cornerColor) {
        // Main border
        guiGraphics.fill(x, y, x + width, y + BORDER_WIDTH, borderColor);
        guiGraphics.fill(x, y + height - BORDER_WIDTH, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + BORDER_WIDTH, y + height, borderColor);
        guiGraphics.fill(x + width - BORDER_WIDTH, y, x + width, y + height, borderColor);

        // Corner decorations
        int cornerSize = 2;
        guiGraphics.fill(x, y, x + cornerSize, y + cornerSize, cornerColor);
        guiGraphics.fill(x + width - cornerSize, y, x + width, y + cornerSize, cornerColor);
        guiGraphics.fill(x, y + height - cornerSize, x + cornerSize, y + height, cornerColor);
        guiGraphics.fill(x + width - cornerSize, y + height - cornerSize, x + width, y + height, cornerColor);
    }

    private void drawGradientBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int topColor, int bottomColor) {
        if (width <= 0 || height <= 0) return;
        guiGraphics.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
    }
}
