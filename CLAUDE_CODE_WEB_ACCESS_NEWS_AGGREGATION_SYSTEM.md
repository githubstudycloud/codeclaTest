# Claude Code网页访问机制分析与智能新闻聚合系统设计

## 概述

本文深入分析Claude Code的网页访问机制，并基于其工具生态设计一套完整的领域新闻聚合与智能总结系统。通过技术剖析和实际案例，展示如何构建高效的信息收集与处理管道。

## 目录
1. [Claude Code网页访问机制分析](#claude-code网页访问机制分析)
2. [工具生态系统架构](#工具生态系统架构)
3. [新闻聚合系统设计](#新闻聚合系统设计)
4. [智能总结引擎](#智能总结引擎)
5. [实施方案与代码实现](#实施方案与代码实现)
6. [部署与运维](#部署与运维)
7. [性能优化策略](#性能优化策略)
8. [扩展与未来发展](#扩展与未来发展)

## Claude Code网页访问机制分析

### 1. 核心访问工具分析

#### WebFetch工具机制
Claude Code的WebFetch工具是其访问网页内容的核心组件，具有以下特性：

```python
# WebFetch工具的核心工作流程
class WebFetchMechanism:
    """
    Claude Code WebFetch工具工作机制分析
    """
    
    def __init__(self):
        self.capabilities = {
            "content_fetching": "获取网页HTML内容",
            "markdown_conversion": "将HTML转换为Markdown格式", 
            "ai_processing": "使用小型快速模型处理内容",
            "caching": "15分钟自清理缓存机制",
            "redirect_handling": "处理HTTP重定向",
            "security_filtering": "内容安全过滤"
        }
    
    def fetch_workflow(self, url, prompt):
        """
        WebFetch工作流程
        """
        steps = [
            "1. URL验证和标准化",
            "2. 检查缓存是否存在有效内容", 
            "3. 发送HTTP请求获取内容",
            "4. 处理重定向和错误",
            "5. HTML到Markdown转换",
            "6. 内容安全检查和过滤",
            "7. 使用AI模型处理prompt",
            "8. 返回处理结果并缓存"
        ]
        return steps
    
    def technical_limitations(self):
        """
        技术限制分析
        """
        return {
            "cache_duration": "15分钟自动清理",
            "content_size_limit": "大内容会被截断",
            "rate_limiting": "请求频率限制",
            "geographic_restrictions": "某些地区可能受限",
            "javascript_rendering": "不支持JavaScript渲染",
            "authentication": "不支持需要登录的页面"
        }
```

#### WebSearch工具特性
```python
class WebSearchMechanism:
    """
    Claude Code WebSearch工具分析
    """
    
    def __init__(self):
        self.features = {
            "search_scope": "仅在美国可用",
            "result_format": "搜索结果块格式",
            "domain_filtering": "支持域名包含/排除",
            "real_time_access": "访问最新信息",
            "knowledge_cutoff": "突破知识截止限制"
        }
    
    def search_capabilities(self):
        return {
            "allowed_domains": "白名单域名筛选",
            "blocked_domains": "黑名单域名排除", 
            "query_optimization": "自动查询优化",
            "result_ranking": "智能结果排序",
            "content_extraction": "关键信息提取"
        }
```

### 2. 网页访问架构分析

#### 技术架构图
```
┌─────────────────────────────────────────────────────────┐
│                Claude Code Runtime                      │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │ WebFetch    │  │ WebSearch   │  │ Other Tools │      │
│  │ Tool        │  │ Tool        │  │             │      │
│  └─────┬───────┘  └─────┬───────┘  └─────────────┘      │
├────────┼──────────────────┼────────────────────────────────┤
│        │                  │                             │
│  ┌─────▼───────┐    ┌─────▼───────┐                     │
│  │HTTP Client  │    │Search API   │                     │
│  │             │    │Interface    │                     │
│  └─────┬───────┘    └─────┬───────┘                     │
├────────┼──────────────────┼────────────────────────────────┤
│        │                  │                             │
│  ┌─────▼───────┐    ┌─────▼───────┐                     │
│  │Content      │    │Result       │                     │
│  │Processing   │    │Processing   │                     │
│  │Engine       │    │Engine       │                     │
│  └─────┬───────┘    └─────┬───────┘                     │
├────────┼──────────────────┼────────────────────────────────┤
│        │                  │                             │
│  ┌─────▼──────────────────▼───────┐                     │
│  │       AI Processing Layer      │                     │
│  │    (Small, Fast Model)         │                     │
│  └─────┬──────────────────────────┘                     │
├────────┼────────────────────────────────────────────────────┤
│        │                                                │
│  ┌─────▼───────┐                                        │
│  │Cache Layer  │                                        │
│  │(15min TTL)  │                                        │
│  └─────────────┘                                        │
└─────────────────────────────────────────────────────────┘
```

#### 内容处理管道
```python
class ContentProcessingPipeline:
    """
    Claude Code内容处理管道
    """
    
    def __init__(self):
        self.pipeline_stages = [
            "URL解析与验证",
            "HTTP请求发送",
            "响应内容获取", 
            "HTML解析与清理",
            "Markdown转换",
            "内容结构化",
            "AI模型处理",
            "结果格式化",
            "缓存存储"
        ]
    
    def html_to_markdown_conversion(self, html_content):
        """
        HTML到Markdown转换机制
        """
        conversion_rules = {
            "heading_preservation": "保留标题层级结构",
            "link_extraction": "提取并保留链接信息",
            "image_handling": "处理图片标签和alt文本",
            "table_conversion": "表格到Markdown表格转换",
            "code_block_preservation": "保留代码块格式",
            "list_structure_maintenance": "维护列表结构",
            "noise_removal": "移除广告和无关内容"
        }
        return conversion_rules
    
    def ai_processing_capabilities(self):
        """
        AI处理能力分析
        """
        return {
            "content_summarization": "内容摘要生成",
            "key_information_extraction": "关键信息提取",
            "sentiment_analysis": "情感分析",
            "entity_recognition": "实体识别",
            "topic_classification": "主题分类",
            "relevance_scoring": "相关性评分"
        }
```

## 工具生态系统架构

### 1. 工具组合策略

#### 多工具协同机制
```python
class ToolEcosystemAnalysis:
    """
    Claude Code工具生态系统分析
    """
    
    def __init__(self):
        self.tool_categories = {
            "web_access_tools": {
                "WebFetch": "网页内容获取",
                "WebSearch": "搜索引擎查询"
            },
            "file_operations": {
                "Read": "文件读取",
                "Write": "文件写入", 
                "Edit": "文件编辑",
                "Glob": "文件模式匹配"
            },
            "code_execution": {
                "Bash": "命令行执行",
                "Task": "代理任务执行"
            },
            "data_processing": {
                "Grep": "文本搜索",
                "TodoWrite": "任务管理"
            }
        }
    
    def tool_integration_patterns(self):
        """
        工具集成模式
        """
        return {
            "sequential_processing": {
                "description": "顺序处理模式",
                "example": "WebSearch -> WebFetch -> Write",
                "use_case": "信息收集和存储"
            },
            "parallel_execution": {
                "description": "并行执行模式", 
                "example": "多个WebFetch同时执行",
                "use_case": "批量内容获取"
            },
            "conditional_branching": {
                "description": "条件分支模式",
                "example": "基于搜索结果决定后续操作",
                "use_case": "智能决策流程"
            },
            "iterative_refinement": {
                "description": "迭代优化模式",
                "example": "循环搜索和过滤",
                "use_case": "精确信息定位"
            }
        }
```

### 2. 工具链设计原则

#### 效率优化策略
```python
class ToolChainOptimization:
    """
    工具链优化策略
    """
    
    def __init__(self):
        self.optimization_principles = {
            "batch_operations": "批量操作减少调用次数",
            "intelligent_caching": "智能缓存避免重复请求",
            "error_handling": "错误处理和重试机制",
            "rate_limiting": "请求速率控制",
            "resource_management": "资源使用优化"
        }
    
    def performance_best_practices(self):
        return {
            "minimize_api_calls": {
                "strategy": "合并多个小请求为单个大请求",
                "example": "使用Glob一次性匹配多个文件模式"
            },
            "leverage_parallelism": {
                "strategy": "利用并行执行能力",
                "example": "同时调用多个WebFetch获取不同URL"
            },
            "smart_filtering": {
                "strategy": "早期过滤减少处理量",
                "example": "使用domain filtering限制搜索范围"
            },
            "incremental_processing": {
                "strategy": "增量处理大量数据",
                "example": "分批处理新闻源列表"
            }
        }
```

## 新闻聚合系统设计

### 1. 系统架构设计

#### 整体架构图
```
┌─────────────────────────────────────────────────────────────────┐
│                    新闻聚合系统架构                              │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   数据源    │  │   调度器    │  │  存储层     │             │
│  │   管理      │  │   系统      │  │            │             │
│  └─────┬───────┘  └─────┬───────┘  └─────┬───────┘             │
├────────┼──────────────────┼──────────────────┼───────────────────┤
│        │                  │                  │                 │
│  ┌─────▼───────┐    ┌─────▼───────┐    ┌─────▼───────┐         │
│  │新闻源发现   │    │内容抓取     │    │结构化存储   │         │
│  │和验证       │    │引擎         │    │引擎         │         │
│  └─────┬───────┘    └─────┬───────┘    └─────┬───────┘         │
├────────┼──────────────────┼──────────────────┼───────────────────┤
│        │                  │                  │                 │
│  ┌─────▼──────────────────▼──────────────────▼───────┐         │
│  │              内容处理中心                         │         │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │         │
│  │  │去重过滤 │ │内容清洗 │ │实体提取 │ │分类标记 │   │         │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘   │         │
│  └─────┬───────────────────────────────────────────────┘         │
├────────┼─────────────────────────────────────────────────────────┤
│        │                                                        │
│  ┌─────▼───────┐                                                │
│  │AI总结引擎   │                                                │
│  │             │                                                │
│  └─────┬───────┘                                                │
├────────┼─────────────────────────────────────────────────────────┤
│        │                                                        │
│  ┌─────▼───────┐  ┌─────────────┐  ┌─────────────┐             │
│  │输出生成     │  │订阅推送     │  │API服务      │             │
│  │引擎         │  │系统         │  │层           │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

#### 核心组件设计
```python
class NewsAggregationSystem:
    """
    新闻聚合系统核心架构
    """
    
    def __init__(self):
        self.system_components = {
            "source_manager": {
                "description": "新闻源管理器",
                "responsibilities": [
                    "RSS源发现和验证",
                    "网站结构分析",
                    "更新频率检测", 
                    "源质量评估",
                    "失效源清理"
                ]
            },
            "content_crawler": {
                "description": "内容抓取引擎",
                "responsibilities": [
                    "多源并行抓取",
                    "增量更新检测",
                    "反爬虫处理",
                    "内容完整性检查",
                    "错误重试机制"
                ]
            },
            "processing_engine": {
                "description": "内容处理引擎",
                "responsibilities": [
                    "重复内容识别",
                    "内容标准化",
                    "实体抽取",
                    "主题分类",
                    "质量评分"
                ]
            },
            "ai_summarizer": {
                "description": "AI总结引擎",
                "responsibilities": [
                    "单篇文章摘要",
                    "主题聚合总结",
                    "趋势分析",
                    "关键事件提取",
                    "观点整合"
                ]
            },
            "output_generator": {
                "description": "输出生成器",
                "responsibilities": [
                    "报告格式化",
                    "多格式输出",
                    "个性化定制",
                    "分发调度",
                    "存档管理"
                ]
            }
        }
    
    def data_flow_design(self):
        """
        数据流设计
        """
        return {
            "input_phase": {
                "sources": ["RSS feeds", "News websites", "Social media", "APIs"],
                "discovery": "自动发现新源",
                "validation": "源质量验证"
            },
            "processing_phase": {
                "extraction": "内容提取和清洗",
                "deduplication": "去重和合并",
                "enrichment": "内容增强和标注",
                "classification": "自动分类和标签"
            },
            "analysis_phase": {
                "summarization": "智能摘要生成",
                "trend_analysis": "趋势分析",
                "sentiment_analysis": "情感分析",
                "impact_assessment": "影响力评估"
            },
            "output_phase": {
                "formatting": "多格式输出",
                "personalization": "个性化定制",
                "distribution": "多渠道分发",
                "feedback": "用户反馈收集"
            }
        }
```

### 2. 数据源管理策略

#### 新闻源发现与管理
```python
class NewsSourceManager:
    """
    新闻源管理器
    """
    
    def __init__(self):
        self.source_types = {
            "rss_feeds": {
                "description": "RSS/Atom订阅源",
                "advantages": ["标准化格式", "易于解析", "更新及时"],
                "examples": [
                    "https://feeds.bbci.co.uk/news/technology/rss.xml",
                    "https://techcrunch.com/feed/", 
                    "https://www.wired.com/feed/"
                ]
            },
            "news_websites": {
                "description": "新闻网站直接抓取",
                "advantages": ["内容丰富", "格式多样", "覆盖面广"],
                "challenges": ["反爬虫", "格式不统一", "更新不规律"]
            },
            "social_media": {
                "description": "社交媒体平台",
                "advantages": ["实时性强", "话题敏感", "用户互动"],
                "platforms": ["Twitter", "Reddit", "LinkedIn", "微博"]
            },
            "news_apis": {
                "description": "新闻API服务",
                "advantages": ["稳定可靠", "结构化数据", "配额管理"],
                "providers": ["NewsAPI", "Guardian API", "Reuters API"]
            }
        }
    
    def source_discovery_strategies(self):
        """
        新闻源发现策略
        """
        return {
            "automatic_discovery": {
                "rss_detection": {
                    "method": "检测网站RSS链接",
                    "implementation": """
                    def discover_rss_feeds(website_url):
                        # 使用WebFetch获取网站首页
                        content = webfetch(website_url, 
                            "找出页面中所有RSS、Atom或feed链接")
                        
                        # 检查常见RSS路径
                        common_paths = ['/feed', '/rss', '/atom.xml', '/feed.xml']
                        potential_feeds = []
                        
                        for path in common_paths:
                            feed_url = urljoin(website_url, path)
                            if validate_feed(feed_url):
                                potential_feeds.append(feed_url)
                        
                        return potential_feeds
                    """
                },
                "sitemap_analysis": {
                    "method": "分析网站sitemap",
                    "implementation": """
                    def analyze_sitemap(website_url):
                        sitemap_url = urljoin(website_url, '/sitemap.xml')
                        content = webfetch(sitemap_url, 
                            "提取sitemap中的新闻页面URL模式")
                        return extract_news_patterns(content)
                    """
                }
            },
            "manual_curation": {
                "expert_recommendations": "专家推荐的优质源",
                "community_submissions": "社区用户提交",
                "competitor_analysis": "竞品分析发现"
            },
            "quality_assessment": {
                "update_frequency": "更新频率评估",
                "content_quality": "内容质量评分",
                "reliability_score": "可靠性评分",
                "coverage_analysis": "覆盖范围分析"
            }
        }
```

#### 动态源管理系统
```python
class DynamicSourceManager:
    """
    动态新闻源管理系统
    """
    
    def __init__(self):
        self.source_metadata = {
            "basic_info": {
                "url": "源URL地址",
                "name": "源名称",
                "description": "源描述",
                "category": "分类标签",
                "language": "语言代码",
                "region": "地理区域"
            },
            "technical_info": {
                "type": "源类型(RSS/Website/API)",
                "format": "内容格式",
                "encoding": "编码格式",
                "update_interval": "更新间隔",
                "parsing_rules": "解析规则"
            },
            "quality_metrics": {
                "reliability_score": "可靠性评分(0-100)",
                "update_frequency": "实际更新频率",
                "content_quality": "内容质量评分",
                "duplicate_rate": "重复内容比率",
                "error_rate": "错误率统计"
            },
            "performance_metrics": {
                "response_time": "平均响应时间",
                "success_rate": "成功抓取率",
                "bandwidth_usage": "带宽使用量",
                "cpu_cost": "CPU消耗",
                "storage_requirement": "存储需求"
            }
        }
    
    def adaptive_management_algorithm(self):
        """
        自适应管理算法
        """
        return """
        def adaptive_source_management():
            for source in active_sources:
                metrics = calculate_source_metrics(source)
                
                # 质量评估
                if metrics.quality_score < QUALITY_THRESHOLD:
                    if metrics.consecutive_failures > MAX_FAILURES:
                        deactivate_source(source)
                    else:
                        schedule_quality_check(source)
                
                # 性能优化
                if metrics.response_time > RESPONSE_THRESHOLD:
                    adjust_fetch_interval(source, increase=True)
                elif metrics.response_time < OPTIMAL_RESPONSE:
                    adjust_fetch_interval(source, increase=False)
                
                # 内容价值评估
                if metrics.unique_content_ratio < VALUE_THRESHOLD:
                    reduce_priority(source)
                else:
                    maintain_or_increase_priority(source)
                
                # 资源优化
                optimize_resource_allocation(source, metrics)
        """
```

## 智能总结引擎

### 1. 多层次总结架构

#### 总结引擎设计
```python
class IntelligentSummarizationEngine:
    """
    智能总结引擎
    """
    
    def __init__(self):
        self.summarization_levels = {
            "article_level": {
                "description": "单篇文章摘要",
                "input": "单个新闻文章",
                "output": "文章核心要点摘要",
                "max_length": "150-300词",
                "focus": ["关键事实", "主要观点", "影响分析"]
            },
            "topic_level": {
                "description": "主题聚合摘要", 
                "input": "同一主题的多篇文章",
                "output": "主题发展脉络和关键信息",
                "max_length": "300-500词",
                "focus": ["事件发展", "不同观点", "影响评估"]
            },
            "trend_level": {
                "description": "趋势分析摘要",
                "input": "时间序列的相关文章",
                "output": "趋势变化和预测分析",
                "max_length": "500-800词", 
                "focus": ["发展趋势", "关键转折", "未来预测"]
            },
            "domain_level": {
                "description": "领域综合摘要",
                "input": "整个领域的所有文章",
                "output": "领域全景和深度分析",
                "max_length": "800-1500词",
                "focus": ["全局视角", "深度分析", "专业见解"]
            }
        }
    
    def summarization_strategies(self):
        """
        总结策略设计
        """
        return {
            "extractive_summarization": {
                "description": "抽取式总结",
                "method": "从原文中选择重要句子",
                "advantages": ["保持原文准确性", "处理速度快"],
                "implementation": """
                def extractive_summary(article_text):
                    # 1. 句子分割和预处理
                    sentences = split_sentences(article_text)
                    
                    # 2. 特征提取
                    features = extract_sentence_features(sentences)
                    
                    # 3. 重要性评分
                    scores = calculate_importance_scores(features)
                    
                    # 4. 选择top-k句子
                    selected = select_top_sentences(sentences, scores, k=3)
                    
                    # 5. 重新排序和组织
                    summary = reorder_and_format(selected)
                    
                    return summary
                """
            },
            "abstractive_summarization": {
                "description": "生成式总结",
                "method": "理解内容后重新生成摘要",
                "advantages": ["语言自然流畅", "信息密度高"],
                "implementation": """
                def abstractive_summary(article_text):
                    # 使用Claude Code的AI能力
                    prompt = f'''
                    请为以下新闻文章生成一个简洁准确的摘要：
                    
                    文章内容：
                    {article_text}
                    
                    要求：
                    1. 摘要长度150-200词
                    2. 包含关键事实和主要观点
                    3. 保持客观中立的语调
                    4. 突出新闻价值和影响
                    '''
                    
                    summary = process_with_ai(prompt)
                    return summary
                """
            },
            "hybrid_approach": {
                "description": "混合式总结",
                "method": "结合抽取和生成两种方法",
                "advantages": ["准确性和流畅性兼顾", "适应性强"],
                "workflow": [
                    "1. 抽取关键信息点",
                    "2. 生成连接和过渡语言", 
                    "3. 整合形成完整摘要",
                    "4. 质量检查和优化"
                ]
            }
        }
```

### 2. 上下文感知总结

#### 智能上下文管理
```python
class ContextAwareSummarization:
    """
    上下文感知总结系统
    """
    
    def __init__(self):
        self.context_dimensions = {
            "temporal_context": {
                "description": "时间维度上下文",
                "elements": [
                    "历史事件关联",
                    "发展时间线",
                    "季节性模式",
                    "周期性趋势"
                ]
            },
            "spatial_context": {
                "description": "空间维度上下文", 
                "elements": [
                    "地理位置关联",
                    "区域影响范围",
                    "跨区域对比",
                    "本地化差异"
                ]
            },
            "topical_context": {
                "description": "主题维度上下文",
                "elements": [
                    "相关主题链接",
                    "子话题分解",
                    "跨领域影响",
                    "专业背景知识"
                ]
            },
            "stakeholder_context": {
                "description": "利益相关方上下文",
                "elements": [
                    "关键人物立场",
                    "组织机构观点",
                    "公众反应",
                    "专家意见"
                ]
            }
        }
    
    def context_enrichment_process(self):
        """
        上下文丰富化处理
        """
        return {
            "background_research": {
                "method": "背景信息研究",
                "implementation": """
                def enrich_with_background(article_content):
                    # 提取关键实体
                    entities = extract_entities(article_content)
                    
                    # 为每个实体搜索背景信息
                    background_info = {}
                    for entity in entities:
                        search_query = f"{entity} 背景 历史 相关信息"
                        results = websearch(search_query)
                        background_info[entity] = summarize_background(results)
                    
                    return background_info
                """
            },
            "trend_analysis": {
                "method": "趋势分析集成",
                "implementation": """
                def integrate_trend_analysis(article_content, time_window='30d'):
                    # 识别文章主题
                    main_topics = extract_main_topics(article_content)
                    
                    # 搜索相关历史文章
                    historical_articles = []
                    for topic in main_topics:
                        query = f"{topic} 最近{time_window}"
                        articles = search_historical_articles(query)
                        historical_articles.extend(articles)
                    
                    # 分析趋势变化
                    trend_analysis = analyze_trends(historical_articles)
                    
                    return trend_analysis
                """
            },
            "impact_assessment": {
                "method": "影响评估分析",
                "implementation": """
                def assess_impact(article_content):
                    impact_dimensions = {
                        'economic': assess_economic_impact(article_content),
                        'social': assess_social_impact(article_content),
                        'political': assess_political_impact(article_content),
                        'technological': assess_tech_impact(article_content)
                    }
                    
                    return consolidate_impact_assessment(impact_dimensions)
                """
            }
        }
```

## 实施方案与代码实现

### 1. 核心系统实现

#### 主控制器实现
```python
#!/usr/bin/env python3
"""
智能新闻聚合系统主控制器
基于Claude Code工具链实现
"""

import json
import asyncio
import logging
from datetime import datetime, timedelta
from dataclasses import dataclass
from typing import List, Dict, Optional
from pathlib import Path

@dataclass
class NewsSource:
    """新闻源数据结构"""
    url: str
    name: str
    category: str
    type: str  # 'rss', 'website', 'api'
    language: str = 'en'
    update_interval: int = 3600  # seconds
    last_updated: Optional[datetime] = None
    quality_score: float = 0.0
    active: bool = True

@dataclass 
class NewsArticle:
    """新闻文章数据结构"""
    title: str
    content: str
    url: str
    source: str
    published_date: datetime
    category: str
    summary: str = ""
    sentiment: str = ""
    entities: List[str] = None
    tags: List[str] = None
    quality_score: float = 0.0

class NewsAggregationController:
    """
    新闻聚合系统主控制器
    """
    
    def __init__(self, config_path: str = "config/news_config.json"):
        self.config = self.load_config(config_path)
        self.sources = []
        self.articles = []
        self.setup_logging()
        
    def load_config(self, config_path: str) -> dict:
        """加载配置文件"""
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except FileNotFoundError:
            return self.create_default_config(config_path)
    
    def create_default_config(self, config_path: str) -> dict:
        """创建默认配置"""
        default_config = {
            "domains": ["technology", "artificial-intelligence", "blockchain"],
            "languages": ["en", "zh"],
            "max_articles_per_run": 100,
            "summary_length": 200,
            "update_interval": 3600,
            "output_formats": ["json", "markdown", "html"],
            "quality_threshold": 0.7,
            "sources": [
                {
                    "name": "TechCrunch",
                    "url": "https://techcrunch.com/feed/",
                    "type": "rss",
                    "category": "technology"
                },
                {
                    "name": "MIT Technology Review",
                    "url": "https://www.technologyreview.com/feed/",
                    "type": "rss", 
                    "category": "technology"
                },
                {
                    "name": "Wired",
                    "url": "https://www.wired.com/feed/",
                    "type": "rss",
                    "category": "technology"
                }
            ]
        }
        
        # 保存默认配置
        Path(config_path).parent.mkdir(parents=True, exist_ok=True)
        with open(config_path, 'w', encoding='utf-8') as f:
            json.dump(default_config, f, indent=2, ensure_ascii=False)
            
        return default_config
    
    def setup_logging(self):
        """设置日志系统"""
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler('logs/news_aggregation.log'),
                logging.StreamHandler()
            ]
        )
        self.logger = logging.getLogger(__name__)
    
    async def discover_news_sources(self, domain: str) -> List[NewsSource]:
        """
        发现指定领域的新闻源
        使用Claude Code的WebSearch和WebFetch工具
        """
        self.logger.info(f"发现 {domain} 领域的新闻源...")
        
        # 使用WebSearch查找相关新闻网站
        search_queries = [
            f"{domain} news RSS feeds",
            f"best {domain} news sources",
            f"{domain} industry news websites"
        ]
        
        discovered_sources = []
        
        for query in search_queries:
            # 这里模拟使用Claude Code的WebSearch工具
            search_prompt = f"""
            搜索查询: {query}
            
            请找出相关的新闻网站和RSS源，返回格式：
            网站名称 | URL | 类型(RSS/Website) | 描述
            """
            
            # 模拟搜索结果处理
            search_results = await self.simulate_websearch(query)
            
            for result in search_results:
                source = NewsSource(
                    url=result['url'],
                    name=result['name'],
                    category=domain,
                    type=result['type']
                )
                discovered_sources.append(source)
        
        return discovered_sources
    
    async def simulate_websearch(self, query: str) -> List[Dict]:
        """
        模拟WebSearch工具调用
        实际使用时会调用Claude Code的WebSearch工具
        """
        # 这里是模拟实现，实际使用时替换为真实的WebSearch调用
        mock_results = [
            {
                "name": "Tech News RSS",
                "url": "https://example-tech-news.com/feed",
                "type": "rss",
                "description": "Technology news RSS feed"
            }
        ]
        return mock_results
    
    async def fetch_article_content(self, url: str) -> Optional[str]:
        """
        获取文章内容
        使用Claude Code的WebFetch工具
        """
        try:
            # 模拟WebFetch工具调用
            fetch_prompt = f"""
            请获取以下URL的新闻文章内容：{url}
            
            提取要求：
            1. 文章标题
            2. 正文内容
            3. 发布时间
            4. 作者信息
            5. 关键词标签
            
            返回清洁的文本内容，去除广告和无关信息。
            """
            
            # 实际实现时这里会调用WebFetch工具
            content = await self.simulate_webfetch(url, fetch_prompt)
            return content
            
        except Exception as e:
            self.logger.error(f"获取文章内容失败 {url}: {e}")
            return None
    
    async def simulate_webfetch(self, url: str, prompt: str) -> str:
        """
        模拟WebFetch工具调用
        """
        # 模拟返回的文章内容
        return f"模拟文章内容来自 {url}"
    
    async def generate_article_summary(self, article: NewsArticle) -> str:
        """
        生成文章摘要
        使用Claude Code的AI处理能力
        """
        summary_prompt = f"""
        请为以下新闻文章生成摘要：
        
        标题：{article.title}
        内容：{article.content[:2000]}...
        
        要求：
        1. 摘要长度不超过{self.config['summary_length']}字
        2. 突出关键信息和新闻价值
        3. 保持客观中立
        4. 包含影响分析
        """
        
        # 模拟AI总结生成
        summary = await self.simulate_ai_summary(summary_prompt)
        return summary
    
    async def simulate_ai_summary(self, prompt: str) -> str:
        """
        模拟AI摘要生成
        """
        return "这是一个模拟生成的新闻摘要，包含了文章的关键信息和主要观点。"
    
    async def extract_entities_and_tags(self, article: NewsArticle) -> tuple:
        """
        提取实体和标签
        """
        entity_prompt = f"""
        从以下新闻文章中提取：
        1. 关键实体（人名、组织、地点、产品等）
        2. 主题标签
        
        文章内容：{article.content[:1500]}...
        
        返回JSON格式：
        {{
            "entities": ["实体1", "实体2", ...],
            "tags": ["标签1", "标签2", ...]
        }}
        """
        
        # 模拟实体提取
        result = await self.simulate_entity_extraction(entity_prompt)
        return result.get('entities', []), result.get('tags', [])
    
    async def simulate_entity_extraction(self, prompt: str) -> dict:
        """
        模拟实体提取
        """
        return {
            "entities": ["实体1", "实体2", "实体3"],
            "tags": ["technology", "AI", "innovation"]
        }
    
    async def process_news_batch(self, sources: List[NewsSource]) -> List[NewsArticle]:
        """
        批量处理新闻源
        """
        all_articles = []
        
        for source in sources:
            try:
                self.logger.info(f"处理新闻源: {source.name}")
                
                # 获取源内容
                content = await self.fetch_article_content(source.url)
                if not content:
                    continue
                
                # 解析文章列表
                articles = await self.parse_source_content(content, source)
                
                # 处理每篇文章
                for article in articles:
                    # 生成摘要
                    article.summary = await self.generate_article_summary(article)
                    
                    # 提取实体和标签
                    entities, tags = await self.extract_entities_and_tags(article)
                    article.entities = entities
                    article.tags = tags
                    
                    # 质量评分
                    article.quality_score = await self.calculate_quality_score(article)
                    
                    all_articles.append(article)
                
                # 更新源的最后更新时间
                source.last_updated = datetime.now()
                
            except Exception as e:
                self.logger.error(f"处理新闻源失败 {source.name}: {e}")
        
        return all_articles
    
    async def parse_source_content(self, content: str, source: NewsSource) -> List[NewsArticle]:
        """
        解析新闻源内容
        """
        # 这里应该根据源类型(RSS/Website)进行不同的解析
        # 模拟返回文章列表
        mock_articles = [
            NewsArticle(
                title=f"来自{source.name}的新闻标题",
                content="新闻正文内容...",
                url=f"{source.url}/article/1",
                source=source.name,
                published_date=datetime.now(),
                category=source.category
            )
        ]
        return mock_articles
    
    async def calculate_quality_score(self, article: NewsArticle) -> float:
        """
        计算文章质量评分
        """
        # 质量评分算法
        score = 0.0
        
        # 内容长度评分
        content_length = len(article.content)
        if content_length > 500:
            score += 0.3
        elif content_length > 200:
            score += 0.2
        
        # 标题质量评分
        if len(article.title.split()) > 5:
            score += 0.2
        
        # 实体丰富度评分
        if article.entities and len(article.entities) > 2:
            score += 0.3
        
        # 时效性评分
        time_diff = datetime.now() - article.published_date
        if time_diff < timedelta(hours=24):
            score += 0.2
        
        return min(score, 1.0)
    
    async def generate_domain_summary(self, articles: List[NewsArticle], domain: str) -> str:
        """
        生成领域总结报告
        """
        # 按质量过滤文章
        quality_articles = [a for a in articles if a.quality_score >= self.config['quality_threshold']]
        
        # 按主题聚类
        topic_clusters = await self.cluster_articles_by_topic(quality_articles)
        
        # 生成综合总结
        summary_prompt = f"""
        基于以下{domain}领域的新闻文章，生成一份综合分析报告：
        
        文章数量：{len(quality_articles)}
        时间范围：{min(a.published_date for a in quality_articles)} 到 {max(a.published_date for a in quality_articles)}
        
        主要话题：
        {self.format_topic_clusters(topic_clusters)}
        
        请生成包含以下内容的报告：
        1. 领域概况和主要趋势
        2. 重要事件和突破性新闻
        3. 关键观点和不同立场
        4. 影响分析和未来展望
        5. 值得关注的发展方向
        
        报告长度：800-1200字
        """
        
        domain_summary = await self.simulate_ai_summary(summary_prompt)
        return domain_summary
    
    async def cluster_articles_by_topic(self, articles: List[NewsArticle]) -> Dict[str, List[NewsArticle]]:
        """
        按主题聚类文章
        """
        # 简化的主题聚类实现
        clusters = {}
        
        for article in articles:
            # 使用标签作为聚类依据
            main_tag = article.tags[0] if article.tags else "其他"
            
            if main_tag not in clusters:
                clusters[main_tag] = []
            
            clusters[main_tag].append(article)
        
        return clusters
    
    def format_topic_clusters(self, clusters: Dict[str, List[NewsArticle]]) -> str:
        """
        格式化主题聚类结果
        """
        formatted = []
        for topic, articles in clusters.items():
            formatted.append(f"- {topic}: {len(articles)}篇文章")
        return "\n".join(formatted)
    
    async def save_results(self, articles: List[NewsArticle], domain_summary: str, domain: str):
        """
        保存处理结果
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # 保存文章数据
        articles_data = [
            {
                "title": article.title,
                "summary": article.summary,
                "url": article.url,
                "source": article.source,
                "published_date": article.published_date.isoformat(),
                "category": article.category,
                "entities": article.entities,
                "tags": article.tags,
                "quality_score": article.quality_score
            }
            for article in articles
        ]
        
        # JSON格式
        output_dir = Path(f"output/{domain}")
        output_dir.mkdir(parents=True, exist_ok=True)
        
        with open(output_dir / f"articles_{timestamp}.json", 'w', encoding='utf-8') as f:
            json.dump(articles_data, f, indent=2, ensure_ascii=False)
        
        # Markdown格式报告
        markdown_report = self.generate_markdown_report(articles, domain_summary, domain)
        with open(output_dir / f"report_{timestamp}.md", 'w', encoding='utf-8') as f:
            f.write(markdown_report)
        
        self.logger.info(f"结果已保存到 {output_dir}")
    
    def generate_markdown_report(self, articles: List[NewsArticle], domain_summary: str, domain: str) -> str:
        """
        生成Markdown格式报告
        """
        report = f"""# {domain.upper()}领域新闻聚合报告

生成时间：{datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

## 执行摘要

{domain_summary}

## 详细文章列表

"""
        
        for i, article in enumerate(articles, 1):
            report += f"""### {i}. {article.title}

**来源：** {article.source}  
**发布时间：** {article.published_date.strftime("%Y-%m-%d %H:%M")}  
**质量评分：** {article.quality_score:.2f}  
**链接：** {article.url}

**摘要：**
{article.summary}

**关键词：** {', '.join(article.tags) if article.tags else '无'}  
**相关实体：** {', '.join(article.entities) if article.entities else '无'}

---

"""
        
        return report
    
    async def run_aggregation(self, domains: List[str] = None):
        """
        运行新闻聚合主流程
        """
        if domains is None:
            domains = self.config['domains']
        
        self.logger.info(f"开始新闻聚合任务，目标领域：{domains}")
        
        for domain in domains:
            try:
                self.logger.info(f"处理领域：{domain}")
                
                # 1. 发现新闻源
                sources = await self.discover_news_sources(domain)
                sources.extend([NewsSource(**src) for src in self.config['sources'] 
                              if src.get('category') == domain])
                
                # 2. 批量处理新闻
                articles = await self.process_news_batch(sources)
                
                # 3. 质量过滤
                quality_articles = [a for a in articles 
                                  if a.quality_score >= self.config['quality_threshold']]
                
                # 4. 生成领域总结
                domain_summary = await self.generate_domain_summary(quality_articles, domain)
                
                # 5. 保存结果
                await self.save_results(quality_articles, domain_summary, domain)
                
                self.logger.info(f"领域 {domain} 处理完成，共处理 {len(quality_articles)} 篇高质量文章")
                
            except Exception as e:
                self.logger.error(f"处理领域 {domain} 时发生错误：{e}")
        
        self.logger.info("新闻聚合任务完成")

# 使用示例
async def main():
    """
    主函数示例
    """
    # 创建聚合器实例
    aggregator = NewsAggregationController()
    
    # 运行聚合任务
    await aggregator.run_aggregation(['technology', 'artificial-intelligence'])

if __name__ == "__main__":
    asyncio.run(main())
```

### 2. 专用工具实现

#### 源质量评估工具
```python
#!/usr/bin/env python3
"""
新闻源质量评估工具
"""

import asyncio
import statistics
from datetime import datetime, timedelta
from typing import Dict, List, Tuple

class SourceQualityAssessor:
    """
    新闻源质量评估器
    """
    
    def __init__(self):
        self.quality_metrics = {
            "content_quality": 0.3,    # 内容质量权重
            "update_frequency": 0.2,   # 更新频率权重  
            "reliability": 0.25,       # 可靠性权重
            "uniqueness": 0.15,        # 独特性权重
            "performance": 0.1         # 性能权重
        }
    
    async def assess_source_quality(self, source: NewsSource, 
                                  historical_data: List[NewsArticle]) -> Dict[str, float]:
        """
        综合评估新闻源质量
        """
        assessments = {
            "content_quality": await self.assess_content_quality(historical_data),
            "update_frequency": await self.assess_update_frequency(historical_data),
            "reliability": await self.assess_reliability(source, historical_data),
            "uniqueness": await self.assess_uniqueness(historical_data),
            "performance": await self.assess_performance(source)
        }
        
        # 计算综合评分
        overall_score = sum(
            score * self.quality_metrics[metric] 
            for metric, score in assessments.items()
        )
        
        assessments["overall_score"] = overall_score
        return assessments
    
    async def assess_content_quality(self, articles: List[NewsArticle]) -> float:
        """
        评估内容质量
        """
        if not articles:
            return 0.0
        
        quality_indicators = []
        
        for article in articles:
            score = 0.0
            
            # 内容长度评分
            content_length = len(article.content)
            if content_length > 1000:
                score += 0.3
            elif content_length > 500:
                score += 0.2
            elif content_length > 200:
                score += 0.1
            
            # 标题质量评分
            title_words = len(article.title.split())
            if 5 <= title_words <= 15:
                score += 0.2
            
            # 结构化信息评分
            if article.entities and len(article.entities) >= 3:
                score += 0.2
            
            if article.tags and len(article.tags) >= 2:
                score += 0.1
            
            # 语言质量评分（简化版）
            if not self.has_quality_issues(article.content):
                score += 0.2
            
            quality_indicators.append(min(score, 1.0))
        
        return statistics.mean(quality_indicators)
    
    async def assess_update_frequency(self, articles: List[NewsArticle]) -> float:
        """
        评估更新频率
        """
        if len(articles) < 2:
            return 0.5
        
        # 计算文章间时间间隔
        articles_sorted = sorted(articles, key=lambda x: x.published_date)
        intervals = []
        
        for i in range(1, len(articles_sorted)):
            interval = articles_sorted[i].published_date - articles_sorted[i-1].published_date
            intervals.append(interval.total_seconds() / 3600)  # 转换为小时
        
        avg_interval = statistics.mean(intervals)
        
        # 根据平均间隔评分
        if avg_interval <= 6:      # 6小时内更新
            return 1.0
        elif avg_interval <= 24:   # 24小时内更新
            return 0.8
        elif avg_interval <= 72:   # 72小时内更新
            return 0.6
        elif avg_interval <= 168:  # 一周内更新
            return 0.4
        else:
            return 0.2
    
    async def assess_reliability(self, source: NewsSource, 
                               articles: List[NewsArticle]) -> float:
        """
        评估可靠性
        """
        reliability_score = 0.0
        
        # 基于源的历史表现
        if source.last_updated:
            days_since_update = (datetime.now() - source.last_updated).days
            if days_since_update <= 1:
                reliability_score += 0.3
            elif days_since_update <= 7:
                reliability_score += 0.2
            elif days_since_update <= 30:
                reliability_score += 0.1
        
        # 基于文章质量一致性
        if articles:
            quality_scores = [a.quality_score for a in articles if a.quality_score > 0]
            if quality_scores:
                quality_std = statistics.stdev(quality_scores) if len(quality_scores) > 1 else 0
                consistency_score = max(0, 1 - quality_std)  # 标准差越小，一致性越好
                reliability_score += consistency_score * 0.4
        
        # 基于错误率（模拟）
        error_rate = await self.calculate_error_rate(source)
        reliability_score += (1 - error_rate) * 0.3
        
        return min(reliability_score, 1.0)
    
    async def assess_uniqueness(self, articles: List[NewsArticle]) -> float:
        """
        评估内容独特性
        """
        if len(articles) < 2:
            return 1.0
        
        # 简化的内容相似度检测
        unique_titles = set()
        duplicate_count = 0
        
        for article in articles:
            # 标题去重检测
            title_words = set(article.title.lower().split())
            
            is_duplicate = False
            for existing_title in unique_titles:
                existing_words = set(existing_title.split())
                overlap = len(title_words & existing_words) / len(title_words | existing_words)
                if overlap > 0.7:  # 70%相似度认为是重复
                    is_duplicate = True
                    duplicate_count += 1
                    break
            
            if not is_duplicate:
                unique_titles.add(article.title.lower())
        
        uniqueness_ratio = 1 - (duplicate_count / len(articles))
        return uniqueness_ratio
    
    async def assess_performance(self, source: NewsSource) -> float:
        """
        评估性能指标
        """
        # 模拟性能评估
        performance_score = 0.0
        
        # 响应时间评分（模拟）
        avg_response_time = await self.get_avg_response_time(source.url)
        if avg_response_time < 2:
            performance_score += 0.4
        elif avg_response_time < 5:
            performance_score += 0.3
        elif avg_response_time < 10:
            performance_score += 0.2
        
        # 可用性评分（模拟）
        availability = await self.get_availability_rate(source.url)
        performance_score += availability * 0.6
        
        return min(performance_score, 1.0)
    
    def has_quality_issues(self, content: str) -> bool:
        """
        检查内容质量问题
        """
        # 简化的质量检查
        quality_issues = [
            len(content) < 100,  # 内容过短
            content.count('http') > len(content) / 100,  # 链接过多
            content.upper() == content,  # 全大写
            len(set(content.split())) / len(content.split()) < 0.5  # 重复词汇过多
        ]
        
        return any(quality_issues)
    
    async def get_avg_response_time(self, url: str) -> float:
        """
        获取平均响应时间（模拟）
        """
        # 在实际实现中，这里会记录和计算真实的响应时间
        return 3.5  # 模拟返回3.5秒
    
    async def get_availability_rate(self, url: str) -> float:
        """
        获取可用性率（模拟）
        """
        # 在实际实现中，这里会基于历史监控数据计算
        return 0.95  # 模拟返回95%可用性
    
    async def calculate_error_rate(self, source: NewsSource) -> float:
        """
        计算错误率（模拟）
        """
        # 在实际实现中，这里会基于错误日志计算
        return 0.05  # 模拟返回5%错误率
```

#### 内容去重工具
```python
#!/usr/bin/env python3
"""
新闻内容去重工具
"""

import hashlib
import re
from typing import List, Set, Tuple
from difflib import SequenceMatcher

class ContentDeduplicator:
    """
    内容去重器
    """
    
    def __init__(self):
        self.similarity_threshold = 0.8
        self.title_threshold = 0.7
        self.content_threshold = 0.85
    
    async def deduplicate_articles(self, articles: List[NewsArticle]) -> List[NewsArticle]:
        """
        对文章列表进行去重
        """
        if not articles:
            return []
        
        unique_articles = []
        processed_hashes = set()
        
        for article in articles:
            # 快速哈希检查
            content_hash = self.calculate_content_hash(article)
            if content_hash in processed_hashes:
                continue
            
            # 详细相似度检查
            is_duplicate = False
            for existing_article in unique_articles:
                similarity = await self.calculate_similarity(article, existing_article)
                if similarity > self.similarity_threshold:
                    # 保留质量更高的文章
                    if article.quality_score > existing_article.quality_score:
                        unique_articles.remove(existing_article)
                        unique_articles.append(article)
                    is_duplicate = True
                    break
            
            if not is_duplicate:
                unique_articles.append(article)
                processed_hashes.add(content_hash)
        
        return unique_articles
    
    def calculate_content_hash(self, article: NewsArticle) -> str:
        """
        计算内容哈希值
        """
        # 标准化内容
        normalized_title = self.normalize_text(article.title)
        normalized_content = self.normalize_text(article.content[:500])  # 只取前500字符
        
        # 生成哈希
        content_string = f"{normalized_title}|{normalized_content}"
        return hashlib.md5(content_string.encode('utf-8')).hexdigest()
    
    async def calculate_similarity(self, article1: NewsArticle, article2: NewsArticle) -> float:
        """
        计算两篇文章的相似度
        """
        # 标题相似度
        title_sim = self.text_similarity(
            self.normalize_text(article1.title),
            self.normalize_text(article2.title)
        )
        
        # 内容相似度
        content_sim = self.text_similarity(
            self.normalize_text(article1.content[:1000]),
            self.normalize_text(article2.content[:1000])
        )
        
        # URL相似度
        url_sim = self.url_similarity(article1.url, article2.url)
        
        # 综合相似度
        overall_similarity = (
            title_sim * 0.4 +
            content_sim * 0.5 +
            url_sim * 0.1
        )
        
        return overall_similarity
    
    def normalize_text(self, text: str) -> str:
        """
        文本标准化
        """
        # 转换为小写
        text = text.lower()
        
        # 移除标点符号和特殊字符
        text = re.sub(r'[^\w\s]', ' ', text)
        
        # 移除多余空格
        text = re.sub(r'\s+', ' ', text).strip()
        
        # 移除常见停用词
        stop_words = {'the', 'a', 'an', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'of', 'with', 'by'}
        words = [word for word in text.split() if word not in stop_words]
        
        return ' '.join(words)
    
    def text_similarity(self, text1: str, text2: str) -> float:
        """
        计算两个文本的相似度
        """
        if not text1 or not text2:
            return 0.0
        
        # 使用SequenceMatcher计算相似度
        matcher = SequenceMatcher(None, text1, text2)
        return matcher.ratio()
    
    def url_similarity(self, url1: str, url2: str) -> float:
        """
        计算URL相似度
        """
        if url1 == url2:
            return 1.0
        
        # 提取域名
        domain1 = self.extract_domain(url1)
        domain2 = self.extract_domain(url2)
        
        if domain1 == domain2:
            return 0.5  # 同域名但不同URL
        else:
            return 0.0
    
    def extract_domain(self, url: str) -> str:
        """
        提取URL域名
        """
        import urllib.parse
        parsed = urllib.parse.urlparse(url)
        return parsed.netloc
    
    async def find_article_clusters(self, articles: List[NewsArticle]) -> List[List[NewsArticle]]:
        """
        将相似文章聚类
        """
        clusters = []
        processed = set()
        
        for i, article in enumerate(articles):
            if i in processed:
                continue
            
            cluster = [article]
            processed.add(i)
            
            for j, other_article in enumerate(articles[i+1:], i+1):
                if j in processed:
                    continue
                
                similarity = await self.calculate_similarity(article, other_article)
                if similarity > self.similarity_threshold:
                    cluster.append(other_article)
                    processed.add(j)
            
            clusters.append(cluster)
        
        return clusters
    
    async def merge_similar_articles(self, articles: List[NewsArticle]) -> NewsArticle:
        """
        合并相似文章
        """
        if len(articles) == 1:
            return articles[0]
        
        # 选择质量最高的文章作为主文章
        main_article = max(articles, key=lambda x: x.quality_score)
        
        # 合并信息
        all_entities = set()
        all_tags = set()
        all_sources = set()
        
        for article in articles:
            if article.entities:
                all_entities.update(article.entities)
            if article.tags:
                all_tags.update(article.tags)
            all_sources.add(article.source)
        
        # 创建合并后的文章
        merged_article = NewsArticle(
            title=main_article.title,
            content=main_article.content,
            url=main_article.url,
            source=f"合并来源: {', '.join(all_sources)}",
            published_date=main_article.published_date,
            category=main_article.category,
            summary=main_article.summary,
            entities=list(all_entities),
            tags=list(all_tags),
            quality_score=main_article.quality_score
        )
        
        return merged_article
```

## 部署与运维

### 1. 容器化部署方案

#### Docker配置
```dockerfile
# Dockerfile
FROM python:3.11-slim

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY src/ ./src/
COPY config/ ./config/
COPY scripts/ ./scripts/

# 创建必要的目录
RUN mkdir -p logs output data

# 设置环境变量
ENV PYTHONPATH=/app/src
ENV LOG_LEVEL=INFO

# 暴露端口
EXPOSE 8000

# 启动命令
CMD ["python", "src/main.py"]
```

#### Docker Compose配置
```yaml
# docker-compose.yml
version: '3.8'

services:
  news-aggregator:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: news-aggregator
    environment:
      - LOG_LEVEL=${LOG_LEVEL:-INFO}
      - MAX_WORKERS=${MAX_WORKERS:-4}
      - UPDATE_INTERVAL=${UPDATE_INTERVAL:-3600}
    volumes:
      - ./config:/app/config
      - ./data:/app/data
      - ./logs:/app/logs
      - ./output:/app/output
    ports:
      - "8000:8000"
    networks:
      - news-net
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  redis:
    image: redis:7-alpine
    container_name: news-redis
    volumes:
      - redis_data:/data
    networks:
      - news-net
    restart: unless-stopped

  mongodb:
    image: mongo:6
    container_name: news-mongodb
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD:-password}
    volumes:
      - mongodb_data:/data/db
    ports:
      - "27017:27017"
    networks:
      - news-net
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: news-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - news-aggregator
    networks:
      - news-net
    restart: unless-stopped

volumes:
  redis_data:
  mongodb_data:

networks:
  news-net:
    driver: bridge
```

### 2. 自动化运维脚本

#### 部署脚本
```bash
#!/bin/bash
# deploy.sh - 自动化部署脚本

set -e

echo "🚀 开始部署新闻聚合系统..."

# 环境检查
check_requirements() {
    echo "检查系统要求..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        echo "❌ Docker未安装"
        exit 1
    fi
    
    # 检查Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        echo "❌ Docker Compose未安装"
        exit 1
    fi
    
    echo "✅ 系统要求满足"
}

# 配置初始化
init_config() {
    echo "初始化配置..."
    
    # 创建必要目录
    mkdir -p {config,data,logs,output,ssl}
    
    # 生成环境变量文件
    if [ ! -f .env ]; then
        cat > .env << EOF
LOG_LEVEL=INFO
MAX_WORKERS=4
UPDATE_INTERVAL=3600
MONGO_PASSWORD=secure_password
EOF
    fi
    
    # 生成Nginx配置
    if [ ! -f nginx.conf ]; then
        cat > nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    upstream app {
        server news-aggregator:8000;
    }
    
    server {
        listen 80;
        server_name _;
        
        location / {
            proxy_pass http://app;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }
    }
}
EOF
    fi
    
    echo "✅ 配置初始化完成"
}

# 启动服务
start_services() {
    echo "启动服务..."
    
    # 构建和启动容器
    docker-compose build
    docker-compose up -d
    
    # 等待服务启动
    echo "等待服务启动..."
    sleep 30
    
    # 健康检查
    if curl -f http://localhost:8000/health > /dev/null 2>&1; then
        echo "✅ 服务启动成功"
    else
        echo "❌ 服务启动失败"
        docker-compose logs news-aggregator
        exit 1
    fi
}

# 主流程
main() {
    check_requirements
    init_config
    start_services
    
    echo "🎉 部署完成！"
    echo "访问地址: http://localhost"
    echo "管理命令:"
    echo "  查看状态: docker-compose ps"
    echo "  查看日志: docker-compose logs -f"
    echo "  停止服务: docker-compose down"
}

main "$@"
```

#### 监控脚本
```bash
#!/bin/bash
# monitor.sh - 系统监控脚本

# 健康检查
health_check() {
    echo "🏥 系统健康检查"
    echo "===================="
    
    # 检查容器状态
    echo "📦 容器状态:"
    docker-compose ps
    
    # 检查服务响应
    echo -e "\n🌐 服务响应:"
    if curl -f http://localhost/health > /dev/null 2>&1; then
        echo "✅ Web服务正常"
    else
        echo "❌ Web服务异常"
    fi
    
    # 检查数据库连接
    echo -e "\n🗄️ 数据库状态:"
    if docker exec news-mongodb mongo --eval "db.adminCommand('ismaster')" > /dev/null 2>&1; then
        echo "✅ MongoDB正常"
    else
        echo "❌ MongoDB异常"
    fi
    
    if docker exec news-redis redis-cli ping > /dev/null 2>&1; then
        echo "✅ Redis正常"
    else
        echo "❌ Redis异常"
    fi
    
    # 检查资源使用
    echo -e "\n💻 资源使用:"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"
    
    # 检查日志错误
    echo -e "\n📋 错误日志:"
    error_count=$(docker-compose logs --since="1h" 2>&1 | grep -i error | wc -l)
    echo "最近1小时错误数量: $error_count"
    
    if [ $error_count -gt 10 ]; then
        echo "⚠️  错误数量过多，请检查日志"
    fi
}

# 性能监控
performance_monitor() {
    echo "📈 性能监控"
    echo "=================="
    
    # 响应时间测试
    echo "测试响应时间..."
    for i in {1..5}; do
        response_time=$(curl -o /dev/null -s -w "%{time_total}" http://localhost/)
        echo "请求 $i: ${response_time}s"
    done
    
    # 内存使用分析
    echo -e "\n内存使用详情:"
    docker exec news-aggregator python3 -c "
import psutil
memory = psutil.virtual_memory()
print(f'总内存: {memory.total // 1024 // 1024}MB')
print(f'已用内存: {memory.used // 1024 // 1024}MB')
print(f'使用率: {memory.percent}%')
"
    
    # 磁盘空间检查
    echo -e "\n磁盘空间:"
    df -h | grep -E "(Filesystem|/dev/)"
}

# 日志分析
log_analysis() {
    echo "📜 日志分析"
    echo "=================="
    
    # 最近的错误日志
    echo "最近的错误:"
    docker-compose logs --since="24h" | grep -i error | tail -10
    
    # 访问统计
    echo -e "\n访问统计:"
    docker exec news-nginx tail -100 /var/log/nginx/access.log | \
        awk '{print $1}' | sort | uniq -c | sort -nr | head -10
}

# 自动修复
auto_fix() {
    echo "🔧 自动修复检查"
    echo "===================="
    
    # 重启不健康的容器
    unhealthy_containers=$(docker ps --filter "health=unhealthy" --format "{{.Names}}")
    
    if [ ! -z "$unhealthy_containers" ]; then
        echo "发现不健康容器: $unhealthy_containers"
        echo "正在重启..."
        
        for container in $unhealthy_containers; do
            docker restart $container
            echo "已重启: $container"
        done
    else
        echo "✅ 所有容器健康"
    fi
    
    # 清理旧日志
    echo "清理30天前的日志..."
    find ./logs -name "*.log" -mtime +30 -delete
    
    # 清理Docker
    echo "清理无用的Docker资源..."
    docker system prune -f
}

# 备份
backup_data() {
    echo "💾 数据备份"
    echo "=================="
    
    backup_dir="backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p $backup_dir
    
    # 备份MongoDB
    echo "备份MongoDB..."
    docker exec news-mongodb mongodump --out /tmp/backup
    docker cp news-mongodb:/tmp/backup $backup_dir/mongodb
    
    # 备份Redis
    echo "备份Redis..."
    docker exec news-redis redis-cli BGSAVE
    docker cp news-redis:/data/dump.rdb $backup_dir/
    
    # 备份配置文件
    echo "备份配置..."
    cp -r config $backup_dir/
    
    # 备份输出数据
    echo "备份输出数据..."
    cp -r output $backup_dir/
    
    echo "✅ 备份完成: $backup_dir"
}

# 主菜单
case "$1" in
    health)
        health_check
        ;;
    performance)
        performance_monitor
        ;;
    logs)
        log_analysis
        ;;
    fix)
        auto_fix
        ;;
    backup)
        backup_data
        ;;
    all)
        health_check
        performance_monitor
        log_analysis
        ;;
    *)
        echo "用法: $0 {health|performance|logs|fix|backup|all}"
        echo ""
        echo "命令说明:"
        echo "  health      - 健康检查"
        echo "  performance - 性能监控"
        echo "  logs        - 日志分析"
        echo "  fix         - 自动修复"
        echo "  backup      - 数据备份"
        echo "  all         - 完整监控"
        exit 1
        ;;
esac
```

### 3. 定时任务配置

#### Crontab配置
```bash
# crontab -e
# 新闻聚合定时任务配置

# 每小时执行一次新闻聚合
0 * * * * cd /opt/news-aggregator && python3 src/main.py >> logs/cron.log 2>&1

# 每4小时进行健康检查
0 */4 * * * cd /opt/news-aggregator && ./scripts/monitor.sh health >> logs/monitor.log 2>&1

# 每天凌晨2点备份数据
0 2 * * * cd /opt/news-aggregator && ./scripts/monitor.sh backup >> logs/backup.log 2>&1

# 每周清理日志
0 0 * * 0 cd /opt/news-aggregator && find logs -name "*.log" -mtime +7 -delete

# 每月更新源质量评估
0 0 1 * * cd /opt/news-aggregator && python3 scripts/source_quality_assessment.py >> logs/quality.log 2>&1
```

## 性能优化策略

### 1. 并发处理优化

#### 异步批处理实现
```python
import asyncio
import aiohttp
from concurrent.futures import ThreadPoolExecutor
from typing import List, Dict, Any

class ConcurrentProcessingOptimizer:
    """
    并发处理优化器
    """
    
    def __init__(self, max_workers: int = 10, max_concurrent_requests: int = 20):
        self.max_workers = max_workers
        self.max_concurrent_requests = max_concurrent_requests
        self.session = None
        self.executor = ThreadPoolExecutor(max_workers=max_workers)
    
    async def __aenter__(self):
        self.session = aiohttp.ClientSession(
            connector=aiohttp.TCPConnector(limit=self.max_concurrent_requests),
            timeout=aiohttp.ClientTimeout(total=30)
        )
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            await self.session.close()
        self.executor.shutdown(wait=True)
    
    async def fetch_urls_batch(self, urls: List[str]) -> List[Dict[str, Any]]:
        """
        批量获取URL内容
        """
        semaphore = asyncio.Semaphore(self.max_concurrent_requests)
        
        async def fetch_single_url(url: str) -> Dict[str, Any]:
            async with semaphore:
                try:
                    async with self.session.get(url) as response:
                        content = await response.text()
                        return {
                            "url": url,
                            "status": response.status,
                            "content": content,
                            "error": None
                        }
                except Exception as e:
                    return {
                        "url": url,
                        "status": None,
                        "content": None,
                        "error": str(e)
                    }
        
        tasks = [fetch_single_url(url) for url in urls]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        # 处理异常结果
        processed_results = []
        for result in results:
            if isinstance(result, Exception):
                processed_results.append({
                    "url": "unknown",
                    "status": None,
                    "content": None,
                    "error": str(result)
                })
            else:
                processed_results.append(result)
        
        return processed_results
    
    async def process_articles_batch(self, articles: List[NewsArticle]) -> List[NewsArticle]:
        """
        批量处理文章
        """
        # 将CPU密集型任务分配到线程池
        loop = asyncio.get_event_loop()
        
        # 分批处理
        batch_size = self.max_workers
        processed_articles = []
        
        for i in range(0, len(articles), batch_size):
            batch = articles[i:i + batch_size]
            
            # 并行处理批次
            tasks = []
            for article in batch:
                task = loop.run_in_executor(
                    self.executor,
                    self.process_single_article,
                    article
                )
                tasks.append(task)
            
            batch_results = await asyncio.gather(*tasks)
            processed_articles.extend(batch_results)
        
        return processed_articles
    
    def process_single_article(self, article: NewsArticle) -> NewsArticle:
        """
        处理单篇文章（CPU密集型任务）
        """
        # 文本处理和分析
        processed_article = article
        
        # 实体提取
        processed_article.entities = self.extract_entities_sync(article.content)
        
        # 标签生成
        processed_article.tags = self.generate_tags_sync(article.content)
        
        # 质量评分
        processed_article.quality_score = self.calculate_quality_sync(article)
        
        return processed_article
    
    def extract_entities_sync(self, content: str) -> List[str]:
        """
        同步实体提取
        """
        # 简化的实体提取实现
        import re
        
        # 提取人名模式 (大写字母开头的词)
        name_pattern = r'\b[A-Z][a-z]+\s+[A-Z][a-z]+\b'
        names = re.findall(name_pattern, content)
        
        # 提取组织名称模式
        org_pattern = r'\b[A-Z][A-Za-z\s]+(?:Inc|Corp|Ltd|LLC|Company|Organization)\b'
        orgs = re.findall(org_pattern, content)
        
        return list(set(names + orgs))
    
    def generate_tags_sync(self, content: str) -> List[str]:
        """
        同步标签生成
        """
        # 简化的关键词提取
        keywords = {
            'technology': ['AI', 'artificial intelligence', 'machine learning', 'technology'],
            'business': ['business', 'company', 'market', 'revenue', 'profit'],
            'science': ['research', 'study', 'scientist', 'discovery']
        }
        
        content_lower = content.lower()
        extracted_tags = []
        
        for tag, words in keywords.items():
            if any(word.lower() in content_lower for word in words):
                extracted_tags.append(tag)
        
        return extracted_tags
    
    def calculate_quality_sync(self, article: NewsArticle) -> float:
        """
        同步质量评分计算
        """
        score = 0.0
        
        # 内容长度评分
        if len(article.content) > 1000:
            score += 0.3
        elif len(article.content) > 500:
            score += 0.2
        
        # 标题质量评分
        title_words = len(article.title.split())
        if 5 <= title_words <= 15:
            score += 0.3
        
        # 结构化信息评分
        if article.entities and len(article.entities) >= 2:
            score += 0.2
        
        if article.tags and len(article.tags) >= 1:
            score += 0.2
        
        return min(score, 1.0)

# 使用示例
async def optimized_processing_example():
    """
    优化处理示例
    """
    urls = [
        "https://example.com/news1",
        "https://example.com/news2",
        # ... 更多URL
    ]
    
    async with ConcurrentProcessingOptimizer(max_workers=8, max_concurrent_requests=20) as processor:
        # 批量获取内容
        url_results = await processor.fetch_urls_batch(urls)
        
        # 转换为文章对象
        articles = []
        for result in url_results:
            if result["content"] and result["status"] == 200:
                article = NewsArticle(
                    title="提取的标题",
                    content=result["content"],
                    url=result["url"],
                    source="来源",
                    published_date=datetime.now(),
                    category="科技"
                )
                articles.append(article)
        
        # 批量处理文章
        processed_articles = await processor.process_articles_batch(articles)
        
        return processed_articles
```

### 2. 缓存策略优化

#### 多层缓存系统
```python
import redis
import pickle
import hashlib
from typing import Any, Optional, Union
from datetime import datetime, timedelta

class MultiLevelCacheSystem:
    """
    多层缓存系统
    """
    
    def __init__(self, redis_host: str = 'localhost', redis_port: int = 6379):
        self.redis_client = redis.Redis(host=redis_host, port=redis_port, decode_responses=False)
        self.memory_cache = {}
        self.memory_cache_size = 1000  # 内存缓存最大条目数
        self.default_ttl = 3600  # 默认TTL 1小时
    
    def _generate_cache_key(self, prefix: str, identifier: str) -> str:
        """
        生成缓存键
        """
        return f"{prefix}:{hashlib.md5(identifier.encode()).hexdigest()}"
    
    async def get_cached_content(self, url: str) -> Optional[Dict[str, Any]]:
        """
        获取缓存的网页内容
        """
        cache_key = self._generate_cache_key("web_content", url)
        
        # 1. 检查内存缓存
        if cache_key in self.memory_cache:
            cached_data = self.memory_cache[cache_key]
            if cached_data['expires'] > datetime.now():
                return cached_data['data']
            else:
                # 过期删除
                del self.memory_cache[cache_key]
        
        # 2. 检查Redis缓存
        try:
            cached_bytes = self.redis_client.get(cache_key)
            if cached_bytes:
                cached_data = pickle.loads(cached_bytes)
                
                # 写入内存缓存
                self._set_memory_cache(cache_key, cached_data, ttl=300)  # 内存缓存5分钟
                
                return cached_data
        except Exception as e:
            print(f"Redis缓存读取失败: {e}")
        
        return None
    
    async def set_cached_content(self, url: str, content: Dict[str, Any], ttl: int = None):
        """
        设置缓存内容
        """
        if ttl is None:
            ttl = self.default_ttl
        
        cache_key = self._generate_cache_key("web_content", url)
        
        # 设置Redis缓存
        try:
            serialized_data = pickle.dumps(content)
            self.redis_client.setex(cache_key, ttl, serialized_data)
        except Exception as e:
            print(f"Redis缓存写入失败: {e}")
        
        # 设置内存缓存
        self._set_memory_cache(cache_key, content, ttl=min(ttl, 300))
    
    def _set_memory_cache(self, key: str, data: Any, ttl: int):
        """
        设置内存缓存
        """
        # 检查缓存大小
        if len(self.memory_cache) >= self.memory_cache_size:
            # 删除最旧的条目
            oldest_key = min(self.memory_cache.keys(), 
                           key=lambda k: self.memory_cache[k]['created'])
            del self.memory_cache[oldest_key]
        
        self.memory_cache[key] = {
            'data': data,
            'created': datetime.now(),
            'expires': datetime.now() + timedelta(seconds=ttl)
        }
    
    async def get_cached_summary(self, content_hash: str) -> Optional[str]:
        """
        获取缓存的摘要
        """
        cache_key = self._generate_cache_key("summary", content_hash)
        
        try:
            cached_summary = self.redis_client.get(cache_key)
            if cached_summary:
                return cached_summary.decode('utf-8')
        except Exception as e:
            print(f"摘要缓存读取失败: {e}")
        
        return None
    
    async def set_cached_summary(self, content_hash: str, summary: str, ttl: int = 86400):
        """
        设置缓存的摘要 (默认24小时)
        """
        cache_key = self._generate_cache_key("summary", content_hash)
        
        try:
            self.redis_client.setex(cache_key, ttl, summary.encode('utf-8'))
        except Exception as e:
            print(f"摘要缓存写入失败: {e}")
    
    async def invalidate_cache(self, pattern: str):
        """
        使缓存失效
        """
        try:
            keys = self.redis_client.keys(pattern)
            if keys:
                self.redis_client.delete(*keys)
        except Exception as e:
            print(f"缓存失效操作失败: {e}")
    
    def get_cache_stats(self) -> Dict[str, Any]:
        """
        获取缓存统计信息
        """
        try:
            redis_info = self.redis_client.info()
            redis_stats = {
                'used_memory': redis_info.get('used_memory_human', 'N/A'),
                'connected_clients': redis_info.get('connected_clients', 0),
                'total_commands_processed': redis_info.get('total_commands_processed', 0)
            }
        except Exception:
            redis_stats = {'error': 'Redis连接失败'}
        
        return {
            'memory_cache_size': len(self.memory_cache),
            'memory_cache_limit': self.memory_cache_size,
            'redis_stats': redis_stats
        }
```

### 3. 资源优化策略

#### 内存和存储优化
```python
import gc
import psutil
import os
from typing import List
import logging

class ResourceOptimizer:
    """
    资源优化器
    """
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        self.memory_threshold = 0.8  # 内存使用阈值
        self.disk_threshold = 0.9    # 磁盘使用阈值
    
    def monitor_system_resources(self) -> Dict[str, Any]:
        """
        监控系统资源使用情况
        """
        # 内存使用情况
        memory = psutil.virtual_memory()
        memory_usage = {
            'total': memory.total,
            'available': memory.available,
            'percent': memory.percent,
            'used': memory.used
        }
        
        # 磁盘使用情况
        disk = psutil.disk_usage('/')
        disk_usage = {
            'total': disk.total,
            'used': disk.used,
            'free': disk.free,
            'percent': disk.used / disk.total
        }
        
        # CPU使用情况
        cpu_usage = psutil.cpu_percent(interval=1)
        
        return {
            'memory': memory_usage,
            'disk': disk_usage,
            'cpu': cpu_usage,
            'timestamp': datetime.now()
        }
    
    def optimize_memory_usage(self, articles: List[NewsArticle]) -> List[NewsArticle]:
        """
        优化内存使用
        """
        current_memory = psutil.virtual_memory().percent / 100
        
        if current_memory > self.memory_threshold:
            self.logger.warning(f"内存使用率过高: {current_memory:.2%}")
            
            # 1. 强制垃圾回收
            gc.collect()
            
            # 2. 清理文章内容中的冗余数据
            optimized_articles = []
            for article in articles:
                # 截断过长的内容
                if len(article.content) > 5000:
                    article.content = article.content[:5000] + "..."
                
                # 清理空白字符
                article.content = ' '.join(article.content.split())
                article.title = article.title.strip()
                
                # 去重实体和标签
                if article.entities:
                    article.entities = list(set(article.entities))
                if article.tags:
                    article.tags = list(set(article.tags))
                
                optimized_articles.append(article)
            
            # 3. 再次垃圾回收
            gc.collect()
            
            new_memory = psutil.virtual_memory().percent / 100
            self.logger.info(f"内存优化完成，使用率从 {current_memory:.2%} 降至 {new_memory:.2%}")
            
            return optimized_articles
        
        return articles
    
    def cleanup_old_files(self, directories: List[str], max_age_days: int = 30):
        """
        清理旧文件
        """
        current_time = datetime.now()
        total_freed = 0
        
        for directory in directories:
            if not os.path.exists(directory):
                continue
            
            for root, dirs, files in os.walk(directory):
                for file in files:
                    file_path = os.path.join(root, file)
                    
                    try:
                        # 检查文件修改时间
                        file_mtime = datetime.fromtimestamp(os.path.getmtime(file_path))
                        age = current_time - file_mtime
                        
                        if age.days > max_age_days:
                            file_size = os.path.getsize(file_path)
                            os.remove(file_path)
                            total_freed += file_size
                            self.logger.info(f"删除旧文件: {file_path}")
                    
                    except Exception as e:
                        self.logger.error(f"删除文件失败 {file_path}: {e}")
        
        self.logger.info(f"清理完成，释放空间: {total_freed / (1024*1024):.2f} MB")
    
    def optimize_disk_usage(self):
        """
        优化磁盘使用
        """
        disk_usage = psutil.disk_usage('/').used / psutil.disk_usage('/').total
        
        if disk_usage > self.disk_threshold:
            self.logger.warning(f"磁盘使用率过高: {disk_usage:.2%}")
            
            # 清理临时文件
            temp_dirs = ['/tmp', './logs', './output/temp']
            self.cleanup_old_files(temp_dirs, max_age_days=7)
            
            # 清理旧的输出文件
            output_dirs = ['./output']
            self.cleanup_old_files(output_dirs, max_age_days=30)
            
            # 压缩日志文件
            self.compress_log_files('./logs')
    
    def compress_log_files(self, log_directory: str):
        """
        压缩日志文件
        """
        import gzip
        
        for root, dirs, files in os.walk(log_directory):
            for file in files:
                if file.endswith('.log') and not file.endswith('.gz'):
                    file_path = os.path.join(root, file)
                    
                    try:
                        # 检查文件大小
                        file_size = os.path.getsize(file_path)
                        if file_size > 10 * 1024 * 1024:  # 大于10MB
                            
                            # 压缩文件
                            with open(file_path, 'rb') as f_in:
                                with gzip.open(f"{file_path}.gz", 'wb') as f_out:
                                    f_out.writelines(f_in)
                            
                            # 删除原文件
                            os.remove(file_path)
                            self.logger.info(f"压缩日志文件: {file_path}")
                    
                    except Exception as e:
                        self.logger.error(f"压缩文件失败 {file_path}: {e}")
    
    def auto_optimize(self, articles: List[NewsArticle]) -> List[NewsArticle]:
        """
        自动优化
        """
        # 监控资源使用
        resources = self.monitor_system_resources()
        
        # 内存优化
        if resources['memory']['percent'] > self.memory_threshold * 100:
            articles = self.optimize_memory_usage(articles)
        
        # 磁盘优化
        if resources['disk']['percent'] > self.disk_threshold:
            self.optimize_disk_usage()
        
        return articles
```

## 扩展与未来发展

### 1. 多语言支持扩展

#### 国际化框架设计
```python
class MultiLanguageNewsAggregator:
    """
    多语言新闻聚合器
    """
    
    def __init__(self):
        self.supported_languages = {
            'en': 'English',
            'zh': '中文',
            'ja': '日本語',
            'ko': '한국어',
            'es': 'Español',
            'fr': 'Français',
            'de': 'Deutsch',
            'ru': 'Русский'
        }
        
        self.language_specific_sources = {
            'en': [
                'https://feeds.bbci.co.uk/news/technology/rss.xml',
                'https://techcrunch.com/feed/'
            ],
            'zh': [
                'https://tech.sina.com.cn/rss/roll.xml',
                'https://www.36kr.com/feed'
            ],
            'ja': [
                'https://tech.nikkeibp.co.jp/rss/index.rdf'
            ]
        }
    
    async def detect_article_language(self, article: NewsArticle) -> str:
        """
        检测文章语言
        """
        # 使用Claude Code的AI能力进行语言检测
        detection_prompt = f"""
        请检测以下文本的语言，返回ISO 639-1语言代码：
        
        标题: {article.title}
        内容片段: {article.content[:200]}...
        
        仅返回语言代码，如: en, zh, ja, ko, es, fr, de, ru
        """
        
        # 模拟语言检测
        detected_language = await self.simulate_language_detection(detection_prompt)
        return detected_language
    
    async def translate_summary(self, summary: str, target_language: str) -> str:
        """
        翻译摘要
        """
        if target_language == 'en':
            return summary  # 假设原始摘要是英文
        
        translation_prompt = f"""
        请将以下英文摘要翻译为{self.supported_languages[target_language]}：
        
        原文: {summary}
        
        要求：
        1. 保持专业术语的准确性
        2. 保持摘要的简洁性
        3. 适应目标语言的表达习惯
        """
        
        translated_summary = await self.simulate_translation(translation_prompt)
        return translated_summary
    
    async def generate_multilingual_report(self, articles: List[NewsArticle]) -> Dict[str, str]:
        """
        生成多语言报告
        """
        # 按语言分组文章
        articles_by_language = {}
        for article in articles:
            lang = await self.detect_article_language(article)
            if lang not in articles_by_language:
                articles_by_language[lang] = []
            articles_by_language[lang].append(article)
        
        # 为每种语言生成报告
        multilingual_reports = {}
        
        for lang, lang_articles in articles_by_language.items():
            # 生成该语言的基础报告
            base_report = await self.generate_language_specific_report(lang_articles, lang)
            
            # 翻译为其他支持的语言
            for target_lang in self.supported_languages.keys():
                if target_lang != lang:
                    translated_report = await self.translate_summary(base_report, target_lang)
                    multilingual_reports[f"{lang}_to_{target_lang}"] = translated_report
                else:
                    multilingual_reports[lang] = base_report
        
        return multilingual_reports
```

### 2. AI增强功能

#### 深度分析引擎
```python
class AIEnhancedAnalysisEngine:
    """
    AI增强分析引擎
    """
    
    def __init__(self):
        self.analysis_capabilities = {
            'sentiment_analysis': '情感分析',
            'trend_prediction': '趋势预测', 
            'impact_assessment': '影响评估',
            'bias_detection': '偏见检测',
            'fact_verification': '事实核查',
            'expert_opinion_synthesis': '专家观点综合'
        }
    
    async def perform_sentiment_analysis(self, articles: List[NewsArticle]) -> Dict[str, Any]:
        """
        执行情感分析
        """
        sentiment_prompt = f"""
        对以下新闻文章进行情感分析：
        
        文章数量: {len(articles)}
        
        分析要求：
        1. 整体情感倾向（积极/中性/消极）
        2. 情感强度评分（1-10）
        3. 主要情感触发词
        4. 不同主题的情感分布
        
        返回JSON格式的分析结果。
        """
        
        sentiment_result = await self.simulate_ai_analysis(sentiment_prompt)
        return sentiment_result
    
    async def predict_trends(self, historical_articles: List[NewsArticle]) -> Dict[str, Any]:
        """
        预测趋势
        """
        trend_prompt = f"""
        基于历史新闻数据预测未来趋势：
        
        历史文章数量: {len(historical_articles)}
        时间跨度: {self.calculate_time_span(historical_articles)}
        
        预测维度：
        1. 技术发展趋势
        2. 市场变化预测
        3. 政策影响分析
        4. 社会关注度变化
        
        请提供具体的预测结果和置信度评估。
        """
        
        trend_prediction = await self.simulate_ai_analysis(trend_prompt)
        return trend_prediction
    
    async def assess_credibility(self, article: NewsArticle) -> Dict[str, Any]:
        """
        评估文章可信度
        """
        credibility_prompt = f"""
        评估以下新闻文章的可信度：
        
        标题: {article.title}
        来源: {article.source}
        内容: {article.content[:1000]}...
        
        评估维度：
        1. 来源权威性（1-10分）
        2. 内容客观性（1-10分）
        3. 事实支撑度（1-10分）
        4. 语言专业性（1-10分）
        5. 潜在偏见识别
        
        提供详细的分析报告和综合评分。
        """
        
        credibility_assessment = await self.simulate_ai_analysis(credibility_prompt)
        return credibility_assessment
    
    async def generate_expert_insights(self, domain_articles: List[NewsArticle]) -> str:
        """
        生成专家洞察
        """
        insights_prompt = f"""
        作为该领域的专家，基于以下新闻文章提供深度洞察：
        
        文章数量: {len(domain_articles)}
        主要话题: {self.extract_main_topics(domain_articles)}
        
        请提供：
        1. 行业发展的深层次分析
        2. 技术趋势的专业解读
        3. 市场机会和挑战识别
        4. 未来发展方向预测
        5. 投资和决策建议
        
        要求：专业、客观、具有前瞻性
        """
        
        expert_insights = await self.simulate_ai_analysis(insights_prompt)
        return expert_insights
    
    def calculate_time_span(self, articles: List[NewsArticle]) -> str:
        """
        计算时间跨度
        """
        if not articles:
            return "无数据"
        
        dates = [article.published_date for article in articles]
        start_date = min(dates)
        end_date = max(dates)
        
        time_span = end_date - start_date
        return f"{time_span.days}天"
    
    def extract_main_topics(self, articles: List[NewsArticle]) -> List[str]:
        """
        提取主要话题
        """
        all_tags = []
        for article in articles:
            if article.tags:
                all_tags.extend(article.tags)
        
        # 统计标签频率
        from collections import Counter
        tag_counts = Counter(all_tags)
        
        # 返回前5个最频繁的标签
        return [tag for tag, count in tag_counts.most_common(5)]
```

### 3. 个性化推荐系统

#### 用户偏好学习引擎
```python
class PersonalizationEngine:
    """
    个性化推荐引擎
    """
    
    def __init__(self):
        self.user_profiles = {}
        self.interaction_history = {}
        
    async def learn_user_preferences(self, user_id: str, interactions: List[Dict]) -> Dict[str, Any]:
        """
        学习用户偏好
        """
        if user_id not in self.user_profiles:
            self.user_profiles[user_id] = {
                'preferred_categories': {},
                'preferred_sources': {},
                'reading_time_patterns': {},
                'content_preferences': {
                    'length': 'medium',  # short, medium, long
                    'complexity': 'intermediate',  # basic, intermediate, advanced
                    'style': 'objective'  # objective, analytical, narrative
                }
            }
        
        profile = self.user_profiles[user_id]
        
        # 分析交互数据
        for interaction in interactions:
            action = interaction['action']  # read, like, share, bookmark
            article_id = interaction['article_id']
            timestamp = interaction['timestamp']
            
            # 更新偏好权重
            if action in ['like', 'share', 'bookmark']:
                article = self.get_article_by_id(article_id)
                
                # 更新类别偏好
                if article.category in profile['preferred_categories']:
                    profile['preferred_categories'][article.category] += 1
                else:
                    profile['preferred_categories'][article.category] = 1
                
                # 更新来源偏好
                if article.source in profile['preferred_sources']:
                    profile['preferred_sources'][article.source] += 1
                else:
                    profile['preferred_sources'][article.source] = 1
        
        return profile
    
    async def recommend_articles(self, user_id: str, candidate_articles: List[NewsArticle]) -> List[NewsArticle]:
        """
        推荐文章
        """
        if user_id not in self.user_profiles:
            # 新用户，返回默认推荐
            return self.get_default_recommendations(candidate_articles)
        
        profile = self.user_profiles[user_id]
        scored_articles = []
        
        for article in candidate_articles:
            score = self.calculate_relevance_score(article, profile)
            scored_articles.append((article, score))
        
        # 按分数排序
        scored_articles.sort(key=lambda x: x[1], reverse=True)
        
        # 返回前N篇推荐文章
        recommended_articles = [article for article, score in scored_articles[:20]]
        return recommended_articles
    
    def calculate_relevance_score(self, article: NewsArticle, user_profile: Dict) -> float:
        """
        计算相关性分数
        """
        score = 0.0
        
        # 类别偏好评分
        category_preference = user_profile['preferred_categories'].get(article.category, 0)
        score += category_preference * 0.3
        
        # 来源偏好评分
        source_preference = user_profile['preferred_sources'].get(article.source, 0)
        score += source_preference * 0.2
        
        # 内容质量评分
        score += article.quality_score * 0.3
        
        # 时效性评分
        time_diff = datetime.now() - article.published_date
        if time_diff < timedelta(hours=6):
            score += 0.2
        elif time_diff < timedelta(hours=24):
            score += 0.1
        
        return score
    
    def get_default_recommendations(self, articles: List[NewsArticle]) -> List[NewsArticle]:
        """
        获取默认推荐
        """
        # 基于质量分数和时效性的默认推荐
        scored_articles = []
        
        for article in articles:
            # 综合质量和时效性
            time_factor = max(0, 1 - (datetime.now() - article.published_date).total_seconds() / 86400)
            score = article.quality_score * 0.7 + time_factor * 0.3
            scored_articles.append((article, score))
        
        scored_articles.sort(key=lambda x: x[1], reverse=True)
        return [article for article, score in scored_articles[:20]]
```

## 总结

本文深入分析了Claude Code的网页访问机制，并基于其工具生态设计了一套完整的智能新闻聚合系统。主要贡献包括：

### 技术创新点

1. **工具链协同机制**：充分利用Claude Code的WebFetch、WebSearch等工具的协同能力
2. **多层次总结架构**：从单篇文章到领域综合的层次化总结体系
3. **智能质量评估**：基于多维度指标的新闻源和内容质量评估
4. **高效并发处理**：异步批处理和资源优化策略

### 系统优势

1. **自动化程度高**：从源发现到报告生成的全流程自动化
2. **扩展性强**：支持多语言、多领域的灵活扩展
3. **智能化水平高**：集成AI分析和个性化推荐
4. **运维友好**：完整的监控、备份和优化机制

### 应用价值

1. **个人用户**：获得高质量的个性化新闻聚合服务
2. **企业用户**：构建行业情报和竞争分析系统
3. **研究机构**：支持大规模信息收集和趋势分析
4. **媒体机构**：提升内容生产和分发效率

通过本系统的设计和实现，展示了Claude Code在复杂信息处理任务中的强大能力，为构建下一代智能信息系统提供了有价值的参考。