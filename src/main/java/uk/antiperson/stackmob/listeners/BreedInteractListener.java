package uk.antiperson.stackmob.listeners;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.EntityFood;
import uk.antiperson.stackmob.entity.StackEntity;
import uk.antiperson.stackmob.utils.Utilities;

@ListenerMetadata(config = "events.breed.enabled")
public class BreedInteractListener implements Listener {

    private final StackMob sm;

    public BreedInteractListener(StackMob sm) {
        this.sm = sm;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreedInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Animals)) {
            return;
        }
        final Animals animals = (Animals) event.getRightClicked();
        if (!animals.canBreed()) {
            return;
        }
        final Player player = event.getPlayer();
        final ItemStack foodItem = player.getInventory().getItemInMainHand();
        if (!EntityFood.isCorrectFood(event.getRightClicked(), foodItem.getType())) {
            return;
        }
        final StackEntity stackEntity = sm.getEntityManager().getStackEntity(animals);
        if (stackEntity == null || stackEntity.isSingle()) {
            return;
        }
        final ListenerMode breed = sm.getMainConfig().getListenerMode(animals.getType(), "breed");
        if (breed == ListenerMode.SPLIT) {
            stackEntity.slice();
            return;
        }
        final int itemAmount = player.getInventory().getItemInMainHand().getAmount();
        stackEntity.splitIfNotEnough(itemAmount);
        if (itemAmount == 1) {
            Utilities.removeHandItem(player, 1);
            return;
        }
        final int kidAmount = sm.getMainConfig().getEventMultiplyLimit(animals.getType(), "breed", stackEntity.getSize() / 2);
        final int parentAmount = kidAmount * 2;
        if (stackEntity.getSize() > parentAmount) {
            stackEntity.slice(parentAmount);
        }
        Utilities.removeHandItem(player, parentAmount);
        stackEntity.getDrops().dropExperience(event.getRightClicked().getLocation(), 1, 7, kidAmount);
        // Spawn the kid
        stackEntity.spawnChild(kidAmount);
        // Update the adult
        animals.setBreed(false);
        animals.setBreedCause(player.getUniqueId());
    }

}
