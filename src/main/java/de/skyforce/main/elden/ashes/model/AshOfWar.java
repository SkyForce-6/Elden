package de.skyforce.main.elden.ashes.model;

public final class AshOfWar {

    private final String id;
    private final String displayName;
    private final AshSkillType skillType;
    private final AffinityType affinity;
    private final WeaponCategory targetCategory;
    private final int requiredLevel;

    public AshOfWar(String id, String displayName, AshSkillType skillType,
                    AffinityType affinity, WeaponCategory targetCategory,
                    int requiredLevel) {
        this.id = id;
        this.displayName = displayName;
        this.skillType = skillType;
        this.affinity = affinity;
        this.targetCategory = targetCategory;
        this.requiredLevel = requiredLevel;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public AshSkillType skillType() {
        return skillType;
    }

    public AffinityType affinity() {
        return affinity;
    }

    public WeaponCategory targetCategory() {
        return targetCategory;
    }

    public int requiredLevel() {
        return requiredLevel;
    }
}
