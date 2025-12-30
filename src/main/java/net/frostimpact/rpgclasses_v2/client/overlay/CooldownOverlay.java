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
    private static final int ICON_SIZE = 22;
    private static final int ICON_SPACING = 3;
    private static final int MARGIN_BOTTOM = 38; // Above hotbar level
    private static final int BORDER_WIDTH = 1;
    private static final int INNER_SIZE = ICON_SIZE - (BORDER_WIDTH * 2);
    
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
        int maxCooldown = AbilityUtils.getAbilityCooldownTicks(currentClass, slot);
        int mana = rpgData.getMana();
        int manaCost = AbilityUtils.getAbilityManaCost(currentClass, slot);
        
        // Background color - lighter base
        int bgColor = 0xCC2A2A2A;
        guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);
        
        // Border color based on state - gray shades
        int borderColor;
        if (cooldown > 0) {
            borderColor = 0xFF555555; // Dark gray when on cooldown
        } else if (mana < manaCost) {
            borderColor = 0xFF666688; // Slightly blue-gray when not enough mana
        } else {
            borderColor = 0xFFAAAAAA; // Light gray when ready
        }
        
        // Draw border (1px thick)
        guiGraphics.fill(x, y, x + ICON_SIZE, y + BORDER_WIDTH, borderColor);
        guiGraphics.fill(x, y + ICON_SIZE - BORDER_WIDTH, x + ICON_SIZE, y + ICON_SIZE, borderColor);
        guiGraphics.fill(x, y + BORDER_WIDTH, x + BORDER_WIDTH, y + ICON_SIZE - BORDER_WIDTH, borderColor);
        guiGraphics.fill(x + ICON_SIZE - BORDER_WIDTH, y + BORDER_WIDTH, x + ICON_SIZE, y + ICON_SIZE - BORDER_WIDTH, borderColor);
        
        // Draw cooldown overlay if on cooldown - dark gray fill that ticks down from top to bottom
        if (cooldown > 0 && maxCooldown > 0) {
            // Calculate the fill percentage (1.0 = full cooldown, 0.0 = ready)
            float cooldownProgress = (float) cooldown / maxCooldown;
            // Fill height from top - decreases as cooldown progresses
            int fillHeight = (int) (INNER_SIZE * cooldownProgress);
            if (fillHeight > 0) {
                // Dark gray cooldown overlay - fills from top and shrinks downward
                int cooldownColor = 0xDD333333;
                guiGraphics.fill(x + BORDER_WIDTH, y + BORDER_WIDTH, 
                                 x + ICON_SIZE - BORDER_WIDTH, y + BORDER_WIDTH + fillHeight, 
                                 cooldownColor);
            }
        }
        
        // Get ability icon
        String abilityIcon = getAbilityIcon(currentClass, slot);
        
        // Draw ability icon centered vertically and horizontally in the box
        int iconColor = cooldown > 0 ? 0xFF888888 : (mana < manaCost ? 0xFF8888AA : 0xFFFFFFFF);
        int iconWidth = mc.font.width(abilityIcon);
        int iconX = x + (ICON_SIZE - iconWidth) / 2;
        int iconY = y + (ICON_SIZE - 8) / 2 - 2; // Center vertically, accounting for font height (~8px)
        guiGraphics.drawString(mc.font, abilityIcon, iconX, iconY, iconColor, false);
        
        // Keybind letter at bottom - ensure it fits inside the box
        String keybind = AbilityUtils.getAbilityKeybind(slot);
        int textColor = cooldown > 0 ? 0xFF777777 : (mana < manaCost ? 0xFF8888BB : 0xFFCCCCCC);
        int textWidth = mc.font.width(keybind);
        int textX = x + (ICON_SIZE - textWidth) / 2;
        int textY = y + ICON_SIZE - 9; // Position inside bottom of box
        guiGraphics.drawString(mc.font, keybind, textX, textY, textColor, false);
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
                case 3 -> "💨"; // Escape
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
            case "hawkeye" -> switch (slot) {
                case 1 -> "🪶"; // Glide
                case 2 -> "↑"; // Updraft
                case 3 -> "➤"; // Vault
                case 4 -> "◎"; // Seekers
                default -> "⭐";
            };
            case "marksman" -> switch (slot) {
                case 1 -> "⊙"; // Steady Shot
                case 2 -> "↠"; // Piercing Shot
                case 3 -> "✖"; // Mark Target
                case 4 -> "☠"; // Headshot
                default -> "⭐";
            };
            case "beastmaster" -> switch (slot) {
                case 1 -> "🐺"; // Wolf Pack
                case 2 -> "🐻"; // Bear Strength
                case 3 -> "🦅"; // Eagle Eye
                case 4 -> "🐗"; // Stampede
                default -> "⭐";
            };
            default -> "⭐";
        };
    }
}
