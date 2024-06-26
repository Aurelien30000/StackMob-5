package uk.antiperson.stackmob.entity;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.packets.NMSHelper;
import uk.antiperson.stackmob.utils.Utilities;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Drops {

    private final StackMob sm;
    private final LivingEntity dead;

    public Drops(StackMob sm, StackEntity entity) {
        this.sm = sm;
        this.dead = entity.getEntity();
    }

    public Map<ItemStack, Integer> calculateDrops(int deathAmount, List<ItemStack> originalDrops, boolean skipDeathAnimation) {
        final Object2IntMap<ItemStack> items = new Object2IntOpenHashMap<>();
        if (!sm.getMainConfig().isDropMultiEnabled(dead.getType())) {
            return items;
        }
        if (sm.getMainConfig().isDropTypeBlacklist(dead.getType())) {
            return items;
        }
        final EntityDamageEvent lastDamageCause = dead.getLastDamageCause();
        if (lastDamageCause == null || sm.getMainConfig().isDropReasonBlacklist(dead.getType(), lastDamageCause.getCause())) {
            return items;
        }
        final boolean useLootTables = sm.getMainConfig().isDropLootTables(dead.getType());
        final LootContext lootContext;
        Collection<ItemStack> genItems;
        if (useLootTables) {
            lootContext = new LootContext.Builder(dead.getLocation()).lootedEntity(dead).killer(dead.getKiller()).build();
            genItems = null;
        } else {
            lootContext = null;
            genItems = originalDrops.stream().map(ItemStack::clone).toList();
        }
        for (int i = 0; i < deathAmount; i++) {
            if (useLootTables) {
                genItems = ((Mob) dead).getLootTable().populateLoot(ThreadLocalRandom.current(), lootContext);
            }
            for (ItemStack is : genItems) {
                if (is == null || is.getType() == Material.AIR) {
                    continue;
                }
                if (dropIsArmor(is)) {
                    continue;
                }
                if (is.getType() == Material.LEAD && dead.isLeashed()) {
                    continue;
                }
                if (sm.getMainConfig().isDropItemBlacklisted(dead.getType(), is.getType())) {
                    continue;
                }
                final int dropAmount = sm.getMainConfig().isDropItemOnePer(dead.getType(), is.getType()) ? 1 : is.getAmount();
                is.setAmount(1);
                items.mergeInt(is, dropAmount, Integer::sum);
            }
        }

        // Special case with death skip animation and a Wither kill. This handles/fixes wither rose dropping everytime.
        if (skipDeathAnimation && NMSHelper.canCreateWitherRose(dead)) {
            // Simulates LivingEntity#createWitherRose, deathAmount times.
            items.put(new ItemStack(Material.WITHER_ROSE), deathAmount);
        }

        return items;
    }

    public static void dropItems(Location location, Map<ItemStack, Integer> items) {
        for (Map.Entry<ItemStack, Integer> entry : items.entrySet())
            dropItem(location, entry.getKey(), entry.getValue());
    }

    public static void dropItem(Location location, ItemStack stack, int amount) {
        dropItem(location, stack, amount, false);
    }

    public static void dropItem(Location location, ItemStack stack, int amount, boolean above) {
        final Location dropLocation = above ? location.clone().add(0, 1, 0) : location;
        for (int itemAmount : Utilities.split(amount, stack.getMaxStackSize())) {
            stack.setAmount(itemAmount);
            location.getWorld().dropItem(dropLocation, stack);
        }
    }

    private boolean dropIsArmor(ItemStack stack) {
        if (dead.getEquipment().getItemInMainHand().equals(stack) || dead.getEquipment().getItemInOffHand().equals(stack)) {
            return true;
        }
        for (ItemStack itemStack : dead.getEquipment().getArmorContents()) {
            if (itemStack.equals(stack)) {
                return true;
            }
        }
        return false;
    }

    public int calculateDeathExperience(int deadCount, int exp) {
        if (!sm.getMainConfig().isExpMultiEnabled(dead.getType())) {
            return exp;
        }
        if (sm.getMainConfig().isExpTypeBlacklist(dead.getType())) {
            return exp;
        }
        double minMulti = sm.getMainConfig().getExpMinBound(dead.getType()) * exp;
        double maxMulti = sm.getMainConfig().getExpMaxBound(dead.getType()) * exp;
        return exp + calculateExperience(minMulti, maxMulti, deadCount);
    }

    private int calculateExperience(double min, double max, int entities) {
        double randRange = min == max ? max : ThreadLocalRandom.current().nextDouble(min, max);
        double randMultiplier = ThreadLocalRandom.current().nextDouble(0.5, 1);
        return (int) Math.round(randRange * randMultiplier * entities);
    }

    public void dropExperience(Location location, int min, int max, int entities) {
        ExperienceOrb exp = (ExperienceOrb) location.getWorld().spawnEntity(location, EntityType.EXPERIENCE_ORB);
        exp.setExperience(calculateExperience(min, max, entities));
    }

}
