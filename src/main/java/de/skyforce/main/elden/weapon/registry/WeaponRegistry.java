package de.skyforce.main.elden.weapon.registry;

import de.skyforce.main.elden.smithing.model.SmithingTrack;
import de.skyforce.main.elden.weapon.model.WeaponAttackStats;
import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.model.WeaponGuardStats;
import de.skyforce.main.elden.weapon.model.WeaponRequirements;
import de.skyforce.main.elden.weapon.model.WeaponScaling;
import de.skyforce.main.elden.weapon.model.WeaponScalingGrade;
import de.skyforce.main.elden.weapon.model.WeaponType;
import org.bukkit.Material;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class WeaponRegistry {

    private final Map<String, WeaponDefinition> weaponsById = new HashMap<>();

    public WeaponRegistry() {
        registerDefaults();
    }

    public void register(WeaponDefinition weapon) {
        Objects.requireNonNull(weapon, "weapon");

        String normalizedId = weapon.id().toLowerCase(Locale.ROOT);
        if (weaponsById.containsKey(normalizedId)) {
            throw new IllegalArgumentException("Weapon id already registered: " + weapon.id());
        }

        weaponsById.put(normalizedId, weapon);
    }

    public Optional<WeaponDefinition> getById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(weaponsById.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<WeaponDefinition> all() {
        return Collections.unmodifiableCollection(weaponsById.values());
    }

    private void registerDefaults() {
        register(new WeaponDefinition(
                "short_sword",
                "Short Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Kick",
                "0",
                "-",
                attack(102, 0, 0, 0, 0, 100),
                guard(42, 28, 28, 28, 28, 28),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        8,
                        10
                ),
                3.0
        ));

        register(new WeaponDefinition(
                "fire_knights_shortsword",
                "Fire Knight's Shortsword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Quickstep",
                "3",
                "-",
                attack(75, 0, 22, 0, 0, 110),
                guard(32, 18, 31, 18, 18, 15),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        8,
                        13,
                        0,
                        12,
                        0
                ),
                2.0
        ));

        register(new WeaponDefinition(
                "main_gauche",
                "Main-gauche",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Parry",
                "-",
                "-",
                attack(79, 0, 0, 0, 0, 110),
                guard(38, 22, 22, 22, 22, 16),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        7,
                        15
                ),
                2.5
        ));

        register(new WeaponDefinition(
                "dagger",
                "Dagger",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Quickstep",
                "3",
                "-",
                attack(74, 0, 0, 0, 0, 130),
                guard(35, 20, 20, 20, 20, 15),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        5,
                        9
                ),
                1.5
        ));

        register(new WeaponDefinition(
                "parrying_dagger",
                "Parrying Dagger",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Parry",
                "-",
                "-",
                attack(75, 0, 0, 0, 0, 110),
                guard(35, 20, 20, 20, 20, 15),
                WeaponScaling.strDex(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        5,
                        14
                ),
                1.5
        ));

        register(new WeaponDefinition(
                "misericorde",
                "Misericorde",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Quickstep",
                "3",
                "-",
                attack(92, 0, 0, 0, 0, 140),
                guard(36, 21, 21, 21, 21, 15),
                WeaponScaling.strDex(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        7,
                        12
                ),
                2.0
        ));

        register(new WeaponDefinition(
                "great_knife",
                "Great Knife",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Quickstep",
                "3",
                "Blood Loss (38)",
                attack(75, 0, 0, 0, 0, 110),
                guard(35, 20, 20, 20, 20, 15),
                WeaponScaling.strDex(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        6,
                        12
                ),
                1.5
        ));

        register(new WeaponDefinition(
                "bloodstained_dagger",
                "Bloodstained Dagger",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Quickstep",
                "3",
                "Blood Loss (38)",
                attack(81, 0, 0, 0, 0, 110),
                guard(36, 21, 21, 21, 21, 15),
                WeaponScaling.strDex(
                        WeaponScalingGrade.C,
                        WeaponScalingGrade.E
                ),
                WeaponRequirements.strDex(
                        9,
                        12
                ),
                2.0
        ));

        register(new WeaponDefinition(
                "erdsteel_dagger",
                "Erdsteel Dagger",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Quickstep",
                "3",
                "-",
                attack(67, 0, 0, 0, 0, 110),
                guard(36, 21, 21, 21, 21, 15),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        7,
                        12,
                        0,
                        14,
                        0
                ),
                2.0
        ));

        register(new WeaponDefinition(
                "wakizashi",
                "Wakizashi",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Quickstep",
                "3",
                "Blood Loss (38)",
                attack(94, 0, 0, 0, 0, 100),
                guard(42, 24, 24, 24, 24, 18),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        9,
                        13
                ),
                3.0
        ));

        register(new WeaponDefinition(
                "celebrants_sickle",
                "Celebrant's Sickle",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Quickstep",
                "3",
                "Generates runes on hit",
                attack(79, 0, 0, 0, 0, 100),
                guard(35, 20, 20, 20, 20, 15),
                WeaponScaling.strDex(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        6,
                        11
                ),
                1.5
        ));

        register(new WeaponDefinition(
                "ivory_sickle",
                "Ivory Sickle",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Quickstep",
                "3",
                "-",
                attack(60, 60, 0, 0, 0, 100),
                guard(26, 42, 15, 15, 15, 15),
                WeaponScaling.of(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        6,
                        11,
                        13,
                        0,
                        0
                ),
                2.0
        ));

        register(new WeaponDefinition(
                "crystal_knife",
                "Crystal Knife",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Quickstep",
                "3",
                "-",
                attack(82, 53, 0, 0, 0, 100),
                guard(32, 31, 18, 18, 18, 15),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        8,
                        12,
                        9,
                        0,
                        0
                ),
                2.0
        ));

        register(new WeaponDefinition(
                "scorpions_stinger",
                "Scorpion's Stinger",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Repeating Thrust",
                "7",
                "Scarlet Rot (55)",
                attack(79, 0, 0, 0, 0, 110),
                guard(38, 22, 22, 22, 22, 16),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        6,
                        12
                ),
                2.5
        ));

        register(new WeaponDefinition(
                "cinquedea",
                "Cinquedea",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Quickstep",
                "3",
                "Boosts Bestial Incantations",
                attack(98, 0, 0, 0, 0, 100),
                guard(43, 25, 25, 25, 25, 18),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.E
                ),
                WeaponRequirements.strDex(
                        10,
                        10
                ),
                3.5
        ));

        register(new WeaponDefinition(
                "glintstone_kris",
                "Glintstone Kris",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Glintstone Dart",
                "10 (-4)",
                "-",
                attack(57, 68, 0, 0, 0, 110),
                guard(23, 45, 14, 14, 14, 15),
                WeaponScaling.of(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        5,
                        12,
                        16,
                        0,
                        0
                ),
                SmithingTrack.SOMBER,
                1.5
        ));

        register(new WeaponDefinition(
                "reduvia",
                "Reduvia",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Reduvia Blood Blade",
                "6 (-6)",
                "Blood Loss (38)",
                attack(79, 0, 0, 0, 0, 110),
                guard(38, 22, 22, 22, 22, 16),
                WeaponScaling.of(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.of(
                        5,
                        13,
                        0,
                        0,
                        13
                ),
                SmithingTrack.SOMBER,
                2.5
        ));

        register(new WeaponDefinition(
                "blade_of_calling",
                "Blade of Calling",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Blade of Gold",
                "20",
                "-",
                attack(71, 0, 0, 0, 43, 110),
                guard(31, 18, 18, 18, 30, 15),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.C,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        6,
                        13,
                        0,
                        15,
                        0
                ),
                SmithingTrack.SOMBER,
                1.5
        ));

        register(new WeaponDefinition(
                "black_knife",
                "Black Knife",
                Material.IRON_SWORD,
                WeaponType.DAGGER,
                "Slash / Pierce",
                "Blade of Death",
                "25",
                "-",
                attack(66, 0, 0, 0, 65, 110),
                guard(26, 15, 15, 15, 42, 15),
                WeaponScaling.of(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        8,
                        12,
                        0,
                        18,
                        0
                ),
                SmithingTrack.SOMBER,
                2.0
        ));
        register(new WeaponDefinition(
                "velvet_sword_of_st_trina",
                "Velvet Sword of St Trina",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Mists of Eternal Sleep",
                "23",
                "Eternal Sleep",
                attack(95, 61, 0, 0, 0, 110),
                guard(37, 37, 25, 25, 25, 27),
                WeaponScaling.of(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        10,
                        12,
                        14,
                        0,
                        0
                ),
                SmithingTrack.SOMBER,
                2.5
        ));

        register(new WeaponDefinition(
                "stone_sheathed_sword",
                "Stone-Sheathed Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Square Off",
                "- (6 8)",
                "-",
                attack(119, 0, 0, 0, 0, 100),
                guard(54, 33, 33, 33, 33, 32),
                WeaponScaling.strDex(
                        WeaponScalingGrade.C,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        16,
                        8
                ),
                5.0
        ));

        register(new WeaponDefinition(
                "sword_of_light",
                "Sword of Light",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Light",
                "30",
                "-",
                attack(93, 0, 0, 0, 93, 100),
                guard(26, 15, 15, 15, 42, 31),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        14,
                        11,
                        0,
                        24,
                        0
                ),
                SmithingTrack.SOMBER,
                4.0
        ));

        register(new WeaponDefinition(
                "sword_of_darkness",
                "Sword of Darkness",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Darkness",
                "30",
                "-",
                attack(93, 0, 0, 0, 93, 100),
                guard(26, 15, 15, 15, 42, 31),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        14,
                        11,
                        0,
                        24,
                        0
                ),
                SmithingTrack.SOMBER,
                4.0
        ));

        register(new WeaponDefinition(
                "longsword",
                "Longsword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Square Off",
                "- (6 8)",
                "-",
                attack(110, 0, 0, 0, 0, 100),
                guard(45, 30, 30, 30, 30, 30),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        10,
                        10
                ),
                3.5
        ));

        register(new WeaponDefinition(
                "broadsword",
                "Broadsword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Square Off",
                "- (6 8)",
                "-",
                attack(117, 0, 0, 0, 0, 100),
                guard(47, 31, 31, 31, 31, 31),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.E
                ),
                WeaponRequirements.strDex(
                        10,
                        10
                ),
                4.0
        ));

        register(new WeaponDefinition(
                "weathered_straight_sword",
                "Weathered Straight Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Square Off",
                "- (6 8)",
                "-",
                attack(103, 0, 0, 0, 0, 100),
                guard(43, 29, 29, 29, 29, 27),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        7,
                        10
                ),
                3.0
        ));

        register(new WeaponDefinition(
                "lordsworns_straight_sword",
                "Lordsworn's Straight Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Square Off",
                "- (6 8)",
                "-",
                attack(115, 0, 0, 0, 0, 110),
                guard(45, 30, 30, 30, 30, 30),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        10,
                        10
                ),
                3.5
        ));

        register(new WeaponDefinition(
                "nobles_slender_sword",
                "Noble's Slender Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Square Off",
                "- (6 8)",
                "-",
                attack(101, 0, 0, 0, 0, 110),
                guard(43, 29, 29, 29, 29, 30),
                WeaponScaling.strDex(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        8,
                        11
                ),
                3.5
        ));

        register(new WeaponDefinition(
                "cane_sword",
                "Cane Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Square Off",
                "- (6 8)",
                "-",
                attack(96, 0, 0, 0, 0, 100),
                guard(41, 27, 27, 27, 27, 27),
                WeaponScaling.strDex(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        8,
                        11
                ),
                2.5
        ));

        register(new WeaponDefinition(
                "warhawks_talon",
                "Warhawk's Talon",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Spinning Slash",
                "6 (-12)",
                "-",
                attack(101, 0, 0, 0, 0, 100),
                guard(42, 28, 28, 28, 28, 30),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        10,
                        16
                ),
                3.0
        ));

        register(new WeaponDefinition(
                "lazuli_glintstone_sword",
                "Lazuli Glintstone Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Glintstone Pebble",
                "8 (-4)",
                "-",
                attack(79, 94, 0, 0, 0, 100),
                guard(30, 55, 25, 25, 25, 30),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        8,
                        9,
                        13,
                        0,
                        0
                ),
                SmithingTrack.SOMBER,
                3.5
        ));

        register(new WeaponDefinition(
                "carian_knights_sword",
                "Carian Knight's Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Carian Grandeur",
                "26",
                "-",
                attack(88, 88, 0, 0, 0, 100),
                guard(36, 52, 26, 26, 26, 31),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        10,
                        10,
                        18,
                        0,
                        0
                ),
                SmithingTrack.SOMBER,
                4.0
        ));

        register(new WeaponDefinition(
                "crystal_sword",
                "Crystal Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Spinning Slash",
                "6 (-12)",
                "-",
                attack(106, 68, 0, 0, 0, 100),
                guard(44, 44, 30, 30, 30, 33),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        13,
                        10,
                        15,
                        0,
                        0
                ),
                SmithingTrack.SOMBER,
                4.5
        ));

        register(new WeaponDefinition(
                "rotten_crystal_sword",
                "Rotten Crystal Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Spinning Slash",
                "6 (-12)",
                "Scarlet Rot",
                attack(102, 66, 0, 0, 0, 100),
                guard(48, 38, 31, 31, 31, 33),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        13,
                        10,
                        15,
                        0,
                        0
                ),
                SmithingTrack.SOMBER,
                4.5
        ));

        register(new WeaponDefinition(
                "miquellan_knights_sword",
                "Miquellan Knight's Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Sacred Blade",
                "19",
                "-",
                attack(105, 0, 0, 0, 68, 100),
                guard(40, 28, 28, 28, 40, 30),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        11,
                        11,
                        0,
                        16,
                        0
                ),
                SmithingTrack.SOMBER,
                3.5
        ));

        register(new WeaponDefinition(
                "ornamental_straight_sword",
                "Ornamental Straight Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Golden Tempering",
                "30",
                "-",
                attack(101, 0, 0, 0, 0, 110),
                guard(42, 28, 28, 28, 28, 28),
                WeaponScaling.strDex(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        10,
                        14
                ),
                3.0
        ));

        register(new WeaponDefinition(
                "golden_epitaph",
                "Golden Epitaph",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Last Rites",
                "25",
                "-",
                attack(85, 0, 0, 0, 85, 100),
                guard(25, 15, 15, 15, 40, 30),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        12,
                        10,
                        0,
                        14,
                        0
                ),
                3.5
        ));

        register(new WeaponDefinition(
                "sword_of_st_trina",
                "Sword of St Trina",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Mists of Slumber",
                "20",
                "Sleep",
                attack(107, 32, 0, 0, 0, 100),
                guard(39, 33, 27, 27, 27, 28),
                WeaponScaling.of(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        10,
                        12,
                        14,
                        0,
                        0
                ),
                3.0
        ));

        register(new WeaponDefinition(
                "regalia_of_eochaid",
                "Regalia of Eochaid",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Eochaid's Dancing Blade",
                "15",
                "-",
                attack(89, 57, 0, 0, 0, 100),
                guard(48, 40, 31, 31, 31, 33),
                WeaponScaling.of(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.of(
                        12,
                        18,
                        0,
                        0,
                        15
                ),
                5.5
        ));

        register(new WeaponDefinition(
                "coded_sword",
                "Coded Sword",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Unblockable Blade",
                "25",
                "-",
                attack(0, 0, 0, 0, 85, 100),
                guard(13, 22, 22, 22, 58, 27),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.B,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        0,
                        0,
                        0,
                        20,
                        0
                ),
                2.5
        ));

        register(new WeaponDefinition(
                "sword_of_night_and_flame",
                "Sword of Night and Flame",
                Material.STONE_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Standard / Pierce",
                "Night-and-Flame Stance",
                "- (26 32)",
                "-",
                attack(87, 56, 56, 0, 0, 100),
                guard(36, 42, 42, 26, 26, 31),
                WeaponScaling.of(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        12,
                        12,
                        24,
                        24,
                        0
                ),
                4.0
        ));

        register(new WeaponDefinition(
                "rapier",
                "Rapier",
                Material.STONE_SWORD,
                WeaponType.THRUSTING_SWORD,
                "Pierce",
                "Impaling Thrust",
                "9",
                "-",
                attack(95, 0, 0, 0, 0, 100),
                guard(37, 24, 24, 24, 24, 18),
                WeaponScaling.strDex(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        7,
                        12
                ),
                2.5
        ));

        register(new WeaponDefinition(
                "buckler",
                "Buckler",
                Material.SHIELD,
                WeaponType.SHIELD,
                "Strike",
                "Buckler Parry",
                "0",
                "-",
                attack(62, 0, 0, 0, 0, 100),
                guard(76, 38, 38, 38, 38, 52),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        8,
                        0,
                        0,
                        0,
                        0
                ),
                1.5
        ));

        register(new WeaponDefinition(
                "twinblade",
                "Twinblade",
                Material.STONE_SWORD,
                WeaponType.TWINBLADE,
                "Standard / Pierce",
                "Spinning Slash",
                "6",
                "-",
                attack(119, 0, 0, 0, 0, 100),
                guard(45, 30, 30, 30, 30, 28),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        10,
                        18
                ),
                7.0
        ));

        register(new WeaponDefinition(
                "club",
                "Club",
                Material.WOODEN_AXE,
                WeaponType.AXE,
                "Strike",
                "War Cry",
                "0",
                "-",
                attack(103, 0, 0, 0, 0, 100),
                guard(37, 22, 22, 22, 22, 20),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        10,
                        0,
                        0,
                        0,
                        0
                ),
                3.5
        ));

        register(new WeaponDefinition(
                "scimitar",
                "Scimitar",
                Material.GOLDEN_SWORD,
                WeaponType.STRAIGHT_SWORD,
                "Slash",
                "Spinning Slash",
                "6",
                "-",
                attack(106, 0, 0, 0, 0, 100),
                guard(40, 26, 26, 26, 26, 24),
                WeaponScaling.strDex(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        7,
                        13
                ),
                3.5
        ));

        register(new WeaponDefinition(
                "battle_axe",
                "Battle Axe",
                Material.IRON_AXE,
                WeaponType.AXE,
                "Standard",
                "Wild Strikes",
                "5",
                "-",
                attack(123, 0, 0, 0, 0, 100),
                guard(48, 31, 31, 31, 31, 31),
                WeaponScaling.strDex(
                        WeaponScalingGrade.C,
                        WeaponScalingGrade.E
                ),
                WeaponRequirements.strDex(
                        15,
                        8
                ),
                4.5
        ));

        register(new WeaponDefinition(
                "estoc",
                "Estoc",
                Material.IRON_SWORD,
                WeaponType.THRUSTING_SWORD,
                "Pierce",
                "Impaling Thrust",
                "9",
                "-",
                attack(107, 0, 0, 0, 0, 100),
                guard(39, 26, 26, 26, 26, 20),
                WeaponScaling.strDex(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        11,
                        13
                ),
                4.0
        ));

        register(new WeaponDefinition(
                "uchigatana",
                "Uchigatana",
                Material.IRON_SWORD,
                WeaponType.KATANA,
                "Slash / Pierce",
                "Unsheathe",
                "10",
                "Blood Loss (45)",
                attack(115, 0, 0, 0, 0, 100),
                guard(45, 30, 30, 30, 30, 30),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        11,
                        15
                ),
                5.5
        ));

        register(new WeaponDefinition(
                "halberd",
                "Halberd",
                Material.IRON_AXE,
                WeaponType.HALBERD,
                "Standard / Pierce",
                "Charge Forth",
                "9",
                "-",
                attack(125, 0, 0, 0, 0, 100),
                guard(52, 34, 34, 34, 34, 34),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        14,
                        12
                ),
                8.0
        ));

        register(new WeaponDefinition(
                "shortbow",
                "Shortbow",
                Material.BOW,
                WeaponType.BOW,
                "Pierce",
                "Barrage",
                "4",
                "-",
                attack(65, 0, 0, 0, 0, 100),
                guard(18, 12, 12, 12, 12, 10),
                WeaponScaling.strDex(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        8,
                        12
                ),
                3.0
        ));

        register(new WeaponDefinition(
                "longbow",
                "Longbow",
                Material.BOW,
                WeaponType.BOW,
                "Pierce",
                "Mighty Shot",
                "6",
                "-",
                attack(80, 0, 0, 0, 0, 100),
                guard(20, 13, 13, 13, 13, 10),
                WeaponScaling.strDex(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.C
                ),
                WeaponRequirements.strDex(
                        9,
                        14
                ),
                4.0
        ));

        register(new WeaponDefinition(
                "finger_seal",
                "Finger Seal",
                Material.BLAZE_ROD,
                WeaponType.STAFF,
                "Strike",
                "No Skill",
                "0",
                "Incantation Scaling",
                attack(25, 0, 0, 0, 52, 100),
                guard(20, 25, 20, 20, 36, 12),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.C,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        0,
                        0,
                        0,
                        10,
                        0
                ),
                SmithingTrack.SOMBER,
                1.5
        ));

        register(new WeaponDefinition(
                "scripture_wooden_shield",
                "Scripture Wooden Shield",
                Material.SHIELD,
                WeaponType.SHIELD,
                "Strike",
                "Shield Bash",
                "0",
                "-",
                attack(60, 0, 0, 0, 0, 100),
                guard(83, 42, 42, 42, 42, 48),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        8,
                        0,
                        0,
                        0,
                        0
                ),
                3.0
        ));

        register(new WeaponDefinition(
                "blue_crest_heater_shield",
                "Blue Crest Heater Shield",
                Material.SHIELD,
                WeaponType.SHIELD,
                "Strike",
                "Shield Bash",
                "0",
                "-",
                attack(75, 0, 0, 0, 0, 100),
                guard(100, 51, 51, 51, 51, 56),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        10,
                        0,
                        0,
                        0,
                        0
                ),
                3.5
        ));

        register(new WeaponDefinition(
                "large_leather_shield",
                "Large Leather Shield",
                Material.SHIELD,
                WeaponType.SHIELD,
                "Strike",
                "Shield Bash",
                "0",
                "-",
                attack(70, 0, 0, 0, 0, 100),
                guard(92, 48, 48, 48, 48, 54),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        8,
                        0,
                        0,
                        0,
                        0
                ),
                3.0
        ));

        register(new WeaponDefinition(
                "heater_shield",
                "Heater Shield",
                Material.SHIELD,
                WeaponType.SHIELD,
                "Strike",
                "Shield Bash",
                "0",
                "-",
                attack(75, 0, 0, 0, 0, 100),
                guard(100, 50, 50, 50, 50, 55),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        10,
                        0,
                        0,
                        0,
                        0
                ),
                3.5
        ));

        register(new WeaponDefinition(
                "rickety_shield",
                "Rickety Shield",
                Material.SHIELD,
                WeaponType.SHIELD,
                "Strike",
                "Parry",
                "0",
                "-",
                attack(50, 0, 0, 0, 0, 100),
                guard(78, 36, 36, 36, 36, 44),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        7,
                        0,
                        0,
                        0,
                        0
                ),
                2.5
        ));

        register(new WeaponDefinition(
                "red_thorn_roundshield",
                "Red Thorn Roundshield",
                Material.SHIELD,
                WeaponType.SHIELD,
                "Strike",
                "Parry",
                "0",
                "-",
                attack(63, 0, 0, 0, 0, 100),
                guard(89, 45, 45, 45, 45, 50),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        8,
                        0,
                        0,
                        0,
                        0
                ),
                2.8
        ));

        register(new WeaponDefinition(
                "rift_shield",
                "Rift Shield",
                Material.SHIELD,
                WeaponType.SHIELD,
                "Strike",
                "Shield Bash",
                "0",
                "Magic Guard",
                attack(68, 0, 0, 0, 0, 100),
                guard(92, 58, 48, 48, 48, 54),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        11,
                        0,
                        0,
                        0,
                        0
                ),
                4.2
        ));

        register(new WeaponDefinition(
                "riveted_wooden_shield",
                "Riveted Wooden Shield",
                Material.SHIELD,
                WeaponType.SHIELD,
                "Strike",
                "Shield Bash",
                "0",
                "-",
                attack(58, 0, 0, 0, 0, 100),
                guard(81, 40, 40, 40, 40, 46),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        8,
                        0,
                        0,
                        0,
                        0
                ),
                2.8
        ));

        register(new WeaponDefinition(
                "glintstone_staff",
                "Glintstone Staff",
                Material.BLAZE_ROD,
                WeaponType.STAFF,
                "Strike",
                "No Skill",
                "0",
                "Sorcery Scaling",
                attack(24, 58, 0, 0, 0, 100),
                guard(22, 40, 22, 22, 22, 14),
                WeaponScaling.of(
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.C,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        0,
                        0,
                        12,
                        0,
                        0
                ),
                SmithingTrack.SOMBER,
                2.5
        ));

        register(new WeaponDefinition(
                "short_spear",
                "Short Spear",
                Material.TRIDENT,
                WeaponType.SPEAR,
                "Pierce",
                "Charge Forth",
                "5",
                "-",
                attack(101, 0, 0, 0, 0, 100),
                guard(42, 28, 28, 28, 28, 30),
                WeaponScaling.strDex(
                        WeaponScalingGrade.D,
                        WeaponScalingGrade.D
                ),
                WeaponRequirements.strDex(
                        11,
                        10
                ),
                4.0
        ));

        register(new WeaponDefinition(
                "astrologers_staff",
                "Astrologer's Staff",
                Material.BLAZE_ROD,
                WeaponType.STAFF,
                "Strike",
                "No Skill",
                "0",
                "Sorcery Scaling",
                attack(29, 60, 0, 0, 0, 100),
                guard(25, 42, 25, 25, 25, 15),
                WeaponScaling.of(
                        WeaponScalingGrade.E,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.C,
                        WeaponScalingGrade.NONE,
                        WeaponScalingGrade.NONE
                ),
                WeaponRequirements.of(
                        7,
                        0,
                        16,
                        0,
                        0
                ),
                SmithingTrack.SOMBER,
                3.0
        ));
    }

    private WeaponAttackStats attack(int physical, int magic, int fire, int lightning, int holy, int critical) {
        return new WeaponAttackStats(physical, magic, fire, lightning, holy, critical);
    }

    private WeaponGuardStats guard(int physical, int magic, int fire, int lightning, int holy, int boost) {
        return new WeaponGuardStats(physical, magic, fire, lightning, holy, boost);
    }
}


