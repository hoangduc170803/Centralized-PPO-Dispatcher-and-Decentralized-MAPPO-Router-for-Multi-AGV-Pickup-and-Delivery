package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

public enum NavigationType {
    AUTONOMOUS, PHYSICAL_LINDE_GUIDED, VIRTUAL_LINE_GUIDED;

    @JsonValue
    public String toValue() {
        switch (this) {
            case AUTONOMOUS: return "AUTONOMOUS";
            case PHYSICAL_LINDE_GUIDED: return "PHYSICAL_LINDE_GUIDED";
            case VIRTUAL_LINE_GUIDED: return "VIRTUAL_LINE_GUIDED";
        }
        return null;
    }

    @JsonCreator
    public static NavigationType forValue(String value) throws IOException {
        if (value.equals("AUTONOMOUS")) return AUTONOMOUS;
        if (value.equals("PHYSICAL_LINDE_GUIDED")) return PHYSICAL_LINDE_GUIDED;
        if (value.equals("VIRTUAL_LINE_GUIDED")) return VIRTUAL_LINE_GUIDED;
        throw new IOException("Cannot deserialize NavigationType");
    }
}
