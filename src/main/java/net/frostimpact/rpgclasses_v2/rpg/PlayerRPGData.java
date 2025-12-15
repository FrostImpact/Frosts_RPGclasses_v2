package net.frostimpact.rpgclasses_v2.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

public class PlayerRPGData {
    public static final Codec<PlayerRPGData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("mana").forGetter(d -> d.mana),
            Codec.INT.fieldOf("maxMana").forGetter(d -> d.maxMana),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("cooldowns").forGetter(d -> d.cooldowns),
            Codec.STRING.fieldOf("currentClass").forGetter(d -> d.currentClass)
        ).apply(instance, PlayerRPGData::new)
    );

    private int mana;
    private int maxMana;
    private Map<String, Integer> cooldowns;
    private String currentClass;

    public PlayerRPGData() {
        this.mana = 100;
        this.maxMana = 100;
        this.cooldowns = new HashMap<>();
        this.currentClass = "NONE";
    }

    private PlayerRPGData(int mana, int maxMana, Map<String, Integer> cooldowns, String currentClass) {
        this.mana = mana;
        this.maxMana = maxMana;
        this.cooldowns = new HashMap<>(cooldowns);
        this.currentClass = currentClass;
    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = Math.max(0, Math.min(mana, maxMana));
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
        this.mana = Math.min(this.mana, maxMana);
    }

    public void useMana(int amount) {
        this.mana = Math.max(0, this.mana - amount);
    }

    public void regenMana(int amount) {
        this.mana = Math.min(maxMana, this.mana + amount);
    }

    public int getAbilityCooldown(String abilityId) {
        return cooldowns.getOrDefault(abilityId, 0);
    }

    public void setAbilityCooldown(String abilityId, int ticks) {
        if (ticks <= 0) {
            cooldowns.remove(abilityId);
        } else {
            cooldowns.put(abilityId, ticks);
        }
    }

    public void tickCooldowns() {
        cooldowns.replaceAll((key, value) -> value - 1);
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    public void clearAllCooldowns() {
        cooldowns.clear();
    }

    public String getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(String currentClass) {
        this.currentClass = currentClass;
    }
}
