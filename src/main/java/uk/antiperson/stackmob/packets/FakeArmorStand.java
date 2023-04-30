package uk.antiperson.stackmob.packets;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface FakeArmorStand {

    void spawnFakeArmorStand(Entity owner, Location location, Component name, double offset);

    void updateName(Component newName);

    void teleport(Entity entity, double offset);

    void removeFakeArmorStand();

    default Location adjustLocation(Entity entity, double offset) {
        final Component customName;
        double adjustment = offset > 0
                ? offset
                : (customName = entity.customName()) == null || customName == Component.empty() ? 0.1 : 0.3;
        return entity.getLocation().add(0, entity.getHeight() + adjustment, 0);
    }

}