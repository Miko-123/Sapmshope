
INSERT INTO users (
    email, 
    username, 
    password, 
    first_name, 
    middle_name, 
    last_name, 
    gender, 
    is_deleted, 
    created_at, 
    updated_at
) VALUES 
('head.accounting@hopes.edu', 'head_accounting', '$2y$10$VDwx04ZTTuWM9U.WHItPAeD.Blo/CD.PQ97Rgj8nBKGpxQQ79J4Bu', 'Dawit', 'Kebede', 'Yilma', 'MALE', FALSE, NOW(), NOW()),
('head.management@hopes.edu', 'head_management', '$2y$10$VDwx04ZTTuWM9U.WHItPAeD.Blo/CD.PQ97Rgj8nBKGpxQQ79J4Bu', 'Hanna', 'Girma', 'Tadesse', 'FEMALE', FALSE, NOW(), NOW()),
('head.marketing@hopes.edu', 'head_marketing', '$2y$10$VDwx04ZTTuWM9U.WHItPAeD.Blo/CD.PQ97Rgj8nBKGpxQQ79J4Bu', 'Robel', 'Mulugeta', 'Assefa', 'MALE', FALSE, NOW(), NOW()),
('head.is@hopes.edu', 'head_is', '$2y$10$VDwx04ZTTuWM9U.WHItPAeD.Blo/CD.PQ97Rgj8nBKGpxQQ79J4Bu', 'Bethlehem', 'Tewodros', 'Alemu', 'FEMALE', FALSE, NOW(), NOW()),
('head.it@hopes.edu', 'head_it', '$2y$10$VDwx04ZTTuWM9U.WHItPAeD.Blo/CD.PQ97Rgj8nBKGpxQQ79J4Bu', 'Samuel', 'Yohannes', 'Girma', 'MALE', FALSE, NOW(), NOW()),
('head.cs@hopes.edu', 'head_cs', '$2y$10$VDwx04ZTTuWM9U.WHItPAeD.Blo/CD.PQ97Rgj8nBKGpxQQ79J4Bu', 'Solomon', 'Haile', 'Kebede', 'MALE', FALSE, NOW(), NOW()),
('head.architecture@hopes.edu', 'head_architecture', '$2y$10$VDwx04ZTTuWM9U.WHItPAeD.Blo/CD.PQ97Rgj8nBKGpxQQ79J4Bu', 'Rahel', 'Solomon', 'Bekele', 'FEMALE', FALSE, NOW(), NOW());

INSERT INTO users_roles (user_id, role_id) 
SELECT id, 4 FROM users WHERE email IN (
    'head.accounting@hopes.edu',
    'head.management@hopes.edu',
    'head.marketing@hopes.edu',
    'head.is@hopes.edu',
    'head.it@hopes.edu',
    'head.cs@hopes.edu',
    'head.architecture@hopes.edu'
);