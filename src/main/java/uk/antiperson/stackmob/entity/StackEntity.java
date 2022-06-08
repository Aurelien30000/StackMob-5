package uk.antiperson.stackmob.entity;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.events.EventHelper;
import uk.antiperson.stackmob.hook.StackableMobHook;
import uk.antiperson.stackmob.hook.hooks.ProtocolLibHook;
import uk.antiperson.stackmob.utils.NMSHelper;
import uk.antiperson.stackmob.utils.Utilities;

import java.util.Set;

public class StackEntity {

    private static final EntityManager entityManager = StackMob.getInstance().getEntityManager();

    private final LivingEntity entity;
    private final StackMob sm;
    private boolean waiting;
    private int waitCount;
    private boolean forgetOnSpawn;
    private boolean removed;
    private Location lastLocation;
    private int stackSize;
    private Set<ItemStack> equiptItems;
    private Tag tag;

    public StackEntity(StackMob sm, LivingEntity entity) {
        this.sm = sm;
        this.entity = entity;
    }

    /**
     * Sets the size of this stack.
     *
     * @param newSize the size this stack should be changed to.
     */
    public void setSize(int newSize) {
        setSize(newSize, true);
    }

    /**
     * Sets the size of this stack.
     *
     * @param newSize the size this stack should be changed to.
     */
    public void setSize(int newSize, boolean update) {
        if (newSize < 1) {
            throw new IllegalArgumentException("Stack size can not be less than one!");
        }
        if (newSize > getMaxSize()) {
            sm.getLogger().info("New stack size for entity (with id: " + getEntity().getEntityId()
                    + ") is bigger than the allowed maximum. Setting to the configured maximum value.");
            newSize = getMaxSize();
        }
        entity.getPersistentDataContainer().set(sm.getStackKey(), PersistentDataType.INTEGER, newSize);
        stackSize = newSize;
        if (update) {
            getTag().update();
        }
    }

    public void removeStackData() {
        entity.getPersistentDataContainer().remove(sm.getStackKey());
        entity.setCustomNameVisible(false);
        entityManager.unregisterStackedEntity(this);
        if (!isSingle()) {
            getTag().update();
        }
    }

    /**
     * Returns the location of the entity where it was last checked for stacking.
     *
     * @return the location of the entity where it was last checked for stacking, null if 'stack.check-location' is disabled.
     */
    public Location getLastLocation() {
        if (lastLocation == null && sm.getMainConfig().isCheckHasMoved()) {
            lastLocation = entity.getLocation();
        }
        return lastLocation;
    }

    /**
     * Sets the location of the entity where it was last checked for stacking.
     *
     * @param lastLocation the location of the entity where it was last checked for stacking.
     */
    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    /**
     * Returns whether the entity will have its stack data removed on spawn.
     * See {@link #setForgetOnSpawn(boolean)} for further details.
     *
     * @return whether the entity will have its stack data removed on spawn.
     */
    public boolean isForgetOnSpawn() {
        return forgetOnSpawn;
    }

    /**
     * Make it so that the entity will have its stack data removed in the spawn event.
     * This will only work if the entity already has stack data.
     * If you are a plugin developer, use {@link uk.antiperson.stackmob.events.StackSpawnEvent} instead.
     *
     * @param forgetOnSpawn whether the entity should have its stack data removed in the spawn event.
     */
    public void setForgetOnSpawn(boolean forgetOnSpawn) {
        this.forgetOnSpawn = forgetOnSpawn;
    }

    /**
     * In order to not break mob grinders, stacked entities can have a waiting status.
     * This waiting status means that this stacked entity will be ignored on all stacking attempts until the count reaches 0
     *
     * @return whether this entity is currently waiting
     */
    public boolean isWaiting() {
        return waiting;
    }

    /**
     * Gets the wait count for this entity.
     *
     * @return the wait count for this entity
     */
    public int getWaitCount() {
        return waitCount;
    }

