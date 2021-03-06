package uk.antiperson.stackmob.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.regex.Pattern;

@ListenerMetadata(config = "events.equip.enabled")
public class EquipListener implements Listener {

    private final StackMob sm;
    private final EnumSet<Material> endings;

    public EquipListener(StackMob sm) {
        this.sm = sm;
        final Pattern pattern = Pattern.compile("(HELMET|CHESTPLATE|LEGGINGS|BOOTS|SHOVEL|HOE|SWORD|SHIELD)$");
        this.endings = EnumSet.of(Material.SHIELD, Arrays.stream(Material.values()).filter(material -> pattern.matcher(material.name()).find()).toArray(Material[]::new));
    }

    @EventHandler
    public void onEntityEquip(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Mob)) {
            return;
        }
        if (!isCorrectEquipment(event.getItem().getItemStack().getType())) {
            return;
        }
        if (!sm.getEntityManager().isStackedEntity(event.getEntity())) {
            return;
        }
        StackEntity stackEntity = sm.getEntityManager().getStackEntity(event.getEntity());
        stackEntity.addEquipItem(event.getItem().getItemStack());
    }

    private boolean isCorrectEquipment(Material material) {
        return endings.contains(material);
    }

}