package de.skyforce.main.elden.level;

import de.skyforce.main.elden.classes.EldenClass;
import java.util.EnumMap;
import java.util.Map;

public final class PlayerProgress {

    private int level;
    private final EnumMap<AttributeType, Integer> attributes;

    private PlayerProgress(int level, EnumMap<AttributeType, Integer> attributes) {
        this.level = level;
        this.attributes = attributes;
    }

    public static PlayerProgress fromClass(EldenClass eldenClass) {
        EnumMap<AttributeType, Integer> attrs = new EnumMap<>(AttributeType.class);
        attrs.put(AttributeType.VIGOR, eldenClass.vig());
        attrs.put(AttributeType.MIND, eldenClass.mnd());
        attrs.put(AttributeType.ENDURANCE, eldenClass.end());
        attrs.put(AttributeType.STRENGTH, eldenClass.str());
        attrs.put(AttributeType.DEXTERITY, eldenClass.dex());
        attrs.put(AttributeType.INTELLIGENCE, eldenClass.intl());
        attrs.put(AttributeType.FAITH, eldenClass.fth());
        attrs.put(AttributeType.ARCANE, eldenClass.arc());
        return new PlayerProgress(eldenClass.level(), attrs);
    }

    public static PlayerProgress of(int level, Map<AttributeType, Integer> attributes) {
        EnumMap<AttributeType, Integer> copy = new EnumMap<>(AttributeType.class);
        for (AttributeType type : AttributeType.values()) {
            copy.put(type, Math.max(1, attributes.getOrDefault(type, 10)));
        }
        return new PlayerProgress(Math.max(1, level), copy);
    }

    public int level() {
        return level;
    }

    public int attribute(AttributeType attributeType) {
        return attributes.getOrDefault(attributeType, 1);
    }

    public boolean increase(AttributeType attributeType, int attributeCap, int levelCap) {
        if (level >= levelCap) {
            return false;
        }

        int current = attribute(attributeType);
        if (current >= attributeCap) {
            return false;
        }

        attributes.put(attributeType, current + 1);
        level += 1;
        return true;
    }

    public EnumMap<AttributeType, Integer> attributesView() {
        return new EnumMap<>(attributes);
    }
}

