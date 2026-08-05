---
title: "Diffusion"
slug: "diffusion-model"
date: "2026-08-01"
updated: "2026-08-01"
category: "Math & Coding"
tags: "技术笔记"
summary: "Diffusion原理及相关实践。"
cover: "/legacy-assets/assets/images/background2.png"
top: true
published: true
---
# Conditional Flow Matching（CFM）原理详解

> Conditional Flow Matching（条件流匹配）的核心目标是：在给定条件 \(c\) 时，学习一个随生成时间变化的速度场，把简单噪声分布中的样本连续运输为目标数据分布中的样本。

在具身智能中，条件 \(c\) 可以包含第一人称图像、BEV、语言指令、导航目标、机器人状态和历史观测；生成目标可以是未来轨迹、动作块或控制序列。

## 1. 为什么需要 CFM？

设机器人需要根据条件 \(c\) 生成未来轨迹：

$$
\tau=[a_1,a_2,\ldots,a_H].
$$

如果直接使用回归模型：

$$
\hat\tau=f_\theta(c),
$$

并使用均方误差训练，模型往往倾向于预测条件均值。然而，同一场景可能存在多条合理轨迹，例如从行人左侧或右侧绕行。直接回归可能把两种模式平均成一条穿过障碍物的轨迹。

CFM 学习的是完整的条件分布：

$$
p_{\mathrm{data}}(\tau\mid c),
$$

因而具有表达多模态行为的能力。

## 2. 从噪声到数据的连续流

定义简单的起点分布：

$$
x_0\sim p_0(x)=\mathcal N(0,I),
$$

以及目标数据：

$$
x_1\sim p_{\mathrm{data}}(x\mid c).
$$

CFM 构造一条连续路径：

$$
x_0\rightarrow x_t\rightarrow x_1,\qquad t\in[0,1].
$$

这里的 \(t\) 是噪声向数据演化的“生成时间”，不是机器人在物理世界中的运动时间。

模型学习条件速度场：

$$
v_\theta(x_t,t,c),
$$

并定义常微分方程：

$$
\frac{dx_t}{dt}=v_\theta(x_t,t,c).
$$

推理时，从随机噪声出发对该 ODE 积分：

$$
x_1=x_0+\int_0^1v_\theta(x_t,t,c)\,dt.
$$

因此，CFM 本质上是在学习一个条件神经 ODE 的速度场。

## 3. Flow Matching 与 Conditional Flow Matching

概率密度在速度场中的演化满足连续性方程：

$$
\frac{\partial p_t(x)}{\partial t}
+\nabla_x\cdot\bigl(p_t(x)v_t(x)\bigr)=0.
$$

理想的 Flow Matching 目标是：

$$
\mathcal L_{\mathrm{FM}}
=\mathbb E\left[\left\|v_\theta(x_t,t)-v_t(x_t)\right\|_2^2\right].
$$

难点在于真实边缘速度场 \(v_t(x)\) 通常未知。CFM 转而给定端点 \((x_0,x_1)\)，人为定义容易采样的条件概率路径，并使用可解析计算的条件速度 \(u_t\) 监督模型：

$$
\mathcal L_{\mathrm{CFM}}
=\mathbb E\left[
\left\|v_\theta(x_t,t,c)-u_t(x_t\mid x_0,x_1)\right\|_2^2
\right].
$$

需要区分两种“条件”：

1. 数学上的路径条件：端点 \(x_0,x_1\)；
2. 任务上的外部条件：图像、指令、目标等 \(c\)。

具身任务中的条件生成模型通常同时包含两者。

## 4. 最常用的线性插值路径

定义：

$$
x_t=(1-t)x_0+t x_1.
$$

对生成时间求导：

$$
u_t=\frac{dx_t}{dt}=x_1-x_0.
$$

因此基本训练目标为：

$$
\boxed{
\mathcal L_{\mathrm{CFM}}
=\mathbb E\left[
\left\|v_\theta(x_t,t,c)-(x_1-x_0)\right\|_2^2
\right]
}.
$$

单个训练样本的构造过程是：

1. 采样真实数据 \(x_1\sim p_{\mathrm{data}}(x\mid c)\)；
2. 采样高斯噪声 \(x_0\sim\mathcal N(0,I)\)；
3. 采样生成时间 \(t\sim U(0,1)\)；
4. 构造中间状态 \(x_t=(1-t)x_0+t x_1\)；
5. 构造目标速度 \(u_t=x_1-x_0\)；
6. 预测 \(\hat u_t=v_\theta(x_t,t,c)\)；
7. 最小化 \(\|\hat u_t-u_t\|_2^2\)。

训练时不需要完整求解 ODE，只需在随机时刻监督局部速度。

## 5. 训练与推理的区别

### 训练阶段

训练时已知真实终点 \(x_1\)，因此可以直接构造中间状态和目标速度：

$$
x_t=(1-t)x_0+t x_1,\qquad u_t=x_1-x_0.
$$

### 推理阶段

推理时没有真实终点，只有条件 \(c\)。先采样噪声：

$$
\hat x_0\sim\mathcal N(0,I),
$$

再数值求解：

$$
\frac{d\hat x_t}{dt}=v_\theta(\hat x_t,t,c).
$$

最简单的 Euler 更新为：

$$
\hat x_{t+\Delta t}=\hat x_t+\Delta t\,v_\theta(\hat x_t,t,c).
$$

```python
x = torch.randn(batch_size, horizon, action_dim)

for i in range(num_steps):
    t = torch.full((batch_size,), i / num_steps, device=x.device)
    velocity = model(x, t, condition)
    x = x + velocity / num_steps

trajectory = x
```

