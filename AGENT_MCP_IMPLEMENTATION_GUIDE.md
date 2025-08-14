# Agent与MCP实现完整指南

## 概述

本文档提供了构建自定义Agent和Model Context Protocol (MCP)服务器的完整实现指南，包括架构设计、代码实现、部署方案和最佳实践。

## 目录
1. [Agent架构设计](#agent架构设计)
2. [MCP协议实现](#mcp协议实现)
3. [核心组件开发](#核心组件开发)
4. [实际案例实现](#实际案例实现)
5. [部署和集成](#部署和集成)
6. [测试和调试](#测试和调试)
7. [扩展和优化](#扩展和优化)
8. [生产环境部署](#生产环境部署)

## Agent架构设计

### 1. Agent基础架构

#### 核心组件图
```
┌─────────────────────────────────────────────────────────┐
│                    Agent Framework                      │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │   Memory    │  │   Tools     │  │  Planning   │      │
│  │  Management │  │  Registry   │  │   Engine    │      │
│  └─────┬───────┘  └─────┬───────┘  └─────┬───────┘      │
├────────┼──────────────────┼──────────────────┼────────────┤
│        │                  │                  │           │
│  ┌─────▼──────────────────▼──────────────────▼───────┐   │
│  │              Execution Engine                    │   │
│  │     ┌─────────────┐  ┌─────────────┐             │   │
│  │     │ Task Queue  │  │ Result      │             │   │
│  │     │ Manager     │  │ Processor   │             │   │
│  │     └─────────────┘  └─────────────┘             │   │
│  └─────┬───────────────────────────────────────────┘   │
├────────┼─────────────────────────────────────────────────┤
│        │                                                │
│  ┌─────▼───────┐  ┌─────────────┐  ┌─────────────┐     │
│  │   MCP       │  │ Claude Code │  │  External   │     │
│  │ Interface   │  │ Integration │  │   APIs      │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└─────────────────────────────────────────────────────────┘
```

#### Agent基础类实现
```python
#!/usr/bin/env python3
"""
Agent基础框架实现
"""

import asyncio
import json
import logging
import uuid
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Dict, List, Optional, Callable, Union
from enum import Enum

class TaskStatus(Enum):
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"

class AgentCapability(Enum):
    WEB_SEARCH = "web_search"
    FILE_OPERATIONS = "file_operations"
    CODE_EXECUTION = "code_execution"
    DATA_ANALYSIS = "data_analysis"
    CONTENT_GENERATION = "content_generation"

@dataclass
class Task:
    """任务数据结构"""
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    description: str = ""
    type: str = "generic"
    parameters: Dict[str, Any] = field(default_factory=dict)
    status: TaskStatus = TaskStatus.PENDING
    result: Optional[Any] = None
    error: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.now)
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    parent_task_id: Optional[str] = None
    subtasks: List[str] = field(default_factory=list)
    context: Dict[str, Any] = field(default_factory=dict)

@dataclass
class AgentConfig:
    """Agent配置"""
    name: str
    description: str
    capabilities: List[AgentCapability]
    max_concurrent_tasks: int = 5
    memory_limit: int = 1000  # 记忆条目数限制
    timeout_seconds: int = 300
    retry_attempts: int = 3
    tools: List[str] = field(default_factory=list)

class ToolRegistry:
    """工具注册表"""
    
    def __init__(self):
        self.tools: Dict[str, Callable] = {}
        self.tool_descriptions: Dict[str, str] = {}
    
    def register_tool(self, name: str, func: Callable, description: str = ""):
        """注册工具"""
        self.tools[name] = func
        self.tool_descriptions[name] = description
        logging.info(f"Registered tool: {name}")
    
    def get_tool(self, name: str) -> Optional[Callable]:
        """获取工具"""
        return self.tools.get(name)
    
    def list_tools(self) -> Dict[str, str]:
        """列出所有工具"""
        return self.tool_descriptions.copy()

class Memory:
    """Agent记忆系统"""
    
    def __init__(self, limit: int = 1000):
        self.limit = limit
        self.short_term: List[Dict[str, Any]] = []
        self.long_term: Dict[str, Any] = {}
        self.working_memory: Dict[str, Any] = {}
    
    def store_short_term(self, key: str, value: Any, metadata: Dict = None):
        """存储短期记忆"""
        entry = {
            "key": key,
            "value": value,
            "timestamp": datetime.now(),
            "metadata": metadata or {}
        }
        
        self.short_term.append(entry)
        
        # 超出限制时清理旧记忆
        if len(self.short_term) > self.limit:
            self.short_term = self.short_term[-self.limit:]
    
    def store_long_term(self, key: str, value: Any):
        """存储长期记忆"""
        self.long_term[key] = {
            "value": value,
            "created_at": datetime.now(),
            "access_count": 0
        }
    
    def retrieve(self, key: str, memory_type: str = "both") -> Any:
        """检索记忆"""
        if memory_type in ["both", "working"] and key in self.working_memory:
            return self.working_memory[key]
        
        if memory_type in ["both", "long_term"] and key in self.long_term:
            entry = self.long_term[key]
            entry["access_count"] += 1
            entry["last_accessed"] = datetime.now()
            return entry["value"]
        
        if memory_type in ["both", "short_term"]:
            for entry in reversed(self.short_term):
                if entry["key"] == key:
                    return entry["value"]
        
        return None
    
    def search_memories(self, query: str, limit: int = 10) -> List[Dict]:
        """搜索记忆"""
        results = []
        
        # 搜索短期记忆
        for entry in reversed(self.short_term):
            if query.lower() in str(entry["value"]).lower():
                results.append(entry)
                if len(results) >= limit:
                    break
        
        return results

class PlanningEngine:
    """任务规划引擎"""
    
    def __init__(self, agent_config: AgentConfig):
        self.config = agent_config
        self.strategies: Dict[str, Callable] = {}
        self.register_default_strategies()
    
    def register_default_strategies(self):
        """注册默认策略"""
        self.strategies["sequential"] = self._sequential_strategy
        self.strategies["parallel"] = self._parallel_strategy
        self.strategies["divide_and_conquer"] = self._divide_and_conquer_strategy
    
    async def plan_task(self, task: Task) -> List[Task]:
        """规划任务执行"""
        strategy = task.context.get("strategy", "sequential")
        
        if strategy in self.strategies:
            return await self.strategies[strategy](task)
        else:
            return [task]  # 默认返回原任务
    
    async def _sequential_strategy(self, task: Task) -> List[Task]:
        """顺序执行策略"""
        if task.type == "complex":
            # 将复杂任务分解为子任务
            subtasks = []
            steps = task.parameters.get("steps", [])
            
            for i, step in enumerate(steps):
                subtask = Task(
                    description=step.get("description", f"Step {i+1}"),
                    type=step.get("type", "simple"),
                    parameters=step.get("parameters", {}),
                    parent_task_id=task.id,
                    context={"sequence_order": i}
                )
                subtasks.append(subtask)
                task.subtasks.append(subtask.id)
            
            return subtasks
        
        return [task]
    
    async def _parallel_strategy(self, task: Task) -> List[Task]:
        """并行执行策略"""
        if task.type == "parallel_batch":
            subtasks = []
            items = task.parameters.get("items", [])
            
            for i, item in enumerate(items):
                subtask = Task(
                    description=f"Process item {i+1}",
                    type="parallel_item",
                    parameters={"item": item},
                    parent_task_id=task.id,
                    context={"parallel_index": i}
                )
                subtasks.append(subtask)
                task.subtasks.append(subtask.id)
            
            return subtasks
        
        return [task]
    
    async def _divide_and_conquer_strategy(self, task: Task) -> List[Task]:
        """分治策略"""
        if task.type == "large_dataset":
            subtasks = []
            data = task.parameters.get("data", [])
            chunk_size = task.parameters.get("chunk_size", 100)
            
            for i in range(0, len(data), chunk_size):
                chunk = data[i:i + chunk_size]
                subtask = Task(
                    description=f"Process chunk {i//chunk_size + 1}",
                    type="data_chunk",
                    parameters={"chunk": chunk, "chunk_index": i//chunk_size},
                    parent_task_id=task.id
                )
                subtasks.append(subtask)
                task.subtasks.append(subtask.id)
            
            return subtasks
        
        return [task]

class BaseAgent(ABC):
    """Agent基类"""
    
    def __init__(self, config: AgentConfig):
        self.config = config
        self.tool_registry = ToolRegistry()
        self.memory = Memory(config.memory_limit)
        self.planning_engine = PlanningEngine(config)
        self.task_queue: asyncio.Queue = asyncio.Queue()
        self.active_tasks: Dict[str, Task] = {}
        self.completed_tasks: Dict[str, Task] = {}
        self.running = False
        
        # 设置日志
        self.logger = logging.getLogger(f"Agent-{config.name}")
        
        # 注册默认工具
        self._register_default_tools()
    
    def _register_default_tools(self):
        """注册默认工具"""
        self.tool_registry.register_tool("echo", self._echo_tool, "Echo input text")
        self.tool_registry.register_tool("sleep", self._sleep_tool, "Sleep for specified seconds")
        self.tool_registry.register_tool("memory_store", self._memory_store_tool, "Store data in memory")
        self.tool_registry.register_tool("memory_retrieve", self._memory_retrieve_tool, "Retrieve data from memory")
    
    async def _echo_tool(self, text: str) -> str:
        """回显工具"""
        return f"Echo: {text}"
    
    async def _sleep_tool(self, seconds: float) -> str:
        """睡眠工具"""
        await asyncio.sleep(seconds)
        return f"Slept for {seconds} seconds"
    
    async def _memory_store_tool(self, key: str, value: Any, memory_type: str = "short_term") -> str:
        """内存存储工具"""
        if memory_type == "short_term":
            self.memory.store_short_term(key, value)
        elif memory_type == "long_term":
            self.memory.store_long_term(key, value)
        elif memory_type == "working":
            self.memory.working_memory[key] = value
        
        return f"Stored {key} in {memory_type} memory"
    
    async def _memory_retrieve_tool(self, key: str, memory_type: str = "both") -> Any:
        """内存检索工具"""
        return self.memory.retrieve(key, memory_type)
    
    async def start(self):
        """启动Agent"""
        self.running = True
        self.logger.info(f"Agent {self.config.name} started")
        
        # 启动任务处理循环
        task_processor = asyncio.create_task(self._process_tasks())
        
        return task_processor
    
    async def stop(self):
        """停止Agent"""
        self.running = False
        self.logger.info(f"Agent {self.config.name} stopped")
    
    async def add_task(self, task: Task) -> str:
        """添加任务"""
        await self.task_queue.put(task)
        self.logger.info(f"Task added: {task.id} - {task.description}")
        return task.id
    
    async def get_task_status(self, task_id: str) -> Optional[TaskStatus]:
        """获取任务状态"""
        if task_id in self.active_tasks:
            return self.active_tasks[task_id].status
        elif task_id in self.completed_tasks:
            return self.completed_tasks[task_id].status
        return None
    
    async def get_task_result(self, task_id: str) -> Optional[Any]:
        """获取任务结果"""
        if task_id in self.completed_tasks:
            return self.completed_tasks[task_id].result
        return None
    
    async def _process_tasks(self):
        """处理任务队列"""
        while self.running:
            try:
                # 控制并发任务数量
                if len(self.active_tasks) >= self.config.max_concurrent_tasks:
                    await asyncio.sleep(0.1)
                    continue
                
                # 获取任务
                try:
                    task = await asyncio.wait_for(self.task_queue.get(), timeout=1.0)
                except asyncio.TimeoutError:
                    continue
                
                # 启动任务处理
                asyncio.create_task(self._execute_task(task))
                
            except Exception as e:
                self.logger.error(f"Error in task processing loop: {e}")
                await asyncio.sleep(1)
    
    async def _execute_task(self, task: Task):
        """执行任务"""
        task.status = TaskStatus.RUNNING
        task.started_at = datetime.now()
        self.active_tasks[task.id] = task
        
        try:
            self.logger.info(f"Executing task: {task.id}")
            
            # 任务规划
            planned_tasks = await self.planning_engine.plan_task(task)
            
            if len(planned_tasks) > 1:
                # 执行子任务
                await self._execute_subtasks(task, planned_tasks)
            else:
                # 执行单个任务
                result = await self._execute_single_task(task)
                task.result = result
            
            task.status = TaskStatus.COMPLETED
            task.completed_at = datetime.now()
            
            self.logger.info(f"Task completed: {task.id}")
            
        except Exception as e:
            task.status = TaskStatus.FAILED
            task.error = str(e)
            task.completed_at = datetime.now()
            
            self.logger.error(f"Task failed: {task.id} - {e}")
        
        finally:
            # 移动到已完成任务
            if task.id in self.active_tasks:
                del self.active_tasks[task.id]
            self.completed_tasks[task.id] = task
            
            # 存储任务结果到记忆
            self.memory.store_short_term(
                f"task_result_{task.id}",
                {
                    "task": task,
                    "result": task.result,
                    "status": task.status.value
                }
            )
    
    async def _execute_subtasks(self, parent_task: Task, subtasks: List[Task]):
        """执行子任务"""
        strategy = parent_task.context.get("strategy", "sequential")
        
        if strategy == "parallel":
            # 并行执行子任务
            results = await asyncio.gather(
                *[self._execute_single_task(subtask) for subtask in subtasks],
                return_exceptions=True
            )
            parent_task.result = results
        else:
            # 顺序执行子任务
            results = []
            for subtask in subtasks:
                result = await self._execute_single_task(subtask)
                results.append(result)
            parent_task.result = results
    
    @abstractmethod
    async def _execute_single_task(self, task: Task) -> Any:
        """执行单个任务 - 子类必须实现"""
        pass

class SpecializedAgent(BaseAgent):
    """专门化Agent实现"""
    
    def __init__(self, config: AgentConfig):
        super().__init__(config)
        self._register_specialized_tools()
    
    def _register_specialized_tools(self):
        """注册专门化工具"""
        # Web搜索工具
        if AgentCapability.WEB_SEARCH in self.config.capabilities:
            self.tool_registry.register_tool("web_search", self._web_search_tool, "Search the web")
        
        # 文件操作工具
        if AgentCapability.FILE_OPERATIONS in self.config.capabilities:
            self.tool_registry.register_tool("read_file", self._read_file_tool, "Read file content")
            self.tool_registry.register_tool("write_file", self._write_file_tool, "Write file content")
        
        # 代码执行工具
        if AgentCapability.CODE_EXECUTION in self.config.capabilities:
            self.tool_registry.register_tool("execute_code", self._execute_code_tool, "Execute code")
    
    async def _web_search_tool(self, query: str, num_results: int = 5) -> List[Dict]:
        """Web搜索工具"""
        # 模拟Web搜索
        results = []
        for i in range(num_results):
            results.append({
                "title": f"Search result {i+1} for '{query}'",
                "url": f"https://example.com/result-{i+1}",
                "snippet": f"This is a search result snippet for query '{query}'"
            })
        
        # 存储搜索结果到记忆
        self.memory.store_short_term(f"search_{query}", results)
        
        return results
    
    async def _read_file_tool(self, file_path: str) -> str:
        """读取文件工具"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 存储文件内容到记忆
            self.memory.store_short_term(f"file_content_{file_path}", content)
            
            return content
        except Exception as e:
            return f"Error reading file: {e}"
    
    async def _write_file_tool(self, file_path: str, content: str) -> str:
        """写入文件工具"""
        try:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            
            return f"File written successfully: {file_path}"
        except Exception as e:
            return f"Error writing file: {e}"
    
    async def _execute_code_tool(self, code: str, language: str = "python") -> Dict:
        """代码执行工具"""
        if language == "python":
            try:
                # 在受限环境中执行Python代码
                exec_globals = {"__builtins__": {}}
                exec_locals = {}
                
                exec(code, exec_globals, exec_locals)
                
                return {
                    "success": True,
                    "result": exec_locals,
                    "output": "Code executed successfully"
                }
            except Exception as e:
                return {
                    "success": False,
                    "error": str(e),
                    "output": f"Execution failed: {e}"
                }
        else:
            return {
                "success": False,
                "error": f"Unsupported language: {language}",
                "output": "Language not supported"
            }
    
    async def _execute_single_task(self, task: Task) -> Any:
        """执行单个任务"""
        task_type = task.type
        parameters = task.parameters
        
        if task_type == "web_search":
            query = parameters.get("query", "")
            num_results = parameters.get("num_results", 5)
            return await self._web_search_tool(query, num_results)
        
        elif task_type == "file_read":
            file_path = parameters.get("file_path", "")
            return await self._read_file_tool(file_path)
        
        elif task_type == "file_write":
            file_path = parameters.get("file_path", "")
            content = parameters.get("content", "")
            return await self._write_file_tool(file_path, content)
        
        elif task_type == "code_execution":
            code = parameters.get("code", "")
            language = parameters.get("language", "python")
            return await self._execute_code_tool(code, language)
        
        elif task_type == "tool_call":
            tool_name = parameters.get("tool_name", "")
            tool_args = parameters.get("args", {})
            
            tool = self.tool_registry.get_tool(tool_name)
            if tool:
                return await tool(**tool_args)
            else:
                raise ValueError(f"Tool not found: {tool_name}")
        
        elif task_type == "analysis":
            data = parameters.get("data", [])
            analysis_type = parameters.get("analysis_type", "summary")
            
            if analysis_type == "summary":
                return {
                    "total_items": len(data),
                    "data_type": type(data).__name__,
                    "summary": f"Analyzed {len(data)} items"
                }
        
        else:
            # 默认处理
            return {
                "task_type": task_type,
                "parameters": parameters,
                "message": "Task processed with default handler"
            }
```

## MCP协议实现

### 1. MCP服务器基础框架

#### MCP协议实现
```python
#!/usr/bin/env python3
"""
Model Context Protocol (MCP) 服务器实现
"""

import asyncio
import json
import logging
import uuid
from abc import ABC, abstractmethod
from dataclasses import dataclass, field, asdict
from datetime import datetime
from typing import Any, Dict, List, Optional, Union, Callable
from enum import Enum

class MCPMessageType(Enum):
    REQUEST = "request"
    RESPONSE = "response"
    NOTIFICATION = "notification"

class MCPMethod(Enum):
    # 核心协议方法
    INITIALIZE = "initialize"
    PING = "ping"
    
    # 资源管理
    RESOURCES_LIST = "resources/list"
    RESOURCES_READ = "resources/read"
    RESOURCES_SUBSCRIBE = "resources/subscribe"
    RESOURCES_UNSUBSCRIBE = "resources/unsubscribe"
    
    # 工具管理
    TOOLS_LIST = "tools/list"
    TOOLS_CALL = "tools/call"
    
    # 提示管理
    PROMPTS_LIST = "prompts/list"
    PROMPTS_GET = "prompts/get"
    
    # 通知
    NOTIFICATION_CANCELLED = "notifications/cancelled"
    NOTIFICATION_PROGRESS = "notifications/progress"
    NOTIFICATION_MESSAGE = "notifications/message"

@dataclass
class MCPMessage:
    """MCP消息基类"""
    jsonrpc: str = "2.0"
    id: Optional[Union[str, int]] = None
    method: Optional[str] = None
    params: Optional[Dict[str, Any]] = None
    result: Optional[Any] = None
    error: Optional[Dict[str, Any]] = None
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        data = {}
        if self.jsonrpc:
            data["jsonrpc"] = self.jsonrpc
        if self.id is not None:
            data["id"] = self.id
        if self.method:
            data["method"] = self.method
        if self.params is not None:
            data["params"] = self.params
        if self.result is not None:
            data["result"] = self.result
        if self.error is not None:
            data["error"] = self.error
        return data
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'MCPMessage':
        """从字典创建"""
        return cls(
            jsonrpc=data.get("jsonrpc", "2.0"),
            id=data.get("id"),
            method=data.get("method"),
            params=data.get("params"),
            result=data.get("result"),
            error=data.get("error")
        )

@dataclass
class MCPResource:
    """MCP资源"""
    uri: str
    name: str
    description: Optional[str] = None
    mimeType: Optional[str] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

@dataclass
class MCPTool:
    """MCP工具"""
    name: str
    description: str
    inputSchema: Dict[str, Any]
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

@dataclass
class MCPPrompt:
    """MCP提示"""
    name: str
    description: str
    arguments: Optional[List[Dict[str, Any]]] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

class MCPError(Exception):
    """MCP错误"""
    
    def __init__(self, code: int, message: str, data: Optional[Any] = None):
        self.code = code
        self.message = message
        self.data = data
        super().__init__(message)
    
    def to_dict(self) -> Dict[str, Any]:
        error = {"code": self.code, "message": self.message}
        if self.data is not None:
            error["data"] = self.data
        return error

# 标准MCP错误码
class MCPErrorCode:
    PARSE_ERROR = -32700
    INVALID_REQUEST = -32600
    METHOD_NOT_FOUND = -32601
    INVALID_PARAMS = -32602
    INTERNAL_ERROR = -32603
    
    # MCP特定错误码
    INVALID_RESOURCE = -32001
    RESOURCE_NOT_FOUND = -32002
    TOOL_ERROR = -32003

class MCPTransport(ABC):
    """MCP传输层抽象"""
    
    @abstractmethod
    async def send_message(self, message: MCPMessage):
        """发送消息"""
        pass
    
    @abstractmethod
    async def receive_message(self) -> MCPMessage:
        """接收消息"""
        pass
    
    @abstractmethod
    async def close(self):
        """关闭连接"""
        pass

class StdioTransport(MCPTransport):
    """标准输入输出传输层"""
    
    def __init__(self):
        self.running = False
        self.message_queue = asyncio.Queue()
    
    async def start(self):
        """启动传输层"""
        self.running = True
        # 启动消息读取任务
        asyncio.create_task(self._read_messages())
    
    async def _read_messages(self):
        """读取标准输入消息"""
        import sys
        
        while self.running:
            try:
                # 读取一行JSON消息
                line = await asyncio.get_event_loop().run_in_executor(
                    None, sys.stdin.readline
                )
                
                if not line:
                    break
                
                line = line.strip()
                if line:
                    try:
                        data = json.loads(line)
                        message = MCPMessage.from_dict(data)
                        await self.message_queue.put(message)
                    except json.JSONDecodeError as e:
                        logging.error(f"Failed to parse JSON: {e}")
                        
            except Exception as e:
                logging.error(f"Error reading message: {e}")
                break
    
    async def send_message(self, message: MCPMessage):
        """发送消息到标准输出"""
        import sys
        
        try:
            json_str = json.dumps(message.to_dict())
            print(json_str, flush=True)
        except Exception as e:
            logging.error(f"Failed to send message: {e}")
    
    async def receive_message(self) -> MCPMessage:
        """接收消息"""
        return await self.message_queue.get()
    
    async def close(self):
        """关闭传输层"""
        self.running = False

class WebSocketTransport(MCPTransport):
    """WebSocket传输层"""
    
    def __init__(self, websocket):
        self.websocket = websocket
    
    async def send_message(self, message: MCPMessage):
        """发送WebSocket消息"""
        try:
            json_str = json.dumps(message.to_dict())
            await self.websocket.send(json_str)
        except Exception as e:
            logging.error(f"Failed to send WebSocket message: {e}")
    
    async def receive_message(self) -> MCPMessage:
        """接收WebSocket消息"""
        try:
            json_str = await self.websocket.recv()
            data = json.loads(json_str)
            return MCPMessage.from_dict(data)
        except Exception as e:
            logging.error(f"Failed to receive WebSocket message: {e}")
            raise
    
    async def close(self):
        """关闭WebSocket连接"""
        await self.websocket.close()

class MCPServer:
    """MCP服务器"""
    
    def __init__(self, name: str, version: str):
        self.name = name
        self.version = version
        self.transport: Optional[MCPTransport] = None
        self.initialized = False
        
        # 注册表
        self.resources: Dict[str, MCPResource] = {}
        self.tools: Dict[str, MCPTool] = {}
        self.prompts: Dict[str, MCPPrompt] = {}
        
        # 处理器注册表
        self.resource_handlers: Dict[str, Callable] = {}
        self.tool_handlers: Dict[str, Callable] = {}
        self.prompt_handlers: Dict[str, Callable] = {}
        
        # 运行状态
        self.running = False
        
        # 设置日志
        self.logger = logging.getLogger(f"MCPServer-{name}")
    
    def set_transport(self, transport: MCPTransport):
        """设置传输层"""
        self.transport = transport
    
    def register_resource(self, resource: MCPResource, handler: Callable):
        """注册资源"""
        self.resources[resource.uri] = resource
        self.resource_handlers[resource.uri] = handler
        self.logger.info(f"Registered resource: {resource.uri}")
    
    def register_tool(self, tool: MCPTool, handler: Callable):
        """注册工具"""
        self.tools[tool.name] = tool
        self.tool_handlers[tool.name] = handler
        self.logger.info(f"Registered tool: {tool.name}")
    
    def register_prompt(self, prompt: MCPPrompt, handler: Callable):
        """注册提示"""
        self.prompts[prompt.name] = prompt
        self.prompt_handlers[prompt.name] = handler
        self.logger.info(f"Registered prompt: {prompt.name}")
    
    async def start(self):
        """启动服务器"""
        if not self.transport:
            raise ValueError("Transport not set")
        
        self.running = True
        self.logger.info(f"MCP Server {self.name} starting...")
        
        # 启动传输层
        if hasattr(self.transport, 'start'):
            await self.transport.start()
        
        # 启动消息处理循环
        await self._message_loop()
    
    async def stop(self):
        """停止服务器"""
        self.running = False
        if self.transport:
            await self.transport.close()
        self.logger.info(f"MCP Server {self.name} stopped")
    
    async def _message_loop(self):
        """消息处理循环"""
        while self.running:
            try:
                # 接收消息
                message = await self.transport.receive_message()
                
                # 处理消息
                asyncio.create_task(self._handle_message(message))
                
            except Exception as e:
                self.logger.error(f"Error in message loop: {e}")
                await asyncio.sleep(1)
    
    async def _handle_message(self, message: MCPMessage):
        """处理消息"""
        try:
            if message.method:
                # 请求消息
                response = await self._handle_request(message)
                if response and message.id is not None:
                    response.id = message.id
                    await self.transport.send_message(response)
            
        except MCPError as e:
            # MCP错误
            error_response = MCPMessage(
                id=message.id,
                error=e.to_dict()
            )
            await self.transport.send_message(error_response)
            
        except Exception as e:
            # 内部错误
            error_response = MCPMessage(
                id=message.id,
                error=MCPError(
                    MCPErrorCode.INTERNAL_ERROR,
                    f"Internal server error: {str(e)}"
                ).to_dict()
            )
            await self.transport.send_message(error_response)
    
    async def _handle_request(self, message: MCPMessage) -> Optional[MCPMessage]:
        """处理请求"""
        method = message.method
        params = message.params or {}
        
        if method == MCPMethod.INITIALIZE.value:
            return await self._handle_initialize(params)
        elif method == MCPMethod.PING.value:
            return await self._handle_ping(params)
        elif method == MCPMethod.RESOURCES_LIST.value:
            return await self._handle_resources_list(params)
        elif method == MCPMethod.RESOURCES_READ.value:
            return await self._handle_resources_read(params)
        elif method == MCPMethod.TOOLS_LIST.value:
            return await self._handle_tools_list(params)
        elif method == MCPMethod.TOOLS_CALL.value:
            return await self._handle_tools_call(params)
        elif method == MCPMethod.PROMPTS_LIST.value:
            return await self._handle_prompts_list(params)
        elif method == MCPMethod.PROMPTS_GET.value:
            return await self._handle_prompts_get(params)
        else:
            raise MCPError(
                MCPErrorCode.METHOD_NOT_FOUND,
                f"Method not found: {method}"
            )
    
    async def _handle_initialize(self, params: Dict) -> MCPMessage:
        """处理初始化请求"""
        client_info = params.get("clientInfo", {})
        protocol_version = params.get("protocolVersion", "2024-11-05")
        
        self.initialized = True
        
        server_info = {
            "name": self.name,
            "version": self.version
        }
        
        capabilities = {
            "resources": bool(self.resources),
            "tools": bool(self.tools),
            "prompts": bool(self.prompts)
        }
        
        return MCPMessage(
            result={
                "protocolVersion": protocol_version,
                "serverInfo": server_info,
                "capabilities": capabilities
            }
        )
    
    async def _handle_ping(self, params: Dict) -> MCPMessage:
        """处理ping请求"""
        return MCPMessage(result={})
    
    async def _handle_resources_list(self, params: Dict) -> MCPMessage:
        """处理资源列表请求"""
        resources = [resource.to_dict() for resource in self.resources.values()]
        return MCPMessage(result={"resources": resources})
    
    async def _handle_resources_read(self, params: Dict) -> MCPMessage:
        """处理资源读取请求"""
        uri = params.get("uri")
        if not uri:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing uri parameter")
        
        if uri not in self.resources:
            raise MCPError(MCPErrorCode.RESOURCE_NOT_FOUND, f"Resource not found: {uri}")
        
        handler = self.resource_handlers.get(uri)
        if not handler:
            raise MCPError(MCPErrorCode.INTERNAL_ERROR, f"No handler for resource: {uri}")
        
        try:
            content = await handler(uri, params)
            return MCPMessage(result={"contents": [content]})
        except Exception as e:
            raise MCPError(MCPErrorCode.INTERNAL_ERROR, f"Handler error: {str(e)}")
    
    async def _handle_tools_list(self, params: Dict) -> MCPMessage:
        """处理工具列表请求"""
        tools = [tool.to_dict() for tool in self.tools.values()]
        return MCPMessage(result={"tools": tools})
    
    async def _handle_tools_call(self, params: Dict) -> MCPMessage:
        """处理工具调用请求"""
        name = params.get("name")
        arguments = params.get("arguments", {})
        
        if not name:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing name parameter")
        
        if name not in self.tools:
            raise MCPError(MCPErrorCode.METHOD_NOT_FOUND, f"Tool not found: {name}")
        
        handler = self.tool_handlers.get(name)
        if not handler:
            raise MCPError(MCPErrorCode.INTERNAL_ERROR, f"No handler for tool: {name}")
        
        try:
            result = await handler(name, arguments)
            return MCPMessage(result={"content": [{"type": "text", "text": str(result)}]})
        except Exception as e:
            raise MCPError(MCPErrorCode.TOOL_ERROR, f"Tool execution error: {str(e)}")
    
    async def _handle_prompts_list(self, params: Dict) -> MCPMessage:
        """处理提示列表请求"""
        prompts = [prompt.to_dict() for prompt in self.prompts.values()]
        return MCPMessage(result={"prompts": prompts})
    
    async def _handle_prompts_get(self, params: Dict) -> MCPMessage:
        """处理提示获取请求"""
        name = params.get("name")
        arguments = params.get("arguments", {})
        
        if not name:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing name parameter")
        
        if name not in self.prompts:
            raise MCPError(MCPErrorCode.METHOD_NOT_FOUND, f"Prompt not found: {name}")
        
        handler = self.prompt_handlers.get(name)
        if not handler:
            raise MCPError(MCPErrorCode.INTERNAL_ERROR, f"No handler for prompt: {name}")
        
        try:
            result = await handler(name, arguments)
            return MCPMessage(result={"messages": result})
        except Exception as e:
            raise MCPError(MCPErrorCode.INTERNAL_ERROR, f"Prompt handler error: {str(e)}")

# MCP客户端实现
class MCPClient:
    """MCP客户端"""
    
    def __init__(self, name: str, version: str):
        self.name = name
        self.version = version
        self.transport: Optional[MCPTransport] = None
        self.initialized = False
        self.request_id = 0
        self.pending_requests: Dict[Union[str, int], asyncio.Future] = {}
    
    def set_transport(self, transport: MCPTransport):
        """设置传输层"""
        self.transport = transport
    
    async def connect(self):
        """连接到服务器"""
        if not self.transport:
            raise ValueError("Transport not set")
        
        # 启动传输层
        if hasattr(self.transport, 'start'):
            await self.transport.start()
        
        # 启动消息处理循环
        asyncio.create_task(self._message_loop())
        
        # 发送初始化请求
        await self.initialize()
    
    async def initialize(self):
        """初始化连接"""
        params = {
            "protocolVersion": "2024-11-05",
            "clientInfo": {
                "name": self.name,
                "version": self.version
            },
            "capabilities": {}
        }
        
        result = await self.send_request(MCPMethod.INITIALIZE.value, params)
        self.initialized = True
        return result
    
    async def send_request(self, method: str, params: Dict = None) -> Any:
        """发送请求"""
        self.request_id += 1
        request_id = self.request_id
        
        message = MCPMessage(
            id=request_id,
            method=method,
            params=params
        )
        
        # 创建Future等待响应
        future = asyncio.Future()
        self.pending_requests[request_id] = future
        
        # 发送请求
        await self.transport.send_message(message)
        
        # 等待响应
        try:
            return await future
        finally:
            self.pending_requests.pop(request_id, None)
    
    async def _message_loop(self):
        """消息处理循环"""
        while True:
            try:
                message = await self.transport.receive_message()
                await self._handle_message(message)
            except Exception as e:
                logging.error(f"Error in client message loop: {e}")
                break
    
    async def _handle_message(self, message: MCPMessage):
        """处理消息"""
        if message.id in self.pending_requests:
            future = self.pending_requests[message.id]
            
            if message.error:
                error = MCPError(
                    message.error.get("code", MCPErrorCode.INTERNAL_ERROR),
                    message.error.get("message", "Unknown error"),
                    message.error.get("data")
                )
                future.set_exception(error)
            else:
                future.set_result(message.result)
    
    async def list_resources(self) -> List[Dict]:
        """列出资源"""
        result = await self.send_request(MCPMethod.RESOURCES_LIST.value)
        return result.get("resources", [])
    
    async def read_resource(self, uri: str) -> Any:
        """读取资源"""
        params = {"uri": uri}
        result = await self.send_request(MCPMethod.RESOURCES_READ.value, params)
        return result.get("contents", [])
    
    async def list_tools(self) -> List[Dict]:
        """列出工具"""
        result = await self.send_request(MCPMethod.TOOLS_LIST.value)
        return result.get("tools", [])
    
    async def call_tool(self, name: str, arguments: Dict = None) -> Any:
        """调用工具"""
        params = {"name": name, "arguments": arguments or {}}
        result = await self.send_request(MCPMethod.TOOLS_CALL.value, params)
        return result.get("content", [])
    
    async def list_prompts(self) -> List[Dict]:
        """列出提示"""
        result = await self.send_request(MCPMethod.PROMPTS_LIST.value)
        return result.get("prompts", [])
    
    async def get_prompt(self, name: str, arguments: Dict = None) -> Any:
        """获取提示"""
        params = {"name": name, "arguments": arguments or {}}
        result = await self.send_request(MCPMethod.PROMPTS_GET.value, params)
        return result.get("messages", [])
    
    async def disconnect(self):
        """断开连接"""
        if self.transport:
            await self.transport.close()
```

### 2. 实际应用实现

#### 文件系统MCP服务器
```python
#!/usr/bin/env python3
"""
文件系统MCP服务器实现
提供文件和目录操作功能
"""

import os
import json
import mimetypes
from pathlib import Path
from typing import Dict, Any, List

class FileSystemMCPServer(MCPServer):
    """文件系统MCP服务器"""
    
    def __init__(self, root_path: str = ".", name: str = "filesystem-server", version: str = "1.0.0"):
        super().__init__(name, version)
        self.root_path = Path(root_path).resolve()
        self._register_resources_and_tools()
    
    def _register_resources_and_tools(self):
        """注册资源和工具"""
        # 注册文件资源
        self.register_resource(
            MCPResource(
                uri="file://",
                name="File System",
                description="Access to local file system",
                mimeType="application/octet-stream"
            ),
            self._handle_file_resource
        )
        
        # 注册文件操作工具
        self.register_tool(
            MCPTool(
                name="read_file",
                description="Read the contents of a file",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "path": {
                            "type": "string",
                            "description": "Path to the file to read"
                        }
                    },
                    "required": ["path"]
                }
            ),
            self._handle_read_file_tool
        )
        
        self.register_tool(
            MCPTool(
                name="write_file",
                description="Write content to a file",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "path": {
                            "type": "string",
                            "description": "Path to the file to write"
                        },
                        "content": {
                            "type": "string",
                            "description": "Content to write to the file"
                        }
                    },
                    "required": ["path", "content"]
                }
            ),
            self._handle_write_file_tool
        )
        
        self.register_tool(
            MCPTool(
                name="list_directory",
                description="List files and directories in a directory",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "path": {
                            "type": "string",
                            "description": "Path to the directory to list"
                        }
                    },
                    "required": ["path"]
                }
            ),
            self._handle_list_directory_tool
        )
        
        self.register_tool(
            MCPTool(
                name="create_directory",
                description="Create a new directory",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "path": {
                            "type": "string",
                            "description": "Path to the directory to create"
                        }
                    },
                    "required": ["path"]
                }
            ),
            self._handle_create_directory_tool
        )
        
        self.register_tool(
            MCPTool(
                name="delete_file",
                description="Delete a file or directory",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "path": {
                            "type": "string",
                            "description": "Path to the file or directory to delete"
                        }
                    },
                    "required": ["path"]
                }
            ),
            self._handle_delete_file_tool
        )
    
    def _resolve_path(self, path: str) -> Path:
        """解析和验证路径"""
        resolved_path = (self.root_path / path).resolve()
        
        # 确保路径在根目录内
        if not str(resolved_path).startswith(str(self.root_path)):
            raise MCPError(
                MCPErrorCode.INVALID_PARAMS,
                f"Path outside of allowed root: {path}"
            )
        
        return resolved_path
    
    async def _handle_file_resource(self, uri: str, params: Dict) -> Dict[str, Any]:
        """处理文件资源请求"""
        # 从URI提取文件路径
        if uri.startswith("file://"):
            file_path = uri[7:]  # 移除 "file://" 前缀
        else:
            file_path = uri
        
        resolved_path = self._resolve_path(file_path)
        
        if not resolved_path.exists():
            raise MCPError(
                MCPErrorCode.RESOURCE_NOT_FOUND,
                f"File not found: {file_path}"
            )
        
        if resolved_path.is_file():
            # 读取文件内容
            try:
                with open(resolved_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                mime_type, _ = mimetypes.guess_type(str(resolved_path))
                
                return {
                    "uri": uri,
                    "mimeType": mime_type or "text/plain",
                    "text": content
                }
            except UnicodeDecodeError:
                # 二进制文件
                return {
                    "uri": uri,
                    "mimeType": "application/octet-stream",
                    "blob": str(resolved_path)  # 返回路径，实际应用中可能需要base64编码
                }
        else:
            # 目录
            return {
                "uri": uri,
                "mimeType": "application/json",
                "text": json.dumps({
                    "type": "directory",
                    "path": str(resolved_path),
                    "items": [item.name for item in resolved_path.iterdir()]
                })
            }
    
    async def _handle_read_file_tool(self, name: str, arguments: Dict) -> str:
        """处理读取文件工具"""
        path = arguments.get("path")
        if not path:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing path parameter")
        
        resolved_path = self._resolve_path(path)
        
        if not resolved_path.exists():
            raise MCPError(MCPErrorCode.INVALID_PARAMS, f"File not found: {path}")
        
        if not resolved_path.is_file():
            raise MCPError(MCPErrorCode.INVALID_PARAMS, f"Path is not a file: {path}")
        
        try:
            with open(resolved_path, 'r', encoding='utf-8') as f:
                content = f.read()
            return f"File content of {path}:\n{content}"
        except UnicodeDecodeError:
            return f"Binary file: {path} (cannot display content)"
        except Exception as e:
            raise MCPError(MCPErrorCode.INTERNAL_ERROR, f"Error reading file: {str(e)}")
    
    async def _handle_write_file_tool(self, name: str, arguments: Dict) -> str:
        """处理写入文件工具"""
        path = arguments.get("path")
        content = arguments.get("content")
        
        if not path:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing path parameter")
        if content is None:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing content parameter")
        
        resolved_path = self._resolve_path(path)
        
        try:
            # 确保父目录存在
            resolved_path.parent.mkdir(parents=True, exist_ok=True)
            
            with open(resolved_path, 'w', encoding='utf-8') as f:
                f.write(content)
            
            return f"Successfully wrote {len(content)} characters to {path}"
        except Exception as e:
            raise MCPError(MCPErrorCode.INTERNAL_ERROR, f"Error writing file: {str(e)}")
    
    async def _handle_list_directory_tool(self, name: str, arguments: Dict) -> str:
        """处理列出目录工具"""
        path = arguments.get("path", ".")
        resolved_path = self._resolve_path(path)
        
        if not resolved_path.exists():
            raise MCPError(MCPErrorCode.INVALID_PARAMS, f"Directory not found: {path}")
        
        if not resolved_path.is_dir():
            raise MCPError(MCPErrorCode.INVALID_PARAMS, f"Path is not a directory: {path}")
        
        try:
            items = []
            for item in resolved_path.iterdir():
                item_type = "directory" if item.is_dir() else "file"
                size = item.stat().st_size if item.is_file() else "-"
                items.append(f"{item_type:<10} {size:<10} {item.name}")
            
            result = f"Contents of {path}:\n"
            result += f"{'Type':<10} {'Size':<10} {'Name'}\n"
            result += "-" * 40 + "\n"
            result += "\n".join(items)
            
            return result
        except Exception as e:
            raise MCPError(MCPErrorCode.INTERNAL_ERROR, f"Error listing directory: {str(e)}")
    
    async def _handle_create_directory_tool(self, name: str, arguments: Dict) -> str:
        """处理创建目录工具"""
        path = arguments.get("path")
        if not path:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing path parameter")
        
        resolved_path = self._resolve_path(path)
        
        try:
            resolved_path.mkdir(parents=True, exist_ok=True)
            return f"Successfully created directory: {path}"
        except Exception as e:
            raise MCPError(MCPErrorCode.INTERNAL_ERROR, f"Error creating directory: {str(e)}")
    
    async def _handle_delete_file_tool(self, name: str, arguments: Dict) -> str:
        """处理删除文件工具"""
        path = arguments.get("path")
        if not path:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing path parameter")
        
        resolved_path = self._resolve_path(path)
        
        if not resolved_path.exists():
            raise MCPError(MCPErrorCode.INVALID_PARAMS, f"Path not found: {path}")
        
        try:
            if resolved_path.is_file():
                resolved_path.unlink()
                return f"Successfully deleted file: {path}"
            elif resolved_path.is_dir():
                import shutil
                shutil.rmtree(resolved_path)
                return f"Successfully deleted directory: {path}"
            else:
                raise MCPError(MCPErrorCode.INVALID_PARAMS, f"Unknown file type: {path}")
        except Exception as e:
            raise MCPError(MCPErrorCode.INTERNAL_ERROR, f"Error deleting: {str(e)}")

# 数据库MCP服务器
class DatabaseMCPServer(MCPServer):
    """数据库MCP服务器"""
    
    def __init__(self, database_url: str, name: str = "database-server", version: str = "1.0.0"):
        super().__init__(name, version)
        self.database_url = database_url
        self.connection = None
        self._register_tools()
    
    def _register_tools(self):
        """注册数据库工具"""
        self.register_tool(
            MCPTool(
                name="execute_query",
                description="Execute a SQL query",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "query": {
                            "type": "string",
                            "description": "SQL query to execute"
                        },
                        "parameters": {
                            "type": "array",
                            "description": "Query parameters",
                            "items": {"type": "string"}
                        }
                    },
                    "required": ["query"]
                }
            ),
            self._handle_execute_query_tool
        )
        
        self.register_tool(
            MCPTool(
                name="list_tables",
                description="List all tables in the database",
                inputSchema={
                    "type": "object",
                    "properties": {}
                }
            ),
            self._handle_list_tables_tool
        )
        
        self.register_tool(
            MCPTool(
                name="describe_table",
                description="Describe the structure of a table",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "table_name": {
                            "type": "string",
                            "description": "Name of the table to describe"
                        }
                    },
                    "required": ["table_name"]
                }
            ),
            self._handle_describe_table_tool
        )
    
    async def _get_connection(self):
        """获取数据库连接"""
        if not self.connection:
            # 这里应该根据database_url类型创建适当的连接
            # 为简化，使用SQLite作为示例
            import sqlite3
            self.connection = sqlite3.connect(self.database_url)
            self.connection.row_factory = sqlite3.Row
        return self.connection
    
    async def _handle_execute_query_tool(self, name: str, arguments: Dict) -> str:
        """处理SQL查询工具"""
        query = arguments.get("query")
        parameters = arguments.get("parameters", [])
        
        if not query:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing query parameter")
        
        try:
            conn = await self._get_connection()
            cursor = conn.cursor()
            
            if parameters:
                cursor.execute(query, parameters)
            else:
                cursor.execute(query)
            
            if query.strip().lower().startswith('select'):
                # 查询操作
                rows = cursor.fetchall()
                if rows:
                    columns = [description[0] for description in cursor.description]
                    result = f"Query executed successfully. {len(rows)} rows returned.\n\n"
                    result += " | ".join(columns) + "\n"
                    result += "-" * (len(" | ".join(columns))) + "\n"
                    
                    for row in rows:
                        result += " | ".join(str(row[col]) for col in columns) + "\n"
                    
                    return result
                else:
                    return "Query executed successfully. No rows returned."
            else:
                # 修改操作
                conn.commit()
                return f"Query executed successfully. {cursor.rowcount} rows affected."
                
        except Exception as e:
            raise MCPError(MCPErrorCode.TOOL_ERROR, f"Database error: {str(e)}")
    
    async def _handle_list_tables_tool(self, name: str, arguments: Dict) -> str:
        """处理列出表格工具"""
        try:
            conn = await self._get_connection()
            cursor = conn.cursor()
            
            # SQLite查询表格列表
            cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
            tables = cursor.fetchall()
            
            if tables:
                result = "Tables in database:\n"
                for table in tables:
                    result += f"  - {table[0]}\n"
                return result
            else:
                return "No tables found in database."
                
        except Exception as e:
            raise MCPError(MCPErrorCode.TOOL_ERROR, f"Database error: {str(e)}")
    
    async def _handle_describe_table_tool(self, name: str, arguments: Dict) -> str:
        """处理描述表格工具"""
        table_name = arguments.get("table_name")
        if not table_name:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing table_name parameter")
        
        try:
            conn = await self._get_connection()
            cursor = conn.cursor()
            
            # SQLite查询表格结构
            cursor.execute(f"PRAGMA table_info({table_name})")
            columns = cursor.fetchall()
            
            if columns:
                result = f"Structure of table '{table_name}':\n\n"
                result += f"{'Column':<20} {'Type':<15} {'Not Null':<10} {'Default':<15} {'Primary Key'}\n"
                result += "-" * 80 + "\n"
                
                for col in columns:
                    result += f"{col[1]:<20} {col[2]:<15} {col[3]:<10} {str(col[4]):<15} {col[5]}\n"
                
                return result
            else:
                return f"Table '{table_name}' not found."
                
        except Exception as e:
            raise MCPError(MCPErrorCode.TOOL_ERROR, f"Database error: {str(e)}")

# Web API MCP服务器
class WebAPIMCPServer(MCPServer):
    """Web API MCP服务器"""
    
    def __init__(self, name: str = "web-api-server", version: str = "1.0.0"):
        super().__init__(name, version)
        self._register_tools()
    
    def _register_tools(self):
        """注册Web API工具"""
        self.register_tool(
            MCPTool(
                name="http_request",
                description="Make an HTTP request",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "url": {
                            "type": "string",
                            "description": "URL to request"
                        },
                        "method": {
                            "type": "string",
                            "description": "HTTP method",
                            "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"]
                        },
                        "headers": {
                            "type": "object",
                            "description": "Request headers"
                        },
                        "data": {
                            "type": "string",
                            "description": "Request body data"
                        }
                    },
                    "required": ["url"]
                }
            ),
            self._handle_http_request_tool
        )
        
        self.register_tool(
            MCPTool(
                name="fetch_json",
                description="Fetch JSON data from a URL",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "url": {
                            "type": "string",
                            "description": "URL to fetch JSON from"
                        }
                    },
                    "required": ["url"]
                }
            ),
            self._handle_fetch_json_tool
        )
    
    async def _handle_http_request_tool(self, name: str, arguments: Dict) -> str:
        """处理HTTP请求工具"""
        import aiohttp
        
        url = arguments.get("url")
        method = arguments.get("method", "GET").upper()
        headers = arguments.get("headers", {})
        data = arguments.get("data")
        
        if not url:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing url parameter")
        
        try:
            async with aiohttp.ClientSession() as session:
                async with session.request(
                    method=method,
                    url=url,
                    headers=headers,
                    data=data
                ) as response:
                    content = await response.text()
                    
                    result = f"HTTP {method} {url}\n"
                    result += f"Status: {response.status}\n"
                    result += f"Headers: {dict(response.headers)}\n"
                    result += f"Content:\n{content}"
                    
                    return result
                    
        except Exception as e:
            raise MCPError(MCPErrorCode.TOOL_ERROR, f"HTTP request error: {str(e)}")
    
    async def _handle_fetch_json_tool(self, name: str, arguments: Dict) -> str:
        """处理获取JSON工具"""
        import aiohttp
        
        url = arguments.get("url")
        if not url:
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Missing url parameter")
        
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(url) as response:
                    if response.content_type == 'application/json':
                        data = await response.json()
                        return f"JSON data from {url}:\n{json.dumps(data, indent=2)}"
                    else:
                        content = await response.text()
                        return f"Non-JSON response from {url}:\n{content}"
                        
        except Exception as e:
            raise MCPError(MCPErrorCode.TOOL_ERROR, f"JSON fetch error: {str(e)}")

# Agent与MCP集成示例
class AgentMCPIntegration:
    """Agent与MCP服务器集成"""
    
    def __init__(self, agent: SpecializedAgent):
        self.agent = agent
        self.mcp_servers: Dict[str, MCPClient] = {}
    
    async def connect_to_mcp_server(self, server_name: str, transport: MCPTransport):
        """连接到MCP服务器"""
        client = MCPClient(f"agent-{self.agent.config.name}", "1.0.0")
        client.set_transport(transport)
        
        await client.connect()
        
        self.mcp_servers[server_name] = client
        self.agent.logger.info(f"Connected to MCP server: {server_name}")
        
        # 注册MCP工具到Agent
        await self._register_mcp_tools(server_name, client)
    
    async def _register_mcp_tools(self, server_name: str, client: MCPClient):
        """注册MCP工具到Agent"""
        try:
            # 获取MCP服务器的工具列表
            tools = await client.list_tools()
            
            for tool in tools:
                tool_name = f"{server_name}_{tool['name']}"
                
                # 创建工具包装器
                async def mcp_tool_wrapper(*args, **kwargs):
                    return await client.call_tool(tool['name'], kwargs)
                
                # 注册到Agent工具注册表
                self.agent.tool_registry.register_tool(
                    tool_name,
                    mcp_tool_wrapper,
                    f"MCP tool from {server_name}: {tool['description']}"
                )
        
        except Exception as e:
            self.agent.logger.error(f"Failed to register MCP tools from {server_name}: {e}")
    
    async def call_mcp_tool(self, server_name: str, tool_name: str, arguments: Dict = None) -> Any:
        """调用MCP工具"""
        if server_name not in self.mcp_servers:
            raise ValueError(f"MCP server not connected: {server_name}")
        
        client = self.mcp_servers[server_name]
        return await client.call_tool(tool_name, arguments or {})
    
    async def read_mcp_resource(self, server_name: str, uri: str) -> Any:
        """读取MCP资源"""
        if server_name not in self.mcp_servers:
            raise ValueError(f"MCP server not connected: {server_name}")
        
        client = self.mcp_servers[server_name]
        return await client.read_resource(uri)
```

## 核心组件开发

### 1. Agent-MCP整合示例

#### 完整使用示例
```python
#!/usr/bin/env python3
"""
Agent与MCP服务器完整集成示例
"""

import asyncio
import logging
import subprocess
from pathlib import Path

async def main():
    """主函数演示Agent和MCP的集成使用"""
    # 设置日志
    logging.basicConfig(level=logging.INFO)
    
    # 创建Agent配置
    agent_config = AgentConfig(
        name="smart-assistant",
        description="智能助手Agent",
        capabilities=[
            AgentCapability.FILE_OPERATIONS,
            AgentCapability.WEB_SEARCH,
            AgentCapability.CODE_EXECUTION
        ],
        max_concurrent_tasks=3
    )
    
    # 创建Agent
    agent = SpecializedAgent(agent_config)
    
    # 启动Agent
    await agent.start()
    
    # 创建MCP集成
    mcp_integration = AgentMCPIntegration(agent)
    
    # 启动文件系统MCP服务器
    filesystem_server = FileSystemMCPServer(root_path="./workspace")
    filesystem_transport = StdioTransport()
    filesystem_server.set_transport(filesystem_transport)
    
    # 在后台启动MCP服务器
    asyncio.create_task(filesystem_server.start())
    
    # 连接到MCP服务器
    await mcp_integration.connect_to_mcp_server("filesystem", filesystem_transport)
    
    # 示例任务1: 文件操作
    print("=== 示例1: 文件操作 ===")
    file_task = Task(
        description="创建并写入文件",
        type="tool_call",
        parameters={
            "tool_name": "filesystem_write_file",
            "args": {
                "path": "example.txt",
                "content": "Hello from Agent-MCP integration!"
            }
        }
    )
    
    task_id = await agent.add_task(file_task)
    
    # 等待任务完成
    while await agent.get_task_status(task_id) != TaskStatus.COMPLETED:
        await asyncio.sleep(0.1)
    
    result = await agent.get_task_result(task_id)
    print(f"文件写入结果: {result}")
    
    # 示例任务2: 读取文件
    print("=== 示例2: 读取文件 ===")
    read_task = Task(
        description="读取文件内容",
        type="tool_call",
        parameters={
            "tool_name": "filesystem_read_file",
            "args": {"path": "example.txt"}
        }
    )
    
    task_id = await agent.add_task(read_task)
    
    while await agent.get_task_status(task_id) != TaskStatus.COMPLETED:
        await asyncio.sleep(0.1)
    
    result = await agent.get_task_result(task_id)
    print(f"文件读取结果: {result}")
    
    # 示例任务3: 复杂任务组合
    print("=== 示例3: 复杂任务组合 ===")
    complex_task = Task(
        description="分析项目文件",
        type="complex",
        parameters={
            "steps": [
                {
                    "description": "列出目录内容",
                    "type": "tool_call",
                    "parameters": {
                        "tool_name": "filesystem_list_directory",
                        "args": {"path": "."}
                    }
                },
                {
                    "description": "读取README文件",
                    "type": "tool_call",
                    "parameters": {
                        "tool_name": "filesystem_read_file",
                        "args": {"path": "README.md"}
                    }
                },
                {
                    "description": "分析文件结构",
                    "type": "analysis",
                    "parameters": {
                        "analysis_type": "summary"
                    }
                }
            ]
        },
        context={"strategy": "sequential"}
    )
    
    task_id = await agent.add_task(complex_task)
    
    while await agent.get_task_status(task_id) != TaskStatus.COMPLETED:
        await asyncio.sleep(0.1)
    
    result = await agent.get_task_result(task_id)
    print(f"复杂任务结果: {result}")
    
    # 停止Agent
    await agent.stop()
    await filesystem_server.stop()

if __name__ == "__main__":
    asyncio.run(main())
```

## 实际案例实现

### 1. 智能代码助手

#### 代码分析MCP服务器
```python
#!/usr/bin/env python3
"""
代码分析MCP服务器
提供代码分析、重构建议等功能
"""

import ast
import subprocess
import tempfile
from pathlib import Path
from typing import Dict, Any, List

class CodeAnalysisMCPServer(MCPServer):
    """代码分析MCP服务器"""
    
    def __init__(self, name: str = "code-analysis-server", version: str = "1.0.0"):
        super().__init__(name, version)
        self._register_tools()
    
    def _register_tools(self):
        """注册代码分析工具"""
        # 代码质量分析
        self.register_tool(
            MCPTool(
                name="analyze_python_code",
                description="Analyze Python code quality and structure",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "code": {"type": "string", "description": "Python code to analyze"},
                        "file_path": {"type": "string", "description": "Optional file path"}
                    },
                    "required": ["code"]
                }
            ),
            self._analyze_python_code
        )
        
        # 代码格式化
        self.register_tool(
            MCPTool(
                name="format_python_code",
                description="Format Python code using black",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "code": {"type": "string", "description": "Python code to format"}
                    },
                    "required": ["code"]
                }
            ),
            self._format_python_code
        )
        
        # 代码复杂度分析
        self.register_tool(
            MCPTool(
                name="complexity_analysis",
                description="Analyze code complexity metrics",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "code": {"type": "string", "description": "Python code to analyze"}
                    },
                    "required": ["code"]
                }
            ),
            self._complexity_analysis
        )
        
        # 生成文档
        self.register_tool(
            MCPTool(
                name="generate_docstring",
                description="Generate docstring for Python function",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "function_code": {"type": "string", "description": "Function code"}
                    },
                    "required": ["function_code"]
                }
            ),
            self._generate_docstring
        )
    
    async def _analyze_python_code(self, name: str, arguments: Dict) -> str:
        """分析Python代码"""
        code = arguments.get("code", "")
        file_path = arguments.get("file_path", "<string>")
        
        if not code.strip():
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Empty code provided")
        
        analysis_results = []
        
        try:
            # 解析AST
            tree = ast.parse(code, filename=file_path)
            
            # 分析代码结构
            analyzer = CodeStructureAnalyzer()
            analyzer.visit(tree)
            
            analysis_results.append("=== 代码结构分析 ===")
            analysis_results.append(f"函数数量: {analyzer.function_count}")
            analysis_results.append(f"类数量: {analyzer.class_count}")
            analysis_results.append(f"导入数量: {analyzer.import_count}")
            analysis_results.append(f"代码行数: {len(code.split('\n'))}")
            
            # 检查潜在问题
            issues = self._check_code_issues(tree)
            if issues:
                analysis_results.append("\n=== 潜在问题 ===")
                for issue in issues:
                    analysis_results.append(f"- {issue}")
            
            # 代码质量建议
            suggestions = self._get_quality_suggestions(analyzer)
            if suggestions:
                analysis_results.append("\n=== 改进建议 ===")
                for suggestion in suggestions:
                    analysis_results.append(f"- {suggestion}")
            
            return "\n".join(analysis_results)
            
        except SyntaxError as e:
            return f"语法错误: {e.msg} (行 {e.lineno})"
        except Exception as e:
            raise MCPError(MCPErrorCode.TOOL_ERROR, f"代码分析失败: {str(e)}")
    
    async def _format_python_code(self, name: str, arguments: Dict) -> str:
        """格式化Python代码"""
        code = arguments.get("code", "")
        
        if not code.strip():
            raise MCPError(MCPErrorCode.INVALID_PARAMS, "Empty code provided")
        
        try:
            # 使用black格式化代码
            with tempfile.NamedTemporaryFile(mode='w', suffix='.py', delete=False) as f:
                f.write(code)
                temp_file = f.name
            
            try:
                result = subprocess.run([
                    'black', '--code', code
                ], capture_output=True, text=True, timeout=30)
                
                if result.returncode == 0:
                    return f"格式化成功:\n\n{result.stdout}"
                else:
                    return f"格式化失败: {result.stderr}"
                    
            finally:
                Path(temp_file).unlink(missing_ok=True)
                
        except subprocess.TimeoutExpired:
            return "格式化超时"
        except FileNotFoundError:
            return "black工具未安装，请先安装: pip install black"
        except Exception as e:
            raise MCPError(MCPErrorCode.TOOL_ERROR, f"格式化失败: {str(e)}")
    
    async def _complexity_analysis(self, name: str, arguments: Dict) -> str:
        """复杂度分析"""
        code = arguments.get("code", "")
        
        try:
            tree = ast.parse(code)
            complexity_analyzer = ComplexityAnalyzer()
            complexity_analyzer.visit(tree)
            
            results = []
            results.append("=== 复杂度分析 ===")
            results.append(f"圈复杂度: {complexity_analyzer.cyclomatic_complexity}")
            results.append(f"嵌套深度: {complexity_analyzer.max_depth}")
            results.append(f"函数长度: {complexity_analyzer.max_function_length}")
            
            # 复杂度建议
            if complexity_analyzer.cyclomatic_complexity > 10:
                results.append("\n⚠️ 圈复杂度过高，建议重构")
            
            if complexity_analyzer.max_depth > 4:
                results.append("⚠️ 嵌套过深，建议简化逻辑")
            
            if complexity_analyzer.max_function_length > 50:
                results.append("⚠️ 函数过长，建议拆分")
            
            return "\n".join(results)
            
        except Exception as e:
            raise MCPError(MCPErrorCode.TOOL_ERROR, f"复杂度分析失败: {str(e)}")
    
    async def _generate_docstring(self, name: str, arguments: Dict) -> str:
        """生成文档字符串"""
        function_code = arguments.get("function_code", "")
        
        try:
            tree = ast.parse(function_code)
            
            # 查找函数定义
            for node in ast.walk(tree):
                if isinstance(node, ast.FunctionDef):
                    docstring = self._create_docstring(node)
                    return f"建议的文档字符串:\n\n{docstring}"
            
            return "未找到函数定义"
            
        except Exception as e:
            raise MCPError(MCPErrorCode.TOOL_ERROR, f"文档生成失败: {str(e)}")
    
    def _check_code_issues(self, tree: ast.AST) -> List[str]:
        """检查代码问题"""
        issues = []
        
        for node in ast.walk(tree):
            # 检查空的except块
            if isinstance(node, ast.ExceptHandler) and not node.body:
                issues.append("发现空的except块")
            
            # 检查裸露的except
            if isinstance(node, ast.ExceptHandler) and node.type is None:
                issues.append("发现裸露的except语句，建议指定异常类型")
            
            # 检查长参数列表
            if isinstance(node, ast.FunctionDef) and len(node.args.args) > 5:
                issues.append(f"函数 {node.name} 参数过多 ({len(node.args.args)}个)")
        
        return issues
    
    def _get_quality_suggestions(self, analyzer) -> List[str]:
        """获取质量建议"""
        suggestions = []
        
        if analyzer.function_count > 20:
            suggestions.append("考虑将代码分解为多个模块")
        
        if analyzer.class_count == 0 and analyzer.function_count > 5:
            suggestions.append("考虑使用面向对象的设计")
        
        return suggestions
    
    def _create_docstring(self, func_node: ast.FunctionDef) -> str:
        """创建文档字符串"""
        lines = [f'"""', f'{func_node.name}函数', '']
        
        # 参数说明
        if func_node.args.args:
            lines.append('Args:')
            for arg in func_node.args.args:
                if arg.arg != 'self':
                    lines.append(f'    {arg.arg}: 描述{arg.arg}参数')
        
        # 返回值说明
        lines.append('')
        lines.append('Returns:')
        lines.append('    描述返回值')
        
        lines.append('"""')
        
        return '\n'.join(lines)

class CodeStructureAnalyzer(ast.NodeVisitor):
    """代码结构分析器"""
    
    def __init__(self):
        self.function_count = 0
        self.class_count = 0
        self.import_count = 0
    
    def visit_FunctionDef(self, node):
        self.function_count += 1
        self.generic_visit(node)
    
    def visit_ClassDef(self, node):
        self.class_count += 1
        self.generic_visit(node)
    
    def visit_Import(self, node):
        self.import_count += 1
        self.generic_visit(node)
    
    def visit_ImportFrom(self, node):
        self.import_count += 1
        self.generic_visit(node)

class ComplexityAnalyzer(ast.NodeVisitor):
    """复杂度分析器"""
    
    def __init__(self):
        self.cyclomatic_complexity = 1  # 基础复杂度
        self.current_depth = 0
        self.max_depth = 0
        self.current_function_length = 0
        self.max_function_length = 0
    
    def visit_If(self, node):
        self.cyclomatic_complexity += 1
        self.current_depth += 1
        self.max_depth = max(self.max_depth, self.current_depth)
        self.generic_visit(node)
        self.current_depth -= 1
    
    def visit_While(self, node):
        self.cyclomatic_complexity += 1
        self.current_depth += 1
        self.max_depth = max(self.max_depth, self.current_depth)
        self.generic_visit(node)
        self.current_depth -= 1
    
    def visit_For(self, node):
        self.cyclomatic_complexity += 1
        self.current_depth += 1
        self.max_depth = max(self.max_depth, self.current_depth)
        self.generic_visit(node)
        self.current_depth -= 1
    
    def visit_FunctionDef(self, node):
        # 计算函数长度
        function_lines = node.end_lineno - node.lineno + 1 if hasattr(node, 'end_lineno') else 0
        self.max_function_length = max(self.max_function_length, function_lines)
        self.generic_visit(node)
```

### 2. 智能文档助手

#### 文档处理Agent
```python
#!/usr/bin/env python3
"""
智能文档助手Agent
集成多个MCP服务器提供文档处理功能
"""

class DocumentAssistantAgent(SpecializedAgent):
    """文档助手Agent"""
    
    def __init__(self):
        config = AgentConfig(
            name="document-assistant",
            description="智能文档助手",
            capabilities=[
                AgentCapability.FILE_OPERATIONS,
                AgentCapability.CONTENT_GENERATION,
                AgentCapability.DATA_ANALYSIS
            ]
        )
        super().__init__(config)
        self.mcp_integration = AgentMCPIntegration(self)
        self._register_document_tools()
    
    def _register_document_tools(self):
        """注册文档处理工具"""
        self.tool_registry.register_tool("extract_text", self._extract_text_tool, "Extract text from documents")
        self.tool_registry.register_tool("summarize_document", self._summarize_document_tool, "Summarize document content")
        self.tool_registry.register_tool("generate_outline", self._generate_outline_tool, "Generate document outline")
        self.tool_registry.register_tool("translate_text", self._translate_text_tool, "Translate text to another language")
    
    async def setup_mcp_servers(self):
        """设置MCP服务器连接"""
        # 连接文件系统服务器
        filesystem_server = FileSystemMCPServer()
        filesystem_transport = StdioTransport()
        filesystem_server.set_transport(filesystem_transport)
        asyncio.create_task(filesystem_server.start())
        await self.mcp_integration.connect_to_mcp_server("filesystem", filesystem_transport)
        
        # 连接代码分析服务器
        code_server = CodeAnalysisMCPServer()
        code_transport = StdioTransport()
        code_server.set_transport(code_transport)
        asyncio.create_task(code_server.start())
        await self.mcp_integration.connect_to_mcp_server("code_analysis", code_transport)
    
    async def _extract_text_tool(self, file_path: str, format_type: str = "auto") -> str:
        """提取文档文本"""
        try:
            # 使用MCP读取文件
            content = await self.mcp_integration.call_mcp_tool(
                "filesystem", "read_file", {"path": file_path}
            )
            
            # 根据文件类型处理
            if file_path.endswith('.md'):
                return self._process_markdown(content)
            elif file_path.endswith('.py'):
                return self._process_python_code(content)
            else:
                return content
                
        except Exception as e:
            return f"文本提取失败: {str(e)}"
    
    async def _summarize_document_tool(self, file_path: str, max_length: int = 200) -> str:
        """总结文档"""
        try:
            # 提取文本
            text = await self._extract_text_tool(file_path)
            
            # 简单的总结算法
            sentences = text.split('.')
            if len(sentences) <= 3:
                return text
            
            # 选择前几个句子作为总结
            summary_sentences = sentences[:3]
            summary = '. '.join(summary_sentences).strip()
            
            if len(summary) > max_length:
                summary = summary[:max_length] + "..."
            
            return f"文档总结:\n{summary}"
            
        except Exception as e:
            return f"文档总结失败: {str(e)}"
    
    async def _generate_outline_tool(self, file_path: str) -> str:
        """生成文档大纲"""
        try:
            text = await self._extract_text_tool(file_path)
            
            if file_path.endswith('.md'):
                return self._extract_markdown_outline(text)
            elif file_path.endswith('.py'):
                return await self._extract_python_outline(file_path)
            else:
                return self._generate_generic_outline(text)
                
        except Exception as e:
            return f"大纲生成失败: {str(e)}"
    
    async def _translate_text_tool(self, text: str, target_language: str = "zh") -> str:
        """翻译文本（简化版）"""
        # 这里应该集成真实的翻译API
        translation_map = {
            "hello": "你好",
            "world": "世界",
            "code": "代码",
            "function": "函数",
            "class": "类"
        }
        
        words = text.lower().split()
        translated_words = [translation_map.get(word, word) for word in words]
        
        return f"翻译结果 ({target_language}): {' '.join(translated_words)}"
    
    def _process_markdown(self, content: str) -> str:
        """处理Markdown内容"""
        # 移除Markdown标记，提取纯文本
        import re
        
        # 移除标题标记
        content = re.sub(r'^#+\s', '', content, flags=re.MULTILINE)
        # 移除链接
        content = re.sub(r'\[([^\]]+)\]\([^\)]+\)', r'\1', content)
        # 移除粗体和斜体
        content = re.sub(r'\*\*([^\*]+)\*\*', r'\1', content)
        content = re.sub(r'\*([^\*]+)\*', r'\1', content)
        
        return content
    
    def _process_python_code(self, content: str) -> str:
        """处理Python代码"""
        # 提取文档字符串和注释
        import ast
        
        try:
            tree = ast.parse(content)
            docs = []
            
            for node in ast.walk(tree):
                if isinstance(node, (ast.FunctionDef, ast.ClassDef)):
                    if ast.get_docstring(node):
                        docs.append(f"{node.name}: {ast.get_docstring(node)}")
            
            return "\n".join(docs) if docs else "未找到文档字符串"
            
        except:
            return content
    
    def _extract_markdown_outline(self, text: str) -> str:
        """提取Markdown大纲"""
        import re
        
        headers = re.findall(r'^(#+)\s+(.+)$', text, re.MULTILINE)
        outline = []
        
        for level_marks, title in headers:
            level = len(level_marks)
            indent = "  " * (level - 1)
            outline.append(f"{indent}- {title}")
        
        return "文档大纲:\n" + "\n".join(outline)
    
    async def _extract_python_outline(self, file_path: str) -> str:
        """提取Python代码大纲"""
        try:
            # 使用代码分析MCP服务器
            content = await self.mcp_integration.call_mcp_tool(
                "filesystem", "read_file", {"path": file_path}
            )
            
            analysis = await self.mcp_integration.call_mcp_tool(
                "code_analysis", "analyze_python_code", {"code": content}
            )
            
            return f"Python代码大纲:\n{analysis}"
            
        except Exception as e:
            return f"Python大纲提取失败: {str(e)}"
    
    def _generate_generic_outline(self, text: str) -> str:
        """生成通用大纲"""
        paragraphs = text.split('\n\n')
        outline = []
        
        for i, para in enumerate(paragraphs[:5], 1):
            if para.strip():
                first_sentence = para.split('.')[0].strip()
                if len(first_sentence) > 50:
                    first_sentence = first_sentence[:50] + "..."
                outline.append(f"{i}. {first_sentence}")
        
        return "文档大纲:\n" + "\n".join(outline)

    async def _execute_single_task(self, task: Task) -> Any:
        """执行单个任务"""
        task_type = task.type
        parameters = task.parameters
        
        if task_type == "document_analysis":
            file_path = parameters.get("file_path")
            analysis_type = parameters.get("analysis_type", "summary")
            
            if analysis_type == "summary":
                return await self._summarize_document_tool(file_path)
            elif analysis_type == "outline":
                return await self._generate_outline_tool(file_path)
            elif analysis_type == "extract":
                return await self._extract_text_tool(file_path)
        
        elif task_type == "document_translation":
            text = parameters.get("text")
            target_lang = parameters.get("target_language", "zh")
            return await self._translate_text_tool(text, target_lang)
        
        else:
            # 委托给父类处理
            return await super()._execute_single_task(task)

# 使用示例
async def document_assistant_demo():
    """文档助手演示"""
    # 创建文档助手
    assistant = DocumentAssistantAgent()
    
    # 启动助手
    await assistant.start()
    
    # 设置MCP服务器
    await assistant.setup_mcp_servers()
    
    # 示例任务：分析文档
    analysis_task = Task(
        description="分析README文档",
        type="document_analysis",
        parameters={
            "file_path": "README.md",
            "analysis_type": "summary"
        }
    )
    
    task_id = await assistant.add_task(analysis_task)
    
    # 等待完成
    while await assistant.get_task_status(task_id) != TaskStatus.COMPLETED:
        await asyncio.sleep(0.1)
    
    result = await assistant.get_task_result(task_id)
    print(f"分析结果: {result}")
    
    await assistant.stop()
```

## 部署和集成

### 1. Docker部署配置

#### Dockerfile
```dockerfile
# Dockerfile for Agent-MCP System
FROM python:3.11-slim

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    build-essential \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY src/ ./src/
COPY config/ ./config/
COPY scripts/ ./scripts/

# 创建工作目录
RUN mkdir -p workspace logs

# 设置环境变量
ENV PYTHONPATH=/app/src
ENV LOG_LEVEL=INFO

# 暴露端口
EXPOSE 8000 8001 8002

# 启动脚本
COPY docker-entrypoint.sh .
RUN chmod +x docker-entrypoint.sh

ENTRYPOINT ["./docker-entrypoint.sh"]
```

#### Docker Compose配置
```yaml
# docker-compose.yml
version: '3.8'

services:
  # Agent服务
  agent-service:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: agent-mcp-system
    environment:
      - SERVICE_TYPE=agent
      - AGENT_NAME=smart-assistant
      - LOG_LEVEL=${LOG_LEVEL:-INFO}
    volumes:
      - ./workspace:/app/workspace
      - ./logs:/app/logs
      - ./config:/app/config
    ports:
      - "8000:8000"
    networks:
      - agent-net
    restart: unless-stopped

  # 文件系统MCP服务器
  filesystem-mcp:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: filesystem-mcp
    environment:
      - SERVICE_TYPE=mcp_server
      - MCP_SERVER_TYPE=filesystem
      - ROOT_PATH=/app/workspace
    volumes:
      - ./workspace:/app/workspace
      - ./logs:/app/logs
    ports:
      - "8001:8001"
    networks:
      - agent-net
    restart: unless-stopped

  # 代码分析MCP服务器
  code-analysis-mcp:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: code-analysis-mcp
    environment:
      - SERVICE_TYPE=mcp_server
      - MCP_SERVER_TYPE=code_analysis
    volumes:
      - ./workspace:/app/workspace
      - ./logs:/app/logs
    ports:
      - "8002:8002"
    networks:
      - agent-net
    restart: unless-stopped

  # 数据库服务
  redis:
    image: redis:7-alpine
    container_name: agent-redis
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    networks:
      - agent-net
    restart: unless-stopped

  # Web界面
  web-ui:
    build:
      context: ./web-ui
      dockerfile: Dockerfile
    container_name: agent-web-ui
    environment:
      - AGENT_API_URL=http://agent-service:8000
    ports:
      - "3000:3000"
    depends_on:
      - agent-service
    networks:
      - agent-net
    restart: unless-stopped

volumes:
  redis_data:

networks:
  agent-net:
    driver: bridge
```

#### 启动脚本
```bash
#!/bin/bash
# docker-entrypoint.sh

set -e

SERVICE_TYPE=${SERVICE_TYPE:-agent}
LOG_LEVEL=${LOG_LEVEL:-INFO}

echo "Starting service type: $SERVICE_TYPE"

case "$SERVICE_TYPE" in
    "agent")
        echo "Starting Agent service..."
        python src/agent_main.py
        ;;
    "mcp_server")
        MCP_SERVER_TYPE=${MCP_SERVER_TYPE:-filesystem}
        echo "Starting MCP Server: $MCP_SERVER_TYPE"
        python src/mcp_server_main.py --type $MCP_SERVER_TYPE
        ;;
    "web_ui")
        echo "Starting Web UI..."
        python src/web_server.py
        ;;
    *)
        echo "Unknown service type: $SERVICE_TYPE"
        exit 1
        ;;
esac
```

### 2. 部署脚本

#### 自动化部署脚本
```bash
#!/bin/bash
# deploy.sh

set -e

echo "🚀 部署Agent-MCP系统..."

# 检查依赖
check_dependencies() {
    echo "检查依赖..."
    
    command -v docker >/dev/null 2>&1 || { echo "❌ Docker未安装"; exit 1; }
    command -v docker-compose >/dev/null 2>&1 || { echo "❌ Docker Compose未安装"; exit 1; }
    
    echo "✅ 依赖检查通过"
}

# 创建目录结构
setup_directories() {
    echo "创建目录结构..."
    
    mkdir -p {workspace,logs,config,data}
    mkdir -p config/{agent,mcp}
    
    echo "✅ 目录创建完成"
}

# 生成配置文件
generate_configs() {
    echo "生成配置文件..."
    
    # Agent配置
    cat > config/agent/config.json << 'EOF'
{
    "name": "smart-assistant",
    "description": "智能助手Agent",
    "capabilities": [
        "file_operations",
        "web_search", 
        "code_execution",
        "content_generation"
    ],
    "max_concurrent_tasks": 5,
    "memory_limit": 1000,
    "timeout_seconds": 300
}
EOF

    # MCP服务器配置
    cat > config/mcp/filesystem.json << 'EOF'
{
    "name": "filesystem-server",
    "version": "1.0.0",
    "root_path": "/app/workspace",
    "allowed_extensions": [".txt", ".md", ".py", ".json", ".yaml", ".yml"]
}
EOF

    cat > config/mcp/code_analysis.json << 'EOF'
{
    "name": "code-analysis-server", 
    "version": "1.0.0",
    "supported_languages": ["python", "javascript", "typescript"],
    "max_file_size": 1048576
}
EOF

    echo "✅ 配置文件生成完成"
}

# 构建和启动服务
start_services() {
    echo "构建和启动服务..."
    
    # 构建镜像
    docker-compose build
    
    # 启动服务
    docker-compose up -d
    
    echo "等待服务启动..."
    sleep 30
    
    # 检查服务状态
    check_services_health
}

# 检查服务健康状态
check_services_health() {
    echo "检查服务健康状态..."
    
    services=("agent-mcp-system" "filesystem-mcp" "code-analysis-mcp" "agent-redis" "agent-web-ui")
    
    for service in "${services[@]}"; do
        if docker ps --format "{{.Names}}" | grep -q "^${service}$"; then
            echo "✅ $service: 运行中"
        else
            echo "❌ $service: 未运行"
        fi
    done
    
    # 检查端口访问
    echo "检查端口访问..."
    
    if curl -f http://localhost:8000/health >/dev/null 2>&1; then
        echo "✅ Agent API: 可访问"
    else
        echo "❌ Agent API: 不可访问"
    fi
    
    if curl -f http://localhost:3000 >/dev/null 2>&1; then
        echo "✅ Web UI: 可访问"
    else
        echo "❌ Web UI: 不可访问"
    fi
}

# 运行测试
run_tests() {
    echo "运行测试..."
    
    # 测试Agent API
    response=$(curl -s -X POST http://localhost:8000/api/tasks \
        -H "Content-Type: application/json" \
        -d '{
            "description": "测试任务",
            "type": "echo",
            "parameters": {"text": "Hello Agent!"}
        }')
    
    if echo "$response" | grep -q "task_id"; then
        echo "✅ Agent API测试通过"
    else
        echo "❌ Agent API测试失败"
    fi
}

# 显示访问信息
show_access_info() {
    echo ""
    echo "🎉 部署完成！"
    echo ""
    echo "访问地址:"
    echo "  🌐 Web界面: http://localhost:3000"
    echo "  🔧 Agent API: http://localhost:8000"
    echo "  📁 文件系统MCP: http://localhost:8001"
    echo "  💻 代码分析MCP: http://localhost:8002"
    echo ""
    echo "管理命令:"
    echo "  查看状态: docker-compose ps"
    echo "  查看日志: docker-compose logs -f [service_name]"
    echo "  停止服务: docker-compose down"
    echo "  重启服务: docker-compose restart"
    echo ""
    echo "配置目录: ./config"
    echo "工作目录: ./workspace"
    echo "日志目录: ./logs"
}

# 主流程
main() {
    check_dependencies
    setup_directories
    generate_configs
    start_services
    run_tests
    show_access_info
}

# 处理命令行参数
case "${1:-deploy}" in
    "deploy")
        main
        ;;
    "stop")
        echo "停止服务..."
        docker-compose down
        ;;
    "restart")
        echo "重启服务..."
        docker-compose restart
        ;;
    "logs")
        docker-compose logs -f "${2:-}"
        ;;
    "status")
        check_services_health
        ;;
    *)
        echo "用法: $0 {deploy|stop|restart|logs|status}"
        exit 1
        ;;
esac
```

### 3. API服务接口

#### Agent HTTP API
```python
#!/usr/bin/env python3
"""
Agent HTTP API服务
提供REST API接口访问Agent功能
"""

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Dict, Any, List, Optional
import uvicorn
import asyncio
import logging

# 创建FastAPI应用
app = FastAPI(title="Agent MCP API", version="1.0.0")

# 添加CORS中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 全局Agent实例
agent_instance: Optional[DocumentAssistantAgent] = None

# 请求模型
class TaskRequest(BaseModel):
    description: str
    type: str
    parameters: Dict[str, Any] = {}
    context: Dict[str, Any] = {}

class TaskResponse(BaseModel):
    task_id: str
    status: str
    message: str

class TaskStatusResponse(BaseModel):
    task_id: str
    status: str
    result: Optional[Any] = None
    error: Optional[str] = None
    created_at: str
    started_at: Optional[str] = None
    completed_at: Optional[str] = None

class AgentStatusResponse(BaseModel):
    name: str
    status: str
    active_tasks: int
    completed_tasks: int
    capabilities: List[str]
    available_tools: List[str]
    mcp_servers: List[str]

# 启动事件
@app.on_event("startup")
async def startup_event():
    """启动时初始化Agent"""
    global agent_instance
    
    logging.info("初始化Agent...")
    
    # 创建Agent
    agent_instance = DocumentAssistantAgent()
    
    # 启动Agent
    await agent_instance.start()
    
    # 设置MCP服务器
    await agent_instance.setup_mcp_servers()
    
    logging.info("Agent初始化完成")

# 关闭事件
@app.on_event("shutdown")
async def shutdown_event():
    """关闭时清理资源"""
    global agent_instance
    
    if agent_instance:
        await agent_instance.stop()
        logging.info("Agent已停止")

# API端点
@app.get("/health")
async def health_check():
    """健康检查"""
    return {"status": "healthy", "service": "agent-mcp-api"}

@app.get("/api/agent/status", response_model=AgentStatusResponse)
async def get_agent_status():
    """获取Agent状态"""
    if not agent_instance:
        raise HTTPException(status_code=503, detail="Agent not initialized")
    
    return AgentStatusResponse(
        name=agent_instance.config.name,
        status="running" if agent_instance.running else "stopped",
        active_tasks=len(agent_instance.active_tasks),
        completed_tasks=len(agent_instance.completed_tasks),
        capabilities=[cap.value for cap in agent_instance.config.capabilities],
        available_tools=list(agent_instance.tool_registry.tools.keys()),
        mcp_servers=list(agent_instance.mcp_integration.mcp_servers.keys())
    )

@app.post("/api/tasks", response_model=TaskResponse)
async def create_task(task_request: TaskRequest, background_tasks: BackgroundTasks):
    """创建新任务"""
    if not agent_instance:
        raise HTTPException(status_code=503, detail="Agent not initialized")
    
    try:
        # 创建任务
        task = Task(
            description=task_request.description,
            type=task_request.type,
            parameters=task_request.parameters,
            context=task_request.context
        )
        
        # 添加到Agent
        task_id = await agent_instance.add_task(task)
        
        return TaskResponse(
            task_id=task_id,
            status="pending",
            message="Task created successfully"
        )
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to create task: {str(e)}")

@app.get("/api/tasks/{task_id}", response_model=TaskStatusResponse)
async def get_task_status(task_id: str):
    """获取任务状态"""
    if not agent_instance:
        raise HTTPException(status_code=503, detail="Agent not initialized")
    
    # 查找任务
    task = None
    if task_id in agent_instance.active_tasks:
        task = agent_instance.active_tasks[task_id]
    elif task_id in agent_instance.completed_tasks:
        task = agent_instance.completed_tasks[task_id]
    
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    
    return TaskStatusResponse(
        task_id=task.id,
        status=task.status.value,
        result=task.result,
        error=task.error,
        created_at=task.created_at.isoformat(),
        started_at=task.started_at.isoformat() if task.started_at else None,
        completed_at=task.completed_at.isoformat() if task.completed_at else None
    )

@app.get("/api/tasks")
async def list_tasks(status: Optional[str] = None, limit: int = 50):
    """列出任务"""
    if not agent_instance:
        raise HTTPException(status_code=503, detail="Agent not initialized")
    
    tasks = []
    
    # 收集所有任务
    all_tasks = list(agent_instance.active_tasks.values()) + list(agent_instance.completed_tasks.values())
    
    # 过滤和排序
    if status:
        all_tasks = [t for t in all_tasks if t.status.value == status]
    
    all_tasks.sort(key=lambda t: t.created_at, reverse=True)
    
    # 限制数量
    all_tasks = all_tasks[:limit]
    
    # 转换为响应格式
    for task in all_tasks:
        tasks.append({
            "task_id": task.id,
            "description": task.description,
            "type": task.type,
            "status": task.status.value,
            "created_at": task.created_at.isoformat(),
            "completed_at": task.completed_at.isoformat() if task.completed_at else None
        })
    
    return {"tasks": tasks, "total": len(tasks)}

@app.post("/api/tools/{tool_name}")
async def call_tool(tool_name: str, arguments: Dict[str, Any] = {}):
    """直接调用工具"""
    if not agent_instance:
        raise HTTPException(status_code=503, detail="Agent not initialized")
    
    tool = agent_instance.tool_registry.get_tool(tool_name)
    if not tool:
        raise HTTPException(status_code=404, detail=f"Tool not found: {tool_name}")
    
    try:
        result = await tool(**arguments)
        return {"result": result}
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Tool execution failed: {str(e)}")

@app.get("/api/tools")
async def list_tools():
    """列出可用工具"""
    if not agent_instance:
        raise HTTPException(status_code=503, detail="Agent not initialized")
    
    tools = []
    for name, description in agent_instance.tool_registry.tool_descriptions.items():
        tools.append({
            "name": name,
            "description": description
        })
    
    return {"tools": tools}

@app.get("/api/mcp/servers")
async def list_mcp_servers():
    """列出MCP服务器"""
    if not agent_instance:
        raise HTTPException(status_code=503, detail="Agent not initialized")
    
    servers = []
    for name, client in agent_instance.mcp_integration.mcp_servers.items():
        servers.append({
            "name": name,
            "status": "connected" if client else "disconnected"
        })
    
    return {"servers": servers}

@app.get("/api/mcp/{server_name}/tools")
async def list_mcp_tools(server_name: str):
    """列出MCP服务器工具"""
    if not agent_instance:
        raise HTTPException(status_code=503, detail="Agent not initialized")
    
    if server_name not in agent_instance.mcp_integration.mcp_servers:
        raise HTTPException(status_code=404, detail=f"MCP server not found: {server_name}")
    
    try:
        client = agent_instance.mcp_integration.mcp_servers[server_name]
        tools = await client.list_tools()
        return {"tools": tools}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to list tools: {str(e)}")

@app.post("/api/mcp/{server_name}/tools/{tool_name}")
async def call_mcp_tool(server_name: str, tool_name: str, arguments: Dict[str, Any] = {}):
    """调用MCP工具"""
    if not agent_instance:
        raise HTTPException(status_code=503, detail="Agent not initialized")
    
    try:
        result = await agent_instance.mcp_integration.call_mcp_tool(
            server_name, tool_name, arguments
        )
        return {"result": result}
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"MCP tool execution failed: {str(e)}")

# 主程序入口
def main():
    """启动API服务器"""
    logging.basicConfig(level=logging.INFO)
    
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info",
        access_log=True
    )

if __name__ == "__main__":
    main()
```

## 测试和调试

### 1. 单元测试

#### Agent测试
```python
#!/usr/bin/env python3
"""
Agent和MCP系统测试
"""

import pytest
import asyncio
from unittest.mock import Mock, AsyncMock

class TestAgent:
    """Agent测试类"""
    
    @pytest.fixture
    async def agent(self):
        """创建测试Agent"""
        config = AgentConfig(
            name="test-agent",
            description="测试Agent",
            capabilities=[AgentCapability.FILE_OPERATIONS]
        )
        agent = SpecializedAgent(config)
        await agent.start()
        yield agent
        await agent.stop()
    
    async def test_agent_initialization(self, agent):
        """测试Agent初始化"""
        assert agent.config.name == "test-agent"
        assert agent.running is True
        assert len(agent.tool_registry.tools) > 0
    
    async def test_task_creation(self, agent):
        """测试任务创建"""
        task = Task(
            description="测试任务",
            type="echo",
            parameters={"text": "hello"}
        )
        
        task_id = await agent.add_task(task)
        assert task_id is not None
        
        # 等待任务完成
        await asyncio.sleep(1)
        
        status = await agent.get_task_status(task_id)
        assert status == TaskStatus.COMPLETED
    
    async def test_tool_execution(self, agent):
        """测试工具执行"""
        result = await agent._echo_tool("test message")
        assert result == "Echo: test message"
    
    async def test_memory_operations(self, agent):
        """测试记忆操作"""
        # 存储记忆
        await agent._memory_store_tool("test_key", "test_value")
        
        # 检索记忆
        result = await agent._memory_retrieve_tool("test_key")
        assert result == "test_value"

class TestMCPServer:
    """MCP服务器测试类"""
    
    @pytest.fixture
    async def mcp_server(self):
        """创建测试MCP服务器"""
        server = FileSystemMCPServer(root_path="./test_workspace")
        yield server
        await server.stop()
    
    async def test_server_initialization(self, mcp_server):
        """测试服务器初始化"""
        assert mcp_server.name == "filesystem-server"
        assert len(mcp_server.tools) > 0
        assert len(mcp_server.resources) > 0
    
    async def test_tool_registration(self, mcp_server):
        """测试工具注册"""
        assert "read_file" in mcp_server.tools
        assert "write_file" in mcp_server.tools
        assert "list_directory" in mcp_server.tools
    
    @pytest.mark.asyncio
    async def test_file_operations(self, mcp_server):
        """测试文件操作"""
        # 创建测试目录
        import os
        os.makedirs("./test_workspace", exist_ok=True)
        
        # 测试写文件
        result = await mcp_server._handle_write_file_tool(
            "write_file",
            {"path": "test.txt", "content": "test content"}
        )
        assert "Successfully wrote" in result
        
        # 测试读文件
        result = await mcp_server._handle_read_file_tool(
            "read_file",
            {"path": "test.txt"}
        )
        assert "test content" in result
        
        # 清理
        os.remove("./test_workspace/test.txt")

class TestMCPIntegration:
    """MCP集成测试类"""
    
    @pytest.fixture
    async def integration_setup(self):
        """设置集成测试环境"""
        # 创建Agent
        config = AgentConfig(
            name="integration-test-agent",
            description="集成测试Agent",
            capabilities=[AgentCapability.FILE_OPERATIONS]
        )
        agent = SpecializedAgent(config)
        await agent.start()
        
        # 创建MCP集成
        integration = AgentMCPIntegration(agent)
        
        yield agent, integration
        
        await agent.stop()
    
    async def test_mcp_connection(self, integration_setup):
        """测试MCP连接"""
        agent, integration = integration_setup
        
        # 模拟MCP服务器连接
        mock_transport = Mock()
        mock_client = Mock()
        mock_client.connect = AsyncMock()
        mock_client.list_tools = AsyncMock(return_value=[])
        
        # 这里应该测试实际的连接逻辑
        assert len(integration.mcp_servers) == 0
    
    async def test_tool_integration(self, integration_setup):
        """测试工具集成"""
        agent, integration = integration_setup
        
        # 验证工具注册
        tools = agent.tool_registry.list_tools()
        assert len(tools) > 0

# 性能测试
class TestPerformance:
    """性能测试类"""
    
    @pytest.mark.asyncio
    async def test_concurrent_tasks(self):
        """测试并发任务处理"""
        config = AgentConfig(
            name="perf-test-agent",
            description="性能测试Agent",
            capabilities=[AgentCapability.FILE_OPERATIONS],
            max_concurrent_tasks=10
        )
        agent = SpecializedAgent(config)
        await agent.start()
        
        # 创建多个并发任务
        tasks = []
        for i in range(20):
            task = Task(
                description=f"任务 {i}",
                type="echo",
                parameters={"text": f"message {i}"}
            )
            task_id = await agent.add_task(task)
            tasks.append(task_id)
        
        # 等待所有任务完成
        import time
        start_time = time.time()
        
        completed_count = 0
        while completed_count < len(tasks):
            completed_count = 0
            for task_id in tasks:
                status = await agent.get_task_status(task_id)
                if status in [TaskStatus.COMPLETED, TaskStatus.FAILED]:
                    completed_count += 1
            
            await asyncio.sleep(0.1)
        
        end_time = time.time()
        execution_time = end_time - start_time
        
        print(f"执行时间: {execution_time:.2f}秒")
        print(f"平均每任务: {execution_time/len(tasks):.4f}秒")
        
        await agent.stop()
        
        # 验证性能
        assert execution_time < 10  # 应该在10秒内完成
        assert execution_time/len(tasks) < 0.5  # 平均每任务不超过0.5秒

# 集成测试
@pytest.mark.integration
class TestSystemIntegration:
    """系统集成测试"""
    
    @pytest.mark.asyncio
    async def test_full_system_workflow(self):
        """测试完整系统工作流"""
        # 这里应该测试完整的Agent-MCP系统工作流
        # 包括Agent启动、MCP连接、任务处理、结果返回等
        pass

# 运行测试的脚本
def run_tests():
    """运行所有测试"""
    import subprocess
    
    # 运行pytest
    result = subprocess.run([
        "python", "-m", "pytest",
        "-v",
        "--tb=short",
        "--cov=src",
        "--cov-report=html",
        "--cov-report=term-missing"
    ], capture_output=True, text=True)
    
    print(result.stdout)
    if result.stderr:
        print("错误:", result.stderr)
    
    return result.returncode == 0

if __name__ == "__main__":
    success = run_tests()
    exit(0 if success else 1)
```

### 2. 调试工具

#### 调试和监控工具
```python
#!/usr/bin/env python3
"""
Agent-MCP系统调试和监控工具
"""

import asyncio
import logging
import time
import psutil
import json
from datetime import datetime
from typing import Dict, List, Any

class AgentDebugger:
    """Agent调试器"""
    
    def __init__(self, agent: SpecializedAgent):
        self.agent = agent
        self.start_time = time.time()
        self.task_history: List[Dict] = []
        self.performance_metrics: Dict = {}
        
        # 设置调试日志
        self.debug_logger = logging.getLogger("AgentDebugger")
        self.debug_logger.setLevel(logging.DEBUG)
        
        # 创建文件处理器
        handler = logging.FileHandler("debug_agent.log")
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        handler.setFormatter(formatter)
        self.debug_logger.addHandler(handler)
    
    async def start_monitoring(self):
        """开始监控"""
        self.debug_logger.info("开始Agent监控")
        
        # 启动性能监控任务
        asyncio.create_task(self._performance_monitor())
        asyncio.create_task(self._task_monitor())
        asyncio.create_task(self._memory_monitor())
    
    async def _performance_monitor(self):
        """性能监控"""
        while self.agent.running:
            try:
                # 收集性能指标
                process = psutil.Process()
                
                metrics = {
                    "timestamp": datetime.now().isoformat(),
                    "cpu_percent": process.cpu_percent(),
                    "memory_mb": process.memory_info().rss / 1024 / 1024,
                    "active_tasks": len(self.agent.active_tasks),
                    "completed_tasks": len(self.agent.completed_tasks),
                    "memory_entries": len(self.agent.memory.short_term)
                }
                
                self.performance_metrics[metrics["timestamp"]] = metrics
                
                # 记录日志
                self.debug_logger.debug(f"性能指标: {metrics}")
                
                await asyncio.sleep(5)  # 每5秒收集一次
                
            except Exception as e:
                self.debug_logger.error(f"性能监控错误: {e}")
                await asyncio.sleep(1)
    
    async def _task_monitor(self):
        """任务监控"""
        monitored_tasks = set()
        
        while self.agent.running:
            try:
                # 监控新任务
                all_tasks = {**self.agent.active_tasks, **self.agent.completed_tasks}
                
                for task_id, task in all_tasks.items():
                    if task_id not in monitored_tasks:
                        # 新任务
                        self.debug_logger.info(f"新任务: {task_id} - {task.description}")
                        
                        task_info = {
                            "task_id": task_id,
                            "description": task.description,
                            "type": task.type,
                            "status": task.status.value,
                            "created_at": task.created_at.isoformat(),
                            "parameters": task.parameters
                        }
                        
                        self.task_history.append(task_info)
                        monitored_tasks.add(task_id)
                    
                    elif task.status in [TaskStatus.COMPLETED, TaskStatus.FAILED]:
                        # 任务状态变化
                        self.debug_logger.info(
                            f"任务完成: {task_id} - 状态: {task.status.value}"
                        )
                        
                        # 更新任务历史
                        for task_info in self.task_history:
                            if task_info["task_id"] == task_id:
                                task_info["status"] = task.status.value
                                task_info["completed_at"] = task.completed_at.isoformat()
                                task_info["result"] = task.result
                                task_info["error"] = task.error
                                break
                
                await asyncio.sleep(1)
                
            except Exception as e:
                self.debug_logger.error(f"任务监控错误: {e}")
                await asyncio.sleep(1)
    
    async def _memory_monitor(self):
        """内存监控"""
        while self.agent.running:
            try:
                memory_info = {
                    "short_term_count": len(self.agent.memory.short_term),
                    "long_term_count": len(self.agent.memory.long_term),
                    "working_memory_count": len(self.agent.memory.working_memory)
                }
                
                self.debug_logger.debug(f"内存状态: {memory_info}")
                
                # 检查内存泄漏
                if memory_info["short_term_count"] > self.agent.memory.limit * 0.9:
                    self.debug_logger.warning("短期内存接近限制")
                
                await asyncio.sleep(10)  # 每10秒检查一次
                
            except Exception as e:
                self.debug_logger.error(f"内存监控错误: {e}")
                await asyncio.sleep(1)
    
    def get_debug_report(self) -> Dict[str, Any]:
        """获取调试报告"""
        current_time = time.time()
        uptime = current_time - self.start_time
        
        # 计算任务统计
        completed_tasks = [t for t in self.task_history if t["status"] == "completed"]
        failed_tasks = [t for t in self.task_history if t["status"] == "failed"]
        
        # 计算平均执行时间
        avg_execution_time = 0
        if completed_tasks:
            total_time = 0
            count = 0
            for task in completed_tasks:
                if "completed_at" in task:
                    start = datetime.fromisoformat(task["created_at"])
                    end = datetime.fromisoformat(task["completed_at"])
                    total_time += (end - start).total_seconds()
                    count += 1
            
            if count > 0:
                avg_execution_time = total_time / count
        
        report = {
            "system_info": {
                "uptime_seconds": uptime,
                "agent_name": self.agent.config.name,
                "agent_status": "running" if self.agent.running else "stopped"
            },
            "task_statistics": {
                "total_tasks": len(self.task_history),
                "completed_tasks": len(completed_tasks),
                "failed_tasks": len(failed_tasks),
                "success_rate": len(completed_tasks) / len(self.task_history) if self.task_history else 0,
                "average_execution_time": avg_execution_time
            },
            "performance_summary": self._summarize_performance(),
            "memory_status": {
                "short_term_entries": len(self.agent.memory.short_term),
                "long_term_entries": len(self.agent.memory.long_term),
                "working_memory_entries": len(self.agent.memory.working_memory)
            },
            "recent_tasks": self.task_history[-10:]  # 最近10个任务
        }
        
        return report
    
    def _summarize_performance(self) -> Dict[str, Any]:
        """性能统计摘要"""
        if not self.performance_metrics:
            return {}
        
        metrics_values = list(self.performance_metrics.values())
        
        cpu_values = [m["cpu_percent"] for m in metrics_values]
        memory_values = [m["memory_mb"] for m in metrics_values]
        
        return {
            "avg_cpu_percent": sum(cpu_values) / len(cpu_values),
            "max_cpu_percent": max(cpu_values),
            "avg_memory_mb": sum(memory_values) / len(memory_values),
            "max_memory_mb": max(memory_values),
            "data_points": len(metrics_values)
        }
    
    def save_debug_data(self, filename: str = None):
        """保存调试数据"""
        if not filename:
            filename = f"debug_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        debug_data = {
            "report": self.get_debug_report(),
            "task_history": self.task_history,
            "performance_metrics": self.performance_metrics
        }
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(debug_data, f, indent=2, ensure_ascii=False)
        
        self.debug_logger.info(f"调试数据已保存到: {filename}")

class MCPDebugger:
    """MCP调试器"""
    
    def __init__(self, server: MCPServer):
        self.server = server
        self.request_history: List[Dict] = []
        self.error_history: List[Dict] = []
        
        # 设置调试日志
        self.debug_logger = logging.getLogger("MCPDebugger")
        self.debug_logger.setLevel(logging.DEBUG)
        
        handler = logging.FileHandler("debug_mcp.log")
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        handler.setFormatter(formatter)
        self.debug_logger.addHandler(handler)
        
        # 包装原始的消息处理方法
        self._wrap_message_handler()
    
    def _wrap_message_handler(self):
        """包装消息处理方法"""
        original_handle_message = self.server._handle_message
        
        async def debug_handle_message(message):
            start_time = time.time()
            
            # 记录请求
            request_info = {
                "timestamp": datetime.now().isoformat(),
                "method": message.method,
                "id": message.id,
                "params": message.params
            }
            
            self.debug_logger.debug(f"收到请求: {request_info}")
            
            try:
                result = await original_handle_message(message)
                
                # 记录成功
                execution_time = time.time() - start_time
                request_info.update({
                    "status": "success",
                    "execution_time": execution_time
                })
                
                self.request_history.append(request_info)
                
                return result
                
            except Exception as e:
                # 记录错误
                execution_time = time.time() - start_time
                error_info = {
                    **request_info,
                    "status": "error",
                    "error": str(e),
                    "execution_time": execution_time
                }
                
                self.error_history.append(error_info)
                self.debug_logger.error(f"请求处理错误: {error_info}")
                
                raise
        
        self.server._handle_message = debug_handle_message
    
    def get_debug_report(self) -> Dict[str, Any]:
        """获取MCP调试报告"""
        total_requests = len(self.request_history) + len(self.error_history)
        success_requests = len(self.request_history)
        
        # 计算平均响应时间
        all_requests = self.request_history + self.error_history
        avg_response_time = 0
        if all_requests:
            total_time = sum(req.get("execution_time", 0) for req in all_requests)
            avg_response_time = total_time / len(all_requests)
        
        # 统计方法调用
        method_stats = {}
        for req in all_requests:
            method = req.get("method", "unknown")
            if method not in method_stats:
                method_stats[method] = {"count": 0, "errors": 0}
            method_stats[method]["count"] += 1
            if req.get("status") == "error":
                method_stats[method]["errors"] += 1
        
        return {
            "server_info": {
                "name": self.server.name,
                "version": self.server.version,
                "initialized": self.server.initialized
            },
            "request_statistics": {
                "total_requests": total_requests,
                "successful_requests": success_requests,
                "failed_requests": len(self.error_history),
                "success_rate": success_requests / total_requests if total_requests > 0 else 0,
                "average_response_time": avg_response_time
            },
            "method_statistics": method_stats,
            "resource_count": len(self.server.resources),
            "tool_count": len(self.server.tools),
            "prompt_count": len(self.server.prompts),
            "recent_errors": self.error_history[-5:]  # 最近5个错误
        }

# 调试工具主程序
async def debug_main():
    """调试工具主程序"""
    # 创建Agent
    config = AgentConfig(
        name="debug-agent",
        description="调试测试Agent",
        capabilities=[AgentCapability.FILE_OPERATIONS]
    )
    agent = SpecializedAgent(config)
    
    # 创建调试器
    agent_debugger = AgentDebugger(agent)
    
    # 启动Agent和监控
    await agent.start()
    await agent_debugger.start_monitoring()
    
    # 运行一些测试任务
    print("运行测试任务...")
    
    for i in range(5):
        task = Task(
            description=f"测试任务 {i}",
            type="echo",
            parameters={"text": f"测试消息 {i}"}
        )
        await agent.add_task(task)
    
    # 等待任务完成
    await asyncio.sleep(10)
    
    # 生成调试报告
    report = agent_debugger.get_debug_report()
    print("调试报告:", json.dumps(report, indent=2, ensure_ascii=False))
    
    # 保存调试数据
    agent_debugger.save_debug_data()
    
    await agent.stop()

if __name__ == "__main__":
    asyncio.run(debug_main())
```

## 扩展和优化

### 1. 高级功能扩展

#### 分布式Agent集群
```python
#!/usr/bin/env python3
"""
分布式Agent集群实现
支持多Agent协作和负载均衡
"""

import asyncio
import json
import redis
from typing import Dict, List, Optional
from dataclasses import asdict

class AgentCluster:
    """Agent集群管理器"""
    
    def __init__(self, cluster_name: str, redis_url: str = "redis://localhost:6379"):
        self.cluster_name = cluster_name
        self.redis_client = redis.from_url(redis_url)
        self.agents: Dict[str, SpecializedAgent] = {}
        self.running = False
        
        # 集群配置
        self.heartbeat_interval = 30  # 心跳间隔（秒）
        self.task_timeout = 300  # 任务超时（秒）
    
    async def add_agent(self, agent: SpecializedAgent):
        """添加Agent到集群"""
        agent_id = f"{agent.config.name}_{id(agent)}"
        self.agents[agent_id] = agent
        
        # 注册到Redis
        await self._register_agent(agent_id, agent)
        
        print(f"Agent {agent_id} 已加入集群")
    
    async def _register_agent(self, agent_id: str, agent: SpecializedAgent):
        """在Redis中注册Agent"""
        agent_info = {
            "id": agent_id,
            "name": agent.config.name,
            "capabilities": [cap.value for cap in agent.config.capabilities],
            "max_concurrent_tasks": agent.config.max_concurrent_tasks,
            "status": "active",
            "last_heartbeat": time.time()
        }
        
        # 存储Agent信息
        self.redis_client.hset(
            f"cluster:{self.cluster_name}:agents",
            agent_id,
            json.dumps(agent_info)
        )
    
    async def start_cluster(self):
        """启动集群"""
        self.running = True
        
        # 启动所有Agent
        for agent in self.agents.values():
            await agent.start()
        
        # 启动集群服务
        asyncio.create_task(self._heartbeat_loop())
        asyncio.create_task(self._task_distribution_loop())
        asyncio.create_task(self._health_monitor_loop())
        
        print(f"集群 {self.cluster_name} 已启动")
    
    async def stop_cluster(self):
        """停止集群"""
        self.running = False
        
        # 停止所有Agent
        for agent in self.agents.values():
            await agent.stop()
        
        # 从Redis清理信息
        self.redis_client.delete(f"cluster:{self.cluster_name}:agents")
        
        print(f"集群 {self.cluster_name} 已停止")
    
    async def submit_task(self, task: Task) -> str:
        """提交任务到集群"""
        # 选择最适合的Agent
        best_agent_id = await self._select_agent_for_task(task)
        
        if not best_agent_id:
            raise ValueError("没有可用的Agent处理此任务")
        
        # 将任务分配给选定的Agent
        agent = self.agents[best_agent_id]
        task_id = await agent.add_task(task)
        
        # 记录任务分配
        task_info = {
            "task_id": task_id,
            "agent_id": best_agent_id,
            "submitted_at": time.time(),
            "task": asdict(task)
        }
        
        self.redis_client.hset(
            f"cluster:{self.cluster_name}:tasks",
            task_id,
            json.dumps(task_info)
        )
        
        return task_id
    
    async def _select_agent_for_task(self, task: Task) -> Optional[str]:
        """为任务选择最合适的Agent"""
        # 获取所有活跃的Agent
        active_agents = []
        
        for agent_id, agent in self.agents.items():
            if agent.running and len(agent.active_tasks) < agent.config.max_concurrent_tasks:
                # 检查Agent能力
                required_capabilities = self._get_required_capabilities(task)
                if all(cap in agent.config.capabilities for cap in required_capabilities):
                    active_agents.append((agent_id, agent))
        
        if not active_agents:
            return None
        
        # 选择负载最少的Agent
        best_agent = min(active_agents, key=lambda x: len(x[1].active_tasks))
        return best_agent[0]
    
    def _get_required_capabilities(self, task: Task) -> List[AgentCapability]:
        """根据任务类型确定所需能力"""
        capability_map = {
            "web_search": [AgentCapability.WEB_SEARCH],
            "file_read": [AgentCapability.FILE_OPERATIONS],
            "file_write": [AgentCapability.FILE_OPERATIONS],
            "code_execution": [AgentCapability.CODE_EXECUTION],
            "analysis": [AgentCapability.DATA_ANALYSIS]
        }
        
        return capability_map.get(task.type, [])
    
    async def _heartbeat_loop(self):
        """心跳循环"""
        while self.running:
            try:
                for agent_id, agent in self.agents.items():
                    if agent.running:
                        # 更新心跳
                        agent_info_str = self.redis_client.hget(
                            f"cluster:{self.cluster_name}:agents",
                            agent_id
                        )
                        
                        if agent_info_str:
                            agent_info = json.loads(agent_info_str)
                            agent_info["last_heartbeat"] = time.time()
                            agent_info["active_tasks"] = len(agent.active_tasks)
                            
                            self.redis_client.hset(
                                f"cluster:{self.cluster_name}:agents",
                                agent_id,
                                json.dumps(agent_info)
                            )
                
                await asyncio.sleep(self.heartbeat_interval)
                
            except Exception as e:
                print(f"心跳循环错误: {e}")
                await asyncio.sleep(5)
    
    async def _task_distribution_loop(self):
        """任务分发循环"""
        while self.running:
            try:
                # 检查待分发的任务队列
                task_queue_key = f"cluster:{self.cluster_name}:pending_tasks"
                
                # 从Redis队列获取待处理任务
                task_data = self.redis_client.lpop(task_queue_key)
                
                if task_data:
                    task_info = json.loads(task_data)
                    task = Task(**task_info["task"])
                    
                    # 分配任务
                    try:
                        task_id = await self.submit_task(task)
                        print(f"任务 {task_id} 已分配")
                    except Exception as e:
                        print(f"任务分配失败: {e}")
                        # 重新放回队列
                        self.redis_client.rpush(task_queue_key, task_data)
                
                await asyncio.sleep(1)
                
            except Exception as e:
                print(f"任务分发循环错误: {e}")
                await asyncio.sleep(5)
    
    async def _health_monitor_loop(self):
        """健康监控循环"""
        while self.running:
            try:
                current_time = time.time()
                
                # 检查Agent健康状态
                agent_infos = self.redis_client.hgetall(f"cluster:{self.cluster_name}:agents")
                
                for agent_id, agent_info_str in agent_infos.items():
                    agent_info = json.loads(agent_info_str)
                    last_heartbeat = agent_info.get("last_heartbeat", 0)
                    
                    # 检查是否超时
                    if current_time - last_heartbeat > self.heartbeat_interval * 2:
                        print(f"Agent {agent_id} 心跳超时，标记为不健康")
                        agent_info["status"] = "unhealthy"
                        
                        self.redis_client.hset(
                            f"cluster:{self.cluster_name}:agents",
                            agent_id,
                            json.dumps(agent_info)
                        )
                
                await asyncio.sleep(self.heartbeat_interval)
                
            except Exception as e:
                print(f"健康监控循环错误: {e}")
                await asyncio.sleep(5)
    
    async def get_cluster_status(self) -> Dict:
        """获取集群状态"""
        agent_infos = self.redis_client.hgetall(f"cluster:{self.cluster_name}:agents")
        task_infos = self.redis_client.hgetall(f"cluster:{self.cluster_name}:tasks")
        
        active_agents = 0
        total_active_tasks = 0
        
        for agent_info_str in agent_infos.values():
            agent_info = json.loads(agent_info_str)
            if agent_info.get("status") == "active":
                active_agents += 1
                total_active_tasks += agent_info.get("active_tasks", 0)
        
        return {
            "cluster_name": self.cluster_name,
            "total_agents": len(agent_infos),
            "active_agents": active_agents,
            "total_active_tasks": total_active_tasks,
            "total_completed_tasks": len(task_infos)
        }

# 使用示例
async def cluster_demo():
    """集群使用演示"""
    # 创建集群
    cluster = AgentCluster("demo-cluster")
    
    # 创建多个Agent
    agents = []
    for i in range(3):
        config = AgentConfig(
            name=f"agent-{i}",
            description=f"集群Agent {i}",
            capabilities=[
                AgentCapability.FILE_OPERATIONS,
                AgentCapability.WEB_SEARCH,
                AgentCapability.CODE_EXECUTION
            ]
        )
        agent = SpecializedAgent(config)
        agents.append(agent)
        await cluster.add_agent(agent)
    
    # 启动集群
    await cluster.start_cluster()
    
    # 提交任务
    for i in range(10):
        task = Task(
            description=f"集群任务 {i}",
            type="echo",
            parameters={"text": f"消息 {i}"}
        )
        task_id = await cluster.submit_task(task)
        print(f"任务 {task_id} 已提交")
    
    # 等待处理
    await asyncio.sleep(10)
    
    # 获取集群状态
    status = await cluster.get_cluster_status()
    print("集群状态:", status)
    
    # 停止集群
    await cluster.stop_cluster()

if __name__ == "__main__":
    import time
    asyncio.run(cluster_demo())
```

### 2. 性能优化

#### 缓存和优化策略
```python
#!/usr/bin/env python3
"""
Agent-MCP系统性能优化
"""

import asyncio
import time
import hashlib
import pickle
from functools import wraps
from typing import Any, Callable, Dict, Optional

class CacheManager:
    """缓存管理器"""
    
    def __init__(self, max_size: int = 1000, ttl: int = 3600):
        self.max_size = max_size
        self.ttl = ttl
        self.cache: Dict[str, Dict] = {}
        
        # 启动清理任务
        asyncio.create_task(self._cleanup_loop())
    
    def _generate_key(self, func_name: str, args: tuple, kwargs: dict) -> str:
        """生成缓存键"""
        key_data = f"{func_name}:{args}:{sorted(kwargs.items())}"
        return hashlib.md5(key_data.encode()).hexdigest()
    
    async def get(self, key: str) -> Optional[Any]:
        """获取缓存值"""
        if key in self.cache:
            entry = self.cache[key]
            if time.time() - entry["timestamp"] < self.ttl:
                entry["access_count"] += 1
                entry["last_access"] = time.time()
                return entry["value"]
            else:
                # 过期删除
                del self.cache[key]
        return None
    
    async def set(self, key: str, value: Any):
        """设置缓存值"""
        # 检查缓存大小
        if len(self.cache) >= self.max_size:
            await self._evict_lru()
        
        self.cache[key] = {
            "value": value,
            "timestamp": time.time(),
            "last_access": time.time(),
            "access_count": 1
        }
    
    async def _evict_lru(self):
        """LRU淘汰策略"""
        if not self.cache:
            return
        
        # 找到最少使用的条目
        lru_key = min(
            self.cache.keys(),
            key=lambda k: (self.cache[k]["access_count"], self.cache[k]["last_access"])
        )
        del self.cache[lru_key]
    
    async def _cleanup_loop(self):
        """定期清理过期缓存"""
        while True:
            try:
                current_time = time.time()
                expired_keys = [
                    key for key, entry in self.cache.items()
                    if current_time - entry["timestamp"] > self.ttl
                ]
                
                for key in expired_keys:
                    del self.cache[key]
                
                await asyncio.sleep(300)  # 每5分钟清理一次
                
            except Exception as e:
                print(f"缓存清理错误: {e}")
                await asyncio.sleep(60)

# 缓存装饰器
def cached(cache_manager: CacheManager, ttl: Optional[int] = None):
    """缓存装饰器"""
    def decorator(func: Callable):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            # 生成缓存键
            cache_key = cache_manager._generate_key(func.__name__, args, kwargs)
            
            # 尝试从缓存获取
            cached_result = await cache_manager.get(cache_key)
            if cached_result is not None:
                return cached_result
            
            # 执行函数
            result = await func(*args, **kwargs)
            
            # 存储到缓存
            await cache_manager.set(cache_key, result)
            
            return result
        
        return wrapper
    return decorator

class OptimizedAgent(SpecializedAgent):
    """优化的Agent实现"""
    
    def __init__(self, config: AgentConfig):
        super().__init__(config)
        
        # 初始化缓存管理器
        self.cache_manager = CacheManager(max_size=500, ttl=1800)
        
        # 性能统计
        self.performance_stats = {
            "task_execution_times": [],
            "cache_hits": 0,
            "cache_misses": 0,
            "total_requests": 0
        }
        
        # 注册优化工具
        self._register_optimized_tools()
    
    def _register_optimized_tools(self):
        """注册优化工具"""
        # 使用缓存的Web搜索工具
        @cached(self.cache_manager, ttl=3600)  # 缓存1小时
        async def cached_web_search(query: str, num_results: int = 5):
            return await super(OptimizedAgent, self)._web_search_tool(query, num_results)
        
        self.tool_registry.register_tool(
            "cached_web_search",
            cached_web_search,
            "Cached web search tool"
        )
        
        # 批量文件操作工具
        async def batch_file_read(file_paths: list) -> dict:
            """批量读取文件"""
            results = {}
            
            # 并行读取文件
            tasks = []
            for path in file_paths:
                task = asyncio.create_task(self._read_file_tool(path))
                tasks.append((path, task))
            
            # 等待所有任务完成
            for path, task in tasks:
                try:
                    results[path] = await task
                except Exception as e:
                    results[path] = f"Error: {str(e)}"
            
            return results
        
        self.tool_registry.register_tool(
            "batch_file_read",
            batch_file_read,
            "Batch file reading tool"
        )
    
    async def _execute_single_task(self, task: Task) -> Any:
        """优化的任务执行"""
        start_time = time.time()
        
        try:
            # 记录请求
            self.performance_stats["total_requests"] += 1
            
            # 执行任务
            result = await super()._execute_single_task(task)
            
            # 记录执行时间
            execution_time = time.time() - start_time
            self.performance_stats["task_execution_times"].append(execution_time)
            
            # 保持统计数据在合理大小
            if len(self.performance_stats["task_execution_times"]) > 1000:
                self.performance_stats["task_execution_times"] = \
                    self.performance_stats["task_execution_times"][-500:]
            
            return result
            
        except Exception as e:
            # 记录执行时间（即使失败）
            execution_time = time.time() - start_time
            self.performance_stats["task_execution_times"].append(execution_time)
            raise
    
    def get_performance_stats(self) -> Dict[str, Any]:
        """获取性能统计"""
        execution_times = self.performance_stats["task_execution_times"]
        
        if execution_times:
            avg_time = sum(execution_times) / len(execution_times)
            max_time = max(execution_times)
            min_time = min(execution_times)
        else:
            avg_time = max_time = min_time = 0
        
        cache_total = self.performance_stats["cache_hits"] + self.performance_stats["cache_misses"]
        cache_hit_rate = (
            self.performance_stats["cache_hits"] / cache_total
            if cache_total > 0 else 0
        )
        
        return {
            "total_requests": self.performance_stats["total_requests"],
            "average_execution_time": avg_time,
            "max_execution_time": max_time,
            "min_execution_time": min_time,
            "cache_hit_rate": cache_hit_rate,
            "cache_size": len(self.cache_manager.cache),
            "active_tasks": len(self.active_tasks),
            "completed_tasks": len(self.completed_tasks)
        }

class ConnectionPool:
    """连接池管理器"""
    
    def __init__(self, max_connections: int = 10):
        self.max_connections = max_connections
        self.available_connections = asyncio.Queue(maxsize=max_connections)
        self.all_connections = []
        self._initialized = False
    
    async def initialize(self):
        """初始化连接池"""
        if self._initialized:
            return
        
        for i in range(self.max_connections):
            # 这里应该创建实际的连接对象
            connection = f"connection_{i}"  # 简化示例
            await self.available_connections.put(connection)
            self.all_connections.append(connection)
        
        self._initialized = True
    
    async def get_connection(self):
        """获取连接"""
        if not self._initialized:
            await self.initialize()
        
        return await self.available_connections.get()
    
    async def return_connection(self, connection):
        """归还连接"""
        await self.available_connections.put(connection)
    
    async def close_all(self):
        """关闭所有连接"""
        while not self.available_connections.empty():
            connection = await self.available_connections.get()
            # 这里应该关闭实际的连接
        
        self.all_connections.clear()
        self._initialized = False

# 使用连接池的上下文管理器
class ConnectionContext:
    """连接上下文管理器"""
    
    def __init__(self, pool: ConnectionPool):
        self.pool = pool
        self.connection = None
    
    async def __aenter__(self):
        self.connection = await self.pool.get_connection()
        return self.connection
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.connection:
            await self.pool.return_connection(self.connection)

# 性能优化的MCP服务器
class OptimizedMCPServer(MCPServer):
    """优化的MCP服务器"""
    
    def __init__(self, name: str, version: str):
        super().__init__(name, version)
        self.connection_pool = ConnectionPool(max_connections=20)
        self.request_stats = {
            "total_requests": 0,
            "successful_requests": 0,
            "failed_requests": 0,
            "response_times": []
        }
    
    async def _handle_message(self, message: MCPMessage):
        """优化的消息处理"""
        start_time = time.time()
        
        try:
            self.request_stats["total_requests"] += 1
            
            # 使用连接池
            async with ConnectionContext(self.connection_pool) as connection:
                result = await super()._handle_message(message)
                
                self.request_stats["successful_requests"] += 1
                return result
                
        except Exception as e:
            self.request_stats["failed_requests"] += 1
            raise
        finally:
            # 记录响应时间
            response_time = time.time() - start_time
            self.request_stats["response_times"].append(response_time)
            
            # 保持统计数据在合理大小
            if len(self.request_stats["response_times"]) > 1000:
                self.request_stats["response_times"] = \
                    self.request_stats["response_times"][-500:]
    
    def get_performance_metrics(self) -> Dict[str, Any]:
        """获取性能指标"""
        response_times = self.request_stats["response_times"]
        
        if response_times:
            avg_response_time = sum(response_times) / len(response_times)
            max_response_time = max(response_times)
            min_response_time = min(response_times)
        else:
            avg_response_time = max_response_time = min_response_time = 0
        
        success_rate = (
            self.request_stats["successful_requests"] / 
            self.request_stats["total_requests"]
            if self.request_stats["total_requests"] > 0 else 0
        )
        
        return {
            "total_requests": self.request_stats["total_requests"],
            "success_rate": success_rate,
            "average_response_time": avg_response_time,
            "max_response_time": max_response_time,
            "min_response_time": min_response_time,
            "connection_pool_size": len(self.connection_pool.all_connections),
            "available_connections": self.connection_pool.available_connections.qsize()
        }

# 性能测试和优化演示
async def optimization_demo():
    """性能优化演示"""
    print("创建优化的Agent...")
    
    config = AgentConfig(
        name="optimized-agent",
        description="优化的Agent",
        capabilities=[AgentCapability.FILE_OPERATIONS, AgentCapability.WEB_SEARCH]
    )
    
    agent = OptimizedAgent(config)
    await agent.start()
    
    # 执行一些任务来生成性能数据
    print("执行测试任务...")
    
    tasks = []
    for i in range(20):
        task = Task(
            description=f"性能测试任务 {i}",
            type="cached_web_search" if i % 2 == 0 else "echo",
            parameters={"query": f"test query {i % 5}", "text": f"message {i}"}
        )
        task_id = await agent.add_task(task)
        tasks.append(task_id)
    
    # 等待任务完成
    print("等待任务完成...")
    completed = 0
    while completed < len(tasks):
        completed = 0
        for task_id in tasks:
            status = await agent.get_task_status(task_id)
            if status in [TaskStatus.COMPLETED, TaskStatus.FAILED]:
                completed += 1
        await asyncio.sleep(0.5)
    
    # 显示性能统计
    stats = agent.get_performance_stats()
    print("性能统计:")
    print(f"  总请求数: {stats['total_requests']}")
    print(f"  平均执行时间: {stats['average_execution_time']:.4f}秒")
    print(f"  最大执行时间: {stats['max_execution_time']:.4f}秒")
    print(f"  缓存命中率: {stats['cache_hit_rate']:.2%}")
    print(f"  缓存大小: {stats['cache_size']}")
    
    await agent.stop()

if __name__ == "__main__":
    asyncio.run(optimization_demo())
```

## 生产环境部署

### 1. 生产级配置

#### 生产环境配置文件
```yaml
# production.yml - 生产环境配置
version: '3.8'

services:
  # Agent服务 - 高可用配置
  agent-service:
    build:
      context: .
      dockerfile: Dockerfile.prod
    deploy:
      replicas: 3
      update_config:
        parallelism: 1
        delay: 10s
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
    environment:
      - SERVICE_TYPE=agent
      - AGENT_NAME=production-assistant
      - LOG_LEVEL=WARNING
      - REDIS_URL=redis://redis-cluster:6379
      - POSTGRES_URL=postgresql://user:password@postgres:5432/agentdb
    volumes:
      - agent_data:/app/data
      - agent_logs:/app/logs
    networks:
      - agent-net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # Redis集群
  redis-cluster:
    image: redis:7-alpine
    command: redis-server --appendonly yes --cluster-enabled yes
    deploy:
      replicas: 6
    volumes:
      - redis_data:/data
    networks:
      - agent-net

  # PostgreSQL主从
  postgres-master:
    image: postgres:15
    environment:
      POSTGRES_DB: agentdb
      POSTGRES_USER: agent_user
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_REPLICATION_USER: replicator
      POSTGRES_REPLICATION_PASSWORD: ${REPLICATION_PASSWORD}
    volumes:
      - postgres_master_data:/var/lib/postgresql/data
      - ./postgres-master.conf:/etc/postgresql/postgresql.conf
    command: postgres -c config_file=/etc/postgresql/postgresql.conf
    networks:
      - agent-net

  postgres-slave:
    image: postgres:15
    environment:
      POSTGRES_MASTER_SERVICE: postgres-master
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_USER: agent_user
      POSTGRES_DB: agentdb
    volumes:
      - postgres_slave_data:/var/lib/postgresql/data
    depends_on:
      - postgres-master
    networks:
      - agent-net

  # Nginx负载均衡
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx-prod.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - agent-service
    networks:
      - agent-net
    deploy:
      replicas: 2

  # 监控服务
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus-prod.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
    networks:
      - monitoring-net
      - agent-net

  grafana:
    image: grafana/grafana:latest
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
      GF_SECURITY_ADMIN_USER: admin
    volumes:
      - grafana_data:/var/lib/grafana
    ports:
      - "3000:3000"
    networks:
      - monitoring-net

  # 日志收集
  fluentd:
    image: fluent/fluentd:v1.16-debian-1
    volumes:
      - ./fluentd.conf:/fluentd/etc/fluent.conf
      - agent_logs:/var/log/agent
    networks:
      - agent-net

volumes:
  agent_data:
  agent_logs:
  redis_data:
  postgres_master_data:
  postgres_slave_data:
  prometheus_data:
  grafana_data:

networks:
  agent-net:
    driver: overlay
    attachable: true
  monitoring-net:
    driver: overlay

secrets:
  postgres_password:
    external: true
  grafana_password:
    external: true
```

### 2. 安全配置

#### 安全强化脚本
```bash
#!/bin/bash
# security-hardening.sh - 生产环境安全强化

set -e

echo "🔒 开始生产环境安全强化..."

# 1. 创建安全用户
create_secure_user() {
    echo "创建安全用户..."
    
    # 创建专用用户
    useradd -r -s /bin/false -d /opt/agent-mcp agent-service
    
    # 设置目录权限
    mkdir -p /opt/agent-mcp/{data,logs,config}
    chown -R agent-service:agent-service /opt/agent-mcp
    chmod 750 /opt/agent-mcp
    chmod 700 /opt/agent-mcp/{data,logs}
    chmod 755 /opt/agent-mcp/config
}

# 2. 配置防火墙
configure_firewall() {
    echo "配置防火墙..."
    
    # 启用防火墙
    ufw --force enable
    
    # 默认策略
    ufw default deny incoming
    ufw default allow outgoing
    
    # 允许必要端口
    ufw allow 22/tcp    # SSH
    ufw allow 80/tcp    # HTTP
    ufw allow 443/tcp   # HTTPS
    
    # 内部服务端口（限制源IP）
    ufw allow from 10.0.0.0/8 to any port 6379    # Redis
    ufw allow from 10.0.0.0/8 to any port 5432    # PostgreSQL
    ufw allow from 10.0.0.0/8 to any port 9090    # Prometheus
    
    # 显示状态
    ufw status verbose
}

# 3. SSL证书配置
setup_ssl_certificates() {
    echo "配置SSL证书..."
    
    SSL_DIR="/etc/ssl/agent-mcp"
    mkdir -p $SSL_DIR
    
    # 生成自签名证书（生产环境应使用正式证书）
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout $SSL_DIR/private.key \
        -out $SSL_DIR/certificate.crt \
        -subj "/C=US/ST=State/L=City/O=Organization/CN=agent-mcp.local"
    
    # 设置证书权限
    chmod 600 $SSL_DIR/private.key
    chmod 644 $SSL_DIR/certificate.crt
    chown root:root $SSL_DIR/*
}

# 4. 密钥管理
setup_secrets_management() {
    echo "配置密钥管理..."
    
    # 创建密钥目录
    SECRETS_DIR="/opt/agent-mcp/secrets"
    mkdir -p $SECRETS_DIR
    chmod 700 $SECRETS_DIR
    chown agent-service:agent-service $SECRETS_DIR
    
    # 生成随机密码
    generate_password() {
        openssl rand -base64 32
    }
    
    # 数据库密码
    echo "$(generate_password)" > $SECRETS_DIR/postgres_password
    echo "$(generate_password)" > $SECRETS_DIR/redis_password
    echo "$(generate_password)" > $SECRETS_DIR/api_secret_key
    
    # 设置密钥权限
    chmod 600 $SECRETS_DIR/*
    chown agent-service:agent-service $SECRETS_DIR/*
}

# 5. 审计日志配置
configure_audit_logging() {
    echo "配置审计日志..."
    
    # 安装auditd
    apt-get update
    apt-get install -y auditd audispd-plugins
    
    # 配置审计规则
    cat >> /etc/audit/rules.d/agent-mcp.rules << 'EOF'
# 监控Agent-MCP相关文件
-w /opt/agent-mcp/ -p wa -k agent-mcp-files
-w /etc/ssl/agent-mcp/ -p wa -k agent-mcp-ssl

# 监控系统调用
-a always,exit -F arch=b64 -S execve -k exec
-a always,exit -F arch=b64 -S connect -k network

# 监控用户活动
-w /etc/passwd -p wa -k user-management
-w /etc/group -p wa -k user-management
-w /etc/shadow -p wa -k user-management
EOF
    
    # 重启auditd
    systemctl restart auditd
    systemctl enable auditd
}

# 6. 系统加固
system_hardening() {
    echo "系统加固..."
    
    # 更新系统
    apt-get update && apt-get upgrade -y
    
    # 安装安全工具
    apt-get install -y \
        fail2ban \
        rkhunter \
        chkrootkit \
        aide \
        logwatch
    
    # 配置fail2ban
    cat > /etc/fail2ban/jail.local << 'EOF'
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = ssh
logpath = /var/log/auth.log
maxretry = 3

[nginx-http-auth]
enabled = true
filter = nginx-http-auth
logpath = /var/log/nginx/error.log
maxretry = 5
EOF
    
    # 启动服务
    systemctl restart fail2ban
    systemctl enable fail2ban
    
    # 配置系统参数
    cat >> /etc/sysctl.conf << 'EOF'
# 网络安全参数
net.ipv4.conf.default.rp_filter = 1
net.ipv4.conf.all.rp_filter = 1
net.ipv4.ip_forward = 0
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.default.send_redirects = 0
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.accept_source_route = 0
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv4.conf.all.secure_redirects = 0
net.ipv4.conf.default.secure_redirects = 0
net.ipv4.conf.all.log_martians = 1
net.ipv4.conf.default.log_martians = 1
net.ipv4.icmp_echo_ignore_broadcasts = 1
net.ipv4.icmp_ignore_bogus_error_responses = 1
net.ipv4.tcp_syncookies = 1
EOF
    
    # 应用系统参数
    sysctl -p
}

# 7. Docker安全配置
configure_docker_security() {
    echo "配置Docker安全..."
    
    # 创建Docker daemon配置
    mkdir -p /etc/docker
    cat > /etc/docker/daemon.json << 'EOF'
{
    "live-restore": true,
    "userland-proxy": false,
    "no-new-privileges": true,
    "log-driver": "json-file",
    "log-opts": {
        "max-size": "10m",
        "max-file": "3"
    },
    "storage-driver": "overlay2",
    "selinux-enabled": true,
    "userns-remap": "default"
}
EOF
    
    # 重启Docker
    systemctl restart docker
}

# 主函数
main() {
    # 检查权限
    if [[ $EUID -ne 0 ]]; then
        echo "此脚本需要root权限运行"
        exit 1
    fi
    
    create_secure_user
    configure_firewall
    setup_ssl_certificates
    setup_secrets_management
    configure_audit_logging
    system_hardening
    configure_docker_security
    
    echo "✅ 安全强化完成"
    echo ""
    echo "重要提醒:"
    echo "1. 修改默认SSH端口"
    echo "2. 配置密钥认证，禁用密码登录"
    echo "3. 定期更新系统和依赖"
    echo "4. 监控审计日志"
    echo "5. 备份重要数据"
}

main "$@"
```

### 3. 监控和告警

#### 完整监控配置
```yaml
# monitoring-stack.yml
version: '3.8'

services:
  # Prometheus配置
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=90d'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'
      - '--web.enable-admin-api'
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/rules:/etc/prometheus/rules
      - prometheus_data:/prometheus
    ports:
      - "9090:9090"
    networks:
      - monitoring
    restart: unless-stopped

  # Grafana仪表盘
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
      - GF_USERS_ALLOW_SIGN_UP=false
      - GF_SMTP_ENABLED=true
      - GF_SMTP_HOST=${SMTP_HOST}
      - GF_SMTP_USER=${SMTP_USER}
      - GF_SMTP_PASSWORD=${SMTP_PASSWORD}
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources
    ports:
      - "3000:3000"
    networks:
      - monitoring
    restart: unless-stopped

  # AlertManager告警
  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
      - '--web.external-url=http://localhost:9093'
    volumes:
      - ./monitoring/alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager_data:/alertmanager
    ports:
      - "9093:9093"
    networks:
      - monitoring
    restart: unless-stopped

  # Node Exporter
  node-exporter:
    image: prom/node-exporter:latest
    container_name: node-exporter
    command:
      - '--path.procfs=/host/proc'
      - '--path.rootfs=/rootfs'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    ports:
      - "9100:9100"
    networks:
      - monitoring
    restart: unless-stopped

  # cAdvisor容器监控
  cadvisor:
    image: gcr.io/cadvisor/cadvisor:latest
    container_name: cadvisor
    volumes:
      - /:/rootfs:ro
      - /var/run:/var/run:rw
      - /sys:/sys:ro
      - /var/lib/docker/:/var/lib/docker:ro
    ports:
      - "8080:8080"
    networks:
      - monitoring
    restart: unless-stopped

  # 日志聚合
  loki:
    image: grafana/loki:latest
    container_name: loki
    command: -config.file=/etc/loki/local-config.yaml
    volumes:
      - ./monitoring/loki-config.yml:/etc/loki/local-config.yaml
      - loki_data:/loki
    ports:
      - "3100:3100"
    networks:
      - monitoring
    restart: unless-stopped

  promtail:
    image: grafana/promtail:latest
    container_name: promtail
    command: -config.file=/etc/promtail/config.yml
    volumes:
      - ./monitoring/promtail-config.yml:/etc/promtail/config.yml
      - /var/log:/var/log:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
    networks:
      - monitoring
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:
  alertmanager_data:
  loki_data:

networks:
  monitoring:
    driver: bridge
```

## 总结

本Agent与MCP实现指南提供了完整的解决方案，涵盖：

### 核心特性
1. **模块化架构**: 可扩展的Agent和MCP服务器框架
2. **异步处理**: 高性能的并发任务处理
3. **智能规划**: 多策略任务规划引擎
4. **记忆系统**: 多层次记忆管理
5. **工具集成**: 丰富的工具注册和调用机制

### 生产就绪功能
1. **容器化部署**: Docker和Kubernetes支持
2. **高可用配置**: 集群部署和负载均衡
3. **安全强化**: 多层安全防护
4. **监控告警**: 完整的监控和告警系统
5. **性能优化**: 缓存、连接池等优化策略

### 扩展能力
1. **分布式集群**: 多Agent协作
2. **MCP生态**: 多样化的MCP服务器实现
3. **API接口**: RESTful API服务
4. **调试工具**: 完整的调试和分析工具

通过这套解决方案，您可以构建强大、可扩展、生产级的Agent-MCP系统。

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"id": "1", "content": "Research agent and MCP implementation strategies", "status": "completed"}, {"id": "2", "content": "Design agent architecture and MCP integration", "status": "completed"}, {"id": "3", "content": "Write comprehensive implementation guide", "status": "completed"}, {"id": "4", "content": "Create example code and deployment scripts", "status": "in_progress"}, {"id": "5", "content": "Commit and push the documentation", "status": "pending"}]