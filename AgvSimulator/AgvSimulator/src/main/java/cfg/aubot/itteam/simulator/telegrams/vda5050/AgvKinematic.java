package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * simplified description of AGV kinematics-type.
 */
public enum AgvKinematic {
    DIFF, OMNI, THREEWHEEL;

    @JsonValue
    public String toValue() {
        switch (this) {
            case DIFF: return "DIFF";
            case OMNI: return "OMNI";
            case THREEWHEEL: return "THREEWHEEL";
        }
        return null;
    }

    @JsonCreator
    public static AgvKinematic forValue(String value) throws IOException {
        if (value.equals("DIFF")) return DIFF;
        if (value.equals("OMNI")) return OMNI;
        if (value.equals("THREEWHEEL")) return THREEWHEEL;
        throw new IOException("Cannot deserialize AgvKinematic");
    }
}
