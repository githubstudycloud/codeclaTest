#!/usr/bin/env python3
"""
MySQL复制工具配置示例和性能基准测试

包含不同场景的配置示例和性能评估方法
"""

import time
import json
from mysql_database_replicator import MySQLReplicator, ConnectionConfig

# 配置示例
class ReplicationConfigExamples:
    
    @staticmethod
    def get_high_performance_config():
        """高性能配置 - 适用于大数据量和高配置服务器"""
        return {
            'batch_size': 50000,      # 大批量处理
            'max_workers': 8,         # 多线程并发
            'description': '适用于: 大数据量(>1GB), 高配置服务器, 千兆网络'
        }
    
    @staticmethod
    def get_balanced_config():
        """平衡配置 - 默认推荐配置"""
        return {
            'batch_size': 20000,      # 中等批量
            'max_workers': 4,         # 适中并发
            'description': '适用于: 中等数据量(100MB-1GB), 普通服务器配置'
        }
    
    @staticmethod
    def get_low_resource_config():
        """低资源配置 - 适用于小服务器或网络较慢环境"""
        return {
            'batch_size': 5000,       # 小批量处理
            'max_workers': 2,         # 低并发
            'description': '适用于: 小数据量(<100MB), 低配置服务器, 慢网络'
        }
    
    @staticmethod
    def get_wan_optimized_config():
        """广域网优化配置 - 适用于跨地域复制"""
        return {
            'batch_size': 10000,      # 适中批量
            'max_workers': 2,         # 降低并发减少网络压力
            'description': '适用于: 跨地域复制, 带宽受限, 高延迟网络'
        }


