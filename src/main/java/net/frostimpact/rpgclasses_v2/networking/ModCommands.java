package net.frostimpact.rpgclasses_v2.networking;

import com.mojang.brigadier. CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.frostimpact.rpgclasses_v2.entity.custom.CustomEnemy;
import net.frostimpact.rpgclasses_v2.entity.custom.CustomEntityRegistry;
import net.frostimpact.rpgclasses_v2.entity.custom.CustomEnemySpawner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net. minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft. world.phys.Vec3;
import net.neoforged.bus.api. SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net. neoforged. neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

@EventBusSubscriber(modid = "rpgclasses_v2")
public class ModCommands {

    private static final SuggestionProvider<CommandSourceStack> ENEMY_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(CustomEntityRegistry.getAllEnemyIds(), builder);
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /rpgclasses summon <enemy_id>
        dispatcher. register(Commands.literal("rpgclasses")
            .requires(source -> source.hasPermission(2)) // Requires operator permission
            .then(Commands.literal("summon")
                .then(Commands.argument("enemy_id", StringArgumentType.word())
                    . suggests(ENEMY_SUGGESTIONS)
                    .executes(context -> {
                        String enemyId = StringArgumentType.getString(context, "enemy_id");
                        return summonEnemy(context. getSource(), enemyId);
                    })
                )
            )
            .then(Commands.literal("list")
                .executes(context -> {
                    return listEnemies(context.getSource());
                })
            )
        );
    }

    private static int summonEnemy(CommandSourceStack source, String enemyId) {
        Optional<CustomEnemy> enemyOpt = CustomEntityRegistry.getEnemy(enemyId);
        
        if (enemyOpt. isEmpty()) {
            source.sendFailure(Component.literal("Unknown enemy:  " + enemyId));
            source.sendFailure(Component.literal("Use /rpgclasses list to see available enemies"));
            return 0;
        }

        CustomEnemy enemy = enemyOpt.get();
        ServerLevel level = source. getLevel();
        Vec3 pos = source.getPosition();

        // Spawn the custom enemy
        boolean success = CustomEnemySpawner.spawn(enemy, level, new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
        
        if (success) {
            source.sendSuccess(() -> Component.literal("Summoned " + enemy.getDisplayName()), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to spawn " + enemy.getDisplayName()));
            return 0;
        }
    }

    private static int listEnemies(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Available Custom Enemies ==="), false);
        
        for (String id : CustomEntityRegistry. getAllEnemyIds()) {
            Optional<CustomEnemy> enemy = CustomEntityRegistry. getEnemy(id);
            enemy.ifPresent(e -> {
                source.sendSuccess(() -> Component.literal("  - " + id + " (" + e.getDisplayName() + ")"), false);
            });
        }
        
        return 1;
    }
}