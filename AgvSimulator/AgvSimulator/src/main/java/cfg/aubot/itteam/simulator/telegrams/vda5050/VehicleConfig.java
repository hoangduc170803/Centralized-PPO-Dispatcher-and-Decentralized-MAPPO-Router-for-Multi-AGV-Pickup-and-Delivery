package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class VehicleConfig {
    private Network network;
    private Version[] versions;

    @JsonProperty("network")
    public Network getNetwork() { return network; }
    @JsonProperty("network")
    public void setNetwork(Network value) { this.network = value; }

    /**
     * Array containing various hardware and software versions running on the vehicle.
     */
    @JsonProperty("versions")
    public Version[] getVersions() { return versions; }
    @JsonProperty("versions")
    public void setVersions(Version[] value) { this.versions = value; }
}
