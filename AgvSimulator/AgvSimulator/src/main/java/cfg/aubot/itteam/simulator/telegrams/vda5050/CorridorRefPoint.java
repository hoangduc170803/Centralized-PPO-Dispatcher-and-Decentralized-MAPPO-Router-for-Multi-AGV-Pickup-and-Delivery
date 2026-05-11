package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * Defines whether the boundaries are valid for the kinematic center or the contour of the
 * vehicle.
 */
public enum CorridorRefPoint {
    CONTOUR, KINEMATICCENTER;

    @JsonValue
    public String toValue() {
        switch (this) {
            case CONTOUR: return "CONTOUR";
            case KINEMATICCENTER: return "KINEMATICCENTER";
        }
        return null;
    }

    @JsonCreator
    public static CorridorRefPoint forValue(String value) throws IOException {
        if (value.equals("CONTOUR")) return CONTOUR;
        if (value.equals("KINEMATICCENTER")) return KINEMATICCENTER;
        throw new IOException("Cannot deserialize CorridorRefPoint");
    }
}
