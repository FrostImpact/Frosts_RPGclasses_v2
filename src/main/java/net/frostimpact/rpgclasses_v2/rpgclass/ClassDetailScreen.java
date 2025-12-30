package net.frostimpact.rpgclasses_v2.rpgclass;

import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSelectClass;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * In-depth screen showing class details, abilities, and stats
 */
public class ClassDetailScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassDetailScreen.class);
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 350;
    private static final int ABILITY_CARD_HEIGHT = 60;
    private static final int ABILITY_CARD_SPACING = 8;
    
    private final RPGClass rpgClass;
    private final Screen parentScreen;
    private String currentClass = "NONE";
    
    public ClassDetailScreen(RPGClass rpgClass, Screen parentScreen) {
        super(Component.literal(rpgClass.getName()));
        this.rpgClass = rpgClass;
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        if (Minecraft.getInstance().player != null) {
            PlayerRPGData rpgData = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_RPG);
            currentClass = rpgData.getCurrentClass();
        }
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Override to prevent default blur
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Solid dark translucent background
        graphics.fill(0, 0, this.width, this.height, 0xC0000000);
        
        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = 30;
        
        // Draw main panel background
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE0101010);
        
        // Draw border with class color
        int classColor = getClassColor(rpgClass.getId());
        drawBorder(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, classColor | 0xFF000000);
        
        int contentY = panelY + 10;
        
        // Back button at top
        int backBtnWidth = 60;
        int backBtnHeight = 20;
        int backBtnX = panelX + 10;
        int backBtnY = contentY;
        boolean backHovered = mouseX >= backBtnX && mouseX <= backBtnX + backBtnWidth &&
                mouseY >= backBtnY && mouseY <= backBtnY + backBtnHeight;
        
        graphics.fill(backBtnX, backBtnY, backBtnX + backBtnWidth, backBtnY + backBtnHeight,
                backHovered ? 0x80404040 : 0x80202020);
        drawBorder(graphics, backBtnX, backBtnY, backBtnWidth, backBtnHeight,
                backHovered ? 0xFFAAAAAA : 0xFF666666);
        graphics.drawCenteredString(this.font, "← Back", backBtnX + backBtnWidth / 2, backBtnY + 6,
                backHovered ? 0xFFFFFF : 0xAAAAAA);
        
        contentY += 30;
        
        // Class title with icon
        String icon = getClassIcon(rpgClass.getId());
        String title = icon + " " + rpgClass.getName().toUpperCase();
        graphics.drawCenteredString(this.font, "§l" + title, centerX, contentY, classColor);
        contentY += 20;
        
        // Description
        List<String> descLines = wrapText(rpgClass.getDescription(), PANEL_WIDTH - 40);
        for (String line : descLines) {
            graphics.drawCenteredString(this.font, "§7" + line, centerX, contentY, 0x888888);
            contentY += 10;
        }
        contentY += 15;
        
        // ═══════════ BASE STATS SECTION ═══════════
        graphics.drawString(this.font, "§6━━━━━ Base Stats ━━━━━", panelX + 15, contentY, 0xFFAA00, false);
        contentY += 15;
        
        var baseStats = rpgClass.getAllBaseStats();
        if (!baseStats.isEmpty()) {
            int statsPerRow = 2;
            int statWidth = (PANEL_WIDTH - 40) / statsPerRow;
            int statIndex = 0;
            
            for (Map.Entry<StatType, List<net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier>> entry : baseStats.entrySet()) {
                double totalValue = entry.getValue().stream()
                        .mapToDouble(mod -> mod.getValue())
                        .sum();
                
                String statName = formatStatName(entry.getKey().name());
                String statText = getStatIcon(entry.getKey()) + " " + statName + ": +" +
                        (entry.getKey().isPercentage() ?
                                String.format("%.1f%%", totalValue) :
                                String.format("%.0f", totalValue));
                
                int statX = panelX + 20 + (statIndex % statsPerRow) * statWidth;
                int statY = contentY + (statIndex / statsPerRow) * 12;
                
                graphics.drawString(this.font, statText, statX, statY, getStatColor(entry.getKey()), false);
                statIndex++;
            }
            contentY += ((statIndex + statsPerRow - 1) / statsPerRow) * 12 + 10;
        } else {
            graphics.drawString(this.font, "§7  No base stat bonuses", panelX + 20, contentY, 0x777777, false);
            contentY += 15;
        }
        
        contentY += 10;
        
        // ═══════════ ABILITIES SECTION ═══════════
        graphics.drawString(this.font, "§b━━━━━ Class Abilities ━━━━━", panelX + 15, contentY, 0x55AAFF, false);
        contentY += 15;
        
        // Get abilities for this class
        List<ClassAbility> abilities = getClassAbilities(rpgClass.getId());
        
        if (abilities.isEmpty()) {
            graphics.drawString(this.font, "§7  Abilities coming soon...", panelX + 20, contentY, 0x777777, false);
            contentY += 15;
        } else {
            for (ClassAbility ability : abilities) {
                renderAbilityCard(graphics, ability, panelX + 15, contentY, PANEL_WIDTH - 30, mouseX, mouseY);
                contentY += ABILITY_CARD_HEIGHT + ABILITY_CARD_SPACING;
            }
        }
        
        // Select Class button at bottom
        int selectBtnWidth = 150;
        int selectBtnHeight = 30;
        int selectBtnX = centerX - selectBtnWidth / 2;
        int selectBtnY = panelY + PANEL_HEIGHT - 45;
        
        boolean isCurrent = rpgClass.getId().equalsIgnoreCase(currentClass);
        boolean selectHovered = !isCurrent && mouseX >= selectBtnX && mouseX <= selectBtnX + selectBtnWidth &&
                mouseY >= selectBtnY && mouseY <= selectBtnY + selectBtnHeight;
        
        int btnBgColor = isCurrent ? 0xFF333333 : (selectHovered ? 0xFF006600 : 0xFF004400);
        graphics.fill(selectBtnX, selectBtnY, selectBtnX + selectBtnWidth, selectBtnY + selectBtnHeight, btnBgColor);
        
        int btnBorderColor = isCurrent ? 0xFF555555 : (selectHovered ? 0xFF00FF00 : 0xFF00AA00);
        drawBorder(graphics, selectBtnX, selectBtnY, selectBtnWidth, selectBtnHeight, btnBorderColor);
        
        String btnText = isCurrent ? "§8§lCURRENT CLASS" : (selectHovered ? "§a§lSELECT CLASS" : "§f§lSELECT CLASS");
        graphics.drawCenteredString(this.font, btnText, centerX, selectBtnY + 11, 0xFFFFFF);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderAbilityCard(GuiGraphics graphics, ClassAbility ability, int x, int y, int width, int mouseX, int mouseY) {
        int cardHeight = ABILITY_CARD_HEIGHT;
        
        // Card background
        boolean isHovered = mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + cardHeight;
        
        int bgColor = isHovered ? 0x60303030 : 0x40202020;
        graphics.fill(x, y, x + width, y + cardHeight, bgColor);
        
        // Card border
        int borderColor = getAbilityTypeColor(ability.type);
        drawBorder(graphics, x, y, width, cardHeight, borderColor);
        
        // Keybind indicator
        String keybind = "[" + ability.keybind + "]";
        int keybindWidth = this.font.width(keybind);
        graphics.fill(x + width - keybindWidth - 10, y + 2, x + width - 2, y + 14, 0x80000000);
        graphics.drawString(this.font, "§e" + keybind, x + width - keybindWidth - 6, y + 4, 0xFFFF55, false);
        
        // Ability name with type color
        String typePrefix = ability.type.equals("passive") ? "§d" : (ability.type.equals("ultimate") ? "§c" : "§b");
        graphics.drawString(this.font, typePrefix + "§l" + ability.name, x + 8, y + 5, 0xFFFFFF, false);
        
        // Ability type
        String typeText = "§7[" + ability.type.toUpperCase() + "]";
        graphics.drawString(this.font, typeText, x + 8 + this.font.width(ability.name) + 10, y + 5, 0x888888, false);
        
        // Description (wrapped)
        List<String> descLines = wrapText(ability.description, width - 20);
        int descY = y + 18;
        for (int i = 0; i < Math.min(descLines.size(), 2); i++) {
            graphics.drawString(this.font, "§7" + descLines.get(i), x + 8, descY + i * 10, 0xAAAAAA, false);
        }
        
        // Cooldown and mana cost
        String costText = "";
        if (ability.manaCost > 0) {
            costText += "§3Mana: " + ability.manaCost + "  ";
        }
        if (ability.cooldown > 0) {
            costText += "§6CD: " + ability.cooldown + "s";
        }
        if (!costText.isEmpty()) {
            graphics.drawString(this.font, costText, x + 8, y + cardHeight - 12, 0xAAAAAA, false);
        }
    }
    
    private int getAbilityTypeColor(String type) {
        return switch (type.toLowerCase()) {
            case "passive" -> 0xFFAA00FF;
            case "ultimate" -> 0xFFFF4444;
            case "active" -> 0xFF55AAFF;
            default -> 0xFF888888;
        };
    }
    
    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
    
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (this.font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) lines.add(currentLine.toString());
        return lines;
    }
    
    private String formatStatName(String statType) {
        return switch (statType) {
            case "MAX_HEALTH" -> "Max Health";
            case "MAX_MANA" -> "Max Mana";
            case "DAMAGE" -> "Damage";
            case "DEFENSE" -> "Defense";
            case "MOVE_SPEED" -> "Move Speed";
            case "ATTACK_SPEED" -> "Attack Speed";
            case "HEALTH_REGEN" -> "Health Regen";
            case "MANA_REGEN" -> "Mana Regen";
            case "COOLDOWN_REDUCTION" -> "CDR";
            default -> statType;
        };
    }
    
    private String getStatIcon(StatType statType) {
        return switch (statType) {
            case MAX_HEALTH -> "❤";
            case MAX_MANA -> "⚡";
            case DAMAGE -> "⚔";
            case DEFENSE -> "🛡";
            case MOVE_SPEED -> "👟";
            case ATTACK_SPEED -> "🗡";
            case HEALTH_REGEN -> "💚";
            case MANA_REGEN -> "💙";
            case COOLDOWN_REDUCTION -> "⏱";
        };
    }
    
    private int getStatColor(StatType statType) {
        return switch (statType) {
            case MAX_HEALTH -> 0xFFFF5555;
            case MAX_MANA -> 0xFF55FFFF;
            case DAMAGE -> 0xFFFFAA00;
            case DEFENSE -> 0xFF55AAFF;
            case MOVE_SPEED -> 0xFF55FF55;
            case ATTACK_SPEED -> 0xFFFF55FF;
            case HEALTH_REGEN -> 0xFFFF8888;
            case MANA_REGEN -> 0xFF88FFFF;
            case COOLDOWN_REDUCTION -> 0xFFAA55FF;
        };
    }
    
    private String getClassIcon(String classId) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> "⚔";
            case "mage" -> "✨";
            case "rogue" -> "🗡";
            case "ranger" -> "🏹";
            case "tank" -> "🛡";
            case "priest" -> "❤";
            case "hawkeye" -> "👁";
            case "marksman" -> "🎯";
            case "beastmaster" -> "🐺";
            default -> "⭐";
        };
    }
    
    private int getClassColor(String classId) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> 0xFF4444;
            case "mage" -> 0xAA00FF;
            case "rogue" -> 0x55FF55;
            case "ranger" -> 0x88DD44;
            case "tank" -> 0x55AAFF;
            case "priest" -> 0xFFAA00;
            case "hawkeye" -> 0x55DDAA;
            case "marksman" -> 0xFFCC44;
            case "beastmaster" -> 0xBB8844;
            default -> 0xAAAAAA;
        };
    }
    
    /**
     * Get the abilities for a given class
     */
    private List<ClassAbility> getClassAbilities(String classId) {
        List<ClassAbility> abilities = new ArrayList<>();
        
        switch (classId.toLowerCase()) {
            case "warrior" -> {
                abilities.add(new ClassAbility("Power Strike", "Z", "active",
                        "Deliver a powerful melee strike that deals 150% damage and briefly stuns the target.",
                        20, 3));
                abilities.add(new ClassAbility("Battle Cry", "X", "active",
                        "Let out a war cry that increases your damage by 25% and attack speed by 15% for 10 seconds.",
                        30, 15));
                abilities.add(new ClassAbility("Whirlwind", "C", "active",
                        "Spin in a circle dealing damage to all nearby enemies. Hits up to 5 times.",
                        40, 8));
                abilities.add(new ClassAbility("Berserker Rage", "V", "ultimate",
                        "Enter a rage state, gaining 50% damage and 30% attack speed but taking 20% more damage for 15 seconds.",
                        60, 45));
            }
            case "mage" -> {
                abilities.add(new ClassAbility("Fireball", "Z", "active",
                        "Launch a fireball that explodes on impact, dealing fire damage in an area.",
                        25, 4));
                abilities.add(new ClassAbility("Frost Nova", "X", "active",
                        "Release a burst of frost around you, slowing and damaging nearby enemies.",
                        35, 10));
                abilities.add(new ClassAbility("Arcane Shield", "C", "active",
                        "Create a magical barrier that absorbs damage equal to 50% of your max mana.",
                        40, 20));
                abilities.add(new ClassAbility("Meteor Storm", "V", "ultimate",
                        "Call down a storm of meteors in a large area, dealing massive fire damage.",
                        80, 60));
            }
            case "rogue" -> {
                abilities.add(new ClassAbility("Backstab", "Z", "active",
                        "Teleport behind your target and strike for 200% damage. Extra damage if target is unaware.",
                        15, 5));
                abilities.add(new ClassAbility("Smoke Bomb", "X", "active",
                        "Drop a smoke bomb that blinds enemies and grants you brief invisibility.",
                        20, 12));
                abilities.add(new ClassAbility("Fan of Knives", "C", "active",
                        "Throw knives in all directions, dealing damage and applying bleed to hit enemies.",
                        30, 8));
                abilities.add(new ClassAbility("Shadow Dance", "V", "ultimate",
                        "Enter the shadows, becoming invisible and gaining 100% crit chance for 8 seconds.",
                        50, 45));
            }
            case "ranger" -> {
                abilities.add(new ClassAbility("Precise Shot", "Z", "active",
                        "Fire a MASSIVE glowing arrow projectile (3x normal size) with radiating energy circles. Deals 175% damage and ignores 50% of armor.",
                        20, 5));
                abilities.add(new ClassAbility("Multi-Shot", "X", "active",
                        "Fire 6 arrows in a spread pattern from your position. Each arrow travels in the direction you're looking.",
                        30, 8));
                abilities.add(new ClassAbility("Escape", "C", "active",
                        "Launch yourself backward away from danger with a burst of speed.",
                        20, 15));
                abilities.add(new ClassAbility("Rain of Arrows", "V", "ultimate",
                        "Create a massive arrow storm that lasts 6 SECONDS. Continuous stream of particle arrows rain down dealing damage over time.",
                        80, 60));
            }
            case "tank" -> {
                abilities.add(new ClassAbility("Shield Bash", "Z", "active",
                        "Bash enemies with your shield, dealing damage and stunning for 1.5 seconds.",
                        15, 6));
                abilities.add(new ClassAbility("Taunt", "X", "active",
                        "Force nearby enemies to attack you for 5 seconds. Gain 30% damage reduction.",
                        10, 10));
                abilities.add(new ClassAbility("Iron Skin", "C", "active",
                        "Harden your skin, reducing all incoming damage by 50% for 6 seconds.",
                        25, 20));
                abilities.add(new ClassAbility("Fortress", "V", "ultimate",
                        "Become immovable. Gain 80% damage reduction and reflect 25% of damage taken for 10 seconds.",
                        40, 60));
            }
            case "priest" -> {
                abilities.add(new ClassAbility("Holy Light", "Z", "active",
                        "Heal yourself or an ally for a significant amount. Can also damage undead.",
                        30, 3));
                abilities.add(new ClassAbility("Blessing", "X", "active",
                        "Grant a blessing that increases max health by 20% and health regen for 30 seconds.",
                        25, 15));
                abilities.add(new ClassAbility("Smite", "C", "active",
                        "Call down holy wrath on an enemy, dealing holy damage and applying a damage debuff.",
                        35, 8));
                abilities.add(new ClassAbility("Divine Intervention", "V", "ultimate",
                        "Become immune to damage and heal all nearby allies to full health. Cannot attack during effect.",
                        80, 90));
            }
            case "hawkeye" -> {
                abilities.add(new ClassAbility("Glide", "Z", "active",
                        "Gain Slow Falling for 10 seconds, allowing you to float gracefully through the air with feather-like particles.",
                        10, 2));
                abilities.add(new ClassAbility("Updraft", "X", "active",
                        "Launch yourself high into the air with a powerful vertical boost. Grants slow falling and 1 Seeker charge.",
                        15, 12));
                abilities.add(new ClassAbility("Vault", "C", "active",
                        "Dash forward and lob a LARGE turtle scute projectile that deals significant impact damage. Grants 1 Seeker charge.",
                        20, 8));
                abilities.add(new ClassAbility("Seekers", "V", "ultimate",
                        "Release homing projectiles that track enemies in your vision. Medium-speed moving orbs that home in on targets. Consumes all Seeker charges.",
                        0, 5));
            }
            case "marksman" -> {
                abilities.add(new ClassAbility("Steady Shot", "Z", "active",
                        "Fire a high-velocity, perfectly accurate arrow with extended range. Deals massive single-target damage.",
                        15, 3));
                abilities.add(new ClassAbility("Piercing Shot", "X", "active",
                        "Fire an arrow that pierces through multiple enemies, damaging all targets in its path.",
                        25, 6));
                abilities.add(new ClassAbility("Mark Target", "C", "active",
                        "Mark an enemy in your sights, causing them to glow and take increased damage from your attacks.",
                        20, 20));
                abilities.add(new ClassAbility("Headshot", "V", "ultimate",
                        "Deliver a devastating critical hit to a single target, dealing MASSIVE damage. Shows dramatic impact effect.",
                        50, 30));
            }
            case "beastmaster" -> {
                abilities.add(new ClassAbility("Wolf Pack", "Z", "active",
                        "Command wolves to attack up to 3 nearby enemies, dealing damage with claw slash effects.",
                        25, 10));
                abilities.add(new ClassAbility("Bear Strength", "X", "active",
                        "Channel the power of a bear, gaining damage resistance and increased damage for a duration.",
                        30, 20));
                abilities.add(new ClassAbility("Eagle Eye", "C", "active",
                        "Activate enhanced vision, gaining night vision and revealing all enemies in a large radius with glowing effect.",
                        20, 15));
                abilities.add(new ClassAbility("Stampede", "V", "ultimate",
                        "Summon a stampede of beasts that charge forward, dealing damage and knocking back all enemies in their path.",
                        60, 45));
            }
            default -> {
                abilities.add(new ClassAbility("Basic Attack", "Z", "passive",
                        "A standard attack. Select a class to unlock special abilities!", 0, 0));
            }
        }
        
        return abilities;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = 30;
        
        // Back button check
        int backBtnWidth = 60;
        int backBtnHeight = 20;
        int backBtnX = panelX + 10;
        int backBtnY = panelY + 10;
        
        if (mouseX >= backBtnX && mouseX <= backBtnX + backBtnWidth &&
                mouseY >= backBtnY && mouseY <= backBtnY + backBtnHeight && button == 0) {
            Minecraft.getInstance().setScreen(parentScreen);
            return true;
        }
        
        // Select class button check
        int selectBtnWidth = 150;
        int selectBtnHeight = 30;
        int selectBtnX = centerX - selectBtnWidth / 2;
        int selectBtnY = panelY + PANEL_HEIGHT - 45;
        
        boolean isCurrent = rpgClass.getId().equalsIgnoreCase(currentClass);
        
        if (!isCurrent && mouseX >= selectBtnX && mouseX <= selectBtnX + selectBtnWidth &&
                mouseY >= selectBtnY && mouseY <= selectBtnY + selectBtnHeight && button == 0) {
            // Select this class
            LOGGER.info("Selecting class: {}", rpgClass.getId());
            ModMessages.sendToServer(new PacketSelectClass(rpgClass.getId()));
            this.onClose();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * Simple class ability data holder
     */
    private static class ClassAbility {
        final String name;
        final String keybind;
        final String type; // "active", "passive", "ultimate"
        final String description;
        final int manaCost;
        final int cooldown;
        
        ClassAbility(String name, String keybind, String type, String description, int manaCost, int cooldown) {
            this.name = name;
            this.keybind = keybind;
            this.type = type;
            this.description = description;
            this.manaCost = manaCost;
            this.cooldown = cooldown;
        }
    }
}
