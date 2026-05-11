package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class LoadSetLoadDimensions {
    private Double height;
    private double length;
    private double width;

    /**
     * Absolute height of the load´s bounding box. Optional: Set value only if known.
     */
    @JsonProperty("height")
    public Double getHeight() { return height; }
    @JsonProperty("height")
    public void setHeight(Double value) { this.height = value; }

    /**
     * Absolute length of the load´s bounding box.
     */
    @JsonProperty("length")
    public double getLength() { return length; }
    @JsonProperty("length")
    public void setLength(double value) { this.length = value; }

    /**
     * Absolute width of the load´s bounding bo
     */
    @JsonProperty("width")
    public double getWidth() { return width; }
    @JsonProperty("width")
    public void setWidth(double value) { this.width = value; }
}
