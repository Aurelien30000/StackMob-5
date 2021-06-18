package uk.antiperson.stackmob.utils;

import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class NMSHelper {

    public static void sendPacket(Player player, Entity entity, boolean tagVisible) {
        final CraftEntity craftEntity = (CraftEntity) entity;
        final DataWatcher watcher = new DataWatcher(craftEntity.getHandle());
        watcher.register(new DataWatcherObject<>(3, DataWatcherRegistry.i), tagVisible);
        final PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(craftEntity.getHandle().getId(), watcher, true);
        ((CraftPlayer) player).getHandle().b.sendPacket(packet);
    }

}
