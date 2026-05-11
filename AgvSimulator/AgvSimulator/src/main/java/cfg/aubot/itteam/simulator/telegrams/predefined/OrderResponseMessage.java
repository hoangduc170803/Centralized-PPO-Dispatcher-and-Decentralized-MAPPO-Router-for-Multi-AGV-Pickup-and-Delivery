package cfg.aubot.itteam.simulator.telegrams.predefined;


public class OrderResponseMessage extends MqttTelegram {
    private long requestId;
    private OrderState state;
    private String message;

    public OrderResponseMessage(String thingName, long requestId, OrderState state, String message) {
        super(thingName, "order-response");
        this.requestId = requestId;
        this.state = state;
        this.message = message;
    }

    public long getRequestId() {
        return requestId;
    }

    public OrderState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public enum OrderState {
        RECEIVED,
        FINISHED,
        EXECUTING,
        FAILED
    }

    @Override
    public String toString() {
        String content = String.format("Order ID: %d\nState: %s\nMessage: %s\n", requestId, state, message);
        return super.toString().concat(content);
    }
}
