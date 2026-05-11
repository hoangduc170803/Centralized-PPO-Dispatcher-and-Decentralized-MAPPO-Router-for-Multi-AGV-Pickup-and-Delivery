package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * ActionParameter Object
 */
public class ActionActionParameter {
    private String key;
    private Value value;

    /**
     * The key of the action parameter. For example. duration, direction, signal.
     */
    @JsonProperty("key")
    public String getKey() { return key; }
    @JsonProperty("key")
    public void setKey(String value) { this.key = value; }

    /**
     * The value of the action parameter. For example: 103.2, "left", true, [ 1, 2, 3].
     */
    @JsonProperty("value")
    public Value getValue() { return value; }
    @JsonProperty("value")
    public void setValue(Value value) { this.value = value; }
}
