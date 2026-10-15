-- Sample data: 30 employees, so search, filters and pagination have something to work with.
-- Run after schema.sql:   mysql -u root -p < database/seed.sql
-- INSERT IGNORE + the unique email key make this safe to run more than once.

USE ems_db;

INSERT IGNORE INTO employees
    (full_name, email, phone_number, department, designation, date_of_joining, salary, status)
VALUES
    ('Aarav Sharma',       'aarav.sharma@example.com',       '+91 98450 10001', 'Engineering',       'Software Engineer',              '2022-06-13', 1200000.00, 'ACTIVE'),
    ('Priya Nair',         'priya.nair@example.com',         '+91 98450 10002', 'Engineering',       'Senior Software Engineer',       '2020-02-03', 2200000.00, 'ACTIVE'),
    ('Rohan Mehta',        'rohan.mehta@example.com',        '+91 98450 10003', 'Engineering',       'Backend Developer',              '2023-08-21', 1400000.00, 'ACTIVE'),
    ('Ananya Iyer',        'ananya.iyer@example.com',        '+91 98450 10004', 'Engineering',       'Frontend Developer',             '2023-01-16', 1300000.00, 'ACTIVE'),
    ('Vikram Singh',       'vikram.singh@example.com',       '+91 98450 10005', 'Engineering',       'Engineering Manager',            '2018-09-10', 3500000.00, 'ACTIVE'),
    ('Sneha Reddy',        'sneha.reddy@example.com',        '+91 98450 10006', 'Engineering',       'DevOps Engineer',                '2021-11-29', 1700000.00, 'INACTIVE'),
    ('Karthik Rao',        'karthik.rao@example.com',        '+91 98450 10007', 'Quality Assurance', 'QA Engineer',                    '2022-03-07',  900000.00, 'ACTIVE'),
    ('Divya Menon',        'divya.menon@example.com',        '+91 98450 10008', 'Quality Assurance', 'Senior QA Engineer',             '2019-07-15', 1500000.00, 'ACTIVE'),
    ('Arjun Patel',        'arjun.patel@example.com',        '+91 98450 10009', 'Quality Assurance', 'Automation Tester',              '2024-02-05',  850000.00, 'ACTIVE'),
    ('Meera Kapoor',       'meera.kapoor@example.com',       '+91 98450 10010', 'Human Resources',   'HR Manager',                     '2017-04-24', 2400000.00, 'ACTIVE'),
    ('Rahul Verma',        'rahul.verma@example.com',        '+91 98450 10011', 'Human Resources',   'Talent Acquisition Specialist',  '2022-10-17',  800000.00, 'ACTIVE'),
    ('Pooja Joshi',        'pooja.joshi@example.com',        '+91 98450 10012', 'Human Resources',   'HR Executive',                   '2023-05-02',  600000.00, 'INACTIVE'),
    ('Sandeep Kumar',      'sandeep.kumar@example.com',      '+91 98450 10013', 'Finance',           'Financial Analyst',              '2021-01-18', 1100000.00, 'ACTIVE'),
    ('Neha Gupta',         'neha.gupta@example.com',         '+91 98450 10014', 'Finance',           'Senior Accountant',              '2019-12-09', 1350000.00, 'ACTIVE'),
    ('Aditya Bansal',      'aditya.bansal@example.com',      '+91 98450 10015', 'Finance',           'Finance Manager',                '2016-06-20', 3000000.00, 'ACTIVE'),
    ('Ishita Chatterjee',  'ishita.chatterjee@example.com',  '+91 98450 10016', 'Finance',           'Payroll Specialist',             '2022-09-12',  750000.00, 'ACTIVE'),
    ('Manish Desai',       'manish.desai@example.com',       '+91 98450 10017', 'Marketing',         'Marketing Manager',              '2018-03-26', 2600000.00, 'ACTIVE'),
    ('Kavya Pillai',       'kavya.pillai@example.com',       '+91 98450 10018', 'Marketing',         'Content Strategist',             '2023-03-13',  900000.00, 'ACTIVE'),
    ('Nikhil Agarwal',     'nikhil.agarwal@example.com',     '+91 98450 10019', 'Marketing',         'SEO Analyst',                    '2021-08-23',  780000.00, 'INACTIVE'),
    ('Riya Malhotra',      'riya.malhotra@example.com',      '+91 98450 10020', 'Marketing',         'Graphic Designer',               '2022-12-05',  820000.00, 'ACTIVE'),
    ('Suresh Naidu',       'suresh.naidu@example.com',       '+91 98450 10021', 'Sales',             'Sales Executive',                '2022-07-11',  700000.00, 'ACTIVE'),
    ('Tanvi Kulkarni',     'tanvi.kulkarni@example.com',     '+91 98450 10022', 'Sales',             'Account Manager',                '2020-10-19', 1600000.00, 'ACTIVE'),
    ('Harsh Vardhan',      'harsh.vardhan@example.com',      '+91 98450 10023', 'Sales',             'Regional Sales Head',            '2017-11-06', 3200000.00, 'ACTIVE'),
    ('Lakshmi Narayan',    'lakshmi.narayan@example.com',    '+91 98450 10024', 'Sales',             'Business Development Associate','2024-04-01',  650000.00, 'ACTIVE'),
    ('Gaurav Saxena',      'gaurav.saxena@example.com',      '+91 98450 10025', 'Operations',        'Operations Manager',             '2018-05-14', 2700000.00, 'ACTIVE'),
    ('Swati Bhatt',        'swati.bhatt@example.com',        '+91 98450 10026', 'Operations',        'Logistics Coordinator',          '2021-06-28',  720000.00, 'INACTIVE'),
    ('Deepak Choudhary',   'deepak.choudhary@example.com',   '+91 98450 10027', 'Operations',        'Supply Chain Analyst',           '2023-09-18',  950000.00, 'ACTIVE'),
    ('Ritu Sethi',         'ritu.sethi@example.com',         '+91 98450 10028', 'Operations',        'Facilities Executive',           '2020-01-27',  680000.00, 'ACTIVE'),
    ('Yash Thakur',        'yash.thakur@example.com',        '+91 98450 10029', 'Engineering',       'Data Engineer',                  '2022-02-14', 1650000.00, 'ACTIVE'),
    ('Amrita Bose',        'amrita.bose@example.com',        '+91 98450 10030', 'Engineering',       'Software Engineer Intern',       '2025-01-20',  300000.00, 'ACTIVE');
