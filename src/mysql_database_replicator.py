#!/usr/bin/env python3
"""
MySQL数据库快速复制工具

功能:
1. 基于JDBC连接的MySQL 5.7数据库复制
2. 支持指定数据库复制
3. 支持表的包含/排除过滤
4. 支持存储过程、函数、视图、触发器复制
5. 多线程并行传输
6. 实时速度估算和进度显示
7. 数据一致性校验
8. 错误处理和恢复机制
"""

import pymysql
import threading
import time
import json
import argparse
import logging
import hashlib
from datetime import datetime, timedelta
from typing import List, Dict, Set, Tuple, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from queue import Queue
import re
import sys


@dataclass
class ConnectionConfig:
    """数据库连接配置"""
    host: str
    port: int
    user: str
    password: str
    database: str
    charset: str = 'utf8mb4'
    
    def to_dict(self):
        return {
            'host': self.host,
            'port': self.port,
            'user': self.user,
            'password': self.password,
            'database': self.database,
            'charset': self.charset,
            'autocommit': True
        }


@dataclass
class ReplicationStats:
    """复制统计信息"""
    start_time: datetime
    total_tables: int = 0
    completed_tables: int = 0
    total_rows: int = 0
    transferred_rows: int = 0
    total_size_mb: float = 0.0
    transferred_size_mb: float = 0.0
    current_table: str = ""
    errors: List[str] = None
    
    def __post_init__(self):
        if self.errors is None:
            self.errors = []
    
    @property
    def elapsed_time(self) -> timedelta:
        return datetime.now() - self.start_time
    
    @property
    def rows_per_second(self) -> float:
        elapsed = self.elapsed_time.total_seconds()
        return self.transferred_rows / elapsed if elapsed > 0 else 0
    
    @property
    def mb_per_second(self) -> float:
        elapsed = self.elapsed_time.total_seconds()
        return self.transferred_size_mb / elapsed if elapsed > 0 else 0
    
    @property
    def progress_percentage(self) -> float:
        return (self.transferred_rows / self.total_rows * 100) if self.total_rows > 0 else 0
    
    @property
    def eta(self) -> Optional[timedelta]:
        if self.transferred_rows == 0 or self.rows_per_second == 0:
            return None
        remaining_rows = self.total_rows - self.transferred_rows
        eta_seconds = remaining_rows / self.rows_per_second
        return timedelta(seconds=eta_seconds)


