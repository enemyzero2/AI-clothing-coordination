 -- AI衣搭应用数据库定义脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_outfit_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE ai_outfit_app;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` VARCHAR(36) PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password_hash` VARCHAR(128) NOT NULL,
    `avatar_uri` VARCHAR(255),
    `display_name` VARCHAR(100),
    `bio` TEXT,
    `registration_date` BIGINT NOT NULL,
    `last_login_date` BIGINT,
    `token` VARCHAR(255),
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_username (`username`),
    INDEX idx_email (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户身体数据表
CREATE TABLE IF NOT EXISTS `user_body` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `height` FLOAT, -- 厘米
    `weight` FLOAT, -- 千克
    `gender` VARCHAR(10),
    `shoulder_width` FLOAT,
    `chest` FLOAT,
    `waist` FLOAT,
    `hip` FLOAT,
    `inseam` FLOAT,
    `arm_length` FLOAT,
    `last_updated` BIGINT NOT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户风格表
CREATE TABLE IF NOT EXISTS `user_style` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `style_name` VARCHAR(50) NOT NULL,
    `preference_level` INT NOT NULL DEFAULT 3, -- 1-5
    `date_added` BIGINT NOT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `user_style_unique` (`user_id`, `style_name`),
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 服装表
CREATE TABLE IF NOT EXISTS `clothing` (
    `id` VARCHAR(36) PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(100),
    `brand` VARCHAR(100),
    `type` VARCHAR(20) NOT NULL, -- 上装, 下装, 外套等
    `colors` JSON, -- 颜色列表
    `seasons` JSON, -- 适用季节
    `tags` JSON, -- 标签
    `image_uri` VARCHAR(255),
    `notes` TEXT,
    `date_added` BIGINT NOT NULL,
    `favorite_level` INT NOT NULL DEFAULT 3, -- 1-5
    `is_favorite` BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX idx_user_id (`user_id`),
    INDEX idx_type (`type`),
    INDEX idx_is_favorite (`is_favorite`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 搭配方案表
CREATE TABLE IF NOT EXISTS `outfit` (
    `id` VARCHAR(36) PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(100),
    `description` TEXT,
    `date_created` BIGINT NOT NULL,
    `last_modified` BIGINT NOT NULL,
    `occasion` VARCHAR(50), -- 场合：正式、休闲、运动等
    `season` VARCHAR(20), -- 季节
    `style` VARCHAR(50), -- 风格
    `image_uri` VARCHAR(255), -- 搭配效果图
    `is_favorite` BOOLEAN NOT NULL DEFAULT FALSE,
    `rating` INT, -- 1-5星评分
    `is_ai_generated` BOOLEAN NOT NULL DEFAULT FALSE, -- 是否由AI生成
    `weather_condition` VARCHAR(50), -- 天气条件
    `temperature_range` VARCHAR(30), -- 温度范围
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX idx_user_id (`user_id`),
    INDEX idx_occasion (`occasion`),
    INDEX idx_season (`season`),
    INDEX idx_is_favorite (`is_favorite`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 搭配方案项目表
CREATE TABLE IF NOT EXISTS `outfit_item` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `outfit_id` VARCHAR(36) NOT NULL,
    `clothing_id` VARCHAR(36) NOT NULL,
    `position` INT NOT NULL DEFAULT 0, -- 在搭配中的位置/顺序
    `layering_level` INT NOT NULL DEFAULT 1, -- 层次级别
    `comments` TEXT,
    FOREIGN KEY (`outfit_id`) REFERENCES `outfit`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`clothing_id`) REFERENCES `clothing`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `outfit_clothing_unique` (`outfit_id`, `clothing_id`),
    INDEX idx_outfit_id (`outfit_id`),
    INDEX idx_clothing_id (`clothing_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 社区分享表
CREATE TABLE IF NOT EXISTS `shared_outfit` (
    `id` VARCHAR(36) PRIMARY KEY,
    `outfit_id` VARCHAR(36) NOT NULL,
    `user_id` VARCHAR(36) NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `content` TEXT,
    `shared_date` BIGINT NOT NULL,
    `likes_count` INT NOT NULL DEFAULT 0,
    `comments_count` INT NOT NULL DEFAULT 0,
    `is_public` BOOLEAN NOT NULL DEFAULT TRUE,
    `tags` JSON,
    FOREIGN KEY (`outfit_id`) REFERENCES `outfit`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX idx_user_id (`user_id`),
    INDEX idx_shared_date (`shared_date`),
    INDEX idx_likes_count (`likes_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `shared_outfit_id` VARCHAR(36) NOT NULL,
    `user_id` VARCHAR(36) NOT NULL,
    `content` TEXT NOT NULL,
    `comment_date` BIGINT NOT NULL,
    `parent_comment_id` INT, -- 回复的评论ID
    FOREIGN KEY (`shared_outfit_id`) REFERENCES `shared_outfit`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`parent_comment_id`) REFERENCES `comment`(`id`) ON DELETE SET NULL,
    INDEX idx_shared_outfit_id (`shared_outfit_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_comment_date (`comment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 点赞表
CREATE TABLE IF NOT EXISTS `like` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `shared_outfit_id` VARCHAR(36) NOT NULL,
    `like_date` BIGINT NOT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`shared_outfit_id`) REFERENCES `shared_outfit`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `user_outfit_like_unique` (`user_id`, `shared_outfit_id`),
    INDEX idx_shared_outfit_id (`shared_outfit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 关注表
CREATE TABLE IF NOT EXISTS `follow` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `follower_id` VARCHAR(36) NOT NULL, -- 关注者
    `followed_id` VARCHAR(36) NOT NULL, -- 被关注者
    `follow_date` BIGINT NOT NULL,
    FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`followed_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `follower_followed_unique` (`follower_id`, `followed_id`),
    INDEX idx_follower_id (`follower_id`),
    INDEX idx_followed_id (`followed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 私信表
CREATE TABLE IF NOT EXISTS `message` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `sender_id` VARCHAR(36) NOT NULL,
    `receiver_id` VARCHAR(36) NOT NULL,
    `content` TEXT NOT NULL,
    `send_date` BIGINT NOT NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`receiver_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX idx_sender_id (`sender_id`),
    INDEX idx_receiver_id (`receiver_id`),
    INDEX idx_send_date (`send_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `type` VARCHAR(20) NOT NULL, -- 通知类型：点赞、评论、关注等
    `related_id` VARCHAR(36), -- 相关ID
    `content` TEXT NOT NULL,
    `create_date` BIGINT NOT NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX idx_user_id (`user_id`),
    INDEX idx_create_date (`create_date`),
    INDEX idx_is_read (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 穿着记录表
CREATE TABLE IF NOT EXISTS `wear_log` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `outfit_id` VARCHAR(36) NOT NULL,
    `wear_date` BIGINT NOT NULL,
    `weather` VARCHAR(50),
    `temperature` FLOAT,
    `occasion` VARCHAR(50),
    `notes` TEXT,
    `rating` INT, -- 穿着体验评分
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`outfit_id`) REFERENCES `outfit`(`id`) ON DELETE CASCADE,
    INDEX idx_user_id (`user_id`),
    INDEX idx_outfit_id (`outfit_id`),
    INDEX idx_wear_date (`wear_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 示例DML插入语句

-- 插入用户数据
INSERT INTO `user` (
    `id`, `username`, `email`, `password_hash`, `avatar_uri`, 
    `display_name`, `bio`, `registration_date`, `last_login_date`, 
    `token`, `is_active`
) VALUES (
    'u1', 'xiaoming', 'xiaoming@example.com', 'hashed_password_here', 'avatars/xiaoming.jpg',
    '小明', '热爱时尚的科技爱好者', 1621234567890, 1631234567890,
    'token123', TRUE
), (
    'u2', 'xiaohua', 'xiaohua@example.com', 'hashed_password_here', 'avatars/xiaohua.jpg',
    '小华', '每天都要穿得美美的！', 1622345678901, 1632345678901,
    'token456', TRUE
);

-- 插入用户身体数据
INSERT INTO `user_body` (
    `user_id`, `height`, `weight`, `gender`, `shoulder_width`,
    `chest`, `waist`, `hip`, `inseam`, `arm_length`, `last_updated`
) VALUES (
    'u1', 175.5, 70.2, '男', 42.3,
    98.5, 82.3, 95.1, 80.2, 60.3, 1631234560000
), (
    'u2', 165.0, 52.5, '女', 38.1,
    88.2, 65.8, 92.3, 75.5, 55.2, 1632345670000
);

-- 插入用户风格数据
INSERT INTO `user_style` (
    `user_id`, `style_name`, `preference_level`, `date_added`
) VALUES 
    ('u1', '商务休闲', 5, 1631234560000),
    ('u1', '运动风', 4, 1631234560000),
    ('u1', '街头潮流', 3, 1631234560000),
    ('u2', '优雅简约', 5, 1632345670000),
    ('u2', '田园风', 4, 1632345670000),
    ('u2', '波西米亚', 3, 1632345670000);

-- 插入服装数据
INSERT INTO `clothing` (
    `id`, `user_id`, `name`, `brand`, `type`, `colors`, `seasons`, `tags`,
    `image_uri`, `notes`, `date_added`, `favorite_level`, `is_favorite`
) VALUES (
    'c1', 'u1', '白色衬衫', 'UNIQLO', 'TOP', '["白色"]', '["SPRING", "SUMMER", "AUTUMN"]', '["正式", "百搭"]',
    'clothes/white_shirt.jpg', '质量很好的基础款', 1631234560000, 5, TRUE
), (
    'c2', 'u1', '黑色西裤', 'ZARA', 'BOTTOM', '["黑色"]', '["SPRING", "AUTUMN", "WINTER"]', '["正式", "商务"]',
    'clothes/black_pants.jpg', '合身的剪裁', 1631234570000, 4, FALSE
), (
    'c3', 'u1', '蓝色牛仔外套', 'Levis', 'OUTERWEAR', '["蓝色"]', '["SPRING", "AUTUMN"]', '["休闲", "百搭"]',
    'clothes/denim_jacket.jpg', '经典款式', 1631234580000, 5, TRUE
), (
    'c4', 'u2', '红色连衣裙', 'H&M', 'DRESS', '["红色"]', '["SUMMER"]', '["约会", "优雅"]',
    'clothes/red_dress.jpg', '非常适合约会', 1632345680000, 5, TRUE
), (
    'c5', 'u2', '白色T恤', 'UNIQLO', 'TOP', '["白色"]', '["SPRING", "SUMMER"]', '["基础", "百搭"]',
    'clothes/white_tshirt.jpg', '基础款必备', 1632345690000, 4, FALSE
), (
    'c6', 'u2', '黑色短靴', 'Dr. Martens', 'SHOES', '["黑色"]', '["AUTUMN", "WINTER"]', '["百搭", "耐穿"]',
    'clothes/black_boots.jpg', '经典款，很耐穿', 1632345700000, 5, TRUE
);

-- 插入搭配方案数据
INSERT INTO `outfit` (
    `id`, `user_id`, `name`, `description`, `date_created`, `last_modified`,
    `occasion`, `season`, `style`, `image_uri`, `is_favorite`, `rating`,
    `is_ai_generated`, `weather_condition`, `temperature_range`
) VALUES (
    'o1', 'u1', '日常商务搭配', '适合日常办公室工作的商务休闲搭配', 1631234590000, 1631234590000,
    '工作', 'SPRING', '商务休闲', 'outfits/business_casual.jpg', TRUE, 5,
    FALSE, '晴天', '15-25°C'
), (
    'o2', 'u1', '周末街拍搭配', '周末出街的休闲搭配', 1631234600000, 1631234600000,
    '休闲', 'AUTUMN', '街头潮流', 'outfits/street_style.jpg', FALSE, 4,
    TRUE, '晴天', '10-20°C'
), (
    'o3', 'u2', '约会优雅搭配', '适合约会的优雅搭配', 1632345710000, 1632345710000,
    '约会', 'SUMMER', '优雅简约', 'outfits/elegant_date.jpg', TRUE, 5,
    FALSE, '晴天', '20-30°C'
), (
    'o4', 'u2', '日常休闲搭配', '日常舒适的休闲搭配', 1632345720000, 1632345720000,
    '休闲', 'SPRING', '田园风', 'outfits/casual_daily.jpg', FALSE, 4,
    TRUE, '晴天', '15-25°C'
);

-- 插入搭配方案项目数据
INSERT INTO `outfit_item` (
    `outfit_id`, `clothing_id`, `position`, `layering_level`, `comments`
) VALUES 
    ('o1', 'c1', 1, 1, '打底衬衫'),
    ('o1', 'c2', 2, 1, '配套西裤'),
    ('o2', 'c5', 1, 1, '基础T恤'),
    ('o2', 'c3', 2, 2, '外搭牛仔外套'),
    ('o3', 'c4', 1, 1, '主体连衣裙'),
    ('o3', 'c6', 2, 1, '配套短靴'),
    ('o4', 'c5', 1, 1, '基础T恤'),
    ('o4', 'c6', 2, 1, '配套短靴');

-- 插入分享数据
INSERT INTO `shared_outfit` (
    `id`, `outfit_id`, `user_id`, `title`, `content`, 
    `shared_date`, `likes_count`, `comments_count`, `is_public`, `tags`
) VALUES (
    's1', 'o1', 'u1', '今日商务搭配分享', '今天去开会的搭配，感觉很得体，大家觉得怎么样？', 
    1631234610000, 15, 3, TRUE, '["商务", "正式", "日常"]'
), (
    's2', 'o3', 'u2', '约会搭配推荐', '昨天约会穿的这一身，男朋友超喜欢！分享给大家～', 
    1632345730000, 25, 5, TRUE, '["约会", "优雅", "连衣裙"]'
);

-- 插入评论数据
INSERT INTO `comment` (
    `shared_outfit_id`, `user_id`, `content`, `comment_date`, `parent_comment_id`
) VALUES 
    ('s1', 'u2', '搭配很棒，看起来很专业！', 1631234620000, NULL),
    ('s1', 'u1', '谢谢，我很喜欢这个搭配', 1631234630000, 1),
    ('s1', 'u2', '衬衫是什么牌子的？看起来质量不错', 1631234640000, NULL),
    ('s2', 'u1', '裙子颜色很适合你，真漂亮！', 1632345740000, NULL),
    ('s2', 'u2', '谢谢夸奖～', 1632345750000, 4);

-- 插入点赞数据
INSERT INTO `like` (
    `user_id`, `shared_outfit_id`, `like_date`
) VALUES 
    ('u2', 's1', 1631234650000),
    ('u1', 's2', 1632345760000);

-- 插入关注数据
INSERT INTO `follow` (
    `follower_id`, `followed_id`, `follow_date`
) VALUES 
    ('u1', 'u2', 1632345770000),
    ('u2', 'u1', 1631234660000);

-- 插入私信数据
INSERT INTO `message` (
    `sender_id`, `receiver_id`, `content`, `send_date`, `is_read`
) VALUES 
    ('u1', 'u2', '你好，我很喜欢你分享的搭配！', 1632345780000, TRUE),
    ('u2', 'u1', '谢谢，很高兴你喜欢！', 1632345790000, FALSE);

-- 插入通知数据
INSERT INTO `notification` (
    `user_id`, `type`, `related_id`, `content`, `create_date`, `is_read`
) VALUES 
    ('u1', 'LIKE', 's1', '小华赞了你的搭配', 1631234650000, TRUE),
    ('u2', 'COMMENT', 's2', '小明评论了你的搭配', 1632345740000, FALSE),
    ('u2', 'FOLLOW', 'u1', '小明关注了你', 1632345770000, TRUE);

-- 插入穿着记录数据
INSERT INTO `wear_log` (
    `user_id`, `outfit_id`, `wear_date`, `weather`, `temperature`,
    `occasion`, `notes`, `rating`
) VALUES 
    ('u1', 'o1', 1631234670000, '晴天', 22.5,
    '工作会议', '今天穿着很舒适，会议反馈很好', 5),
    ('u2', 'o3', 1632345800000, '晴天', 26.0,
    '餐厅约会', '穿着很合适，得到了很多赞美', 5);