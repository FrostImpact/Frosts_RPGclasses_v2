package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet to sync seeker charges from server to client for Hawkeye class
 */
public record PacketSyncSeekerCharges(int seekerCharges) implements CustomPacketPayload {
    public static final Type<PacketSyncSeekerCharges> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "sync_seeker_charges"));
    
    public static final StreamCodec<ByteBuf, PacketSyncSeekerCharges> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketSyncSeekerCharges::seekerCharges,
        PacketSyncSeekerCharges::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
