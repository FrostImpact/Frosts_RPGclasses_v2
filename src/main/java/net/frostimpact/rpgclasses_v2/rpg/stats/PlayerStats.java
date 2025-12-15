package net.frostimpact.rpgclasses_v2.rpg.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerStats {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerStats.class);
    private static final double MAX_MOVE_SPEED = 300.0; // Maximum +300% move speed bonus
    
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


        
        // Apply clamping for MOVE_SPEED to prevent excessive values
        if (statType == StatType.MOVE_SPEED && total > MAX_MOVE_SPEED) {
            // Debug logging for troubleshooting - only active when debug logging is enabled
            LOGGER.debug("MOVE_SPEED stat clamped from {} to {} (max allowed)", total, MAX_MOVE_SPEED);
            
            // Log all MOVE_SPEED modifiers for debugging to identify source of excessive values
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MOVE_SPEED modifiers:");
                for (StatModifier modifier : modifiers) {
                    if (modifier.getStatType() == StatType.MOVE_SPEED) {
                        LOGGER.debug("  - Source: {}, Value: {}, Duration: {}", 
                            modifier.getSource(), modifier.getValue(), modifier.getDuration());
                    }
                }
            }
            
            total = MAX_MOVE_SPEED;
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