class MySQLReplicator:
    def __init__(self, source_config: ConnectionConfig, target_config: ConnectionConfig,
                 batch_size: int = 10000, max_workers: int = 4):
        self.source_config = source_config
        self.target_config = target_config
        self.batch_size = batch_size
        self.max_workers = max_workers
        self.stats = ReplicationStats(start_time=datetime.now())
        self.stop_event = threading.Event()
        
        # 设置日志
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(f'mysql_replication_{datetime.now().strftime("%Y%m%d_%H%M%S")}.log'),
                logging.StreamHandler(sys.stdout)
            ]
        )
        self.logger = logging.getLogger(__name__)
    
    def get_connection(self, config: ConnectionConfig, database: str = None):
        """获取数据库连接"""
        conn_config = config.to_dict()
        if database:
            conn_config['database'] = database
        return pymysql.connect(**conn_config)
    
    def get_databases(self, exclude_system_dbs: bool = True) -> List[str]:
        """获取所有数据库列表"""
        with self.get_connection(self.source_config) as conn:
            cursor = conn.cursor()
            cursor.execute("SHOW DATABASES")
            databases = [row[0] for row in cursor.fetchall()]
            
            if exclude_system_dbs:
                system_dbs = {'information_schema', 'performance_schema', 'mysql', 'sys'}
                databases = [db for db in databases if db not in system_dbs]
            
            return databases
    
    def get_tables(self, database: str, include_tables: Set[str] = None, 
                   exclude_tables: Set[str] = None) -> List[str]:
        """获取指定数据库的表列表"""
        with self.get_connection(self.source_config, database) as conn:
            cursor = conn.cursor()
            cursor.execute("SHOW TABLES")
            all_tables = [row[0] for row in cursor.fetchall()]
            
            # 应用过滤规则
            if include_tables:
                all_tables = [t for t in all_tables if t in include_tables]
            
            if exclude_tables:
                all_tables = [t for t in all_tables if t not in exclude_tables]
            
            return all_tables
    
    def get_table_info(self, database: str, table: str) -> Dict:
        """获取表的详细信息"""
        with self.get_connection(self.source_config, database) as conn:
            cursor = conn.cursor()
            
            # 获取表结构
            cursor.execute(f"SHOW CREATE TABLE `{table}`")
            create_sql = cursor.fetchone()[1]
            
            # 获取行数估算
            cursor.execute(f"SELECT COUNT(*) FROM `{table}`")
            row_count = cursor.fetchone()[0]
            
            # 获取表大小估算
            cursor.execute(f"""
                SELECT 
                    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
                FROM information_schema.tables 
                WHERE table_schema = '{database}' AND table_name = '{table}'
            """)
            result = cursor.fetchone()
            size_mb = result[0] if result and result[0] else 0
            
            return {
                'create_sql': create_sql,
                'row_count': row_count,
                'size_mb': float(size_mb) if size_mb else 0
            }
    
    def create_target_database(self, database: str):
        """创建目标数据库"""
        with self.get_connection(self.target_config) as conn:
            cursor = conn.cursor()
            try:
                cursor.execute(f"CREATE DATABASE IF NOT EXISTS `{database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
                self.logger.info(f"目标数据库已创建: {database}")
            except Exception as e:
                self.logger.error(f"创建数据库失败 {database}: {e}")
                raise
    
    def create_target_table(self, database: str, table: str, create_sql: str):
        """在目标数据库创建表"""
        with self.get_connection(self.target_config, database) as conn:
            cursor = conn.cursor()
            try:
                # 删除已存在的表
                cursor.execute(f"DROP TABLE IF EXISTS `{table}`")
                # 创建新表
                cursor.execute(create_sql)
                self.logger.info(f"目标表已创建: {database}.{table}")
            except Exception as e:
                self.logger.error(f"创建表失败 {database}.{table}: {e}")
                raise
    
    def get_table_columns(self, database: str, table: str) -> List[str]:
        """获取表的列名列表"""
        with self.get_connection(self.source_config, database) as conn:
            cursor = conn.cursor()
            cursor.execute(f"DESCRIBE `{table}`")
            return [row[0] for row in cursor.fetchall()]
    
    def transfer_table_data(self, database: str, table: str, table_info: Dict):
        """传输单个表的数据"""
        total_rows = table_info['row_count']
        if total_rows == 0:
            self.logger.info(f"表 {database}.{table} 为空，跳过数据传输")
            return
        
        self.stats.current_table = f"{database}.{table}"
        columns = self.get_table_columns(database, table)
        column_list = '`, `'.join(columns)
        
        transferred_rows = 0
        
        try:
            with self.get_connection(self.source_config, database) as source_conn:
                with self.get_connection(self.target_config, database) as target_conn:
                    source_cursor = source_conn.cursor()
                    target_cursor = target_conn.cursor()
                    
                    # 分批传输数据
                    offset = 0
                    while offset < total_rows and not self.stop_event.is_set():
                        # 从源数据库读取数据
                        select_sql = f"SELECT `{column_list}` FROM `{table}` LIMIT {self.batch_size} OFFSET {offset}"
                        source_cursor.execute(select_sql)
                        rows = source_cursor.fetchall()
                        
                        if not rows:
                            break
                        
                        # 准备插入语句
                        placeholders = ', '.join(['%s'] * len(columns))
                        insert_sql = f"INSERT INTO `{table}` (`{column_list}`) VALUES ({placeholders})"
                        
                        # 批量插入到目标数据库
                        target_cursor.executemany(insert_sql, rows)
                        target_conn.commit()
                        
                        transferred_rows += len(rows)
                        offset += self.batch_size
                        
                        # 更新统计信息
                        self.stats.transferred_rows += len(rows)
                        self.stats.transferred_size_mb += (len(rows) / total_rows) * table_info['size_mb']
                        
                        # 显示进度
                        progress = (transferred_rows / total_rows) * 100
                        self.logger.info(f"表 {database}.{table}: {transferred_rows}/{total_rows} ({progress:.1f}%)")
        
        except Exception as e:
            error_msg = f"传输表数据失败 {database}.{table}: {e}"
            self.logger.error(error_msg)
            self.stats.errors.append(error_msg)
            raise
        
        finally:
            self.stats.completed_tables += 1
    
    def get_routines(self, database: str, routine_type: str = 'PROCEDURE') -> List[Dict]:
        """获取存储过程或函数列表"""
        with self.get_connection(self.source_config, database) as conn:
            cursor = conn.cursor()
            cursor.execute(f"""
                SELECT ROUTINE_NAME, ROUTINE_DEFINITION 
                FROM information_schema.ROUTINES 
                WHERE ROUTINE_SCHEMA = '{database}' AND ROUTINE_TYPE = '{routine_type}'
            """)
            
            routines = []
            for row in cursor.fetchall():
                routine_name = row[0]
                # 获取完整的创建语句
                cursor.execute(f"SHOW CREATE {routine_type} `{routine_name}`")
                create_result = cursor.fetchone()
                if create_result:
                    create_sql = create_result[2]  # CREATE语句在第3列
                    routines.append({
                        'name': routine_name,
                        'create_sql': create_sql
                    })
            
            return routines
    
    def get_views(self, database: str) -> List[Dict]:
        """获取视图列表"""
        with self.get_connection(self.source_config, database) as conn:
            cursor = conn.cursor()
            cursor.execute(f"SHOW FULL TABLES IN `{database}` WHERE Table_type = 'VIEW'")
            views = []
            
            for row in cursor.fetchall():
                view_name = row[0]
                cursor.execute(f"SHOW CREATE VIEW `{view_name}`")
                create_result = cursor.fetchone()
                if create_result:
                    views.append({
                        'name': view_name,
                        'create_sql': create_result[1]
                    })
            
            return views
    
    def get_triggers(self, database: str) -> List[Dict]:
        """获取触发器列表"""
        with self.get_connection(self.source_config, database) as conn:
            cursor = conn.cursor()
            cursor.execute(f"SHOW TRIGGERS FROM `{database}`")
            triggers = []
            
            for row in cursor.fetchall():
                trigger_name = row[0]
                cursor.execute(f"SHOW CREATE TRIGGER `{trigger_name}`")
                create_result = cursor.fetchone()
                if create_result:
                    triggers.append({
                        'name': trigger_name,
                        'create_sql': create_result[2]
                    })
            
            return triggers
    
    def create_routines(self, database: str, routines: List[Dict], routine_type: str):
        """创建存储过程或函数"""
        with self.get_connection(self.target_config, database) as conn:
            cursor = conn.cursor()
            
            for routine in routines:
                try:
                    # 删除已存在的例程
                    cursor.execute(f"DROP {routine_type} IF EXISTS `{routine['name']}`")
                    # 创建新例程
                    cursor.execute(routine['create_sql'])
                    self.logger.info(f"{routine_type} {routine['name']} 已创建")
                except Exception as e:
                    error_msg = f"创建{routine_type}失败 {routine['name']}: {e}"
                    self.logger.error(error_msg)
                    self.stats.errors.append(error_msg)
    
    def create_views_and_triggers(self, database: str):
        """创建视图和触发器"""
        # 创建视图
        views = self.get_views(database)
        with self.get_connection(self.target_config, database) as conn:
            cursor = conn.cursor()
            for view in views:
                try:
                    cursor.execute(f"DROP VIEW IF EXISTS `{view['name']}`")
                    cursor.execute(view['create_sql'])
                    self.logger.info(f"视图 {view['name']} 已创建")
                except Exception as e:
                    error_msg = f"创建视图失败 {view['name']}: {e}"
                    self.logger.error(error_msg)
                    self.stats.errors.append(error_msg)
        
        # 创建触发器
        triggers = self.get_triggers(database)
        with self.get_connection(self.target_config, database) as conn:
            cursor = conn.cursor()
            for trigger in triggers:
                try:
                    cursor.execute(f"DROP TRIGGER IF EXISTS `{trigger['name']}`")
                    cursor.execute(trigger['create_sql'])
                    self.logger.info(f"触发器 {trigger['name']} 已创建")
                except Exception as e:
                    error_msg = f"创建触发器失败 {trigger['name']}: {e}"
                    self.logger.error(error_msg)
                    self.stats.errors.append(error_msg)
    
    def verify_data_consistency(self, database: str, table: str) -> bool:
        """验证数据一致性"""
        try:
            with self.get_connection(self.source_config, database) as source_conn:
                with self.get_connection(self.target_config, database) as target_conn:
                    source_cursor = source_conn.cursor()
                    target_cursor = target_conn.cursor()
                    
                    # 比较行数
                    source_cursor.execute(f"SELECT COUNT(*) FROM `{table}`")
                    source_count = source_cursor.fetchone()[0]
                    
                    target_cursor.execute(f"SELECT COUNT(*) FROM `{table}`")
                    target_count = target_cursor.fetchone()[0]
                    
                    if source_count != target_count:
                        self.logger.error(f"数据一致性检查失败 {database}.{table}: 源({source_count}) != 目标({target_count})")
                        return False
                    
                    # 简单的校验和比较（针对小表）
                    if source_count < 100000:  # 只对小于10万行的表进行校验和检查
                        source_cursor.execute(f"SELECT MD5(GROUP_CONCAT(CONCAT_WS('|', *) ORDER BY 1)) FROM `{table}`")
                        source_hash = source_cursor.fetchone()[0]
                        
                        target_cursor.execute(f"SELECT MD5(GROUP_CONCAT(CONCAT_WS('|', *) ORDER BY 1)) FROM `{table}`")
                        target_hash = target_cursor.fetchone()[0]
                        
                        if source_hash != target_hash:
                            self.logger.error(f"数据校验和不匹配 {database}.{table}")
                            return False
                    
                    self.logger.info(f"数据一致性检查通过 {database}.{table}")
                    return True
        
        except Exception as e:
            self.logger.error(f"数据一致性检查失败 {database}.{table}: {e}")
            return False
    
    def print_progress(self):
        """打印进度信息"""
        while not self.stop_event.is_set():
            time.sleep(5)  # 每5秒更新一次
            
            elapsed = self.stats.elapsed_time
            progress = self.stats.progress_percentage
            eta = self.stats.eta
            
            print(f"\n{'='*60}")
            print(f"复制进度: {progress:.1f}% ({self.stats.transferred_rows}/{self.stats.total_rows} 行)")
            print(f"当前表: {self.stats.current_table}")
            print(f"已完成表: {self.stats.completed_tables}/{self.stats.total_tables}")
            print(f"传输速度: {self.stats.rows_per_second:.0f} 行/秒, {self.stats.mb_per_second:.2f} MB/秒")
            print(f"已用时间: {str(elapsed).split('.')[0]}")
            if eta:
                print(f"预计剩余: {str(eta).split('.')[0]}")
            print(f"数据传输: {self.stats.transferred_size_mb:.2f}/{self.stats.total_size_mb:.2f} MB")
            if self.stats.errors:
                print(f"错误数量: {len(self.stats.errors)}")
            print(f"{'='*60}")
    
    def replicate_database(self, database: str, include_tables: Set[str] = None, 
                          exclude_tables: Set[str] = None, include_routines: bool = True,
                          verify_consistency: bool = True):
        """复制指定数据库"""
        self.logger.info(f"开始复制数据库: {database}")
        
        try:
            # 1. 创建目标数据库
            self.create_target_database(database)
            
            # 2. 获取表列表
            tables = self.get_tables(database, include_tables, exclude_tables)
            self.stats.total_tables = len(tables)
            
            if not tables:
                self.logger.warning(f"数据库 {database} 中没有找到需要复制的表")
                return
            
            # 3. 收集表信息和统计
            table_infos = {}
            for table in tables:
                table_info = self.get_table_info(database, table)
                table_infos[table] = table_info
                self.stats.total_rows += table_info['row_count']
                self.stats.total_size_mb += table_info['size_mb']
            
            self.logger.info(f"准备复制 {len(tables)} 个表，总计 {self.stats.total_rows} 行，{self.stats.total_size_mb:.2f} MB")
            
            # 4. 启动进度显示线程
            progress_thread = threading.Thread(target=self.print_progress)
            progress_thread.daemon = True
            progress_thread.start()
            
            # 5. 创建表结构
            for table in tables:
                self.create_target_table(database, table, table_infos[table]['create_sql'])
            
            # 6. 并行传输数据
            with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
                future_to_table = {
                    executor.submit(self.transfer_table_data, database, table, table_infos[table]): table
                    for table in tables
                }
                
                for future in as_completed(future_to_table):
                    table = future_to_table[future]
                    try:
                        future.result()
                        self.logger.info(f"表 {database}.{table} 复制完成")
                    except Exception as e:
                        self.logger.error(f"表 {database}.{table} 复制失败: {e}")
            
            # 7. 复制存储过程、函数、视图、触发器
            if include_routines:
                self.logger.info("开始复制存储过程和函数...")
                procedures = self.get_routines(database, 'PROCEDURE')
                functions = self.get_routines(database, 'FUNCTION')
                
                self.create_routines(database, procedures, 'PROCEDURE')
                self.create_routines(database, functions, 'FUNCTION')
                self.create_views_and_triggers(database)
            
            # 8. 数据一致性验证
            if verify_consistency:
                self.logger.info("开始数据一致性验证...")
                consistency_failures = []
                for table in tables:
                    if not self.verify_data_consistency(database, table):
                        consistency_failures.append(table)
                
                if consistency_failures:
                    self.logger.error(f"以下表数据一致性验证失败: {consistency_failures}")
                else:
                    self.logger.info("所有表数据一致性验证通过")
            
            # 停止进度显示
            self.stop_event.set()
            
            self.logger.info(f"数据库 {database} 复制完成！")
            
        except Exception as e:
            self.logger.error(f"复制数据库失败 {database}: {e}")
            self.stop_event.set()
            raise
    
    def get_replication_summary(self) -> Dict:
        """获取复制摘要信息"""
        elapsed = self.stats.elapsed_time
        return {
            'total_time': str(elapsed).split('.')[0],
            'total_tables': self.stats.total_tables,
            'completed_tables': self.stats.completed_tables,
            'total_rows': self.stats.total_rows,
            'transferred_rows': self.stats.transferred_rows,
            'total_size_mb': self.stats.total_size_mb,
            'transferred_size_mb': self.stats.transferred_size_mb,
            'average_speed_rows_per_sec': self.stats.rows_per_second,
            'average_speed_mb_per_sec': self.stats.mb_per_second,
            'success_rate': (self.stats.completed_tables / self.stats.total_tables * 100) if self.stats.total_tables > 0 else 0,
            'errors': self.stats.errors
        }


def parse_table_filter(filter_str: str) -> Set[str]:
    """解析表过滤字符串"""
    if not filter_str:
        return set()
    return set(table.strip() for table in filter_str.split(','))


def main():
    parser = argparse.ArgumentParser(description='MySQL数据库快速复制工具')
    
    # 源数据库配置
    parser.add_argument('--src-host', required=True, help='源数据库主机')
    parser.add_argument('--src-port', type=int, default=3306, help='源数据库端口')
    parser.add_argument('--src-user', required=True, help='源数据库用户名')
    parser.add_argument('--src-password', required=True, help='源数据库密码')
    parser.add_argument('--src-database', required=True, help='源数据库名')
    
    # 目标数据库配置
    parser.add_argument('--dst-host', required=True, help='目标数据库主机')
    parser.add_argument('--dst-port', type=int, default=3306, help='目标数据库端口')
    parser.add_argument('--dst-user', required=True, help='目标数据库用户名')
    parser.add_argument('--dst-password', required=True, help='目标数据库密码')
    parser.add_argument('--dst-database', help='目标数据库名（默认与源数据库相同）')
    
    # 过滤配置
    parser.add_argument('--include-tables', help='包含的表名列表（逗号分隔）')
    parser.add_argument('--exclude-tables', help='排除的表名列表（逗号分隔）')
    parser.add_argument('--no-routines', action='store_true', help='不复制存储过程、函数、视图、触发器')
    parser.add_argument('--no-verify', action='store_true', help='跳过数据一致性验证')
    
    # 性能配置
    parser.add_argument('--batch-size', type=int, default=10000, help='批处理大小')
    parser.add_argument('--max-workers', type=int, default=4, help='最大并发线程数')
    
    args = parser.parse_args()
    
    # 构建连接配置
    source_config = ConnectionConfig(
        host=args.src_host,
        port=args.src_port,
        user=args.src_user,
        password=args.src_password,
        database=args.src_database
    )
    
    target_database = args.dst_database if args.dst_database else args.src_database
    target_config = ConnectionConfig(
        host=args.dst_host,
        port=args.dst_port,
        user=args.dst_user,
        password=args.dst_password,
        database=target_database
    )
    
    # 解析表过滤规则
    include_tables = parse_table_filter(args.include_tables)
    exclude_tables = parse_table_filter(args.exclude_tables)
    
    try:
        # 创建复制器
        replicator = MySQLReplicator(
            source_config=source_config,
            target_config=target_config,
            batch_size=args.batch_size,
            max_workers=args.max_workers
        )
        
        print(f"开始复制数据库: {args.src_database} -> {target_database}")
        print(f"源数据库: {args.src_host}:{args.src_port}")
        print(f"目标数据库: {args.dst_host}:{args.dst_port}")
        print(f"批大小: {args.batch_size}, 并发数: {args.max_workers}")
        
        if include_tables:
            print(f"包含表: {include_tables}")
        if exclude_tables:
            print(f"排除表: {exclude_tables}")
        
        # 执行复制
        replicator.replicate_database(
            database=args.src_database,
            include_tables=include_tables if include_tables else None,
            exclude_tables=exclude_tables if exclude_tables else None,
            include_routines=not args.no_routines,
            verify_consistency=not args.no_verify
        )
        
        # 输出摘要信息
        summary = replicator.get_replication_summary()
        print(f"\n{'='*60}")
        print("复制完成摘要:")
        print(f"总用时: {summary['total_time']}")
        print(f"完成表数: {summary['completed_tables']}/{summary['total_tables']}")
        print(f"传输行数: {summary['transferred_rows']}/{summary['total_rows']}")
        print(f"传输数据: {summary['transferred_size_mb']:.2f}/{summary['total_size_mb']:.2f} MB")
        print(f"平均速度: {summary['average_speed_rows_per_sec']:.0f} 行/秒, {summary['average_speed_mb_per_sec']:.2f} MB/秒")
        print(f"成功率: {summary['success_rate']:.1f}%")
        
        if summary['errors']:
            print(f"\n错误列表:")
            for error in summary['errors']:
                print(f"  - {error}")
        
        print(f"{'='*60}")
        
    except Exception as e:
        print(f"复制失败: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()