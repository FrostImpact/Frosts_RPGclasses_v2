package net.frostimpact.rpgclasses_v2.rpgclass;

/**
 * Utility class for ability-related data and class display information
 */
public final class AbilityUtils {
    
    private AbilityUtils() {
        // Prevent instantiation
    }
    
    /**
     * Get the display name for an ability
     */
    public static String getAbilityName(String classId, int slot) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> switch (slot) {
                case 1 -> "Power Strike";
                case 2 -> "Battle Cry";
                case 3 -> "Whirlwind";
                case 4 -> "Berserker Rage";
                default -> "Unknown";
            };
            case "mage" -> switch (slot) {
                case 1 -> "Fireball";
                case 2 -> "Frost Nova";
                case 3 -> "Arcane Shield";
                case 4 -> "Meteor Storm";
                default -> "Unknown";
            };
            case "rogue" -> switch (slot) {
                case 1 -> "Backstab";
                case 2 -> "Smoke Bomb";
                case 3 -> "Fan of Knives";
                case 4 -> "Shadow Dance";
                default -> "Unknown";
            };
            case "ranger" -> switch (slot) {
                case 1 -> "Energy Beam";
                case 2 -> "Spread Shot";
                case 3 -> "Escape";
                case 4 -> "Arrow Rain";
                default -> "Unknown";
            };
            case "tank" -> switch (slot) {
                case 1 -> "Shield Bash";
                case 2 -> "Taunt";
                case 3 -> "Iron Skin";
                case 4 -> "Fortress";
                default -> "Unknown";
            };
            case "priest" -> switch (slot) {
                case 1 -> "Holy Light";
                case 2 -> "Blessing";
                case 3 -> "Smite";
                case 4 -> "Divine Intervention";
                default -> "Unknown";
            };
            case "hawkeye" -> switch (slot) {
                case 1 -> "Glide";
                case 2 -> "Updraft";
                case 3 -> "Vault";
                case 4 -> "Seekers";
                default -> "Unknown";
            };
            case "marksman" -> switch (slot) {
                case 1 -> "Sniper Shot";
                case 2 -> "Railgun";
                case 3 -> "Hunter's Mark";
                case 4 -> "Execution";
                default -> "Unknown";
            };
            case "beastmaster" -> switch (slot) {
                case 1 -> "Summon Wolves";
                case 2 -> "Summon Bear";
                case 3 -> "Summon Eagle";
                case 4 -> "Beast Stampede";
                default -> "Unknown";
            };
            default -> "Ability " + slot;
        };
    }
    
    /**
     * Get the mana cost for an ability
     */
    public static int getAbilityManaCost(String classId, int slot) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> switch (slot) {
                case 1 -> 20;
                case 2 -> 30;
                case 3 -> 40;
                case 4 -> 60;
                default -> 0;
            };
            case "mage" -> switch (slot) {
                case 1 -> 25;
                case 2 -> 35;
                case 3 -> 40;
                case 4 -> 80;
                default -> 0;
            };
            case "rogue" -> switch (slot) {
                case 1 -> 15;
                case 2 -> 20;
                case 3 -> 30;
                case 4 -> 50;
                default -> 0;
            };
            case "ranger" -> switch (slot) {
                case 1 -> 20;  // Precise Shot - increased for dramatic effect
                case 2 -> 30;  // Multi-Shot - shoots 6 actual arrows
                case 3 -> 20;  // Escape
                case 4 -> 80;  // Rain of Arrows - 6 second duration
                default -> 0;
            };
            case "tank" -> switch (slot) {
                case 1 -> 15;
                case 2 -> 10;
                case 3 -> 25;
                case 4 -> 40;
                default -> 0;
            };
            case "priest" -> switch (slot) {
                case 1 -> 30;
                case 2 -> 25;
                case 3 -> 35;
                case 4 -> 80;
                default -> 0;
            };
            case "hawkeye" -> switch (slot) {
                case 1 -> 10;  // Glide - low cost
                case 2 -> 15;  // Updraft
                case 3 -> 20;  // Vault - lobbing scute projectile
                case 4 -> 0;   // Seekers - cost depends on charges (5 * charges)
                default -> 0;
            };
            case "marksman" -> switch (slot) {
                case 1 -> 15;  // Steady Shot
                case 2 -> 25;  // Piercing Shot
                case 3 -> 20;  // Mark Target
                case 4 -> 50;  // Headshot (ultimate single target)
                default -> 0;
            };
            case "beastmaster" -> switch (slot) {
                case 1 -> 25;  // Wolf Pack
                case 2 -> 30;  // Bear Strength
                case 3 -> 20;  // Eagle Eye
                case 4 -> 60;  // Stampede
                default -> 0;
            };
            default -> 0;
        };
    }
    
    /**
     * Get the cooldown in ticks (20 ticks = 1 second) for an ability
     */
    public static int getAbilityCooldownTicks(String classId, int slot) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> switch (slot) {
                case 1 -> 60;   // 3s
                case 2 -> 300;  // 15s
                case 3 -> 160;  // 8s
                case 4 -> 900;  // 45s
                default -> 40;
            };
            case "mage" -> switch (slot) {
                case 1 -> 80;   // 4s
                case 2 -> 200;  // 10s
                case 3 -> 400;  // 20s
                case 4 -> 1200; // 60s
                default -> 40;
            };
            case "rogue" -> switch (slot) {
                case 1 -> 100;  // 5s
                case 2 -> 240;  // 12s
                case 3 -> 160;  // 8s
                case 4 -> 900;  // 45s
                default -> 40;
            };
            case "ranger" -> switch (slot) {
                case 1 -> 100;  // 5s - Precise Shot (charge time effect)
                case 2 -> 160;  // 8s - Multi-Shot
                case 3 -> 300;  // 15s - Escape
                case 4 -> 1200; // 60s - Rain of Arrows (6 second duration ultimate)
                default -> 40;
            };
            case "tank" -> switch (slot) {
                case 1 -> 120;  // 6s
                case 2 -> 200;  // 10s
                case 3 -> 400;  // 20s
                case 4 -> 1200; // 60s
                default -> 40;
            };
            case "priest" -> switch (slot) {
                case 1 -> 60;   // 3s
                case 2 -> 300;  // 15s
                case 3 -> 160;  // 8s
                case 4 -> 1800; // 90s
                default -> 40;
            };
            case "hawkeye" -> switch (slot) {
                case 1 -> 40;   // 2s - Glide
                case 2 -> 240;  // 12s - Updraft
                case 3 -> 160;  // 8s - Vault
                case 4 -> 100;  // 5s - Seekers
                default -> 40;
            };
            case "marksman" -> switch (slot) {
                case 1 -> 60;   // 3s - Steady Shot
                case 2 -> 120;  // 6s - Piercing Shot
                case 3 -> 400;  // 20s - Mark Target
                case 4 -> 600;  // 30s - Headshot
                default -> 40;
            };
            case "beastmaster" -> switch (slot) {
                case 1 -> 200;  // 10s - Wolf Pack
                case 2 -> 400;  // 20s - Bear Strength
                case 3 -> 300;  // 15s - Eagle Eye
                case 4 -> 900;  // 45s - Stampede
                default -> 40;
            };
            default -> 40;
        };
    }
    
    /**
     * Get the keybind character for an ability slot
     */
    public static String getAbilityKeybind(int slot) {
        return switch (slot) {
            case 1 -> "Z";
            case 2 -> "X";
            case 3 -> "C";
            case 4 -> "V";
            default -> "?";
        };
    }
    
    /**
     * Get the icon for a class
     */
    public static String getClassIcon(String classId) {
        if (classId == null) return "⭐";
        return switch (classId.toLowerCase()) {
            case "warrior" -> "⚔";
            case "mage" -> "✨";
            case "rogue" -> "🗡";
            case "ranger" -> "🏹";
            case "tank" -> "🛡";
            case "priest" -> "❤";
            case "berserker" -> "💢";
            case "paladin" -> "✝";
            case "pyromancer" -> "🔥";
            case "frostmage" -> "❄";
            case "assassin" -> "☠";
            case "shadowdancer" -> "👤";
            case "hawkeye" -> "👁";
            case "marksman" -> "🎯";
            case "beastmaster" -> "🐺";
            case "guardian" -> "🏰";
            case "juggernaut" -> "💪";
            case "cleric" -> "💚";
            case "templar" -> "⚡";
            default -> "⭐";
        };
    }
    
    /**
     * Get the display color for a class
     */
    public static int getClassColor(String classId) {
        if (classId == null) return 0xAAAAAA;
        return switch (classId.toLowerCase()) {
            case "warrior", "berserker" -> 0xFF4444;
            case "paladin" -> 0xFFDD44;
            case "mage", "pyromancer", "frostmage" -> 0xAA00FF;
            case "rogue", "assassin", "shadowdancer" -> 0x55FF55;
            case "ranger", "hawkeye", "marksman", "beastmaster" -> 0x88DD44;
            case "tank", "guardian", "juggernaut" -> 0x55AAFF;
            case "priest", "cleric", "templar" -> 0xFFAA00;
            default -> 0xAAAAAA;
        };
    }
}
