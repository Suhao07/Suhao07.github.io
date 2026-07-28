-- Apply after blog_mysql8.sql. This removes FishBlog demo content while
-- preserving roles, menus, resources, and a local administrator login.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM tb_article_tag;
DELETE FROM tb_comment;
DELETE FROM tb_article;
DELETE FROM tb_category;
DELETE FROM tb_tag;
DELETE FROM tb_message;
DELETE FROM tb_notice;
DELETE FROM tb_friend_link;
DELETE FROM tb_favorite_tag;
DELETE FROM tb_favorite;
DELETE FROM tb_favorite_category;
DELETE FROM tb_unique_view;
DELETE FROM tb_operation_log;
DELETE FROM tb_chat_record;

DELETE FROM tb_user_role WHERE user_id NOT IN (1, 2);
DELETE FROM tb_user_auth WHERE user_info_id NOT IN (1, 2);
DELETE FROM tb_user_info WHERE id NOT IN (1, 2);

UPDATE tb_user_info
SET email = 'owner@localhost',
    phone = NULL,
    nickname = 'RickSu',
    avatar = '/legacy-assets/static/images/logo.png',
    intro = '记录技术、研究与实践',
    web_site = 'https://suhao07.github.io/',
    update_time = NOW(),
    is_disable = 0
WHERE id = 1;

UPDATE tb_user_auth
SET username = 'owner@localhost',
    ip_addr = NULL,
    ip_source = NULL,
    last_login_time = NULL
WHERE user_info_id = 1;

UPDATE tb_user_info
SET email = 'admin@localhost',
    phone = NULL,
    nickname = 'Administrator',
    avatar = '/legacy-assets/static/images/logo.png',
    intro = '本地管理账户',
    web_site = 'https://suhao07.github.io/',
    update_time = NOW(),
    is_disable = 0
WHERE id = 2;

UPDATE tb_user_auth
SET username = 'admin@localhost',
    password = '$2a$10$fXTS/xe.56PjifLmWys7lepVkOnhLyMZ/Xzxb8wIOCvsPnW5MBYCq',
    ip_addr = NULL,
    ip_source = NULL,
    last_login_time = NULL
WHERE user_info_id = 2;

INSERT INTO tb_notice (id, content, create_time, update_time)
VALUES (1, '### 欢迎来到 RickSu 的技术博客', NOW(), NULL)
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  update_time = NOW();

INSERT INTO tb_friend_link
  (id, link_name, link_avatar, link_address, link_intro, create_time)
VALUES
  (100, 'GitHub', '/legacy-assets/static/images/logo.png',
   'https://github.com/Suhao07', 'RickSu 的代码与项目', NOW())
ON DUPLICATE KEY UPDATE
  link_name = VALUES(link_name),
  link_avatar = VALUES(link_avatar),
  link_address = VALUES(link_address),
  link_intro = VALUES(link_intro);

SET FOREIGN_KEY_CHECKS = 1;
