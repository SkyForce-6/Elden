package de.skyforce.main.elden.ashes.registry;

import de.skyforce.main.elden.ashes.model.AshOfWarDefinition;
import de.skyforce.main.elden.weapon.model.WeaponType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AshOfWarRegistry {

    private final Map<String, AshOfWarDefinition> ashesById = new HashMap<>();

    public AshOfWarRegistry() {
        registerAshesOfWar();
    }

    private void registerAshesOfWar() {
        register(AshOfWarDefinition.of(
            "assassins_gambit",
            "Assassin's Gambit",
            "Masks the player's presence at the cost of a self-inflicted wound. Grants near-invisibility and silenced footsteps.",
            "Small and medium Straight Swords and Thrusting Swords",
            "Occult",
            "Sold by Knight Bernahl in Volcano Manor for 6500 runes",
            12.0D,
            60L,
            weaponTypes(WeaponType.STRAIGHT_SWORD, WeaponType.THRUSTING_SWORD)
        ));

        register(AshOfWarDefinition.of(
            "barbaric_roar",
            "Barbaric Roar",
            "Increase attack power. Strong attacks turn into savage combo attacks.",
            "All melee armaments (Daggers, Thrusting Swords, and Whips excepted)",
            "Heavy",
            "On a Teardrop Beetle in Ravine-Veiled Village",
            16.0D,
            60L,
            weaponTypes(
                    WeaponType.STRAIGHT_SWORD,
                    WeaponType.GREATSWORD,
                    WeaponType.KATANA,
                    WeaponType.AXE,
                    WeaponType.GREATAXE,
                    WeaponType.SPEAR,
                    WeaponType.HALBERD,
                    WeaponType.TWINBLADE
            )
        ));

        register(AshOfWarDefinition.of(
            "barrage",
            "Barrage",
            "Archery skill using a bow held horizontally. Ready the bow, then fire off a rapid succession of shots faster than the eye can see.",
            "Light Bows only",
            "Standard",
            "Mt. Gelmir: Dropped by a Teardrop Scarab in the narrow channel north of Seethewater River",
            8.0D,
            30L,
            weaponTypes(WeaponType.BOW)
        ));

        register(AshOfWarDefinition.of(
            "barricade_shield",
            "Barricade Shield",
            "Focus your energy into the shield, temporarily hardening it to deflect greater blows.",
            "Shields only",
            "Standard",
            "Dropped by Night's Cavalry at Weeping Peninsula",
            12.0D,
            60L,
            weaponTypes(WeaponType.SHIELD)
        ));

        register(AshOfWarDefinition.of(
            "beasts_roar",
            "Beast's Roar",
            "Unleash a beastly roar, rending the air as a forward-travelling projectile.",
            "All melee armaments",
            "Keen",
            "Bestial Sanctum: Reward from Gurranq, Beast Clergyman after giving him the fourth Deathroot",
            10.0D,
            40L,
            weaponTypes(
                    WeaponType.STRAIGHT_SWORD,
                    WeaponType.THRUSTING_SWORD,
                    WeaponType.GREATSWORD,
                    WeaponType.KATANA,
                    WeaponType.DAGGER,
                    WeaponType.AXE,
                    WeaponType.GREATAXE,
                    WeaponType.SPEAR,
                    WeaponType.HALBERD,
                    WeaponType.TWINBLADE
            )
        ));

        register(AshOfWarDefinition.of(
            "black_flame_tornado",
            "Black Flame Tornado",
            "Spin armament overhead and then plunge it into the ground to summon a raging vortex of black flames. Hold to create an initial flame tornado while spinning the armament.",
            "Polearms and Twinblades",
            "Flame Art",
            "Crumbling Farum Azula: Dropped by the Godskin Duo Boss inside the Dragon Temple area",
            18.0D,
            80L,
            weaponTypes(WeaponType.SPEAR, WeaponType.HALBERD, WeaponType.TWINBLADE)
        ));

        register(AshOfWarDefinition.of(
            "blood_blade",
            "Blood Blade",
            "Wound self to coat the armament with blood, then unleash an airborne blood blade that causes hemorrhaging. Can be fired in rapid succession.",
            "Small and medium Swords only",
            "Blood",
            "Dropped by a Teardrop Scarab in Altus Plateau, northeast of Erdtree-Gazing Hill, above a pond",
            8.0D,
            30L,
            weaponTypes(WeaponType.STRAIGHT_SWORD, WeaponType.THRUSTING_SWORD)
        ));

        register(AshOfWarDefinition.of(
            "blood_tax",
            "Blood Tax",
            "Twist to build power, then unleash a flurry of thrusts that rob the target of both their blood and their HP.",
            "All armaments capable of thrusting (Colossal Weapons excepted)",
            "Blood",
            "Found in the Mohgwyn Dynasty in a small cave at the northern edge of the blood lake",
            14.0D,
            45L,
            weaponTypes(WeaponType.THRUSTING_SWORD, WeaponType.SPEAR, WeaponType.TWINBLADE)
        ));

        register(AshOfWarDefinition.of(
            "bloodhounds_step",
            "Bloodhound's Step",
            "Skill that allows the user to become temporarily invisible while dodging at high speed. Moves faster and travels farther than a regular quickstep.",
            "All melee armaments",
            "Keen",
            "Dropped by a Night's Cavalry at the bridge in front of Lenne's Rise tower, in Dragonbarrow",
            6.0D,
            20L,
            weaponTypes(
                    WeaponType.STRAIGHT_SWORD,
                    WeaponType.THRUSTING_SWORD,
                    WeaponType.GREATSWORD,
                    WeaponType.KATANA,
                    WeaponType.DAGGER,
                    WeaponType.AXE,
                    WeaponType.GREATAXE,
                    WeaponType.SPEAR,
                    WeaponType.HALBERD,
                    WeaponType.TWINBLADE
            )
        ));

        register(AshOfWarDefinition.of(
            "bloody_slash",
            "Bloody Slash",
            "From a low stance, coat the blade in your own blood to unleash a rending blood slash in a wide arc.",
            "Swords (colossal weapons excepted)",
            "Blood",
            "Dropped by a Godrick Knight at the top of the ramparts of Fort Haight",
            16.0D,
            50L,
            weaponTypes(WeaponType.STRAIGHT_SWORD, WeaponType.THRUSTING_SWORD, WeaponType.GREATSWORD, WeaponType.KATANA)
        ));

        register(AshOfWarDefinition.of(
            "braggarts_roar",
            "Braggart's Roar",
            "Declare your presence with a boastful roar. Raises attack power, defense, and stamina recovery speed.",
            "All melee armaments (Daggers, Thrusting Swords, and Whips excepted)",
            "Heavy",
            "Found on Blackguard Big Boggart's weapon after killing him or looting the body later in his questline",
            16.0D,
            60L,
            weaponTypes(
                    WeaponType.STRAIGHT_SWORD,
                    WeaponType.GREATSWORD,
                    WeaponType.KATANA,
                    WeaponType.AXE,
                    WeaponType.GREATAXE,
                    WeaponType.SPEAR,
                    WeaponType.HALBERD,
                    WeaponType.TWINBLADE
            )
        ));

        register(AshOfWarDefinition.of(
            "carian_grandeur",
            "Carian Grandeur",
            "Transform blade into a magical greatsword and swing it down. Can be charged to increase its power by up to two levels.",
            "Swords (Colossal Weapons excepted)",
            "Magic",
            "Caria Manor: Found on the roof of a tall structure in the gardens",
            18.0D,
            60L,
            weaponTypes(WeaponType.STRAIGHT_SWORD, WeaponType.THRUSTING_SWORD, WeaponType.GREATSWORD, WeaponType.KATANA)
        ));

        register(AshOfWarDefinition.of(
            "carian_greatsword",
            "Carian Greatsword",
            "Transform blade into a magical greatsword and swing it down. Can be charged to increase its power.",
            "Swords (Colossal Weapons excepted)",
            "Magic",
            "Sold by Sorcerer Rogier at Stormveil Castle or the Roundtable Hold",
            16.0D,
            50L,
            weaponTypes(WeaponType.STRAIGHT_SWORD, WeaponType.THRUSTING_SWORD, WeaponType.GREATSWORD, WeaponType.KATANA)
        ));

        register(AshOfWarDefinition.of(
            "carian_retaliation",
            "Carian Retaliation",
            "Dispels incoming sorceries and incantations, transforming the magic into retaliatory glintblades.",
            "Small and medium shields only",
            "Magic",
            "Caria Manor: On a balcony above the Manor Lower Level bonfire",
            12.0D,
            30L,
            weaponTypes(WeaponType.SHIELD)
        ));

        register(AshOfWarDefinition.of(
            "charge_forth",
            "Charge Forth",
            "Quickly charge forward with the armament at the hip, carrying the momentum into a thrust. Hold to cover a greater distance.",
            "Polearms capable of thrusting, Heavy Thrusting Swords, and Twinblades",
            "Quality",
            "Dropped by a Teardrop Scarab in Liurnia of the Lakes, west of the Academy Gate Town site of grace",
            10.0D,
            35L,
            weaponTypes(WeaponType.THRUSTING_SWORD, WeaponType.SPEAR, WeaponType.HALBERD, WeaponType.TWINBLADE)
        ));

        register(AshOfWarDefinition.of(
            "chilling_mist",
            "Chilling Mist",
            "Coat armament in frost, then slash to spread frigid mist forward. The armament retains frost for a while.",
            "All melee armaments (Whips, Fists, and Claws excepted)",
            "Cold",
            "Dropped by a Teardrop Scarab in Three Sisters, Liurnia, south of Renna's Rise",
            14.0D,
            50L,
            weaponTypes(
                    WeaponType.STRAIGHT_SWORD,
                    WeaponType.THRUSTING_SWORD,
                    WeaponType.GREATSWORD,
                    WeaponType.KATANA,
                    WeaponType.DAGGER,
                    WeaponType.AXE,
                    WeaponType.GREATAXE,
                    WeaponType.SPEAR,
                    WeaponType.HALBERD,
                    WeaponType.TWINBLADE
            )
        ));

        register(AshOfWarDefinition.of(
            "cragblade",
            "Cragblade",
            "Reinforce weapon with earth.",
            "All melee armaments (Whips excluded)",
            "Heavy",
            "Dropped by scarab west of Impassable Bridge",
            10.0D,
            45L,
            weaponTypes(
                    WeaponType.STRAIGHT_SWORD,
                    WeaponType.THRUSTING_SWORD,
                    WeaponType.GREATSWORD,
                    WeaponType.KATANA,
                    WeaponType.DAGGER,
                    WeaponType.AXE,
                    WeaponType.GREATAXE,
                    WeaponType.SPEAR,
                    WeaponType.HALBERD,
                    WeaponType.TWINBLADE
            )
        ));

        register(AshOfWarDefinition.of(
            "determination",
            "Determination",
            "Hold the flat of the armament to your face and pledge your resolve, powering up your next attack.",
            "All melee armaments",
            "Quality",
            "Dropped by a Teardrop Scarab on the path at the bridge north of Agheel Lake",
            10.0D,
            40L,
            weaponTypes(
                    WeaponType.STRAIGHT_SWORD,
                    WeaponType.THRUSTING_SWORD,
                    WeaponType.GREATSWORD,
                    WeaponType.KATANA,
                    WeaponType.DAGGER,
                    WeaponType.AXE,
                    WeaponType.GREATAXE,
                    WeaponType.SPEAR,
                    WeaponType.HALBERD,
                    WeaponType.TWINBLADE
            )
        ));

        register(AshOfWarDefinition.of(
            "double_slash",
            "Double Slash",
            "Perform a crossing slash attack from a low stance. Repeated inputs allow for up to two follow-up attacks.",
            "Swords and Polearms capable of slashing (colossal weapons excepted)",
            "Keen",
            "Sellia, Town of Sorcery: Dropped by a Teardrop Scarab found high above on a large root",
            12.0D,
            35L,
            weaponTypes(WeaponType.STRAIGHT_SWORD, WeaponType.GREATSWORD, WeaponType.KATANA, WeaponType.HALBERD, WeaponType.TWINBLADE)
        ));

        register(AshOfWarDefinition.of(
            "earthshaker",
            "Earthshaker",
            "Thrust armament into the ground, then gather strength to unleash an earth-shaking shockwave.",
            "Greataxes, Warhammers, and Colossal Weapons",
            "Heavy",
            "Dropped by an invisible scarab on the road after Grand Lift of Dectus, only at night",
            16.0D,
            60L,
            weaponTypes(WeaponType.GREATAXE)
        ));

        register(AshOfWarDefinition.of(
            "enchanted_shot",
            "Enchanted Shot",
            "Gather spiritual essence within the arrow, letting it fly faster and curve toward the target.",
            "Light Bows and Longbows",
            "Standard",
            "Dropped by a Teardrop Beetle near a brazier in Nokron, Eternal City",
            8.0D,
            30L,
            weaponTypes(WeaponType.BOW)
        ));

        register(AshOfWarDefinition.of(
            "endure",
            "Endure",
            "Assume an anchored stance to brace for incoming attacks, briefly boosting poise and reducing damage taken.",
            "All melee armaments",
            "Heavy",
            "Sold by Knight Bernahl at the Warmaster's Shack for 600 runes",
            8.0D,
            30L,
            weaponTypes(
                    WeaponType.STRAIGHT_SWORD,
                    WeaponType.THRUSTING_SWORD,
                    WeaponType.GREATSWORD,
                    WeaponType.KATANA,
                    WeaponType.DAGGER,
                    WeaponType.AXE,
                    WeaponType.GREATAXE,
                    WeaponType.SPEAR,
                    WeaponType.HALBERD,
                    WeaponType.TWINBLADE
            )
        ));

        register(AshOfWarDefinition.of(
            "eruption",
            "Eruption",
            "Slam the armament into the ground to spawn roiling lava that also spouts up upon release.",
            "Greatswords, Greataxes, Great Hammers, Heavy Thrusting Swords, and Colossal Weapons",
            "Fire",
            "Sold by Knight Bernahl in Volcano Manor for 8000 runes",
            18.0D,
            70L,
            weaponTypes(WeaponType.GREATSWORD, WeaponType.GREATAXE, WeaponType.THRUSTING_SWORD)
        ));

        register(AshOfWarDefinition.of(
            "flame_of_the_redmanes",
            "Flame of the Redmanes",
            "Produce a powerful burst of flames in a wide frontward arc.",
            "All melee armaments",
            "Fire",
            "Dropped by an invisible Teardrop Scarab in front of the Fort Gael North Site of Grace",
            14.0D,
            45L,
            weaponTypes(
                    WeaponType.STRAIGHT_SWORD,
                    WeaponType.THRUSTING_SWORD,
                    WeaponType.GREATSWORD,
                    WeaponType.KATANA,
                    WeaponType.DAGGER,
                    WeaponType.AXE,
                    WeaponType.GREATAXE,
                    WeaponType.SPEAR,
                    WeaponType.HALBERD,
                    WeaponType.TWINBLADE
            )
        ));

        register(AshOfWarDefinition.of(
            "flaming_strike",
            "Flaming Strike",
            "Emit flame in a wide frontward arc. Follow up with a strong attack to perform a lunging sweeping strike and coat the armament in fire.",
            "All melee armaments (Colossal Weapons and Whips excepted)",
            "Fire",
            "Dropped by a Teardrop Scarab in Redmane Castle",
            14.0D,
            45L,
            weaponTypes(
                    WeaponType.STRAIGHT_SWORD,
                    WeaponType.THRUSTING_SWORD,
                    WeaponType.GREATSWORD,
                    WeaponType.KATANA,
                    WeaponType.DAGGER,
                    WeaponType.AXE,
                    WeaponType.GREATAXE,
                    WeaponType.SPEAR,
                    WeaponType.HALBERD,
                    WeaponType.TWINBLADE
            )
        ));

        register(AshOfWarDefinition.of(
            "giant_hunt",
            "Giant Hunt",
            "Step forward from a low stance, carrying the momentum into a sudden upward thrust.",
            "Large and colossal weapons capable of thrusting, spears, and Twinblades",
            "Quality",
            "Dropped by Night's Cavalry in northern Liurnia, only at night",
            18.0D,
            50L,
            weaponTypes(WeaponType.GREATSWORD, WeaponType.THRUSTING_SWORD, WeaponType.SPEAR, WeaponType.TWINBLADE)
        ));

        register(AshOfWarDefinition.of(
            "gravity_well",
            "Gravity Well",
            "Summon a gravity well at target location, pulling enemies toward its center.",
            "Staffs",
            "Standard",
            "Leyndell, Royal Capital: In the prayer room near the Erdtree Greatshield location",
            14.0D,
            40L,
            weaponTypes(WeaponType.STAFF)
        ));

        register(AshOfWarDefinition.of(
            "lions_claw",
            "Lion's Claw",
            "Leap forward with a slashing attack. Can be used to cross gaps.",
            "Greatswords and Colossal Swords",
            "Standard",
            "Redmane Castle: Dropped by the Leonine Misbegotten",
            20.0D,
            80L,
            weaponTypes(WeaponType.GREATSWORD)
        ));

        register(AshOfWarDefinition.of(
            "parry",
            "Parry",
            "Defend yourself against attacks with a precise stance. When successful, leave the attacker in an opening for a riposte.",
            "Medium Shields and Daggers",
            "Standard",
            "Starting skill for most players",
            4.0D,
            20L,
            weaponTypes(WeaponType.DAGGER, WeaponType.SHIELD)
        ));
    }

    public void register(AshOfWarDefinition ash) {
        ashesById.put(ash.id(), ash);
    }

    public Optional<AshOfWarDefinition> getById(String id) {
        return Optional.ofNullable(ashesById.get(id.toLowerCase()));
    }

    public Map<String, AshOfWarDefinition> getAll() {
        return new HashMap<>(ashesById);
    }

    private Set<WeaponType> weaponTypes(WeaponType first, WeaponType... rest) {
        return EnumSet.of(first, rest);
    }
}
