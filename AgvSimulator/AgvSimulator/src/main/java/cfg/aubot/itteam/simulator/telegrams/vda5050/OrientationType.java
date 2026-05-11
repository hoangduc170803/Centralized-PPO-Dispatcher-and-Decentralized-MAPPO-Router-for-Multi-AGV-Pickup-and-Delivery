package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * Enum {GLOBAL, TANGENTIAL}:
 * "GLOBAL"- relative to the global project specific map coordinate system;
 * "TANGENTIAL"- tangential to the edge.
 * If not defined, the default value is "TANGENTIAL".
 */
public enum OrientationType {
    GLOBAL, TANGENTIAL;

    @JsonValue
    public String toValue() {
        switch (this) {
            case GLOBAL: return "GLOBAL";
            case TANGENTIAL: return "TANGENTIAL";
        }
        return null;
    }

    @JsonCreator
    public static OrientationType forValue(String value) throws IOException {
        if (value.equals("GLOBAL")) return GLOBAL;
        if (value.equals("TANGENTIAL")) return TANGENTIAL;
        throw new IOException("Cannot deserialize OrientationType");
    }
}
