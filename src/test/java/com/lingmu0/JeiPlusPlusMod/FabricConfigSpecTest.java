package com.lingmu0.JeiPlusPlusMod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Headless config regression checks; deliberately never initializes Minecraft. */
public final class FabricConfigSpecTest {
    public static void main(String[] args) throws Exception {
        Path path = Path.of("build", "config-regression", UUID.randomUUID().toString(), "config.json");
        var builder = new FabricConfigSpec.Builder();
        var enabled = builder.define("enabled", true);
        var tabs = builder.defineList("tabs", List.of(), item -> item instanceof String);
        var nested = builder.push("defaultGroups").define("wood", true);
        builder.pop();
        var spec = builder.build(path);
        spec.load();
        check(Files.exists(path) && enabled.get() && nested.get(), "default creation");
        tabs.set(List.of("minecraft:tools", "minecraft:building_blocks"));
        tabs.save();
        check(Files.readString(path).contains("minecraft:tools"), "pin persistence");
        Files.writeString(path, "{\"enabled\": false, \"tabs\": [\"minecraft:food\"], \"defaultGroups\": {\"wood\": false}}");
        spec.load();
        check(!enabled.get() && !nested.get() && tabs.get().equals(List.of("minecraft:food")), "valid reload");
        Files.writeString(path, "{\"enabled\": true, \"tabs\": [4]}");
        spec.load();
        check(!enabled.get() && !nested.get(), "invalid reload stays atomic");
        check(Files.readString(path).contains("[4]"), "invalid file is not overwritten");
        Files.writeString(path, "{}");
        spec.load();
        check(enabled.get() && nested.get() && tabs.get().isEmpty(), "missing fields use defaults");
        try {
            enabled.set(null);
            throw new AssertionError("null accepted");
        } catch (IllegalArgumentException expected) {
            // Invalid updates are rejected.
        }
        var pins = new java.util.ArrayList<String>();
        pins.add("minecraft:tools");
        tabs.set(pins);
        pins.clear();
        check(tabs.get().equals(List.of("minecraft:tools")), "defensive list copy");
        System.out.println("Fabric configuration regression checks passed (8 cases).");
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
