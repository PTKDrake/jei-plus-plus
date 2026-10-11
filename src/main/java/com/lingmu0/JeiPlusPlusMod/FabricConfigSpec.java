package com.lingmu0.JeiPlusPlusMod;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Small native Fabric configuration, with defaults, validation and live reload. */
public final class FabricConfigSpec {
    private static final Logger LOGGER = LoggerFactory.getLogger("jei_plus_plus");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, ConfigValue<?>> values;
    private final Path path;
    private long lastCheck, lastModified = Long.MIN_VALUE;

    private FabricConfigSpec(Map<String, ConfigValue<?>> values) {
        this(values, FabricLoader.getInstance().getConfigDir().resolve("jei_plus_plus-client.json"));
    }

    private FabricConfigSpec(Map<String, ConfigValue<?>> values, Path path) {
        this.values = Map.copyOf(values);
        this.path = path;
        values.values().forEach(value -> value.owner = this);
    }

    public synchronized void load() {
        if (!Files.exists(path)) {
            save();
        } else {
            reload();
        }
    }

    private synchronized void reloadIfChanged() {
        long now = System.nanoTime();
        if (now - lastCheck < 1_000_000_000L) return;
        lastCheck = now;
        try {
            if (Files.exists(path) && Files.getLastModifiedTime(path).toMillis() != lastModified) reload();
        } catch (IOException exception) {
            LOGGER.warn("Could not check JEI++ config", exception);
        }
    }

    private void reload() {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            // Validate all values before replacing the active configuration.
            Map<ConfigValue<?>, Object> loaded = new LinkedHashMap<>();
            for (var entry : values.entrySet()) {
                JsonElement element = root;
                for (String part : entry.getKey().split("\\.")) {
                    element = element != null && element.isJsonObject() ? element.getAsJsonObject().get(part) : null;
                }
                loaded.put(entry.getValue(), entry.getValue().decode(element));
            }
            loaded.forEach(ConfigValue::replace);
            lastModified = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException | RuntimeException exception) {
            // Never overwrite a malformed user config; retain the last valid values.
            LOGGER.warn("Could not load JEI++ config {}; keeping previous settings", path, exception);
        }
    }

    public synchronized void save() {
        JsonObject root = new JsonObject();
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            JsonObject parent = root;
            String[] parts = entry.getKey().split("\\.");
            for (int index = 0; index < parts.length - 1; index++) {
                if (!parent.has(parts[index])) parent.add(parts[index], new JsonObject());
                parent = parent.getAsJsonObject(parts[index]);
            }
            parent.add(parts[parts.length - 1], GSON.toJsonTree(entry.getValue().value));
        });
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            lastModified = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            LOGGER.warn("Could not save JEI++ config {}", path, exception);
        }
    }

    public static class ConfigValue<T> {
        private final T defaultValue;
        private final Predicate<Object> validator;
        private volatile T value;
        private FabricConfigSpec owner;

        private ConfigValue(T defaultValue, Predicate<Object> validator) {
            this.defaultValue = defaultValue;
            this.value = defaultValue;
            this.validator = validator;
        }

        public T get() {
            if (owner != null) owner.reloadIfChanged();
            return value;
        }

        public void set(T value) {
            if (!validator.test(value)) throw new IllegalArgumentException("Invalid JEI++ config value");
            replace(value);
        }

        public void save() {
            if (owner != null) owner.save();
        }

        private Object decode(JsonElement element) {
            if (element == null) return defaultValue;
            Object decoded;
            if (defaultValue instanceof Boolean) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
                    throw new IllegalArgumentException("Expected a boolean");
                }
                decoded = element.getAsBoolean();
            } else {
                if (!element.isJsonArray()) throw new IllegalArgumentException("Expected a string list");
                List<String> strings = new ArrayList<>();
                for (JsonElement item : element.getAsJsonArray()) {
                    if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
                        throw new IllegalArgumentException("Expected a string");
                    }
                    strings.add(item.getAsString());
                }
                decoded = List.copyOf(strings);
            }
            if (!validator.test(decoded)) throw new IllegalArgumentException("Invalid JEI++ config value");
            return decoded;
        }

        @SuppressWarnings("unchecked")
        private void replace(Object value) {
            this.value = (T) (value instanceof List<?> list ? List.copyOf(list) : value);
        }
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {
        private BooleanValue(boolean value) {
            super(value, candidate -> candidate instanceof Boolean);
        }
    }

    public static final class Builder {
        private final Map<String, ConfigValue<?>> values = new LinkedHashMap<>();
        private final Deque<String> sections = new ArrayDeque<>();

        public Builder comment(String... ignored) { return this; }
        public Builder push(String section) { sections.addLast(section); return this; }
        public Builder pop() { sections.removeLast(); return this; }

        private String key(String name) {
            return sections.isEmpty() ? name : String.join(".", sections) + "." + name;
        }

        public BooleanValue define(String name, boolean defaultValue) {
            BooleanValue value = new BooleanValue(defaultValue);
            values.put(key(name), value);
            return value;
        }

        public ConfigValue<List<? extends String>> defineList(
                String name, List<? extends String> defaults, Predicate<Object> elementValidator) {
            ConfigValue<List<? extends String>> value = new ConfigValue<>(List.copyOf(defaults),
                candidate -> candidate instanceof List<?> list && list.stream().allMatch(elementValidator));
            values.put(key(name), value);
            return value;
        }

        public FabricConfigSpec build() { return new FabricConfigSpec(values); }
        FabricConfigSpec build(Path path) { return new FabricConfigSpec(values, path); }
    }
}
