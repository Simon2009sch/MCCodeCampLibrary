package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.simoncrafter.mCCodeCampLibrary.utility.JsonConfig;

public abstract class AEditable {

    protected String ID = "";
    protected JsonConfig config = new JsonConfig();

    protected void setConfigValue(String path, Object obj) {
        config.set(path, obj);
    }

    protected JsonConfig getConfig() {
        return config;
    }

    protected void setConfig(JsonConfig config) {
        this.config = config;
    }

    protected String getSaveDataString() {
        JsonObject data = new JsonObject();
        data.addProperty("id", ID);
        data.add("config", config.getRoot());
        return data.toString();
    }

    protected AEditable parseSaveDataString(String data) {
        JsonObject root = JsonParser.parseString(data).getAsJsonObject();
        this.ID = root.has("id") ? root.get("id").getAsString() : "";
        this.config = root.has("config") ? new JsonConfig(root.getAsJsonObject("config")) : new JsonConfig();
        return this;
    }

    protected String getID() {
        return ID;
    }

}
