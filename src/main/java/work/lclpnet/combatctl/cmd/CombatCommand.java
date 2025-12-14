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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.combatctl.config.ClientConfig;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.config.ConfigOption;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.impl.CombatStyles;
import work.lclpnet.kibu.config.ConfigAccess;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static me.lucko.fabric.api.permissions.v0.Permissions.require;
import static net.minecraft.ChatFormatting.ITALIC;
import static net.minecraft.ChatFormatting.YELLOW;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static work.lclpnet.combatctl.CCModInit.permission;

public class CombatCommand {

    private static final String VALUE_NAME = "value";

    private final ModTranslations translations;
    private final CombatControlConfig config;
    private final List<ConfigOption.Instance> options;
    private final DynamicCommandExceptionType unknownStyleError;
    private final Component missingPermission, invalidValue;

    public CombatCommand(ModTranslations translations, ConfigAccess<CombatControlConfig> configManager) {
        this.translations = translations;
        this.config = configManager.config();

        options = ConfigOption.instanceTree(CombatControlConfig.class)
                .stream()
                .filter(inst -> inst.option().srcClass() != ClientConfig.class)
                .filter(inst -> !Modifier.isTransient(inst.option().field().getModifiers()))
                .map(inst -> {
                    ConfigOption opt = inst.option();
                    int len = inst.srcPath().size();

                    if (opt.srcClass() == PlayerConfig.class && len >= 1) {
                        return new ConfigOption.Instance(opt, inst.srcPath().subList(1, len), inst.path());
                    }

                    return inst;
                })
                .sorted(Comparator.comparing(inst -> inst.option().field().getName()))
                .toList();

        unknownStyleError = new DynamicCommandExceptionType(arg -> translations.fallback("argument.combat_style.notfound", arg));
        missingPermission = translations.fallback("error.combat-control.missing_permission_cmd");
        invalidValue = translations.fallback("error.combat-control.invalid_value");
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("combat")
                .requires(require(permission("command.combat"), 2))
                .then(thenEach(literal("set")
                        .requires(require(permission("command.combat.set"), 2)), options, inst -> inst.option().argumentType()
                        .map(arg -> literal(inst.option().field().getName())
                                .requires(require(permission("command.combat.set." + inst.option().field().getName()), 2))
                                .then(thenIf(argument(VALUE_NAME, arg)
                                                .suggests(inst.option().suggestions())
                                                .executes(ctx -> setGlobalOpt(ctx, inst)),
                                        inst.option().srcClass() == PlayerConfig.class,
                                        argument("targets", EntityArgument.players())
                                                .executes(ctx -> setOpt(ctx, inst)))))))
                .then(thenEach(literal("get")
                        .requires(require(permission("command.combat.get"), 2)), options, inst -> Optional.of(thenIf(
                        literal(inst.option().field().getName())
                                .requires(require(permission("command.combat.get." + inst.option().field().getName()), 2))
                                .executes(ctx -> getGlobalOpt(ctx, inst)),
                        inst.option().srcClass() == PlayerConfig.class,
                        argument("target", EntityArgument.player())
                                .executes(ctx -> getOpt(ctx, inst))))))
                .then(literal("style")
                        .requires(require(permission("command.combat.style"), 2))
                        .then(argument("style", IdentifierArgument.id())
                                .suggests(CombatCommand::suggestStyles)
                                .executes(this::applyGlobalStyle)
                                .then(argument("targets", EntityArgument.players())
                                        .executes(this::applyStyle)))));
    }

    private int applyGlobalStyle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!Permissions.check(ctx.getSource(), permission("command.combat.style.global"), 2)) {
            ctx.getSource().sendFailure(missingPermission);
            return 0;
        }

        var style = combatStyleArg(ctx);

        CombatControl.get(ctx.getSource().getServer()).setStyle(style.value());
        ctx.getSource().sendSuccess(() -> translations.fallback("commands.combat.style.global", style.key().toString()), true);

        return 1;
    }

    private int applyStyle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var style = combatStyleArg(ctx);
        var players = EntityArgument.getPlayers(ctx, "targets");

        CombatControl cc = CombatControl.get(ctx.getSource().getServer());

        players.forEach(player -> cc.setStyle(player, style.value()));

        ctx.getSource().sendSuccess(() -> players.size() == 1
                ? translations.fallback("commands.combat.style.single", Component.literal(players.iterator().next().getScoreboardName()).withStyle(YELLOW), style.key().toString())
                : translations.fallback("commands.combat.style.multiple", players.size(), style.key().toString()), true);

        return 1;
    }

    private int getGlobalOpt(CommandContext<CommandSourceStack> ctx, ConfigOption.Instance inst) {
        ConfigOption opt = inst.option();
        Object value = get(inst, null);

        ctx.getSource().sendSuccess(() -> translations.fallback("commands.combat.get",
                translations.optionTitle(inst).withStyle(ITALIC),
                opt.asText(value, inst, translations).withStyle(ITALIC)), false);

        return code(value);
    }

    private int getOpt(CommandContext<CommandSourceStack> ctx, ConfigOption.Instance inst) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        ConfigOption opt = inst.option();

        Object value = get(inst, player);

        ctx.getSource().sendSuccess(() -> translations.fallback("commands.combat.get.player",
                translations.optionTitle(inst).withStyle(ITALIC),
                Component.literal(player.getScoreboardName()).withStyle(YELLOW),
                opt.asText(value, inst, translations).withStyle(ITALIC)), false);

        return code(value);
    }

    private int setGlobalOpt(CommandContext<CommandSourceStack> ctx, ConfigOption.Instance inst) {
        ConfigOption opt = inst.option();
        String name = opt.field().getName();

        if (!Permissions.check(ctx.getSource(), permission("command.combat.set.global." + name), 2)) {
            ctx.getSource().sendFailure(missingPermission);
            return 0;
        }

        Object value = opt.argumentValue(ctx, VALUE_NAME);

        if (value == null) {
            ctx.getSource().sendFailure(invalidValue);
            return 0;
        }

        // set global player config
        set(inst, value, null);

        // set online player configs
        if (opt.srcClass() == PlayerConfig.class) {
            for (ServerPlayer player : PlayerLookup.all(ctx.getSource().getServer())) {
                set(inst, value, player);
            }
        }

        CombatControl.get(ctx.getSource().getServer()).update();

        ctx.getSource().sendSuccess(() -> translations.fallback("commands.combat.set",
                translations.optionTitle(inst).withStyle(ITALIC),
                opt.asText(value, inst, translations).withStyle(ITALIC)), false);

        return 1;
    }

    private int setOpt(CommandContext<CommandSourceStack> ctx, ConfigOption.Instance inst) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "targets");

        ConfigOption opt = inst.option();
        Object value = opt.argumentValue(ctx, VALUE_NAME);

        if (value == null) {
            ctx.getSource().sendFailure(invalidValue);
            return 0;
        }

        var control = CombatControl.get(ctx.getSource().getServer());

        for (ServerPlayer player : players) {
            set(inst, value, player);
            control.update(player);
        }

        var name = translations.optionTitle(inst).withStyle(ITALIC);
        var val = opt.asText(value, inst, translations).withStyle(ITALIC);

        ctx.getSource().sendSuccess(() -> players.size() == 1
                ? translations.fallback("commands.combat.set.single", name, Component.literal(players.iterator().next().getScoreboardName()).withStyle(YELLOW), val)
                : translations.fallback("commands.combat.set.multiple", name, players.size(), val), false);

        return 1;
    }

    private void set(ConfigOption.Instance inst, Object val, @Nullable ServerPlayer player) {
        if (inst.option().srcClass() != PlayerConfig.class) {
            inst.set(config, val);
            return;
        }

        inst.set(playerCfg(player), val);
    }

    private Object get(ConfigOption.Instance inst, @Nullable ServerPlayer player) {
        if (inst.option().srcClass() != PlayerConfig.class) {
            return inst.get(config);
        }

        return inst.get(playerCfg(player));
    }

    private PlayerConfig playerCfg(@Nullable ServerPlayer player) {
        return player != null
                ? CombatControl.get(player.level().getServer()).playerConfig(player)
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

    private Pair<Identifier, CombatStyle> combatStyleArg(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(ctx, "style");

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

    private static CompletableFuture<Suggestions> suggestStyles(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        CombatStyles.registry().keySet().stream()
                .map(Identifier::toString)
                .forEach(builder::suggest);

        return builder.buildFuture();
    }
}
