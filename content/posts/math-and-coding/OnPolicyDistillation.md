---
title: "OPD"
slug: "On Policy Distillation"
date: "2026-08-01"
updated: "2026-08-01"
category: "Math & Coding"
tags: "技术笔记"
summary: "OPD原理及相关实践。"
cover: "/legacy-assets/assets/images/background2.png"
top: true
published: true
---

A-M0.5
t / timesteps	Flow Matching 的噪声时间，表示当前有多“脏”
frame_st_id	当前 chunk 在整个 episode 轨迹中的绝对帧位置
action 的 H 维	每个视频帧内部对应的多个动作 sub-step

首个chunk到后续chunk的完整推理过程

1. reset episode
   frame_st_id = 0
   编码任务 prompt
   清空 KV cache

2. 第一次 infer
   当前真实图像 → VAE → init_latent
   video_cond = 当前真实图像 latent
   action_cond = 归一化空间零动作
   生成其余未来视频和动作

3. 客户端执行
   跳过第 0 个条件动作帧
   执行动作帧 1、2、3

4. 回填真实状态
   实际相机关键帧 + 实际执行动作
     → _build_transformer_input
     → video KV cache
     → action KV cache

5. frame_st_id 前进
   后续预测通过 KV cache 读取历史
   因此 cond=None，不再额外插入第一条件帧