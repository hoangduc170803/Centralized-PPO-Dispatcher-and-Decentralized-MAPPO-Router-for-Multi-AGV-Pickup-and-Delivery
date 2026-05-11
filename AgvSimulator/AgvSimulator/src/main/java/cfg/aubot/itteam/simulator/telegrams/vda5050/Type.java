package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * wheel type. DRIVE, CASTER, FIXED, MECANUM
 */
public enum Type {
    CASTER, DRIVE, FIXED, MECANUM;

    @JsonValue
    public String toValue() {
        switch (this) {
            case CASTER: return "CASTER";
            case DRIVE: return "DRIVE";
            case FIXED: return "FIXED";
            case MECANUM: return "MECANUM";
        }
        return null;
    }

    @JsonCreator
    public static Type forValue(String value) throws IOException {
        if (value.equals("CASTER")) return CASTER;
        if (value.equals("DRIVE")) return DRIVE;
        if (value.equals("FIXED")) return FIXED;
        if (value.equals("MECANUM")) return MECANUM;
        throw new IOException("Cannot deserialize Type");
    }
}
