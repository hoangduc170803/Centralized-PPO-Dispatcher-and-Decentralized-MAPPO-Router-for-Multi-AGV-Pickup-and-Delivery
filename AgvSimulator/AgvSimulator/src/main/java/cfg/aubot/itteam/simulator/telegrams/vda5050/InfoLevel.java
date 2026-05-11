package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * Info level.
 * DEBUG: used for debugging.
 * INFO: used for visualization.
 */
public enum InfoLevel {
    DEBUG, INFO;

    @JsonValue
    public String toValue() {
        switch (this) {
            case DEBUG: return "DEBUG";
            case INFO: return "INFO";
        }
        return null;
    }

    @JsonCreator
    public static InfoLevel forValue(String value) throws IOException {
        if (value.equals("DEBUG")) return DEBUG;
        if (value.equals("INFO")) return INFO;
        throw new IOException("Cannot deserialize InfoLevel");
    }
}