    /**
     * Whether this entity should wait.
     *
     * @param spawnReason the spawn reason of the entity.
     * @return whether this entity should wait.
     */
    public boolean shouldWait(CreatureSpawnEvent.SpawnReason spawnReason) {
        if (!sm.getMainConfig().isWaitingEnabled(getEntity().getType())) {
            return false;
        }
        if (!sm.getMainConfig().isWaitingTypes(getEntity().getType())) {
            return false;
        }
        return sm.getMainConfig().isWaitingReasons(getEntity().getType(), spawnReason);
    }

    /**
     * In order to not break mob grinders, stacked entities can have a waiting status.
     * This waiting status means that this stacked entity will be ignored on all stacking attempts until the count reaches 0.
     */
    public void makeWait() {
        if (isWaiting()) {
            throw new UnsupportedOperationException("Stack is already waiting!");
        }
        waitCount = sm.getMainConfig().getWaitingTime(getEntity().getType());
        waiting = true;
    }

    /**
     * Increment the waiting count.
     */
    public void incrementWait() {
        if (waitCount < 1) {
            waiting = false;
            setSize(1);
            return;
        }
        waitCount -= 1;
    }

    /**
     * Increments the stack size by the value given.
     *
     * @param increment increment for stack size.
     */
    public void incrementSize(int increment) {
        setSize(getSize() + increment);
    }

    /**
     * Gets the current stack size for this entity.
     *
     * @return the current stack size for this entity.
     */
    public int getSize() {
        if (stackSize == 0) {
            stackSize = getEntity().getPersistentDataContainer().getOrDefault(sm.getStackKey(), PersistentDataType.INTEGER, 1);
        }
        return stackSize;
    }

    /**
     * Gets the maximum stack size.
     *
     * @return the maximum stack size
     */
    public int getMaxSize() {
        return sm.getMainConfig().getMaxStack(getEntity().getType());
    }

    /**
     * Removes this entity.
     */
    public void remove() {
        remove(true);
    }

    public void remove(boolean unregister) {
        setRemoved();
        entity.remove();
        if (unregister) {
            entityManager.unregisterStackedEntity(this);
        }
        if (getEntity().isLeashed()) {
            ItemStack leash = new ItemStack(Material.LEAD, 1);
            getWorld().dropItemNaturally(entity.getLocation(), leash);
        }
        dropEquipItems();
    }

    /**
     * Returns the LivingEntity of this stack.
     *
     * @return the LivingEntity of this stack.
     */
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * Returns the world the entity is in.
     *
     * @return the world the entity is in.
     */
    public World getWorld() {
        return entity.getWorld();
    }

    /**
     * Returns a new instance of Tag for this entity.
     *
     * @return a new instance of Tag for this entity.
     */
    public Tag getTag() {
        if (tag == null) {
            tag = new Tag();
        }
        return tag;
    }

    /**
     * Returns a new instance of Drops for this entity.
     *
     * @return a new instance of Drops for this entity.
     */
    public Drops getDrops() {
        return new Drops(sm, this);
    }

    /**
     * Check if the stack is at its maximum size.
     *
     * @return if the stack is at its maximum size.
     */
    public boolean isMaxSize() {
        return getSize() == getMaxSize();
    }

    /**
     * Check if the given entity and this entity should stack.
     *
     * @param nearby another entity
     * @return if the given entity and this entity should stack.
     */
    public boolean match(StackEntity nearby) {
        if (getEntity().getType() != nearby.getEntity().getType()) {
            return false;
        }
        if (sm.getTraitManager().checkTraits(this, nearby)) {
            return false;
        }
        return !sm.getHookManager().checkHooks(this, nearby);
    }

    public boolean canStack() {
        if (hasEquipItem()) {
            if (sm.getMainConfig().getEquipItemMode(getEntity().getType()) == EquipItemMode.PREVENT_STACK) {
                return false;
            }
        }
        return Utilities.isPaper() ? entity.isTicking() && !getEntity().isDead() && !isMaxSize() && !isWaiting() : !getEntity().isDead() && !isMaxSize() && !isWaiting();
    }

