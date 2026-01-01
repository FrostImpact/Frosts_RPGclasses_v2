package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server requesting skill tree data synchronization
 */
public record PacketRequestSkillTreeData() implements CustomPacketPayload {
    public static final Type<PacketRequestSkillTreeData> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "request_skill_tree_data"));
    
    public static final StreamCodec<ByteBuf, PacketRequestSkillTreeData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PacketRequestSkillTreeData decode(ByteBuf buffer) {
            return new PacketRequestSkillTreeData();
        }
        
        @Override
        public void encode(ByteBuf buffer, PacketRequestSkillTreeData packet) {
            // No data to encode
        }
    };
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
