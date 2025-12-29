package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketAllocateStatPoint(StatType statType) implements CustomPacketPayload {
    public static final Type<PacketAllocateStatPoint> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "allocate_stat_point"));
    
    public static final StreamCodec<ByteBuf, PacketAllocateStatPoint> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        packet -> packet.statType.name(),
        statTypeName -> new PacketAllocateStatPoint(StatType.valueOf(statTypeName))
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
