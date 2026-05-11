package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Defines the position on a map in world coordinates. Each floor has its own map. Precision
 * is up to the specific implementation.
 *
 * Node position. The object is defined in chapter 6.6. Optional: master control has this
 * information. Can be sent additionally, e.g. for debugging purposes.
 */
public class NodePosition {
    private String mapID;
    private Double theta;
    private double x;
    private double y;

    /**
     * Unique identification of the map in which the position is referenced.
     * Each map has the same origin of coordinates. When an AGV uses an elevator, e. g. leading
     * from a departure floor to a target floor, it will disappear off the map of the departure
     * floor and spawn in the related lift node on the map of the target floor.
     */
    @JsonProperty("mapId")
    public String getMapID() { return mapID; }
    @JsonProperty("mapId")
    public void setMapID(String value) { this.mapID = value; }

    /**
     * Range: [-pi .. pi].
     * Orientation of the AGV on the node.
     * Optional: vehicle can plan the path by itself.
     * If defined, the AGV has to assume the theta angle on this node.
     * If previous edge disallows rotation, the AGV is to rotate on the node.
     * If following edge has a differing orientation defined but disallows rotation, the AGV is
     * to rotate on the node to the edges desired rotation before entering the edge.
     */
    @JsonProperty("theta")
    public Double getTheta() { return theta; }
    @JsonProperty("theta")
    public void setTheta(Double value) { this.theta = value; }

    /**
     * X coordinate described in the world coordinate system.
     */
    @JsonProperty("x")
    public double getX() { return x; }
    @JsonProperty("x")
    public void setX(double value) { this.x = value; }

    /**
     * Y coordinate described in the world coordinate system.
     */
    @JsonProperty("y")
    public double getY() { return y; }
    @JsonProperty("y")
    public void setY(double value) { this.y = value; }
}
