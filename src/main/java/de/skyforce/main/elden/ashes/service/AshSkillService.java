package de.skyforce.main.elden.ashes.service;

import de.skyforce.main.elden.ashes.model.WeaponAshData;
import de.skyforce.main.elden.ashes.registry.AshSkillRegistry;
import de.skyforce.main.elden.ashes.skill.AshSkill;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class AshSkillService {

    private final AshBindingService ashBindingService;
    private final AshSkillRegistry ashSkillRegistry;

    public AshSkillService(AshBindingService ashBindingService, AshSkillRegistry ashSkillRegistry) {
        this.ashBindingService = ashBindingService;
        this.ashSkillRegistry = ashSkillRegistry;
    }

    public boolean executeBoundSkill(Player player, ItemStack weapon) {
        WeaponAshData data = ashBindingService.getBoundData(weapon);
        if (data == null) {
            return false;
        }

        AshSkill skill = ashSkillRegistry.get(data.skillType());
        if (skill == null) {
            return false;
        }

        skill.execute(player, weapon);
        return true;
    }
}
