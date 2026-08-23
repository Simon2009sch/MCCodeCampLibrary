package me.simoncrafter.mCCodeCampLibrary.internal.editor.editables;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfigEditQuestion.ConfigEditQuestion;
import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import me.simoncrafter.CraftersDisplayLibrary.core.interfaces.IDisplayable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface IEditable {

    void onPlayerEnterEditor(Player player);
    void onPlayerLeaveEditor(Player player);
    Set<Interaction> getActivationEntityList();
    boolean isPlayerInEditingRage(Player player);
    void onPlayerSelect(Player player);
    void onPlayerDeselect(Player player);

    PositionObject getDisplay();
    void spawnDisplay();
    void despawnDisplay();
    /**
     * Recursively converts a {@link ConfigurationNode} into a plain {@code Map<String, Object>}.
     * <p>
     * Map children are recursed into nested maps; list children are converted to lists of
     * the recursively-converted elements; scalar values are read as their raw Java values.
     * The result is a deep mutable structure suitable for
     * {@code ConfigEditData.create(...)} (the chat config editor edits maps in place).
     *
     * @param config the node to convert (typically {@code getConfig()})
     * @return a deep, mutable map representation of the node's contents
     */
    default Map<String, Object> configToMap(ConfigurationNode config) {
        return configToMap(config, new HashMap<>());
    }

    private Map<String, Object> configToMap(ConfigurationNode config, Map<String, Object> out) {
        if (config == null || config.virtual()) {
            return out;
        }
        if (config.isMap()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : config.childrenMap().entrySet()) {
                String key = String.valueOf(entry.getKey());
                ConfigurationNode child = entry.getValue();
                if (child.isMap()) {
                    out.put(key, configToMap(child, new HashMap<>()));
                } else if (child.isList()) {
                    out.put(key, listToMapList(child));
                } else {
                    out.put(key, scalarValue(child));
                }
            }
        } else if (config.isList()) {
            // Not reachable via the map branch; kept for completeness if called directly on a list node.
            out.put(String.valueOf(config.key()), listToMapList(config));
        }
        return out;
    }

    private java.util.List<Object> listToMapList(ConfigurationNode listNode) {
        java.util.List<Object> list = new java.util.ArrayList<>();
        for (ConfigurationNode child : listNode.childrenList()) {
            if (child.isMap()) {
                list.add(configToMap(child, new HashMap<>()));
            } else if (child.isList()) {
                list.add(listToMapList(child));
            } else {
                list.add(scalarValue(child));
            }
        }
        return list;
    }

    /**
     * Reads the raw value of a scalar node. {@code SerializationException} is not
     * expected for scalar reads (it is declared by the generic {@code get}); a failure
     * falls back to {@code null} rather than propagating.
     */
    private Object scalarValue(ConfigurationNode node) {
        try {
            return node.get(Object.class);
        } catch (SerializationException e) {
            return null;
        }
    }
}

