package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Trajectory JSON-object for this edge as a NURBS. Defines the curve on which the AGV
 * should move between startNode and endNode.
 * Optional: Can be omitted if AGV cannot process trajectories or if AGV plans its own
 * trajectory.
 *
 * The trajectory is to be communicated as a NURBS and is defined in chapter 6.4.
 * Trajectory segments are from the point where the AGV starts to enter the edge until the
 * point where it reports that the next node was traversed.
 */
public class Trajectory {
    private ControlPoint[] controlPoints;
    private long degree;
    private double[] knotVector;

    /**
     * List of JSON controlPoint objects defining the control points of the NURBS. This includes
     * the start and end point.
     */
    @JsonProperty("controlPoints")
    public ControlPoint[] getControlPoints() { return controlPoints; }
    @JsonProperty("controlPoints")
    public void setControlPoints(ControlPoint[] value) { this.controlPoints = value; }

    /**
     * Defines the number of control points that influence any given point on the curve.
     * Increasing the degree increases continuity.
     * If not defined, the default value is 1.
     */
    @JsonProperty("degree")
    public long getDegree() { return degree; }
    @JsonProperty("degree")
    public void setDegree(long value) { this.degree = value; }

    /**
     * Sequence of parameter values that determine where and how the control points affect the
     * NURBS curve. knotVector has size of number of control points + degree + 1
     */
    @JsonProperty("knotVector")
    public double[] getKnotVector() { return knotVector; }
    @JsonProperty("knotVector")
    public void setKnotVector(double[] value) { this.knotVector = value; }
}
