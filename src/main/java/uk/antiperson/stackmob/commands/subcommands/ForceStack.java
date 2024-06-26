package uk.antiperson.stackmob.commands.subcommands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.commands.*;

import java.util.Arrays;
import java.util.function.Predicate;

@CommandMetadata(command = "forcestack", playerReq = false, desc = "Force all currently loaded entities to stack.")
public class ForceStack extends SubCommand {

    private final StackMob sm;

    public ForceStack(StackMob sm) {
        super(CommandArgument.construct(ArgumentType.STRING, true, Arrays.asList("named", "tamed", "chunk")));
        this.sm = sm;
    }

    @Override
    public boolean onCommand(User sender, String[] args) {
        int count = 0;
        Predicate<LivingEntity> predicate = null;
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "named" ->
                        predicate = pEntity -> pEntity.customName() != null && pEntity.customName() != Component.empty();
                case "tamed" -> predicate = pEntity -> (pEntity instanceof Tameable) && ((Tameable) pEntity).isTamed();
                case "chunk" -> {
                    if (!(sender.sender() instanceof Player)) {
                        sender.sendError("You need to be a player!");
                        return false;
                    }
                    predicate = pEntity -> pEntity.getLocation().getChunk() == ((Player) sender.sender()).getLocation().getChunk();
                }
            }
        }
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getEntitiesByClass(Mob.class)) {
                if (sm.getEntityManager().isStackedEntity(entity)) {
                    continue;
                }
                if (predicate != null && !predicate.test(entity)) {
                    continue;
                }
                if (sm.getMainConfig().isEntityBlacklisted(entity)) {
                    continue;
                }
                sm.getEntityManager().registerStackedEntity(entity).setSize(1);
                count++;
            }
        }

        final String entityType = predicate != null ? args[0].toLowerCase() + " " : "";
        sender.sendSuccess(count + " " + entityType + "entities have been forced to stack!");
        return false;
    }
}
