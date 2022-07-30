package uk.antiperson.stackmob.entity.traits.trait;

import org.bukkit.entity.Sheep;
import uk.antiperson.stackmob.entity.traits.EntityTrait;
import uk.antiperson.stackmob.entity.traits.TraitMetadata;

@TraitMetadata(path = "sheep-sheared")
public class SheepShear extends EntityTrait<Sheep> {

    @Override
    public boolean checkTrait(Sheep first, Sheep nearby) {
        return first.isSheared() != nearby.isSheared();
    }

    @Override
    public void applyTrait(Sheep spawned, Sheep dead) {
        spawned.setSheared(dead.isSheared());
    }
}
