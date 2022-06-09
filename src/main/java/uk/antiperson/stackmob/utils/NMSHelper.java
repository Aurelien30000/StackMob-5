package uk.antiperson.stackmob.utils;

import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.boss.wither.EntityWither;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class NMSHelper {

    public static void sendPacket(Player player, Entity entity, boolean tagVisible) {
        final CraftEntity craftEntity = (CraftEntity) entity;
        final DataWatcher watcher = new DataWatcher(craftEntity.getHandle());
        watcher.a(new DataWatcherObject<>(3, DataWatcherRegistry.i), tagVisible);
        PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(craftEntity.getEntityId(), watcher, true);
        ((CraftPlayer) player).getHandle().b.a(packet);
    }

    public static boolean canCreateWitherRose(LivingEntity entity) {
        EntityLiving entityLiving = ((CraftLivingEntity) entity).getHandle();
        return entityLiving.et() instanceof EntityWither;
    }
}
