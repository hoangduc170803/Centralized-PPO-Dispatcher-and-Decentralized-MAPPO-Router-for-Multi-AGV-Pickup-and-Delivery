package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * Current operating mode of the AGV. For additional information, see the table
 * OperatingModes in chapter 6.10.6.
 */
public enum OperatingMode {
    AUTOMATIC, MANUAL, SEMIAUTOMATIC, SERVICE, TEACHIN;

    @JsonValue
    public String toValue() {
        switch (this) {
            case AUTOMATIC: return "AUTOMATIC";
            case MANUAL: return "MANUAL";
            case SEMIAUTOMATIC: return "SEMIAUTOMATIC";
            case SERVICE: return "SERVICE";
            case TEACHIN: return "TEACHIN";
        }
        return null;
    }

    @JsonCreator
    public static OperatingMode forValue(String value) throws IOException {
        if (value.equals("AUTOMATIC")) return AUTOMATIC;
        if (value.equals("MANUAL")) return MANUAL;
        if (value.equals("SEMIAUTOMATIC")) return SEMIAUTOMATIC;
        if (value.equals("SERVICE")) return SERVICE;
        if (value.equals("TEACHIN")) return TEACHIN;
        throw new IOException("Cannot deserialize OperatingMode");
    }
}
