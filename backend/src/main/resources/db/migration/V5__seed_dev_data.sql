-- V5: Dev Seed Data
-- Password for all users: Admin@123 (BCrypt hash)

INSERT INTO users (name, email, mobile_number, password_hash, status) VALUES
('Super Admin',  'superadmin@donation.com',  '9876543210', '$2a$12$xY8mLn4JkQ2z1w0vP5rT8OzK6hFjBqX3dVuAcEiLsNpRmGtOwH7Yy', 'ACTIVE'),
('Admin User 1', 'admin1@donation.com',       '9876543211', '$2a$12$xY8mLn4JkQ2z1w0vP5rT8OzK6hFjBqX3dVuAcEiLsNpRmGtOwH7Yy', 'ACTIVE'),
('Admin User 2', 'admin2@donation.com',       '9876543212', '$2a$12$xY8mLn4JkQ2z1w0vP5rT8OzK6hFjBqX3dVuAcEiLsNpRmGtOwH7Yy', 'ACTIVE');

-- Assign roles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'superadmin@donation.com' AND r.name = 'Super Admin';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin1@donation.com' AND r.name = 'Admin';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin2@donation.com' AND r.name = 'Admin';

-- Assign areas to admins
INSERT INTO user_areas (user_id, area_id)
SELECT u.id, a.id FROM users u, areas a
WHERE u.email = 'admin1@donation.com' AND a.name IN ('North Zone', 'East Zone');

INSERT INTO user_areas (user_id, area_id)
SELECT u.id, a.id FROM users u, areas a
WHERE u.email = 'admin2@donation.com' AND a.name IN ('South Zone', 'West Zone');

-- Seed sample APPROVED income transactions
INSERT INTO transactions (transaction_number, type, amount, income_category_id, area_id, created_by, transaction_date, payment_method, reference_number, description, status, approved_by, approved_at)
SELECT
    CONCAT('TXN-2026-', LPAD(seq, 8, '0')),
    'INCOME',
    amount,
    cat_id,
    area_id,
    creator_id,
    tx_date,
    method,
    ref_num,
    descr,
    'APPROVED',
    approver_id,
    NOW()
FROM (
    SELECT 1 AS seq, 25000.00 AS amount,
           (SELECT id FROM income_categories WHERE name = 'Donations') AS cat_id,
           (SELECT id FROM areas WHERE name = 'North Zone') AS area_id,
           (SELECT id FROM users WHERE email = 'admin1@donation.com') AS creator_id,
           DATE_SUB(CURDATE(), INTERVAL 5 DAY) AS tx_date,
           'CASH' AS method, 'REF001' AS ref_num, 'Monthly donation collection' AS descr,
           (SELECT id FROM users WHERE email = 'superadmin@donation.com') AS approver_id
    UNION ALL SELECT 2, 15000.00,
           (SELECT id FROM income_categories WHERE name = 'Zakat'),
           (SELECT id FROM areas WHERE name = 'North Zone'),
           (SELECT id FROM users WHERE email = 'admin1@donation.com'),
           DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'BANK_TRANSFER', 'REF002', 'Zakat collection',
           (SELECT id FROM users WHERE email = 'superadmin@donation.com')
    UNION ALL SELECT 3, 30000.00,
           (SELECT id FROM income_categories WHERE name = 'Donations'),
           (SELECT id FROM areas WHERE name = 'South Zone'),
           (SELECT id FROM users WHERE email = 'admin2@donation.com'),
           DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'UPI', 'REF003', 'Community donation drive',
           (SELECT id FROM users WHERE email = 'superadmin@donation.com')
    UNION ALL SELECT 4, 10000.00,
           (SELECT id FROM income_categories WHERE name = 'Sadaqah'),
           (SELECT id FROM areas WHERE name = 'South Zone'),
           (SELECT id FROM users WHERE email = 'admin2@donation.com'),
           DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'CASH', 'REF004', 'Sadaqah collection',
           (SELECT id FROM users WHERE email = 'superadmin@donation.com')
    UNION ALL SELECT 5, 20000.00,
           (SELECT id FROM income_categories WHERE name = 'Event Collection'),
           (SELECT id FROM areas WHERE name = 'East Zone'),
           (SELECT id FROM users WHERE email = 'admin1@donation.com'),
           DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'CASH', 'REF005', 'Eid event collection',
           (SELECT id FROM users WHERE email = 'superadmin@donation.com')
) t;

