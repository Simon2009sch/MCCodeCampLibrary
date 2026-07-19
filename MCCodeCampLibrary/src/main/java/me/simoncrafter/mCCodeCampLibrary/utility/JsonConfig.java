package me.simoncrafter.mCCodeCampLibrary.utility;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Dot-path accessor over a Gson {@link JsonObject}, e.g. {@code set("position.x", 3)}. */
public class JsonConfig {

    private final JsonObject root;

    public JsonConfig() {
        this.root = new JsonObject();
    }

    public JsonConfig(JsonObject root) {
        this.root = root != null ? root : new JsonObject();
    }

    public static JsonConfig fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new JsonConfig();
        }
        JsonElement parsed = JsonParser.parseString(json);
        return new JsonConfig(parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject());
    }

    public JsonObject getRoot() {
        return root;
    }

    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = current.get(parts[i]);
            JsonObject child;
            if (next != null && next.isJsonObject()) {
                child = next.getAsJsonObject();
            } else {
                child = new JsonObject();
                current.add(parts[i], child);
            }
            current = child;
        }
        current.add(parts[parts.length - 1], toElement(value));
    }

    public JsonElement get(String path) {
        String[] parts = path.split("\\.");
        JsonElement current = root;
        for (String part : parts) {
            if (current == null || !current.isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject().get(part);
        }
        return current;
    }

    public boolean has(String path) {
        JsonElement value = get(path);
        return value != null && !value.isJsonNull();
    }

    public String getString(String path, String fallback) {
        JsonElement value = get(path);
        return value != null && !value.isJsonNull() ? value.getAsString() : fallback;
    }

    public double getDouble(String path, double fallback) {
        JsonElement value = get(path);
        return value != null && !value.isJsonNull() ? value.getAsDouble() : fallback;
    }

    public int getInt(String path, int fallback) {
        JsonElement value = get(path);
        return value != null && !value.isJsonNull() ? value.getAsInt() : fallback;
    }

    public boolean getBoolean(String path, boolean fallback) {
        JsonElement value = get(path);
        return value != null && !value.isJsonNull() ? value.getAsBoolean() : fallback;
    }

    private static JsonElement toElement(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof JsonElement element) {
            return element;
        }
        return new Gson().toJsonTree(value);
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
