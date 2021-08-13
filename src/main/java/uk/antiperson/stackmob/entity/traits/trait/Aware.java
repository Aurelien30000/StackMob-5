package uk.antiperson.stackmob.entity.traits.trait;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import uk.antiperson.stackmob.entity.traits.Trait;
import uk.antiperson.stackmob.entity.traits.TraitMetadata;

@TraitMetadata(entity = Mob.class, path = "no-ai")
public class Aware implements Trait {

    @Override
    public boolean checkTrait(LivingEntity first, LivingEntity nearby) {
        return ((Mob) first).isAware() != ((Mob) nearby).isAware();
    }

    @Override
    public void applyTrait(LivingEntity spawned, LivingEntity dead) {
        ((Mob) spawned).setAware(((Mob) dead).isAware());
    }
}
