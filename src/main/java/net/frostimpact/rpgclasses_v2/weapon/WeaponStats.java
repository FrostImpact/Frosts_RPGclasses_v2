package net.frostimpact.rpgclasses_v2.weapon;

/**
 * Represents the statistics and properties of a weapon
 */
public class WeaponStats {
    private final String weaponId;
    private final WeaponType weaponType;
    private int baseDamage;
    private double attackSpeed;
    private int criticalChance; // Percentage 0-100
    private int criticalDamage; // Percentage multiplier
    private int reach; // Reach distance
    private int manaCost; // Mana cost for special attacks
    private int cooldown; // Cooldown in ticks
    
    public WeaponStats(String weaponId, WeaponType weaponType) {
        this.weaponId = weaponId;
        this.weaponType = weaponType;
        this.baseDamage = 5;
        this.attackSpeed = 1.0;
        this.criticalChance = 5;
        this.criticalDamage = 150;
        this.reach = 3;
        this.manaCost = 0;
        this.cooldown = 0;
    }
    
    // Getters
    public String getWeaponId() {
        return weaponId;
    }
    
    public WeaponType getWeaponType() {
        return weaponType;
    }
    
    public int getBaseDamage() {
        return baseDamage;
    }
    
    public double getAttackSpeed() {
        return attackSpeed;
    }
    
    public int getCriticalChance() {
        return criticalChance;
    }
    
    public int getCriticalDamage() {
        return criticalDamage;
    }
    
    public int getReach() {
        return reach;
    }
    
    public int getManaCost() {
        return manaCost;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    // Setters for configuration
    public WeaponStats setBaseDamage(int baseDamage) {
        this.baseDamage = baseDamage;
        return this;
    }
    
    public WeaponStats setAttackSpeed(double attackSpeed) {
        this.attackSpeed = attackSpeed;
        return this;
    }
    
    public WeaponStats setCriticalChance(int criticalChance) {
        this.criticalChance = criticalChance;
        return this;
    }
    
    public WeaponStats setCriticalDamage(int criticalDamage) {
        this.criticalDamage = criticalDamage;
        return this;
    }
    
    public WeaponStats setReach(int reach) {
        this.reach = reach;
        return this;
    }
    
    public WeaponStats setManaCost(int manaCost) {
        this.manaCost = manaCost;
        return this;
    }
    
    public WeaponStats setCooldown(int cooldown) {
        this.cooldown = cooldown;
        return this;
    }
}