    /**
     * Merge this stack with another stack, providing they are similar.
     *
     * @param toMerge    stack to merge with.
     * @param unregister whether to unregister the entity that is removed.
     * @return the entity that was removed
     */
    public StackEntity merge(StackEntity toMerge, boolean unregister) {
        final boolean toMergeBigger = toMerge.getSize() > getSize();
        final StackEntity smallest = toMergeBigger ? this : toMerge;
        final StackEntity biggest = toMergeBigger ? toMerge : this;
        if (EventHelper.callStackMergeEvent(smallest, biggest).isCancelled()) {
            return null;
        }
        final int totalSize = smallest.getSize() + biggest.getSize();
        final int maxSize = getMaxSize();
        if (totalSize > maxSize) {
            smallest.setSize(totalSize - maxSize);
            biggest.setSize(maxSize);
            return null;
        }
        mergePotionEffects(biggest, smallest);
        biggest.incrementSize(smallest.getSize());
        smallest.remove(unregister);
        return smallest;
    }

    public void mergePotionEffects(StackEntity toKeep, StackEntity toRemove) {
        if (!sm.getMainConfig().isTraitEnabled("potion-effect")) {
            return;
        }
        for (PotionEffect potionEffect : toRemove.getEntity().getActivePotionEffects()) {
            final double combinedSize = toKeep.getSize() + toRemove.getSize();
            final double percentOfTotal = toRemove.getSize() / combinedSize;
            final int newDuration = (int) Math.ceil(potionEffect.getDuration() * percentOfTotal);
            final PotionEffect newPotion = new PotionEffect(potionEffect.getType(), newDuration, potionEffect.getAmplifier());
            toKeep.getEntity().addPotionEffect(newPotion);
        }
    }

    public StackEntity splitIfNotEnough(int itemAmount) {
        // If there is not enough food, then spawn a new stack with the remaining.
        if (getSize() > itemAmount) {
            return slice(itemAmount);
        }
        return null;
    }

    /**
     * Creates a clone of this entity.
     *
     * @param amount amount to
     * @return a clone of this entity.
     */
    public StackEntity duplicate(int amount) {
        final LivingEntity clone = spawnClone();
        final StackEntity cloneStack = sm.getEntityManager().registerStackedEntity(clone);
        cloneStack.setSize(amount);
        duplicateTraits(cloneStack);
        return cloneStack;
    }

    public void spawnChild(int kidAmount) {
        // Spawn the kid
        StackEntity kid;
        if (Utilities.isVersionAtLeast(Utilities.MinecraftVersion.V1_19_R1) && getEntity().getType() == EntityType.valueOf("FROG")) {
            // tadpoles and frogs are separate entities
            LivingEntity tadpole = spawn(EntityType.valueOf("TADPOLE"));
            kid = sm.getEntityManager().registerStackedEntity(tadpole);
            kid.setSize(1);
            duplicateTraits(kid);
        } else {
            kid = duplicate(1);
            ((Animals) kid.getEntity()).setBaby();
        }
        kid.setSize(kidAmount);
    }

    private void duplicateTraits(StackEntity cloneStack) {
        LivingEntity clone = cloneStack.getEntity();
        sm.getTraitManager().applyTraits(cloneStack, this);
        sm.getHookManager().onSpawn(cloneStack);
        if (Utilities.isPaper()) {
            // Remove equipment if is a drowned spawned by drowning
            if (clone.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.DROWNED) {
                for (EquipmentSlot equipmentSlot : Utilities.HAND_SLOTS) {
                    if (clone.getEquipment() == null) {
                        break;
                    }
                    for (Material material : Utilities.DROWNED_MATERIALS) {
                        if (clone.getEquipment().getItem(equipmentSlot).getType() != material) {
                            continue;
                        }
                        clone.getEquipment().setItem(equipmentSlot, null, true);
                    }
                }
            }
        }
    }

    private LivingEntity spawnClone() {
        final LivingEntity entity = sm.getHookManager().spawnClone(getEntity().getLocation(), this);
        if (entity != null) {
            return entity;
        }
        return spawn(getEntity().getType());
    }

    private LivingEntity spawn(EntityType entityType) {
        if (Utilities.isPaper()) {
            return (LivingEntity) getWorld().spawnEntity(getEntity().getLocation(), entityType, getEntity().getEntitySpawnReason());
        }
        return (LivingEntity) getWorld().spawnEntity(getEntity().getLocation(), entityType);
    }

