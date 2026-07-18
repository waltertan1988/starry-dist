# MySQL部署说明
介绍MySQL的部署，包括MySQL的主从异步复制、半同步复制和MHA高可用。

## 一、MySQL的主从异步复制及半同步复制配置
### 1.1 MySQL配置文件
* MYSQL主库配置文件：
cat ./data/mysql/master/conf.d/config-file.cnf
```shell
[mysqld]
server_id=1
port=3306
default-time-zone='+08:00'

#read_only = 1  		# MHA必选：从库只读
#super_read_only = 1  	# MHA必选：禁止super权限写从库

sync_binlog=1
binlog_format=ROW
binlog_expire_logs_seconds = 604800  	# binlog7天自动清理
relay_log_recovery = 1  				# MHA必选：重启后自动恢复中继日志，避免复制中断
#relay_log_purge = 0					# MHA下，relay_log_purge建议设置为0，保留从库的中继日志‌，以便在主库故障切换时，利用这些日志将其他滞后从库的数据补齐，确保集群数据一致性

# 开启慢日志，日志名称由slow_query_log_file指定，由long_query_time指定超过多少秒为慢SQL
slow_query_log=1
# 慢管理语句也记录到慢日志里
log_slow_admin_statements=1

# 参与选主的节点建议开启（默认开启）
#log_replica_updates=1

# 配置半同步复制
rpl_semi_sync_source_enabled=1		#即rpl_semi_sync_master_enabled，安装完半同步复制插件后再打开
rpl_semi_sync_replica_enabled=1		#即rpl_semi_sync_slave_enabled，安装完半同步复制插件后再打开
replication_sender_observe_commit_only=1
replication_optimize_for_static_plugin_config=1

# 配置GTID
gtid_mode=ON
enforce-gtid-consistency=ON
#skip_replica_start=ON

# InnoDB核心优化
innodb_flush_log_at_trx_commit = 1  #事务提交刷盘，保证数据安全
```

* MYSQL从库1（MHA备主库）配置文件：
cat ./data/mysql/slave1/conf.d/config-file.cnf
```shell
[mysqld]
server_id=2
port=3306
default-time-zone='+08:00'

read_only = 1  			# MHA必选：从库只读
super_read_only = 1  	# MHA必选：禁止super权限写从库

sync_binlog=1
binlog_format=ROW
binlog_expire_logs_seconds = 604800  	# binlog7天自动清理
relay_log_recovery = 1  				# MHA必选：重启后自动恢复中继日志，避免复制中断
#relay_log_purge = 0					# MHA下，relay_log_purge建议设置为0，保留从库的中继日志‌，以便在主库故障切换时，利用这些日志将其他滞后从库的数据补齐，确保集群数据一致性

# 开启慢日志，日志名称由slow_query_log_file指定，由long_query_time指定超过多少秒为慢SQL
slow_query_log=1
# 慢管理语句也记录到慢日志里
log_slow_admin_statements=1

# 参与选主的节点建议开启（默认开启）
#log_replica_updates=1

# 配置半同步复制
rpl_semi_sync_source_enabled=1		#即rpl_semi_sync_master_enabled，安装完半同步复制插件后再打开
rpl_semi_sync_replica_enabled=1		#即rpl_semi_sync_slave_enabled，安装完半同步复制插件后再打开
replication_sender_observe_commit_only=1
replication_optimize_for_static_plugin_config=1

# 配置GTID
gtid_mode=ON
enforce-gtid-consistency=ON
#skip_replica_start=ON

# InnoDB核心优化
innodb_flush_log_at_trx_commit = 1  # 事务提交刷盘，保证数据安全
```

