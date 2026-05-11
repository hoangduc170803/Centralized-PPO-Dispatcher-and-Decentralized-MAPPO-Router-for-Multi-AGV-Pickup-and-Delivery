package cfg.aubot.itteam.simulator.telegrams.predefined;



import java.util.HashMap;
import java.util.Map;

public class AgvErrorMessage extends MqttTelegram {

    private Map<String, String> errors = new HashMap<>();

    public AgvErrorMessage(String thingName, Map<String, String> errors) {
        super(thingName, "error");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append("Errors: \n");
        errors.entrySet().forEach(entry -> {
            String error = entry.getKey();
            String message = entry.getValue();
            builder.append(String.format(" %s - %s", error, message));
        });
        return builder.toString();
    }
}
