package com.mercell.kafka.connect.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.connect.jdbc.dialect.DatabaseDialect;
import io.confluent.connect.jdbc.dialect.DatabaseDialectProvider;
import io.confluent.connect.jdbc.dialect.PostgreSqlDatabaseDialect;
import io.confluent.connect.jdbc.sink.metadata.SinkRecordField;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.connect.data.*;
import org.postgresql.util.PGobject;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ComplexTypesPostgresDatabaseDialect extends PostgreSqlDatabaseDialect {

    private static String SUB_PROTOCOL_NAME = "postgrescmplx";

    /**
     * The provider for {@link ComplexTypesPostgresDatabaseDialect}.
     */
    public static class Provider extends DatabaseDialectProvider.SubprotocolBasedProvider {
        public Provider() {
            super(ComplexTypesPostgresDatabaseDialect.class.getSimpleName(), SUB_PROTOCOL_NAME);
        }

        @Override
        public DatabaseDialect create(AbstractConfig config) {
            return new ComplexTypesPostgresDatabaseDialect(config);
        }
    }

    public ComplexTypesPostgresDatabaseDialect(AbstractConfig config) {
        super(config);
    }

    @Override
    protected String getSqlType(SinkRecordField field) {
        if (field.schemaName() != null) {
            switch (field.schemaName()) {
                case Decimal.LOGICAL_NAME:
                    return "DECIMAL";
                case Date.LOGICAL_NAME:
                    return "DATE";
                case Time.LOGICAL_NAME:
                    return "TIME";
                case Timestamp.LOGICAL_NAME:
                    return "TIMESTAMP";
                default:
                    // fall through to normal types
            }
        }
        switch (field.schemaType()) {
            case STRUCT:
            case ARRAY:
                return "JSONB";
            default:
                return super.getSqlType(field);
        }
    }

    @Override
    protected boolean maybeBindPrimitive(PreparedStatement statement, int index, Schema schema, Object value) throws SQLException {
        switch (schema.type()) {
            case STRUCT:
            case ARRAY:
                SimpleJsonConverter simpleJson = new SimpleJsonConverter(); // SimpleJsonConverter from datamountaineer
                JsonNode node = simpleJson.fromConnectData(schema, value);
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue(node.toString());
                statement.setObject(index, jsonObject);
                break;
            default:
                return super.maybeBindPrimitive(statement, index, schema, value);
        }
        return true;
    }
}

