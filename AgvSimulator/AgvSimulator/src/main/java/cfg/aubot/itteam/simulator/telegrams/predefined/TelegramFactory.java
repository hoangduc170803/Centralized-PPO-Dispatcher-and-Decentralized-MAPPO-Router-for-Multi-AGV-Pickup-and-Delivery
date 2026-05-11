package cfg.aubot.itteam.simulator.telegrams.predefined;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;


public class TelegramFactory {
    private static final Gson GSON = new Gson();

    public static MqttTelegram parse(String json) throws JsonParseException {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);

        String type = jsonObject.get("type").getAsString();
        switch (type) {
            case "order-request":
                return GSON.fromJson(jsonObject, OrderRequestMessage.class);
            case "state":
                return GSON.fromJson(jsonObject, AgvStateMessage.class);
            case "order-response":
                jsonObject.addProperty("state", jsonObject.get("state").getAsString().toUpperCase());
                return GSON.fromJson(jsonObject, OrderResponseMessage.class);
            case "route":
                return GSON.fromJson(jsonObject, RouteMessage.class);
            case "error":
                return GSON.fromJson(jsonObject, AgvErrorMessage.class);
            default:
                throw new JsonParseException("Unknown telegram");
        }
    }

    public static String toJson(MqttTelegram telegram) {
        return GSON.toJson(telegram);
    }
}