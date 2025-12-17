package net.frostimpact.rpgclasses_v2.rpg.stats.weapon;

import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.frostimpact.rpgclasses_v2.rpg.stats.weapon.sword.ClaymoreItem;
import net.frostimpact.rpgclasses_v2.rpg.stats.weapon.sword.LongswordItem;
import net.frostimpact.rpgclasses_v2.rpg.stats.weapon.sword.ShortswordItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for all weapon items
 */
public class ModWeapons {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RpgClassesMod.MOD_ID);

    // ===== SHORTSWORDS (Fast, 3-hit combo) =====
    public static final DeferredHolder<Item, Item> IRON_SHORTSWORD = ITEMS.register("iron_shortsword",
            () -> new ShortswordItem(Tiers.IRON, WeaponStats.builder()
                    .attackSpeed(5.0)      // Faster attacks
                    .moveSpeed(5.0)        // More mobile
                    .build(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DIAMOND_SHORTSWORD = ITEMS.register("diamond_shortsword",
            () -> new ShortswordItem(Tiers.DIAMOND, WeaponStats.builder()
                    .attackSpeed(7.0)
                    .moveSpeed(7.0)
                    .build(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> NETHERITE_SHORTSWORD = ITEMS.register("netherite_shortsword",
            () -> new ShortswordItem(Tiers.NETHERITE, WeaponStats.builder()
                    .attackSpeed(10.0)
                    .moveSpeed(10.0)
                    .build(), new Item.Properties()));

    // ===== LONGSWORDS (Balanced, 4-hit combo) =====
    public static final DeferredHolder<Item, Item> IRON_LONGSWORD = ITEMS.register("iron_longsword",
            () -> new LongswordItem(Tiers.IRON, WeaponStats.builder()
                    .damage(5.0)
                    .build(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DIAMOND_LONGSWORD = ITEMS.register("diamond_longsword",
            () -> new LongswordItem(Tiers.DIAMOND, WeaponStats.builder()
                    .damage(8.0)
                    .build(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> NETHERITE_LONGSWORD = ITEMS.register("netherite_longsword",
            () -> new LongswordItem(Tiers.NETHERITE, WeaponStats.builder()
                    .damage(12.0)
                    .build(), new Item.Properties()));

    // ===== CLAYMORES (Heavy, 4-hit combo with AOE finisher) =====
    public static final DeferredHolder<Item, Item> IRON_CLAYMORE = ITEMS.register("iron_claymore",
            () -> new ClaymoreItem(Tiers.IRON, WeaponStats.builder()
                    .damage(10.0)          // Higher damage
                    .maxHealth(10.0)       // More tanky
                    .moveSpeed(-5.0)       // Slower movement
                    .build(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DIAMOND_CLAYMORE = ITEMS.register("diamond_claymore",
            () -> new ClaymoreItem(Tiers.DIAMOND, WeaponStats.builder()
                    .damage(15.0)
                    .maxHealth(15.0)
                    .moveSpeed(-5.0)
                    .build(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> NETHERITE_CLAYMORE = ITEMS.register("netherite_claymore",
            () -> new ClaymoreItem(Tiers.NETHERITE, WeaponStats.builder()
                    .damage(20.0)
                    .maxHealth(20.0)
                    .moveSpeed(-5.0)
                    .build(), new Item.Properties()));

    // ===== LEGACY (Keep old names for compatibility) =====
    public static final DeferredHolder<Item, Item> IRON_RPG_SWORD = ITEMS.register("iron_rpg_sword",
            () -> new LongswordItem(Tiers.IRON, new Item.Properties()));

    public static final DeferredHolder<Item, Item> DIAMOND_RPG_SWORD = ITEMS.register("diamond_rpg_sword",
            () -> new LongswordItem(Tiers.DIAMOND, new Item.Properties()));

    public static final DeferredHolder<Item, Item> NETHERITE_RPG_SWORD = ITEMS.register("netherite_rpg_sword",
            () -> new LongswordItem(Tiers.NETHERITE, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}