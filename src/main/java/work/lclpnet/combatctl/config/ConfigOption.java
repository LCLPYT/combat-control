package work.lclpnet.combatctl.config;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.cmd.ModTranslations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.text.Text.literal;
import static net.minecraft.util.Formatting.GREEN;
import static net.minecraft.util.Formatting.RED;

public class ConfigOption {

    private final Field field;
    private final Class<?> srcClass;
    private final @Nullable Method getter, setter;

    public ConfigOption(Field field, Class<?> srcClass) {
        this.field = field;
        this.srcClass = srcClass;

        getter = findGetter(field, srcClass);
        setter = findSetter(field, srcClass);
    }

    public Field field() {
        return field;
    }

    public Class<?> srcClass() {
        return srcClass;
    }

    public Optional<ArgumentType<?>> argumentType() {
        var type = field.getType();

        if (type == boolean.class) {
            return Optional.of(BoolArgumentType.bool());
        }

        if (type.isEnum()) {
            return Optional.of(StringArgumentType.word());
        }

        return Optional.empty();
    }

    public @Nullable SuggestionProvider<ServerCommandSource> suggestions() {
        Class<?> type = field.getType();

        if (type.isEnum()) {
            return (context, builder) -> {
                for (Field typeField : type.getFields()) {
                    builder.suggest(typeField.getName());
                }

                return builder.buildFuture();
            };
        }

        return null;
    }

    public Object argumentValue(CommandContext<?> ctx, String name) {
        var type = field.getType();

        if (type == boolean.class) {
            return BoolArgumentType.getBool(ctx, name);
        }

        if (type.isEnum()) {
            String strVal = StringArgumentType.getString(ctx, name);

            for (Field typeField : type.getFields()) {
                if (!typeField.getName().equals(strVal)) continue;

                try {
                    return typeField.get(null);
                } catch (Throwable t) {
                    return null;
                }
            }

            return null;
        }

        return null;
    }

    public Object get(Object src) {
        if (getter == null) return null;

        try {
            return getter.invoke(src);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public void set(Object src, Object value) {
        if (setter == null) return;

        try {
            setter.invoke(src, value);
        } catch (ReflectiveOperationException ignored) {}
    }

    public MutableText asText(Object val, Instance inst, ModTranslations translations) {
        var type = field.getType();

        if (type == boolean.class) {
            boolean bool = val instanceof Boolean b && b;
            return literal(Boolean.toString(bool)).formatted(bool ? GREEN : RED);
        }

        if (type.isEnum() && val instanceof Enum<?> enumVal) {
            return translations.enumName(enumVal, inst);
        }

        return literal("unknown");
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

    private static @Nullable Method findGetter(Field field, Class<?> srcClass) {
        String getterName = (field.getType() == boolean.class ? "is" : "get") + ucfirst(field.getName());

        try {
            Method getter = srcClass.getDeclaredMethod(getterName);
            getter.setAccessible(true);

            return getter;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static @Nullable Method findSetter(Field field, Class<?> srcClass) {
        String setterName = "set" + ucfirst(field.getName());

        try {
            Method setterMethod = srcClass.getDeclaredMethod(setterName, field.getType());
            setterMethod.setAccessible(true);

            return setterMethod;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static boolean isValue(Class<?> type) {
        return type.isPrimitive() || type.isArray() || type.isEnum() || type.isAssignableFrom(Collection.class);
    }

    public static List<Instance> instanceTree(Class<?> srcClass) {
        List<Instance> list = new ArrayList<>();
        _instanceTree(srcClass, list, new LinkedList<>());
        return list;
    }

    private static void _instanceTree(Class<?> srcClass, List<Instance> list, LinkedList<Field> path) {
        for (Field field : srcClass.getDeclaredFields()) {
            if (isValue(field.getType())) {
                list.add(new Instance(new ConfigOption(field, srcClass), path));
                continue;
            }

            var nextPath = new LinkedList<>(path);
            nextPath.add(field);

            _instanceTree(field.getType(), list, nextPath);
        }
    }

    public record Instance(ConfigOption option, List<Field> srcPath, String path) {

        public Instance(ConfigOption option, List<Field> srcPath) {
            this(option, srcPath, srcPath.stream()
                    .map(Field::getName)
                    .collect(Collectors.joining(".")) + "." + option.field().getName());
        }

        public Object get(Object src) {
            src = applyPath(src);

            return src != null ? option.get(src) : null;
        }

        public void set(Object src, Object val) {
            src = applyPath(src);

            if (src != null) {
                option.set(src, val);
            }
        }

        private @Nullable Object applyPath(Object src) {
            try {
                for (Field field : srcPath) {
                    src = field.get(src);
                }
            } catch (IllegalAccessException e) {
                return null;
            }

            return src;
        }
    }
}
