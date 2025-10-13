SELECT u.username, r.name as role_name 
FROM users u 
JOIN users_roles ur ON u.id = ur.user_id 
JOIN roles r ON ur.role_id = r.id 
WHERE u.username = 'admin';
