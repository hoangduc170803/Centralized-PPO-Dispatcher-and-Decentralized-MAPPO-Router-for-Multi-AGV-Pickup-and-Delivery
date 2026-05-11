package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class Version {
    private String key;
    private String value;

    /**
     * The key of the version.
     */
    @JsonProperty("key")
    public String getKey() { return key; }
    @JsonProperty("key")
    public void setKey(String value) { this.key = value; }

    /**
     * The value of the action parameter.
     */
    @JsonProperty("value")
    public String getValue() { return value; }
    @JsonProperty("value")
    public void setValue(String value) { this.value = value; }
}
