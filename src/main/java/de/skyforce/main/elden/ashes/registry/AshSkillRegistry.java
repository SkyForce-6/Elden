package de.skyforce.main.elden.ashes.registry;

import de.skyforce.main.elden.ashes.skill.AshSkill;
import de.skyforce.main.elden.ashes.model.AshSkillType;

import java.util.EnumMap;
import java.util.Map;

public final class AshSkillRegistry {

    private final Map<AshSkillType, AshSkill> skills = new EnumMap<>(AshSkillType.class);

    public void register(AshSkillType type, AshSkill skill) {
        skills.put(type, skill);
    }

    public AshSkill get(AshSkillType type) {
        return skills.get(type);
    }
}
