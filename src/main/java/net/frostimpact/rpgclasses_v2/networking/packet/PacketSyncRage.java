package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet to sync Berserker RAGE data from server to client
 */
public record PacketSyncRage(int rage, boolean enraged, boolean enhancedEnraged, boolean exhausted, int axeThrowCharges) implements CustomPacketPayload {
    public static final Type<PacketSyncRage> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "sync_rage"));
    
    public static final StreamCodec<ByteBuf, PacketSyncRage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketSyncRage::rage,
        ByteBufCodecs.BOOL,
        PacketSyncRage::enraged,
        ByteBufCodecs.BOOL,
        PacketSyncRage::enhancedEnraged,
        ByteBufCodecs.BOOL,
        PacketSyncRage::exhausted,
        ByteBufCodecs.INT,
        PacketSyncRage::axeThrowCharges,
        PacketSyncRage::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
