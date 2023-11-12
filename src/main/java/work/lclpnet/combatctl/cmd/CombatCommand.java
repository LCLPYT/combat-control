package work.lclpnet.combatctl.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.CombatStyle;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CombatCommand {

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("combat")
                .requires(s -> s.hasPermissionLevel(2))
                .then(literal("old")
                        .executes(this::setOldCombat)
                        .then(argument("targets", EntityArgumentType.players())
                                .executes(this::setOldCombatFor)))
                .then(literal("modern")
                        .executes(this::setModernCombat)
                        .then(argument("targets", EntityArgumentType.players())
                                .executes(this::setModernCombatFor))));
    }

    private int setOldCombat(CommandContext<ServerCommandSource> ctx) {
        setGlobal(ctx, CombatStyle.OLD, "old (1.8)");
        return 1;
    }

    private int setOldCombatFor(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        setMulti(ctx, CombatStyle.OLD, "old (1.8)");
        return 1;
    }

    private int setModernCombat(CommandContext<ServerCommandSource> ctx) {
        setGlobal(ctx, CombatStyle.MODERN, "modern (1.9+)");
        return 1;
    }

    private int setModernCombatFor(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        setMulti(ctx, CombatStyle.MODERN, "modern (1.9+)");
        return 1;
    }

    private static void setGlobal(CommandContext<ServerCommandSource> ctx, CombatStyle combatStyle, String name) {
        ServerCommandSource source = ctx.getSource();

        CombatControl.get(source.getServer()).setStyle(combatStyle);

        source.sendMessage(Text.literal("Changed the combat system to the %s system".formatted(name))
                .formatted(Formatting.GREEN));
    }

    private static void setMulti(CommandContext<ServerCommandSource> ctx, CombatStyle combatStyle, String name) throws CommandSyntaxException {
        var targets = EntityArgumentType.getPlayers(ctx, "targets");
        ServerCommandSource source = ctx.getSource();

        for (ServerPlayerEntity player : targets) {
            CombatControl.get(source.getServer()).setStyle(player, combatStyle);
        }

        int count = targets.size();

        MutableText msg;
        if (count == 1) {
            msg = Text.literal("Changed the combat system to the %s system for ".formatted(name))
                    .formatted(Formatting.GREEN)
                    .append(Text.literal(targets.iterator().next().getDisplayName().getString())
                            .formatted(Formatting.YELLOW));
        } else {
            msg = Text.literal("Changed the combat system to the %s system for ".formatted(name))
                    .formatted(Formatting.GREEN)
                    .append(Text.literal(Integer.toString(count))
                            .formatted(Formatting.YELLOW))
                    .append(Text.literal(" players").formatted(Formatting.GREEN));
        }

        source.sendMessage(msg);
    }
}
