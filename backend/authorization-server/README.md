# Authorization-Server
一个基于Spring-Authorization-Server、MySQL、Redis、Pulsar的授权服务器。

## 1. 开始使用
### 1.1 依赖中间件
#### 必须中间件
* JDK 21
* MySql 8.0.36
* Redis 7.2.0-v9
* Pulsar 3.2.1
#### 可选中间件
* Docker Compose
* Elasticsearch(v8.12.1) + IK分词器插件

### 1.2 模块说明
* security-spring-boot-autoconfigure  
提供用户、角色、Spring的Security认证与授权功能。

* authorization-server-spring-boot-autoconfigure  
在security-spring-boot-autoconfigure的基础上，补充提供OAuth2授权服务器的特性。

* authorization-server-app  
完整的可执行的OAuth2授权服务器应用

* authorization-client-app  
完整的可执行的OAuth2资源服务器应用

### 1.3 mysql初始化数据
#### DDL
##### Spring Security认证与授权相关
```mysql
CREATE TABLE `users` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '物理主键',
    `username` VARCHAR(128) NOT NULL COMMENT '账号',
    `nickname` VARCHAR(255) NOT NULL COMMENT '昵称',
    `password` VARCHAR(128) NOT NULL COMMENT '密码',
    `enabled` BIT(1) NOT NULL COMMENT '是否启用',
    `account_expired` BIT(1) NOT NULL COMMENT '账号是否已过期',
    `account_locked` BIT(1) NOT NULL COMMENT '账号是否已被锁',
    `credentials_expired` BIT(1) NOT NULL COMMENT '密码是否已过期',
    `oidc_registration_id` varchar(255) DEFAULT NULL COMMENT 'OAuth2授权服务器的在本应用内的OIDC注册ID',
    `open_id` varchar(128) DEFAULT NULL COMMENT '用户在OAuth2授权服务器中的开放账号',
    `expired_sessions_clean_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '清理失效会话集时间',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY `uk_username`(`username`),
    KEY `idx_oidcRegistrationId_openId` (`oidc_registration_id`,`open_id`),
    KEY `idx_expiredSessionsCleanTime` (`expired_sessions_clean_time`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT '用户表';

CREATE TABLE `authorities` (
   `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '物理主键',
   `username` varchar(128) NOT NULL COMMENT '账号，关联users.username',
   `authority` varchar(128) NOT NULL COMMENT '权限项编码值，关联authority_item.code',
   `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
   `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
   UNIQUE KEY `ix_auth_username` (`username`,`authority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户权限配置表';

CREATE TABLE `resource_group` (
    `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '物理主键',
    `code` VARCHAR(128) NOT NULL COMMENT '编码值',
    `name` VARCHAR(255) NOT NULL COMMENT '名称',
    `type` INT NOT NULL COMMENT '类型（1: 菜单, 2: 功能）',
    `seq` BIGINT(20) NOT NULL DEFAULT 1000 COMMENT '顺序，数值越小越靠前',
    `parent_group_code` VARCHAR(128) NOT NULL COMMENT '上一层级分组的编码值，关联resource_group.code',
    `config` VARCHAR(1024) DEFAULT NULL COMMENT 'JSON配置',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_type` (`type`),
    KEY `idx_parentGroupCode` (`parent_group_code`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT '资源分组配置表';

CREATE TABLE `resource_item` (
    `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '物理主键',
    `code` VARCHAR(128) NOT NULL COMMENT '编码值',
    `http_method_list` VARCHAR(255) NOT NULL COMMENT 'http请求方法类型',
    `pattern` VARCHAR(255) NOT NULL COMMENT '路径模式',
    `name` VARCHAR(255) NOT NULL COMMENT '名称',
    `seq` BIGINT(20) NOT NULL DEFAULT 1000 COMMENT '顺序，数值越小越靠前',
    `parent_group_code` VARCHAR(128) NOT NULL COMMENT '所在资源分组的编码值，关联resource_group.code',
    `config` VARCHAR(1024) DEFAULT NULL COMMENT 'JSON配置',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY `uk_code` (`code`),
    UNIQUE KEY `uk_pattern` (`pattern`),
    KEY `idx_parentGroupCode` (`parent_group_code`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT '资源项配置表';

CREATE TABLE `authority_item` (
    `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '物理主键',
    `code` VARCHAR(128) NOT NULL COMMENT '编码值',
    `name` VARCHAR(255) NOT NULL COMMENT '名称',
    `parent_code` VARCHAR(128) NULL COMMENT '上一层级权限的编码值，关联authority_item.code',
    `priority` INT NOT NULL DEFAULT 1000 COMMENT '优先级，数值越小，优先级越高',
    `system_authority` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为系统权限（即无法修改）。0-否，1-是',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_parentCode` (`parent_code`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='权限项配置表';

CREATE TABLE `authority_resource` (
    `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '物理主键',
    `resource_item_code` VARCHAR(128) NOT NULL COMMENT '资源项编码，关联resource_item.code',
    `authority_item_code` VARCHAR(128) NOT NULL COMMENT '权限项编码，关联authority_item.code',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY `idx_resourceItemCode_authorityItemCode` (`resource_item_code`,`authority_item_code`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源项关联表';
```

##### OAuth2授权服务器相关
```mysql
CREATE TABLE `oauth2_registered_client` (
    `id` varchar(255) NOT NULL,
    `client_id` varchar(255) NOT NULL,
    `client_id_issued_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `client_secret` varchar(255) DEFAULT NULL,
    `client_secret_expires_at` datetime(3) DEFAULT NULL,
    `client_name` varchar(255) NOT NULL,
    `client_authentication_methods` varchar(1000) NOT NULL,
    `authorization_grant_types` varchar(1000) NOT NULL,
    `redirect_uris` varchar(1000) DEFAULT NULL,
    `post_logout_redirect_uris` varchar(1000) DEFAULT NULL,
    `scopes` varchar(1000) NOT NULL,
    `client_settings` varchar(2000) NOT NULL,
    `token_settings` varchar(2000) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `oauth2_authorization` (
    `id` varchar(255) NOT NULL,
    `registered_client_id` varchar(255) NOT NULL,
    `principal_name` varchar(255) NOT NULL,
    `authorization_grant_type` varchar(255) NOT NULL,
    `authorized_scopes` varchar(1000) DEFAULT NULL,
    `attributes` blob DEFAULT NULL,
    `state` varchar(500) DEFAULT NULL,
    `authorization_code_value` blob DEFAULT NULL,
    `authorization_code_issued_at` datetime(3) DEFAULT NULL,
    `authorization_code_expires_at` datetime(3) DEFAULT NULL,
    `authorization_code_metadata` blob DEFAULT NULL,
    `access_token_value` blob DEFAULT NULL,
    `access_token_issued_at` datetime(3) DEFAULT NULL,
    `access_token_expires_at` datetime(3) DEFAULT NULL,
    `access_token_metadata` blob DEFAULT NULL,
    `access_token_type` varchar(100) DEFAULT NULL,
    `access_token_scopes` varchar(1000) DEFAULT NULL,
    `oidc_id_token_value` blob DEFAULT NULL,
    `oidc_id_token_issued_at` datetime(3) DEFAULT NULL,
    `oidc_id_token_expires_at` datetime(3) DEFAULT NULL,
    `oidc_id_token_metadata` blob DEFAULT NULL,
    `refresh_token_value` blob DEFAULT NULL,
    `refresh_token_issued_at` datetime(3) DEFAULT NULL,
    `refresh_token_expires_at` datetime(3) DEFAULT NULL,
    `refresh_token_metadata` blob DEFAULT NULL,
    `user_code_value` blob DEFAULT NULL,
    `user_code_issued_at` datetime(3) DEFAULT NULL,
    `user_code_expires_at` datetime(3) DEFAULT NULL,
    `user_code_metadata` blob DEFAULT NULL,
    `device_code_value` blob DEFAULT NULL,
    `device_code_issued_at` datetime(3) DEFAULT NULL,
    `device_code_expires_at` datetime(3) DEFAULT NULL,
    `device_code_metadata` blob DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `oauth2_authorization_consent` (
    `registered_client_id` varchar(255) NOT NULL,
    `principal_name` varchar(255) NOT NULL,
    `authorities` varchar(1000) NOT NULL,
    PRIMARY KEY (`registered_client_id`,`principal_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

##### OAuth2客户端相关
```mysql
# 客户端用户获得授权后的令牌记录表（非必须）
CREATE TABLE oauth2_authorized_client (
    client_registration_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    access_token_type VARCHAR(100) NOT NULL,
    access_token_value BLOB NOT NULL,
    access_token_issued_at DATETIME(3) NOT NULL,
    access_token_expires_at DATETIME(3) NOT NULL,
    access_token_scopes VARCHAR(1000) DEFAULT NULL,
    refresh_token_value BLOB DEFAULT NULL,
    refresh_token_issued_at DATETIME(3) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (client_registration_id, principal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '客户端用户获得授权后的令牌记录表';
```

> OAuth2的相关表字段和DAO相关定义，可参考：  
> https://docs.spring.io/spring-authorization-server/docs/current/reference/html/guides/how-to-jpa.html#registered-client-repository

#### DML
##### 初始化用户数据
```mysql
# 初始化账号密码：
# admin：admin
# user：password
# 其他用户：123456
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('005f1963cea242d7bfff02ac015d99c2','孙悟空','{bcrypt}$2a$10$BNXc26ePiWL8IU5jbSBtEeXbWxbPZDN4HG0bc0KH5uvMPuxr48F4u','','','','',null,null,'2023-10-13 15:03:31.000','2023-10-17 16:27:47.000');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('46519d99796b44c8a118ed4bda76e932','牛魔','{bcrypt}$2a$10$JrhJqcNCFMqAi0xNhcremeP.VCX/AcDU9eNXzC6zf786ATAMvb7qK','','','','',null,null,'2023-10-13 15:00:58.000','2023-10-13 15:00:58.000');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('4c48613ca16d4c059c338c72d5f5c50f','虞姬','{bcrypt}$2a$10$.brmthUo6puO4yVvmo2U5.l3jmy5qSrS.70Yq07/tCDU8oCixXzb6','','','','',null,null,'2023-10-13 15:02:30.000','2023-10-13 15:02:30.000');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('6713043017134daa84a4fe3bc48273b0','王昭君','{bcrypt}$2a$10$YihEcfnG8tZuYwA7Lf2TDeUcyfzq7sO0qBc.0Il2qoADNhuTK3WMa','','','','',null,null,'2023-10-13 15:01:21.000','2023-10-13 15:01:21.000');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('8bfe071ac9f0435db23c3d776d5056a7','亚瑟','{bcrypt}$2a$10$4DUfBhn9W6URMo9b1/0yV.WEwhSrDjxn/xn2cfVBWmxK5luv.PMx6','','','','',null,null,'2023-10-13 15:01:38.000','2023-10-13 15:01:38.000');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('a2c96299b8a14d22a3a5a1b8b57d4688','孙策','{bcrypt}$2a$10$Opj/t1yOySxH0xUXjEnEeuUW5d2LOstslWpvYBj5zdqtxXxW/d3W.','','','','',null,null,'2023-10-13 15:03:21.000','2023-10-13 15:03:21.000');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('admin','系统管理员','{bcrypt}$2a$10$5YvVJO2gRQVuPVVDjOR4nupwXAvpL1WgVIbRXxI.m3D6TgvcpuJwy',b'1','','','','oidc-client','admin','2099-09-19 16:30:24.000','2099-09-19 16:30:24.000');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('b030502b03044a819150eae9b7764a0b','花木兰','{bcrypt}$2a$10$5kFPJ0L45vR.goSlwjWpqeqGlI5yyDHGi6KxU4JAv3veBgwQSzvOy','','','','',null,null,'2023-10-13 15:00:17.000','2023-10-13 15:00:17.000');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('bizadmin','B端系统管理员A','{bcrypt}$2a$10$Z6krsAwqjxdzNBTPywwequkYrPS2YPQwQJ6g95MFpkQNupxLi4MkK',b'1','','','',null,null,'2099-09-19 16:30:24.000','2023-10-12 19:24:02.796');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('cb714c1aa257442f93b319013d8d5753','阿轲','{bcrypt}$2a$10$lpDqpBb/lVk6SlahAJHbv.rlcm6jVVL5aZNk.ouP/cgNbEbjes9fC','','','','',null,null,'2023-10-13 15:00:35.000','2023-10-13 15:00:35.000');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('cliadmin','C端系统管理员A','{bcrypt}$2a$10$Z6krsAwqjxdzNBTPywwequkYrPS2YPQwQJ6g95MFpkQNupxLi4MkK',b'1','','','',null,null,'2099-09-19 16:30:24.000','2023-10-12 19:44:03.047');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('director','总监A','{bcrypt}$2a$10$1cpm5ojUVH.VoRl9/EfUK.s2eRYtnxmsUOh6KmP/EzagMKsOrnu2O',b'1','','','',null,null,'2099-09-19 16:30:24.000','2023-10-12 19:24:11.576');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('member','C端会员A','{bcrypt}$2a$10$Z6krsAwqjxdzNBTPywwequkYrPS2YPQwQJ6g95MFpkQNupxLi4MkK',b'1','','','',null,null,'2099-09-19 16:30:24.000','2023-10-12 19:24:00.015');
insert into `users` (`username`, `nickname`, `password`, `enabled`, `account_expired`, `account_locked`, `credentials_expired`, `oidc_registration_id`, `open_id`, `create_time`, `update_time`) values('user','普通用户A','{bcrypt}$2a$10$HijxNDm6TA7ZZ/ELSAnd7eAk4F5/HuHOxmtsNW0Mn5pXdOJex74SW',b'1','','','','oidc-client','user','2099-09-19 16:30:24.000','2023-10-12 19:24:07.366');
```
> 注：也可以通过执行此方法创建用户：com.walter.starry.authorizationserver.app.AuthorizationServerApplicationTests.UserDetailsServiceTest.createUser

##### 初始化角色和权限
```mysql
## 以下为系统固定角色
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('1','ROLE_ANONYMOUS','匿名用户',NULL,'1000',b'1','2023-09-19 16:13:05.363','2023-10-06 12:00:48.695');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('2','ROLE_ADMIN','系统管理员',NULL,'1000',b'1','2023-09-19 16:13:27.100','2023-10-12 17:56:09.903');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('3','ROLE_USER','已登录用户','ROLE_ADMIN','1000',b'1','2023-09-20 17:50:39.519','2023-10-06 12:01:55.805');

## 以下为业务角色
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('4','ROLE_BIZ_ADMIN','B端系统管理员','ROLE_ADMIN','1000','','2023-10-03 20:54:01.000','2023-10-17 09:44:42.000');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('5','ROLE_CLI_ADMIN','C端系统管理员','ROLE_ADMIN','1000','','2023-10-07 12:20:02.025','2023-10-12 17:56:27.549');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('6','ROLE_CLI_USER','C端系统已登录用户','ROLE_CLI_ADMIN','1000','','2023-10-07 12:21:04.328','2023-10-12 19:17:23.772');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('8','ROLE_CLI_MEMBER_2','C端中级会员','ROLE_CLI_MEMBER_3','1000','','2023-10-07 12:22:32.131','2023-10-07 12:23:28.280');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('9','ROLE_CLI_MEMBER_1','C端初级会员','ROLE_CLI_MEMBER_2','1000','','2023-10-07 12:23:31.686','2023-10-07 12:24:05.942');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('10','ROLE_BIZ_USER','B端系统已登录用户','ROLE_BIZ_ADMIN','1000','','2023-10-12 17:31:38.000','2023-10-17 09:51:48.000');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('11','ROLE_BIZ_DIRECTOR','总监','ROLE_BIZ_PRESIDENT','1000','','2023-10-12 17:33:11.295','2023-10-12 19:21:24.986');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('12','ROLE_BIZ_MANAGER','经理','ROLE_BIZ_DIRECTOR','1000','','2023-10-12 19:05:10.896','2023-10-12 19:21:20.282');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('13','ROLE_CLI_MEMBER_3','C端高级会员','ROLE_CLI_ADMIN','1000','','2023-10-12 19:05:55.283','2023-10-12 19:17:25.627');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('14','ROLE_BIZ_PRESIDENT','总裁','ROLE_BIZ_ADMIN','1000','','2023-10-12 19:19:55.130','2023-10-17 09:44:42.000');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('36','ROLE_BIZ_STAFF','职员','ROLE_BIZ_MANAGER','1000','','2023-10-17 06:22:59.000','2023-10-17 08:44:38.000');
insert into `authority_item` (`id`, `code`, `name`, `parent_code`, `priority`, `system_authority`, `create_time`, `update_time`) values('37','ROLE_BIZ_EC','外包','ROLE_BIZ_STAFF','1000','','2023-10-17 06:23:14.000','2023-10-17 08:44:38.000');
```
##### 层次角色说明
| 一级角色    | 二级角色     | 三级角色   | 四级角色  | 五级角色  |
|---------|----------|--------|-------|-------|
| 匿名用户  |         |        |       |       |
| 系统管理员|         |        |       |       |
|         | 已登录用户  |        |       |       |
|         | B端管理员  |        |       |       |
|         |          | B端已登录用户   |       |       |
|         |          | 总裁1   |       |       |
|         |          |        | 总监11 |       |
|         |          |        |       | 经理111 |
|         |          |        |       | 经理112 |
|         |          |        | 总监12 |       |
|         |          |        |       | 经理121 |
|         |          |        |       | 经理122 |
|         | C端管理员  |        |       |       |
|         |          | C端已登录用户 |       |       |
|         |          | 高级会员 |       |       |
|         |          |        | 中级会员 |       |
|         |          |        |       | 初级会员 |
> 层次角色：上级角色自动包含下级角色的全部权限。上面表格中的“匿名用户”“系统管理员”“已登录用户”为系统固定角色，其他为业务角色。业务角色可按需自行定义。

##### 初始化用户拥有的角色或权限
```mysql
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('1','admin','ROLE_ADMIN','2023-09-13 13:19:46.812','2023-09-19 16:15:38.605');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('2','user','ROLE_USER','2023-09-13 13:19:27.877','2023-09-19 16:15:41.826');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('3','bizadmin','ROLE_BIZ_ADMIN','2023-09-19 16:38:05.190','2023-10-17 09:44:42.000');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('4','bizadmin','ROLE_USER','2023-10-03 20:34:56.526','2023-10-12 19:39:55.347');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('5','member','ROLE_USER','2023-10-07 12:27:04.719','2023-10-07 16:15:59.134');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('7','member','ROLE_CLI_MEMBER_2','2023-10-07 12:25:48.913','2023-10-07 16:16:04.717');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('59','member','ROLE_CLI_MEMBER_1','2023-10-10 07:46:24.000','2023-10-10 07:46:24.000');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('69','director','ROLE_BIZ_DIRECTOR','2023-10-12 17:36:07.802','2023-10-12 17:36:07.802');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('70','director','ROLE_USER','2023-10-12 17:36:09.597','2023-10-12 17:36:13.157');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('71','member','ROLE_CLI_USER','2023-10-12 19:07:38.730','2023-10-12 19:07:38.730');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('72','director','ROLE_BIZ_USER','2023-10-12 19:07:52.355','2023-10-17 09:51:48.000');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('73','cliadmin','ROLE_USER','2023-10-12 19:41:35.176','2023-10-12 19:41:39.094');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('74','cliadmin','ROLE_CLI_ADMIN','2023-10-12 19:41:45.491','2023-10-12 19:41:54.021');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('75','b030502b03044a819150eae9b7764a0b','ROLE_USER','2023-10-13 15:00:17.000','2023-10-13 15:00:17.000');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('76','cb714c1aa257442f93b319013d8d5753','ROLE_USER','2023-10-13 15:00:35.000','2023-10-13 15:00:35.000');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('77','46519d99796b44c8a118ed4bda76e932','ROLE_USER','2023-10-13 15:00:58.000','2023-10-13 15:00:58.000');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('78','6713043017134daa84a4fe3bc48273b0','ROLE_USER','2023-10-13 15:01:21.000','2023-10-13 15:01:21.000');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('79','8bfe071ac9f0435db23c3d776d5056a7','ROLE_USER','2023-10-13 15:01:38.000','2023-10-13 15:01:38.000');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('80','4c48613ca16d4c059c338c72d5f5c50f','ROLE_USER','2023-10-13 15:02:30.000','2023-10-13 15:02:30.000');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('81','a2c96299b8a14d22a3a5a1b8b57d4688','ROLE_USER','2023-10-13 15:03:21.000','2023-10-13 15:03:21.000');
insert into `authorities` (`id`, `username`, `authority`, `create_time`, `update_time`) values('82','005f1963cea242d7bfff02ac015d99c2','ROLE_USER','2023-10-13 15:03:31.000','2023-10-13 15:03:31.000');
```

##### 初始化资源分组（包括菜单组和功能组）
```mysql
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('1','root_menu_group','根菜单组','1','1000','root_menu_group',NULL,'2023-09-19 11:45:09.333','2023-09-19 15:33:58.979');
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('2','system_admin_menu_group','系统管理','1','1000','root_menu_group','{\"defaultOpen\":true,\"icon\":\"Setting\"}','2023-09-19 14:56:26.000','2023-11-03 13:06:05.000');
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('3','root_function_group','根功能组','2','1000','root_function_group',NULL,'2023-09-19 15:33:52.499','2023-09-19 15:34:11.111');
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('4','system_admin_function_group','系统管理','2','1000','root_function_group','{\"icon\":\"Setting\"}','2023-09-19 15:36:02.000','2023-11-06 13:57:24.000');
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('5','menu_admin_function_group','菜单管理','2','1100','system_admin_function_group','{\"icon\":\"Menu\"}','2023-09-19 15:39:10.000','2023-11-07 08:05:02.000');
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('6','user_admin_function_group','用户管理','2','900','system_admin_function_group','{\"icon\":\"User\"}','2023-09-28 14:24:26.000','2023-11-06 14:00:17.000');
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('17','test_func_group','测试功能分组','2','1000','root_function_group','{\"icon\":\"QuestionFilled\"}','2023-11-06 09:20:15.000','2023-11-07 02:32:24.000');
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('20','role_admin_function_group','角色管理','2','1000','system_admin_function_group','{\"icon\":\"UserFilled\"}','2023-11-07 08:04:49.000','2023-11-07 08:04:49.000');
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('21','function_admin_function_group','功能管理','2','1200','system_admin_function_group','{\"icon\":\"Key\"}','2023-11-07 08:36:34.000','2023-11-07 08:36:43.000');
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('22','shop_admin_menu_group','电商管理','1','1100','root_menu_group','{\"defaultOpen\":false,\"icon\":\"Goods\"}','2024-12-23 16:29:54.200','2024-12-23 16:44:40.151');
insert into `resource_group` (`id`, `code`, `name`, `type`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('23','product_admin_menu_group','商品管理','1','1000','shop_admin_menu_group','{\"defaultOpen\":false,\"icon\":\"GoodsFilled\"}','2024-12-23 16:35:04.821','2024-12-23 16:35:04.821');
```

##### 初始化资源项（包括菜单项和功能项）
```mysql
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('1','AdminPage','GET','/admin','管理台首页','1000','root_menu_group','{\"icon\":\"House\"}','2023-09-19 00:04:45.000','2023-11-03 13:11:28.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('2','AdminUserPage','GET','/admin/user','用户管理','1000','system_admin_menu_group','{\"icon\":\"User\"}','2023-09-19 00:04:58.692','2023-09-23 10:58:30.569');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('3','AdminRolePage','GET','/admin/role','角色管理','1000','system_admin_menu_group','{\"icon\":\"UserFilled\"}','2023-09-19 14:36:54.447','2023-09-23 10:58:35.849');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('4','AdminMenuPage','GET','/admin/menu','菜单管理','1000','system_admin_menu_group','{\"icon\":\"Menu\"}','2023-09-19 15:22:11.708','2023-09-23 10:58:40.927');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('5','AdminFunctionPage','GET','/admin/function','功能管理','1000','system_admin_menu_group','{\"icon\":\"Key\"}','2023-09-19 15:26:21.000','2023-11-03 13:09:33.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('6','admin_menutree_load_for_all_user','GET','/admin/menu/loadForAllUser','菜单列表加载给所有用户','1000','menu_admin_function_group',NULL,'2023-09-19 15:40:45.884','2023-11-07 08:05:02.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('7','admin_menu_operation','POST','/admin/menu/**','菜单管理操作','1000','menu_admin_function_group',NULL,'2023-09-19 15:52:41.267','2023-11-07 08:05:02.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('10','root_function_test','GET,POST','/test/**','测试型操作','1000','test_func_group',NULL,'2023-09-21 02:38:46.000','2023-11-07 02:32:24.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('12','admin_user_authority_operation','GET,POST','/admin/user/authority/**','用户管理授权操作','900','user_admin_function_group',NULL,'2023-09-28 14:22:43.856','2023-11-06 14:00:17.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('16','admin_user_read_operation','GET,POST','/admin/user/list','用户管理读用户操作','900','user_admin_function_group',NULL,'2023-10-07 11:38:29.660','2023-11-06 14:00:17.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('17','admin_user_write_operation','GET,POST','/admin/user/**','用户管理写用户操作','1000','user_admin_function_group',NULL,'2023-10-07 11:42:04.548','2023-11-06 14:00:17.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('29','admin_role_operation','POST','/admin/role/**','角色管理操作','1000','role_admin_function_group','{\"icon\":null}','2023-11-07 08:28:47.000','2023-11-07 08:28:47.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('30','admin_function_operation','POST','/admin/function/**','功能管理操作','1000','function_admin_function_group','{\"icon\":null}','2023-11-07 08:39:36.000','2023-11-07 08:39:36.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('31','admin_function_has','POST','/admin/function/has','判断当前登录用户是否拥有指定的功能权限','900','function_admin_function_group','{\"icon\":null}','2023-11-09 06:20:39.000','2023-11-09 06:20:39.000');
insert into `resource_item` (`id`, `code`, `http_method_list`, `pattern`, `name`, `seq`, `parent_group_code`, `config`, `create_time`, `update_time`) values('32','AdminProductSpuPage','GET','/admin/product/spu','SPU管理','1000','product_admin_menu_group','{\"icon\":\"Goods\"}','2024-12-23 16:31:26.931','2024-12-23 16:35:13.602');
```

##### 初始化资源项所需的角色或权限
```mysql
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('1','AdminPage','ROLE_USER','2023-09-19 16:07:08.284','2023-09-23 16:42:09.900');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('2','AdminUserPage','ROLE_USER','2023-09-19 16:07:32.744','2023-10-07 11:37:12.213');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('3','AdminRolePage','ROLE_USER','2023-09-19 16:07:48.948','2023-10-11 18:41:36.786');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('4','AdminMenuPage','ROLE_ADMIN','2023-09-19 16:07:59.290','2023-09-23 10:59:20.843');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('5','AdminFunctionPage','ROLE_ADMIN','2023-09-19 16:08:07.882','2023-09-23 10:59:29.144');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('6','admin_menutree_load_for_all_user','ROLE_USER','2023-09-19 16:09:50.410','2023-10-18 10:07:24.884');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('7','admin_menu_operation','ROLE_ADMIN','2023-09-19 16:09:59.166','2023-09-23 11:34:12.978');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('10','admin_user_write_operation','ROLE_ADMIN','2023-09-28 14:25:24.491','2023-10-07 11:40:40.951');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('11','admin_user_authority_operation','ROLE_BIZ_USER','2023-10-06 22:11:21.012','2023-10-17 09:51:48.000');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('12','admin_user_authority_operation','ROLE_CLI_ADMIN','2023-10-07 11:40:43.210','2023-10-12 19:37:49.579');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('13','admin_user_read_operation','ROLE_USER','2023-10-12 19:37:10.762','2023-10-12 19:37:22.929');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('19','root_function_test','ROLE_ANONYMOUS','2023-11-07 07:46:32.000','2023-11-07 07:46:32.000');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('21','admin_role_operation','ROLE_USER','2023-11-07 08:32:06.000','2023-11-07 08:32:06.000');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('22','admin_function_operation','ROLE_ADMIN','2023-11-07 08:40:42.000','2023-11-07 08:40:42.000');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('23','admin_function_has','ROLE_ANONYMOUS','2023-11-09 06:26:26.000','2023-11-09 06:26:26.000');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('24','admin_function_has','ROLE_USER','2023-11-09 06:26:26.000','2023-11-09 06:26:26.000');
insert into `authority_resource` (`id`, `resource_item_code`, `authority_item_code`, `create_time`, `update_time`) values('25','AdminProductSpuPage','ROLE_BIZ_MANAGER','2024-12-23 16:32:18.345','2024-12-23 16:32:18.345');
```

##### 初始化OAuth2的Client配置数据
```mysql
insert into `oauth2_registered_client` (`id`, `client_id`, `client_id_issued_at`, `client_secret`, `client_secret_expires_at`, `client_name`, `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`, `post_logout_redirect_uris`, `scopes`, `client_settings`, `token_settings`) values('2f3f7e1f-8ee0-46ce-8d12-d427e3bdac08','oidc-client','2023-08-25 15:14:52','{bcrypt}$2a$10$v84Cecwigq73D7oLZTg2R.7y.UoTFMpXyYfTWoreuKAT6cOco9LMC',NULL,'oidc-client-name','client_secret_basic','refresh_token,client_credentials,authorization_code','http://127.0.0.1:8080/login/oauth2/code/oidc-client','http://127.0.0.1:8080/','openid,profile,email','{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-proof-key\":false,\"settings.client.require-authorization-consent\":true}','{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":true,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",300.000000000],\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",3600.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000],\"settings.token.device-code-time-to-live\":[\"java.time.Duration\",300.000000000]}');
```
> 注：也可以通过执行程序来创建OAuth2 Client配置数据：com.walter.starry.authorizationserver.app.AuthorizationServerApplicationTests.RegisteredClientRepositoryTest.save

#### 常用SQL
```mysql
# 查看指定权限可访问的菜单（或功能）明细
SELECT ri.*
FROM resource_item ri, resource_group rg, authority_resource ar
WHERE ri.`parent_group_code` = rg.`code`
AND ar.`resource_item_code` = ri.`code`
AND rg.`type` = 1 # 1-菜单，2-功能
AND ar.`authority_item_code` IN ('ROLE_ANONYMOUS','ROLE_USER','ROLE_ADMIN')
ORDER BY rg.`seq`, rg.`id`, ri.`seq`, ri.`id`;

# 查看全部菜单（或功能）与权限之间的关系
SELECT ar.*
FROM `authority_resource` ar, `resource_item` ri, `resource_group` rg
WHERE ar.`resource_item_code` = ri.`code`
AND ri.`parent_group_code` = rg.`code`
AND rg.`type` = 1 # 1-菜单，2-功能
ORDER BY ar.id;
```

### 1.4 Elasticsearch（可选）
#### 创建索引
##### 用户信息索引
```text
PUT /authorization_server.user.v1
{
    "aliases": {
        "authorization_server.user": {}
    },
    "settings": {
        "number_of_shards": 3,
        "number_of_replicas": 1
    },
    "mappings": {
        "dynamic": "strict",
        "properties": {
            "id": {
                "type": "long"
            },
            "username": {
                "type": "keyword",
                "ignore_above": 128
            },
            "nickname": {
                "type": "text",
                "analyzer": "ik_max_word",
                "search_analyzer": "ik_smart",
                "fields": {
                    "keyword": {
                        "type": "keyword",
                        "ignore_above": 255
                    }
                }
            },
            "enabled": {
                "type": "boolean"
            },
            "account_expired": {
                "type": "boolean"
            },
            "account_locked": {
                "type": "boolean"
            },
            "credentials_expired": {
                "type": "boolean"
            },
            "oidc_registration_id": {
                "type": "keyword",
                "ignore_above": 255
            },
            "open_id": {
                "type": "keyword",
                "ignore_above": 128
            },
            "expired_sessions_clean_time": {
                "type": "date",
                "format": "date_optional_time"
            },
            "create_time": {
                "type": "date",
                "format": "date_optional_time"
            },
            "update_time": {
                "type": "date",
                "format": "date_optional_time"
            },
            "authorities": {
                "type": "nested",
                "properties": {
                    "id": {
                        "type": "long"
                    },
                    "authority": {
                        "type": "keyword",
                        "ignore_above": 128
                    },
                    "create_time": {
                        "type": "date",
                        "format": "date_optional_time"
                    },
                    "update_time": {
                        "type": "date",
                        "format": "date_optional_time"
                    }
                }
            }
        }
    }
}
```

##### 索引用户信息
```text
// 为用户文档添加索引
com.walter.starry.security.ElasticsearchTest.DocumentTest.bulkIndex

// 搜索用户文档
com.walter.starry.security.ElasticsearchTest.DocumentTest.searchEsUser
```

## 2. 启动服务
### 2.1 启动Mysql和Elasticsearch
在本示例中，Elasticsearch分别独立部署

### 2.2 Docker Compose启动其他中间件（如：MySQL、Redis-Stack、Pulsar等）
docker compose的主文件为compose.yml

#### 2.2.1 宿主机上准备待挂载的目录
```shell
# Pulsar相关目录（参考：https://pulsar.apache.org/docs/3.2.x/getting-started-docker-compose/#step-2-create-a-pulsar-cluster）
sudo mkdir -p ./data/zookeeper ./data/bookkeeper 
# Reids相关目录
sudo mkdir -p ./data/redis
# MySQL相关目录
sudo mkdir -p ./data/mysql/master/conf.d ./data/mysql/master/datadir
sudo mkdir -p ./data/mysql/slave1/conf.d ./data/mysql/slave1/datadir
# this step might not be necessary on other than Linux platforms
sudo chown 10000 -R data
```

#### 2.2.2 配置MYSQL
MYSQL主服务配置文件：
cat ./data/mysql/master/conf.d/config-file.cnf
```
[mysqld]
server_id=1
port=3306

sync_binlog=1
innodb_flush_log_at_trx_commit=1
binlog_format=ROW

# 配置半同步复制
#rpl_semi_sync_source_enabled=1
#rpl_semi_sync_replica_enabled=1
replication_sender_observe_commit_only=1
replication_optimize_for_static_plugin_config=1

# 配置GTID
gtid_mode=ON
enforce-gtid-consistency=ON
#skip_replica_start=ON
```

MYSQL从服务1配置文件：
cat ./data/mysql/slave1/conf.d/config-file.cnf
```
[mysqld]
server_id=2
port=3306

sync_binlog=1
innodb_flush_log_at_trx_commit=1
binlog_format=ROW

# 配置半同步复制
rpl_semi_sync_source_enabled=1
rpl_semi_sync_replica_enabled=1
replication_sender_observe_commit_only=1
replication_optimize_for_static_plugin_config=1

# 配置GTID
gtid_mode=ON
enforce-gtid-consistency=ON
#skip_replica_start=ON
```

MYSQL从服务2配置文件：
cat ./data/mysql/slave2/conf.d/config-file.cnf
```
[mysqld]
server_id=3
port=3306

sync_binlog=1
innodb_flush_log_at_trx_commit=1
binlog_format=ROW

# 配置半同步复制
rpl_semi_sync_source_enabled=1
rpl_semi_sync_replica_enabled=1
replication_sender_observe_commit_only=1
replication_optimize_for_static_plugin_config=1

# 配置GTID
gtid_mode=ON
enforce-gtid-consistency=ON
#skip_replica_start=ON
```
#### 2.2.3 配置主从异步复制的步骤：
```text
主库master：
	CREATE USER 'repl'@'%' IDENTIFIED BY 'replpassword';
	GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
	FLUSH PRIVILEGES;
	
	SHOW MASTER STATUS;
		File           Position  Binlog_Do_DB  Binlog_Ignore_DB  Executed_Gtid_Set  
		-------------  --------  ------------  ----------------  -------------------
		binlog.000003     53320                                                     
		
	mysqldump --all-databases --master-data > dbdump.db
	
从库slave1：
	mysql < dbdump.db
	
	CHANGE REPLICATION SOURCE TO SOURCE_HOST='172.18.1.1', SOURCE_PORT=3306, SOURCE_USER='repl', SOURCE_PASSWORD='replpassword', SOURCE_LOG_FILE='binlog.000003', SOURCE_LOG_POS=53320;
	
	START REPLICA
```
> 注：关于搭建MYSQL主从环境：   
>（1）项目初始阶段如何搭建主从复制环境（本应用使用的复制账/密为：repl/replpassword）：https://dev.mysql.com/doc/refman/8.0/en/replication-howto.html  
>（2）如何在既有的主从复制环境中，在不对主库停机的情况下加入新的从库：https://dev.mysql.com/doc/refman/8.0/en/replication-howto-additionalslaves.html  
>（3）如何配置半同步复制：https://dev.mysql.com/doc/refman/8.0/en/replication-semisync.html
>（4）允许停机的情况下，如何配置GTID复制：https://dev.mysql.com/doc/refman/8.0/en/replication-gtids-howto.html

#### 2.2.4 按需修改以下中间件配置（假设中间件的宿主机IP是192.168.10.131）
* compose-pulsar.yml
```yaml
services: 
  broker: 
    environment: 
      - advertisedListeners=external:pulsar://192.168.10.131:6650
```

#### 2.2.5 启动中间件服务
参考：
* https://pulsar.apache.org/docs/3.2.x/getting-started-docker-compose/#step-2-create-a-pulsar-cluster
* https://redis.io/docs/install/install-stack/docker/
```shell
# 启动
sudo docker compose up -d
# 停止
#sudo docker compose down
```

#### 2.2.6 Pulsar配置
（1）Pulsar启动完毕后，执行以下操作生成Pulsar Manager控制台的登录账密：
```shell
# 登录账密设置的说明参看：https://github.com/apache/pulsar-manager

docker exec -it <PulsarManager容器ID> /bin/bash

CSRF_TOKEN=$(curl http://pulsar-manager:7750/pulsar-manager/csrf-token)

curl \
-H "X-XSRF-TOKEN: $CSRF_TOKEN" \
-H "Cookie: XSRF-TOKEN=$CSRF_TOKEN;" \
-H 'Content-Type: application/json' \
-X PUT http://pulsar-manager:7750/pulsar-manager/users/superuser \
-d '{"name": "admin", "password": "apachepulsar", "description": "test", "email": "username@test.org"}'
```

（2） 访问PulsarManager控制台：http://<宿主机>:9527/#/management/tenants

（3）在Pulsar Manager控制台添加本SpringBoot应用所必须的Pulsar信息：
* 创建环境
> Environment Name：dev  
> Service URL：http://192.168.10.131:8080  
> Bookie URL：http://192.168.10.131:6650  
> 租户：${app.pulsar.base-reg.tenant}
> 命名空间：${app.pulsar.base-reg.namespace}

### 2.3 启动Java应用
#### 2.3.1 按需修改以下springboot应用配置（假设中间件的宿主机IP是192.168.10.131）
* application.yml
```yaml
app:
  middleware-host: 192.168.10.131
```

* redisson.yml
```yaml
singleServerConfig:
  address: "redis://192.168.10.131:6379"
```

#### 2.3.2 启动springboot应用时，添加以下vm参数
```shell
# 在使用Pulsar3.x的情况下，启动Java进程时需要添加VM启动参数--add-opens java.base/sun.net=ALL-UNNAMED
java --add-opens java.base/sun.net=ALL-UNNAMED -jar xxx-app.jar
```

### 2.4 如何访问
* Step1: 用户未登录，客户端请求获取授权码
> 浏览器地址栏输入：  
> http://127.0.0.1:8080/oauth2/authorize?client_id=oidc-client&response_type=code&scope=openid+profile+email&redirect_uri=http://127.0.0.1:8080/login/oauth2/code/oidc-client

* Step2：将重定向到登录页，并输入登录账号密码登录成功

* Step3：用户选择待授权的选项，提交后跳转到重定向url且url里带上授权码（见下面示例重定向url里的code）
```text
http://127.0.0.1:8080/login/oauth2/code/oidc-client?code=KchrQNG1pmQaMdFKh1sT7QSuGhctoq1iFGkn5YySqOx_94lMU-bW01Awtc3opStsj18NP4pQiPzTH-swWtRsqVAb5zDjR1MZjHmRfqr3GT6T3hN8vYKvqXpP7J1x0sIS
```

* Step4：根据授权码，获取AccessToken、IdToken、RefreshToken
```text
[Post]      http://127.0.0.1:8080/oauth2/token
[Header]    Authorization=Basic [[[clientId]:[clientSecret]]的Base64编码值]
[Body]      code=[授权码]
            grant_type=authorization_code
            redirect_uri=[预设的重定向地址]
```

* Step5: 刷新令牌  
```text
[Post]      http://127.0.0.1:8080/oauth2/token
[Header]    Authorization=Basic [[[clientId]:[clientSecret]]的Base64编码值]
[Body]      grant_type=refresh_token
            refresh_token=[刷新令牌值]
```
