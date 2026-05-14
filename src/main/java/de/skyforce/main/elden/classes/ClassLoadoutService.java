package de.skyforce.main.elden.classes;

import de.skyforce.main.elden.armor.registry.ArmorRegistry;
import de.skyforce.main.elden.armor.service.ArmorItemFactory;
import de.skyforce.main.elden.spell.registry.SpellRegistry;
import de.skyforce.main.elden.spell.service.SpellItemFactory;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ClassLoadoutService {

    private final WeaponRegistry weaponRegistry;
    private final WeaponItemFactory weaponItemFactory;
    private final ArmorRegistry armorRegistry;
    private final ArmorItemFactory armorItemFactory;
    private final SpellRegistry spellRegistry;
    private final SpellItemFactory spellItemFactory;

    public ClassLoadoutService(WeaponRegistry weaponRegistry, WeaponItemFactory weaponItemFactory,
                               ArmorRegistry armorRegistry, ArmorItemFactory armorItemFactory,
                               SpellRegistry spellRegistry, SpellItemFactory spellItemFactory) {
        this.weaponRegistry = weaponRegistry;
        this.weaponItemFactory = weaponItemFactory;
        this.armorRegistry = armorRegistry;
        this.armorItemFactory = armorItemFactory;
        this.spellRegistry = spellRegistry;
        this.spellItemFactory = spellItemFactory;
    }

    public void grantStarterLoadout(Player player, EldenClass eldenClass) {
        switch (eldenClass) {
            case ASTROLOGER -> grantAstrologer(player);
            case BANDIT -> grantBandit(player);
            case CONFESSOR -> grantConfessor(player);
            case HEAVY_KNIGHT -> grantHeavyKnight(player);
            case HERO -> grantHero(player);
            case PRISONER -> grantPrisoner(player);
            case PROPHET -> grantProphet(player);
            case SAMURAI -> grantSamurai(player);
            case VAGABOND -> grantVagabond(player);
            case WARRIOR -> grantWarrior(player);
            case WRETCH -> grantWretch(player);
            default -> {
            }
        }

        player.sendMessage(Component.text("Starter loadout granted: " + eldenClass.displayName(), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
    }

    private void grantAstrologer(Player player) {
        giveSpell(player, "glintstone_pebble");
        giveSpell(player, "glintstone_arc");
        giveWeapon(player, "short_sword", "Short Sword", Material.STONE_SWORD);
        giveWeapon(player, "astrologers_staff", "Astrologer's Staff", Material.BLAZE_ROD);
        giveWeapon(player, "scripture_wooden_shield", "Scripture Wooden Shield", Material.SHIELD);
        giveArmor(player, "astrologer_hood", "Astrologer Hood", Material.LEATHER_HELMET);
        giveArmor(player, "astrologer_robe", "Astrologer Robe", Material.LEATHER_CHESTPLATE);
        giveArmor(player, "astrologer_trousers", "Astrologer Trousers", Material.LEATHER_LEGGINGS);
        giveArmor(player, "astrologer_gloves", "Astrologer Gloves", Material.LEATHER_BOOTS);
    }

    private void grantBandit(Player player) {
        giveWeapon(player, "great_knife", "Great Knife", Material.IRON_SWORD);
        giveWeapon(player, "shortbow", "Shortbow", Material.BOW);
        giveItem(player, Material.ARROW, 30, "Bone Arrow (Fletched)");
        giveWeapon(player, "buckler", "Buckler", Material.SHIELD);
        giveArmor(player, "bandit_mask", "Bandit Mask", Material.LEATHER_HELMET);
        giveArmor(player, "bandit_garb", "Bandit Garb", Material.LEATHER_CHESTPLATE);
        giveArmor(player, "bandit_trousers", "Bandit Trousers", Material.LEATHER_LEGGINGS);
        giveArmor(player, "bandit_manchettes", "Bandit Manchettes", Material.LEATHER_BOOTS);
        giveArmor(player, "bandit_boots", "Bandit Boots", Material.LEATHER_BOOTS);
    }

    private void grantConfessor(Player player) {
        giveSpell(player, "urgent_heal");
        giveSpell(player, "assassins_approach");
        giveWeapon(player, "broadsword", "Broadsword", Material.IRON_SWORD);
        giveWeapon(player, "finger_seal", "Finger Seal", Material.BLAZE_ROD);
        giveWeapon(player, "blue_crest_heater_shield", "Blue Crest Heater Shield", Material.SHIELD);
        giveArmor(player, "confessor_hood", "Confessor Hood", Material.CHAINMAIL_HELMET);
        giveArmor(player, "confessor_armor", "Confessor Armor", Material.CHAINMAIL_CHESTPLATE);
        giveArmor(player, "confessor_gloves", "Confessor Gloves", Material.CHAINMAIL_BOOTS);
        giveArmor(player, "confessor_boots", "Confessor Boots", Material.CHAINMAIL_BOOTS);
    }

    private void grantHeavyKnight(Player player) {
        player.sendMessage(Component.text("Heavy Knight starter gear is still marked as TODO in the source list.", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
    }

    private void grantHero(Player player) {
        giveWeapon(player, "battle_axe", "Battle Axe", Material.IRON_AXE);
        giveWeapon(player, "large_leather_shield", "Large Leather Shield", Material.SHIELD);
        giveArmor(player, "champion_headband", "Champion Headband", Material.LEATHER_HELMET);
        giveArmor(player, "champion_pauldron", "Champion Pauldron", Material.LEATHER_CHESTPLATE);
        giveArmor(player, "champion_gaiters", "Champion Gaiters", Material.LEATHER_LEGGINGS);
        giveArmor(player, "champion_bracers", "Champion Bracers", Material.LEATHER_BOOTS);
    }

    private void grantPrisoner(Player player) {
        giveSpell(player, "magic_glintblade");
        giveWeapon(player, "estoc", "Estoc", Material.IRON_SWORD);
        giveWeapon(player, "glintstone_staff", "Glintstone Staff", Material.BLAZE_ROD);
        giveWeapon(player, "rift_shield", "Rift Shield", Material.SHIELD);
        giveArmor(player, "prisoner_iron_mask", "Prisoner Iron Mask", Material.IRON_HELMET);
        giveArmor(player, "prisoner_clothing", "Prisoner Clothing", Material.CHAINMAIL_CHESTPLATE);
        giveArmor(player, "prisoner_trousers", "Prisoner Trousers", Material.CHAINMAIL_LEGGINGS);
    }

    private void grantProphet(Player player) {
        giveSpell(player, "heal");
        giveSpell(player, "catch_flame");
        giveWeapon(player, "short_spear", "Short Spear", Material.TRIDENT);
        giveWeapon(player, "finger_seal", "Finger Seal", Material.BLAZE_ROD);
        giveWeapon(player, "rickety_shield", "Rickety Shield", Material.SHIELD);
        giveArmor(player, "prophet_blindfold", "Prophet Blindfold", Material.LEATHER_HELMET);
        giveArmor(player, "prophet_robe", "Prophet Robe", Material.LEATHER_CHESTPLATE);
        giveArmor(player, "prophet_trousers", "Prophet Trousers", Material.LEATHER_LEGGINGS);
    }

    private void grantSamurai(Player player) {
        giveWeapon(player, "uchigatana", "Uchigatana", Material.IRON_SWORD);
        giveWeapon(player, "longbow", "Longbow", Material.BOW);
        giveItem(player, Material.ARROW, 20, "Arrow");
        giveItem(player, Material.ARROW, 10, "Fire Arrow");
        giveWeapon(player, "red_thorn_roundshield", "Red Thorn Roundshield", Material.SHIELD);
        giveArmor(player, "land_of_reeds_helm", "Land of Reeds Helm", Material.IRON_HELMET);
        giveArmor(player, "land_of_reeds_armor", "Land of Reeds Armor", Material.IRON_CHESTPLATE);
        giveArmor(player, "land_of_reeds_greaves", "Land of Reeds Greaves", Material.IRON_LEGGINGS);
        giveArmor(player, "land_of_reeds_gauntlets", "Land of Reeds Gauntlets", Material.IRON_BOOTS);
    }

    private void grantVagabond(Player player) {
        giveWeapon(player, "longsword", "Longsword", Material.IRON_SWORD);
        giveWeapon(player, "halberd", "Halberd", Material.IRON_AXE);
        giveWeapon(player, "heater_shield", "Heater Shield", Material.SHIELD);
        giveArmor(player, "vagabond_helm", "Vagabond Knight Helm", Material.IRON_HELMET);
        giveArmor(player, "vagabond_armor", "Vagabond Knight Armor", Material.IRON_CHESTPLATE);
        giveArmor(player, "vagabond_greaves", "Vagabond Knight Greaves", Material.IRON_LEGGINGS);
        giveArmor(player, "vagabond_knight_gauntlets", "Vagabond Knight Gauntlets", Material.IRON_BOOTS);
    }

    private void grantWarrior(Player player) {
        giveWeapon(player, "scimitar", "Scimitar", Material.GOLDEN_SWORD);
        giveWeapon(player, "scimitar", "Scimitar", Material.GOLDEN_SWORD);
        giveWeapon(player, "riveted_wooden_shield", "Riveted Wooden Shield", Material.SHIELD);
        giveArmor(player, "blue_cloth_cowl", "Blue Cloth Cowl", Material.LEATHER_HELMET);
        giveArmor(player, "blue_cloth_vest", "Blue Cloth Vest", Material.LEATHER_CHESTPLATE);
        giveArmor(player, "warrior_greaves", "Warrior Greaves", Material.LEATHER_LEGGINGS);
        giveArmor(player, "warrior_gauntlets", "Warrior Gauntlets", Material.LEATHER_BOOTS);
    }

    private void grantWretch(Player player) {
        giveWeapon(player, "club", "Club", Material.WOODEN_AXE);
    }

    private void giveWeapon(Player player, String weaponId, String fallbackName, Material fallbackMaterial) {
        ItemStack item = weaponRegistry.getById(weaponId)
                .map(weaponItemFactory::createWeaponItem)
                .orElseGet(() -> namedItem(fallbackMaterial, fallbackName, 1));
        addItem(player, item);
    }

    private void giveArmor(Player player, String armorId, String fallbackName, Material fallbackMaterial) {
        ItemStack item = armorRegistry.getById(armorId)
                .map(armorItemFactory::createArmorItem)
                .orElseGet(() -> namedItem(fallbackMaterial, fallbackName, 1));
        addItem(player, item);
    }

    private void giveSpell(Player player, String spellId) {
        spellRegistry.getById(spellId)
                .map(spellItemFactory::createSpellItem)
                .ifPresent(item -> addItem(player, item));
    }

    private void giveItem(Player player, Material material, int amount, String displayName) {
        addItem(player, namedItem(material, displayName, amount));
    }

    private void addItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private ItemStack namedItem(Material material, String displayName, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

}




