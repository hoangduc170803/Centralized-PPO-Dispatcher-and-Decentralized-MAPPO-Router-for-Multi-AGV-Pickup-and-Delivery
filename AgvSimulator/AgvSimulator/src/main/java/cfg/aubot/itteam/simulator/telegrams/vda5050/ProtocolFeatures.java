package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Supported features of VDA5050 protocol
 */
public class ProtocolFeatures {
    private AgvAction[] agvActions;
    private OptionalParameter[] optionalParameters;

    /**
     * list of all actions with parameters supported by this AGV. This includes standard actions
     * specified in VDA5050 and manufacturer-specific actions
     */
    @JsonProperty("agvActions")
    public AgvAction[] getAgvActions() { return agvActions; }
    @JsonProperty("agvActions")
    public void setAgvActions(AgvAction[] value) { this.agvActions = value; }

    /**
     * list of supported and/or required optional parameters. Optional parameters, that are not
     * listed here, are assumed to be not supported by the AGV.
     */
    @JsonProperty("optionalParameters")
    public OptionalParameter[] getOptionalParameters() { return optionalParameters; }
    @JsonProperty("optionalParameters")
    public void setOptionalParameters(OptionalParameter[] value) { this.optionalParameters = value; }
}
