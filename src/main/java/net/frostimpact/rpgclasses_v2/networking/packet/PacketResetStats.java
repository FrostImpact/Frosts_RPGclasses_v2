package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketResetStats() implements CustomPacketPayload {
    public static final Type<PacketResetStats> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "reset_stats"));
    
    public static final StreamCodec<ByteBuf, PacketResetStats> STREAM_CODEC = StreamCodec.unit(new PacketResetStats());
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
