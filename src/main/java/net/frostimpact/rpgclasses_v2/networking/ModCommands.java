package net.frostimpact.rpgclasses_v2.networking;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.frostimpact.rpgclasses_v2.rpgclass.ClassRegistry;
import net.frostimpact.rpgclasses_v2.rpgclass.RPGClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

/**
 * Command system for managing RPG data.
 * 
 * Commands:
 * /rpgclasses xp add <player> <amount> - Add class XP to a player
 * /rpgclasses xp set <player> <amount> - Set class XP for a player
 * /rpgclasses xp get <player> - Get class XP for a player
 * /rpgclasses skillpoints add <player> <amount> - Add skill tree points to a player
 * /rpgclasses skillpoints set <player> <amount> - Set skill tree points for a player
 * /rpgclasses skillpoints get <player> - Get skill tree points for a player
 * /rpgclasses statpoints add <player> <amount> - Add stat points to a player
 * /rpgclasses statpoints set <player> <amount> - Set stat points for a player
 * /rpgclasses statpoints get <player> - Get stat points for a player
 * /rpgclasses level set <player> <level> - Set class level for a player
 * /rpgclasses level get <player> - Get class level for a player
 * /rpgclasses class set <player> <class_id> - Set class for a player
 * /rpgclasses class get <player> - Get class for a player
 * /rpgclasses info <player> - Get full RPG info for a player
 */
@EventBusSubscriber(modid = "rpgclasses_v2")
public class ModCommands {

    private static final SuggestionProvider<CommandSourceStack> CLASS_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(ClassRegistry.getAllClassIds(), builder);
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("rpgclasses")
            .requires(source -> source.hasPermission(2)) // Requires operator permission
            
