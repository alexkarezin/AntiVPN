package dev.brighten.antivpn.database.sql.utils;

import dev.brighten.antivpn.AntiVPN;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {
    private static Connection conn;

    public static void init() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection("jdbc:mysql://" + AntiVPN.getInstance().getConfig().getIp()
                                + ":" + AntiVPN.getInstance().getConfig().getPort()
                                + "/?useSSL=true&autoReconnect=true",
                        AntiVPN.getInstance().getConfig().getUsername(),
                        AntiVPN.getInstance().getConfig().getPassword());
                conn.setAutoCommit(true);
                Query.use(conn);
                Query.prepare("CREATE DATABASE IF NOT EXISTS `"
                        + AntiVPN.getInstance().getConfig().getDatabaseName() + "`").execute();
                Query.prepare("USE `" + AntiVPN.getInstance().getConfig().getDatabaseName() + "`").execute();
                AntiVPN.getInstance().getExecutor().log("Connection to MySQL has been established.");
            }
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().log("Failed to load mysql: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*public static void initH2() {
        File dataFolder = new File(AntiVPN.getInstance().getPluginFolder(), "databases" + File.separator + "database");
        try {
            Class.forName("org.h2.Driver");
            conn = new NonClosableConnection(new JdbcConnection("jdbc:h2:file:" +
                    dataFolder.getAbsolutePath(), new Properties()));
            conn.setAutoCommit(true);
            Query.use(conn);
            AntiVPN.getInstance().getExecutor().log("Connection to SQlLite has been established.");
        } catch (SQLException ex) {
            AntiVPN.getInstance().getExecutor().log("SQLite exception on initialize");
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            AntiVPN.getInstance().getExecutor().log("No H2 library found!");
        }
    }*/

    public static void use() {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        try {
            if(conn != null && !conn.isClosed()) {
                if(conn instanceof NonClosableConnection) {
                    ((NonClosableConnection)conn).shutdown();
                } else conn.close();
                conn = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isClosed() {
        if(conn == null)
            return true;

        try {
            return conn.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }
}
