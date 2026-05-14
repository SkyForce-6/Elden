package de.skyforce.main.elden.smithing.service;

import de.skyforce.main.elden.smithing.model.SmithingTrack;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmithingStoneService {

    private final NamespacedKey trackKey;
    private final NamespacedKey tierKey;

    public SmithingStoneService(JavaPlugin plugin) {
        this.trackKey = new NamespacedKey(plugin, "smithing-stone-track");
        this.tierKey = new NamespacedKey(plugin, "smithing-stone-tier");
    }

    public ItemStack createStone(SmithingTrack track, int tier, int amount) {
        ItemStack item = new ItemStack(materialFor(track, tier), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName(track, tier), colorFor(track))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Upgrade Material", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(track.displayName() + " weapon tier " + tier, NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(trackKey, PersistentDataType.STRING, track.name());
        pdc.set(tierKey, PersistentDataType.INTEGER, tier);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isSmithingStone(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(trackKey, PersistentDataType.STRING) && pdc.has(tierKey, PersistentDataType.INTEGER);
    }

    public boolean matches(ItemStack item, SmithingTrack track, int tier) {
        if (!isSmithingStone(item)) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String storedTrack = pdc.get(trackKey, PersistentDataType.STRING);
        Integer storedTier = pdc.get(tierKey, PersistentDataType.INTEGER);
        return track.name().equalsIgnoreCase(storedTrack) && storedTier != null && storedTier == tier;
    }

    public int countMatching(Player player, SmithingTrack track, int tier) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (matches(stack, track, tier)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public boolean removeMatching(Player player, SmithingTrack track, int tier, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (countMatching(player, track, tier) < amount) {
            return false;
        }

        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (!matches(stack, track, tier)) {
                continue;
            }

            int take = Math.min(remaining, stack.getAmount());
            int left = stack.getAmount() - take;
            remaining -= take;
            contents[slot] = left > 0 ? stack.asQuantity(left) : null;
        }
        player.getInventory().setContents(contents);
        return true;
    }

    public List<String> allTrackNames() {
        List<String> values = new ArrayList<>();
        for (SmithingTrack track : SmithingTrack.values()) {
            values.add(track.name().toLowerCase(java.util.Locale.ROOT));
        }
        return values;
    }

    public String displayName(SmithingTrack track, int tier) {
        if (track == SmithingTrack.STANDARD) {
            return tier >= 9 ? "Ancient Dragon Smithing Stone" : "Smithing Stone [" + tier + "]";
        }
        return tier >= 10 ? "Somber Ancient Dragon Smithing Stone" : "Somber Smithing Stone [" + tier + "]";
    }

    private Material materialFor(SmithingTrack track, int tier) {
        if (track == SmithingTrack.STANDARD) {
            return tier >= 9 ? Material.NETHERITE_SCRAP : Material.AMETHYST_SHARD;
        }
        return tier >= 10 ? Material.NETHER_STAR : Material.PRISMARINE_CRYSTALS;
    }

    private NamedTextColor colorFor(SmithingTrack track) {
        return track == SmithingTrack.STANDARD ? NamedTextColor.GREEN : NamedTextColor.AQUA;
    }
}