            // XP Commands
            .then(Commands.literal("xp")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                return addXp(context.getSource(), player, amount);
                            })
                        )
                    )
                )
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                return setXp(context.getSource(), player, amount);
                            })
                        )
                    )
                )
                .then(Commands.literal("get")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            return getXp(context.getSource(), player);
                        })
                    )
                )
            )
            
            // Skill Points Commands
            .then(Commands.literal("skillpoints")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                return addSkillPoints(context.getSource(), player, amount);
                            })
                        )
                    )
                )
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                return setSkillPoints(context.getSource(), player, amount);
                            })
                        )
                    )
                )
                .then(Commands.literal("get")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            return getSkillPoints(context.getSource(), player);
                        })
                    )
                )
            )
            
            // Stat Points Commands
            .then(Commands.literal("statpoints")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                return addStatPoints(context.getSource(), player, amount);
                            })
                        )
                    )
                )
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                return setStatPoints(context.getSource(), player, amount);
                            })
                        )
                    )
                )
                .then(Commands.literal("get")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            return getStatPoints(context.getSource(), player);
                        })
                    )
                )
            )
            
            // Level Commands
            .then(Commands.literal("level")
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                int level = IntegerArgumentType.getInteger(context, "level");
                                return setLevel(context.getSource(), player, level);
                            })
                        )
                    )
                )
                .then(Commands.literal("get")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            return getLevel(context.getSource(), player);
                        })
                    )
                )
            )
            
            // Class Commands
            .then(Commands.literal("class")
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("class_id", StringArgumentType.word())
                            .suggests(CLASS_SUGGESTIONS)
                            .executes(context -> {
                                ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                String classId = StringArgumentType.getString(context, "class_id");
                                return setClass(context.getSource(), player, classId);
                            })
                        )
                    )
                )
                .then(Commands.literal("get")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            return getClass(context.getSource(), player);
                        })
                    )
                )
            )
            
            // Info Command
            .then(Commands.literal("info")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                        return getInfo(context.getSource(), player);
                    })
                )
            )
        );
    }

    // XP Methods
    private static int addXp(CommandSourceStack source, ServerPlayer player, int amount) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        data.addClassExperience(amount);
        source.sendSuccess(() -> Component.literal("Added " + amount + " XP to " + player.getName().getString() + 
            ". New XP: " + data.getClassExperience() + ", Level: " + data.getClassLevel()), true);
        return 1;
    }

    private static int setXp(CommandSourceStack source, ServerPlayer player, int amount) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        data.setClassExperience(amount);
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s XP to " + amount), true);
        return 1;
    }

    private static int getXp(CommandSourceStack source, ServerPlayer player) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " has " + data.getClassExperience() + 
            " XP (Level " + data.getClassLevel() + ")"), false);
        return 1;
    }

    // Skill Points Methods
    private static int addSkillPoints(CommandSourceStack source, ServerPlayer player, int amount) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        data.addSkillPoints(amount);
        source.sendSuccess(() -> Component.literal("Added " + amount + " skill points to " + player.getName().getString() + 
            ". Total: " + data.getAvailableSkillPoints()), true);
        return 1;
    }

    private static int setSkillPoints(CommandSourceStack source, ServerPlayer player, int amount) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        data.setAvailableSkillPoints(amount);
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s skill points to " + amount), true);
        return 1;
    }

    private static int getSkillPoints(CommandSourceStack source, ServerPlayer player) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " has " + data.getAvailableSkillPoints() + " skill points"), false);
        return 1;
    }

    // Stat Points Methods
    private static int addStatPoints(CommandSourceStack source, ServerPlayer player, int amount) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        data.addStatPoints(amount);
        source.sendSuccess(() -> Component.literal("Added " + amount + " stat points to " + player.getName().getString() + 
            ". Total: " + data.getAvailableStatPoints()), true);
        return 1;
    }

    private static int setStatPoints(CommandSourceStack source, ServerPlayer player, int amount) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        data.setAvailableStatPoints(amount);
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s stat points to " + amount), true);
        return 1;
    }

    private static int getStatPoints(CommandSourceStack source, ServerPlayer player) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " has " + data.getAvailableStatPoints() + " stat points"), false);
        return 1;
    }

    // Level Methods
    private static int setLevel(CommandSourceStack source, ServerPlayer player, int level) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        data.setClassLevel(level);
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s class level to " + level), true);
        return 1;
    }

    private static int getLevel(CommandSourceStack source, ServerPlayer player) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " is class level " + data.getClassLevel()), false);
        return 1;
    }

    // Class Methods
    private static int setClass(CommandSourceStack source, ServerPlayer player, String classId) {
        Optional<RPGClass> rpgClassOpt = ClassRegistry.getClass(classId);
        if (rpgClassOpt.isEmpty()) {
            source.sendFailure(Component.literal("Unknown class: " + classId));
            return 0;
        }
        
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        data.setCurrentClass(classId);
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s class to " + rpgClassOpt.get().getName()), true);
        return 1;
    }

    private static int getClass(CommandSourceStack source, ServerPlayer player) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        String classId = data.getCurrentClass();
        Optional<RPGClass> rpgClassOpt = ClassRegistry.getClass(classId);
        String className = rpgClassOpt.map(RPGClass::getName).orElse(classId);
        source.sendSuccess(() -> Component.literal(player.getName().getString() + "'s class is " + className), false);
        return 1;
    }

    // Info Method
    private static int getInfo(CommandSourceStack source, ServerPlayer player) {
        PlayerRPGData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        String classId = data.getCurrentClass();
        Optional<RPGClass> rpgClassOpt = ClassRegistry.getClass(classId);
        String className = rpgClassOpt.map(RPGClass::getName).orElse(classId);
        
        source.sendSuccess(() -> Component.literal("=== RPG Info for " + player.getName().getString() + " ==="), false);
        source.sendSuccess(() -> Component.literal("Class: " + className), false);
        source.sendSuccess(() -> Component.literal("Class Level: " + data.getClassLevel()), false);
        source.sendSuccess(() -> Component.literal("Class XP: " + data.getClassExperience()), false);
        source.sendSuccess(() -> Component.literal("Skill Points: " + data.getAvailableSkillPoints()), false);
        source.sendSuccess(() -> Component.literal("Stat Points: " + data.getAvailableStatPoints()), false);
        source.sendSuccess(() -> Component.literal("Mana: " + data.getMana() + "/" + data.getMaxMana()), false);
        return 1;
    }
}