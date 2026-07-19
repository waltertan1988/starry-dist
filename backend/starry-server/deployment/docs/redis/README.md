# Redis部署说明
Redis的部署，包括Redis的主从复制模式、哨兵模式（sentinel）和集群模式（cluster）

## 一、配置并使用Redis-Serve的主从复制模式
### 1.1 下载默认的配置文件
```shell
curl -o ./data/redis/replication/master/conf/redis.conf https://raw.githubusercontent.com/redis/redis/8.2/redis.conf
curl -o ./data/redis/replication/slave1/conf/redis.conf https://raw.githubusercontent.com/redis/redis/8.2/redis.conf
```
> 参考：[官方配置说明](https://redis.io/docs/latest/operate/oss_and_stack/management/config/)

### 1.2 找到以下配置值并修改为如下
```shell
# 放行所有IP可访问
# bind 127.0.0.1 -::1

# no可以放行无password用户可从非本机的IP访问，保护模式设置为yes时也可通过同时用requirepass设置密码让所有IP可以访问
protected-mode yes

# 设置密码
requirepass 123456

# pid文件保存位置
pidfile /var/run/redis_6379.pid

# 日志文件位置
logfile ""

# RDB/AOF文件的保存位置
dir ./

# RDB文件名称
dbfilename dump.rdb

# RDB自动备份策略（默认开启）
save 3600 1 300 100 60 10000

# 开启AOF（注：务必先通过config set在线开启，待生成aof文件后，再修改配置文件，否则会丢失已有数据）
appendonly yes

# AOF文件的落盘策略
appendfsync everysec

# AOF文件的重写策略
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# 主从复制时，从节点配置主节点的ip和端口。仅从节点需要配置。从节点可执行命令`replicaof no one`解除主从关系。
# replicaof <masterip> <masterport>

# 至少有1个从节点在线（最多延迟10秒）时，主节点才可以写入数据（注：该配置在redis-cluster模式不生效）
min-replicas-to-write 1
min-replicas-max-lag 10

# 主从复制时，从节点配置主节点的密码（与主节点的requirepass一致）。建议主节点也一同配置
masterauth 123456

# 从节点是否只读（注：该配置在redis-cluster模式不生效，redis-cluster强制从节点只读）
replica-read-only yes

# 从节点跟主节点临时断开时，主节点临时存放增量数据（用于从节点重新连接后进行部分复制）的临时缓存区大小
# repl-backlog-size 1mb

# 主节点在该时间内没有任何从节点连接上，则清空临时缓存区
# repl-backlog-ttl 3600

# 使用Sentinel进行主从故障转移时，从节点被提升为主节点的优先级，越小越优先，0表示永不提升为主节点（注：该配置在redis-cluster模式不生效）
replica-priority 100

# 禁用不安全的命令
rename-command KEYS ""
rename-command FLUSHDB ""
rename-command FLUSHALL ""
```

### 1.3 启动服务
docker compose启动示例参看：[这里](https://github.com/waltertan1988/starry-dist/blob/main/backend/starry-server/deployment/middleware/compose-redis-replication.yml)

### 1.4 查看主从复制信息
查看主节点信息：
```shell
root@redis-slave1:/data# redis-cli -p 6379 -a 123456
127.0.0.1:6379> info replication
# Replication
role:master
connected_slaves:2
min_slaves_good_slaves:2
slave0:ip=192.168.100.42,port=6381,state=online,offset=34643,lag=0
slave1:ip=192.168.100.42,port=6380,state=online,offset=34643,lag=0
master_failover_state:no-failover
master_replid:4bafdd8d908ee15beb5b5f2f1bb401630e03dc56
master_replid2:0000000000000000000000000000000000000000
master_repl_offset:34786
second_repl_offset:-1
repl_backlog_active:1
repl_backlog_size:1048576
repl_backlog_first_byte_offset:1
repl_backlog_histlen:34786
127.0.0.1:6379>
127.0.0.1:6379> role
1) "master"
2) (integer) 60383
3) 1) 1) "192.168.100.42"
      2) "6381"
      3) "60383"
   2) 1) "192.168.100.42"
      2) "6380"
      3) "60240"
````

查看从节点信息：
```shell
root@redis-slave1:/data# redis-cli -p 6380 -a 123456
127.0.0.1:6380> info replication
# Replication
role:slave
master_host:192.168.100.42
master_port:6379
master_link_status:up
master_last_io_seconds_ago:0
master_sync_in_progress:0
slave_read_repl_offset:71251
slave_repl_offset:71251
replica_full_sync_buffer_size:0
replica_full_sync_buffer_peak:0
master_current_sync_attempts:1
master_total_sync_attempts:1
master_link_up_since_seconds:334
total_disconnect_time_sec:0
slave_priority:90
slave_read_only:1
replica_announced:1
connected_slaves:0
min_slaves_good_slaves:0
master_failover_state:no-failover
master_replid:4bafdd8d908ee15beb5b5f2f1bb401630e03dc56
master_replid2:0000000000000000000000000000000000000000
master_repl_offset:71251
second_repl_offset:-1
repl_backlog_active:1
repl_backlog_size:1048576
repl_backlog_first_byte_offset:15
repl_backlog_histlen:71237
127.0.0.1:6380> 
127.0.0.1:6380> role
1) "slave"
2) "192.168.100.42"
3) (integer) 6379
4) "connected"
5) (integer) 72681
```

## 二、配置并使用Redis-Sentinel模式
### 2.1 前提
* 注意：使用docker部署redis哨兵模式时，网络模式需要改用host模式，原因参考[这里](https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel/#sentinel-docker-nat-and-possible-issues)
* 哨兵实例数为2n+1，建议至少3个节点

### 2.2 下载默认的sentinel配置文件
```shell
curl -o ./data/redis/sentinel/stl1/sentinel.conf https://raw.githubusercontent.com/redis/redis/refs/tags/8.2.7/sentinel.conf
curl -o ./data/redis/sentinel/stl2/sentinel.conf https://raw.githubusercontent.com/redis/redis/refs/tags/8.2.7/sentinel.conf
curl -o ./data/redis/sentinel/stl3/sentinel.conf https://raw.githubusercontent.com/redis/redis/refs/tags/8.2.7/sentinel.conf
```

### 2.3 找到以下配置值并修改为如下
```shell
# pid文件位置
pidfile /var/run/redis-sentinel.pid

