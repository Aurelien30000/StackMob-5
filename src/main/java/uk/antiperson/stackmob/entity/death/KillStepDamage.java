package uk.antiperson.stackmob.entity.death;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;
import uk.antiperson.stackmob.entity.traits.TraitMetadata;
import uk.antiperson.stackmob.entity.traits.trait.Potion;
import uk.antiperson.stackmob.hook.StackableMobHook;
import uk.antiperson.stackmob.hook.hooks.MythicMobsStackHook;

public class KillStepDamage extends DeathMethod {

    private double leftOverDamage;

    public KillStepDamage(StackMob sm, StackEntity dead) {
        super(sm, dead);
    }

    @Override
    public int calculateStep() {
        if (getDead().getEntity().getLastDamageCause() == null) {
            return 1;
        }
        final double healthBefore = ((LivingEntity) getDead().getEntity().getLastDamageCause().getEntity()).getHealth();
        final double damageDone = getEntity().getLastDamageCause().getFinalDamage();
        double maxHealth = getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        if (getStackMob().getMainConfig().isTraitEnabled(Potion.class.getAnnotation(TraitMetadata.class).path())) {
            if (getDead().getEntity().getPotionEffect(PotionEffectType.HEALTH_BOOST) != null) {
                maxHealth = getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            }
        }
        final double damageLeft = Math.min(maxHealth * (getDead().getSize() - 1), Math.abs(healthBefore - damageDone));
        final double divided = damageLeft / maxHealth;
        final int entities = (int) Math.floor(divided);
        leftOverDamage = (divided - entities) * maxHealth;
        return entities + 1;
    }

    @Override
    public void onSpawn(StackEntity spawned) {
        final AttributeInstance attribute = getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH);
        final AttributeInstance spawnedAttribute = spawned.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH);
        final double maxHealth = Math.min(attribute.getValue(), attribute.getDefaultValue());
        final StackableMobHook smh = sm.getHookManager().getApplicableHook(spawned);
        if (smh instanceof MythicMobsStackHook) {
            sm.getScheduler().runTaskLater(sm, spawned.getEntity(), () -> {
                if (!spawned.getEntity().isDead()) {
                    spawned.getEntity().setHealth(maxHealth - leftOverDamage);
                }
            }, 5);
        }
        try {
            spawned.getEntity().setHealth(maxHealth - leftOverDamage);
        } catch (IllegalArgumentException e) {
            sm.getLogger().warning("New health value is too high! Please report and include the message below.");
            sm.getLogger().info(attribute.getBaseValue() + "," + attribute.getDefaultValue() + "," + attribute.getValue() + "," + leftOverDamage);
            sm.getLogger().info("Type: " + getEntity().getType() + ", Name: " + getEntity().getCustomName() + ", Location: " + getEntity().getLocation());
            if (spawnedAttribute != null) {
                sm.getLogger().info(spawnedAttribute.getBaseValue() + "," + spawnedAttribute.getDefaultValue() + "," + spawnedAttribute.getValue());
            }
        }
    }
}
