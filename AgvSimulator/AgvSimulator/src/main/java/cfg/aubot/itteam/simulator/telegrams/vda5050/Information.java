package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * An information object.
 */
public class Information {
    private String infoDescription;
    private InfoLevel infoLevel;
    private InfoReference[] infoReferences;
    private String infoType;

    /**
     * Info description.
     */
    @JsonProperty("infoDescription")
    public String getInfoDescription() { return infoDescription; }
    @JsonProperty("infoDescription")
    public void setInfoDescription(String value) { this.infoDescription = value; }

    /**
     * Info level.
     * DEBUG: used for debugging.
     * INFO: used for visualization.
     */
    @JsonProperty("infoLevel")
    public InfoLevel getInfoLevel() { return infoLevel; }
    @JsonProperty("infoLevel")
    public void setInfoLevel(InfoLevel value) { this.infoLevel = value; }

    /**
     * Array of references.
     */
    @JsonProperty("infoReferences")
    public InfoReference[] getInfoReferences() { return infoReferences; }
    @JsonProperty("infoReferences")
    public void setInfoReferences(InfoReference[] value) { this.infoReferences = value; }

    /**
     * Type / name of information.
     */
    @JsonProperty("infoType")
    public String getInfoType() { return infoType; }
    @JsonProperty("infoType")
    public void setInfoType(String value) { this.infoType = value; }
}
