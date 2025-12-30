package net.frostimpact.rpgclasses_v2.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.frostimpact.rpgclasses_v2.rpgclass.AbilityUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/**
 * Overlay that displays ability cooldowns next to the hotbar on the bottom right
 */
public class CooldownOverlay implements LayeredDraw.Layer {
    private static final int ICON_SIZE = 20;
    private static final int ICON_SPACING = 4;
    private static final int MARGIN_RIGHT = 5;
    private static final int MARGIN_BOTTOM = 35; // Above hotbar level
    
    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // Don't render if any screen is open
        if (mc.screen != null) return;
        
        PlayerRPGData rpgData = player.getData(ModAttachments.PLAYER_RPG);
        String currentClass = rpgData.getCurrentClass();
        
        if (currentClass == null || currentClass.equals("NONE")) return;
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // Position to the right of center (right of the hotbar)
        int startX = screenWidth / 2 + 95; // Start after hotbar (hotbar is 182 wide, so center + 91 + some margin)
        int y = screenHeight - MARGIN_BOTTOM;
        
        RenderSystem.enableBlend();
        
        // Draw 4 ability icons
        for (int slot = 1; slot <= 4; slot++) {
            int x = startX + (slot - 1) * (ICON_SIZE + ICON_SPACING);
            drawAbilityIcon(guiGraphics, mc, rpgData, currentClass, slot, x, y);
        }
        
        RenderSystem.disableBlend();
    }
    
    private void drawAbilityIcon(GuiGraphics guiGraphics, Minecraft mc, PlayerRPGData rpgData, 
            String currentClass, int slot, int x, int y) {
        String abilityId = currentClass.toLowerCase() + "_ability_" + slot;
        int cooldown = rpgData.getAbilityCooldown(abilityId);
        int mana = rpgData.getMana();
        int manaCost = AbilityUtils.getAbilityManaCost(currentClass, slot);
        
        // Background
        int bgColor = 0xCC222222;
        guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);
        
        // Border color based on state
        int borderColor;
        if (cooldown > 0) {
            borderColor = 0xFFFF4444; // Red when on cooldown
        } else if (mana < manaCost) {
            borderColor = 0xFF4444FF; // Blue when not enough mana
        } else {
            borderColor = 0xFF44FF44; // Green when ready
        }
        
        // Draw border
        guiGraphics.fill(x, y, x + ICON_SIZE, y + 1, borderColor);
        guiGraphics.fill(x, y + ICON_SIZE - 1, x + ICON_SIZE, y + ICON_SIZE, borderColor);
        guiGraphics.fill(x, y + 1, x + 1, y + ICON_SIZE - 1, borderColor);
        guiGraphics.fill(x + ICON_SIZE - 1, y + 1, x + ICON_SIZE, y + ICON_SIZE - 1, borderColor);
        
        // Keybind letter
        String keybind = AbilityUtils.getAbilityKeybind(slot);
        int textColor = cooldown > 0 ? 0xFF888888 : (mana < manaCost ? 0xFF6666FF : 0xFFFFFFFF);
        
        // Center the keybind text
        int textWidth = mc.font.width(keybind);
        int textX = x + (ICON_SIZE - textWidth) / 2;
        int textY = y + 2;
        guiGraphics.drawString(mc.font, keybind, textX, textY, textColor, false);
        
        // Draw cooldown overlay if on cooldown
        if (cooldown > 0) {
            // Semi-transparent red overlay
            int cooldownSeconds = (cooldown + 19) / 20; // Round up
            
            // Draw cooldown number
            String cdText = String.valueOf(cooldownSeconds);
            int cdWidth = mc.font.width(cdText);
            int cdX = x + (ICON_SIZE - cdWidth) / 2;
            int cdY = y + ICON_SIZE - 10;
            guiGraphics.drawString(mc.font, cdText, cdX, cdY, 0xFFFFAA00, false);
        }
        
        // Draw mana cost below if not enough mana
        if (cooldown <= 0 && mana < manaCost) {
            String manaText = String.valueOf(manaCost);
            int manaWidth = mc.font.width(manaText);
            int manaX = x + (ICON_SIZE - manaWidth) / 2;
            int manaY = y + ICON_SIZE - 10;
            guiGraphics.drawString(mc.font, manaText, manaX, manaY, 0xFF5555FF, false);
        }
    }
}
}
