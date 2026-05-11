package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Load object that describes the load if the AGV has information about it.
 */
public class Load {
    private LoadBoundingBoxReference boundingBoxReference;
    private LoadLoadDimensions loadDimensions;
    private String loadID;
    private String loadPosition;
    private String loadType;
    private Double weight;

    /**
     * This point describes the loads position on the AGV in the vehicle coordinates. The
     * boundingBoxReference point is in the middle of the footprint of the load, so length/2 and
     * width/2.
     */
    @JsonProperty("boundingBoxReference")
    public LoadBoundingBoxReference getBoundingBoxReference() { return boundingBoxReference; }
    @JsonProperty("boundingBoxReference")
    public void setBoundingBoxReference(LoadBoundingBoxReference value) { this.boundingBoxReference = value; }

    /**
     * Dimensions of the load's bounding box in meters.
     */
    @JsonProperty("loadDimensions")
    public LoadLoadDimensions getLoadDimensions() { return loadDimensions; }
    @JsonProperty("loadDimensions")
    public void setLoadDimensions(LoadLoadDimensions value) { this.loadDimensions = value; }

    /**
     * Unique identification number of the load (e. g. barcode or RFID)
     * Empty field if the AGV can identify the load but didn't identify the load yet.
     * Optional if the AGV has cannot identify the load.
     */
    @JsonProperty("loadId")
    public String getLoadID() { return loadID; }
    @JsonProperty("loadId")
    public void setLoadID(String value) { this.loadID = value; }

    /**
     * Indicates which load handling/carrying unit of the AGV is used, e. g. in case the AGV has
     * multiple spots/positions to carry loads.
     * For example: front, back, positionC1, etc.
     * Optional for vehicles with only one loadPosition.
     */
    @JsonProperty("loadPosition")
    public String getLoadPosition() { return loadPosition; }
    @JsonProperty("loadPosition")
    public void setLoadPosition(String value) { this.loadPosition = value; }

    /**
     * Type of load.
     */
    @JsonProperty("loadType")
    public String getLoadType() { return loadType; }
    @JsonProperty("loadType")
    public void setLoadType(String value) { this.loadType = value; }

    /**
     * Weight of load in kg
     */
    @JsonProperty("weight")
    public Double getWeight() { return weight; }
    @JsonProperty("weight")
    public void setWeight(Double value) { this.weight = value; }
}
