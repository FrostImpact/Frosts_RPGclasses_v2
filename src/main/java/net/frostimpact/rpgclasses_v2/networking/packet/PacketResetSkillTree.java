package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketResetSkillTree(String skillTreeId) implements CustomPacketPayload {
    public static final Type<PacketResetSkillTree> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "reset_skill_tree"));
    
    public static final StreamCodec<ByteBuf, PacketResetSkillTree> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        PacketResetSkillTree::skillTreeId,
        PacketResetSkillTree::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