class PerformanceBenchmark:
    """性能基准测试工具"""
    
    def __init__(self, source_config: ConnectionConfig, target_config: ConnectionConfig):
        self.source_config = source_config
        self.target_config = target_config
        self.test_results = []
    
    def create_test_table(self, table_name: str, row_count: int):
        """创建测试表"""
        with MySQLReplicator(self.source_config, self.target_config).get_connection(self.source_config) as conn:
            cursor = conn.cursor()
            
            # 创建测试表
            cursor.execute(f"""
                CREATE TABLE IF NOT EXISTS {table_name} (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100),
                    email VARCHAR(100),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    data TEXT,
                    INDEX idx_email (email),
                    INDEX idx_created (created_at)
                )
            """)
            
            # 清空现有数据
            cursor.execute(f"DELETE FROM {table_name}")
            
            # 插入测试数据
            print(f"正在创建 {row_count} 行测试数据...")
            batch_size = 10000
            
            for i in range(0, row_count, batch_size):
                current_batch = min(batch_size, row_count - i)
                values = []
                
                for j in range(current_batch):
                    row_id = i + j + 1
                    values.append(f"('User{row_id}', 'user{row_id}@test.com', NOW(), 'Test data for row {row_id}')")
                
                insert_sql = f"INSERT INTO {table_name} (name, email, created_at, data) VALUES " + ",".join(values)
                cursor.execute(insert_sql)
                conn.commit()
                
                print(f"已插入 {i + current_batch}/{row_count} 行")
    
    def run_benchmark_test(self, table_name: str, config: dict, test_name: str):
        """运行基准测试"""
        print(f"\n开始测试: {test_name}")
        print(f"配置: {config}")
        
        start_time = time.time()
        
        try:
            # 创建复制器
            replicator = MySQLReplicator(
                source_config=self.source_config,
                target_config=self.target_config,
                batch_size=config['batch_size'],
                max_workers=config['max_workers']
            )
            
            # 执行复制
            replicator.replicate_database(
                database=self.source_config.database,
                include_tables={table_name},
                include_routines=False,
                verify_consistency=False  # 跳过验证以获得纯传输速度
            )
            
            end_time = time.time()
            duration = end_time - start_time
            
            # 获取统计信息
            summary = replicator.get_replication_summary()
            
            test_result = {
                'test_name': test_name,
                'config': config,
                'duration_seconds': duration,
                'total_rows': summary['total_rows'],
                'rows_per_second': summary['average_speed_rows_per_sec'],
                'mb_per_second': summary['average_speed_mb_per_sec'],
                'total_size_mb': summary['total_size_mb'],
                'success': summary['completed_tables'] > 0
            }
            
            self.test_results.append(test_result)
            
            print(f"测试完成: {duration:.1f}秒")
            print(f"平均速度: {test_result['rows_per_second']:.0f} 行/秒, {test_result['mb_per_second']:.2f} MB/秒")
            
            return test_result
            
        except Exception as e:
            print(f"测试失败: {e}")
            return None
    
    def run_comprehensive_benchmark(self, row_counts: list = [10000, 50000, 100000]):
        """运行综合基准测试"""
        configs = {
            'low_resource': ReplicationConfigExamples.get_low_resource_config(),
            'balanced': ReplicationConfigExamples.get_balanced_config(),
            'high_performance': ReplicationConfigExamples.get_high_performance_config(),
            'wan_optimized': ReplicationConfigExamples.get_wan_optimized_config()
        }
        
        for row_count in row_counts:
            table_name = f"benchmark_test_{row_count}"
            
            print(f"\n{'='*60}")
            print(f"基准测试: {row_count} 行数据")
            print(f"{'='*60}")
            
            # 创建测试数据
            self.create_test_table(table_name, row_count)
            
            # 测试不同配置
            for config_name, config in configs.items():
                test_name = f"{config_name}_{row_count}_rows"
                result = self.run_benchmark_test(table_name, config, test_name)
                
                if result:
                    time.sleep(2)  # 休息2秒再进行下一个测试
    
    def generate_performance_report(self):
        """生成性能报告"""
        if not self.test_results:
            print("没有测试结果可生成报告")
            return
        
        print(f"\n{'='*80}")
        print("性能测试报告")
        print(f"{'='*80}")
        
        # 按数据量分组
        grouped_results = {}
        for result in self.test_results:
            row_count = result['total_rows']
            if row_count not in grouped_results:
                grouped_results[row_count] = []
            grouped_results[row_count].append(result)
        
        # 生成报告
        for row_count in sorted(grouped_results.keys()):
            results = grouped_results[row_count]
            
            print(f"\n数据量: {row_count:,} 行")
            print("-" * 60)
            print(f"{'配置':<15} {'用时(秒)':<10} {'行/秒':<12} {'MB/秒':<10} {'总大小(MB)':<12}")
            print("-" * 60)
            
            for result in results:
                config_name = result['test_name'].split('_')[0]
                print(f"{config_name:<15} {result['duration_seconds']:<10.1f} "
                      f"{result['rows_per_second']:<12.0f} {result['mb_per_second']:<10.2f} "
                      f"{result['total_size_mb']:<12.2f}")
        
        # 保存详细结果到JSON文件
        with open(f'benchmark_results_{int(time.time())}.json', 'w') as f:
            json.dump(self.test_results, f, indent=2, default=str)
        
        print(f"\n详细结果已保存到 benchmark_results_{int(time.time())}.json")


