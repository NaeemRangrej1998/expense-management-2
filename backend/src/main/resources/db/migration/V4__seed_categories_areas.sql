-- V4: Seed Categories and Areas

-- Income Categories
INSERT INTO income_categories (name, description, status) VALUES
('Donations', 'General monetary donations', 'ACTIVE'),
('Zakat', 'Obligatory Islamic alms', 'ACTIVE'),
('Sadaqah', 'Voluntary charitable contributions', 'ACTIVE'),
('Grants', 'Grants from organizations or government', 'ACTIVE'),
('Membership Fees', 'Annual membership contributions', 'ACTIVE'),
('Event Collection', 'Collections from events and programs', 'ACTIVE'),
('Other Income', 'Miscellaneous income', 'ACTIVE');

-- Expense Categories
INSERT INTO expense_categories (name, description, status) VALUES
('Food Distribution', 'Food and ration distribution to beneficiaries', 'ACTIVE'),
('Medical Support', 'Medical expenses and healthcare aid', 'ACTIVE'),
('Education', 'Educational support, books, fees', 'ACTIVE'),
('Transportation', 'Transport and logistics costs', 'ACTIVE'),
('Administrative Expenses', 'Office and operational costs', 'ACTIVE'),
('Utilities', 'Electricity, water, and other utilities', 'ACTIVE'),
('Salaries', 'Staff salaries and wages', 'ACTIVE'),
('Other Expenses', 'Miscellaneous expenses', 'ACTIVE');

-- Sample Areas
INSERT INTO areas (name, description, status) VALUES
('North Zone', 'Northern operational area', 'ACTIVE'),
('South Zone', 'Southern operational area', 'ACTIVE'),
('East Zone', 'Eastern operational area', 'ACTIVE'),
('West Zone', 'Western operational area', 'ACTIVE'),
('Central Zone', 'Central operational area', 'ACTIVE');
