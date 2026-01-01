package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record PacketSyncSkillTreeData(Map<String, Map<String, Integer>> skillTreeAllocations) implements CustomPacketPayload {
    public static final Type<PacketSyncSkillTreeData> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "sync_skill_tree_data"));
    
    // Custom codec for nested maps
    public static final StreamCodec<ByteBuf, PacketSyncSkillTreeData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PacketSyncSkillTreeData decode(ByteBuf buffer) {
            int treeCount = ByteBufCodecs.VAR_INT.decode(buffer);
            Map<String, Map<String, Integer>> allocations = new HashMap<>();
            
            for (int i = 0; i < treeCount; i++) {
                String treeId = ByteBufCodecs.STRING_UTF8.decode(buffer);
                int nodeCount = ByteBufCodecs.VAR_INT.decode(buffer);
                Map<String, Integer> treeAllocations = new HashMap<>();
                
                for (int j = 0; j < nodeCount; j++) {
                    String nodeId = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    int level = ByteBufCodecs.VAR_INT.decode(buffer);
                    treeAllocations.put(nodeId, level);
                }
                
                allocations.put(treeId, treeAllocations);
            }
            
            return new PacketSyncSkillTreeData(allocations);
        }
        
        @Override
        public void encode(ByteBuf buffer, PacketSyncSkillTreeData packet) {
            ByteBufCodecs.VAR_INT.encode(buffer, packet.skillTreeAllocations.size());
            
            for (Map.Entry<String, Map<String, Integer>> treeEntry : packet.skillTreeAllocations.entrySet()) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, treeEntry.getKey());
                ByteBufCodecs.VAR_INT.encode(buffer, treeEntry.getValue().size());
                
                for (Map.Entry<String, Integer> nodeEntry : treeEntry.getValue().entrySet()) {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, nodeEntry.getKey());
                    ByteBufCodecs.VAR_INT.encode(buffer, nodeEntry.getValue());
                }
            }
        }
    };
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
