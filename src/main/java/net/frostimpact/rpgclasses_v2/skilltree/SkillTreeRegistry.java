package net.frostimpact.rpgclasses_v2.skilltree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry for managing skill trees
 */
public class SkillTreeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillTreeRegistry.class);
    private static final Map<String, SkillTree> skillTrees = new HashMap<>();
    
    /**
     * Register a skill tree
     */
    public static void register(SkillTree skillTree) {
        String treeId = skillTree.getId();
        if (skillTrees.containsKey(treeId)) {
            LOGGER.warn("Skill tree {} is already registered. Overwriting.", treeId);
        }
        skillTrees.put(treeId, skillTree);
        LOGGER.debug("Registered skill tree: {}", treeId);
    }
    
    /**
     * Get a skill tree by ID
     */
    public static Optional<SkillTree> getSkillTree(String treeId) {
        return Optional.ofNullable(skillTrees.get(treeId));
    }
    
    /**
     * Check if a skill tree is registered
     */
    public static boolean isRegistered(String treeId) {
        return skillTrees.containsKey(treeId);
    }
    
    /**
     * Initialize placeholder skill trees
     */
    public static void initializePlaceholderTrees() {
        // Warrior skill tree
        SkillTree warriorTree = new SkillTree("warrior", "Warrior Skills", "Combat skills for warriors");
        
        // Root node at top
        SkillNode powerStrike = new SkillNode("power_strike", "Power Strike", 
            "Increases melee damage by 10% per level", 5, 1, 1, 2, 0, "");
        warriorTree.addNode(powerStrike);
        
        // Second tier - branches out
        SkillNode toughness = new SkillNode("toughness", "Toughness", 
            "Increases max health by 5 per level", 5, 1, 1, 1, 1, "");
        warriorTree.addNode(toughness);
        
        SkillNode battleCry = new SkillNode("battle_cry", "Battle Cry", 
            "Increases attack speed by 5% per level", 3, 1, 3, 3, 1, "");
        battleCry.addRequirement("power_strike");
        warriorTree.addNode(battleCry);
        
        // Third tier - advanced skills
        SkillNode whirlwind = new SkillNode("whirlwind", "Whirlwind", 
            "Unlocks a spinning attack ability", 1, 2, 5, 2, 2, "");
        whirlwind.addRequirement("power_strike");
        warriorTree.addNode(whirlwind);
        
        register(warriorTree);
        
        // Mage skill tree
        SkillTree mageTree = new SkillTree("mage", "Mage Skills", "Magical skills for mages");
        
        // Root node
        SkillNode spellPower = new SkillNode("spell_power", "Spell Power", 
            "Increases magic damage by 15% per level", 5, 1, 1, 2, 0, "");
        mageTree.addNode(spellPower);
        
        // Second tier
        SkillNode manaPool = new SkillNode("mana_pool", "Expanded Mana Pool", 
            "Increases max mana by 10 per level", 5, 1, 1, 1, 1, "");
        manaPool.addRequirement("spell_power");
        mageTree.addNode(manaPool);
        
        SkillNode manaRegen = new SkillNode("mana_regen", "Mana Regeneration", 
            "Increases mana regen by 1 per level", 3, 1, 3, 3, 1, "");
        manaRegen.addRequirement("spell_power");
        mageTree.addNode(manaRegen);
        
        // Third tier
        SkillNode fireball = new SkillNode("fireball", "Fireball", 
            "Unlocks the fireball spell", 1, 2, 5, 2, 2, "");
        fireball.addRequirement("spell_power");
        mageTree.addNode(fireball);
        
        register(mageTree);
        
        // Rogue skill tree
        SkillTree rogueTree = new SkillTree("rogue", "Rogue Skills", "Stealth and agility skills for rogues");
        
        // Root node
        SkillNode agility = new SkillNode("agility", "Agility", 
            "Increases movement speed by 5% per level", 5, 1, 1, 2, 0, "");
        rogueTree.addNode(agility);
        
        // Second tier
        SkillNode criticalEye = new SkillNode("critical_eye", "Critical Eye", 
            "Increases critical hit chance by 3% per level", 5, 1, 1, 1, 1, "");
        criticalEye.addRequirement("agility");
        rogueTree.addNode(criticalEye);
        
        SkillNode evasion = new SkillNode("evasion", "Evasion", 
            "Increases dodge chance by 2% per level", 3, 1, 3, 3, 1, "");
        evasion.addRequirement("agility");
        rogueTree.addNode(evasion);
        
        // Third tier
        SkillNode shadowStep = new SkillNode("shadow_step", "Shadow Step", 
            "Unlocks teleportation ability", 1, 2, 5, 2, 2, "");
        shadowStep.addRequirement("agility");
        rogueTree.addNode(shadowStep);
        
        register(rogueTree);
        
        // Ranger skill tree - Full comprehensive tree
        SkillTree rangerTree = new SkillTree("ranger", "Ranger Skills", "Archery and tracking skills");
        
        // Tier 1 - Root skills (no requirements)
        SkillNode precision = new SkillNode("precision", "Precision", 
            "Increases ranged damage by 8% per level", 5, 1, 1, 2, 0, "");
        rangerTree.addNode(precision);
        
        // Tier 2 - Branching out
        SkillNode rapidFire = new SkillNode("rapid_fire", "Rapid Fire", 
            "Increases attack speed by 10% per level", 3, 1, 3, 1, 1, "");
        rapidFire.addRequirement("precision");
        rangerTree.addNode(rapidFire);
        
        SkillNode steadyHand = new SkillNode("steady_hand", "Steady Hand", 
            "Increases bow draw speed by 15% per level", 3, 1, 3, 2, 1, "");
        steadyHand.addRequirement("precision");
        rangerTree.addNode(steadyHand);
        
        SkillNode eagleEye = new SkillNode("eagle_eye", "Eagle Eye", 
            "Increases arrow velocity and range by 10% per level", 3, 1, 3, 3, 1, "");
        eagleEye.addRequirement("precision");
        rangerTree.addNode(eagleEye);
        
        // Tier 3 - Intermediate skills
        SkillNode multiShot = new SkillNode("multi_shot", "Multi-Shot", 
            "Fire additional arrows (1 per level, max 3)", 3, 2, 5, 1, 2, "");
        multiShot.addRequirement("rapid_fire");
        rangerTree.addNode(multiShot);
        
        SkillNode tracking = new SkillNode("tracking", "Tracking", 
            "Highlights enemies through walls within 20 blocks", 1, 2, 5, 2, 2, "");
        tracking.addRequirement("steady_hand");
        rangerTree.addNode(tracking);
        
        SkillNode windRunner = new SkillNode("wind_runner", "Wind Runner", 
            "Increases movement speed by 5% per level", 5, 1, 5, 3, 2, "");
        windRunner.addRequirement("eagle_eye");
        rangerTree.addNode(windRunner);
        
        // Tier 4 - Advanced skills
        SkillNode explosiveArrow = new SkillNode("explosive_arrow", "Explosive Arrow", 
            "Arrows explode on impact dealing AoE damage", 1, 3, 8, 1, 3, "");
        explosiveArrow.addRequirement("multi_shot");
        rangerTree.addNode(explosiveArrow);
        
        SkillNode huntersMark = new SkillNode("hunters_mark", "Hunter's Mark", 
            "Mark a target to take 20% more damage from you", 1, 3, 8, 2, 3, "");
        huntersMark.addRequirement("tracking");
        rangerTree.addNode(huntersMark);
        
        SkillNode nimbleFeet = new SkillNode("nimble_feet", "Nimble Feet", 
            "No movement penalty while drawing bow", 1, 2, 8, 3, 3, "");
        nimbleFeet.addRequirement("wind_runner");
        rangerTree.addNode(nimbleFeet);
        
        // Tier 5 - Ultimate skills
        SkillNode arrowStorm = new SkillNode("arrow_storm", "Arrow Storm", 
            "Rain arrows down in a target area", 1, 4, 12, 2, 4, "");
        arrowStorm.addRequirement("explosive_arrow");
        arrowStorm.addRequirement("hunters_mark");
        rangerTree.addNode(arrowStorm);
        
        register(rangerTree);
        
        // Hawkeye skill tree (Ranger subclass) - Aerial combat specialist
        SkillTree hawkeyeTree = new SkillTree("hawkeye", "Hawkeye Skills", "Aerial combat and mobility abilities");
        
        // Tier 1 - Root: GLIDE
        SkillNode glide = new SkillNode("glide", "Glide", 
            "Gain Slow Falling I while in the air. Duration increases with level.", 5, 1, 1, 2, 0, "");
        hawkeyeTree.addNode(glide);
        
        // Tier 2 - AERIAL AFFINITY and basic abilities
        SkillNode aerialAffinity = new SkillNode("aerial_affinity", "Aerial Affinity", 
            "Passively gain SEEKER charges (max 5) while midair. Abilities grant 1 SEEKER charge on usage.", 3, 1, 3, 1, 1, "");
        aerialAffinity.addRequirement("glide");
        hawkeyeTree.addNode(aerialAffinity);
        
        SkillNode updraft = new SkillNode("updraft", "Updraft", 
            "Launch yourself upwards. 12s cooldown, 15 MP cost.", 1, 2, 3, 3, 1, "");
        updraft.addRequirement("glide");
        hawkeyeTree.addNode(updraft);
        
        // Tier 3 - VAULT and SEEKERS
        SkillNode vault = new SkillNode("vault", "Vault", 
            "Launch yourself forward and lob a low velocity projectile. Hitting an enemy resets UPDRAFT cooldown. 8s cooldown, 15 MP cost.", 1, 2, 5, 1, 2, "");
        vault.addRequirement("aerial_affinity");
        hawkeyeTree.addNode(vault);
        
        SkillNode seekers = new SkillNode("seekers", "Seekers", 
            "Release homing projectiles based on SEEKER charges (1 per charge). 5s cooldown, (5 * charges) MP cost.", 1, 2, 5, 3, 2, "");
        seekers.addRequirement("aerial_affinity");
        seekers.addRequirement("updraft");
        hawkeyeTree.addNode(seekers);
        
        // Tier 4 - Enhanced abilities
        SkillNode improvedGlide = new SkillNode("improved_glide", "Improved Glide", 
            "Glide now provides Slow Falling II and slight horizontal drift control", 3, 2, 7, 1, 3, "");
        improvedGlide.addRequirement("vault");
        hawkeyeTree.addNode(improvedGlide);
        
        SkillNode aerialMastery = new SkillNode("aerial_mastery", "Aerial Mastery", 
            "Increased damage (5% per level) while airborne", 5, 1, 7, 2, 3, "");
        aerialMastery.addRequirement("seekers");
        hawkeyeTree.addNode(aerialMastery);
        
        SkillNode quickRecovery = new SkillNode("quick_recovery", "Quick Recovery", 
            "Reduce UPDRAFT and VAULT cooldowns by 10% per level", 5, 1, 7, 3, 3, "");
        quickRecovery.addRequirement("seekers");
        hawkeyeTree.addNode(quickRecovery);
        
        // Tier 5 - Ultimate abilities
        SkillNode skyDive = new SkillNode("sky_dive", "Sky Dive", 
            "Dive bomb enemies from above, dealing massive damage on impact", 1, 3, 10, 1, 4, "");
        skyDive.addRequirement("improved_glide");
        skyDive.addRequirement("aerial_mastery");
        hawkeyeTree.addNode(skyDive);
        
        SkillNode seekerBarrage = new SkillNode("seeker_barrage", "Seeker Barrage", 
            "Double the number of SEEKER projectiles released", 1, 3, 10, 3, 4, "");
        seekerBarrage.addRequirement("aerial_mastery");
        seekerBarrage.addRequirement("quick_recovery");
        hawkeyeTree.addNode(seekerBarrage);
        
        register(hawkeyeTree);
        
        // Tank skill tree
        SkillTree tankTree = new SkillTree("tank", "Tank Skills", "Defensive and protective skills");
        
        SkillNode ironSkin = new SkillNode("iron_skin", "Iron Skin", 
            "Increases defense by 3 per level", 5, 1, 1, 2, 0, "");
        tankTree.addNode(ironSkin);
        
        SkillNode shieldWall = new SkillNode("shield_wall", "Shield Wall", 
            "Reduces damage taken by 5% per level", 3, 1, 3, 1, 1, "");
        shieldWall.addRequirement("iron_skin");
        tankTree.addNode(shieldWall);
        
        SkillNode taunt = new SkillNode("taunt", "Taunt", 
            "Forces enemies to target you", 1, 2, 5, 3, 1, "");
        taunt.addRequirement("iron_skin");
        tankTree.addNode(taunt);
        
        register(tankTree);
        
        // Priest skill tree
        SkillTree priestTree = new SkillTree("priest", "Priest Skills", "Healing and support skills");
        
        SkillNode divineBlessing = new SkillNode("divine_blessing", "Divine Blessing", 
            "Increases healing power by 10% per level", 5, 1, 1, 2, 0, "");
        priestTree.addNode(divineBlessing);
        
        SkillNode holyLight = new SkillNode("holy_light", "Holy Light", 
            "Heals nearby allies over time", 3, 1, 3, 1, 1, "");
        holyLight.addRequirement("divine_blessing");
        priestTree.addNode(holyLight);
        
        SkillNode resurrection = new SkillNode("resurrection", "Resurrection", 
            "Revive fallen allies", 1, 2, 10, 3, 1, "");
        resurrection.addRequirement("divine_blessing");
        priestTree.addNode(resurrection);
        
        register(priestTree);
        
        // ===== SUBCLASS SKILL TREES =====
        
        // Berserker skill tree (Warrior subclass)
        SkillTree berserkerTree = new SkillTree("berserker", "Berserker Skills", "Rage-fueled combat skills");
        
        SkillNode rageStrike = new SkillNode("rage_strike", "Rage Strike", 
            "Increases damage by 15% per level when below 50% health", 5, 1, 1, 2, 0, "");
        berserkerTree.addNode(rageStrike);
        
        SkillNode bloodFrenzy = new SkillNode("blood_frenzy", "Blood Frenzy", 
            "Gain attack speed when hitting enemies", 3, 1, 3, 1, 1, "");
        bloodFrenzy.addRequirement("rage_strike");
        berserkerTree.addNode(bloodFrenzy);
        
        SkillNode berserkerRage = new SkillNode("berserker_rage", "Berserker Rage", 
            "Unleash devastating attacks at the cost of defense", 1, 2, 5, 3, 1, "");
        berserkerRage.addRequirement("rage_strike");
        berserkerTree.addNode(berserkerRage);
        
        register(berserkerTree);
        
        // Paladin skill tree (Warrior subclass)
        SkillTree paladinTree = new SkillTree("paladin", "Paladin Skills", "Holy warrior skills");
        
        SkillNode holySmite = new SkillNode("holy_smite", "Holy Smite", 
            "Deal extra holy damage to undead enemies", 5, 1, 1, 2, 0, "");
        paladinTree.addNode(holySmite);
        
        SkillNode divineShield = new SkillNode("divine_shield", "Divine Shield", 
            "Increase defense and heal over time", 3, 1, 3, 1, 1, "");
        divineShield.addRequirement("holy_smite");
        paladinTree.addNode(divineShield);
        
        SkillNode consecration = new SkillNode("consecration", "Consecration", 
            "Create a holy aura that damages enemies and heals allies", 1, 2, 5, 3, 1, "");
        consecration.addRequirement("holy_smite");
        paladinTree.addNode(consecration);
        
        register(paladinTree);
        
        // Pyromancer skill tree (Mage subclass)
        SkillTree pyromancerTree = new SkillTree("pyromancer", "Pyromancer Skills", "Fire magic mastery");
        
        SkillNode ignite = new SkillNode("ignite", "Ignite", 
            "Fire spells have a chance to set enemies on fire", 5, 1, 1, 2, 0, "");
        pyromancerTree.addNode(ignite);
        
        SkillNode inferno = new SkillNode("inferno", "Inferno", 
            "Increase fire damage by 20% per level", 3, 1, 3, 1, 1, "");
        inferno.addRequirement("ignite");
        pyromancerTree.addNode(inferno);
        
        SkillNode meteorStrike = new SkillNode("meteor_strike", "Meteor Strike", 
            "Call down a devastating meteor from the sky", 1, 2, 5, 3, 1, "");
        meteorStrike.addRequirement("ignite");
        pyromancerTree.addNode(meteorStrike);
        
        register(pyromancerTree);
        
        // Frost Mage skill tree (Mage subclass)
        SkillTree frostmageTree = new SkillTree("frostmage", "Frost Mage Skills", "Ice magic mastery");
        
        SkillNode frostbite = new SkillNode("frostbite", "Frostbite", 
            "Ice spells slow enemies by 10% per level", 5, 1, 1, 2, 0, "");
        frostmageTree.addNode(frostbite);
        
        SkillNode iceArmor = new SkillNode("ice_armor", "Ice Armor", 
            "Surround yourself with protective ice", 3, 1, 3, 1, 1, "");
        iceArmor.addRequirement("frostbite");
        frostmageTree.addNode(iceArmor);
        
        SkillNode blizzard = new SkillNode("blizzard", "Blizzard", 
            "Summon a freezing storm around you", 1, 2, 5, 3, 1, "");
        blizzard.addRequirement("frostbite");
        frostmageTree.addNode(blizzard);
        
        register(frostmageTree);
        
        // Assassin skill tree (Rogue subclass)
        SkillTree assassinTree = new SkillTree("assassin", "Assassin Skills", "Deadly stealth attacks");
        
        SkillNode backstab = new SkillNode("backstab", "Backstab", 
            "Deal 50% more damage when attacking from behind", 5, 1, 1, 2, 0, "");
        assassinTree.addNode(backstab);
        
        SkillNode poisonBlade = new SkillNode("poison_blade", "Poison Blade", 
            "Your attacks have a chance to poison enemies", 3, 1, 3, 1, 1, "");
        poisonBlade.addRequirement("backstab");
        assassinTree.addNode(poisonBlade);
        
        SkillNode deathMark = new SkillNode("death_mark", "Death Mark", 
            "Mark an enemy for death, increasing damage taken", 1, 2, 5, 3, 1, "");
        deathMark.addRequirement("backstab");
        assassinTree.addNode(deathMark);
        
        register(assassinTree);
        
        // Shadow Dancer skill tree (Rogue subclass)
        SkillTree shadowdancerTree = new SkillTree("shadowdancer", "Shadow Dancer Skills", "Shadow manipulation");
        
        SkillNode shadowMeld = new SkillNode("shadow_meld", "Shadow Meld", 
            "Become harder to detect in darkness", 5, 1, 1, 2, 0, "");
        shadowdancerTree.addNode(shadowMeld);
        
        SkillNode darkBlink = new SkillNode("dark_blink", "Dark Blink", 
            "Teleport short distances through shadows", 3, 1, 3, 1, 1, "");
        darkBlink.addRequirement("shadow_meld");
        shadowdancerTree.addNode(darkBlink);
        
        SkillNode shadowClones = new SkillNode("shadow_clones", "Shadow Clones", 
            "Create shadow duplicates to confuse enemies", 1, 2, 5, 3, 1, "");
        shadowClones.addRequirement("shadow_meld");
        shadowdancerTree.addNode(shadowClones);
        
        register(shadowdancerTree);
        
        // Marksman skill tree (Ranger subclass)
        SkillTree marksmanTree = new SkillTree("marksman", "Marksman Skills", "Precision archery");
        
        SkillNode steadyAim = new SkillNode("steady_aim", "Steady Aim", 
            "Increase ranged accuracy and damage", 5, 1, 1, 2, 0, "");
        marksmanTree.addNode(steadyAim);
        
        SkillNode piercingShot = new SkillNode("piercing_shot", "Piercing Shot", 
            "Arrows pierce through multiple enemies", 3, 1, 3, 1, 1, "");
        piercingShot.addRequirement("steady_aim");
        marksmanTree.addNode(piercingShot);
        
        SkillNode headshot = new SkillNode("headshot", "Headshot", 
            "Critical hits deal massive bonus damage", 1, 2, 5, 3, 1, "");
        headshot.addRequirement("steady_aim");
        marksmanTree.addNode(headshot);
        
        register(marksmanTree);
        
        // Beast Master skill tree (Ranger subclass)
        SkillTree beastmasterTree = new SkillTree("beastmaster", "Beast Master Skills", "Animal companion abilities");
        
        SkillNode animalBond = new SkillNode("animal_bond", "Animal Bond", 
            "Strengthen your connection with animal companions", 5, 1, 1, 2, 0, "");
        beastmasterTree.addNode(animalBond);
        
        SkillNode packTactics = new SkillNode("pack_tactics", "Pack Tactics", 
            "You and your companions deal bonus damage together", 3, 1, 3, 1, 1, "");
        packTactics.addRequirement("animal_bond");
        beastmasterTree.addNode(packTactics);
        
        SkillNode summonBeast = new SkillNode("summon_beast", "Summon Beast", 
            "Call a powerful animal companion to fight", 1, 2, 5, 3, 1, "");
        summonBeast.addRequirement("animal_bond");
        beastmasterTree.addNode(summonBeast);
        
        register(beastmasterTree);
        
        // Guardian skill tree (Tank subclass)
        SkillTree guardianTree = new SkillTree("guardian", "Guardian Skills", "Protective tank abilities");
        
        SkillNode bulwark = new SkillNode("bulwark", "Bulwark", 
            "Increase shield effectiveness", 5, 1, 1, 2, 0, "");
        guardianTree.addNode(bulwark);
        
        SkillNode protector = new SkillNode("protector", "Protector", 
            "Redirect damage from nearby allies to yourself", 3, 1, 3, 1, 1, "");
        protector.addRequirement("bulwark");
        guardianTree.addNode(protector);
        
        SkillNode lastStand = new SkillNode("last_stand", "Last Stand", 
            "Become invulnerable for a short time at low health", 1, 2, 5, 3, 1, "");
        lastStand.addRequirement("bulwark");
        guardianTree.addNode(lastStand);
        
        register(guardianTree);
        
        // Juggernaut skill tree (Tank subclass)
        SkillTree juggernautTree = new SkillTree("juggernaut", "Juggernaut Skills", "Unstoppable force abilities");
        
        SkillNode unstoppable = new SkillNode("unstoppable", "Unstoppable", 
            "Reduce knockback and slow effects", 5, 1, 1, 2, 0, "");
        juggernautTree.addNode(unstoppable);
        
        SkillNode crush = new SkillNode("crush", "Crush", 
            "Heavy attacks deal bonus damage and stun", 3, 1, 3, 1, 1, "");
        crush.addRequirement("unstoppable");
        juggernautTree.addNode(crush);
        
        SkillNode earthquake = new SkillNode("earthquake", "Earthquake", 
            "Slam the ground to damage and knock down enemies", 1, 2, 5, 3, 1, "");
        earthquake.addRequirement("unstoppable");
        juggernautTree.addNode(earthquake);
        
        register(juggernautTree);
        
        // Cleric skill tree (Priest subclass)
        SkillTree clericTree = new SkillTree("cleric", "Cleric Skills", "Advanced healing abilities");
        
        SkillNode healingTouch = new SkillNode("healing_touch", "Healing Touch", 
            "Increase direct healing effectiveness", 5, 1, 1, 2, 0, "");
        clericTree.addNode(healingTouch);
        
        SkillNode prayerOfMending = new SkillNode("prayer_of_mending", "Prayer of Mending", 
            "Place a healing blessing that jumps between allies", 3, 1, 3, 1, 1, "");
        prayerOfMending.addRequirement("healing_touch");
        clericTree.addNode(prayerOfMending);
        
        SkillNode divineIntervention = new SkillNode("divine_intervention", "Divine Intervention", 
            "Instantly heal all nearby allies", 1, 2, 5, 3, 1, "");
        divineIntervention.addRequirement("healing_touch");
        clericTree.addNode(divineIntervention);
        
        register(clericTree);
        
        // Templar skill tree (Priest subclass)
        SkillTree templarTree = new SkillTree("templar", "Templar Skills", "Holy warrior priest abilities");
        
        SkillNode righteousFury = new SkillNode("righteous_fury", "Righteous Fury", 
            "Deal holy damage with melee attacks", 5, 1, 1, 2, 0, "");
        templarTree.addNode(righteousFury);
        
        SkillNode holyArmor = new SkillNode("holy_armor", "Holy Armor", 
            "Gain bonus defense and reflect damage", 3, 1, 3, 1, 1, "");
        holyArmor.addRequirement("righteous_fury");
        templarTree.addNode(holyArmor);
        
        SkillNode divinePunishment = new SkillNode("divine_punishment", "Divine Punishment", 
            "Smite enemies with holy wrath", 1, 2, 5, 3, 1, "");
        divinePunishment.addRequirement("righteous_fury");
        templarTree.addNode(divinePunishment);
        
        register(templarTree);
        
        LOGGER.info("Initialized {} placeholder skill trees", skillTrees.size());
    }
}
