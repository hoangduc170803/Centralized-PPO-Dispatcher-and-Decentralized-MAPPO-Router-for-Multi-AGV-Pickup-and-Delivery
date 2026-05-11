package cfg.aubot.itteam.simulator.telegrams.predefined;

import java.util.Locale;

public class MoveRequest extends MqttTelegram {

    public MoveRequest(String thingName, MoveState moveState) {
        super(thingName, moveState.name().toLowerCase(Locale.ROOT));
    }
}
