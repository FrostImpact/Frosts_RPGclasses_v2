package net.frostimpact.rpgclasses_v2.rpg.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class StatModifier {
    public static final Codec<StatModifier> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("source").forGetter(m -> m.source),
            Codec.STRING.fieldOf("statType").forGetter(m -> m.statType.name()),
            Codec.DOUBLE.fieldOf("value").forGetter(m -> m.value),
            Codec.INT.fieldOf("duration").forGetter(m -> m.duration)
        ).apply(instance, (source, statTypeName, value, duration) -> 
            new StatModifier(source, StatType.valueOf(statTypeName), value, duration))
    );

    public static final StreamCodec<ByteBuf, StatModifier> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        m -> m.source,
        ByteBufCodecs.STRING_UTF8,
        m -> m.statType.name(),
        ByteBufCodecs.DOUBLE,
        m -> m.value,
        ByteBufCodecs.INT,
        m -> m.duration,
        (source, statTypeName, value, duration) -> 
            new StatModifier(source, StatType.valueOf(statTypeName), value, duration)
    );

    private final String source;
    private final StatType statType;
    private final double value;
    private int duration; // -1 for permanent

    public StatModifier(String source, StatType statType, double value, int duration) {
        this.source = source;
        this.statType = statType;
        this.value = value;
        this.duration = duration;
    }

    public String getSource() {
        return source;
    }

    public StatType getStatType() {
        return statType;
    }

    public double getValue() {
        return value;
    }

    public int getDuration() {
        return duration;
    }

    public void tick() {
        if (duration > 0) {
            duration--;
        }
    }

    public boolean isExpired() {
        return duration == 0;
    }

    public boolean isPermanent() {
        return duration == -1;
    }
}
