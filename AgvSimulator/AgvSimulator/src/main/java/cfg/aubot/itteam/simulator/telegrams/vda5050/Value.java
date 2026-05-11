package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import java.io.IOException;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;

/**
 * The value of the action parameter. For example: 103.2, "left", true, [ 1, 2, 3].
 */
@JsonDeserialize(using = Value.Deserializer.class)
@JsonSerialize(using = Value.Serializer.class)
public class Value {
    public Double doubleValue;
    public Boolean boolValue;
    public String stringValue;
    public Object[] anythingArrayValue;

    static class Deserializer extends JsonDeserializer<Value> {
        @Override
        public Value deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            Value value = new Value();
            switch (jsonParser.currentToken()) {
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                    value.doubleValue = jsonParser.readValueAs(Double.class);
                    break;
                case VALUE_TRUE:
                case VALUE_FALSE:
                    value.boolValue = jsonParser.readValueAs(Boolean.class);
                    break;
                case VALUE_STRING:
                    String string = jsonParser.readValueAs(String.class);
                    value.stringValue = string;
                    break;
                case START_ARRAY:
                    value.anythingArrayValue = jsonParser.readValueAs(Object[].class);
                    break;
                default: throw new IOException("Cannot deserialize Value");
            }
            return value;
        }
    }

    static class Serializer extends JsonSerializer<Value> {
        @Override
        public void serialize(Value obj, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            if (obj.doubleValue != null) {
                jsonGenerator.writeObject(obj.doubleValue);
                return;
            }
            if (obj.boolValue != null) {
                jsonGenerator.writeObject(obj.boolValue);
                return;
            }
            if (obj.stringValue != null) {
                jsonGenerator.writeObject(obj.stringValue);
                return;
            }
            if (obj.anythingArrayValue != null) {
                jsonGenerator.writeObject(obj.anythingArrayValue);
                return;
            }
            throw new IOException("Value must not be null");
        }
    }
}
