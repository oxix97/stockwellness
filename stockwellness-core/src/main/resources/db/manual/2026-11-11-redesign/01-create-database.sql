-- Run outside a transaction as a DBA after creating the least-privilege role.
CREATE DATABASE stockwellness_app OWNER stockwellness_app;
REVOKE ALL ON DATABASE stockwellness_app FROM PUBLIC;
GRANT CONNECT ON DATABASE stockwellness_app TO stockwellness_app;
