package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketSyncRPGData(
        String currentClass,
        int level,
        int classLevel,
        int classExperience,
        int availableStatPoints,
        int availableSkillPoints
) implements CustomPacketPayload {
    public static final Type<PacketSyncRPGData> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "sync_rpg_data"));
    
    public static final StreamCodec<ByteBuf, PacketSyncRPGData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        PacketSyncRPGData::currentClass,
        ByteBufCodecs.INT,
        PacketSyncRPGData::level,
        ByteBufCodecs.INT,
        PacketSyncRPGData::classLevel,
        ByteBufCodecs.INT,
        PacketSyncRPGData::classExperience,
        ByteBufCodecs.INT,
        PacketSyncRPGData::availableStatPoints,
        ByteBufCodecs.INT,
        PacketSyncRPGData::availableSkillPoints,
        PacketSyncRPGData::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
