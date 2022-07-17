package uk.antiperson.stackmob.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.config.EntityConfig;
import uk.antiperson.stackmob.entity.StackEntity;

import java.util.concurrent.ThreadLocalRandom;

@ListenerMetadata(config = "events.explosion.enabled")
public class ExplosionListener implements Listener {

    private final StackMob sm;

    public ExplosionListener(StackMob sm) {
        this.sm = sm;
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        StackEntity stackEntity = sm.getEntityManager().getStackEntity((LivingEntity) event.getEntity());
        if (stackEntity == null || stackEntity.isSingle()) {
            return;
        }
        switch (sm.getMainConfig().getListenerMode(event.getEntityType(), EntityConfig.EventType.EXPLOSION)) {
            case SPLIT -> stackEntity.slice();
            case MULTIPLY -> {
                final double multiplier = ThreadLocalRandom.current().nextDouble(0.4, 0.6);
                final int toMultiply = sm.getMainConfig().getEventMultiplyLimit(event.getEntityType(), "explosion", stackEntity.getSize());
                event.setYield(event.getYield() + Math.round(event.getYield() * toMultiply * multiplier));
            }
        }
    }
}
