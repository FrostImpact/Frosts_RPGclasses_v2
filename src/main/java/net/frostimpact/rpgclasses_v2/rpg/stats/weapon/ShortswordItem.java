package net.frostimpact.rpgclasses_v2.rpg.stats.weapon;

import net.minecraft.world.item.Tier;

/**
 * Shortsword weapon - fast, 3-hit combo with dual slash finisher
 */
public class ShortswordItem extends MeleeWeaponItem {

    public ShortswordItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        this(tier, attackDamage, attackSpeed, WeaponStats.empty(), properties);
    }

    public ShortswordItem(Tier tier, Properties properties) {
        this(tier, 2.5f, -2.0f, WeaponStats.empty(), properties);
    }

    public ShortswordItem(Tier tier, WeaponStats weaponStats, Properties properties) {
        this(tier, 2.5f, -2.0f, weaponStats, properties);
    }

    public ShortswordItem(Tier tier, float attackDamage, float attackSpeed, WeaponStats weaponStats, Properties properties) {
        super(tier, attackDamage, attackSpeed, weaponStats, properties);
    }
    
    @Override
    public WeaponType getWeaponType() {
        return WeaponType.SHORTSWORD;
    }
}