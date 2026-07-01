package com.car.sharing.app.util;

public final class TestConstants {
    public static final String CLEANUP_CARS_PATH = "classpath:db/cleanup/cleanup-cars.sql";
    public static final String CLEANUP_PAYMENTS_PATH =
            "classpath:db/cleanup/cleanup-payments.sql";
    public static final String CLEANUP_RENTALS_PATH = "classpath:db/cleanup/cleanup-rentals.sql";
    public static final String CLEANUP_USERS_PATH = "classpath:db/cleanup/cleanup-users.sql";
    public static final String ADD_USER_DB_PATH = "classpath:db/setup/add-user.sql";
    public static final String ADD_CARS_DB_PATH = "classpath:db/setup/add-three-cars.sql";
    public static final String ADD_USERS_DB_PATH =
            "classpath:db/setup/add-customer-and-manager.sql";
    public static final String ADD_CAR_WITH_STOCK_PATH =
            "classpath:db/setup/add-car-with-stock.sql";
    public static final String ADD_OUT_OF_STOCK_CAR_PATH =
            "classpath:db/setup/add-out-of-stock-car.sql";
    public static final String ADD_RENTAL_PATH = "classpath:db/setup/add-rental-for-customer.sql";
    public static final String ADD_USERS_WITH_ADMIN_PATH =
            "classpath:db/setup/add-customer-manager-admin.sql";
    public static final String ADD_RENTAL_WITH_PAYMENT_PATH =
            "classpath:db/setup/add-rental-with-payment.sql";
    public static final String ADD_PENDING_PAYMENT_PATH =
            "classpath:db/setup/add-pending-payment.sql";
    public static final String CLEANUP_RENTALS_CONTROLLER_PATH =
            "classpath:db/cleanup/cleanup-all.sql";
    public static final String CLEANUP_PAYMENTS_CONTROLLER_PATH =
            "classpath:db/cleanup/cleanup-all.sql";

    private TestConstants() {
    }
}
