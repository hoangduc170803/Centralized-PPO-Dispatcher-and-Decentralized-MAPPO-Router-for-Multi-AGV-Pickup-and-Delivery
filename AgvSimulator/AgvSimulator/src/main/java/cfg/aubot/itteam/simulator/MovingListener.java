package cfg.aubot.itteam.simulator;

import java.util.Map;

public interface MovingListener {
    void onDistanceChange(float distance);

    void onError(Map<String, String> errors);

    void onError(int errors);
}
