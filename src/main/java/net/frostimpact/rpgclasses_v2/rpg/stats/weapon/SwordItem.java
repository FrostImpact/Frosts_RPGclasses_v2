package net.frostimpact.rpgclasses_v2.rpg.stats.weapon;

import net.minecraft.world.item.Tier;

/**
 * Sword weapon implementation with combo attack system
 */
public class SwordItem extends MeleeWeaponItem {
    
    public SwordItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }
    
    public SwordItem(Tier tier, Properties properties) {
        this(tier, 3.0f, -2.4f, properties); // Default sword stats
    }
}
