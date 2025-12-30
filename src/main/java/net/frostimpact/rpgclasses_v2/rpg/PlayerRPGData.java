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
            Codec.STRING.fieldOf("currentClass").forGetter(d -> d.currentClass),
            Codec.INT.fieldOf("availableStatPoints").forGetter(d -> d.availableStatPoints),
            Codec.INT.fieldOf("availableSkillPoints").forGetter(d -> d.availableSkillPoints),
            Codec.INT.fieldOf("level").forGetter(d -> d.level),
            Codec.INT.fieldOf("classLevel").forGetter(d -> d.classLevel),
            Codec.INT.fieldOf("classExperience").forGetter(d -> d.classExperience),
            Codec.INT.fieldOf("seekerCharges").forGetter(d -> d.seekerCharges)
        ).apply(instance, PlayerRPGData::new)
    );

    private int mana;
    private int maxMana;
    private Map<String, Integer> cooldowns;
    private String currentClass;
    private int availableStatPoints;
    private int availableSkillPoints;
    private int level;
    private int classLevel;
    private int classExperience;
    private int seekerCharges; // For Hawkeye SEEKER ability

    public PlayerRPGData() {
        this.mana = 100;
        this.maxMana = 100;
        this.cooldowns = new HashMap<>();
        this.currentClass = "NONE";
        this.availableStatPoints = 0;
        this.availableSkillPoints = 0;
        this.level = 1;
        this.classLevel = 1;
        this.classExperience = 0;
        this.seekerCharges = 0;
    }

    private PlayerRPGData(int mana, int maxMana, Map<String, Integer> cooldowns, String currentClass, 
                         int availableStatPoints, int availableSkillPoints, int level, int classLevel, 
                         int classExperience, int seekerCharges) {
        this.mana = mana;
        this.maxMana = maxMana;
        this.cooldowns = new HashMap<>(cooldowns);
        this.currentClass = currentClass;
        this.availableStatPoints = availableStatPoints;
        this.availableSkillPoints = availableSkillPoints;
        this.level = level;
        this.classLevel = classLevel;
        this.classExperience = classExperience;
        this.seekerCharges = seekerCharges;
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
    
    public Map<String, Integer> getAllCooldowns() {
        return new HashMap<>(cooldowns);
    }
    
    public void setAllCooldowns(Map<String, Integer> cooldowns) {
        this.cooldowns = new HashMap<>(cooldowns);
    }

    public String getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(String currentClass) {
        this.currentClass = currentClass;
    }

    public int getAvailableStatPoints() {
        return availableStatPoints;
    }

    public void setAvailableStatPoints(int points) {
        this.availableStatPoints = Math.max(0, points);
    }

    public void addStatPoints(int points) {
        this.availableStatPoints += points;
    }

    public boolean useStatPoint() {
        if (availableStatPoints > 0) {
            availableStatPoints--;
            return true;
        }
        return false;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }
    
    public int getClassLevel() {
        return classLevel;
    }
    
    public void setClassLevel(int classLevel) {
        this.classLevel = Math.max(1, classLevel);
    }
    
    public int getClassExperience() {
        return classExperience;
    }
    
    public void setClassExperience(int classExperience) {
        this.classExperience = Math.max(0, classExperience);
    }
    
    public void addClassExperience(int amount) {
        this.classExperience += amount;
        // Check for level up (100 XP per level as example)
        int xpNeeded = classLevel * 100;
        while (classExperience >= xpNeeded) {
            classExperience -= xpNeeded;
            classLevel++;
            // Award 1 skill point per level up
            availableSkillPoints++;
            xpNeeded = classLevel * 100;
        }
    }
    
    // Skill Points methods
    public int getAvailableSkillPoints() {
        return availableSkillPoints;
    }
    
    public void setAvailableSkillPoints(int points) {
        this.availableSkillPoints = Math.max(0, points);
    }
    
    public void addSkillPoints(int points) {
        this.availableSkillPoints += points;
    }
    
    public boolean useSkillPoint() {
        if (availableSkillPoints > 0) {
            availableSkillPoints--;
            return true;
        }
        return false;
    }
    
    // Seeker Charges methods (for Hawkeye)
    public static final int MAX_SEEKER_CHARGES = 5;
    
    public int getSeekerCharges() {
        return seekerCharges;
    }
    
    public void setSeekerCharges(int charges) {
        this.seekerCharges = Math.max(0, Math.min(charges, MAX_SEEKER_CHARGES));
    }
    
    public void addSeekerCharge() {
        this.seekerCharges = Math.min(MAX_SEEKER_CHARGES, this.seekerCharges + 1);
    }
    
    public int consumeSeekerCharges() {
        int charges = this.seekerCharges;
        this.seekerCharges = 0;
        return charges;
    }
}
