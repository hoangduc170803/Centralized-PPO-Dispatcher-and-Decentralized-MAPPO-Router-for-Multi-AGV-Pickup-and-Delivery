package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * type of support for the optional parameter, the following values are possible: SUPPORTED:
 * optional parameter is supported like specified. REQUIRED: optional parameter is required
 * for proper AGV-operation.
 */
public enum Support {
    REQUIRED, SUPPORTED;

    @JsonValue
    public String toValue() {
        switch (this) {
            case REQUIRED: return "REQUIRED";
            case SUPPORTED: return "SUPPORTED";
        }
        return null;
    }

    @JsonCreator
    public static Support forValue(String value) throws IOException {
        if (value.equals("REQUIRED")) return REQUIRED;
        if (value.equals("SUPPORTED")) return SUPPORTED;
        throw new IOException("Cannot deserialize Support");
    }
}
