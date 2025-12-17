package net.frostimpact.rpgclasses_v2.rpg.stats.weapon;

import net.minecraft.world.item.Tier;

/**
 * Legacy SwordItem - now just extends LongswordItem for backwards compatibility
 * @deprecated Use LongswordItem instead
 */
@Deprecated
public class SwordItem extends LongswordItem {

    public SwordItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    public SwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    public SwordItem(Tier tier, WeaponStats weaponStats, Properties properties) {
        super(tier, weaponStats, properties);
    }

    public SwordItem(Tier tier, float attackDamage, float attackSpeed, WeaponStats weaponStats, Properties properties) {
        super(tier, attackDamage, attackSpeed, weaponStats, properties);
    }
}