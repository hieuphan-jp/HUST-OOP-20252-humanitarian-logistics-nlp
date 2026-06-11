package com.disaster.analysis.infrastructure.repository;

import com.disaster.analysis.domain.contract.repository.DatabaseInitializer;
import com.disaster.analysis.infrastructure.repository.context.DbContext;
import com.disaster.analysis.util.LogUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Lớp chuyên trách khởi động, kiểm tra trạng thái tồn tại của Database và Table.
 * Tận dụng lại cấu hình môi trường từ DbContext để tránh lặp code (DRY).
 */
public class SqlServerDatabaseInitializerImpl implements DatabaseInitializer{

    public void initialize() {
        LogUtil.info("Database startup and verification sequence initiated.");

        // Lấy tên Database đã được DbContext nạp sẵn từ file .env
        String dbName = DbContext.getDbName();

        if (dbName == null || dbName.trim().isEmpty()) {
            LogUtil.error("Initialization canceled: Missing DB_NAME in environment configuration.");
            return;
        }

        // 🌟 BƯỚC 1: Dùng hàm getRootConnection() của DbContext để lấy kết nối ROOT
        try (Connection conn = DbContext.getRootConnection();
             Statement stmt = conn.createStatement()) {

            // Kiểm tra xem Database mục tiêu đã tồn tại chưa
            String checkDbQuery = "SELECT database_id FROM sys.databases WHERE name = '" + dbName + "'";
            boolean isDatabaseExist = false;

            try (ResultSet rs = stmt.executeQuery(checkDbQuery)) {
                if (rs.next()) {
                    isDatabaseExist = true;
                }
            }

            // BƯỚC 2: Rẽ nhánh xử lý và ghi Log
            if (isDatabaseExist) {
                LogUtil.info("Database [" + dbName + "] already exists. Skipping database creation step.");
            } else {
                LogUtil.info("Database [" + dbName + "] does not exist. Triggering fresh database provisioning...");
                stmt.execute("CREATE DATABASE [" + dbName + "]");
                LogUtil.info("Database [" + dbName + "] has been created successfully from scratch.");
            }

            // BƯỚC 3: Chuyển ống dẫn kết nối vào Database thảm họa
            stmt.execute("USE [" + dbName + "]");

            // BƯỚC 4: Đọc file schema.sql để dựng cấu trúc các bảng
            LogUtil.info("Reading schema.sql from system resources to verify entity tables...");
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/schema.sql")) {
                if (is == null) {
                    LogUtil.warn("Execution paused: schema.sql not found. Application will boot with an empty database state.");
                    return;
                }

                String sqlScript = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String[] sqlStatements = sqlScript.split(";");

                for (String sql : sqlStatements) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql.trim());
                    }
                }

                LogUtil.info("All schema definitions and table instances have been successfully verified/installed.");
            }

        } catch (Exception e) {
            LogUtil.error("Critical failure encountered during target database initialization flow!", e);
        }
    }
}