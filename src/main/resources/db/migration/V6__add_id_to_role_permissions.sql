ALTER TABLE role_permissions DROP CONSTRAINT role_permissions_pkey;
ALTER TABLE role_permissions ADD COLUMN id UUID PRIMARY KEY DEFAULT gen_random_uuid();
ALTER TABLE role_permissions ADD CONSTRAINT role_permissions_role_id_permission_id_key UNIQUE (role_id, permission_id);
