
# 一. 环境说明
## 1.操作系统的前置准备
* 配置静态IP
* 关闭防火墙
* 可以使用命令：
  - ping
  - arping
  - ip addr
  - vi
  - ssh
  - ping
  - telnet

## 1. 版本
* 操作系统：Ubuntu 24.04.4 LTS
* mysql：8.0.46
* mha-node 和 mha-manager：0.58-2

## 2. IP地址规划
* 主库(master1)：192.168.100.51/24:3306
* 备主库(master2)：192.168.100.52/24:3306
* 普通从库(slave1)：192.168.100.53/24:3306
* MHA管理节点（manager）：192.168.100.60/24:3306
* VIP：192.168.100.50/24:3306

## 3. 账户说明
* SSH账户：walter
* MySQL主从复制的账/密：repl/replpassword


# 二、安装MySQL和MHA
## 1. 所有节点
(1) 配置sudo免密执行命令
```text
walter@master1:~$ sudo visudo

找到下面这行记录："#%sudo   ALL=(ALL:ALL) ALL"
修改为：
%sudo   ALL=(ALL:ALL) NOPASSWD:ALL
```

(2) 配置SSH免密登录
```shell
walter@master1:~$ ssh-keygen -t rsa
walter@master1:~$ ssh-copy-id walter@192.168.100.51
walter@master1:~$ ssh-copy-id walter@192.168.100.52
walter@master1:~$ ssh-copy-id walter@192.168.100.53
walter@master1:~$ ssh-copy-id walter@192.168.100.60
```

(3) 创建starry集群的MHA工作目录
```shell
walter@master1:~$ sudo mkdir -p /var/log/mha/starry
walter@master1:~$ sudo chmod 777 -R /var/log/mha/starry
```

## 2. 所有MySQL节点：
(1) 安装MySQL服务和MHA-Node
```shell
walter@master1:~$ sudo apt update
# 安装MySQL
walter@master1:~$ sudo apt install mysql-server
# 所有的node节点执行如下命令，包括manager节点
walter@master1:~$ apt-get install mha4mysql-node -y
```

(2) 修改/etc/mysql/mysql.conf.d/mysqld.cnf，把bind-address =127.0.0.1注释掉，让其他主机可以访问mysql服务。

(3) 修改mysql.cnf配置文件
```shell
walter@master1:~$ cat /etc/mysql/conf.d/mysql.cnf
[mysqld]
server_id=1             # 注意每个MySQL节点必须唯一
port=3306
default-time-zone='+08:00'

#read_only = 1  		# MHA必选：从库设置为1，表示只读
#super_read_only = 1  	# MHA必选：从库设置为1，禁止super权限写从库

sync_binlog=1
binlog_format=ROW
binlog_expire_logs_seconds = 604800  	# binlog7天自动清理
relay_log_recovery = 1  				# MHA必选：重启后自动恢复中继日志，避免复制中断
relay_log_purge = 0					    # MHA下，relay_log_purge建议设置为0，从而保留从库的中继日志，以便在主库故障切换时，利用这些日志将其他滞后从库的数据补齐，确保集群数据一致性

# 参与选主的节点建议开启（默认开启）
#log_replica_updates=1

# 配置半同步复制
rpl_semi_sync_master_enabled=1		    #即rpl_semi_sync_source_enabled，安装完半同步复制插件后再打开
rpl_semi_sync_slave_enabled=1		    #即rpl_semi_sync_replica_enabled，安装完半同步复制插件后再打开
replication_sender_observe_commit_only=1
replication_optimize_for_static_plugin_config=1

# 配置GTID
gtid_mode=ON
enforce-gtid-consistency=ON
#skip_replica_start=ON

# InnoDB核心优化
innodb_flush_log_at_trx_commit = 1  #事务提交刷盘，保证数据安全

[mysql]
```

(4) 给SSH用户附加到mysql分组
```shell
walter@master1:~$ sudo usermod -aG mysql walter
walter@master1:~$ exit

# 重新登录并查看是否附加成功
walter@master1:~$ groups
walter adm cdrom sudo dip plugdev lxd mysql
```

