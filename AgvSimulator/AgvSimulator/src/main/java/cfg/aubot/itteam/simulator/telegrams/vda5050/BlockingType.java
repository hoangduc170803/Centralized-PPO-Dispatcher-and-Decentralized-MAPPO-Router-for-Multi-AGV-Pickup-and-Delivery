package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * Regulates if the action is allowed to be executed during movement and/or parallel to
 * other actions.
 * NONE: action can happen in parallel with others, including movement.
 * SOFT: action can happen simultaneously with others, but not while moving.
 * HARD: no other actions can be performed while this action is running.
 */
public enum BlockingType {
    HARD, NONE, SOFT;

    @JsonValue
    public String toValue() {
        switch (this) {
            case HARD: return "HARD";
            case NONE: return "NONE";
            case SOFT: return "SOFT";
        }
        return null;
    }

    @JsonCreator
    public static BlockingType forValue(String value) throws IOException {
        if (value.equals("HARD")) return HARD;
        if (value.equals("NONE")) return NONE;
        if (value.equals("SOFT")) return SOFT;
        throw new IOException("Cannot deserialize BlockingType");
    }
}
