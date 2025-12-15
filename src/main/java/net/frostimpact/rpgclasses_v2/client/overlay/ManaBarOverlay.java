package net.frostimpact.rpgclasses_v2.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

public class ManaBarOverlay implements LayeredDraw.Layer {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 7;
    private static final int BORDER_WIDTH = 1;

    private long shimmerOffset = 0;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        var rpgData = player.getData(ModAttachments.PLAYER_RPG);
        int mana = rpgData.getMana();
        int maxMana = rpgData.getMaxMana();
        float manaPercent = (float) mana / maxMana;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = screenWidth / 2 + 5;
        int y = screenHeight - 50;

        RenderSystem.enableBlend();

        // Draw ornate frame
        drawOrnateFrame(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT, 0xFF5577aa, 0xFF66ccff);

        // Draw mana bar with cyan-to-blue gradient
        int fillWidth = (int) (BAR_WIDTH * manaPercent);
        drawGradientBar(guiGraphics, x + BORDER_WIDTH, y + BORDER_WIDTH, 
            fillWidth - BORDER_WIDTH * 2, BAR_HEIGHT - BORDER_WIDTH * 2, 0xFF00bfff, 0xFF0066cc);

        // Magic shimmer effect
        shimmerOffset = (shimmerOffset + 1) % (BAR_WIDTH * 2);
        int shimmerX = x + (int) (shimmerOffset - BAR_WIDTH);
        if (shimmerX >= x && shimmerX < x + fillWidth) {
            guiGraphics.fill(shimmerX, y + BORDER_WIDTH, shimmerX + 2, y + BAR_HEIGHT - BORDER_WIDTH, 0x80FFFFFF);
        }

        // Low mana pulsing effect
        if (manaPercent < 0.15f) {
            long currentTime = System.currentTimeMillis();
            float pulse = (float) Math.sin(currentTime / 200.0) * 0.5f + 0.5f;
            int pulseAlpha = (int) (pulse * 128) << 24;
            guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, pulseAlpha | 0x0066cc);
        }

        RenderSystem.disableBlend();

        // Draw mana text
        String manaText = mana + "/" + maxMana;
        int textX = x + BAR_WIDTH / 2 - mc.font.width(manaText) / 2;
        int textY = y + (BAR_HEIGHT - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, manaText, textX, textY, 0xFFFFFFFF);
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
