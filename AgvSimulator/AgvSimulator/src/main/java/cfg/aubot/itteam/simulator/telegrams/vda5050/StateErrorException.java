package cfg.aubot.itteam.simulator.telegrams.vda5050;

public class StateErrorException extends Exception {
    private final Error error;

    public StateErrorException(Error error) {
        this.error = error;
    }

    public Error getError() {
        return error;
    }

    @Override
    public String getMessage() {
        return "Order rejected: " + error.getErrorDescription();
    }
}
