package uk.antiperson.stackmob.commands.subcommands;

import org.bukkit.entity.*;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.commands.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@CommandMetadata(command = "remove", playerReq = true, desc = "Remove all entities")
public class Remove extends SubCommand {

    private final StackMob sm;
    public Remove(StackMob sm) {
        super(CommandArgument.construct(ArgumentType.STRING, true, Arrays.asList("animals", "hostile")));
        this.sm = sm;
    }

    @Override
    public boolean onCommand(User sender, String[] args) {
        Player player = (Player) sender.getSender();
        Function<Entity, Boolean> function = entity -> entity instanceof Mob;
        if (args.length == 1) {
            switch (args[0]) {
                case "animals":
                    function = entity -> entity instanceof Animals;
                    break;
                case "hostile":
                    function = entity -> entity instanceof Monster;
                    break;
            }
        }
        List<LivingEntity> entities = player.getWorld().getLivingEntities();
        for (LivingEntity entity : entities) {
            if (!function.apply(entity)) {
                continue;
            }
            if (!sm.getEntityManager().isStackedEntity(entity)) {
                continue;
            }
            entity.remove();
        }
        sender.sendSuccess("Entities matching your criteria have been removed.");
        return false;
    }

}