实际系统也可以使用 Midpoint、Heun 或 RK4 等求解器。

## 6. 为什么它可以表达多模态轨迹？

若同一条件下存在左绕与右绕两种策略：

$$
p(\tau\mid c)
=0.5p_{\mathrm{left}}(\tau\mid c)
+0.5p_{\mathrm{right}}(\tau\mid c),
$$

普通 MSE 回归容易预测二者的平均值。CFM 则可以把不同初始噪声运输到不同轨迹模式：

$$
x_0^{(1)}\rightarrow\tau_{\mathrm{left}},\qquad
x_0^{(2)}\rightarrow\tau_{\mathrm{right}}.
$$

不过，CFM“能够表达”多模态分布，不代表训练后一定自动获得理想多样性；数据覆盖、噪声与数据的耦合、条件是否充分、网络容量和采样策略都会影响结果。

## 7. CFM 与 Diffusion 的关系

| 方法 | 主要学习对象 | 推理过程 |
|---|---|---|
| Diffusion | score、噪声或去噪目标 | 反向 SDE/ODE |
| Flow Matching | 边缘概率路径的速度场 | ODE |
| Conditional Flow Matching | 可构造条件路径上的速度 | 条件 ODE |

直观上：

- Diffusion 学习如何逐步去除噪声；
- CFM 学习样本在每个位置和时刻应该沿哪个方向流动。

CFM 的训练目标直接，适合连续动作块和轨迹生成，也可能通过更直的运输路径降低推理步数；但普通 CFM 并不天然等于一步生成，推理通常仍需若干次网络前向。

## 8. CFM 中的 velocity 不是机器人物理速度

若生成变量是轨迹张量：

$$
x_t\in\mathbb R^{H\times D},
$$

则：

$$
v_\theta(x_t,t,c)\in\mathbb R^{H\times D}
$$

表示整个轨迹张量在生成空间中的变化方向，不等于机器人当前的线速度和角速度 \((v,\omega)\)。

只有当数据终点本身被定义为控制序列：

$$
x_1=[(v_1,\omega_1),\ldots,(v_H,\omega_H)],
$$

CFM 最终生成的才是直接控制指令。更常见的工程链路是：

$$
\text{CFM 生成轨迹}
\rightarrow
\text{轨迹跟踪器}
\rightarrow
(v,\omega).
$$

## 9. 轨迹 CFM 中存在两种时间

| 时间 | 记号 | 含义 |
|---|---|---|
| 生成时间 | \(t\in[0,1]\) | 噪声向数据演化的进度 |
| 轨迹时间 | \(h=1,\ldots,H\) | 机器人未来第几个轨迹点 |

因此，速度场在每一个生成时刻 \(t\) 都会更新整段未来轨迹，而不是只更新一个物理时间点。

## 10. 在 UrbanNav/SideWalkNav 双系统中的作用

System 2 负责语义推理：

$$
z^{\mathrm{reason}}
=f_{\mathrm{LVLM}}(I^{\mathrm{FPV}},I^{\mathrm{BEV}},l,g).
$$

它回答：

- 场景中有什么；
- 哪些区域可通行；
- 是否需要减速或礼让；
- 应该从哪一侧绕行；
- 当前应前进、停止还是重规划。

System 1 的 CFM 根据这些条件生成连续轨迹：

$$
\hat\tau\sim p_\theta
\left(
\tau\mid
z^{\mathrm{obs}},
z^{\mathrm{reason}},
z^{\mathrm{goal}},
s_{\mathrm{robot}}
\right).
$$

对应速度场为：

$$
v_\theta
\left(
\tau_t,t,
z^{\mathrm{obs}},
z^{\mathrm{reason}},
z^{\mathrm{goal}}
\right).
$$

两者的职责可概括为：

> System 2 决定“应该怎么做”；System 1 把该决策转化为“具体怎样运动”。

## 11. 完整轨迹训练公式

对第 \(i\) 个训练样本，设真实专家轨迹为 \(\tau_i\)，条件为 \(c_i\)：

$$
\epsilon_i\sim\mathcal N(0,I),\qquad t_i\sim U(0,1),
$$

$$
\tau_{t_i}=(1-t_i)\epsilon_i+t_i\tau_i,
$$

$$
u_i=\tau_i-\epsilon_i,
$$

$$
\hat u_i=v_\theta(\tau_{t_i},t_i,c_i).
$$

批量损失为：

$$
\boxed{
\mathcal L_{\mathrm{CFM}}
=\frac{1}{B}\sum_{i=1}^{B}
\left\|
v_\theta(\tau_{t_i},t_i,c_i)
-(\tau_i-\epsilon_i)
\right\|_2^2
}.
$$

若状态包含位置、朝向和停止变量，可以对各维度进行尺度归一化或设置权重。但从严格的 Flow Matching 角度看，主目标仍应是完整状态空间上的速度匹配；额外位置、平滑、碰撞或终点损失是否必要，应通过表示设计和消融实验判断。

## 12. 核心结论

CFM 可以压缩成四步：

1. 从简单噪声分布采样 \(x_0\)；
2. 用人工定义的概率路径连接 \(x_0\) 与真实数据 \(x_1\)；
3. 训练网络预测路径上的瞬时速度；
4. 推理时从噪声出发，积分速度场获得符合条件的轨迹或动作。

其本质不是直接预测机器人下一步，而是：

> 学习一个由视觉、语言、目标和推理状态共同控制的概率运输过程，将随机噪声连续变换为满足当前场景约束的动作或轨迹分布。
>

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