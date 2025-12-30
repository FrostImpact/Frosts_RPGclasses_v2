package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record PacketSyncCooldowns(Map<String, Integer> cooldowns) implements CustomPacketPayload {
    public static final Type<PacketSyncCooldowns> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "sync_cooldowns"));
    
    public static final StreamCodec<ByteBuf, PacketSyncCooldowns> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.INT),
        PacketSyncCooldowns::cooldowns,
        PacketSyncCooldowns::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