* MYSQL从库2（MHA普通从库）配置文件：
cat ./data/mysql/slave2/conf.d/config-file.cnf
```shell
[mysqld]
server_id=3
port=3306
default-time-zone='+08:00'

read_only = 1  			# MHA必选：从库只读
super_read_only = 1  	# MHA必选：禁止super权限写从库

sync_binlog=1
binlog_format=ROW
binlog_expire_logs_seconds = 604800  	# binlog7天自动清理
relay_log_recovery = 1  				# MHA必选：重启后自动恢复中继日志，避免复制中断
#relay_log_purge = 0					# MHA下，relay_log_purge建议设置为0，保留从库的中继日志‌，以便在主库故障切换时，利用这些日志将其他滞后从库的数据补齐，确保集群数据一致性

# 开启慢日志，日志名称由slow_query_log_file指定，由long_query_time指定超过多少秒为慢SQL
slow_query_log=1
# 慢管理语句也记录到慢日志里
log_slow_admin_statements=1

# 参与选主的节点建议开启（默认开启）
#log_replica_updates=1

# 配置半同步复制
rpl_semi_sync_source_enabled=1		#即rpl_semi_sync_master_enabled，安装完半同步复制插件后再打开
rpl_semi_sync_replica_enabled=1		#即rpl_semi_sync_slave_enabled，安装完半同步复制插件后再打开
replication_sender_observe_commit_only=1
replication_optimize_for_static_plugin_config=1

# 配置GTID
gtid_mode=ON
enforce-gtid-consistency=ON
#skip_replica_start=ON

# InnoDB核心优化
innodb_flush_log_at_trx_commit = 1  # 事务提交刷盘，保证数据安全
```

### 1.2 启动MySQL实例
docker compose启动示例参看：[这里](https://github.com/waltertan1988/starry-dist/blob/main/backend/starry-server/deployment/middleware/compose-mysql.yml)

### 1.3 搭建MySQL主从异步复制
```text
主库master：
	ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '';

	CREATE USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '';
	GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
	
	CREATE USER 'repl'@'%' IDENTIFIED WITH mysql_native_password BY 'replpassword';
	GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
	
	FLUSH PRIVILEGES;
	
	SHOW MASTER STATUS;
		File           Position  Binlog_Do_DB  Binlog_Ignore_DB  Executed_Gtid_Set  
		-------------  --------  ------------  ----------------  -------------------
		binlog.000003     53320                                                     
		
	mysqldump --all-databases --master-data=2 --single-transaction > dbdump.db
	
从库slave1：
	mysql < dbdump.db
	
	# 非GTID方式
	CHANGE REPLICATION SOURCE TO SOURCE_HOST='172.18.1.1', SOURCE_PORT=3306, SOURCE_USER='repl', SOURCE_PASSWORD='replpassword', SOURCE_LOG_FILE='binlog.000003', SOURCE_LOG_POS=53320;
	# GTID方式
	CHANGE REPLICATION SOURCE TO SOURCE_HOST='172.18.1.1', SOURCE_PORT=3306, SOURCE_USER='repl', SOURCE_PASSWORD='replpassword', SOURCE_AUTO_POSITION=1;
	
	START REPLICA
```

### 1.4 MySQL主从复制的其他资源
* [项目初始阶段如何搭建主从复制环境](https://dev.mysql.com/doc/refman/8.0/en/replication-howto.html)（本应用使用的复制账/密为：repl/replpassword）  
* [如何在既有的主从复制环境中，在不对主库停机的情况下加入新的从库](https://dev.mysql.com/doc/refman/8.0/en/replication-howto-additionalslaves.html)  
* [如何配置半同步复制](https://dev.mysql.com/doc/refman/8.0/en/replication-semisync.html)  
* [允许停机的情况下，如何配置GTID复制](https://dev.mysql.com/doc/refman/8.0/en/replication-gtids-howto.html)  
* [设置数据源为只读并备份数据](https://dev.mysql.com/doc/refman/8.0/en/replication-solutions-backups-read-only.html)  

## 二、MySQL高可用方案之MHA
### 2.1 使用MHA方案的前提
* MySQL版本：v5.5-v8.0
* MySQL集群的主从复制已配置好并启用中（建议使用半同步复制）

### 2.2 MHA方案的部署与管理
参看：[如何搭建和管理MHA高可用集群](https://github.com/waltertan1988/starry-dist/blob/main/backend/starry-server/deployment/docs/mysql/MySQL8-MHA.md)
