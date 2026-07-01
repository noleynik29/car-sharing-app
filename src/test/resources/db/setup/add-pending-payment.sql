INSERT INTO payments (status, type, rental_id, session_url, session_id, amount_to_pay)
VALUES ('PENDING', 'PAYMENT', 1,
        'https://checkout.stripe.com/session/test_pending',
        'cs_test_pending_session_001',
        182.00);