package uk.antiperson.stackmob.config;

import org.bukkit.entity.*;

import java.util.ArrayList;
import java.util.List;

public class EntityConfig {

    enum EntityGrouping {
        HOSTILE(Monster.class, Ghast.class, Phantom.class),
        ANIMALS(Animals.class),
        WATER(WaterMob.class),
        RAIDER(Raider.class),
        BOSS(Boss.class);

        final Class<? extends Entity>[] classes;

        @SafeVarargs
        EntityGrouping(Class<? extends Entity>... classes) {
            this.classes = classes;
        }

        public boolean isEntityMemberOf(Class<? extends Entity> entity) {
            if (entity == null) {
                return false;
            }
            for (Class<? extends Entity> entityClass : classes) {
                if (entityClass.isAssignableFrom(entity)) {
                    return true;
                }
            }
            return false;
        }

        public List<EntityType> getEntityTypes() {
            final List<EntityType> list = new ArrayList<>();

            for (EntityType entityType : EntityType.values()) {
                final Class<? extends Entity> entityClass = entityType.getEntityClass();
                if (isEntityMemberOf(entityClass)) {
                    list.add(entityType);
                }
            }

            return list;
        }
    }

    public enum EventType {
        BREED("breed"),
        DYE("dye"),
        EQUIP("equip"),
        EXPLOSION("explosion"),
        SHEAR("shear");

        final String configKey;

        EventType(String configKey) {
            this.configKey = configKey;
        }

        public String getConfigKey() {
            return configKey;
        }
    }

    public enum ListenerMode {
        MULTIPLY,
        SPLIT
    }

}
