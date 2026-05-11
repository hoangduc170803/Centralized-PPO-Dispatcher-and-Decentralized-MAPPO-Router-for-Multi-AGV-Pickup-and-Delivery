package cfg.aubot.itteam.simulator.telegrams.predefined;

public class OrderRequestMessage extends RequestMessage {

    private String destination;
    private char direction;
    private String action;

    public OrderRequestMessage(String thingName, long requestId, String destination, char direction, String action) {
        super(thingName, "order-request", requestId);
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

    @Override
    public String toString() {
        String content = String.format("%d %s %s %s", requestId, destination, direction, action);
        return super.toString().concat(content);
    }
}
