package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * Connection state.
 * ONLINE: connection between AGV and broker is active.
 * OFFLINE: connection between AGV and broker has gone offline in a coordinated way.
 * CONNECTIONBROKEN: The connection between AGV and broker has unexpectedly ended.
 */
public enum ConnectionState {
    CONNECTIONBROKEN, OFFLINE, ONLINE;

    @JsonValue
    public String toValue() {
        switch (this) {
            case CONNECTIONBROKEN: return "CONNECTIONBROKEN";
            case OFFLINE: return "OFFLINE";
            case ONLINE: return "ONLINE";
        }
        return null;
    }

    @JsonCreator
    public static ConnectionState forValue(String value) throws IOException {
        if (value.equals("CONNECTIONBROKEN")) return CONNECTIONBROKEN;
        if (value.equals("OFFLINE")) return OFFLINE;
        if (value.equals("ONLINE")) return ONLINE;
        throw new IOException("Cannot deserialize ConnectionState");
    }
}
