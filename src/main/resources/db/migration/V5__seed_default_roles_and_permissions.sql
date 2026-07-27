-- Default Roles
INSERT INTO roles (id, name, description, is_system) VALUES
    ('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Full system access', TRUE),
    ('00000000-0000-0000-0000-000000000002', 'GYM_ADMIN', 'Gym management and staff management', TRUE),
    ('00000000-0000-0000-0000-000000000003', 'TRAINER', 'View assigned customers, manage own schedule', TRUE),
    ('00000000-0000-0000-0000-000000000004', 'MEMBER', 'Self-service: bookings, workouts, profile', TRUE),
    ('00000000-0000-0000-0000-000000000005', 'STAFF', 'Front desk operations', TRUE);

-- Default Permissions
INSERT INTO permissions (id, resource, action, description) VALUES
    (gen_random_uuid(), 'user', 'manage', 'Full user management'),
    (gen_random_uuid(), 'customer', 'manage', 'Full customer management'),
    (gen_random_uuid(), 'customer', 'read', 'View customer profiles'),
    (gen_random_uuid(), 'customer', 'read_own', 'View own customer profile'),
    (gen_random_uuid(), 'exercise', 'manage', 'Full exercise management'),
    (gen_random_uuid(), 'exercise', 'read', 'View exercises'),
    (gen_random_uuid(), 'routine', 'manage', 'Full routine management'),
    (gen_random_uuid(), 'routine', 'read', 'View routines'),
    (gen_random_uuid(), 'routine', 'create', 'Create routines'),
    (gen_random_uuid(), 'routine', 'update_own', 'Update own routines'),
    (gen_random_uuid(), 'session', 'read', 'View sessions'),
    (gen_random_uuid(), 'session', 'create', 'Create sessions'),
    (gen_random_uuid(), 'booking', 'manage', 'Full booking management'),
    (gen_random_uuid(), 'booking', 'read', 'View bookings'),
    (gen_random_uuid(), 'booking', 'create', 'Create bookings'),
    (gen_random_uuid(), 'booking', 'cancel_own', 'Cancel own bookings'),
    (gen_random_uuid(), 'trainer', 'manage', 'Full trainer management'),
    (gen_random_uuid(), 'trainer', 'read', 'View trainer profiles'),
    (gen_random_uuid(), 'trainer', 'update_own', 'Update own trainer profile'),
    (gen_random_uuid(), 'subscription', 'manage', 'Full subscription management'),
    (gen_random_uuid(), 'subscription', 'read_own', 'View own subscription'),
    (gen_random_uuid(), 'payment', 'manage', 'Full payment management'),
    (gen_random_uuid(), 'payment', 'read_own', 'View own payment history'),
    (gen_random_uuid(), 'supplement', 'manage', 'Full supplement management'),
    (gen_random_uuid(), 'supplement', 'read', 'View supplements'),
    (gen_random_uuid(), 'supplement', 'order', 'Place supplement orders'),
    (gen_random_uuid(), 'analytics', 'read', 'View analytics dashboards'),
    (gen_random_uuid(), 'analytics', 'read_own', 'View own analytics'),
    (gen_random_uuid(), 'notification', 'manage', 'Full notification management'),
    (gen_random_uuid(), 'notification', 'read_own', 'View own notifications'),
    (gen_random_uuid(), 'role', 'manage', 'Manage roles and permissions');

-- We can assign the permissions to roles using a DO block to do dynamic lookup
DO $$ 
DECLARE
    role_super_admin UUID := '00000000-0000-0000-0000-000000000001';
BEGIN
    -- SUPER_ADMIN gets all permissions
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT role_super_admin, id FROM permissions;
END $$;
