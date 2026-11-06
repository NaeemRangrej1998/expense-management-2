-- Repair only demo accounts that still have the original unusable seed hash.
UPDATE users
SET password_hash = '$2b$12$k.Khdop5mva9gspW/tN33uYM5T1B.4217uCbkQN6MOvj.ZJdMNA02'
WHERE email IN (
    'superadmin@donation.com',
    'admin1@donation.com',
    'admin2@donation.com'
)
AND password_hash = '$2a$12$xY8mLn4JkQ2z1w0vP5rT8OzK6hFjBqX3dVuAcEiLsNpRmGtOwH7Yy';
