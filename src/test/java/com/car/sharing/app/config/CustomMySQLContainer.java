package com.car.sharing.app.config;

import org.testcontainers.containers.MySQLContainer;

public class CustomMySQLContainer extends MySQLContainer<CustomMySQLContainer> {
    private static final String DB_IMAGE = "mysql:8.0";
    private static CustomMySQLContainer container;

    private CustomMySQLContainer() {
        super(DB_IMAGE);
        withDatabaseName("test_db");
        withUsername("test");
        withPassword("test");
    }

    public static synchronized CustomMySQLContainer getInstance() {
        if (container == null) {
            container = new CustomMySQLContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("TEST_DB_URL", getJdbcUrl());
        System.setProperty("TEST_DB_USERNAME", getUsername());
        System.setProperty("TEST_DB_PASSWORD", getPassword());
    }

    @Override
    public void stop() { }
}
