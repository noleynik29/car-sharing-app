INSERT INTO users (email, first_name, last_name, password, role, is_deleted)
VALUES ('customer@example.com', 'Alice', 'Smith',
        '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq4D1ZLnUF7e9SoZP2dPyqYyMm3xqK',
        'CUSTOMER', false),
       ('manager@example.com', 'Bob', 'Manager',
        '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq4D1ZLnUF7e9SoZP2dPyqYyMm3xqK',
        'MANAGER', false);

INSERT INTO cars (model, brand, type, inventory, daily_fee, is_deleted)
VALUES ('Camry', 'Toyota', 'SEDAN', 5, 45.50, false);

INSERT INTO rentals (rental_date, return_date, actual_return_date, car_id, user_id)
VALUES ('2026-06-01', '2026-06-05', NULL, 1, 1);