package uk.antiperson.stackmob.listeners;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.Drops;
import uk.antiperson.stackmob.entity.StackEntity;

import java.util.concurrent.ThreadLocalRandom;

@ListenerMetadata(config = "events.multiply.drops")
public class DropListener implements Listener {

    private final StackMob sm;

    public DropListener(StackMob sm) {
        this.sm = sm;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropListener(EntityDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() != Material.EGG && event.getItemDrop().getItemStack().getType() != Material.SCUTE) {
            return;
        }
        if (!sm.getEntityManager().isStackedEntity((LivingEntity) event.getEntity())) {
            return;
        }
        final StackEntity stackEntity = sm.getEntityManager().getStackEntity((LivingEntity) event.getEntity());
        if (stackEntity == null || stackEntity.isSingle()) {
            return;
        }
        final double multiplier = ThreadLocalRandom.current().nextDouble(0.4, 0.8);
        final int itemCount = (int) Math.round((stackEntity.getSize() - 1) * multiplier);
        Drops.dropItem(event.getEntity().getLocation(), event.getItemDrop().getItemStack(), itemCount);
    }

}
