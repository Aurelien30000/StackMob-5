package uk.antiperson.stackmob.hook.hooks;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.config.ConfigList;
import uk.antiperson.stackmob.hook.Hook;
import uk.antiperson.stackmob.hook.HookMetadata;
import uk.antiperson.stackmob.hook.StackableMobHook;

@HookMetadata(name = "MythicMobs", config = "mythicmobs.enabled")
public class MythicMobsHook extends Hook implements StackableMobHook {

    private MythicMobs mythicMobs;
    public MythicMobsHook(StackMob sm) {
        super(sm);
    }

    @Override
    public boolean isMatching(LivingEntity first, LivingEntity nearby) {
        ActiveMob activeMobO = mythicMobs.getMobManager().getMythicMobInstance(first);
        ActiveMob activeMobN = mythicMobs.getMobManager().getMythicMobInstance(nearby);
        if(!(activeMobO.getType().equals(activeMobN.getType()))){
            return false;
        }
        ConfigList list = sm.getMainConfig().getList(first.getType(), "hooks.mythicmobs.blacklist");
        return !list.contains(activeMobN.getType().getInternalName());
    }

    @Override
    public LivingEntity spawnClone(Location location, LivingEntity dead) {
        ActiveMob activeMob = mythicMobs.getMobManager().getMythicMobInstance(dead);
        ActiveMob clone = mythicMobs.getMobManager().spawnMob(activeMob.getType().getInternalName(), location);
        if(clone != null){
            return (LivingEntity) clone.getEntity().getBukkitEntity();
        }
        return null;
    }

    @Override
    public String getDisplayName(LivingEntity entity) {
        String displayName =  mythicMobs.getMobManager().getMythicMobInstance(entity).getDisplayName();
        return displayName != null ? displayName : "MythicMob";
    }

    @Override
    public boolean isCustomMob(LivingEntity entity) {
        if (mythicMobs.getMobManager().isActiveMob(entity.getUniqueId())) {
            ActiveMob activeMob = mythicMobs.getMobManager().getMythicMobInstance(entity);
            return !activeMob.getType().getConfig().getFile().getName().equals("VanillaMobs.yml");
        }
        return false;
    }

    @Override
    public void onEnable() {
        mythicMobs = (MythicMobs) getPlugin();
    }
}
