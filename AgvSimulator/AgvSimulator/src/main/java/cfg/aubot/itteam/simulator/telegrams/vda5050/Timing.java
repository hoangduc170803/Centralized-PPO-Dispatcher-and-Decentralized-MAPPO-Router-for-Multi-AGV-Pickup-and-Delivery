package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * timing information
 */
public class Timing {
    private Double defaultStateInterval;
    private double minOrderInterval;
    private double minStateInterval;
    private Double visualizationInterval;

    /**
     * default interval for sending state-messages if not defined, the default value from the
     * main document is used
     */
    @JsonProperty("defaultStateInterval")
    public Double getDefaultStateInterval() { return defaultStateInterval; }
    @JsonProperty("defaultStateInterval")
    public void setDefaultStateInterval(Double value) { this.defaultStateInterval = value; }

    /**
     * minimum interval sending order messages to the AGV
     */
    @JsonProperty("minOrderInterval")
    public double getMinOrderInterval() { return minOrderInterval; }
    @JsonProperty("minOrderInterval")
    public void setMinOrderInterval(double value) { this.minOrderInterval = value; }

    /**
     * minimum interval for sending state-messages
     */
    @JsonProperty("minStateInterval")
    public double getMinStateInterval() { return minStateInterval; }
    @JsonProperty("minStateInterval")
    public void setMinStateInterval(double value) { this.minStateInterval = value; }

    /**
     * default interval for sending messages on visualization topic
     */
    @JsonProperty("visualizationInterval")
    public Double getVisualizationInterval() { return visualizationInterval; }
    @JsonProperty("visualizationInterval")
    public void setVisualizationInterval(Double value) { this.visualizationInterval = value; }
}
