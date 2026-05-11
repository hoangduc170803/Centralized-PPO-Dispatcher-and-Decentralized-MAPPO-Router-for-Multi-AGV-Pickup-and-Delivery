// To use this code, add the following Maven dependency to your project:
//
//
//     com.fasterxml.jackson.core     : jackson-databind          : 2.9.0
//     com.fasterxml.jackson.datatype : jackson-datatype-jsr310   : 2.9.0
//
// Import this package:
//
//     import io.quicktype.Converter;
//
// Then you can deserialize a JSON string with
//
//     Object data = Converter.CommonFromJsonString(jsonString);
//     Connection data = Converter.ConnectionFromJsonString(jsonString);
//     Factsheet data = Converter.FactsheetFromJsonString(jsonString);
//     Header data = Converter.HeaderFromJsonString(jsonString);
//     InstantActions data = Converter.InstantActionsFromJsonString(jsonString);
//     Order data = Converter.OrderFromJsonString(jsonString);
//     State data = Converter.StateFromJsonString(jsonString);
//     Visualization data = Converter.VisualizationFromJsonString(jsonString);

package cfg.aubot.itteam.simulator.telegrams.vda5050;

import java.io.IOException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class Converter {
    // Date-time helpers

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ISO_DATE_TIME)
            .appendOptional(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .appendOptional(DateTimeFormatter.ISO_INSTANT)
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SX"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    public static OffsetDateTime parseDateTimeString(String str) {
        return ZonedDateTime.from(Converter.DATE_TIME_FORMATTER.parse(str)).toOffsetDateTime();
    }

    private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ISO_TIME)
            .appendOptional(DateTimeFormatter.ISO_OFFSET_TIME)
            .parseDefaulting(ChronoField.YEAR, 2020)
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    public static OffsetTime parseTimeString(String str) {
        return ZonedDateTime.from(Converter.TIME_FORMATTER.parse(str)).toOffsetDateTime().toOffsetTime();
    }
    // Serialize/deserialize helpers

    public static Object CommonFromJsonString(String json) throws IOException {
        return getCommonObjectReader().readValue(json);
    }

    public static String CommonToJsonString(Object obj) throws JsonProcessingException {
        return getCommonObjectWriter().writeValueAsString(obj);
    }

    public static Connection ConnectionFromJsonString(String json) throws IOException {
        return getConnectionObjectReader().readValue(json);
    }

    public static String ConnectionToJsonString(Connection obj) throws JsonProcessingException {
        return getConnectionObjectWriter().writeValueAsString(obj);
    }

    public static Factsheet FactsheetFromJsonString(String json) throws IOException {
        return getFactsheetObjectReader().readValue(json);
    }

    public static String FactsheetToJsonString(Factsheet obj) throws JsonProcessingException {
        return getFactsheetObjectWriter().writeValueAsString(obj);
    }

    public static Header HeaderFromJsonString(String json) throws IOException {
        return getHeaderObjectReader().readValue(json);
    }

    public static String HeaderToJsonString(Header obj) throws JsonProcessingException {
        return getHeaderObjectWriter().writeValueAsString(obj);
    }

    public static InstantActions InstantActionsFromJsonString(String json) throws IOException {
        return getInstantActionsObjectReader().readValue(json);
    }

    public static String InstantActionsToJsonString(InstantActions obj) throws JsonProcessingException {
        return getInstantActionsObjectWriter().writeValueAsString(obj);
    }

    public static Order OrderFromJsonString(String json) throws IOException {
        return getOrderObjectReader().readValue(json);
    }

    public static String OrderToJsonString(Order obj) throws JsonProcessingException {
        return getOrderObjectWriter().writeValueAsString(obj);
    }

    public static State StateFromJsonString(String json) throws IOException {
        return getStateObjectReader().readValue(json);
    }

    public static String StateToJsonString(State obj) throws JsonProcessingException {
        return getStateObjectWriter().writeValueAsString(obj);
    }

    public static Visualization VisualizationFromJsonString(String json) throws IOException {
        return getVisualizationObjectReader().readValue(json);
    }

    public static String VisualizationToJsonString(Visualization obj) throws JsonProcessingException {
        return getVisualizationObjectWriter().writeValueAsString(obj);
    }

    private static ObjectReader CommonReader;
    private static ObjectWriter CommonWriter;

    private static void instantiateCommonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String value = jsonParser.getText();
                return Converter.parseDateTimeString(value);
            }
        });
        mapper.registerModule(module);
        CommonReader = mapper.readerFor(Object.class);
        CommonWriter = mapper.writerFor(Object.class);
    }

    private static ObjectReader getCommonObjectReader() {
        if (CommonReader == null) instantiateCommonMapper();
        return CommonReader;
    }

    private static ObjectWriter getCommonObjectWriter() {
        if (CommonWriter == null) instantiateCommonMapper();
        return CommonWriter;
    }

    private static ObjectReader ConnectionReader;
    private static ObjectWriter ConnectionWriter;

    private static void instantiateConnectionMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String value = jsonParser.getText();
                return Converter.parseDateTimeString(value);
            }
        });
        mapper.registerModule(module);
        ConnectionReader = mapper.readerFor(Connection.class);
        ConnectionWriter = mapper.writerFor(Connection.class);
    }

    private static ObjectReader getConnectionObjectReader() {
        if (ConnectionReader == null) instantiateConnectionMapper();
        return ConnectionReader;
    }

    private static ObjectWriter getConnectionObjectWriter() {
        if (ConnectionWriter == null) instantiateConnectionMapper();
        return ConnectionWriter;
    }

    private static ObjectReader FactsheetReader;
    private static ObjectWriter FactsheetWriter;

    private static void instantiateFactsheetMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String value = jsonParser.getText();
                return Converter.parseDateTimeString(value);
            }
        });
        mapper.registerModule(module);
        FactsheetReader = mapper.readerFor(Factsheet.class);
        FactsheetWriter = mapper.writerFor(Factsheet.class);
    }

    private static ObjectReader getFactsheetObjectReader() {
        if (FactsheetReader == null) instantiateFactsheetMapper();
        return FactsheetReader;
    }

    private static ObjectWriter getFactsheetObjectWriter() {
        if (FactsheetWriter == null) instantiateFactsheetMapper();
        return FactsheetWriter;
    }

    private static ObjectReader HeaderReader;
    private static ObjectWriter HeaderWriter;

    private static void instantiateHeaderMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String value = jsonParser.getText();
                return Converter.parseDateTimeString(value);
            }
        });
        mapper.registerModule(module);
        HeaderReader = mapper.readerFor(Header.class);
        HeaderWriter = mapper.writerFor(Header.class);
    }

    private static ObjectReader getHeaderObjectReader() {
        if (HeaderReader == null) instantiateHeaderMapper();
        return HeaderReader;
    }

    private static ObjectWriter getHeaderObjectWriter() {
        if (HeaderWriter == null) instantiateHeaderMapper();
        return HeaderWriter;
    }

    private static ObjectReader InstantActionsReader;
    private static ObjectWriter InstantActionsWriter;

    private static void instantiateInstantActionsMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String value = jsonParser.getText();
                return Converter.parseDateTimeString(value);
            }
        });
        mapper.registerModule(module);
        InstantActionsReader = mapper.readerFor(InstantActions.class);
        InstantActionsWriter = mapper.writerFor(InstantActions.class);
    }

    private static ObjectReader getInstantActionsObjectReader() {
        if (InstantActionsReader == null) instantiateInstantActionsMapper();
        return InstantActionsReader;
    }

    private static ObjectWriter getInstantActionsObjectWriter() {
        if (InstantActionsWriter == null) instantiateInstantActionsMapper();
        return InstantActionsWriter;
    }

    private static ObjectReader OrderReader;
    private static ObjectWriter OrderWriter;

    private static void instantiateOrderMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String value = jsonParser.getText();
                return Converter.parseDateTimeString(value);
            }
        });
        mapper.registerModule(module);
        OrderReader = mapper.readerFor(Order.class);
        OrderWriter = mapper.writerFor(Order.class);
    }

    private static ObjectReader getOrderObjectReader() {
        if (OrderReader == null) instantiateOrderMapper();
        return OrderReader;
    }

    private static ObjectWriter getOrderObjectWriter() {
        if (OrderWriter == null) instantiateOrderMapper();
        return OrderWriter;
    }

    private static ObjectReader StateReader;
    private static ObjectWriter StateWriter;

    private static void instantiateStateMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String value = jsonParser.getText();
                return Converter.parseDateTimeString(value);
            }
        });
        mapper.registerModule(module);
        StateReader = mapper.readerFor(State.class);
        StateWriter = mapper.writerFor(State.class);
    }

    private static ObjectReader getStateObjectReader() {
        if (StateReader == null) instantiateStateMapper();
        return StateReader;
    }

    private static ObjectWriter getStateObjectWriter() {
        if (StateWriter == null) instantiateStateMapper();
        return StateWriter;
    }

    private static ObjectReader VisualizationReader;
    private static ObjectWriter VisualizationWriter;

    private static void instantiateVisualizationMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String value = jsonParser.getText();
                return Converter.parseDateTimeString(value);
            }
        });
        mapper.registerModule(module);
        VisualizationReader = mapper.readerFor(Visualization.class);
        VisualizationWriter = mapper.writerFor(Visualization.class);
    }

    private static ObjectReader getVisualizationObjectReader() {
        if (VisualizationReader == null) instantiateVisualizationMapper();
        return VisualizationReader;
    }

    private static ObjectWriter getVisualizationObjectWriter() {
        if (VisualizationWriter == null) instantiateVisualizationMapper();
        return VisualizationWriter;
    }
}