## 3. MHA-Manager节点：
(1) 安装MHA-Manager
```shell
walter@master1:~$ sudo apt update
# 查看mha安装包
walter@master1:~$ apt-cache search mha4mysql
# 所有的node节点执行如下命令，包括manager节点
walter@master1:~$ apt-get install mha4mysql-node -y
# 在manager节点执行如下命令
walter@master1:~$ apt-get install mha4mysql-manager -y
```

(2) 把starry集群的MHA配置文件app.cnf（不能使用行尾注释），放到/etc/mha/starry下
```shell
walter@master1:~$ cat /etc/mha/starry/app.cnf
[server default]

# MySQL的root用户账号和密码
user=root
password=

# MySQL服务的端口
port=3306

# 服务器中配置ssh免密登录的用户
ssh_user=walter

# MySQL组从同步复制的用户和密码
repl_user=repl
repl_password=replpassword

# 主库的binlog位置
master_binlog_dir=/var/lib/mysql

# 检查频次，每3秒检查一次主节点状态。
ping_interval=3

# manager服务在manger节点的工作目录，这里会生成一些临时文件
manager_workdir=/var/log/mha/starry

# manager服务运行过程中日志文件
manager_log=/var/log/mha/starry/manager.log

# node服务在node节点的工作目录，这里会生成一些临时文件，主要用来保存从已经宕机的主节点上保存下来的binglog日志文件
remote_workdir=/var/log/mha/starry



# 当MHA manager检测到master不可用时，通过masterha_secondary_check脚本来进一步确认，减低误切的风险。建议配置不同机房的主机作为检测跳板。
secondary_check_script=masterha_secondary_check -s 192.168.100.53

# master节点在宕机的时候，执行切换的时候，执行的自定义脚本文件，可以不指定配置这个脚本，如果想在切换的时候，实现自己的逻辑，可以在这里进行编写。比如编写VIP漂移的逻辑等
master_ip_failover_script=/etc/mha/starry/scripts/master_ip_failover

#master_ip_online_change_script=/etc/mha/starry/scripts/master_ip_online_change

# 强制关闭主节点主机的自定义脚本，这个脚本的作用是当发生主从切换之后，把宕机主给再次关闭一次，避免误判后，主还活着，而新的主页选择好了，发生脑裂的现象。
#shutdown_script=/etc/mha/starry/scripts/power_manager

# 发生主从切换之后，发送邮件通知运维人员的自定义脚本
#report_script=/etc/mha/starry/scripts/send_report


#################################
# 数据库节点配置（按优先级排序）#
#################################
[server1]
# 主库节点
hostname=192.168.100.51
ip=192.168.100.51
# candidate_master=1，表示该节点为备选主节点
candidate_master=1
# check_repl_delay=0，表示忽略复制延迟，优先切换
check_repl_delay=0


[server2]
# 备主库节点
hostname=192.168.100.52
ip=192.168.100.52
candidate_master=1
check_repl_delay=0


[server3]
# 普通从库节点
hostname=192.168.100.53
ip=192.168.100.53
# no_master=1，表示该节点不是备选主节点
no_master=1
```

(3) 创建starry集群的MHA自定义脚本的目录，并往里面上传脚本：master_ip_failover、master_ip_online_change、power_manager、send_report
```shell
walter@master1:~$ sudo mkdir -p /etc/mha/starry/scripts
walter@master1:~$ sudo chmod a+x /etc/mha/starry/scripts/*
walter@manager:~$ ll /etc/mha/starry/scripts
total 44
drwxrwxrwx 2 root   root    4096 Jun 20 14:21 ./
drwxr-xr-x 3 root   root    4096 Jun 20 04:54 ../
-rwxrwxr-x 1 walter walter  4594 Jun 19 16:51 master_ip_failover*
-rwxrwxr-x 1 walter walter  9868 Jun 20 06:03 master_ip_online_change*
-rwxrwxr-x 1 walter walter 11873 Jun 20 06:14 power_manager*
-rwxrwxr-x 1 walter walter  1358 Jun 20 06:03 send_report*
```
> 注：以上脚本的示例，可从MHA-Manager的官网获取：
> https://github.com/yoshinorim/mha4mysql-manager/tree/master/samples/scripts

