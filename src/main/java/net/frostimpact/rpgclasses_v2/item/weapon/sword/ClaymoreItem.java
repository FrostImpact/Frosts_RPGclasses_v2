package net.frostimpact.rpgclasses_v2.item.weapon.sword;

import net.frostimpact.rpgclasses_v2.item.weapon.MeleeWeaponItem;
import net.frostimpact.rpgclasses_v2.item.weapon.WeaponStats;
import net.frostimpact.rpgclasses_v2.item.weapon.WeaponType;
import net.minecraft.world.item.Tier;

/**
 * Claymore weapon - heavy, 4-hit combo with AOE spin finisher
 */
public class ClaymoreItem extends MeleeWeaponItem {

    public ClaymoreItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        this(tier, attackDamage, attackSpeed, WeaponStats.empty(), properties);
    }

    public ClaymoreItem(Tier tier, Properties properties) {
        this(tier, 4.0f, -3.0f, WeaponStats.empty(), properties);
    }

    public ClaymoreItem(Tier tier, WeaponStats weaponStats, Properties properties) {
        this(tier, 4.0f, -3.0f, weaponStats, properties);
    }

    public ClaymoreItem(Tier tier, float attackDamage, float attackSpeed, WeaponStats weaponStats, Properties properties) {
        super(tier, attackDamage, attackSpeed, weaponStats, properties);
    }
    
    @Override
    public WeaponType getWeaponType() {
        return WeaponType.CLAYMORE;
    }
}