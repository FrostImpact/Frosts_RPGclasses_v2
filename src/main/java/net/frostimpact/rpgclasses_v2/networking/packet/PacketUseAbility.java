package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketUseAbility(int abilitySlot) implements CustomPacketPayload {
    public static final Type<PacketUseAbility> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "use_ability"));
    
    public static final StreamCodec<ByteBuf, PacketUseAbility> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketUseAbility::abilitySlot,
        PacketUseAbility::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
