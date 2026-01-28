-- =====================================================
-- INSERT ADMIN USER
-- =====================================================
-- Password hash: $2a$10$uNNBl900Ykh07evH2GXWsOa/.FuPZONw9Lq3XgWxB26KBbvvJELcW
-- Plaintext password (for reference): [Use BCrypt to verify]

-- Column order: id, active, created_at, email, full_name, password, phone, role, updated_at, 
--               address, deposit_percentage, district, latitude, longitude, province, tier, total_spent, ward

INSERT INTO users (
    active,
    created_at,
    email,
    full_name,
    password,
    phone,
    role,
    updated_at,
    address,
    deposit_percentage,
    district,
    latitude,
    longitude,
    province,
    tier,
    total_spent,
    ward
) VALUES (
    TRUE,                                                             -- active
    NOW(),                                                            -- created_at
    'admin@badminton.com',                                           -- email
    'System Administrator',                                          -- full_name
    '$2a$10$uNNBl900Ykh07evH2GXWsOa/.FuPZONw9Lq3XgWxB26KBbvvJELcW', -- password (BCrypt hash)
    '0999999999',                                                     -- phone
    'ADMIN',                                                          -- role
    NOW(),                                                            -- updated_at
    '227 Nguyễn Văn Cừ, Quận 5',                                     -- address
    30,                                                               -- deposit_percentage
    'Quận 5',                                                         -- district
    10.7543414,                                                       -- latitude (Saigon, Q5)
    106.6674638,                                                      -- longitude (Saigon, Q5)
    'Hồ Chí Minh',                                                    -- province
    'BRONZE',                                                         -- tier
    0.00,                                                             -- total_spent
    'Phường 4'                                                        -- ward
);

-- =====================================================
-- VERIFY ADMIN USER
-- =====================================================
SELECT 
    id,
    full_name,
    email,
    phone,
    role,
    active,
    tier,
    created_at
FROM users
WHERE email = 'admin@badminton.com';

-- =====================================================
-- ALTERNATIVE: UPDATE EXISTING USER TO ADMIN
-- =====================================================
-- If you want to change an existing user to admin:
/*
UPDATE users 
SET 
    role = 'ADMIN',
    password = '$2a$10$uNNBl900Ykh07evH2GXWsOa/.FuPZONw9Lq3XgWxB26KBbvvJELcW',
    updated_at = NOW()
WHERE email = 'your-email@example.com';
*/

-- =====================================================
-- OPTIONAL: CREATE MULTIPLE ADMIN USERS
-- =====================================================
/*
INSERT INTO users (full_name, email, phone, password, role, active, total_spent, tier, deposit_percentage, created_at, updated_at) VALUES
('Admin 1', 'admin1@badminton.com', '0911111111', '$2a$10$uNNBl900Ykh07evH2GXWsOa/.FuPZONw9Lq3XgWxB26KBbvvJELcW', 'ADMIN', TRUE, 0, 'BRONZE', 30, NOW(), NOW()),
('Admin 2', 'admin2@badminton.com', '0922222222', '$2a$10$uNNBl900Ykh07evH2GXWsOa/.FuPZONw9Lq3XgWxB26KBbvvJELcW', 'ADMIN', TRUE, 0, 'BRONZE', 30, NOW(), NOW());
*/

-- =====================================================
-- DELETE ADMIN USER (if needed)
-- =====================================================
/*
DELETE FROM users WHERE email = 'admin@badminton.com';
*/
