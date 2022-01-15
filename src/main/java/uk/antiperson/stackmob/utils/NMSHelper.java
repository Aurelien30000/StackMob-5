package uk.antiperson.stackmob.utils;

import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.entity.EntityInsentient;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftZombie;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.lang.reflect.Field;

public class NMSHelper {

    private static Field XP_REWARD;

    static {
        try {
            XP_REWARD = EntityInsentient.class.getDeclaredField("bM");
            XP_REWARD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void sendPacket(Player player, Entity entity, boolean tagVisible) {
        final CraftEntity craftEntity = (CraftEntity) entity;
        final DataWatcher watcher = new DataWatcher(craftEntity.getHandle());
        watcher.a(new DataWatcherObject<>(3, DataWatcherRegistry.i), tagVisible);
        PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(craftEntity.getEntityId(), watcher, true);
        ((CraftPlayer) player).getHandle().b.a(packet);
    }

    public static void resetBabyZombieExp(Zombie zombie) {
        try {
            XP_REWARD.set(((CraftZombie) zombie).getHandle(), 5);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
