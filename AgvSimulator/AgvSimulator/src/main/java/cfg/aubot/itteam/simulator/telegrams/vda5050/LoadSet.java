package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class LoadSet {
    private Double agvAccelerationLimit;
    private Double agvDecelerationLimit;
    private Double agvSpeedLimit;
    private LoadSetBoundingBoxReference boundingBoxReference;
    private Double description;
    private Double dropTime;
    private LoadSetLoadDimensions loadDimensions;
    private String[] loadPositions;
    private String loadType;
    private Double maxLoadhandlingDepth;
    private Double maxLoadhandlingHeight;
    private Double maxLoadhandlingTilt;
    private Double maxWeigth;
    private Double minLoadhandlingDepth;
    private Double minLoadhandlingHeight;
    private Double minLoadhandlingTilt;
    private Double pickTime;
    private String setName;

    /**
     * maximum allowed acceleration for this load-type and –weight
     */
    @JsonProperty("agvAccelerationLimit")
    public Double getAgvAccelerationLimit() { return agvAccelerationLimit; }
    @JsonProperty("agvAccelerationLimit")
    public void setAgvAccelerationLimit(Double value) { this.agvAccelerationLimit = value; }

    /**
     * maximum allowed deceleration for this load-type and –weight
     */
    @JsonProperty("agvDecelerationLimit")
    public Double getAgvDecelerationLimit() { return agvDecelerationLimit; }
    @JsonProperty("agvDecelerationLimit")
    public void setAgvDecelerationLimit(Double value) { this.agvDecelerationLimit = value; }

    /**
     * maximum allowed speed for this load-type and –weight
     */
    @JsonProperty("agvSpeedLimit")
    public Double getAgvSpeedLimit() { return agvSpeedLimit; }
    @JsonProperty("agvSpeedLimit")
    public void setAgvSpeedLimit(Double value) { this.agvSpeedLimit = value; }

    /**
     * bounding box reference as defined in parameter loads[] in state-message
     */
    @JsonProperty("boundingBoxReference")
    public LoadSetBoundingBoxReference getBoundingBoxReference() { return boundingBoxReference; }
    @JsonProperty("boundingBoxReference")
    public void setBoundingBoxReference(LoadSetBoundingBoxReference value) { this.boundingBoxReference = value; }

    /**
     * free text description of the load handling set
     */
    @JsonProperty("description")
    public Double getDescription() { return description; }
    @JsonProperty("description")
    public void setDescription(Double value) { this.description = value; }

    /**
     * approx. time for dropping the load
     */
    @JsonProperty("dropTime")
    public Double getDropTime() { return dropTime; }
    @JsonProperty("dropTime")
    public void setDropTime(Double value) { this.dropTime = value; }

    @JsonProperty("loadDimensions")
    public LoadSetLoadDimensions getLoadDimensions() { return loadDimensions; }
    @JsonProperty("loadDimensions")
    public void setLoadDimensions(LoadSetLoadDimensions value) { this.loadDimensions = value; }

    /**
     * list of load positions btw. load handling devices, this load-set is valid for. If this
     * parameter does not exist or is empty, this load-set is valid for all load handling
     * devices on this AGV.
     */
    @JsonProperty("loadPositions")
    public String[] getLoadPositions() { return loadPositions; }
    @JsonProperty("loadPositions")
    public void setLoadPositions(String[] value) { this.loadPositions = value; }

    /**
     * type of load e.g. EPAL, XLT1200, ….
     */
    @JsonProperty("loadType")
    public String getLoadType() { return loadType; }
    @JsonProperty("loadType")
    public void setLoadType(String value) { this.loadType = value; }

    /**
     * maximum allowed depth for this load-type and –weight. references to boundingBoxReference
     */
    @JsonProperty("maxLoadhandlingDepth")
    public Double getMaxLoadhandlingDepth() { return maxLoadhandlingDepth; }
    @JsonProperty("maxLoadhandlingDepth")
    public void setMaxLoadhandlingDepth(Double value) { this.maxLoadhandlingDepth = value; }

    /**
     * maximum allowed height for handling of this load-type and –weight. references to
     * boundingBoxReference
     */
    @JsonProperty("maxLoadhandlingHeight")
    public Double getMaxLoadhandlingHeight() { return maxLoadhandlingHeight; }
    @JsonProperty("maxLoadhandlingHeight")
    public void setMaxLoadhandlingHeight(Double value) { this.maxLoadhandlingHeight = value; }

    /**
     * maximum allowed tilt for this load-type and –weight
     */
    @JsonProperty("maxLoadhandlingTilt")
    public Double getMaxLoadhandlingTilt() { return maxLoadhandlingTilt; }
    @JsonProperty("maxLoadhandlingTilt")
    public void setMaxLoadhandlingTilt(Double value) { this.maxLoadhandlingTilt = value; }

    /**
     * maximum weight of loadtype
     */
    @JsonProperty("maxWeigth")
    public Double getMaxWeigth() { return maxWeigth; }
    @JsonProperty("maxWeigth")
    public void setMaxWeigth(Double value) { this.maxWeigth = value; }

    /**
     * minimum allowed depth for this load-type and –weight. references to boundingBoxReference
     */
    @JsonProperty("minLoadhandlingDepth")
    public Double getMinLoadhandlingDepth() { return minLoadhandlingDepth; }
    @JsonProperty("minLoadhandlingDepth")
    public void setMinLoadhandlingDepth(Double value) { this.minLoadhandlingDepth = value; }

    /**
     * minimum allowed height for handling of this load-type and –weight. References to
     * boundingBoxReference
     */
    @JsonProperty("minLoadhandlingHeight")
    public Double getMinLoadhandlingHeight() { return minLoadhandlingHeight; }
    @JsonProperty("minLoadhandlingHeight")
    public void setMinLoadhandlingHeight(Double value) { this.minLoadhandlingHeight = value; }

    /**
     * minimum allowed tilt for this load-type and –weight
     */
    @JsonProperty("minLoadhandlingTilt")
    public Double getMinLoadhandlingTilt() { return minLoadhandlingTilt; }
    @JsonProperty("minLoadhandlingTilt")
    public void setMinLoadhandlingTilt(Double value) { this.minLoadhandlingTilt = value; }

    /**
     * approx. time for picking up the load
     */
    @JsonProperty("pickTime")
    public Double getPickTime() { return pickTime; }
    @JsonProperty("pickTime")
    public void setPickTime(Double value) { this.pickTime = value; }

    /**
     * Unique name of the load set, e.g. DEFAULT, SET1, ...
     */
    @JsonProperty("setName")
    public String getSetName() { return setName; }
    @JsonProperty("setName")
    public void setSetName(String value) { this.setName = value; }
}
