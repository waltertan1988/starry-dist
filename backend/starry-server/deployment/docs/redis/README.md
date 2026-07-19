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

### 3.2 在各个Redis Cluster节点的配置文件找到以下配置值并修改为如下
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

# 主从复制时，从节点配置主节点的密码（与主节点的requirepass一致）。建议主节点也一同配置
masterauth 123456

# 禁用不安全的命令
rename-command KEYS ""
rename-command FLUSHDB ""
rename-command FLUSHALL ""

####################################
### 以下为Redis Cluster专门的配置项 ###
####################################
# 启用redis cluster模式
# cluster-enabled yes
cluster-enabled yes

# cluster-config-file为非用户编辑的文件
# cluster-config-file nodes-6379.conf
cluster-config-file nodes-6379.conf

# 节点在不被视为故障的情况下，可以处于不可用状态的最长时间（毫秒）。如果主节点在超过指定时间后仍无法访问，其副本节点将对其进行故障转移
cluster-node-timeout 15000

# 其他主节点没有从节点时，允许自己“迁移”成为该孤立主节点的从节点，但“迁移”的前提必须满足自己当前的主节点的其它副本数达到此值（默认为1）
# cluster-migration-barrier 1

# 当集群检测到存在不被覆盖的哈希槽（没有节点为该哈希槽服务）时，整个集群是否都不可用
# cluster-require-full-coverage yes
cluster-require-full-coverage no

# Redis集群被标记为失败时，集群中的节点将停止处理所有流量。设置为yes，以允许在故障状态下在节点上读取数据。
# cluster-allow-reads-when-down no
```
> Redis Cluster配置参数的具体说明可参看：[这里](https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/#create-and-use-a-redis-cluster)

### 3.3 启动服务
docker compose启动示例参看：[这里](https://github.com/waltertan1988/starry-dist/blob/main/backend/starry-server/deployment/middleware/compose-redis-cluster.yml)

### 3.4 建立集群
在任意一个节点中执行`redis-cli --cluster create`命令，创建集群：
```shell
root@redis-node-1:/data# redis-cli -a 123456 --cluster create 192.168.100.42:6379 192.168.100.42:6380 192.168.100.42:6381 192.168.100.42:6382 192.168.100.42:6383 192.168.100.42:6384 --cluster-replicas 1
Warning: Using a password with '-a' or '-u' option on the command line interface may not be safe.
>>> Performing hash slots allocation on 6 nodes...
Master[0] -> Slots 0 - 5460
Master[1] -> Slots 5461 - 10922
Master[2] -> Slots 10923 - 16383
Adding replica 192.168.100.42:6383 to 192.168.100.42:6379
Adding replica 192.168.100.42:6384 to 192.168.100.42:6380
Adding replica 192.168.100.42:6382 to 192.168.100.42:6381
>>> Trying to optimize slaves allocation for anti-affinity
[WARNING] Some slaves are in the same host as their master
M: e16857fa2b900a5ca0060d62a2d0914ea054648d 192.168.100.42:6379
   slots:[0-5460] (5461 slots) master
M: c2b93cd7503a40cdf9066f61dc1d6489bc6a03dd 192.168.100.42:6380
   slots:[5461-10922] (5462 slots) master
M: 81bdf7ef1e54b20898c4f5762216f3b2998d8001 192.168.100.42:6381
   slots:[10923-16383] (5461 slots) master
S: 9ef9f6fdc42a98de1dcf1921f494f8ecb3b280d9 192.168.100.42:6382
   replicates 81bdf7ef1e54b20898c4f5762216f3b2998d8001
S: 1bea1e5f22b6ffcc12619c5d25541edbd71e2860 192.168.100.42:6383
   replicates e16857fa2b900a5ca0060d62a2d0914ea054648d
S: 9dbaab53954ee3a408d284c6e43869b1470d7017 192.168.100.42:6384
   replicates c2b93cd7503a40cdf9066f61dc1d6489bc6a03dd
Can I set the above configuration? (type 'yes' to accept): yes
>>> Nodes configuration updated
>>> Assign a different config epoch to each node
>>> Sending CLUSTER MEET messages to join the cluster
Waiting for the cluster to join
.
>>> Performing Cluster Check (using node 192.168.100.42:6379)
M: e16857fa2b900a5ca0060d62a2d0914ea054648d 192.168.100.42:6379
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
M: 81bdf7ef1e54b20898c4f5762216f3b2998d8001 192.168.100.42:6381
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
S: 9dbaab53954ee3a408d284c6e43869b1470d7017 192.168.100.42:6384
   slots: (0 slots) slave
   replicates c2b93cd7503a40cdf9066f61dc1d6489bc6a03dd
S: 1bea1e5f22b6ffcc12619c5d25541edbd71e2860 192.168.100.42:6383
   slots: (0 slots) slave
   replicates e16857fa2b900a5ca0060d62a2d0914ea054648d
M: c2b93cd7503a40cdf9066f61dc1d6489bc6a03dd 192.168.100.42:6380
   slots:[5461-10922] (5462 slots) master
   1 additional replica(s)
