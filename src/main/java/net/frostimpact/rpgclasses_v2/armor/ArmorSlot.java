package net.frostimpact.rpgclasses_v2.armor;

/**
 * Enum defining armor slot types.
 */
public enum ArmorSlot {
    HELMET("Helmet"),
    CHESTPLATE("Chestplate"),
    LEGGINGS("Leggings"),
    BOOTS("Boots");
    
    private final String displayName;
    
    ArmorSlot(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
