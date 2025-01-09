package work.lclpnet.combatctl.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.impl.CombatStyles;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CombatCommand {

    private static final String VALUE_NAME = "value";

    private final ModTranslations translations;
    private final List<Option> options;
    private final DynamicCommandExceptionType unknownStyleError;

    public CombatCommand(ModTranslations translations) {
        this.translations = translations;
        options = loadOptions();

        unknownStyleError = new DynamicCommandExceptionType(arg -> translations.fallback("argument.combat_style.notfound", arg));
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("combat")
                .requires(s -> s.hasPermissionLevel(2))
                .then(thenEach(literal("set"), options, opt -> opt.createArg()
                        .map(arg -> literal(opt.name()).then(thenIf(
                                argument(VALUE_NAME, arg)
                                        .executes(ctx -> setGlobalOpt(ctx, opt)),
                                !opt.global(),
                                argument("targets", EntityArgumentType.players())
                                        .executes(ctx -> setOpt(ctx, opt)))))))
                .then(thenEach(literal("get"), options, opt -> Optional.of(thenIf(
                        literal(opt.name())
                                .executes(ctx -> getGlobalOpt(ctx, opt)),
                        !opt.global(),
                        argument("target", EntityArgumentType.player())
                                .executes(ctx -> getOpt(ctx, opt))))))
                .then(literal("style")
                        .then(argument("style", IdentifierArgumentType.identifier())
                                .suggests(CombatCommand::suggestStyles)
                                .executes(this::applyGlobalStyle)
                                .then(argument("targets", EntityArgumentType.players())
                                        .executes(this::applyStyle)))));
    }

    private int applyGlobalStyle(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
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

    private int getGlobalOpt(CommandContext<ServerCommandSource> ctx, Option opt) {
        Object value = opt.getValue(ctx.getSource().getServer(), null);

        ctx.getSource().sendFeedback(() -> translations.fallback("commands.combat.get", opt.name, opt.stringify(value)), false);

        return code(value);
    }

    private int getOpt(CommandContext<ServerCommandSource> ctx, Option opt) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "target");
        Object value = opt.getValue(player.getServer(), player);

        ctx.getSource().sendFeedback(() -> translations.fallback("commands.combat.get.player", opt.name, player.getNameForScoreboard(), opt.stringify(value)), false);

        return code(value);
    }

    private int setGlobalOpt(CommandContext<ServerCommandSource> ctx, Option opt) {
        Object value = opt.argValue(ctx);
        opt.setValue(ctx.getSource().getServer(), value, List.of());

        ctx.getSource().sendFeedback(() -> translations.fallback("commands.combat.set", opt.name, opt.stringify(value)), false);

        return 1;
    }

    private int setOpt(CommandContext<ServerCommandSource> ctx, Option opt) throws CommandSyntaxException {
        var players = EntityArgumentType.getPlayers(ctx, "targets");
        Object value = opt.argValue(ctx);

        opt.setValue(ctx.getSource().getServer(), value, players);

        ctx.getSource().sendFeedback(() -> players.size() == 1
                ? translations.fallback("commands.combat.set.single", opt.name, players.iterator().next().getNameForScoreboard(), opt.stringify(value))
                : translations.fallback("commands.combat.set.multiple", opt.name, players.size(), opt.stringify(value)), false);

        return 1;
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

    private static List<Option> loadOptions() {
        var options = new ArrayList<Option>();

        for (Field field : PlayerConfig.class.getDeclaredFields()) {
            options.add(new Option(field.getName(), field.getType(), false));
        }

        for (Field field : GlobalConfig.class.getDeclaredFields()) {
            options.add(new Option(field.getName(), field.getType(), true));
        }

        options.sort(Comparator.comparing(Option::name));

        return options;
    }

    private record Option(String name, Class<?> type, boolean global) {

        public Optional<ArgumentType<?>> createArg() {
            if (type == boolean.class) {
                return Optional.of(BoolArgumentType.bool());
            }

            return Optional.empty();
        }

        public Object argValue(CommandContext<ServerCommandSource> ctx) {
            if (type == boolean.class) {
                return BoolArgumentType.getBool(ctx, VALUE_NAME);
            }

            return null;
        }

        public Object getValue(MinecraftServer server, @Nullable ServerPlayerEntity target) {
            CombatControl cc = CombatControl.get(server);
            Object src = global
                    ? cc.globalConfig()
                    : (target != null ? cc.playerConfig(target) : cc.playerConfig());

            try {
                final String getterName = (type == boolean.class ? "is" : "get") + ucfirst(name);

                Method getter = src.getClass().getDeclaredMethod(getterName);
                getter.setAccessible(true);

                return getter.invoke(src);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        public void setValue(MinecraftServer server, Object value, Collection<ServerPlayerEntity> targets) {
            var setter = setter();

            if (setter == null) return;

            CombatControl cc = CombatControl.get(server);

            if (global) {
                setter.accept(cc.globalConfig(), value);
                cc.update();
                return;
            }

            if (targets.isEmpty()) {
                setter.accept(cc.playerConfig(), value);

                for (ServerPlayerEntity player : PlayerLookup.all(server)) {
                    setter.accept(cc.playerConfig(player), value);
                }

                cc.update();
                return;
            }

            for (ServerPlayerEntity player : targets) {
                setter.accept(cc.playerConfig(player), value);
                cc.update(player);
            }
        }

        private @Nullable BiConsumer<Object, Object> setter() {
            try {
                Class<?> cls = global ? GlobalConfig.class : PlayerConfig.class;

                Method setterMethod = cls.getDeclaredMethod("set" + ucfirst(name), type);
                setterMethod.setAccessible(true);

                return (cfg, val) -> {
                    try {
                        setterMethod.invoke(cfg, val);
                    } catch (ReflectiveOperationException ignored) {}
                };
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        public String stringify(Object val) {
            if (type == boolean.class) {
                return Boolean.toString(val instanceof Boolean b && b);
            }

            return "unknown";
        }

        private static String ucfirst(String s) {
            int len = s.length();

            if (len == 0) {
                return s;
            }

            char c = Character.toTitleCase(s.charAt(0));

            if (len == 1) {
                return String.valueOf(c);
            }

            return c + s.substring(1);
        }
    }
}
