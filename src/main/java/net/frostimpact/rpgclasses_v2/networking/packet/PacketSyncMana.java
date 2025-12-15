package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketSyncMana(int mana, int maxMana) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSyncMana> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "sync_mana"));

    public static final StreamCodec<ByteBuf, PacketSyncMana> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketSyncMana::mana,
        ByteBufCodecs.INT,
        PacketSyncMana::maxMana,
        PacketSyncMana::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
