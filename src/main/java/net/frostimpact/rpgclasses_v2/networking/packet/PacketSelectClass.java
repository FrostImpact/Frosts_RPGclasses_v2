package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketSelectClass(String classId) implements CustomPacketPayload {
    public static final Type<PacketSelectClass> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "select_class"));
    
    public static final StreamCodec<ByteBuf, PacketSelectClass> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        PacketSelectClass::classId,
        PacketSelectClass::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
