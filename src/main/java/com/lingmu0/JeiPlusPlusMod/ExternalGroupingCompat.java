package com.lingmu0.JeiPlusPlusMod;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Locale;
import java.util.Set;

/** Detects client-side JEI grouping addons that would conflict with JEI++. */
final class ExternalGroupingCompat {
    private static final Set<String> KNOWN_MOD_IDS = Set.of(
        "collapsible_groups",
        "jei_tag_groups",
        "jeitaggroups",
        "jei_groups",
        "jeigroups"
    );

    private static final boolean GROUPING_MOD_LOADED = detectGroupingMod();

    private ExternalGroupingCompat() {
    }

    static boolean isGroupingModLoaded() {
        return GROUPING_MOD_LOADED;
    }

    private static boolean detectGroupingMod() {
        FabricLoader modList = FabricLoader.getInstance();
        for (String modId : KNOWN_MOD_IDS) {
            if (modList.isModLoaded(modId)) {
                return true;
            }
        }
        // Keep compatibility with renamed builds whose visible name remains
        // JEI Tag Groups, JEI Groups, or Collapsible Groups.
        return modList.getAllMods().stream()
            .map(info -> normalize(info.getMetadata().getName()))
            .anyMatch(name -> name.equals("jeitaggroups")
                || name.equals("jeigroups")
                || name.equals("collapsiblegroups")
                || name.equals("collapsiblegroupsjei"));
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