(4) 按需修改脚本master_ip_failover（配置vip及mysql节点的网卡名）
```shell
walter@master1:~$ cat /etc/mha/starry/scripts/master_ip_failover
#!/usr/bin/env perl

#  Copyright (C) 2011 DeNA Co.,Ltd.
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 2 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#   along with this program; if not, write to the Free Software
#  Foundation, Inc.,
#  51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

## Note: This is a sample script and is not complete. Modify the script based on your environment.

use strict;
use warnings FATAL => 'all';

use Getopt::Long;
use MHA::DBHelper;

my (
  $command,        $ssh_user,         $orig_master_host,
  $orig_master_ip, $orig_master_port, $new_master_host,
  $new_master_ip,  $new_master_port,  $new_master_user,
  $new_master_password
);

# MySQL cluster VIP for write request
my $vip = '192.168.100.50';
my $subnet_mask_bits = '24';
my $iface = 'enp0s3';
my $ssh_start_vip = "sudo ip addr add $vip/$subnet_mask_bits dev $iface > /dev/null 2>&1; sudo arping -U -I $iface -c 1 $vip > /dev/null 2>&1;";
my $ssh_stop_vip = "sudo ip addr del $vip/$subnet_mask_bits dev $iface > /dev/null 2>&1; sudo arping -U -I $iface -c 1 $vip > /dev/null 2>&1;";

GetOptions(
  'command=s'             => \$command,
  'ssh_user=s'            => \$ssh_user,
  'orig_master_host=s'    => \$orig_master_host,
  'orig_master_ip=s'      => \$orig_master_ip,
  'orig_master_port=i'    => \$orig_master_port,
  'new_master_host=s'     => \$new_master_host,
  'new_master_ip=s'       => \$new_master_ip,
  'new_master_port=i'     => \$new_master_port,
  'new_master_user=s'     => \$new_master_user,
  'new_master_password=s' => \$new_master_password,
);

exit &main();

sub main {
  if ( $command eq "stop" || $command eq "stopssh" ) {

    # $orig_master_host, $orig_master_ip, $orig_master_port are passed.
    # If you manage master ip address at global catalog database,
    # invalidate orig_master_ip here.
    my $exit_code = 1;
    eval {
      print "禁用旧主库的VIP: $orig_master_host \n";
      &stop_vip();
      # updating global catalog, etc
      $exit_code = 0;
    };
    if ($@) {
      warn "Got Error: $@\n";
      exit $exit_code;
    }
    exit $exit_code;
  }
  elsif ( $command eq "start" ) {

    # all arguments are passed.
    # If you manage master ip address at global catalog database,
    # activate new_master_ip here.
    # You can also grant write access (create user, set read_only=0, etc) here.
    my $exit_code = 10;
    eval {
      my $new_master_handler = new MHA::DBHelper();

      # args: hostname, port, user, password, raise_error_or_not
      $new_master_handler->connect( $new_master_ip, $new_master_port,
        $new_master_user, $new_master_password, 1 );

      ## Set read_only=0 on the new master
      $new_master_handler->disable_log_bin_local();
      print "Set read_only=0 on the new master.\n";
      $new_master_handler->disable_read_only();

      ## Creating an app user on the new master
      #print "Creating app user on the new master..\n";
      #FIXME_xxx_create_user( $new_master_handler->{dbh} );
      $new_master_handler->enable_log_bin_local();
      $new_master_handler->disconnect();

      ## Update master ip on the catalog database, etc
      #FIXME_xxx;
      print "启用VIP - $vip 到新主库 - $new_master_host \n";
      &start_vip();
      $exit_code = 0;
    };
    if ($@) {
      warn $@;

      # If you want to continue failover, exit 10.
      exit $exit_code;
    }
    exit $exit_code;
  }
  elsif ( $command eq "status" ) {
    print "Checking the Status of the script.. OK \n";
    # do nothing
    exit 0;
  }
  else {
    &usage();
    exit 1;
  }
}


# A simple system call that enable the VIP on the new master
sub start_vip() {
    `ssh $ssh_user\@$new_master_host \" $ssh_start_vip \"`;
}

