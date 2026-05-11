package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * These parameters specify the basic physical properties of the AGV
 */
public class PhysicalParameters {
    private double accelerationMax;
    private double decelerationMax;
    private double heightMax;
    private Double heightMin;
    private double length;
    private double speedMax;
    private double speedMin;
    private double width;

    /**
     * maximum acceleration with maximum load
     */
    @JsonProperty("accelerationMax")
    public double getAccelerationMax() { return accelerationMax; }
    @JsonProperty("accelerationMax")
    public void setAccelerationMax(double value) { this.accelerationMax = value; }

    /**
     * maximum deceleration with maximum load
     */
    @JsonProperty("decelerationMax")
    public double getDecelerationMax() { return decelerationMax; }
    @JsonProperty("decelerationMax")
    public void setDecelerationMax(double value) { this.decelerationMax = value; }

    /**
     * maximum height of AGV
     */
    @JsonProperty("heightMax")
    public double getHeightMax() { return heightMax; }
    @JsonProperty("heightMax")
    public void setHeightMax(double value) { this.heightMax = value; }

    /**
     * minimum height of AGV
     */
    @JsonProperty("heightMin")
    public Double getHeightMin() { return heightMin; }
    @JsonProperty("heightMin")
    public void setHeightMin(Double value) { this.heightMin = value; }

    /**
     * length of AGV
     */
    @JsonProperty("length")
    public double getLength() { return length; }
    @JsonProperty("length")
    public void setLength(double value) { this.length = value; }

    /**
     * maximum speed of the AGV
     */
    @JsonProperty("speedMax")
    public double getSpeedMax() { return speedMax; }
    @JsonProperty("speedMax")
    public void setSpeedMax(double value) { this.speedMax = value; }

    /**
     * minimal controlled continuous speed of the AGV
     */
    @JsonProperty("speedMin")
    public double getSpeedMin() { return speedMin; }
    @JsonProperty("speedMin")
    public void setSpeedMin(double value) { this.speedMin = value; }

    /**
     * width of AGV
     */
    @JsonProperty("width")
    public double getWidth() { return width; }
    @JsonProperty("width")
    public void setWidth(double value) { this.width = value; }
}
