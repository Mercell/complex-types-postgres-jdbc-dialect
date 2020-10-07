package com.mercell.kafka.connect.postgres;

import com.datamountaineer.streamreactor.connect.json.SimpleJsonConverter;
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
            case INT8:
                return "SMALLINT";
            case INT16:
                return "SMALLINT";
            case INT32:
                return "INT";
            case INT64:
                return "BIGINT";
            case FLOAT32:
                return "REAL";
            case FLOAT64:
                return "DOUBLE PRECISION";
            case BOOLEAN:
                return "BOOLEAN";
            case STRING:
                return "TEXT";
            case BYTES:
                return "BYTEA";
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
            case INT8:
                statement.setByte(index, (Byte) value);
                break;
            case INT16:
                statement.setShort(index, (Short) value);
                break;
            case INT32:
                statement.setInt(index, (Integer) value);
                break;
            case INT64:
                statement.setLong(index, (Long) value);
                break;
            case FLOAT32:
                statement.setFloat(index, (Float) value);
                break;
            case FLOAT64:
                statement.setDouble(index, (Double) value);
                break;
            case BOOLEAN:
                statement.setBoolean(index, (Boolean) value);
                break;
            case STRING:
                statement.setString(index, (String) value);
                break;
            case BYTES:
                final byte[] bytes;
                if (value instanceof ByteBuffer) {
                    final ByteBuffer buffer = ((ByteBuffer) value).slice();
                    bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                } else {
                    bytes = (byte[]) value;
                }
                statement.setBytes(index, bytes);
                break;
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
                return false;
        }
        return true;
    }
}

