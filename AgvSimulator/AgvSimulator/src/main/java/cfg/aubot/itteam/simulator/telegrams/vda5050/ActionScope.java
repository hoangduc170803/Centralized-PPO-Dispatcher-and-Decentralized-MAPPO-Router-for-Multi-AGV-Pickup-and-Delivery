package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

public enum ActionScope {
    EDGE, INSTANT, NODE;

    @JsonValue
    public String toValue() {
        switch (this) {
            case EDGE: return "EDGE";
            case INSTANT: return "INSTANT";
            case NODE: return "NODE";
        }
        return null;
    }

    @JsonCreator
    public static ActionScope forValue(String value) throws IOException {
        if (value.equals("EDGE")) return EDGE;
        if (value.equals("INSTANT")) return INSTANT;
        if (value.equals("NODE")) return NODE;
        throw new IOException("Cannot deserialize ActionScope");
    }
}
