package net.frostimpact.rpgclasses_v2.item.weapon;

/**
 * Enum defining different weapon types with their combat properties
 */
public enum WeaponType {
    SHORTSWORD(4, 0.6f),  // 4-hit combo (updated), faster attacks
    LONGSWORD(4, 1.0f),   // 4-hit combo, balanced
    CLAYMORE(4, 1.4f);    // 4-hit combo, slower but heavier
    
    private final int maxComboCount;
    private final float speedMultiplier;
    
    WeaponType(int maxComboCount, float speedMultiplier) {
        this.maxComboCount = maxComboCount;
        this.speedMultiplier = speedMultiplier;
    }
    
    public int getMaxComboCount() {
        return maxComboCount;
    }
    
    public float getSpeedMultiplier() {
        return speedMultiplier;
    }
}