# A simple system call that disable the VIP on the old_master
sub stop_vip() {
    return 0  unless  ($ssh_user);
    `ssh $ssh_user\@$orig_master_host \" $ssh_stop_vip \"`;
}

sub usage {
  print
"Usage: master_ip_failover --command=start|stop|stopssh|status --orig_master_host=host --orig_master_ip=ip --orig_master_port=port --new_master_host=host --new_master_ip=ip --new_master_port=port\n";
}
```

# 三、在MHA-Manager节点测试MHA配置是否有误
## 1. 验证 SSH 免密互通
```shell
walter@manager:/etc/mha/starry$ masterha_check_ssh --conf=/etc/mha/starry/app.cnf
Fri Jun 19 15:35:08 2026 - [warning] Global configuration file /etc/masterha_default.cnf not found. Skipping.
Fri Jun 19 15:35:08 2026 - [info] Reading application default configuration from /etc/mha/starry/app.cnf..
Fri Jun 19 15:35:08 2026 - [info] Reading server configuration from /etc/mha/starry/app.cnf..
Fri Jun 19 15:35:08 2026 - [info] Starting SSH connection tests..
Fri Jun 19 15:35:11 2026 - [debug] 
Fri Jun 19 15:35:08 2026 - [debug]  Connecting via SSH from walter@192.168.100.51(192.168.100.51:22) to walter@192.168.100.52(192.168.100.52:22)..
Fri Jun 19 15:35:09 2026 - [debug]   ok.
Fri Jun 19 15:35:09 2026 - [debug]  Connecting via SSH from walter@192.168.100.51(192.168.100.51:22) to walter@192.168.100.53(192.168.100.53:22)..
Fri Jun 19 15:35:11 2026 - [debug]   ok.
Fri Jun 19 15:35:12 2026 - [debug] 
Fri Jun 19 15:35:09 2026 - [debug]  Connecting via SSH from walter@192.168.100.53(192.168.100.53:22) to walter@192.168.100.51(192.168.100.51:22)..
Fri Jun 19 15:35:11 2026 - [debug]   ok.
Fri Jun 19 15:35:11 2026 - [debug]  Connecting via SSH from walter@192.168.100.53(192.168.100.53:22) to walter@192.168.100.52(192.168.100.52:22)..
Fri Jun 19 15:35:12 2026 - [debug]   ok.
Fri Jun 19 15:35:12 2026 - [debug] 
Fri Jun 19 15:35:08 2026 - [debug]  Connecting via SSH from walter@192.168.100.52(192.168.100.52:22) to walter@192.168.100.51(192.168.100.51:22)..
Fri Jun 19 15:35:10 2026 - [debug]   ok.
Fri Jun 19 15:35:10 2026 - [debug]  Connecting via SSH from walter@192.168.100.52(192.168.100.52:22) to walter@192.168.100.53(192.168.100.53:22)..
Fri Jun 19 15:35:12 2026 - [debug]   ok.
Fri Jun 19 15:35:12 2026 - [info] All SSH connection tests passed successfully.
Use of uninitialized value in exit at /usr/bin/masterha_check_ssh line 44.
```

## 2. 验证主从复制 + MHA 配置，核心验证，检查主从状态、MHA 配置、账号权限、脚本可用性：
```shell
walter@manager:/etc/mha/starry$ masterha_check_repl --conf=/etc/mha/starry/app.cnf
Fri Jun 19 16:52:42 2026 - [warning] Global configuration file /etc/masterha_default.cnf not found. Skipping.
Fri Jun 19 16:52:42 2026 - [info] Reading application default configuration from /etc/mha/starry/app.cnf..
Fri Jun 19 16:52:42 2026 - [info] Reading server configuration from /etc/mha/starry/app.cnf..
Fri Jun 19 16:52:42 2026 - [info] MHA::MasterMonitor version 0.58.
Fri Jun 19 16:52:43 2026 - [info] GTID failover mode = 1
Fri Jun 19 16:52:43 2026 - [info] Dead Servers:
Fri Jun 19 16:52:43 2026 - [info] Alive Servers:
Fri Jun 19 16:52:43 2026 - [info]   192.168.100.51(192.168.100.51:3306)
Fri Jun 19 16:52:43 2026 - [info]   192.168.100.52(192.168.100.52:3306)
Fri Jun 19 16:52:43 2026 - [info]   192.168.100.53(192.168.100.53:3306)
Fri Jun 19 16:52:43 2026 - [info] Alive Slaves:
Fri Jun 19 16:52:43 2026 - [info]   192.168.100.52(192.168.100.52:3306)  Version=8.0.46-0ubuntu0.24.04.2 (oldest major version between slaves) log-bin:enabled
Fri Jun 19 16:52:43 2026 - [info]     GTID ON
Fri Jun 19 16:52:43 2026 - [info]     Replicating from 192.168.100.51(192.168.100.51:3306)
Fri Jun 19 16:52:43 2026 - [info]     Primary candidate for the new Master (candidate_master is set)
Fri Jun 19 16:52:43 2026 - [info]   192.168.100.53(192.168.100.53:3306)  Version=8.0.46-0ubuntu0.24.04.2 (oldest major version between slaves) log-bin:enabled
Fri Jun 19 16:52:43 2026 - [info]     GTID ON
Fri Jun 19 16:52:43 2026 - [info]     Replicating from 192.168.100.51(192.168.100.51:3306)
Fri Jun 19 16:52:43 2026 - [info]     Not candidate for the new Master (no_master is set)
Fri Jun 19 16:52:43 2026 - [info] Current Alive Master: 192.168.100.51(192.168.100.51:3306)
Fri Jun 19 16:52:43 2026 - [info] Checking slave configurations..
Fri Jun 19 16:52:43 2026 - [info] Checking replication filtering settings..
Fri Jun 19 16:52:43 2026 - [info]  binlog_do_db= , binlog_ignore_db= 
Fri Jun 19 16:52:43 2026 - [info]  Replication filtering check ok.
Fri Jun 19 16:52:43 2026 - [info] GTID (with auto-pos) is supported. Skipping all SSH and Node package checking.
Fri Jun 19 16:52:43 2026 - [info] Checking SSH publickey authentication settings on the current master..
Fri Jun 19 16:52:44 2026 - [info] HealthCheck: SSH to 192.168.100.51 is reachable.
Fri Jun 19 16:52:44 2026 - [info] 
192.168.100.51(192.168.100.51:3306) (current master)
 +--192.168.100.52(192.168.100.52:3306)
 +--192.168.100.53(192.168.100.53:3306)

