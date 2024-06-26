package uk.antiperson.stackmob;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import uk.antiperson.stackmob.commands.Commands;
import uk.antiperson.stackmob.config.EntityTranslation;
import uk.antiperson.stackmob.config.MainConfig;
import uk.antiperson.stackmob.entity.EntityManager;
import uk.antiperson.stackmob.entity.traits.TraitManager;
import uk.antiperson.stackmob.hook.HookManager;
import uk.antiperson.stackmob.listeners.*;
import uk.antiperson.stackmob.packets.PlayerManager;
import uk.antiperson.stackmob.scheduler.BukkitScheduler;
import uk.antiperson.stackmob.scheduler.FoliaScheduler;
import uk.antiperson.stackmob.scheduler.Scheduler;
import uk.antiperson.stackmob.tasks.MergeTask;
import uk.antiperson.stackmob.tasks.TagCheckTask;
import uk.antiperson.stackmob.tasks.TagMoveTask;
import uk.antiperson.stackmob.utils.ItemTools;
import uk.antiperson.stackmob.utils.Updater;
import uk.antiperson.stackmob.utils.Utilities;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public class StackMob extends JavaPlugin {

    public static final boolean IS_FOLIA;

    static {
        boolean f = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            f = true;
        } catch (ClassNotFoundException ignored) {
        }
        IS_FOLIA = f;
    }

    private final NamespacedKey stackKey = new NamespacedKey(this, "stack-size");
    private final NamespacedKey toolKey = new NamespacedKey(this, "stack-tool");

    private static StackMob INSTANCE;

    private MainConfig config;
    private EntityTranslation entityTranslation;
    private TraitManager traitManager;
    private HookManager hookManager;
    private EntityManager entityManager;
    private Updater updater;
    private ItemTools itemTools;
    private PlayerManager playerManager;
    private Scheduler scheduler;

    @Override
    public void onLoad() {
        INSTANCE = this;
        hookManager = new HookManager(this);
        try {
            hookManager.registerOnLoad();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            getLogger().log(Level.SEVERE, "There was a problem registering hooks. Features won't work.");
            e.printStackTrace();
        }
        scheduler = Utilities.IS_FOLIA ? new FoliaScheduler() : new BukkitScheduler();
    }

    @Override
    public void onEnable() {
        traitManager = new TraitManager(this);
        entityManager = new EntityManager(this);
        config = new MainConfig(this);
        entityTranslation = new EntityTranslation(this);
        updater = new Updater(this, 29999);
        itemTools = new ItemTools(this);
        playerManager = new PlayerManager(this);
        getLogger().info("StackMob v" + getDescription().getVersion() + " by antiPerson and contributors.");
        getLogger().info("GitHub: " + Utilities.GITHUB + " Discord: " + Utilities.DISCORD);
        getLogger().info("Loading config files...");
        try {
            getMainConfig().load();
            getEntityTranslation().load();
            getMainConfig().cache();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "There was a problem loading the configuration file.");
            e.printStackTrace();
        }
        getLogger().info("Registering hooks and trait checks...");
        try {
            getHookManager().registerHooks();
            getTraitManager().registerTraits();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }
        getLogger().info("Registering events, commands and tasks...");
        try {
            registerEvents();
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
        PluginCommand command = getCommand("stackmob");
        Commands commands = new Commands(this);
        command.setExecutor(commands);
        command.setTabCompleter(commands);
        commands.registerSubCommands();
        final int stackInterval = getMainConfig().getStackInterval();
        getScheduler().runGlobalTaskTimer(this, new MergeTask(this), 20, stackInterval);
        final int tagInterval = getMainConfig().getTagNearbyInterval();
        getScheduler().runGlobalTaskTimer(this, new TagCheckTask(this), 30, tagInterval);
        if (getMainConfig().isTagNearbyArmorStandEnabled()) {
            getScheduler().runGlobalTaskTimer(this, new TagMoveTask(this), 10, 1);
        }
        getLogger().info("Detected server version " + Utilities.getMinecraftVersion());
        if (getHookManager().getProtocolLibHook() == null) {
            getLogger().warning("ProtocolLib could not be found (or has been disabled). The display name visibility setting 'NEARBY' will not work unless this is fixed.");
        }
        getEntityManager().registerAllEntities();
        getUpdater().checkUpdate().whenComplete(((updateResult, throwable) -> {
            switch (updateResult.getResult()) {
                case NONE -> getLogger().info("No update is currently available.");
                case ERROR -> getLogger().info("There was an error while getting the latest update.");
                case AVAILABLE ->
                        getLogger().info("A new version is currently available. (" + updateResult.getNewVersion() + ")");
            }
        }));
        new Metrics(this, 522);
    }

    @Override
    public void onDisable() {
        getEntityManager().unregisterAllEntities();
        Bukkit.getOnlinePlayers().forEach(player -> getPlayerManager().stopWatching(player));
    }

    private void registerEvents() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        registerEvent(PlayerArmorStandListener.class);
        registerEvent(BucketListener.class);
        registerEvent(DeathListener.class);
        registerEvent(TransformListener.class);
        registerEvent(BreedInteractListener.class);
        registerEvent(TagInteractListener.class);
        registerEvent(DyeListener.class);
        registerEvent(ShearListener.class);
        registerEvent(ExplosionListener.class);
        registerEvent(DropListener.class);
        registerEvent(TameListener.class);
        registerEvent(SlimeListener.class);
        registerEvent(SpawnListener.class);
        registerEvent(TargetListener.class);
        registerEvent(PlayerListener.class);
        registerEvent(BeeListener.class);
        registerEvent(LeashListener.class);
        registerEvent(EquipListener.class);
        if (Utilities.isVersionAtLeast(Utilities.MinecraftVersion.V1_20_4)) {
            registerEvent(KnockbackListener.class);
        }
        if (Utilities.isPaper()) {
            registerEvent(RemoveListener.class);
        } else {
            registerEvent(ChunkListener.class);
        }
    }

    private void registerEvent(Class<? extends Listener> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        ListenerMetadata listenerMetadata = clazz.getAnnotation(ListenerMetadata.class);
        if (listenerMetadata != null) {
            String[] keys = listenerMetadata.config();
            boolean enabled = false;
            int i = 0;

            while (!enabled && i < keys.length) {
                String key = keys[i];
                enabled = getMainConfig().isSet(key) && getMainConfig().getBoolean(key);
                i++;
            }

            if (!enabled)
                return;
        }
        Listener listener = clazz.getDeclaredConstructor(StackMob.class).newInstance(this);
        getServer().getPluginManager().registerEvents(listener, this);
    }

    public static StackMob getInstance() {
        return INSTANCE;
    }

    public EntityTranslation getEntityTranslation() {
        return entityTranslation;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public MainConfig getMainConfig() {
        return config;
    }

    public TraitManager getTraitManager() {
        return traitManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public Updater getUpdater() {
        return updater;
    }

    public NamespacedKey getStackKey() {
        return stackKey;
    }

    public NamespacedKey getToolKey() {
        return toolKey;
    }

    public ItemTools getItemTools() {
        return itemTools;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

}
