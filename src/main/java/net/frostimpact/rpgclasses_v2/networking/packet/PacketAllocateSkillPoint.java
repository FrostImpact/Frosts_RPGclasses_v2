package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketAllocateSkillPoint(String skillTreeId, String skillNodeId) implements CustomPacketPayload {
    public static final Type<PacketAllocateSkillPoint> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "allocate_skill_point"));
    
    public static final StreamCodec<ByteBuf, PacketAllocateSkillPoint> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        PacketAllocateSkillPoint::skillTreeId,
        ByteBufCodecs.STRING_UTF8,
        PacketAllocateSkillPoint::skillNodeId,
        PacketAllocateSkillPoint::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
