package uk.antiperson.stackmob.tasks;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;
import uk.antiperson.stackmob.utils.Utilities;

import java.util.Set;

public class MergeTask implements Runnable {

    private final StackMob sm;

    public MergeTask(StackMob sm) {
        this.sm = sm;
    }

    private void checkEntity(StackEntity original, IntSet toRemove, boolean checkHasMoved, double checkHasMovedDistance) {
        if (toRemove.contains(original.getEntity().getEntityId())) {
            return;
        }
        if (original.isWaiting()) {
            original.incrementWait();
            return;
        }
        if (!original.canStack()) {
            if (!original.getEntity().isValid()) {
                removeEntity(toRemove, original);
            }
            return;
        }
        if (checkHasMoved) {
            if (original.getEntity().getWorld().equals(original.getLastLocation().getWorld())) {
                if (!original.skipLastLocation()) {
                    if (original.getEntity().getLocation().distance(original.getLastLocation()) < checkHasMovedDistance) {
                        return;
                    }
                }
            }
            original.setLastLocation(original.getEntity().getLocation());
        }
        final boolean stackThresholdEnabled = sm.getMainConfig().getStackThresholdEnabled(original.getEntity().getType());
        final Integer[] searchRadius = sm.getMainConfig().getStackRadius(original.getEntity().getType());
        final Set<StackEntity> matches = new ObjectOpenHashSet<>();
        for (Entity nearby : original.getEntity().getNearbyEntities(searchRadius[0], searchRadius[1], searchRadius[2])) {
            if (!(nearby instanceof Mob)) {
                continue;
            }
            final StackEntity nearbyStack = sm.getEntityManager().getStackEntity((LivingEntity) nearby);
            if (nearbyStack == null) {
                continue;
            }
            if (toRemove.contains(nearbyStack.getEntity().getEntityId())) {
                continue;
            }
            if (!nearbyStack.canStack()) {
                if (!original.getEntity().isValid()) {
                    removeEntity(toRemove, original);
                }
                continue;
            }
            if (!original.match(nearbyStack)) {
                continue;
            }
            if (!stackThresholdEnabled || (nearbyStack.getSize() > 1 || original.getSize() > 1)) {
                final StackEntity removed = nearbyStack.merge(original, false);
                if (removed != null) {
                    removeEntity(toRemove, removed);
                    if (original == removed) {
                        return;
                    }
                    break;
                }
                continue;
            }
            matches.add(nearbyStack);
        }
        if (!stackThresholdEnabled) {
            return;
        }
        final int threshold = sm.getMainConfig().getStackThreshold(original.getEntity().getType()) - 1;
        final int size = matches.size();
        if (size < threshold) {
            return;
        }
        for (StackEntity match : matches) {
            match.remove(false);
            removeEntity(toRemove, match);
        }
        matches.clear();
        if (size + original.getSize() > original.getMaxSize()) {
            final int toCompleteStack = (original.getMaxSize() - original.getSize());
            original.incrementSize(toCompleteStack);
            for (int stackSize : Utilities.split(size - toCompleteStack, original.getMaxSize())) {
                original.duplicate(stackSize);
            }
            return;
        }
        original.incrementSize(size);
    }

    private void removeEntity(IntSet toRemove, StackEntity stackEntity) {
        if (Utilities.IS_FOLIA) {
            sm.getEntityManager().unregisterStackedEntity(stackEntity);
        } else {
            toRemove.add(stackEntity.getEntity().getEntityId());
        }
    }

    public void run() {
        final IntSet toRemove = new IntOpenHashSet();
        final boolean checkHasMoved = sm.getMainConfig().isCheckHasMoved();
        final double checkHasMovedDistance = sm.getMainConfig().getCheckHasMovedDistance();
        for (StackEntity original : sm.getEntityManager().getStackEntities()) {
            Runnable runnable = () -> checkEntity(original, toRemove, checkHasMoved, checkHasMovedDistance);
            if (Utilities.IS_FOLIA) {
                sm.getScheduler().runTask(sm, original.getEntity(), runnable);
            } else {
                runnable.run();
            }
        }
        for (int stackEntity : toRemove) {
            sm.getEntityManager().unregisterStackedEntity(stackEntity);
        }
    }

}
