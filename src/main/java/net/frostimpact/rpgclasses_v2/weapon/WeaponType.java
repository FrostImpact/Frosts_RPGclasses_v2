package net.frostimpact.rpgclasses_v2.weapon;

/**
 * Enum defining different weapon types
 */
public enum WeaponType {
    SHORTSWORD("Shortsword"),
    LONGSWORD("Longsword"),
    CLAYMORE("Claymore"),
    DAGGER("Dagger"),
    SPEAR("Spear"),
    AXE("Axe"),
    HAMMER("Hammer"),
    BOW("Bow"),
    STAFF("Staff"),
    WAND("Wand");
    
    private final String displayName;
    
    WeaponType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