# sentinel的日志文件位置
# logfile ""
logfile "/usr/local/etc/redis/sentinel.log"

# sentinel监控的redis主节点
sentinel monitor mymaster 127.0.0.1 6379 2

# sentinel配置redis主节点的密码
# sentinel auth-pass <master-name> <password>
sentinel auth-pass mymaster 123456

# redis主节点下线超过指定时间后被sentinel判定为S_DOWN
# sentinel down-after-milliseconds mymaster 30000
sentinel down-after-milliseconds mymaster 5000
```

### 2.4 启动服务
docker compose启动示例参看：[这里](https://github.com/waltertan1988/starry-dist/blob/main/backend/starry-server/deployment/middleware/compose-redis-sentinel.yml)

### 2.5 查看sentinel的运行信息
查看启动日志：
```shell
root@redis-sentinel-1:/usr/local/etc/redis# cat sentinel.log 
1:X 10 Jul 2026 07:47:48.628 # WARNING Memory overcommit must be enabled! Without it, a background save or replication may fail under low memory condition. Being disabled, it can also cause failures without low memory condition, see https://github.com/jemalloc/jemalloc/issues/1328. To fix this issue add 'vm.overcommit_memory = 1' to /etc/sysctl.conf and then reboot or run the command 'sysctl vm.overcommit_memory=1' for this to take effect.
1:X 10 Jul 2026 07:47:48.628 * oO0OoO0OoO0Oo Redis is starting oO0OoO0OoO0Oo
1:X 10 Jul 2026 07:47:48.628 * Redis version=8.2.7, bits=64, commit=00000000, modified=1, pid=1, just started
1:X 10 Jul 2026 07:47:48.628 * Configuration loaded
1:X 10 Jul 2026 07:47:48.629 * Increased maximum number of open files to 10032 (it was originally set to 1024).
1:X 10 Jul 2026 07:47:48.629 * monotonic clock: POSIX clock_gettime
1:X 10 Jul 2026 07:47:48.638 * Running mode=sentinel, port=26379.
1:X 10 Jul 2026 07:47:48.641 * Sentinel ID is 853a3e558885aee07661ffd62d57bf8246e9a936
1:X 10 Jul 2026 07:47:48.641 # +monitor master mymaster 172.18.2.1 6379 quorum 2
```

查看sentinel信息：
```shell
root@redis-slave1:/data# redis-cli -p 26380
127.0.0.1:26380> info sentinel
# Sentinel
sentinel_masters:1
sentinel_tilt:0
sentinel_tilt_since_seconds:-1
sentinel_total_tilt:1
sentinel_running_scripts:0
sentinel_scripts_queue_length:0
sentinel_simulate_failure_flags:0
master0:name=mymaster,status=ok,address=192.168.100.42:6379,slaves=2,sentinels=3
```

### 2.5 通过sentinel在线切换主节点
进入sentinel节点的redis客户端里，执行`sentinel failover <master-name>`命令：
```shell
root@redis-sentinel-1:~#  redis-cli -p 26379
127.0.0.1:26379> sentinel failover mymaster
```

## 三、配置并使用Redis-Cluster模式
### 3.1 前提
* 必须有2n+1个主节点，最少3个。生产环境建议至少为每个主节点配置1个从节点，一共需要6个节点。
* Redis版本>=5，可直接使用`redis-cli --cluster`命令创建集群；Redis版本<=4，需要使用`redis-trib.rb`工具创建集群（不推荐）。
* 使用docker部署redis cluster模式时，网络模式需要改用host模式，原因参考[这里](https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/#redis-cluster-and-docker)
> 注意：本文使用的Redis版本为8.2。

