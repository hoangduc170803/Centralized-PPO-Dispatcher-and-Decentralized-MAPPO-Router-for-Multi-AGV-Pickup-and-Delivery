package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class Network {
    private String defaultGateway;
    private String[] dnsServers;
    private String localIPAddress;
    private String netmask;
    private String[] ntpServers;

    /**
     * Default gateway used by the vehicle.
     */
    @JsonProperty("defaultGateway")
    public String getDefaultGateway() { return defaultGateway; }
    @JsonProperty("defaultGateway")
    public void setDefaultGateway(String value) { this.defaultGateway = value; }

    /**
     * List of DNS servers used by the vehicle.
     */
    @JsonProperty("dnsServers")
    public String[] getDNSServers() { return dnsServers; }
    @JsonProperty("dnsServers")
    public void setDNSServers(String[] value) { this.dnsServers = value; }

    /**
     * A priori assigned IP address of the vehicle used to communicate with the MQTT broker.
     * Note that this IP address should not be modified/changed during operations.
     */
    @JsonProperty("localIpAddress")
    public String getLocalIPAddress() { return localIPAddress; }
    @JsonProperty("localIpAddress")
    public void setLocalIPAddress(String value) { this.localIPAddress = value; }

    /**
     * Network subnet mask.
     */
    @JsonProperty("netmask")
    public String getNetmask() { return netmask; }
    @JsonProperty("netmask")
    public void setNetmask(String value) { this.netmask = value; }

    /**
     * List of NTP servers used by the vehicle.
     */
    @JsonProperty("ntpServers")
    public String[] getNTPServers() { return ntpServers; }
    @JsonProperty("ntpServers")
    public void setNTPServers(String[] value) { this.ntpServers = value; }
}
