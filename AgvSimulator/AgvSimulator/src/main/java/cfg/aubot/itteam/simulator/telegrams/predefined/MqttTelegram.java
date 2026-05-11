package cfg.aubot.itteam.simulator.telegrams.predefined;

import java.io.Serializable;

public abstract class MqttTelegram implements Serializable {

    protected String thingName;
    protected String type;

    public MqttTelegram(String thingName, String type) {
        this.thingName = thingName;
        this.type = type;
    }

    public String getThingName() {
        return thingName;
    }

    public void setThingName(String thingName) {
        this.thingName = thingName;
    }

    @Override
    public String toString() {
        return String.format("Thing name: %s\nType: %s\n", thingName, type);
    }
}
