package net.frostimpact.rpgclasses_v2.weapon;

/**
 * Enum defining weapon rarity levels.
 * Higher rarity typically means better stats and more abilities.
 */
public enum WeaponRarity {
    COMMON("Common", 0xFFFFFF, 1.0f),           // White
    UNCOMMON("Uncommon", 0x00FF00, 1.1f),       // Green
    RARE("Rare", 0x0000FF, 1.25f),              // Blue
    EPIC("Epic", 0x9400D3, 1.5f),               // Purple
    LEGENDARY("Legendary", 0xFFD700, 2.0f),    // Gold
    MYTHIC("Mythic", 0xFF4500, 2.5f);          // Orange-Red
    
    private final String displayName;
    private final int color;
    private final float statMultiplier;
    
    WeaponRarity(String displayName, int color, float statMultiplier) {
        this.displayName = displayName;
        this.color = color;
        this.statMultiplier = statMultiplier;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getColor() {
        return color;
    }
    
    /**
     * Multiplier applied to base weapon stats
     */
    public float getStatMultiplier() {
        return statMultiplier;
    }
}
