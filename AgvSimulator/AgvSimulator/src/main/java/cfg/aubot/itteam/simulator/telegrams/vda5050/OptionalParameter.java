package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class OptionalParameter {
    private String description;
    private String parameter;
    private Support support;

    /**
     * free text. Description of optional parameter. E.g. Reason, why the optional parameter
     * ‚direction‘ is necessary for this AGV-type and which values it can contain. The parameter
     * ‘nodeMarker’ must contain unsigned interger-numbers only. Nurbs-Support is limited to
     * straight lines and circle segments.
     */
    @JsonProperty("description")
    public String getDescription() { return description; }
    @JsonProperty("description")
    public void setDescription(String value) { this.description = value; }

    /**
     * full name of optional parameter, e.g. “order.nodes.nodePosition.allowedDeviationTheta”
     */
    @JsonProperty("parameter")
    public String getParameter() { return parameter; }
    @JsonProperty("parameter")
    public void setParameter(String value) { this.parameter = value; }

    /**
     * type of support for the optional parameter, the following values are possible: SUPPORTED:
     * optional parameter is supported like specified. REQUIRED: optional parameter is required
     * for proper AGV-operation.
     */
    @JsonProperty("support")
    public Support getSupport() { return support; }
    @JsonProperty("support")
    public void setSupport(Support value) { this.support = value; }
}
