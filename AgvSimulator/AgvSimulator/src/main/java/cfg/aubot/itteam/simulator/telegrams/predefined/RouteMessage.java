package cfg.aubot.itteam.simulator.telegrams.predefined;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RouteMessage extends RequestMessage {

    @SerializedName("map")
    private List<RouteStep> route;

    public RouteMessage(String thingName, long requestId, List<RouteStep> route) {
        super(thingName, "route", requestId);
        this.route = route;
    }

    public List<RouteStep> getRoute() {
        return route;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Route");
        for (RouteStep step : route) {
            builder.append(" >> ").append(step.getDestination());
        }
        return builder.toString();
    }

    public static class RouteStep {
        private String destination;
        private char direction;
        private String action;

        public RouteStep(String destination, char direction, String action) {
            this.destination = destination;
            this.direction = direction;
            this.action = action;
        }

        public String getDestination() {
            return destination;
        }

        public char getDirection() {
            return direction;
        }

        public String getAction() {
            return action;
        }
    }
}
