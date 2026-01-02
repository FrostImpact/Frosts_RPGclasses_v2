package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet to sync Lancer Momentum data from server to client
 */
public record PacketSyncMomentum(float momentum, boolean empoweredAttack) implements CustomPacketPayload {
    public static final Type<PacketSyncMomentum> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "sync_momentum"));
    
    public static final StreamCodec<ByteBuf, PacketSyncMomentum> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT,
        PacketSyncMomentum::momentum,
        ByteBufCodecs.BOOL,
        PacketSyncMomentum::empoweredAttack,
        PacketSyncMomentum::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
