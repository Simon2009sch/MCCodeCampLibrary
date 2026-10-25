package me.simoncrafter.mCCodeCampLibrary.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Console/player testing interface. JSON is data only, never a command or script. */
final class WorldMarkerCommand {
    static final String PERMISSION = "mccodecamp.command.edit.worldmarker";
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).disableHtmlEscaping().create();
    private static final List<String> ACTIONS = List.of("create", "remove", "get", "set", "list", "types", "help");
    private static final int MAX_JSON_LENGTH = 32768;

    private WorldMarkerCommand() { }

    static boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            error(sender, "Missing permission: " + PERMISSION);
            return true;
        }
        if (args.length < 2 || args[1].equalsIgnoreCase("help")) {
            help(sender, label);
            return true;
        }
        BlockMarkerRegistry registry = MCCodeCampLib.getBlockMarkerRegistry();
        try {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "create" -> create(sender, registry, parseObject(jsonTail(args, 2)));
                case "remove" -> {
                    requireLength(args, 3, "remove <uuid>");
                    IBlockRegestryObject marker = find(registry, args[2]);
                    registry.removeObject(marker); // Removes the chunk PDC entry as well as the runtime object.
                    if (registry.findRegisteredObject(marker.getUUID()) != null) {
                        throw new IllegalArgumentException("Marker removal failed; check the server log.");
                    }
                    success(sender, "Removed marker " + marker.getUUID());
                }
                case "get" -> {
                    requireLength(args, 3, "get <uuid>");
                    Object config = find(registry, args[2]).getConfig().raw();
                    sendJson(sender, config == null ? new JsonObject() : JSON.toJsonTree(config));
                }
                case "set" -> {
                    if (args.length < 4) throw new IllegalArgumentException("Usage: set <uuid> <config-json>");
                    IBlockRegestryObject marker = find(registry, args[2]);
                    ConfigurationNode config = configuration(parseObject(jsonTail(args, 3)));
                    IBlockRegestryObject updated = registry.replaceObjectConfig(marker.getUUID(), config);
                    if (updated == null) throw new IllegalArgumentException("Config reload failed; check the server log.");
                    success(sender, "Replaced config and reloaded marker " + updated.getUUID() + "; selections/object editors were reset.");
                }
                case "list" -> {
                    requireLength(args, 2, "list");
                    JsonArray entries = new JsonArray();
                    registry.getRegisteredObjects().stream()
                            .sorted(java.util.Comparator.comparing(marker -> marker.getUUID().toString()))
                            .forEach(marker -> {
                                JsonObject entry = new JsonObject();
                                entry.addProperty("uuid", marker.getUUID().toString());
                                entry.addProperty("type", marker.getTypeID());
                                entry.addProperty("id", marker.getID());
                                entry.add("location", locationJson(marker.getLocation()));
                                entries.add(entry);
                            });
                    sendJson(sender, entries);
                }
                case "types" -> {
                    requireLength(args, 2, "types");
                    sendJson(sender, JSON.toJsonTree(registry.getObjectTypeIDs().stream().sorted().toList()));
                }
                default -> throw new IllegalArgumentException("Unknown worldmarker action. Use /" + label + " worldmarker help.");
            }
        } catch (IllegalArgumentException e) {
            error(sender, e.getMessage());
        }
        return true;
    }

    private static void create(CommandSender sender, BlockMarkerRegistry registry, JsonObject request) {
        requireKeys(request, Set.of("type", "id", "location", "config"));
        String type = string(request, "type");
        String id = string(request, "id");
        if (!id.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("id may contain only letters, digits, _ and -.");
        if (!registry.getObjectTypeIDs().contains(type)) throw new IllegalArgumentException("Unknown marker type: " + type);
        if (!request.has("location") || !request.get("location").isJsonObject()) {
            throw new IllegalArgumentException("location must be an object containing world, x, y and z.");
        }
        JsonObject position = request.getAsJsonObject("location");
        requireKeys(position, Set.of("world", "x", "y", "z"));
        World world = Bukkit.getWorld(string(position, "world"));
        if (world == null) throw new IllegalArgumentException("Unknown/unloaded world.");
        double x = coordinate(position, "x"), y = coordinate(position, "y"), z = coordinate(position, "z");
        if (Math.abs(x) > 29999984 || Math.abs(z) > 29999984 || y < world.getMinHeight() || y >= world.getMaxHeight()) {
            throw new IllegalArgumentException("Location is outside the supported world bounds.");
        }
        JsonElement configJson = request.has("config") ? request.get("config") : new JsonObject();
        if (!configJson.isJsonObject()) throw new IllegalArgumentException("config must be a JSON object.");
        ConfigurationNode config = configuration(configJson.getAsJsonObject());
        Location location = new Location(world, x, y, z);
        // Load the target chunk before checking IDs, so existing entries there are discovered.
        location.getChunk();
        if (registry.hasObject(type, id)) {
            throw new IllegalArgumentException("A loaded marker with this type/id already exists. Remove it by UUID first.");
        }
        IBlockRegestryObject marker = registry.createObject(type, id, location, config);
        if (marker == null) throw new IllegalArgumentException("Marker creation failed; check the server log.");
        success(sender, "Created " + type + " " + id + "; UUID: " + marker.getUUID());
    }

    /** Joins Bukkit's remaining argument tokens without stripping the JSON's quotes. */
    private static String jsonTail(String[] args, int offset) {
        if (args.length <= offset) throw new IllegalArgumentException("Missing JSON object. Use worldmarker help.");
        String json = String.join(" ", Arrays.copyOfRange(args, offset, args.length));
        if (json.length() > MAX_JSON_LENGTH) throw new IllegalArgumentException("JSON exceeds " + MAX_JSON_LENGTH + " characters.");
        return json;
    }

    private static JsonObject parseObject(String json) {
        try {
            JsonElement value = JSON.fromJson(json, JsonElement.class);
            if (value == null || !value.isJsonObject()) throw new IllegalArgumentException("JSON must be an object, not an array, scalar or null.");
            return value.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Invalid JSON: use quoted keys/strings, balanced braces, and no comments or Markdown fences.");
        }
    }

    private static ConfigurationNode configuration(JsonObject config) {
        try {
            return GsonConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new StringReader(JSON.toJson(config))))
                    .build().load();
        } catch (ConfigurateException e) {
            throw new IllegalArgumentException("Could not load config JSON: " + e.getMessage());
        }
    }

    private static void requireKeys(JsonObject object, Set<String> allowed) {
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown field: " + key + ". Expected: " + allowed);
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().isBlank()) {
            throw new IllegalArgumentException(key + " must be a nonempty JSON string.");
        }
        return value.getAsString();
    }

    private static double coordinate(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be a finite JSON number.");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) throw new IllegalArgumentException(key + " must be finite.");
        return number;
    }

    private static IBlockRegestryObject find(BlockMarkerRegistry registry, String text) {
        if (!text.matches("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
            throw new IllegalArgumentException("Use a full marker UUID from worldmarker list.");
        }
        IBlockRegestryObject marker = registry.findRegisteredObject(UUID.fromString(text));
        if (marker == null) throw new IllegalArgumentException("Marker is not loaded. Load its chunk first; use worldmarker list for loaded entries.");
        return marker;
    }

    private static JsonObject locationJson(Location location) {
        JsonObject result = new JsonObject();
        result.addProperty("world", location.getWorld().getName());
        result.addProperty("x", location.getX());
        result.addProperty("y", location.getY());
        result.addProperty("z", location.getZ());
        return result;
    }

    private static void requireLength(String[] args, int expected, String usage) {
        if (args.length != expected) throw new IllegalArgumentException("Usage: worldmarker " + usage);
    }

    private static void sendJson(CommandSender sender, JsonElement json) {
        sender.sendMessage(Component.text(JSON.toJson(json)));
    }

    private static void success(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private static void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    private static void help(CommandSender sender, String label) {
        sender.sendMessage(Component.text("/" + label + " worldmarker create <json> | remove <uuid> | get <uuid> | set <uuid> <config-json> | list | types", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("create JSON: {\"type\":\"button\",\"id\":\"test_button\",\"location\":{\"world\":\"world\",\"x\":0,\"y\":64,\"z\":0},\"config\":{\"cooldown\":20,\"blockdata\":\"minecraft:stone_button\"}}"));
        sender.sendMessage(Component.text("get prints only config JSON; set replaces config (not a merge) and reloads the marker with the same UUID. list/get/set/remove cover loaded markers only.", NamedTextColor.GRAY));
    }

    static List<String> complete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) return List.of();
        if (args.length == 2) return prefix(ACTIONS, args[1]);
        if (args.length == 3 && List.of("remove", "get", "set").contains(args[1].toLowerCase(Locale.ROOT))) {
            return prefix(MCCodeCampLib.getBlockMarkerRegistry().getRegisteredObjects().stream()
                    .map(marker -> marker.getUUID().toString()).sorted().toList(), args[2]);
        }
        return List.of();
    }

    private static List<String> prefix(List<String> values, String prefix) {
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).toList();
    }
}
