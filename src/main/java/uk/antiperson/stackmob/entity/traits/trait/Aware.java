package uk.antiperson.stackmob.entity.traits.trait;

import org.bukkit.entity.Mob;
import uk.antiperson.stackmob.entity.traits.Trait;
import uk.antiperson.stackmob.entity.traits.TraitMetadata;

@TraitMetadata(path = "no-ai")
public class Aware implements Trait<Mob> {

    @Override
    public boolean checkTrait(Mob first, Mob nearby) {
        return first.isAware() != nearby.isAware();
    }

    @Override
    public void applyTrait(Mob spawned, Mob dead) {
        spawned.setAware(dead.isAware());
    }
}
