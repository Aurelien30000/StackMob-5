package uk.antiperson.stackmob.entity.traits;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;
import uk.antiperson.stackmob.entity.traits.trait.*;
import uk.antiperson.stackmob.utils.Utilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class TraitManager {

    private final Map<EntityType, Set<Trait<LivingEntity>>> traitsPerEntity;
    private final StackMob sm;

    public TraitManager(StackMob sm) {
        this.sm = sm;
        this.traitsPerEntity = new EnumMap<>(EntityType.class);
    }

    public void registerTraits() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        registerTrait(SheepColor.class);
        registerTrait(SheepShear.class);
        registerTrait(HorseColor.class);
        registerTrait(SlimeSize.class);
        registerTrait(LlamaColor.class);
        registerTrait(ParrotVariant.class);
        registerTrait(CatType.class);
        registerTrait(MooshroomVariant.class);
        registerTrait(FoxType.class);
        registerTrait(Age.class);
        registerTrait(BreedMode.class);
        registerTrait(LoveMode.class);
        registerTrait(DrownedItem.class);
        registerTrait(ZombieBaby.class);
        registerTrait(BeeNectar.class);
        registerTrait(BeeStung.class);
        registerTrait(Leash.class);
        registerTrait(Potion.class);
        registerTrait(VillagerProfession.class);
        registerTrait(ZoglinBaby.class);
        registerTrait(PiglinBaby.class);
        registerTrait(Aware.class);
        if (Utilities.isPaper()) {
            registerTrait(TurtleHasEgg.class);
        }
        if (Utilities.isVersionAtLeast(Utilities.MinecraftVersion.V1_19_R1)) {
            registerTrait(AllayOwner.class);
            registerTrait(FrogVariant.class);
        }
    }

    /**
     * If a class hasn't been disabled in the config, add this to the hashset so it can be looped over.
     *
     * @param traitClass class that implements trait
     * @throws IllegalAccessException    if class is not accessible
     * @throws InstantiationException    if class can not be instantiated
     * @throws NoSuchMethodException     if class constructor can not be found
     * @throws InvocationTargetException if instantiation fails
     */
    private <T extends Trait<? extends LivingEntity>> void registerTrait(Class<T> traitClass) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        final TraitMetadata traitMetadata = traitClass.getAnnotation(TraitMetadata.class);
        if (!sm.getMainConfig().isTraitEnabled(traitMetadata.path())) {
            return;
        }
        final Trait<LivingEntity> trait = (Trait<LivingEntity>) traitClass.getDeclaredConstructor().newInstance();

        for (EntityType entityType : EntityType.values()) {
            if (entityType.isAlive() && isTraitApplicable(trait, entityType.getEntityClass())) {
                traitsPerEntity.computeIfAbsent(entityType, unused -> new ObjectOpenHashSet<>()).add(trait);
            }
        }
    }

    /**
     * Check if the two given entities have any non-matching characteristics which prevent stacking.
     *
     * @param first  1st entity to check
     * @param nearby entity to compare with
     * @return if these entities have any not matching characteristics (traits.)
     */
    public boolean checkTraits(StackEntity first, StackEntity nearby) {
        for (Trait<LivingEntity> trait : traitsPerEntity.get(first.getEntity().getType())) {
            if (trait.checkTrait(first.getEntity(), nearby.getEntity())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Apply the characteristics of the dead entity to the newly spawned entity.
     *
     * @param spawned the entity that the traits should be copied to.
     * @param dead    the entity which traits should be copied from.
     */
    public void applyTraits(StackEntity spawned, StackEntity dead) {
        for (Trait<LivingEntity> trait : traitsPerEntity.get(spawned.getEntity().getType())) {
            trait.applyTrait(spawned.getEntity(), dead.getEntity());
        }
    }

    /**
     * Check if the trait is applicable to the given entity.
     *
     * @param trait the trait to check.
     * @param clazz the class of the give entity to check.
     * @return if the trait is applicable to the given entity.
     */
    private boolean isTraitApplicable(Trait<LivingEntity> trait, Class<? extends Entity> clazz) {
        final ParameterizedType parameterizedType = (ParameterizedType) trait.getClass().getGenericInterfaces()[0];
        final Class<?> typeArgument = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        return typeArgument.isAssignableFrom(clazz);
    }

}
