package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Object that holds the info reference (e.g. orderId, orderUpdateId, actionId...) as
 * key-value pairs.
 */
public class InfoReference {
    private String referenceKey;
    private String referenceValue;

    /**
     * References the type of reference (e. g. headerId, orderId, actionId, ...).
     */
    @JsonProperty("referenceKey")
    public String getReferenceKey() { return referenceKey; }
    @JsonProperty("referenceKey")
    public void setReferenceKey(String value) { this.referenceKey = value; }

    /**
     * References the value, which belongs to the reference key.
     */
    @JsonProperty("referenceValue")
    public String getReferenceValue() { return referenceValue; }
    @JsonProperty("referenceValue")
    public void setReferenceValue(String value) { this.referenceValue = value; }
}
