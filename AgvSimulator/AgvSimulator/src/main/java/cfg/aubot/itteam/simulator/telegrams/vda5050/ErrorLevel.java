package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * Error level.
 * WARNING: AGV is ready to start (e.g. maintenance cycle expiration warning).
 * FATAL: AGV is not in running condition, user intervention required (e.g. laser scanner is
 * contaminated).
 */
public enum ErrorLevel {
    FATAL, WARNING;

    @JsonValue
    public String toValue() {
        switch (this) {
            case FATAL: return "FATAL";
            case WARNING: return "WARNING";
        }
        return null;
    }

    @JsonCreator
    public static ErrorLevel forValue(String value) throws IOException {
        if (value.equals("FATAL")) return FATAL;
        if (value.equals("WARNING")) return WARNING;
        throw new IOException("Cannot deserialize ErrorLevel");
    }
}
