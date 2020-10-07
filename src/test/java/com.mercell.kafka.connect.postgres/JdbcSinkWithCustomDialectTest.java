package com.mercell.kafka.connect.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.confluent.connect.jdbc.dialect.DatabaseDialect;
import io.confluent.connect.jdbc.dialect.DatabaseDialectProvider;
import io.confluent.connect.jdbc.sink.JdbcSinkConfig;
import io.confluent.connect.jdbc.sink.JdbcSinkTask;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTaskContext;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class JdbcSinkWithCustomDialectTest extends EasyMockSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSinkWithCustomDialectTest.class);
    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:12.4")
            .withDatabaseName("metabase")
            .withUsername("foo")
            .withPassword("secret");

    private static final Schema SCHEMA = SchemaBuilder.struct().name("com.example.Person")
            .field("name", Schema.STRING_SCHEMA)
            .field("age", Schema.OPTIONAL_INT32_SCHEMA)
            .field("cities", SchemaBuilder.array(Schema.STRING_SCHEMA))
            .field("modified", Timestamp.SCHEMA)
            .build();
    private static final Schema CHILD_SCHEMA = SchemaBuilder.struct().name("com.example.Child").field("name", Schema.STRING_SCHEMA).build();
    private static final Schema STRUCT_SCHEMA = SchemaBuilder.struct().name("com.example.Person")
            .field("name", Schema.STRING_SCHEMA)
            .field("age", Schema.OPTIONAL_INT32_SCHEMA)
            .field("child", CHILD_SCHEMA)
            .field("modified", Timestamp.SCHEMA)
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void managesToBuildInsertSchemaWithArray() {
        Map<String, String> props = new HashMap<>();
        props.put(JdbcSinkConfig.CONNECTION_URL, postgres.getJdbcUrl());
        props.put(JdbcSinkConfig.CONNECTION_PASSWORD, postgres.getPassword());
        props.put(JdbcSinkConfig.CONNECTION_USER, postgres.getUsername());
        props.put(JdbcSinkConfig.AUTO_CREATE, "true");
        props.put(JdbcSinkConfig.PK_MODE, "kafka");
        props.put(JdbcSinkConfig.PK_FIELDS, "kafka_topic,kafka_partition,kafka_offset");
        String timeZoneId = "Europe/Oslo";
        props.put(JdbcSinkConfig.DB_TIMEZONE_CONFIG, timeZoneId);
        props.put(JdbcSinkConfig.DIALECT_NAME_CONFIG, ComplexTypesPostgresDatabaseDialect.class.getSimpleName());
        Faker faker = new Faker();
        JdbcSinkTask task = new JdbcSinkTask();
        task.initialize(mock(SinkTaskContext.class));
        task.start(props);

        String name = faker.backToTheFuture().character();
        List<String> cities = Arrays.asList(faker.elderScrolls().city(), faker.elderScrolls().city(), faker.elderScrolls().city());
        int age = faker.number().numberBetween(18, 80);
        Date d = new Date();
        final Struct struct = new Struct(SCHEMA)
                .put("name", name)
                .put("age", age)
                .put("cities", cities)
                .put("modified", d);
        final String topic = "people";
        task.put(Collections.singleton(
                new SinkRecord(topic, 1, null, null, SCHEMA, struct, 42)
        ));
        verifyContentForArray(name, cities, age);
    }

    private void verifyContentForArray(String name, List<String> cities, int age) {
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = conn.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT * FROM people");
            while (rs.next()) {
                assertThat(rs.getString("name")).isEqualTo(name);
                assertThat(rs.getInt("age")).isEqualTo(age);
                JsonNode cities1 = objectMapper.readTree(rs.getString("cities"));
                assertThat(cities1.isArray()).isTrue();
                Iterator<JsonNode> elements = cities1.elements();
                while(elements.hasNext()) {
                    JsonNode next = elements.next();
                    assertThat(cities.contains(next.asText()));
                }
            }
        } catch (SQLException sqlEx) {
            LOGGER.error("Failed in getting connection", sqlEx);
        } catch (JsonMappingException e) {
            LOGGER.error("Failed to map json", e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to process json", e);
        }
    }

    @Test
    public void verifySerializeStruct() {
        Map<String, String> props = new HashMap<>();
        props.put(JdbcSinkConfig.CONNECTION_URL, postgres.getJdbcUrl());
        props.put(JdbcSinkConfig.CONNECTION_PASSWORD, postgres.getPassword());
        props.put(JdbcSinkConfig.CONNECTION_USER, postgres.getUsername());
        props.put(JdbcSinkConfig.AUTO_CREATE, "true");
        props.put(JdbcSinkConfig.PK_MODE, "kafka");
        props.put(JdbcSinkConfig.PK_FIELDS, "kafka_topic,kafka_partition,kafka_offset");
        String timeZoneId = "Europe/Oslo";
        props.put(JdbcSinkConfig.DB_TIMEZONE_CONFIG, timeZoneId);
        props.put(JdbcSinkConfig.DIALECT_NAME_CONFIG, ComplexTypesPostgresDatabaseDialect.class.getSimpleName());

        Faker faker = new Faker();
        JdbcSinkTask task = new JdbcSinkTask();
        task.initialize(mock(SinkTaskContext.class));
        task.start(props);

        String name = faker.backToTheFuture().character();
        String childname = faker.rickAndMorty().character();
        int age = faker.number().numberBetween(18, 80);
        Instant time = Instant.now();
        final Struct child = new Struct(CHILD_SCHEMA).put("name", childname);
        final Struct struct = new Struct(STRUCT_SCHEMA)
                .put("name", name)
                .put("age", age)
                .put("child", child)
                .put("modified", Date.from(time));
        final String topic = "family";
        task.put(Collections.singleton(
                new SinkRecord(topic, 1, null, null, STRUCT_SCHEMA, struct, 42)
        ));
        verifyContentForStruct(name, child, age);
    }

    private void verifyContentForStruct(String name, Struct child, int age) {
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = conn.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT * FROM family");
            while (rs.next()) {
                assertThat(rs.getString("name")).isEqualTo(name);
                assertThat(rs.getInt("age")).isEqualTo(age);
                JsonNode cities1 = objectMapper.readTree(rs.getString("child"));
                assertThat(cities1.get("name").asText()).isEqualTo(child.getString("name"));
            }
        } catch (SQLException sqlEx) {
            LOGGER.error("Failed in getting connection", sqlEx);
        } catch (JsonMappingException e) {
            LOGGER.error("Failed to map json", e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to process json", e);
        }
    }
}
