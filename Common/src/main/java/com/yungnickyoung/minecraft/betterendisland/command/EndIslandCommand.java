package com.yungnickyoung.minecraft.betterendisland.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yungnickyoung.minecraft.betterendisland.world.IDragonFight;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class EndIslandCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("end_island")
                .requires((source) -> source.hasPermission(2))
                .then(Commands.literal("reset")
                        .executes(context -> executeReset(context.getSource())))
        );
    }

    public static int executeReset(CommandSourceStack commandSource) {
        ServerLevel serverLevel = commandSource.getServer().getLevel(Level.END);
        if (serverLevel == null) {
            commandSource.sendFailure(new TextComponent("Could not find the End dimension."));
            return -1;
        }
        if (serverLevel.dragonFight() == null) {
            commandSource.sendFailure(new TextComponent("Could not find the dragon fight."));
            return -1;
        }
        IDragonFight dragonFight = (IDragonFight) serverLevel.dragonFight(); // Cast to custom interface
        dragonFight.betterendisland$reset();
        commandSource.sendSuccess(new TextComponent("Ender Dragon fight has been reset."), false);
        return 1;
    }
}
