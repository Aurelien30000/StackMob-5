package uk.antiperson.stackmob.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;
import uk.antiperson.stackmob.utils.StackingTool;

public class PlayerListener implements Listener {

    private final StackMob sm;
    public PlayerListener(StackMob sm) {
        this.sm = sm;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Mob)) {
            return;
        }
        if (!sm.getItemTools().isStackingTool(event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }
        if (!sm.getEntityManager().isStackedEntity((LivingEntity) event.getRightClicked())) {
            return;
        }
        StackingTool stackingTool = new StackingTool(sm, event.getPlayer());
        if (event.getPlayer().isSneaking()) {
            stackingTool.shiftMode();
            return;
        }
        StackEntity stackEntity = sm.getEntityManager().getStackEntity((LivingEntity) event.getRightClicked());
        stackingTool.performAction(stackEntity);
    }
}
