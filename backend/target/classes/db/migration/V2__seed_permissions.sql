-- V2: Seed Permissions
INSERT INTO permissions (name, description, module, action) VALUES
-- Dashboard
('dashboard.view', 'View dashboard', 'DASHBOARD', 'VIEW'),
-- Income
('income.view', 'View income transactions', 'INCOME', 'VIEW'),
('income.create', 'Create income transactions', 'INCOME', 'CREATE'),
('income.update', 'Update income transactions', 'INCOME', 'UPDATE'),
('income.approve', 'Approve income transactions', 'INCOME', 'APPROVE'),
('income.reject', 'Reject income transactions', 'INCOME', 'REJECT'),
-- Expense
('expense.view', 'View expense transactions', 'EXPENSE', 'VIEW'),
('expense.create', 'Create expense transactions', 'EXPENSE', 'CREATE'),
('expense.update', 'Update expense transactions', 'EXPENSE', 'UPDATE'),
-- Transaction History
('transaction_history.view', 'View transaction history', 'TRANSACTION_HISTORY', 'VIEW'),
-- Users
('users.view', 'View users', 'USERS', 'VIEW'),
('users.create', 'Create users', 'USERS', 'CREATE'),
('users.update', 'Update users', 'USERS', 'UPDATE'),
('users.deactivate', 'Deactivate users', 'USERS', 'DEACTIVATE'),
-- Roles
('roles.view', 'View roles', 'ROLES', 'VIEW'),
('roles.create', 'Create roles', 'ROLES', 'CREATE'),
('roles.update', 'Update roles', 'ROLES', 'UPDATE'),
-- Permissions
('permissions.view', 'View permissions', 'PERMISSIONS', 'VIEW'),
-- Areas
('areas.view', 'View areas', 'AREAS', 'VIEW'),
('areas.create', 'Create areas', 'AREAS', 'CREATE'),
('areas.update', 'Update areas', 'AREAS', 'UPDATE'),
('areas.deactivate', 'Deactivate areas', 'AREAS', 'DEACTIVATE'),
-- Reports
('reports.view', 'View reports', 'REPORTS', 'VIEW'),
-- Audit Logs
('audit_logs.view', 'View audit logs', 'AUDIT_LOGS', 'VIEW');
