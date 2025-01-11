package work.lclpnet.combatctl.config;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
        if (field.getType() == boolean.class) {
            return Optional.of(BoolArgumentType.bool());
        }

        return Optional.empty();
    }

    public Object argumentValue(CommandContext<?> ctx, String name) {
        if (field.getType() == boolean.class) {
            return BoolArgumentType.getBool(ctx, name);
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

    public String stringify(Object val) {
        if (field.getType() == boolean.class) {
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

    public static List<ConfigOption> allOf(Class<?> srcClass, List<ConfigOption> options) {
        for (Field field : srcClass.getDeclaredFields()) {
            var type = field.getType();

            if (isValue(type)) {
                options.add(new ConfigOption(field, srcClass));
            } else {
                allOf(type, options);
            }
        }

        return options;
    }
}
