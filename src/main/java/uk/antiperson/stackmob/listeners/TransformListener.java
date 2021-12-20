package uk.antiperson.stackmob.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;

public class TransformListener implements Listener {

    private final StackMob sm;

    public TransformListener(StackMob sm) {
        this.sm = sm;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (event.getTransformReason() == EntityTransformEvent.TransformReason.SHEARED ||
                event.getTransformReason() == EntityTransformEvent.TransformReason.SPLIT) {
            return;
        }
        final StackEntity stackEntity = sm.getEntityManager().getStackEntity((LivingEntity) event.getEntity());
        final StackEntity transformed = sm.getEntityManager().registerStackedEntity((LivingEntity) event.getTransformedEntity());
        if (stackEntity == null) {
            transformed.setForgetOnSpawn(true);
            return;
        }
        transformed.setSize(stackEntity.getSize());
        stackEntity.remove();
    }

}