Fri Jun 19 16:52:44 2026 - [info] Checking replication health on 192.168.100.52..
Fri Jun 19 16:52:44 2026 - [info]  ok.
Fri Jun 19 16:52:44 2026 - [info] Checking replication health on 192.168.100.53..
Fri Jun 19 16:52:44 2026 - [info]  ok.
Fri Jun 19 16:52:44 2026 - [info] Checking master_ip_failover_script status:
Fri Jun 19 16:52:44 2026 - [info]   /etc/mha/starry/scripts/master_ip_failover --command=status --ssh_user=walter --orig_master_host=192.168.100.51 --orig_master_ip=192.168.100.51 --orig_master_port=3306 
Checking the Status of the script.. OK 
Fri Jun 19 16:52:44 2026 - [info]  OK.
Fri Jun 19 16:52:44 2026 - [warning] shutdown_script is not defined.
Fri Jun 19 16:52:44 2026 - [info] Got exit code 0 (Not master dead).

MySQL Replication Health is OK.
```
如有报错，请参考`附录`。



# 四、给主库节点上绑定VIP
```shell
walter@manager:~$ sudo ip addr add 192.168.100.50/24 dev enp0s3 > /dev/null 2>&1; sudo arping -U -I enp0s3 -c 1 192.168.100.50 > /dev/null 2>&1;
walter@master1:~$ ip addr
# 注意enp0s3网卡绑定了：inet 192.168.100.50/24 scope global secondary enp0s3

1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
       valid_lft forever preferred_lft forever
    inet6 ::1/128 scope host noprefixroute 
       valid_lft forever preferred_lft forever
