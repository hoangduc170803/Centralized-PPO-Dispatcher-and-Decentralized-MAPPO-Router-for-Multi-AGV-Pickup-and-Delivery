package cfg.aubot.itteam.simulator.telegrams.predefined;

public class AgvStateMessage extends MqttTelegram {
    private String position;
    private char operation;
    private char load;
    private float voltage;
    private float current;
    private int energyLevel;
    private String nextPosition;
    private float distance;

    public AgvStateMessage(String thingName,
                           String position, char operation, char load,
                           float voltage, float current, int energyLevel,
                           String nextPosition, float distance) {
        super(thingName, "state");
        this.position = position;
        this.operation = operation;
        this.load = load;
        this.voltage = voltage;
        this.current = current;
        this.energyLevel = energyLevel;
        this.nextPosition = nextPosition;
        this.distance = distance;
    }

    public String getPosition() {
        return position;
    }

    public char getOperation() {
        return operation;
    }

    public char getLoad() {
        return load;
    }

    public float getVoltage() {
        return voltage;
    }

    public float getCurrent() {
        return current;
    }

    public int getEnergyLevel() {
        return energyLevel;
    }

    public String getNextPosition() {
        return nextPosition;
    }

    public double getDistance() {
        return (double) distance * 1000;
    }

    @Override
    public String toString() {
        String content = String.format("Position: %s\nOperation: %s\nLoad state: %s\n" +
                "Voltage: %3.2f\nCurrent: %3.2f\nEnergy level: %s\nNext position: %s\n" +
                "Distance: %3.2f\n",
                position, operation, load, voltage, current, energyLevel, nextPosition, distance);
        return super.toString().concat(content);
    }
}
