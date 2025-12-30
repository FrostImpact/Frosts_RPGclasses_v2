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
    private static final int ICON_SIZE = 24;
    private static final int ICON_SPACING = 4;
    private static final int MARGIN_RIGHT = 5;
    private static final int MARGIN_BOTTOM = 40; // Above hotbar level
    
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
        
        // Background - darker when on cooldown
        int bgColor = cooldown > 0 ? 0xDD1A1A1A : 0xCC2A2A2A;
        guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);
        
        // Border color based on state - gray shades instead of red/green
        int borderColor;
        if (cooldown > 0) {
            borderColor = 0xFF555555; // Dark gray when on cooldown
        } else if (mana < manaCost) {
            borderColor = 0xFF666688; // Slightly blue-gray when not enough mana
        } else {
            borderColor = 0xFFAAAAAA; // Light gray when ready
        }
        
        // Draw border (2px thick)
        guiGraphics.fill(x, y, x + ICON_SIZE, y + 2, borderColor);
        guiGraphics.fill(x, y + ICON_SIZE - 2, x + ICON_SIZE, y + ICON_SIZE, borderColor);
        guiGraphics.fill(x, y + 2, x + 2, y + ICON_SIZE - 2, borderColor);
        guiGraphics.fill(x + ICON_SIZE - 2, y + 2, x + ICON_SIZE, y + ICON_SIZE - 2, borderColor);
        
        // Get ability icon
        String abilityIcon = getAbilityIcon(currentClass, slot);
        
        // Draw ability icon in center
        int iconColor = cooldown > 0 ? 0xFF666666 : (mana < manaCost ? 0xFF8888AA : 0xFFFFFFFF);
        int iconWidth = mc.font.width(abilityIcon);
        int iconX = x + (ICON_SIZE - iconWidth) / 2;
        int iconY = y + 3;
        guiGraphics.drawString(mc.font, abilityIcon, iconX, iconY, iconColor, false);
        
        // Keybind letter at bottom
        String keybind = AbilityUtils.getAbilityKeybind(slot);
        int textColor = cooldown > 0 ? 0xFF777777 : (mana < manaCost ? 0xFF8888BB : 0xFFCCCCCC);
        int textWidth = mc.font.width(keybind);
        int textX = x + (ICON_SIZE - textWidth) / 2;
        int textY = y + ICON_SIZE - 10;
        guiGraphics.drawString(mc.font, keybind, textX, textY, textColor, false);
        
        // Draw cooldown overlay if on cooldown
        if (cooldown > 0) {
            // Semi-transparent dark overlay
            int cooldownSeconds = (cooldown + 19) / 20; // Round up
            
            // Draw cooldown number in center-bottom area
            String cdText = String.valueOf(cooldownSeconds);
            int cdWidth = mc.font.width(cdText);
            int cdX = x + (ICON_SIZE - cdWidth) / 2;
            int cdY = y + ICON_SIZE - 10;
            guiGraphics.drawString(mc.font, cdText, cdX, cdY, 0xFFDDDDDD, false);
        }
        
        // Draw mana cost indicator if not enough mana (and not on cooldown)
        if (cooldown <= 0 && mana < manaCost) {
            String manaText = String.valueOf(manaCost);
            int manaWidth = mc.font.width(manaText);
            int manaX = x + (ICON_SIZE - manaWidth) / 2;
            int manaY = y + ICON_SIZE - 10;
            guiGraphics.drawString(mc.font, manaText, manaX, manaY, 0xFF8888DD, false);
        }
    }
    
    /**
     * Get the emoji icon for a specific ability
     */
    private String getAbilityIcon(String classId, int slot) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> switch (slot) {
                case 1 -> "⚔"; // Power Strike
                case 2 -> "📢"; // Battle Cry
                case 3 -> "🌀"; // Whirlwind
                case 4 -> "💢"; // Berserker Rage
                default -> "⭐";
            };
            case "mage" -> switch (slot) {
                case 1 -> "🔥"; // Fireball
                case 2 -> "❄"; // Frost Nova
                case 3 -> "💎"; // Arcane Shield
                case 4 -> "☄"; // Meteor Storm
                default -> "⭐";
            };
            case "rogue" -> switch (slot) {
                case 1 -> "🗡"; // Backstab
                case 2 -> "💨"; // Smoke Bomb
                case 3 -> "✦"; // Fan of Knives
                case 4 -> "👤"; // Shadow Dance
                default -> "⭐";
            };
            case "ranger" -> switch (slot) {
                case 1 -> "🎯"; // Precise Shot
                case 2 -> "🏹"; // Multi-Shot
                case 3 -> "⚠"; // Trap
                case 4 -> "☔"; // Rain of Arrows
                default -> "⭐";
            };
            case "tank" -> switch (slot) {
                case 1 -> "🛡"; // Shield Bash
                case 2 -> "😠"; // Taunt
                case 3 -> "💪"; // Iron Skin
                case 4 -> "🏰"; // Fortress
                default -> "⭐";
            };
            case "priest" -> switch (slot) {
                case 1 -> "✚"; // Holy Light
                case 2 -> "✨"; // Blessing
                case 3 -> "⚡"; // Smite
                case 4 -> "☀"; // Divine Intervention
                default -> "⭐";
            };
            default -> "⭐";
        };
    }
}