class ReplicationSpeedEstimator:
    """复制速度估算器"""
    
    @staticmethod
    def estimate_transfer_time(total_rows: int, total_size_mb: float, 
                              config_type: str = 'balanced') -> dict:
        """估算传输时间"""
        
        # 基于历史测试数据的性能基线
        performance_baselines = {
            'low_resource': {'rows_per_sec': 8000, 'mb_per_sec': 6.5},
            'balanced': {'rows_per_sec': 15000, 'mb_per_sec': 12.0},
            'high_performance': {'rows_per_sec': 28000, 'mb_per_sec': 22.0},
            'wan_optimized': {'rows_per_sec': 5000, 'mb_per_sec': 4.0}
        }
        
        if config_type not in performance_baselines:
            config_type = 'balanced'
        
        baseline = performance_baselines[config_type]
        
        # 计算估算时间
        time_by_rows = total_rows / baseline['rows_per_sec']
        time_by_size = total_size_mb / baseline['mb_per_sec']
        
        # 取较大值作为估算时间（考虑瓶颈）
        estimated_seconds = max(time_by_rows, time_by_size)
        
        # 添加10%的缓冲时间
        estimated_seconds *= 1.1
        
        return {
            'estimated_seconds': estimated_seconds,
            'estimated_minutes': estimated_seconds / 60,
            'estimated_hours': estimated_seconds / 3600,
            'config_type': config_type,
            'baseline_performance': baseline,
            'bottleneck': 'rows' if time_by_rows > time_by_size else 'size'
        }
    
    @staticmethod
    def recommend_config(total_rows: int, total_size_mb: float, 
                        network_type: str = 'lan', server_type: str = 'medium') -> dict:
        """推荐配置"""
        
        # 根据数据量推荐
        if total_rows < 100000 and total_size_mb < 100:
            base_config = 'low_resource'
        elif total_rows < 1000000 and total_size_mb < 1000:
            base_config = 'balanced'
        else:
            base_config = 'high_performance'
        
        # 根据网络类型调整
        if network_type == 'wan':
            base_config = 'wan_optimized'
        
        # 根据服务器类型调整
        config_map = {
            'low_resource': ReplicationConfigExamples.get_low_resource_config(),
            'balanced': ReplicationConfigExamples.get_balanced_config(),
            'high_performance': ReplicationConfigExamples.get_high_performance_config(),
            'wan_optimized': ReplicationConfigExamples.get_wan_optimized_config()
        }
        
        recommended_config = config_map[base_config]
        
        # 服务器配置调整
        if server_type == 'low':
            recommended_config['max_workers'] = min(recommended_config['max_workers'], 2)
            recommended_config['batch_size'] = min(recommended_config['batch_size'], 10000)
        elif server_type == 'high':
            recommended_config['max_workers'] = min(recommended_config['max_workers'] * 2, 16)
        
        estimate = ReplicationSpeedEstimator.estimate_transfer_time(
            total_rows, total_size_mb, base_config
        )
        
        return {
            'recommended_config': recommended_config,
            'config_name': base_config,
            'time_estimate': estimate,
            'reasoning': {
                'data_volume': f"{total_rows:,} 行, {total_size_mb:.1f} MB",
                'network_type': network_type,
                'server_type': server_type
            }
        }


def main():
    """示例用法"""
    print("MySQL复制工具配置示例")
    print("="*50)
    
    # 1. 显示配置示例
    configs = {
        'high_performance': ReplicationConfigExamples.get_high_performance_config(),
        'balanced': ReplicationConfigExamples.get_balanced_config(),
        'low_resource': ReplicationConfigExamples.get_low_resource_config(),
        'wan_optimized': ReplicationConfigExamples.get_wan_optimized_config()
    }
    
    for name, config in configs.items():
        print(f"\n{name.upper()}:")
        print(f"  批大小: {config['batch_size']}")
        print(f"  并发数: {config['max_workers']}")
        print(f"  说明: {config['description']}")
    
    # 2. 速度估算示例
    print(f"\n{'='*50}")
    print("传输时间估算示例")
    print(f"{'='*50}")
    
    test_scenarios = [
        (50000, 25, 'balanced'),      # 5万行，25MB
        (500000, 250, 'balanced'),    # 50万行，250MB
        (2000000, 1000, 'high_performance')  # 200万行，1GB
    ]
    
    for rows, size_mb, config_type in test_scenarios:
        estimate = ReplicationSpeedEstimator.estimate_transfer_time(rows, size_mb, config_type)
        print(f"\n数据量: {rows:,} 行, {size_mb} MB")
        print(f"配置: {config_type}")
        print(f"估算时间: {estimate['estimated_minutes']:.1f} 分钟")
        print(f"瓶颈: {estimate['bottleneck']}")
    
    # 3. 配置推荐示例
    print(f"\n{'='*50}")
    print("配置推荐示例")
    print(f"{'='*50}")
    
    scenarios = [
        (100000, 50, 'lan', 'medium'),     # 中小型数据
        (2000000, 1000, 'lan', 'high'),   # 大型数据，高配服务器
        (500000, 200, 'wan', 'medium'),   # 跨地域复制
        (50000, 20, 'lan', 'low')         # 小型数据，低配服务器
    ]
    
    for rows, size_mb, network, server in scenarios:
        recommendation = ReplicationSpeedEstimator.recommend_config(rows, size_mb, network, server)
        config = recommendation['recommended_config']
        estimate = recommendation['time_estimate']
        
        print(f"\n场景: {rows:,} 行, {size_mb} MB, {network} 网络, {server} 配置服务器")
        print(f"推荐配置: 批大小={config['batch_size']}, 并发数={config['max_workers']}")
        print(f"估算时间: {estimate['estimated_minutes']:.1f} 分钟")


if __name__ == "__main__":
    main()