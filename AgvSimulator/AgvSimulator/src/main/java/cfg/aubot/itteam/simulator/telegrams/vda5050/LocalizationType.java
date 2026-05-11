package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

public enum LocalizationType {
    DMC, GRID, NATURAL, REFLECTOR, RFID, SPOT;

    @JsonValue
    public String toValue() {
        switch (this) {
            case DMC: return "DMC";
            case GRID: return "GRID";
            case NATURAL: return "NATURAL";
            case REFLECTOR: return "REFLECTOR";
            case RFID: return "RFID";
            case SPOT: return "SPOT";
        }
        return null;
    }

    @JsonCreator
    public static LocalizationType forValue(String value) throws IOException {
        if (value.equals("DMC")) return DMC;
        if (value.equals("GRID")) return GRID;
        if (value.equals("NATURAL")) return NATURAL;
        if (value.equals("REFLECTOR")) return REFLECTOR;
        if (value.equals("RFID")) return RFID;
        if (value.equals("SPOT")) return SPOT;
        throw new IOException("Cannot deserialize LocalizationType");
    }
}