    public boolean isSingle() {
        return getSize() == 1;
    }

    /**
     * Makes this stack smaller by 1 and spawns another stack with the remaining stack size.
     *
     * @return stack with the remaining stack size.
     */
    public StackEntity slice() {
        return slice(1);
    }

    /**
     * Makes this stack smaller and spawns another stack with the remaining stack size.
     *
     * @param amount amount to
     * @return stack with the remaining stack size.
     */
    public StackEntity slice(int amount) {
        if (isSingle()) {
            throw new UnsupportedOperationException("Stack size must be greater than 1 to slice!");
        }
        if (amount >= getSize()) {
            throw new UnsupportedOperationException("Slice amount is bigger than the stack size!");
        }
        final StackEntity duplicate = duplicate(getSize() - amount);
        setSize(amount);
        if (getEntity().isLeashed()) {
            duplicate.getEntity().setLeashHolder(getEntity().getLeashHolder());
            getEntity().setLeashHolder(null);
        }
        return duplicate;
    }

    public Set<ItemStack> getEquiptItems() {
        return equiptItems;
    }

    public boolean hasEquipItem() {
        return getEquiptItems() != null && !getEquiptItems().isEmpty();
    }

    public void addEquipItem(ItemStack equipt) {
        if (!hasEquipItem()) {
            equiptItems = new ObjectOpenHashSet<>();
        }
        equiptItems.add(equipt);
    }

    private void dropEquipItems() {
        if (!hasEquipItem()) {
            return;
        }
        if (sm.getMainConfig().getEquipItemMode(getEntity().getType()) != EquipItemMode.DROP_ITEMS) {
            return;
        }
        for (ItemStack itemStack : equiptItems) {
            getEntity().getWorld().dropItemNaturally(getEntity().getLocation(), itemStack);
        }
    }

    public boolean isRemoved() {
        return removed;
    }

    private void setRemoved() {
        this.removed = true;
    }


    public enum EquipItemMode {
        IGNORE,
        DROP_ITEMS,
        PREVENT_STACK
    }


    public enum TagMode {
        ALWAYS,
        HOVER,
        NEARBY
    }

    public class Tag {

        public void update() {
            LivingEntity entity = getEntity();
            int threshold = sm.getMainConfig().getTagThreshold(entity.getType());
            if (getSize() <= threshold) {
                entity.setCustomName(null);
                entity.setCustomNameVisible(false);
                return;
            }
            String displayName = sm.getMainConfig().getTagFormat(entity.getType());
            displayName = StringUtils.replace(displayName, "%type%", getEntityName());
            displayName = StringUtils.replace(displayName, "%size%", getSize() + "");
            displayName = Utilities.translateColorCodes(displayName);
            entity.setCustomName(displayName);
            if (sm.getMainConfig().getTagMode(entity.getType()) == TagMode.ALWAYS) {
                entity.setCustomNameVisible(true);
            }
        }

        private String getEntityName() {
            LivingEntity entity = getEntity();
            String typeString = sm.getEntityTranslation().getTranslatedName(entity.getType());
            if (typeString != null && typeString.length() > 0) {
                return typeString;
            }
            StackableMobHook smh = sm.getHookManager().getApplicableHook(StackEntity.this);
            typeString = smh != null ? smh.getDisplayName(entity) : entity.getType().toString();
            typeString = typeString == null ? entity.getType().toString() : typeString;
            return WordUtils.capitalizeFully(typeString.replaceAll("[^A-Za-z0-9]", " "));
        }

        public void sendPacket(Player player, boolean tagVisible) {
            if (Utilities.getMinecraftVersion() != Utilities.NMS_VERSION) {
                ProtocolLibHook protocolLibHook = sm.getHookManager().getProtocolLibHook();
                if (protocolLibHook == null) {
                    return;
                }
                protocolLibHook.sendPacket(player, getEntity(), tagVisible);
                return;
            }
            NMSHelper.sendPacket(player, getEntity(), tagVisible);
        }
    }

}
