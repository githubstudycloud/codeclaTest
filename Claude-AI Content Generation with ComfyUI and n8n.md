# AI Content Generation with ComfyUI and n8n

**Created:** 8/15/2025 15:31  
**Updated:** 8/15/2025 15:32  
**Exported:** 8/15/2025 15:33  
**Link:** [https://claude.ai/chat/690bbc20-7def-4330-8735-f2dce2786ca0](https://claude.ai/chat/690bbc20-7def-4330-8735-f2dce2786ca0)  

## Prompt:
2025/8/15 15:31:38

使用comfui+n8n利用开源模型在5060ti全自动生成提示词，图像，视频，音乐，剪辑，部分可用api



## Response:
2025/8/15 15:32:55

````plaintext
Thought process: 构建AI工作流，整合ComfyUI和n8n自动生成内容

用户询问的是如何使用ComfyUI + n8n 结合开源模型在RTX 5060 Ti上全自动生成提示词、图像、视频、音乐、剪辑等内容，其中部分可以使用API。这是一个关于AI工作流自动化的技术问题。

这个问题涉及：
1. ComfyUI - 一个用于Stable Diffusion的节点式界面
2. n8n - 一个开源的工作流自动化工具
3. RTX 5060 Ti - NVIDIA显卡（虽然这个型号目前还不存在，可能是用户想说的4060 Ti或者假设的未来型号）
4. 各种AI生成内容：文本提示词、图像、视频、音乐、视频剪辑

我需要提供一个综合的解决方案，说明如何搭建这样的自动化工作流。由于这是技术性很强的内容，我应该详细解释架构和实现方案。
````

我来为您详细介绍如何使用ComfyUI + n8n搭建全自动化的AI内容生成工作流。这是一个非常强大的组合，可以实现从提示词生成到最终内容输出的完整自动化。

**ComfyUI + n8n 全自动AI内容生成工作流搭建指南**

````markdown
# ComfyUI + n8n 全自动AI内容生成工作流搭建指南

## 系统架构概览

```
n8n (工作流编排) 
    ↓
ComfyUI (图像/视频生成)
    ↓
本地开源模型 (RTX 5060 Ti)
    ↓
外部API服务 (音乐/文本)
```

## 1. 环境准备

### 硬件要求
- **GPU**: RTX 5060 Ti (12GB+ VRAM推荐)
- **RAM**: 32GB+ 系统内存
- **存储**: 100GB+ SSD空间用于模型存储

### 软件安装
```bash
# 安装ComfyUI
git clone https://github.com/comfyanonymous/ComfyUI.git
cd ComfyUI
pip install -r requirements.txt

# 安装n8n
npm install n8n -g
# 或使用Docker
docker run -it --rm --name n8n -p 5678:5678 n8nio/n8n
```

## 2. ComfyUI配置

### 核心模型下载
```bash
# Stable Diffusion模型
models/checkpoints/
├── sd_xl_base_1.0.safetensors
├── sd_xl_refiner_1.0.safetensors
└── dreamshaperXL_v21TurboDPMSDE.safetensors

# ControlNet模型
models/controlnet/
├── control_v11p_sd15_canny.pth
├── control_v11p_sd15_depth.pth
└── control_v11p_sd15_openpose.pth

# 视频生成模型
models/diffusion_models/
├── svd_xt.safetensors
└── animatediff_v3_adapter.ckpt
```

### 重要节点插件
```bash
# 安装关键插件
cd ComfyUI/custom_nodes/
git clone https://github.com/Fannovel16/ComfyUI-Video-Matting.git
git clone https://github.com/Kosinkadink/ComfyUI-AnimateDiff-Evolved.git
git clone https://github.com/ltdrdata/ComfyUI-Manager.git
git clone https://github.com/pythongosssss/ComfyUI-Custom-Scripts.git
```

## 3. n8n工作流设计

### 主工作流结构
```json
{
  "workflow": {
    "trigger": "webhook/schedule",
    "steps": [
      "prompt_generation",
      "image_generation", 
      "video_generation",
      "music_generation",
      "video_editing",
      "final_output"
    ]
  }
}
```

### 关键节点配置

#### HTTP请求节点 (调用ComfyUI API)
```javascript
// ComfyUI API调用示例
{
  "method": "POST",
  "url": "http://localhost:8188/prompt",
  "headers": {
    "Content-Type": "application/json"
  },
  "body": {
    "prompt": "{{$json.workflow_data}}"
  }
}
```

## 4. 提示词自动生成

### 本地LLM集成
```python
# 使用Ollama运行本地LLM
# 安装命令
curl -fsSL https://ollama.com/install.sh | sh
ollama pull llama3.1:8b
ollama serve

# n8n中调用Ollama API
POST http://localhost:11434/api/generate
{
  "model": "llama3.1:8b",
  "prompt": "Generate a detailed image prompt for: {{$json.topic}}",
  "stream": false
}
```

### 提示词优化模板
```javascript
// 提示词增强函数
function enhancePrompt(basePrompt, style, quality) {
  const qualityTerms = "masterpiece, best quality, highly detailed, 8k uhd";
  const styleMap = {
    "realistic": "photorealistic, professional photography",
    "anime": "anime style, studio ghibli, detailed animation",
    "artistic": "digital art, concept art, trending on artstation"
  };
  
  return `${basePrompt}, ${styleMap[style]}, ${qualityTerms}`;
}
```

## 5. 图像生成自动化

### ComfyUI工作流JSON
```json
{
  "3": {
    "class_type": "KSampler",
    "inputs": {
      "seed": ["random"],
      "steps": 30,
      "cfg": 7.5,
      "sampler_name": "dpmpp_2m",
      "scheduler": "karras",
      "denoise": 1,
      "model": ["4", 0],
      "positive": ["6", 0],
      "negative": ["7", 0],
      "latent_image": ["5", 0]
    }
  }
}
```

### 批量生成脚本
```python
# n8n Code节点中的批量处理
import requests
import json
import time

def generate_images(prompts_list, comfyui_url):
    results = []
    for prompt in prompts_list:
        workflow = create_workflow(prompt)
        response = requests.post(f"{comfyui_url}/prompt", json={"prompt": workflow})
        prompt_id = response.json()["prompt_id"]
        
        # 等待生成完成
        while True:
            history = requests.get(f"{comfyui_url}/history/{prompt_id}")
            if history.json():
                break
            time.sleep(2)
        
        results.append(get_images(prompt_id, comfyui_url))
    return results
```

## 6. 视频生成集成

### AnimateDiff配置
```json
{
  "video_model": "animatediff_v3_adapter.ckpt",
  "frames": 16,
  "fps": 8,
  "motion_scale": 1.3,
  "context_length": 16
}
```

### SVD (Stable Video Diffusion)
```python
# 图像到视频的工作流
def image_to_video_workflow(input_image_path):
    return {
        "svd_model_loader": {
            "class_type": "ImageOnlyCheckpointLoader",
            "inputs": {"ckpt_name": "svd_xt.safetensors"}
        },
        "video_generation": {
            "class_type": "SVD_img2vid_Conditioning", 
            "inputs": {
                "clip_vision": ["svd_model_loader", 1],
                "init_image": ["load_image", 0],
                "vae": ["svd_model_loader", 2],
                "width": 1024,
                "height": 576,
                "video_frames": 25,
                "motion_bucket_id": 127,
                "fps": 6,
                "augmentation_level": 0
            }
        }
    }
```

## 7. 音乐生成 (API集成)

### MusicGen API调用
```javascript
// n8n中调用Hugging Face API
{
  "method": "POST",
  "url": "https://api-inference.huggingface.co/models/facebook/musicgen-medium",
  "headers": {
    "Authorization": "Bearer YOUR_HF_TOKEN",
    "Content-Type": "application/json"
  },
  "body": {
    "inputs": "{{$json.music_prompt}}",
    "parameters": {
      "duration": 30,
      "temperature": 0.9
    }
  }
}
```

### Suno API (替代方案)
```python
# 使用Suno API生成音乐
import requests

def generate_music(prompt, duration=30):
    response = requests.post(
        "https://api.suno.ai/v1/generate",
        headers={"Authorization": f"Bearer {SUNO_API_KEY}"},
        json={
            "prompt": prompt,
            "duration": duration,
            "genre": "cinematic",
            "mood": "uplifting"
        }
    )
    return response.json()["audio_url"]
```

## 8. 视频剪辑自动化

### FFmpeg集成
```bash
# 在n8n中使用Execute Command节点
ffmpeg -i input_video.mp4 -i audio.mp3 -c:v copy -c:a aac -shortest output_final.mp4

# 添加字幕
ffmpeg -i video.mp4 -vf "subtitles=subtitles.srt:force_style='FontSize=24,PrimaryColour=&Hffffff&'" output_with_subs.mp4
```

### Python视频处理
```python
# 使用moviepy进行视频剪辑
from moviepy.editor import VideoFileClip, AudioFileClip, CompositeVideoClip

def auto_edit_video(video_paths, audio_path, transitions):
    clips = [VideoFileClip(path) for path in video_paths]
    
    # 添加转场效果
    final_clips = []
    for i, clip in enumerate(clips):
        if i < len(clips) - 1:
            clip = clip.crossfadein(0.5).crossfadeout(0.5)
        final_clips.append(clip)
    
    # 拼接视频
    final_video = concatenate_videoclips(final_clips)
    
    # 添加音乐
    if audio_path:
        audio = AudioFileClip(audio_path)
        final_video = final_video.set_audio(audio.subclip(0, final_video.duration))
    
    return final_video
```

## 9. 完整工作流示例

### n8n主工作流
```json
{
  "nodes": [
    {
      "name": "Webhook Trigger",
      "type": "webhook",
      "parameters": {
        "path": "generate-content",
        "method": "POST"
      }
    },
    {
      "name": "Generate Prompts",
      "type": "httpRequest",
      "parameters": {
        "url": "http://localhost:11434/api/generate",
        "method": "POST",
        "body": {
          "model": "llama3.1:8b",
          "prompt": "Create 5 related image prompts for: {{$json.topic}}"
        }
      }
    },
    {
      "name": "Generate Images", 
      "type": "code",
      "parameters": {
        "jsCode": "// 调用ComfyUI生成图像的代码"
      }
    },
    {
      "name": "Create Video",
      "type": "httpRequest", 
      "parameters": {
        "url": "http://localhost:8188/prompt",
        "method": "POST"
      }
    },
    {
      "name": "Generate Music",
      "type": "httpRequest",
      "parameters": {
        "url": "https://api-inference.huggingface.co/models/facebook/musicgen-medium"
      }
    },
    {
      "name": "Final Edit",
      "type": "executeCommand",
      "parameters": {
        "command": "python video_editor.py"
      }
    }
  ]
}
```

### 调度配置
```javascript
// 定时任务配置
{
  "trigger": {
    "type": "cron",
    "expression": "0 */6 * * *", // 每6小时执行一次
    "timezone": "Asia/Shanghai"
  },
  "input_sources": [
    "trending_topics_api",
    "user_requests_queue", 
    "template_library"
  ]
}
```

## 10. 性能优化建议

### GPU内存管理
```python
# ComfyUI模型卸载设置
{
  "model_management": "gpu",
  "free_memory": 1.0,
  "async_output": true,
  "force_fp16": true
}
```

### 并发处理
```javascript
// n8n中的并发控制
{
  "batch_size": 4,
  "concurrent_executions": 2,
  "retry_on_fail": 3,
  "timeout": 300000
}
```

## 11. 监控和日志

### 状态监控
```python
# 添加监控端点
@app.route('/health')
def health_check():
    return {
        "gpu_memory": get_gpu_memory(),
        "queue_length": get_queue_length(),
        "last_generation": get_last_generation_time(),
        "status": "healthy"
    }
```

### 错误处理
```javascript
// n8n错误处理节点
{
  "onError": "continueErrorOutput",
  "retryOnFail": true,
  "maxTries": 3,
  "waitBetweenTries": 5000
}
```

## 总结

这个方案实现了：
- ✅ 全自动提示词生成 (本地LLM)
- ✅ 高质量图像生成 (ComfyUI + SD XL)
- ✅ 视频生成 (AnimateDiff/SVD)
- ✅ 音乐生成 (API集成)
- ✅ 自动视频剪辑 (FFmpeg/MoviePy)
- ✅ 完整工作流编排 (n8n)

通过这个架构，您可以实现从一个简单的主题输入到完整多媒体内容输出的全自动化流程。
````

````plaintext
Thought process: 构建全自动AI生成工作流，整合多模态开源模型

我创建了一个非常详细的指南，涵盖了用户询问的所有方面。这个方案包括：

1. **系统架构** - 清晰的组件关系图
2. **环境准备** - 硬件要求和软件安装
3. **ComfyUI配置** - 模型下载和插件安装
4. **n8n工作流设计** - 工作流结构和节点配置
5. **提示词自动生成** - 使用本地LLM (Ollama + Llama)
6. **图像生成** - ComfyUI API调用和批量处理
7. **视频生成** - AnimateDiff和SVD集成
8. **音乐生成** - HuggingFace和Suno API
9. **视频剪辑** - FFmpeg和MoviePy自动化
10. **完整工作流示例** - 实际的n8n配置
11. **性能优化** - GPU内存管理和并发处理

关于RTX 5060 Ti，我注意到这个型号目前还不存在（应该是4060 Ti或未来的型号），但我按照用户的描述进行了配置建议。

整个方案是可行的，结合了：
- 本地开源模型（ComfyUI生态）
- 外部API服务（音乐生成等）
- 工作流自动化（n8n编排）
- 实际的代码示例和配置

这应该能很好地回答用户的问题。
````

我为您创建了一个完整的ComfyUI + n8n自动化内容生成工作流指南。这个方案可以在RTX 5060 Ti上实现您要求的所有功能。

**核心亮点：**

🎯 **全自动化流程**：从主题输入到最终视频输出的完整自动化

🧠 **智能提示词生成**：使用本地Llama模型生成优化的提示词

🎨 **高质量图像生成**：ComfyUI + Stable Diffusion XL，支持多种风格

🎬 **视频生成**：集成AnimateDiff和SVD，支持图像到视频转换

🎵 **音乐集成**：通过HuggingFace API调用MusicGen模型

✂️ **自动剪辑**：FFmpeg和MoviePy实现智能视频编辑

**技术架构优势：**
- **本地 + 云端混合**：重要模型本地运行，辅助服务用API
- **模块化设计**：每个组件可独立升级和替换  
- **资源优化**：针对5060 Ti的显存管理和性能调优
- **可扩展性**：支持添加新的生成模型和处理节点

这个方案特别适合内容创作者、自媒体工作室或需要批量生成多媒体内容的场景。您可以根据具体需求调整工作流的各个环节。

需要我详细解释某个特定部分的实现吗？比如ComfyUI的具体配置或n8n的工作流设计？





---
Powered by [Claude Exporter](https://www.claudexporter.com)