package uk.antiperson.stackmob.tasks;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;
import uk.antiperson.stackmob.utils.Utilities;

import java.util.Set;

// This is a very shitty way to port this task to be Folia compatible, but it works.
public class FoliaMergeTask implements Runnable {

    private final StackMob sm;

    public FoliaMergeTask(StackMob sm) {
        this.sm = sm;
    }

    @Override
    public void run() {
        final boolean checkHasMoved = sm.getMainConfig().isCheckHasMoved();
        final double checkHasMovedDistance = sm.getMainConfig().getCheckHasMovedDistance();
        for (StackEntity original : sm.getEntityManager().getStackEntities()) {
            original.getEntity().getScheduler().run(sm, scheduledTask -> {
                if (original.isWaiting()) {
                    original.incrementWait();
                    return;
                }
                if (!original.canStack()) {
                    if (!original.getEntity().isValid()) {
                        sm.getEntityManager().unregisterStackedEntity(original);
                    }
                    return;
                }
                if (checkHasMoved) {
                    if (original.getEntity().getWorld().equals(original.getLastLocation().getWorld())) {
                        if (original.getEntity().getLocation().distance(original.getLastLocation()) < checkHasMovedDistance) {
                            return;
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
                    if (!nearbyStack.canStack()) {
                        if (!original.getEntity().isValid()) {
                            sm.getEntityManager().unregisterStackedEntity(original);
                        }
                        continue;
                    }
                    if (!original.match(nearbyStack)) {
                        continue;
                    }
                    if (!stackThresholdEnabled || (nearbyStack.getSize() > 1 || original.getSize() > 1)) {
                        final StackEntity removed = nearbyStack.merge(original, false);
                        if (removed != null) {
                            sm.getEntityManager().unregisterStackedEntity(removed);
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
                    sm.getEntityManager().unregisterStackedEntity(match);
                }
                if (size + original.getSize() > original.getMaxSize()) {
                    final int toCompleteStack = (original.getMaxSize() - original.getSize());
                    original.incrementSize(toCompleteStack);
                    for (int stackSize : Utilities.split(size - toCompleteStack, original.getMaxSize())) {
                        original.duplicate(stackSize);
                    }
                    return;
                }
                original.incrementSize(size);
            }, () -> {
            });
        }
    }
}