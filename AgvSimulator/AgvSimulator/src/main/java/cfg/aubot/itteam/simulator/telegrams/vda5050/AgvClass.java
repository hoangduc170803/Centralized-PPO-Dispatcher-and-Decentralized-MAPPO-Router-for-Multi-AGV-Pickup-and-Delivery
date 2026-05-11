package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * Simplified description of AGV class.
 */
public enum AgvClass {
    CARRIER, CONVEYOR, FORKLIFT, TUGGER;

    @JsonValue
    public String toValue() {
        switch (this) {
            case CARRIER: return "CARRIER";
            case CONVEYOR: return "CONVEYOR";
            case FORKLIFT: return "FORKLIFT";
            case TUGGER: return "TUGGER";
        }
        return null;
    }

    @JsonCreator
    public static AgvClass forValue(String value) throws IOException {
        if (value.equals("CARRIER")) return CARRIER;
        if (value.equals("CONVEYOR")) return CONVEYOR;
        if (value.equals("FORKLIFT")) return FORKLIFT;
        if (value.equals("TUGGER")) return TUGGER;
        throw new IOException("Cannot deserialize AgvClass");
    }
}
