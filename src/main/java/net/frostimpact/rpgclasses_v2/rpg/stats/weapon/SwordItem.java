package net.frostimpact.rpgclasses_v2.rpg.stats.weapon;

import net.minecraft.world.item.Tier;


/**
 * Sword weapon implementation with combo attack system
 */
public class SwordItem extends MeleeWeaponItem {

    public SwordItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        this(tier, attackDamage, attackSpeed, WeaponStats.empty(), properties);
    }

    public SwordItem(Tier tier, Properties properties) {
        this(tier, 3.0f, -2.4f, WeaponStats.empty(), properties);
    }

    public SwordItem(Tier tier, WeaponStats weaponStats, Properties properties) {
        this(tier, 3.0f, -2.4f, weaponStats, properties);
    }

    public SwordItem(Tier tier, float attackDamage, float attackSpeed, WeaponStats weaponStats, Properties properties) {
        super(tier, attackDamage, attackSpeed, weaponStats, properties);
    }
}