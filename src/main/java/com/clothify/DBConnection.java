package com.clothify;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        Connection con = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            /*
             * For hosting, values come from Render Environment Variables.
             * For local Eclipse, if environment variables are empty,
             * it will use your local MySQL database.
             */

            String host = System.getenv("DB_HOST");
            String port = System.getenv("DB_PORT");
            String dbName = System.getenv("DB_NAME");
            String user = System.getenv("DB_USER");
            String password = System.getenv("DB_PASSWORD");

            // Local fallback for Eclipse
            if (host == null || host.isEmpty()) {
                host = "localhost";
            }

            if (port == null || port.isEmpty()) {
                port = "3306";
            }

            if (dbName == null || dbName.isEmpty()) {
                dbName = "fashion_store";
            }

            if (user == null || user.isEmpty()) {
                user = "root";
            }

            if (password == null) {
                password = "";
            }

            String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                    + "?useSSL=true"
                    + "&requireSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=UTC";

            con = DriverManager.getConnection(url, user, password);

            System.out.println("Database Connected Successfully");

        } catch (Exception e) {
            System.out.println("Database Connection Failed");
            e.printStackTrace();
        }

        return con;
    }
}