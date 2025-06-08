package org.xhy.gateway.infrastructure.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.Map;

/**
 * PostgreSQL JSONB 类型处理器
 * 专门处理 PostgreSQL 的 JSONB 类型与 Java Map 对象之间的转换
 * 
 * @author xhy
 * @since 1.0.0
 */
@MappedTypes({Map.class})
public class PostgreSQLJsonbTypeHandler extends BaseTypeHandler<Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
        try {
            // 将 Map 对象转换为 JSON 字符串
            String jsonString = objectMapper.writeValueAsString(parameter);
            // 使用 PostgreSQL 的 CAST 语法直接转换为 JSONB
            ps.setObject(i, jsonString, Types.OTHER);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting Map to JSONB: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String jsonString = rs.getString(columnName);
        return parseJson(jsonString);
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String jsonString = rs.getString(columnIndex);
        return parseJson(jsonString);
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String jsonString = cs.getString(columnIndex);
        return parseJson(jsonString);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String jsonString) throws SQLException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonString, Map.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error parsing JSON: " + e.getMessage(), e);
        }
    }
} 