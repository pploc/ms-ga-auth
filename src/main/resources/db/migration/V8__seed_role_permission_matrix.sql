-- Seeds the four remaining system roles from the permission matrix in
-- plans/standards/security.md §5. Until now only SUPER_ADMIN carried permissions, so
-- GET /auth/users/{id}/roles/with-permissions answered empty arrays for everybody else and a
-- freshly provisioned platform could not authorize anyone but the super admin.

-- GYM_ADMIN reads roles and permissions but does not manage them (matrix row
-- "Roles / Permissions": CRUD for SUPER_ADMIN, Read for GYM_ADMIN), so the read action is
-- introduced here.
INSERT INTO permissions (id, resource, action, description) VALUES
    (gen_random_uuid(), 'role', 'read', 'View roles and permissions');

-- SUPER_ADMIN holds every permission, including the one just added.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'role' AND p.action = 'read'
WHERE r.name = 'SUPER_ADMIN';

-- GYM_ADMIN: full management of the gym's own domain, read-only on RBAC.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (p.resource, p.action) IN (
    ('user', 'manage'),
    ('customer', 'manage'),
    ('exercise', 'manage'),
    ('routine', 'manage'),
    ('booking', 'manage'),
    ('trainer', 'manage'),
    ('subscription', 'manage'),
    ('payment', 'manage'),
    ('supplement', 'manage'),
    ('analytics', 'read'),
    ('notification', 'manage'),
    ('role', 'read')
)
WHERE r.name = 'GYM_ADMIN';

-- TRAINER: read the material they coach with, manage only what is their own.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (p.resource, p.action) IN (
    ('customer', 'read'),
    ('exercise', 'read'),
    ('routine', 'read'),
    ('session', 'read'),
    ('booking', 'read'),
    ('trainer', 'update_own'),
    ('analytics', 'read_own'),
    ('notification', 'read_own')
)
WHERE r.name = 'TRAINER';

-- MEMBER: self-service — bookings, workouts, own subscription and payments.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (p.resource, p.action) IN (
    ('customer', 'read_own'),
    ('exercise', 'read'),
    ('routine', 'read'),
    ('routine', 'create'),
    ('routine', 'update_own'),
    ('session', 'read'),
    ('session', 'create'),
    ('booking', 'read'),
    ('booking', 'create'),
    ('booking', 'cancel_own'),
    ('trainer', 'read'),
    ('subscription', 'read_own'),
    ('payment', 'read_own'),
    ('supplement', 'read'),
    ('supplement', 'order'),
    ('analytics', 'read_own'),
    ('notification', 'read_own')
)
WHERE r.name = 'MEMBER';

-- STAFF: front desk — look things up, change nothing structural.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (p.resource, p.action) IN (
    ('customer', 'read'),
    ('exercise', 'read'),
    ('booking', 'read'),
    ('trainer', 'read'),
    ('subscription', 'read_own'),
    ('supplement', 'read'),
    ('notification', 'read_own')
)
WHERE r.name = 'STAFF';