S: 9ef9f6fdc42a98de1dcf1921f494f8ecb3b280d9 192.168.100.42:6382
   slots: (0 slots) slave
   replicates 81bdf7ef1e54b20898c4f5762216f3b2998d8001
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
root@redis-node-1:/data#
```

### 3.5 查看redis cluster集群的信息
#### 3.5.1 查看redis cluster集群的总体情况
在任一节点上使用`redis-cli cluster info`命令：
```shell
root@redis-node-1:/data# redis-cli -a 123456 cluster info 
Warning: Using a password with '-a' or '-u' option on the command line interface may not be safe.
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:6
cluster_my_epoch:1
cluster_stats_messages_ping_sent:805
cluster_stats_messages_pong_sent:762
cluster_stats_messages_sent:1567
cluster_stats_messages_ping_received:757
cluster_stats_messages_pong_received:805
cluster_stats_messages_meet_received:5
cluster_stats_messages_received:1567
total_cluster_links_buffer_limit_exceeded:0
root@redis-node-1:/data#
```

#### 3.5.2 查看redis cluster集群中各个节点的主从配对关系
在任意一个redis cluster节点中，通过以下2种方法查看：

* 方法1 - 使用`redis-cli cluster nodes`命令：
```shell
root@redis-node-1:/data# redis-cli -a 123456 cluster nodes
Warning: Using a password with '-a' or '-u' option on the command line interface may not be safe.
81bdf7ef1e54b20898c4f5762216f3b2998d8001 192.168.100.42:6381@16381 master - 0 1784474755000 3 connected 10923-16383
9dbaab53954ee3a408d284c6e43869b1470d7017 192.168.100.42:6384@16384 slave c2b93cd7503a40cdf9066f61dc1d6489bc6a03dd 0 1784474755745 2 connected
e16857fa2b900a5ca0060d62a2d0914ea054648d 192.168.100.42:6379@16379 myself,master - 0 0 1 connected 0-5460
1bea1e5f22b6ffcc12619c5d25541edbd71e2860 192.168.100.42:6383@16383 slave e16857fa2b900a5ca0060d62a2d0914ea054648d 0 1784474755000 1 connected
c2b93cd7503a40cdf9066f61dc1d6489bc6a03dd 192.168.100.42:6380@16380 master - 0 1784474755000 2 connected 5461-10922
9ef9f6fdc42a98de1dcf1921f494f8ecb3b280d9 192.168.100.42:6382@16382 slave 81bdf7ef1e54b20898c4f5762216f3b2998d8001 0 1784474756870 3 connected
root@redis-node-1:/data#
```

* 方法2 - 查看由配置项`cluster-config-file`指定的文件内容：
```shell
root@redis-node-1:/data# cat nodes-6379.conf 
81bdf7ef1e54b20898c4f5762216f3b2998d8001 192.168.100.42:6381@16381,,tls-port=0,shard-id=650ae2437d727dff0139a6ee2b8fd1415c2fb28b master - 0 1784474134000 3 connected 10923-16383
9dbaab53954ee3a408d284c6e43869b1470d7017 192.168.100.42:6384@16384,,tls-port=0,shard-id=2e14e9a9646b5ac106fd300fe2a4d0afadb516a0 slave c2b93cd7503a40cdf9066f61dc1d6489bc6a03dd 0 1784474136007 2 connected
e16857fa2b900a5ca0060d62a2d0914ea054648d 192.168.100.42:6379@16379,,tls-port=0,shard-id=3eedd7fbde1fa456476da59a663984549f219610 myself,master - 0 0 1 connected 0-5460
1bea1e5f22b6ffcc12619c5d25541edbd71e2860 192.168.100.42:6383@16383,,tls-port=0,shard-id=3eedd7fbde1fa456476da59a663984549f219610 slave e16857fa2b900a5ca0060d62a2d0914ea054648d 0 1784474133961 1 connected
c2b93cd7503a40cdf9066f61dc1d6489bc6a03dd 192.168.100.42:6380@16380,,tls-port=0,shard-id=2e14e9a9646b5ac106fd300fe2a4d0afadb516a0 master - 0 1784474134994 2 connected 5461-10922
9ef9f6fdc42a98de1dcf1921f494f8ecb3b280d9 192.168.100.42:6382@16382,,tls-port=0,shard-id=650ae2437d727dff0139a6ee2b8fd1415c2fb28b slave 81bdf7ef1e54b20898c4f5762216f3b2998d8001 0 1784474134000 3 connected
vars currentEpoch 6 lastVoteEpoch 0
root@redis-node-1:/data#
```

#### 3.5.3 查看某个节点的主从复制信息
```shell
root@redis-node-1:/data# redis-cli -a 123456
Warning: Using a password with '-a' or '-u' option on the command line interface may not be safe.
127.0.0.1:6379> info replication
# Replication
role:master
connected_slaves:1
slave0:ip=192.168.100.42,port=6383,state=online,offset=616,lag=1
master_failover_state:no-failover
master_replid:685fad1a32e990a6cd7e81d7d2b923dad16b2fc4
master_replid2:0000000000000000000000000000000000000000
master_repl_offset:616
second_repl_offset:-1
repl_backlog_active:1
repl_backlog_size:1048576
repl_backlog_first_byte_offset:1
repl_backlog_histlen:616
127.0.0.1:6379>
```
