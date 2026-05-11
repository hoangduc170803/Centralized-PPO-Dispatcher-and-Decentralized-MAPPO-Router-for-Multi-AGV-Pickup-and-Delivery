package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;
import java.util.Map;

/**
 * This JSON-object describes the protocol limitations of the AGV. If a parameter is not
 * defined or set to zero then there is no explicit limit for this parameter.
 */
public class ProtocolLimits {
    private Map<String, Object> maxArrayLens;
    private MaxStringLens maxStringLens;
    private Timing timing;

    /**
     * maximum lengths of arrays
     */
    @JsonProperty("maxArrayLens")
    public Map<String, Object> getMaxArrayLens() { return maxArrayLens; }
    @JsonProperty("maxArrayLens")
    public void setMaxArrayLens(Map<String, Object> value) { this.maxArrayLens = value; }

    /**
     * maximum lengths of strings
     */
    @JsonProperty("maxStringLens")
    public MaxStringLens getMaxStringLens() { return maxStringLens; }
    @JsonProperty("maxStringLens")
    public void setMaxStringLens(MaxStringLens value) { this.maxStringLens = value; }

    /**
     * timing information
     */
    @JsonProperty("timing")
    public Timing getTiming() { return timing; }
    @JsonProperty("timing")
    public void setTiming(Timing value) { this.timing = value; }
}
