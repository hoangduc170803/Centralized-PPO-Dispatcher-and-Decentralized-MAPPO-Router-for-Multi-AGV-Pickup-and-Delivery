package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Abstract specification of load capabilities
 */
public class LoadSpecification {
    private String[] loadPositions;
    private LoadSet[] loadSets;

    /**
     * list of load positions / load handling devices. This lists contains the valid values for
     * the oarameter “state.loads[].loadPosition” and for the action parameter “lhd” of the
     * actions pick and drop. If this list doesn’t exist or is empty, the AGV has no load
     * handling device.
     */
    @JsonProperty("loadPositions")
    public String[] getLoadPositions() { return loadPositions; }
    @JsonProperty("loadPositions")
    public void setLoadPositions(String[] value) { this.loadPositions = value; }

    /**
     * list of load-sets that can be handled by the AGV
     */
    @JsonProperty("loadSets")
    public LoadSet[] getLoadSets() { return loadSets; }
    @JsonProperty("loadSets")
    public void setLoadSets(LoadSet[] value) { this.loadSets = value; }
}
