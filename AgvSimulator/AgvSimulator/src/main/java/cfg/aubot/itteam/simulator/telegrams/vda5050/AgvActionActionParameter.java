package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class AgvActionActionParameter {
    private String description;
    private Boolean isOptional;
    private String key;
    private ValueDataType valueDataType;

    /**
     * free text: description of the parameter
     */
    @JsonProperty("description")
    public String getDescription() { return description; }
    @JsonProperty("description")
    public void setDescription(String value) { this.description = value; }

    /**
     * True: optional parameter
     */
    @JsonProperty("isOptional")
    public Boolean getIsOptional() { return isOptional; }
    @JsonProperty("isOptional")
    public void setIsOptional(Boolean value) { this.isOptional = value; }

    /**
     * key-String for Parameter
     */
    @JsonProperty("key")
    public String getKey() { return key; }
    @JsonProperty("key")
    public void setKey(String value) { this.key = value; }

    /**
     * data type of Value, possible data types are: BOOL, NUMBER, INTEGER, FLOAT, STRING,
     * OBJECT, ARRAY
     */
    @JsonProperty("valueDataType")
    public ValueDataType getValueDataType() { return valueDataType; }
    @JsonProperty("valueDataType")
    public void setValueDataType(ValueDataType value) { this.valueDataType = value; }
}
