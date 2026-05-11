package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Detailed definition of AGV geometry
 */
public class AgvGeometry {
    private Envelopes2D[] envelopes2D;
    private Envelopes3D[] envelopes3D;
    private WheelDefinition[] wheelDefinitions;

    @JsonProperty("envelopes2d")
    public Envelopes2D[] getEnvelopes2D() { return envelopes2D; }
    @JsonProperty("envelopes2d")
    public void setEnvelopes2D(Envelopes2D[] value) { this.envelopes2D = value; }

    /**
     * list of AGV-envelope curves in 3D (german: „Hüllkurven“)
     */
    @JsonProperty("envelopes3d")
    public Envelopes3D[] getEnvelopes3D() { return envelopes3D; }
    @JsonProperty("envelopes3d")
    public void setEnvelopes3D(Envelopes3D[] value) { this.envelopes3D = value; }

    /**
     * list of wheels, containing wheel-arrangement and geometry
     */
    @JsonProperty("wheelDefinitions")
    public WheelDefinition[] getWheelDefinitions() { return wheelDefinitions; }
    @JsonProperty("wheelDefinitions")
    public void setWheelDefinitions(WheelDefinition[] value) { this.wheelDefinitions = value; }
}