2: enp0s3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UP group default qlen 1000
    link/ether 08:00:27:df:3c:b6 brd ff:ff:ff:ff:ff:ff
    inet 192.168.100.51/24 brd 192.168.100.255 scope global enp0s3
       valid_lft forever preferred_lft forever
    inet 192.168.100.50/24 scope global secondary enp0s3
       valid_lft forever preferred_lft forever
    inet6 fe80::a00:27ff:fedf:3cb6/64 scope link 
       valid_lft forever preferred_lft forever
walter@master1:~$ 
```

# 五、管理MHA Manager服务
## 1. 启动MHA Manager
```shell
walter@manager:~$ nohup masterha_manager --conf=/etc/mha/starry/app.cnf --ignore_last_failover &
walter@manager:~$ 
walter@manager:~$ tail -f /var/log/mha/starry/manager.log
Checking the Status of the script.. OK 
Sat Jun 20 07:59:08 2026 - [info]  OK.
Sat Jun 20 07:59:08 2026 - [warning] shutdown_script is not defined.
Sat Jun 20 07:59:08 2026 - [info] Set master ping interval 3 seconds.
Sat Jun 20 07:59:08 2026 - [info] Set secondary check script: masterha_secondary_check -s 192.168.100.53
Sat Jun 20 07:59:08 2026 - [info] Starting ping health check on 192.168.100.51(192.168.100.51:3306)..
Sat Jun 20 07:59:08 2026 - [info] Ping(SELECT) succeeded, waiting until MySQL doesn't respond..
```
注：如果不加--ignore_last_failover参数且{manager_workdir}下存在app.failover.complete文件，默认在8小时内不允许再次触发故障转移：
```shell
# 8小时内新主库再次发生故障……禁止故障转移
walter@manager:~$ tail -f /var/log/mha/starry/manager.log
Sat Jun 20 14:39:59 2026 - [error][/usr/share/perl5/MHA/MasterFailover.pm, ln310] Last failover was done at 2026/06/20 08:17:24. Current time is too early to do failover again. If you want to do failover, manually remove /var/log/mha/starry/app.failover.complete and run this script again.
Sat Jun 20 14:39:59 2026 - [error][/usr/share/perl5/MHA/ManagerUtil.pm, ln178] Got ERROR:  at /usr/bin/masterha_manager line 65.
^C
[1]+  Exit 1                  nohup masterha_manager --conf=/etc/mha/starry/app.cnf  (wd: ~)
(wd now: /var/log/mha/starry)
```

## 2. 停止MHA Manager
```shell
walter@manager:~$ masterha_stop --conf=/etc/mha/starry/app.cnf
Stopped app successfully.
[1]+  Exit 1                  nohup masterha_manager --conf=/etc/mha/starry/app.cnf --ignore_last_failover
```

## 3. 检查当前MHA服务的运行状态是running还是stopped
```shell
walter@manager:~$ masterha_check_status --conf=/etc/mha/starry/app.cnf
app is stopped(2:NOT_RUNNING).
```

# 六. 故障演练
## 1. 自动故障转移
(1) 停止主库的mysql服务
```shell
walter@manager:~$ sudo mysqladmin shutdown
```

(2) 监控故障转移过程
以下日志表示故障转移成功：
```shell
walter@manager:~$ tail -f /var/log/mha/starry/manager.log
Started automated(non-interactive) failover.
Invalidated master IP address on 192.168.100.51(192.168.100.51:3306)
Selected 192.168.100.52(192.168.100.52:3306) as a new master.
192.168.100.52(192.168.100.52:3306): OK: Applying all logs succeeded.
192.168.100.52(192.168.100.52:3306): OK: Activated master IP address.
192.168.100.53(192.168.100.53:3306): OK: Slave started, replicating from 192.168.100.52(192.168.100.52:3306)
192.168.100.52(192.168.100.52:3306): Resetting slave info succeeded.
Master failover to 192.168.100.52(192.168.100.52:3306) completed successfully.
```
关注到日志中CHANGE MASTER一句，待旧主节点恢复后，执行这个命令，旧主节点重新作为备主库进行主从复制：
```
Sat Jun 20 08:17:19 2026 - [info]  All other slaves should start replication from here. Statement should be: CHANGE MASTER TO MASTER_HOST='192.168.100.52', MASTER_PORT=3306, MASTER_AUTO_POSITION=1, MASTER_USER='repl', MASTER_PASSWORD='xxx';
Sat Jun 20 08:17:19 2026 - [info] Master Recovery succeeded. File:Pos:Exec_Gtid_Set: binlog.000006, 197, acf5c02b-6993-11f1-9c6c-080027df3cb6:1-13
```

(3) 旧主库重新作为备主库进行主从复制：
```shell
# 重启mysql服务
walter@master1:~$ sudo systemctl start mysql
```
根据管理节点的`/var/log/mha/starry/manager.log`提示，对旧主库重新配置主从复制：
```mysql
CHANGE MASTER TO MASTER_HOST='192.168.100.52', MASTER_PORT=3306, MASTER_AUTO_POSITION=1, MASTER_USER='repl', MASTER_PASSWORD='xxx';
START SLAVE;
```

(4) 故障转移后，masterha_manager进程会自动关闭，需要自行开启
```shell
walter@manager:~$ masterha_check_status --conf=/etc/mha/starry/app.cnf
app is stopped(2:NOT_RUNNING).
```


## 2. 把Master在线切换到192.168.100.52
```shell
walter@manager:~$ masterha_master_switch --master_state=alive --conf=/etc/mha/starry/app.cnf --new_master_host=192.168.100.52 --orig_master_is_new_slave --running_updates_limit=10000 --interactive=0
```
注意：此命令仅仅改变主从复制关系（包括read_only的值），不会漂移VIP


## 3. 安全地清除MySQL从库节点的中继日志
```shell
walter@slave1:~$ sudo purge_relay_logs --user=root --port=3306 --disable_relay_log_purge
2026-06-20 16:03:10: purge_relay_logs script started.
 Opening /var/lib/mysql/slave1-relay-bin.000001 ..
 Opening /var/lib/mysql/slave1-relay-bin.000002 ..
 Executing SET GLOBAL relay_log_purge=1; FLUSH LOGS; sleeping a few seconds so that SQL thread can delete older relay log files (if it keeps up); SET GLOBAL relay_log_purge=0; .. ok.
