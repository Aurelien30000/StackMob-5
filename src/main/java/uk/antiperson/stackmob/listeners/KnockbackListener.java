package uk.antiperson.stackmob.listeners;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;

@ListenerMetadata(config = "disable-knockback.enabled")
public class KnockbackListener implements Listener {

    private final StackMob sm;

    public KnockbackListener(StackMob sm) {
        this.sm = sm;
    }

    @EventHandler
    public void onEntityKnockback(EntityKnockbackEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        StackEntity stackEntity = sm.getEntityManager().getStackEntity((LivingEntity) event.getEntity());
        if (stackEntity == null) {
            return;
        }
        if (sm.getMainConfig().isKnockbackDisabled(event.getEntityType())) {
            return;
        }
        if (!sm.getMainConfig().isKnockbackDisabledTypes(event.getEntityType())) {
            return;
        }
        if (!sm.getMainConfig().isKnockbackDisabledReasons(event.getEntityType(), event.getEntity().getEntitySpawnReason())) {
            return;
        }
        if (!sm.getMainConfig().isKnockbackDisabledCause(event.getEntityType(), event.getCause())) {
            return;
        }
        event.setCancelled(true);
    }

}