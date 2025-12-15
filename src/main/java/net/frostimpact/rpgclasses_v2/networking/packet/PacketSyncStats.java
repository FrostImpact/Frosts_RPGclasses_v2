package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record PacketSyncStats(List<StatModifier> modifiers) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSyncStats> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "sync_stats"));

    public static final StreamCodec<ByteBuf, PacketSyncStats> STREAM_CODEC = StreamCodec.composite(
        StatModifier.STREAM_CODEC.apply(ByteBufCodecs.list()),
        PacketSyncStats::modifiers,
        PacketSyncStats::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
