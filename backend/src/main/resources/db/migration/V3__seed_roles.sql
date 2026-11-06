-- V3: Seed Roles and Assign Permissions

-- Insert system roles
INSERT INTO roles (name, description, is_system_role) VALUES
('Super Admin', 'System-wide administrative access', TRUE),
('Admin', 'Area-scoped administrative access', TRUE);

-- Assign ALL permissions to Super Admin
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Super Admin';

-- Assign default Admin permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Admin'
  AND p.name IN (
    'dashboard.view',
    'income.view',
    'income.create',
    'transaction_history.view'
  );