-- Seed PENDING income transactions
INSERT INTO transactions (transaction_number, type, amount, income_category_id, area_id, created_by, transaction_date, payment_method, description, status)
VALUES
('TXN-2026-00000006', 'INCOME', 8000.00,
 (SELECT id FROM income_categories WHERE name = 'Donations'),
 (SELECT id FROM areas WHERE name = 'North Zone'),
 (SELECT id FROM users WHERE email = 'admin1@donation.com'),
 CURDATE(), 'CASH', 'Today pending donation', 'PENDING'),
('TXN-2026-00000007', 'INCOME', 5500.00,
 (SELECT id FROM income_categories WHERE name = 'Zakat'),
 (SELECT id FROM areas WHERE name = 'South Zone'),
 (SELECT id FROM users WHERE email = 'admin2@donation.com'),
 CURDATE(), 'UPI', 'Zakat pending approval', 'PENDING'),
('TXN-2026-00000008', 'INCOME', 3000.00,
 (SELECT id FROM income_categories WHERE name = 'Sadaqah'),
 (SELECT id FROM areas WHERE name = 'East Zone'),
 (SELECT id FROM users WHERE email = 'admin1@donation.com'),
 CURDATE(), 'CASH', 'Sadaqah pending', 'PENDING');

-- Seed REJECTED income transaction
INSERT INTO transactions (transaction_number, type, amount, income_category_id, area_id, created_by, transaction_date, payment_method, description, status, approved_by, approved_at, rejection_reason)
VALUES
('TXN-2026-00000009', 'INCOME', 2000.00,
 (SELECT id FROM income_categories WHERE name = 'Donations'),
 (SELECT id FROM areas WHERE name = 'West Zone'),
 (SELECT id FROM users WHERE email = 'admin2@donation.com'),
 DATE_SUB(CURDATE(), INTERVAL 6 DAY), 'CASH', 'Duplicate submission', 'REJECTED',
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'), NOW(), 'Duplicate entry - already recorded');

-- Seed APPROVED expenses
INSERT INTO transactions (transaction_number, type, amount, expense_category_id, area_id, created_by, transaction_date, payment_method, description, status, approved_by, approved_at)
VALUES
('TXN-2026-00000010', 'EXPENSE', 12000.00,
 (SELECT id FROM expense_categories WHERE name = 'Food Distribution'),
 (SELECT id FROM areas WHERE name = 'North Zone'),
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'),
 DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'BANK_TRANSFER', 'Monthly food ration distribution', 'APPROVED',
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'), NOW()),
('TXN-2026-00000011', 'EXPENSE', 8500.00,
 (SELECT id FROM expense_categories WHERE name = 'Medical Support'),
 (SELECT id FROM areas WHERE name = 'South Zone'),
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'),
 DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'CASH', 'Medical camp expenses', 'APPROVED',
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'), NOW()),
('TXN-2026-00000012', 'EXPENSE', 15000.00,
 (SELECT id FROM expense_categories WHERE name = 'Education'),
 (SELECT id FROM areas WHERE name = 'East Zone'),
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'),
 DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'BANK_TRANSFER', 'School fees for 10 students', 'APPROVED',
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'), NOW()),
('TXN-2026-00000013', 'EXPENSE', 5000.00,
 (SELECT id FROM expense_categories WHERE name = 'Transportation'),
 NULL,
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'),
 DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'CASH', 'Vehicle fuel and maintenance', 'APPROVED',
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'), NOW()),
('TXN-2026-00000014', 'EXPENSE', 4500.00,
 (SELECT id FROM expense_categories WHERE name = 'Administrative Expenses'),
 NULL,
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'),
 CURDATE(), 'CASH', 'Office supplies and printing', 'APPROVED',
 (SELECT id FROM users WHERE email = 'superadmin@donation.com'), NOW());

-- Seed transaction approval records for the approved income transactions
INSERT INTO transaction_approvals (transaction_id, action, performed_by, comments)
SELECT t.id, 'APPROVED', (SELECT id FROM users WHERE email = 'superadmin@donation.com'), 'Verified and approved'
FROM transactions t
WHERE t.status = 'APPROVED' AND t.type = 'INCOME';

INSERT INTO transaction_approvals (transaction_id, action, performed_by, comments)
SELECT t.id, 'REJECTED', (SELECT id FROM users WHERE email = 'superadmin@donation.com'), t.rejection_reason
FROM transactions t
WHERE t.status = 'REJECTED';
