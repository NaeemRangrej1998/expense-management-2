-- V6: Add new user naimrangrej554@gmail.com
INSERT INTO users (name, email, mobile_number, password_hash, status) 
VALUES ('Naim Rangrej', 'naimrangrej554@gmail.com', '9999999999', '$2a$10$us5GTKZs1pAzz7OkE9p5fuVnSf1UzISDa7O55.1.uvjgqnZGCsGHG', 'ACTIVE');

-- Assign Super Admin role to the new user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'naimrangrej554@gmail.com' AND r.name = 'Super Admin';
