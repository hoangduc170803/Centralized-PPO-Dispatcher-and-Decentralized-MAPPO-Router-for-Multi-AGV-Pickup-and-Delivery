package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

/**
 * data type of Value, possible data types are: BOOL, NUMBER, INTEGER, FLOAT, STRING,
 * OBJECT, ARRAY
 */
public enum ValueDataType {
    ARRAY, BOOL, FLOAT, INTEGER, NUMBER, OBJECT, STRING;

    @JsonValue
    public String toValue() {
        switch (this) {
            case ARRAY: return "ARRAY";
            case BOOL: return "BOOL";
            case FLOAT: return "FLOAT";
            case INTEGER: return "INTEGER";
            case NUMBER: return "NUMBER";
            case OBJECT: return "OBJECT";
            case STRING: return "STRING";
        }
        return null;
    }

    @JsonCreator
    public static ValueDataType forValue(String value) throws IOException {
        if (value.equals("ARRAY")) return ARRAY;
        if (value.equals("BOOL")) return BOOL;
        if (value.equals("FLOAT")) return FLOAT;
        if (value.equals("INTEGER")) return INTEGER;
        if (value.equals("NUMBER")) return NUMBER;
        if (value.equals("OBJECT")) return OBJECT;
        if (value.equals("STRING")) return STRING;
        throw new IOException("Cannot deserialize ValueDataType");
    }
}
