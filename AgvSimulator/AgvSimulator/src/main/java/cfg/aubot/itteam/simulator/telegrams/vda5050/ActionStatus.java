package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * Action status.
 * WAITING: Action was received by AGV but the node where it triggers was not yet reached or
 * the edge where it is active was not yet entered.
 * INITIALIZING: Action was triggered, preparatory measures are initiated.
 * RUNNING: The action is running.
 * PAUSED: The action is paused because of a pause instantAction or external trigger (pause
 * button on AGV).
 * FINISHED: The action is finished. A result is reported via the resultDescription.
 * FAILED: Action could not be finished for whatever reason.
 */
public enum ActionStatus {
    FAILED, FINISHED, INITIALIZING, PAUSED, RUNNING, WAITING;

    @JsonValue
    public String toValue() {
        switch (this) {
            case FAILED: return "FAILED";
            case FINISHED: return "FINISHED";
            case INITIALIZING: return "INITIALIZING";
            case PAUSED: return "PAUSED";
            case RUNNING: return "RUNNING";
            case WAITING: return "WAITING";
        }
        return null;
    }

    @JsonCreator
    public static ActionStatus forValue(String value) throws IOException {
        if (value.equals("FAILED")) return FAILED;
        if (value.equals("FINISHED")) return FINISHED;
        if (value.equals("INITIALIZING")) return INITIALIZING;
        if (value.equals("PAUSED")) return PAUSED;
        if (value.equals("RUNNING")) return RUNNING;
        if (value.equals("WAITING")) return WAITING;
        throw new IOException("Cannot deserialize ActionStatus");
    }
}
