package net.frostimpact.rpgclasses_v2;

import net.frostimpact.rpgclasses_v2.client.ModKeybindings;
import net.frostimpact.rpgclasses_v2.client.overlay.CooldownOverlay;
import net.frostimpact.rpgclasses_v2.client.overlay.HealthBarOverlay;
import net.frostimpact.rpgclasses_v2.client.overlay.LevelDisplayOverlay;
import net.frostimpact.rpgclasses_v2.client.overlay.ManaBarOverlay;
import net.frostimpact.rpgclasses_v2.event.ServerEvents;
import net.frostimpact.rpgclasses_v2.item.ModItems;
import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatsDropdownOverlay;
import net.frostimpact.rpgclasses_v2.rpgclass.ClassRegistry;
import net.frostimpact.rpgclasses_v2.skilltree.SkillTreeRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@Mod("rpgclasses_v2")
public class RpgClassesMod {
    public static final String MOD_ID = "rpgclasses_v2";

    public RpgClassesMod(IEventBus modEventBus) {
        // Register items
        ModItems.register(modEventBus);

        // Register attachments
        ModAttachments.register(modEventBus);

        // Register networking
        ModMessages.register(modEventBus);
        
        // Register common setup
        modEventBus.addListener(this::commonSetup);
        
        // Register client-side overlays and keybindings only on client
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::registerOverlays);
            modEventBus.addListener(this::registerKeybindings);
        }

        // Register server events
        NeoForge.EVENT_BUS.register(new ServerEvents());
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize all registries
            SkillTreeRegistry.initializePlaceholderTrees();
            ClassRegistry.initializePlaceholderClasses();
        });
    }

    private void registerOverlays(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, 
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "health_bar"),
            new HealthBarOverlay());
        
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "mana_bar"),
            new ManaBarOverlay());
        
        event.registerAbove(VanillaGuiLayers.HOTBAR,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "stats_dropdown"),
            new StatsDropdownOverlay());
        
        event.registerAbove(VanillaGuiLayers.HOTBAR,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "level_display"),
            new LevelDisplayOverlay());
        
        event.registerAbove(VanillaGuiLayers.HOTBAR,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "cooldown_overlay"),
            new CooldownOverlay());
    }
    
    private void registerKeybindings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(ModKeybindings.TOGGLE_STATS);
        event.register(ModKeybindings.OPEN_STAT_ALLOCATION);
        event.register(ModKeybindings.ABILITY_1);
        event.register(ModKeybindings.ABILITY_2);
        event.register(ModKeybindings.ABILITY_3);
        event.register(ModKeybindings.ABILITY_4);
    }
}
