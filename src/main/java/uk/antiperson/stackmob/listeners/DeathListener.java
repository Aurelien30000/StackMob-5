package uk.antiperson.stackmob.listeners;

import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
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
import uk.antiperson.stackmob.utils.Utilities;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class DeathListener implements Listener {

    private final StackMob sm;

    public DeathListener(StackMob sm) {
        this.sm = sm;
    }

    @EventHandler(ignoreCancelled = true)
    public void onStackDeath(EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        final StackEntity stackEntity = sm.getEntityManager().getStackEntity(entity);
        if (stackEntity == null) {
            return;
        }
        final DeathMethod deathMethod = calculateDeath(stackEntity);
        final int deathStep = EventHelper.callStackDeathEvent(stackEntity, Math.min(stackEntity.getSize(), deathMethod.calculateStep())).getDeathStep();
        int toMultiply = deathStep - 1;
        if (sm.getMainConfig().getBoolean("traits.leashed")) {
            if (entity.isLeashed() && (stackEntity.getSize() - deathStep) != 0) {
                entity.setMetadata(Utilities.NO_LEASH_METADATA, new FixedMetadataValue(sm, true));
            }
        }
        boolean isSkipDeathAnimation = sm.getMainConfig().isSkipDeathAnimation(event.getEntityType());
        if (deathStep < stackEntity.getSize()) {
            if (isSkipDeathAnimation) {
                toMultiply = deathStep;
                event.setCancelled(true);
                stackEntity.incrementSize(-deathStep);
                deathMethod.onSpawn(stackEntity);
            } else {
                sm.getScheduler().runTask(sm, event.getEntity(), () -> {
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
        // Workaround for craftbukkit bug?/change
        // Enchantment effects are now applied after the death event is fired....
        // Should probably investigate more...? How are the drops in the event correct.
        if (Utilities.isVersionAtLeast(Utilities.MinecraftVersion.V1_21) && sm.getMainConfig().isDropLootTables(entity.getType())) {
            final int finalToMultiply = toMultiply;
            final Runnable runnable = () -> doDrops(entity, drop, finalToMultiply, event.getDrops(), isSkipDeathAnimation);
            sm.getScheduler().runTaskLater(sm, stackEntity.getEntity(), runnable, 1);
        } else {
            doDrops(entity, drop, toMultiply, event.getDrops(), isSkipDeathAnimation);
        }
        if (isSkipDeathAnimation && Utilities.isPaper()) {
            final ExperienceOrb orb = (ExperienceOrb) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.EXPERIENCE_ORB);
            orb.setExperience(experience);
        } else {
            event.setDroppedExp(experience);
        }
        if (sm.getMainConfig().isPlayerStatMulti(event.getEntityType())) {
            if (entity.getKiller() != null) {
                entity.getKiller().incrementStatistic(Statistic.KILL_ENTITY, event.getEntityType(), toMultiply);
            }
        }
        if (entity instanceof Slime && sm.getMainConfig().isSlimeMultiEnabled()) {
            entity.setMetadata("deathcount", new FixedMetadataValue(sm, toMultiply));
        }
    }

    public DeathMethod calculateDeath(StackEntity entity) {
        final DeathType deathType = sm.getMainConfig().getDeathType(entity.getEntity());
        try {
            return deathType.getStepClass().getDeclaredConstructor(StackMob.class, StackEntity.class).newInstance(sm, entity);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Error while determining death step!");
        }
    }

    private void doDrops(LivingEntity entity, Drops drop, int toMultiply, List<ItemStack> drops, boolean isSkipDeathAnimation) {
        final Map<ItemStack, Integer> map = drop.calculateDrops(toMultiply, drops, isSkipDeathAnimation);
        Drops.dropItems(entity.getLocation(), map);
    }

}
