package net.frostimpact.rpgclasses_v2.rpg.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerStats {
    public static final Codec<PlayerStats> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.list(StatModifier.CODEC).fieldOf("modifiers").forGetter(s -> s.modifiers)
        ).apply(instance, PlayerStats::new)
    );

    private List<StatModifier> modifiers;

    public PlayerStats() {
        this.modifiers = new ArrayList<>();
    }

    private PlayerStats(List<StatModifier> modifiers) {
        this.modifiers = new ArrayList<>(modifiers);
    }

    public void addModifier(StatModifier modifier) {
        modifiers.add(modifier);
    }

    public void removeModifier(String source, StatType statType) {
        modifiers.removeIf(m -> m.getSource().equals(source) && m.getStatType() == statType);
    }

    public void removeAllFromSource(String source) {
        modifiers.removeIf(m -> m.getSource().equals(source));
    }

    public double getStatValue(StatType statType) {
        double total = 0.0;
        for (StatModifier modifier : modifiers) {
            if (modifier.getStatType() == statType) {
                total += modifier.getValue();
            }
        }
        return total;
    }

    public void tick() {
        for (StatModifier modifier : modifiers) {
            modifier.tick();
        }
        modifiers.removeIf(StatModifier::isExpired);
    }

    public List<StatModifier> getModifiers() {
        return new ArrayList<>(modifiers);
    }

    public void setModifiers(List<StatModifier> modifiers) {
        this.modifiers = new ArrayList<>(modifiers);
    }

    public void clearAll() {
        modifiers.clear();
    }
}
