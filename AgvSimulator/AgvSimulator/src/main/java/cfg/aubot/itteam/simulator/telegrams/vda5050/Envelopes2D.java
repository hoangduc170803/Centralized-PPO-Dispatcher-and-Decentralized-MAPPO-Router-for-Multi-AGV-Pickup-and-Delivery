package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class Envelopes2D {
    private String description;
    private PolygonPoint[] polygonPoints;
    private String set;

    /**
     * free text: description of envelope curve set
     */
    @JsonProperty("description")
    public String getDescription() { return description; }
    @JsonProperty("description")
    public void setDescription(String value) { this.description = value; }

    /**
     * envelope curve as a x/y-polygon polygon is assumed as closed and must be
     * non-self-intersecting
     */
    @JsonProperty("polygonPoints")
    public PolygonPoint[] getPolygonPoints() { return polygonPoints; }
    @JsonProperty("polygonPoints")
    public void setPolygonPoints(PolygonPoint[] value) { this.polygonPoints = value; }

    /**
     * name of the envelope curve set
     */
    @JsonProperty("set")
    public String getSet() { return set; }
    @JsonProperty("set")
    public void setSet(String value) { this.set = value; }
}
