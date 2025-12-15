package net.frostimpact.rpgclasses_v2;

import net.frostimpact.rpgclasses_v2.client.overlay.HealthBarOverlay;
import net.frostimpact.rpgclasses_v2.client.overlay.ManaBarOverlay;
import net.frostimpact.rpgclasses_v2.event.ServerEvents;
import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.minecraft.client.gui.LayeredDraw;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@Mod("rpgclasses_v2")
public class RpgClassesMod {
    public static final String MOD_ID = "rpgclasses_v2";

    public RpgClassesMod(IEventBus modEventBus) {
        // Register attachments
        ModAttachments.register(modEventBus);

        // Register networking
        ModMessages.register(modEventBus);

        // Register client-side overlays only on client
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::registerOverlays);
        }

        // Register server events
        NeoForge.EVENT_BUS.register(new ServerEvents());
    }

    private void registerOverlays(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, 
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "health_bar"),
            new HealthBarOverlay());
        
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "mana_bar"),
            new ManaBarOverlay());
    }
}
