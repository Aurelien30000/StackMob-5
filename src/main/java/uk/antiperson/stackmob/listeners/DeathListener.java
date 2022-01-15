package uk.antiperson.stackmob.listeners;

import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.Drops;
import uk.antiperson.stackmob.entity.StackEntity;
import uk.antiperson.stackmob.entity.death.DeathMethod;
import uk.antiperson.stackmob.entity.death.DeathType;
import uk.antiperson.stackmob.events.EventHelper;
import uk.antiperson.stackmob.utils.NMSHelper;
import uk.antiperson.stackmob.utils.Utilities;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class DeathListener implements Listener {

    private final StackMob sm;

    public DeathListener(StackMob sm) {
        this.sm = sm;
    }

    @EventHandler(ignoreCancelled = true)
    public void onStackDeath(EntityDeathEvent event) {
        final StackEntity stackEntity = sm.getEntityManager().getStackEntity(event.getEntity());
        if (stackEntity == null) {
            return;
        }
        final DeathMethod deathMethod = calculateDeath(stackEntity);
        final int deathStep = EventHelper.callStackDeathEvent(stackEntity, Math.min(stackEntity.getSize(), deathMethod.calculateStep())).getDeathStep();
        int toMultiply = deathStep - 1;
        if (sm.getMainConfig().getBoolean("traits.leashed")) {
            if (event.getEntity().isLeashed() && (stackEntity.getSize() - deathStep) != 0) {
                event.getEntity().setMetadata(Utilities.NO_LEASH_METADATA, new FixedMetadataValue(sm, true));
            }
        }
        if (deathStep < stackEntity.getSize()) {
            if (sm.getMainConfig().isSkipDeathAnimation(event.getEntityType())) {
                toMultiply = deathStep;
                event.setCancelled(true);
                stackEntity.incrementSize(-deathStep);
                deathMethod.onSpawn(stackEntity);
            } else {
                sm.getServer().getScheduler().runTask(sm, () -> {
                    final StackEntity spawned = stackEntity.duplicate(stackEntity.getSize() - deathStep);
                    deathMethod.onSpawn(spawned);
                    stackEntity.removeStackData();
                });
            }
        }
        if (toMultiply == 0) {
            return;
        }
        final Drops drop = stackEntity.getDrops();
        final int experience = drop.calculateDeathExperience(toMultiply, event.getDroppedExp());
        final Map<ItemStack, Integer> drops = drop.calculateDrops(toMultiply, event.getDrops());
        Drops.dropItems(event.getEntity().getLocation(), drops);
        if (Utilities.isPaper() && event.isCancelled()) {
            final ExperienceOrb orb = (ExperienceOrb) event.getEntity().getWorld().spawnEntity(event.getEntity().getLocation(), EntityType.EXPERIENCE_ORB);
            orb.setExperience(experience);

            if (Utilities.isVersionAtLeast(Utilities.MinecraftVersion.V1_18_R1) && event.getEntity() instanceof final Zombie zombie && !zombie.isAdult()) {
                NMSHelper.resetBabyZombieExp(zombie);
            }
        } else {
            event.setDroppedExp(experience);
        }
        if (sm.getMainConfig().isPlayerStatMulti(event.getEntityType())) {
            if (event.getEntity().getKiller() != null) {
                event.getEntity().getKiller().incrementStatistic(Statistic.KILL_ENTITY, event.getEntityType(), toMultiply);
            }
        }
        if (event.getEntity() instanceof Slime && sm.getMainConfig().isSlimeMultiEnabled()) {
            event.getEntity().setMetadata("deathcount", new FixedMetadataValue(sm, toMultiply));
        }
    }

    public DeathMethod calculateDeath(StackEntity entity) {
        final DeathType deathType = sm.getMainConfig().getDeathType(entity.getEntity());
        try {
            return deathType.getStepClass().getDeclaredConstructor(StackMob.class, StackEntity.class).newInstance(sm, entity);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("Error while determining death step!");
        }
    }

}
