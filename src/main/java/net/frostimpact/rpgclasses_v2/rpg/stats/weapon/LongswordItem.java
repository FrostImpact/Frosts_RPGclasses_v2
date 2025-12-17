package net.frostimpact.rpgclasses_v2.rpg.stats.weapon;

import net.minecraft.world.item.Tier;

/**
 * Longsword weapon - balanced, 4-hit combo with finisher
 */
public class LongswordItem extends MeleeWeaponItem {

    public LongswordItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        this(tier, attackDamage, attackSpeed, WeaponStats.empty(), properties);
    }

    public LongswordItem(Tier tier, Properties properties) {
        this(tier, 3.0f, -2.4f, WeaponStats.empty(), properties);
    }

    public LongswordItem(Tier tier, WeaponStats weaponStats, Properties properties) {
        this(tier, 3.0f, -2.4f, weaponStats, properties);
    }

    public LongswordItem(Tier tier, float attackDamage, float attackSpeed, WeaponStats weaponStats, Properties properties) {
        super(tier, attackDamage, attackSpeed, weaponStats, properties);
    }
    
    @Override
    public WeaponType getWeaponType() {
        return WeaponType.LONGSWORD;
    }
}