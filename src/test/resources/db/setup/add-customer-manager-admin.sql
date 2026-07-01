INSERT INTO users (email, first_name, last_name, password, role, is_deleted)
VALUES ('customer@example.com', 'Alice', 'Smith',
        '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq4D1ZLnUF7e9SoZP2dPyqYyMm3xqK',
        'CUSTOMER', false),
       ('manager@example.com', 'Bob', 'Manager',
        '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq4D1ZLnUF7e9SoZP2dPyqYyMm3xqK',
        'MANAGER', false),
       ('admin@example.com', 'Carol', 'Admin',
        '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq4D1ZLnUF7e9SoZP2dPyqYyMm3xqK',
        'ADMIN', false);