package uk.antiperson.stackmob.entity;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import uk.antiperson.stackmob.StackMob;

public class StackEntity {

    private LivingEntity entity;
    private StackMob sm;
    public StackEntity(StackMob sm, LivingEntity entity) {
        this.sm = sm;
        this.entity = entity;
    }

    /**
     * Sets the size of this stack.
     * @param newSize the size this stack should be changed to.
     */
    public void setSize(int newSize) {
        setSize(newSize, true);
    }

    /**
     * Sets the size of this stack.
     * @param newSize the size this stack should be changed to.
     */
    public void setSize(int newSize, boolean update) {
        if (newSize < 1) {
            throw new IllegalArgumentException("Stack size can not be less than one!");
        }
        if (newSize > getMaxSize()) {
            throw new IllegalArgumentException("Stack size cannot be more than the configured stack size!");
        }
        entity.getPersistentDataContainer().set(sm.getStackKey(), PersistentDataType.INTEGER, newSize);
        if (update) {
            getTag().update();
        }
    }

    public void removeStackData() {
        entity.getPersistentDataContainer().remove(sm.getStackKey());
        getTag().update();
    }

    public boolean shouldWait(CreatureSpawnEvent.SpawnReason spawnReason) {
        if (!sm.getMainConfig().isWaitingEnabled(getEntity().getType())) {
            return false;
        }
        if (!sm.getMainConfig().getWaitingTypes(getEntity().getType()).contains(getEntity().getType().toString())) {
            return false;
        }
        if (!sm.getMainConfig().getWaitingReasons(getEntity().getType()).contains(spawnReason.toString())) {
            return false;
        }
        return true;
    }

    public void makeWait() {
        int time = sm.getMainConfig().getWaitingTime(getEntity().getType());
        getEntity().getPersistentDataContainer().set(sm.getWaitKey(), PersistentDataType.INTEGER, time);
    }

    public void incrementWait() {
        int currentWaiting = getEntity().getPersistentDataContainer().getOrDefault(sm.getWaitKey(), PersistentDataType.INTEGER, 0);
        if (currentWaiting < 1) {
            getEntity().getPersistentDataContainer().remove(sm.getWaitKey());
            setSize(1);
            return;
        }
        getEntity().getPersistentDataContainer().set(sm.getWaitKey(), PersistentDataType.INTEGER, currentWaiting - 1);
    }

    /**
     * Increments the stack size by the value given.
     * @param increment increment for stack size.
     */
    public void incrementSize(int increment) {
        setSize(getSize() + increment);
    }

    /**
     * Gets the current stack size for this entity.
     * @return the current stack size for this entity.
     */
    public int getSize() {
        return entity.getPersistentDataContainer().getOrDefault(sm.getStackKey(), PersistentDataType.INTEGER, 1);
    }

    /**
     * Gets the maximum stack size.
     * @return the maximum stack size
     */
    public int getMaxSize() {
        return sm.getMainConfig().getMaxStack(getEntity().getType());
    }

    /**
     * Removes this entity.
     */
    public void remove() {
        entity.remove();
    }

    /**
     * Returns the LivingEntity of this stack.
     * @return the LivingEntity of this stack.
     */
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * Returns the world the entity is in.
     * @return the world the entity is in.
     */
    public World getWorld() {
        return entity.getWorld();
    }

    /**
     * Returns a new instance of Tag for this entity.
     * @return a new instance of Tag for this entity.
     */
    public Tag getTag() {
        return new Tag(sm, this);
    }

    /**
     * Returns a new instance of Drops for this entity.
     * @return a new instance of Drops for this entity.
     */
    public Drops getDrops() {
        return new Drops(sm, this);
    }

    /**
     * Check if the stack is at its maximum size.
     * @return if the stack is at its maximum size.
     */
    public boolean isMaxSize() {
        return getSize() == getMaxSize();
    }

    /**
     * Check if the given entity and this entity should stack.
     * @param nearby another entity
     * @return if the given entity and this entity should stack.
     */
    public boolean checkNearby(StackEntity nearby) {
        if (getEntity().getType() != nearby.getEntity().getType()) {
            return false;
        }
        if (nearby.isMaxSize()) {
            return false;
        }
        if (sm.getTraitManager().checkTraits(this, nearby)) {
            return false;
        }
        if (sm.getHookManager().checkHooks(this, nearby)) {
            return false;
        }
        if (nearby.getEntity().isDead() || getEntity().isDead()) {
            return false;
        }
        return true;
    }

    /**
     * Merge this stack with another stack, providing they are similar.
     * @param toMerge stack to merge with.
     * @return whether the merge was successful
     */
    public boolean merge(StackEntity toMerge) {
        StackEntity entity1 = toMerge.getSize() < getSize() ? toMerge : this;
        StackEntity entity2 = toMerge.getSize() < getSize() ? this : toMerge;
        int totalSize = entity1.getSize() + entity2.getSize();
        if (totalSize > getMaxSize()) {
            toMerge.setSize(totalSize - entity2.getMaxSize());
            setSize(entity2.getMaxSize());
            return true;
        }
        entity2.incrementSize(entity1.getSize());
        entity1.remove();
        return true;
    }

    public StackEntity splitIfNotEnough(int itemAmount) {
        // If there is not enough food, then spawn a new stack with the remaining.
        if (getSize() > itemAmount) {
            StackEntity notFed = duplicate();
            notFed.setSize(getSize() - itemAmount);
            setSize(itemAmount);
            return notFed;
        }
        return null;
    }

    /**
     * Creates a clone of this entity.
     * @return a clone of this entity.
     */
    public StackEntity duplicate() {
        LivingEntity entity = sm.getHookManager().spawnClone(getEntity().getLocation(), this);
        entity = entity == null ? (LivingEntity) getWorld().spawnEntity(getEntity().getLocation(), getEntity().getType()) : entity;
        StackEntity stackEntity = sm.getEntityManager().getStackEntity(entity);
        stackEntity.setSize(1);
        sm.getTraitManager().applyTraits(stackEntity, this);
        sm.getHookManager().onSpawn(stackEntity);
        return stackEntity;
    }

    public boolean isSingle() {
        return getSize() < 2;
    }

    /**
     * Makes this stack singular and spawns another stack with the remaining stack size.
     * @return stack with the remaining stack size.
     */
    public StackEntity slice() {
        if (isSingle()) {
            throw new UnsupportedOperationException("Stack size must be greater than 1 to slice!");
        }
        StackEntity duplicate = duplicate();
        duplicate.setSize(getSize() - 1);
        setSize(1);
        return duplicate;
    }

}
