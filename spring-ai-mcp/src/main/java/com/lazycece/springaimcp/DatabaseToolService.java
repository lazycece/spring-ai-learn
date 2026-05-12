package com.lazycece.springaimcp;

import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseToolService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseToolService.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseToolService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @McpTool(description = "列出数据库中所有表的名称")
    public List<String> listTables() {
        log.info("Tool [listTables] called");
        return jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' ORDER BY TABLE_NAME",
                String.class
        );
    }

    @McpTool(description = "查看指定表的结构，返回每列的列名、类型、长度和是否可空")
    public List<ColumnInfo> describeTable(@McpToolParam(description = "表名") String tableName) {
        log.info("Tool [describeTable] called, tableName={}", tableName);
        // security: only query current schema
        String sql = "SELECT COLUMN_NAME, TYPE_NAME, COLUMN_SIZE, NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ColumnInfo(
                rs.getString("COLUMN_NAME"),
                rs.getString("TYPE_NAME"),
                rs.getInt("COLUMN_SIZE"),
                "YES".equalsIgnoreCase(rs.getString("NULLABLE"))
        ), tableName);
    }

    @McpTool(description = "执行只读 SELECT 查询，返回结果行列表。仅允许 SELECT 语句，禁止 INSERT/UPDATE/DELETE/DDL")
    public List<Map<String, Object>> executeQuery(@McpToolParam(description = "SQL 查询语句（仅允许 SELECT）") String sql) {
        log.info("Tool [executeQuery] called, sql={}", sql);
        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            return row;
        });
    }
}
