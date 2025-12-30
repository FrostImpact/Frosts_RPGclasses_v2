package net.frostimpact.rpgclasses_v2.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet to notify server when Marksman enters/exits FOCUS mode
 */
public record PacketMarksmanFocusMode(boolean inFocusMode) implements CustomPacketPayload {
    public static final Type<PacketMarksmanFocusMode> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("rpgclasses_v2", "marksman_focus_mode"));
    
    public static final StreamCodec<ByteBuf, PacketMarksmanFocusMode> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        PacketMarksmanFocusMode::inFocusMode,
        PacketMarksmanFocusMode::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
