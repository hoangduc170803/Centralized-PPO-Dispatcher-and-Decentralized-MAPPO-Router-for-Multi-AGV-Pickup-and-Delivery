package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * Acknowledge type of eStop.
 * AUTOACK: auto-acknowledgeable e-stop is activated e.g. by bumper or protective field.
 * MANUAL: e-stop has to be acknowledged manually at the vehicle.
 * REMOTE: facility e-stop has to be acknowledged remotely.
 * NONE: no e-stop activated.
 */
public enum EStop {
    AUTOACK, MANUAL, NONE, REMOTE;

    @JsonValue
    public String toValue() {
        switch (this) {
            case AUTOACK: return "AUTOACK";
            case MANUAL: return "MANUAL";
            case NONE: return "NONE";
            case REMOTE: return "REMOTE";
        }
        return null;
    }

    @JsonCreator
    public static EStop forValue(String value) throws IOException {
        if (value.equals("AUTOACK")) return AUTOACK;
        if (value.equals("MANUAL")) return MANUAL;
        if (value.equals("NONE")) return NONE;
        if (value.equals("REMOTE")) return REMOTE;
        throw new IOException("Cannot deserialize EStop");
    }
}
