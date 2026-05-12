-- Grant inventory_user permission to create and access company_* schemas
GRANT ALL PRIVILEGES ON `company_%`.* TO 'inventory_user'@'%';
GRANT CREATE ON *.* TO 'inventory_user'@'%';
FLUSH PRIVILEGES;
