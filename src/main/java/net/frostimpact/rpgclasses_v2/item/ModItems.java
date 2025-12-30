package net.frostimpact.rpgclasses_v2.item;

import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(Registries.ITEM, RpgClassesMod.MOD_ID);

    public static final DeferredHolder<Item, Item> CLASS_SELECTION_BOOK = ITEMS.register(
        "class_selection_book",
        () -> new ClassSelectionBookItem(new Item.Properties()
            .stacksTo(1))
    );
    
    public static final DeferredHolder<Item, Item> MANA_RESTORATION_CRYSTAL = ITEMS.register(
        "mana_restoration_crystal",
        () -> new ManaRestorationItem(new Item.Properties()
            .stacksTo(16))
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
