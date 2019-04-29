CREATE DATABASE "voila-system";
CREATE DATABASE "voila-tenant";

CREATE USER "voila-system" WITH PASSWORD 'pgpassword';
CREATE USER "voila-tenant" WITH PASSWORD 'pgpassword';

GRANT ALL PRIVILEGES ON DATABASE "voila-system" TO "voila-system";
GRANT ALL PRIVILEGES ON DATABASE "voila-tenant" TO "voila-tenant";
