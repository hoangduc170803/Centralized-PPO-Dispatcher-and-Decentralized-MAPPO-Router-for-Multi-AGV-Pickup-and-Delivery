package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Current position of the AGV on the map.
 * Optional: Can only be omitted for AGVs without the capability to localize themselves,
 * e.g. line guided AGVs.
 */
@JsonPropertyOrder({
    "positionInitialized",
    "x",
    "y",
    "theta",
    "mapId",
    "mapDescription",
    "localizationScore",
    "deviationRange"
})
public class AgvPosition {
    private Double deviationRange;
    private Double localizationScore;
    private String mapDescription;
    private String mapID;
    private boolean positionInitialized;
    private double theta;
    private double x;
    private double y;

    /**
     * Value for the deviation range of the position in meters.
     * Optional for vehicles that cannot estimate their deviation e.g. grid-based localization.
     * Only for logging and visualization purposes.
     */
    @JsonProperty("deviationRange")
    public Double getDeviationRange() { return deviationRange; }
    @JsonProperty("deviationRange")
    public void setDeviationRange(Double value) { this.deviationRange = value; }

    /**
     * Describes the quality of the localization and therefore, can be used e.g. by SLAM-AGVs to
     * describe how accurate the current position information is.
     * 0.0: position unknown
     * 1.0: position known
     * Optional for vehicles that cannot estimate their localization score.
     * Only for logging and visualization purposes
     */
    @JsonProperty("localizationScore")
    public Double getLocalizationScore() { return localizationScore; }
    @JsonProperty("localizationScore")
    public void setLocalizationScore(Double value) { this.localizationScore = value; }

    /**
     * Additional information on the map.
     */
    @JsonProperty("mapDescription")
    public String getMapDescription() { return mapDescription; }
    @JsonProperty("mapDescription")
    public void setMapDescription(String value) { this.mapDescription = value; }

    /**
     * Unique identification of the map in which the position is referenced.
     * Each map has the same origin of coordinates. When an AGV uses an elevator, e.g. leading
     * from a departure floor to a target floor, it will disappear off the map of the departure
     * floor and spawn in the related lift node on the map of the target floor.
     */
    @JsonProperty("mapId")
    public String getMapID() { return mapID; }
    @JsonProperty("mapId")
    public void setMapID(String value) { this.mapID = value; }

    /**
     * True if the AGVs position is initialized, false, if position is not initialized.
     */
    @JsonProperty("positionInitialized")
    public boolean getPositionInitialized() { return positionInitialized; }
    @JsonProperty("positionInitialized")
    public void setPositionInitialized(boolean value) { this.positionInitialized = value; }

    /**
     * Range: [-pi ... pi]
     * Orientation of the AGV.
     */
    @JsonProperty("theta")
    public double getTheta() { return theta; }
    @JsonProperty("theta")
    public void setTheta(double value) { this.theta = value; }

    /**
     * X-position on the map in reference to the map coordinate system. Precision is up to the
     * specific implementation.
     */
    @JsonProperty("x")
    public double getX() { return x; }
    @JsonProperty("x")
    public void setX(double value) { this.x = value; }

    /**
     * Y-position on the map in reference to the map coordinate system. Precision is up to the
     * specific implementation.
     */
    @JsonProperty("y")
    public double getY() { return y; }
    @JsonProperty("y")
    public void setY(double value) { this.y = value; }
}
