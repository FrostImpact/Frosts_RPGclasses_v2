package net.frostimpact.rpgclasses_v2.networking;

import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncStats;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModMessages::onRegisterPayloadHandler);
    }

    private static void onRegisterPayloadHandler(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("rpgclasses_v2");

        registrar.playToClient(
            PacketSyncMana.TYPE,
            PacketSyncMana.STREAM_CODEC,
            (packet, context) -> {
                context.enqueueWork(() -> {
                    if (context.player() != null) {
                        var rpgData = context.player().getData(ModAttachments.PLAYER_RPG);
                        rpgData.setMana(packet.mana());
                        rpgData.setMaxMana(packet.maxMana());
                    }
                });
            }
        );

        registrar.playToClient(
            PacketSyncStats.TYPE,
            PacketSyncStats.STREAM_CODEC,
            (packet, context) -> {
                context.enqueueWork(() -> {

                    if (context.player() != null) {
                        var stats = context.player().getData(ModAttachments.PLAYER_STATS);
                        stats.setModifiers(packet.modifiers());
                    }
                });
            }
        );
    }

    public static void sendToPlayer(PacketSyncMana packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToPlayer(PacketSyncStats packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
