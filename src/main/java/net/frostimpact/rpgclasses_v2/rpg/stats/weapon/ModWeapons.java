package net.frostimpact.rpgclasses_v2.rpg.stats.weapon;

import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.frostimpact.rpgclasses_v2.rpg.stats.weapon.WeaponStatHandler;

/**
 * Registry for weapon items
 */
public class ModWeapons {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RpgClassesMod.MOD_ID);
    
    // Register sword items with different tiers
    public static final DeferredHolder<Item, Item> IRON_RPG_SWORD = ITEMS.register("iron_rpg_sword",
        () -> new SwordItem(Tiers.IRON,WeaponStats.builder()
                .damage(0.0)           // Change this number!
                .attackSpeed(0.0)      // Change this number!
                .moveSpeed(0.0)         // Change this number!
                .defense(0)          // Add new stats!
                .maxHealth(0)        // Any stat you want!
                .build(), new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> DIAMOND_RPG_SWORD = ITEMS.register("diamond_rpg_sword",
        () -> new SwordItem(Tiers.DIAMOND, new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> NETHERITE_RPG_SWORD = ITEMS.register("netherite_rpg_sword",
        () -> new SwordItem(Tiers.NETHERITE, new Item.Properties()));
    
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
