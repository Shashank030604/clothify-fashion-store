package com.clothify;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        Connection con = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String dbHost = System.getenv("DB_HOST");
            String dbPort = System.getenv("DB_PORT");
            String dbName = System.getenv("DB_NAME");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbHost == null || dbPort == null || dbName == null || dbUser == null || dbPassword == null) {
                System.out.println("Database environment variables are missing.");
                return null;
            }

            dbHost = dbHost.trim();
            dbPort = dbPort.trim();
            dbName = dbName.trim();
            dbUser = dbUser.trim();
            dbPassword = dbPassword.trim();

            String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
                    + "?sslMode=REQUIRED"
                    + "&allowPublicKeyRetrieval=true"
                    + "&useSSL=true"
                    + "&serverTimezone=UTC";

            con = DriverManager.getConnection(url, dbUser, dbPassword);

            System.out.println("Database Connected Successfully");

        } catch (Exception e) {
            System.out.println("Database Connection Failed");
            e.printStackTrace();
        }

        return con;
    }
}