2026-06-20 16:03:13: All relay log purging operations succeeded.
walter@slave1:~$ 
```


# 七. 附录
## 1. masterha_check_repl报错的处理方法
(1) Redundant argument in sprintf at /usr/share/perl5/MHA/NodeUtil.pm line 195.
```shell
cat /usr/share/perl5/MHA/NodeUtil.pm

# 把这段代码：
sub parse_mysql_version($) {
  my $str = shift;
  my $result = sprintf( '%03d%03d%03d', $str =~ m/(\d+)/g );
  return $result;
}

# 改为：
sub parse_mysql_version($) {
  my $str = shift;
  ($str) =  $str =~ m/^[^-]*/g;
  my $result = sprintf( '%03d%03d%03d', $str =~ m/(\d+)/g );
  return $result;
}
```

(2) Redundant argument in sprintf at /usr/share/perl5/MHA/NodeUtil.pm line 201
```shell
cat /usr/share/perl5/MHA/NodeUtil.pm

# 把这段代码：
sub parse_mysql_major_version($) {
  my $str = shift;
  my $result = sprintf( '%03d%03d', $str =~ m/(\d+)/g );
  return $result;
}

#改为：
sub parse_mysql_major_version($) {
  my $str = shift;
  # my $result = sprintf( '%03d%03d', $str =~ m/(\d+)/g );
  $str =~ /(\d+)\.(\d+)/;
  my $strmajor = "$1.$2";
  my $result = sprintf( '%03d%03d', $strmajor =~ m/(\d+)/g );
  return $result;
}
```

## 2. 参考资料
- [（官网）mha4mysql-manager](https://github.com/yoshinorim/mha4mysql-manager)
- [（官网）mha4mysql-node](https://github.com/yoshinorim/mha4mysql-node)
- [MySQL高可用集群-MHA](https://www.modb.pro/db/74003)
- [MySQL 8.0+ MHA 高可用集群搭建（生产环境级・超详细）](https://blog.csdn.net/L162476/article/details/157397267)
