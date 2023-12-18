package uk.antiperson.stackmob.packets;

import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.boss.wither.EntityWither;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

public class NMSHelper {

    public static boolean canCreateWitherRose(LivingEntity entity) {
        EntityLiving entityLiving = ((CraftLivingEntity) entity).getHandle();
        // EntityLiving#getKillCredit
        return entityLiving.eL() instanceof EntityWither;
    }
}
