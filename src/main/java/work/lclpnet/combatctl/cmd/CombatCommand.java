package work.lclpnet.combatctl.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.Pair;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.combatctl.config.*;
import work.lclpnet.combatctl.impl.CombatStyles;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static me.lucko.fabric.api.permissions.v0.Permissions.require;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static work.lclpnet.combatctl.CCModInit.permission;

public class CombatCommand {

    private static final String VALUE_NAME = "value";

    private final ModTranslations translations;
    private final CombatControlConfig config;
    private final List<ConfigOption.Instance> options;
    private final DynamicCommandExceptionType unknownStyleError;
    private final Text missingPermission;

    public CombatCommand(ModTranslations translations, ConfigAccess<CombatControlConfig> configManager) {
        this.translations = translations;
        this.config = configManager.config();

        options = ConfigOption.instanceTree(CombatControlConfig.class)
                .stream()
                .filter(inst -> inst.option().srcClass() != ClientConfig.class)
                .map(inst -> {
                    ConfigOption opt = inst.option();
                    int len = inst.srcPath().size();

                    if (opt.srcClass() == PlayerConfig.class && len >= 1) {
                        return new ConfigOption.Instance(opt, inst.srcPath().subList(1, len));
                    }

                    return inst;
                })
                .sorted(Comparator.comparing(inst -> inst.option().field().getName()))
                .toList();

        unknownStyleError = new DynamicCommandExceptionType(arg -> translations.fallback("argument.combat_style.notfound", arg));
        missingPermission = translations.fallback("error.combat-control.missing_permission_cmd");
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("combat")
                .requires(require(permission("command.combat"), 2))
                .then(thenEach(literal("set")
                        .requires(require(permission("command.combat.set"), 2)), options, inst -> inst.option().argumentType()
                        .map(arg -> literal(inst.option().field().getName())
                                .requires(require(permission("command.combat.set." + inst.option().field().getName()), 2))
                                .then(thenIf(argument(VALUE_NAME, arg)
                                                .executes(ctx -> setGlobalOpt(ctx, inst)),
                                        inst.option().srcClass() == PlayerConfig.class,
                                        argument("targets", EntityArgumentType.players())
                                                .executes(ctx -> setOpt(ctx, inst)))))))
                .then(thenEach(literal("get")
                        .requires(require(permission("command.combat.get"), 2)), options, inst -> Optional.of(thenIf(
                        literal(inst.option().field().getName())
                                .requires(require(permission("command.combat.get." + inst.option().field().getName()), 2))
                                .executes(ctx -> getGlobalOpt(ctx, inst)),
                        inst.option().srcClass() == PlayerConfig.class,
                        argument("target", EntityArgumentType.player())
                                .executes(ctx -> getOpt(ctx, inst))))))
                .then(literal("style")
                        .requires(require(permission("command.combat.style"), 2))
                        .then(argument("style", IdentifierArgumentType.identifier())
                                .suggests(CombatCommand::suggestStyles)
                                .executes(this::applyGlobalStyle)
                                .then(argument("targets", EntityArgumentType.players())
                                        .executes(this::applyStyle)))));
    }

    private int applyGlobalStyle(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        if (!Permissions.check(ctx.getSource(), permission("command.combat.style.global"), 2)) {
            ctx.getSource().sendError(missingPermission);
            return 0;
        }

        var style = combatStyleArg(ctx);

        CombatControl.get(ctx.getSource().getServer()).setStyle(style.value());
        ctx.getSource().sendFeedback(() -> translations.fallback("commands.combat.style.global", style.key().toString()), true);

        return 1;
    }

    private int applyStyle(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var style = combatStyleArg(ctx);
        var players = EntityArgumentType.getPlayers(ctx, "targets");

        CombatControl cc = CombatControl.get(ctx.getSource().getServer());

        players.forEach(player -> cc.setStyle(player, style.value()));

        ctx.getSource().sendFeedback(() -> players.size() == 1
                ? translations.fallback("commands.combat.style.single", players.iterator().next().getNameForScoreboard(), style.key().toString())
                : translations.fallback("commands.combat.style.multiple", players.size(), style.key().toString()), true);

        return 1;
    }

    private int getGlobalOpt(CommandContext<ServerCommandSource> ctx, ConfigOption.Instance inst) {
        ConfigOption opt = inst.option();
        Object value = get(inst, null);

        ctx.getSource().sendFeedback(() -> translations.fallback("commands.combat.get", opt.field().getName(), opt.stringify(value)), false);

        return code(value);
    }

    private int getOpt(CommandContext<ServerCommandSource> ctx, ConfigOption.Instance inst) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "target");
        ConfigOption opt = inst.option();

        Object value = get(inst, player);

        ctx.getSource().sendFeedback(() -> translations.fallback("commands.combat.get.player", opt.field().getName(), player.getNameForScoreboard(), opt.stringify(value)), false);

        return code(value);
    }

    private int setGlobalOpt(CommandContext<ServerCommandSource> ctx, ConfigOption.Instance inst) {
        ConfigOption opt = inst.option();
        String name = opt.field().getName();

        if (!Permissions.check(ctx.getSource(), permission("command.combat.set.global." + name), 2)) {
            ctx.getSource().sendError(missingPermission);
            return 0;
        }

        Object value = opt.argumentValue(ctx, VALUE_NAME);

        // set global player config
        set(inst, value, null);

        // set online player configs
        if (opt.srcClass() == PlayerConfig.class) {
            for (ServerPlayerEntity player : PlayerLookup.all(ctx.getSource().getServer())) {
                set(inst, value, player);
            }
        }

        CombatControl.get(ctx.getSource().getServer()).update();

        ctx.getSource().sendFeedback(() -> translations.fallback("commands.combat.set", name, opt.stringify(value)), false);

        return 1;
    }

    private int setOpt(CommandContext<ServerCommandSource> ctx, ConfigOption.Instance inst) throws CommandSyntaxException {
        var players = EntityArgumentType.getPlayers(ctx, "targets");

        ConfigOption opt = inst.option();
        Object value = opt.argumentValue(ctx, VALUE_NAME);

        var control = CombatControl.get(ctx.getSource().getServer());

        for (ServerPlayerEntity player : players) {
            set(inst, value, player);
            control.update(player);
        }

        String name = opt.field().getName();

        ctx.getSource().sendFeedback(() -> players.size() == 1
                ? translations.fallback("commands.combat.set.single", name, players.iterator().next().getNameForScoreboard(), opt.stringify(value))
                : translations.fallback("commands.combat.set.multiple", name, players.size(), opt.stringify(value)), false);

        return 1;
    }

    private void set(ConfigOption.Instance inst, Object val, @Nullable ServerPlayerEntity player) {
        if (inst.option().srcClass() != PlayerConfig.class) {
            inst.set(config, val);
            return;
        }

        inst.set(playerCfg(player), val);
    }

    private Object get(ConfigOption.Instance inst, @Nullable ServerPlayerEntity player) {
        if (inst.option().srcClass() != PlayerConfig.class) {
            return inst.get(config);
        }

        return inst.get(playerCfg(player));
    }

    private PlayerConfig playerCfg(@Nullable ServerPlayerEntity player) {
        return player != null
                ? CombatControl.get(player.getServer()).playerConfig(player)
                : config.player;
    }

    private static <S, B extends ArgumentBuilder<S, B>, T> B thenEach(B parent, Iterable<T> items, Function<T, Optional<ArgumentBuilder<S, ?>>> func) {
        for (T item : items) {
            func.apply(item).ifPresent(parent::then);
        }

        return parent;
    }

    private static <S, B extends ArgumentBuilder<S, B>> B thenIf(B parent, boolean cond, ArgumentBuilder<S, ?> child) {
        if (cond) {
            parent.then(child);
        }

        return parent;
    }

    private Pair<Identifier, CombatStyle> combatStyleArg(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "style");

        CombatStyle style = CombatStyles.registry().getOrDefault(id, null);

        if (style == null) {
            throw unknownStyleError.create(id);
        }

        return Pair.of(id, style);
    }

    private static int code(Object value) {
        return switch (value) {
            case Boolean ignored -> 1;
            case null, default -> 0;
        };
    }

    private static CompletableFuture<Suggestions> suggestStyles(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        CombatStyles.registry().keySet().stream()
                .map(Identifier::toString)
                .forEach(builder::suggest);

        return builder.buildFuture();
    }
}
