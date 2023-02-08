package uk.antiperson.stackmob.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.utils.Utilities;

import java.util.Collections;
import java.util.List;

public abstract class SpecialConfigFile extends ConfigFile {

    public SpecialConfigFile(StackMob sm, String filePath) {
        super(sm, filePath);
    }

    public ConfigList getList(EntityType type, String path) {
        ConfigValue configValue = get(type, path);
        configValue = configValue.value() instanceof List<?> ? configValue : new ConfigValue(path, Collections.emptyList());
        final boolean inverted = getBoolean(path + "-invert");
        return new ConfigList(this, (List<?>) configValue.value(), path, inverted);
    }

    public boolean getBoolean(EntityType type, String path) {
        Object value = getValue(type, path);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    public double getDouble(EntityType type, String path) {
        Object value = getValue(type, path);
        if (value instanceof Integer) {
            return Utilities.toInt(value.toString());
        }
        return value instanceof Double ? Utilities.toDouble(value.toString()) : 0;
    }

    public int getInt(EntityType type, String path) {
        Object value = getValue(type, path);
        return value instanceof Number ? Utilities.toInt(value.toString()) : 0;
    }

    public String getString(EntityType type, String path) {
        Object value = getValue(type, path);
        return value == null ? null : value.toString();
    }

    public ConfigurationSection getConfigurationSection(EntityType type, String path) {
        Object value = getValue(type, path);
        return value == null ? null : (ConfigurationSection) value;
    }

    private Object getValue(EntityType type, String path) {
        return get(type, path).value();
    }

    private ConfigValue get(EntityType type, String path) {
        if (!isFileLoaded()) {
            throw new UnsupportedOperationException("Configuration file has not been loaded!");
        }
        // Check if this entity clones another entity.
        String typeName = getString("custom." + type + ".clone", type.toString()).toUpperCase();
        // Check if the specified general config path is overridden by an entity specific equivalent.
        String customPath = "custom." + typeName + "." + path;
        Object customValue = get(customPath);
        if (customValue != null) {
            return new ConfigValue(customPath, customValue);
        }
        return new ConfigValue(path, get(path));
    }

}
