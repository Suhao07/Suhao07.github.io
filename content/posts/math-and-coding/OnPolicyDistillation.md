---
title: "OPD"
slug: "on-policy-distillation"
date: "2026-08-01"
updated: "2026-08-01"
category: "Math & Coding"
tags: "技术笔记"
summary: "OPD原理及相关实践。"
cover: "/legacy-assets/assets/images/background2.png"
top: true
published: true
------

# 0. 一句话定义

On-Policy Distillation（OPD）的核心是：

$$
\boxed{
\text{让 Teacher 在 Student 当前策略自己产生的状态上提供监督。}
}
$$

在自回归模型中，这个状态通常是 Student 自己生成的 token prefix；在连续生成策略中，它可以是 Student 自己访问的 diffusion / flow state；在真正闭环的具身系统中，它还可以是 Student 执行动作后进入的环境状态。

OPD 不是简单的“换一个 KL Loss”，而是把蒸馏目标从：

$$
\text{Teacher / GT 状态}
$$

移动到：

$$
\text{Student 当前策略访问的状态}
$$

---

# 1. 为什么需要 OPD

## 1.1 监督学习的基本设定

给定输入：

$$
x\sim\mathcal D
$$

Teacher Policy：

$$
q(a\mid s)
$$

Student Policy：

$$
\pi_\theta(a\mid s)
$$

其中 $s$ 表示当前决策状态。

对于自回归模型：

$$
s_i=(x,y_{<i})
$$

对于机器人控制：

$$
s_t=(o_t,h_t)
$$

其中 $o_t$ 是当前观测，$h_t$ 是历史或内部状态。

知识蒸馏希望 Student 在 Teacher 认为合理的动作上赋予较高概率：

$$
\pi_\theta(\cdot\mid s)
\approx
q(\cdot\mid s)
$$

真正关键的问题不是只有“如何比较两个分布”，还包括：

> **在哪些状态 $s$ 上比较？**

---

## 1.2 Off-policy SFT / KD 使用什么状态

普通 SFT 使用 Ground Truth prefix：

$$
s_i^{GT}
=
(x,y_{<i}^{GT})
$$

普通离线 KD 可能使用 Teacher prefix：

$$
s_i^T
=
(x,y_{<i}^{T})
$$

统一记为外部行为策略 $\mu$ 产生的状态：

$$
s\sim d_\mu
$$

离线蒸馏目标是：

$$
\boxed{
\mathcal L_{\mathrm{off}}
=
\mathbb E_{s\sim d_\mu}
\left[
D
\left(
q(\cdot\mid s),
\pi_\theta(\cdot\mid s)
\right)
\right]
}
$$

其中：

- $d_\mu$：GT、Teacher 或固定 replay data 诱导的状态分布；
- $D$：KL、JSD 或其他分布距离。

---

## 1.3 推理时 Student 访问什么状态

部署时，Student 不会一直得到正确 prefix。

Student 自己采样：

$$
y_i^S
\sim
\pi_\theta
\left(
\cdot
\mid
x,y_{<i}^S
\right)
$$

因此实际访问的状态为：

$$
s_i^S
=
(x,y_{<i}^S)
$$

状态分布是：

$$
s\sim d_{\pi_\theta}
$$

通常：

$$
\boxed{
d_\mu
\neq
d_{\pi_\theta}
}
$$

这就是 training–inference distribution mismatch。

---

# 2. Exposure Bias 与错误累积

## 2.1 单步误差为什么会积累

假设 Student 在正确状态上的单步错误概率为：

$$
\epsilon
$$

若每一步都独立且始终处于正确状态，长度为 $T$ 的序列完全正确的概率近似为：

$$
(1-\epsilon)^T
$$

至少出现一次错误的概率为：

$$
1-(1-\epsilon)^T
$$

当 $\epsilon$ 较小时：

$$
1-(1-\epsilon)^T
\approx
T\epsilon
$$

但真实情况比这个估计更严重，因为一旦 Student 生成错误 token：

$$
y_i^S\neq y_i^{GT}
$$

后续状态变成：

$$
(x,y_{<i}^{GT},y_i^S)
$$

这类状态在 SFT 中可能从未出现。

后续误差率不再是原来的 $\epsilon$，而可能变成：

$$
\epsilon_{\mathrm{OOD}}
>
\epsilon
$$

于是形成：

```text
小错误
→ Student 进入训练外 prefix
→ 后续预测质量下降
→ 错误继续放大
```

---

## 2.2 机器人策略中的对应问题

Behavior Cloning 在专家状态上训练：

$$
s_t\sim d_{\pi_E}
$$

部署时 Student 执行自身动作：

$$
a_t^S\sim\pi_\theta(\cdot\mid s_t)
$$

环境转移：

$$
s_{t+1}
\sim
P(\cdot\mid s_t,a_t^S)
$$

Student 的一个小动作误差会改变下一观测，使策略进入：

$$
s_{t+1}\sim d_{\pi_\theta}
$$

而不是专家分布：

$$
s_{t+1}\sim d_{\pi_E}
$$

这就是 imitation learning 中的 covariate shift。

OPD 与 DAgger 的共同思想是：

> 不只在专家访问的状态上训练，还要在 Student 自己会访问的状态上纠正 Student。

区别在于：

- DAgger 通常需要环境 rollout 和专家动作标注；
- token-prefix OPD 可以只用固定数据输入，在生成空间内产生新的 Student states；
- flow-state OPD 可以在固定观测下，让生成器访问自身的连续生成状态。

---

# 3. OPD 的正式目标

## 3.1 Student-induced state distribution

Student 当前策略为：

$$
\pi_\theta
$$

由它产生状态分布：

$$
d_{\pi_\theta}(s)
$$

理想 OPD 目标：

$$
\boxed{
\mathcal J_{\mathrm{OPD}}(\theta)
=
\mathbb E_{s\sim d_{\pi_\theta}}
\left[
D
\left(
\pi_\theta(\cdot\mid s),
q(\cdot\mid s)
\right)
\right]
}
$$

这与 off-policy KD 的关键差别只有一处：

$$
d_\mu
\longrightarrow
d_{\pi_\theta}
$$

但这处变化改变了整个学习问题。

---

## 3.2 自回归序列形式

输入：

$$
x\sim\mathcal D
$$

Student rollout：

$$
y^S
=
(y_1^S,\ldots,y_L^S)
\sim
\pi_\theta(\cdot\mid x)
$$

在第 $i$ 个位置：

$$
P_{S,i}
=
\pi_\theta
\left(
\cdot
\mid
x,y_{<i}^S
\right)
$$

$$
P_{T,i}
=
q
\left(
\cdot
\mid
x,y_{<i}^S
\right)
$$

OPD：

$$
\boxed{
\mathcal L_{\mathrm{OPD}}
=
\mathbb E_{
x\sim\mathcal D,\,
y^S\sim\pi_\theta(\cdot\mid x)
}
\left[
\frac{1}{L}
\sum_{i=1}^{L}
D(P_{S,i},P_{T,i})
\right]
}
$$

Teacher 的作用不是重新生成一条完整答案，而是回答：

> 在 Student 已经生成了这个 prefix 的条件下，下一步怎样分配概率更合理？

---

## 3.3 为什么这是 “On-policy”

强化学习中的 on-policy 通常指：

$$
s,a
\sim
\pi_\theta
$$

训练数据由当前策略产生。

OPD 中同样如此：

$$
y_{<i}^S
\sim
\pi_\theta
$$

Teacher 虽然提供监督，但监督发生的位置由 Student 决定。

因此：

$$
\boxed{
\text{On-policy 描述的是 state distribution 的来源，不是 Teacher 的来源。}
}
$$

Teacher 可以是：

```text
更大模型
同一模型的 privileged version
专家策略
旧 checkpoint
Flow Matching Teacher
由 reward 构造的 proximal teacher
```

只要监督状态来自当前 Student，仍属于 OPD。

---

# 4. 实际训练为什么使用 Stop-gradient

严格地说：

$$
\mathcal J_{\mathrm{OPD}}(\theta)
=
\mathbb E_{s\sim d_{\pi_\theta}}
[
\ell_\theta(s)
]
$$

对 $\theta$ 求导时包含两部分：

$$
\nabla_\theta\mathcal J
=
\underbrace{
\mathbb E_{s\sim d_{\pi_\theta}}
[
\nabla_\theta\ell_\theta(s)
]
}_{\text{在当前状态上的监督梯度}}
+
\underbrace{
\nabla_\theta d_{\pi_\theta}(s)
\cdot
\ell_\theta(s)
}_{\text{状态分布对参数的梯度}}
$$

第二项需要对离散采样或完整环境 rollout 求策略梯度，计算昂贵且方差较高。

多数实际 OPD 采用 semi-gradient：

1. 使用当前 Student 生成 rollout；
2. 将 rollout state 视为固定样本；
3. 对 Teacher–Student 分布差异反向传播；
4. 不通过生成动作本身反向传播。

即：

$$
\boxed{
s^S
=
\operatorname{stopgrad}
\left(
\operatorname{Rollout}(\pi_\theta)
\right)
}
$$

然后：

$$
\nabla_\theta
D
\left(
\pi_\theta(\cdot\mid s^S),
q(\cdot\mid s^S)
\right)
$$

它类似迭代式 policy improvement：

```text
用当前策略收集状态
→ 在这些状态上执行监督更新
→ 得到新策略
→ 再收集新状态
```

---

# 5. KL、JSD 与 Hard Target 的区别

OPD 是否有效，不只取决于状态分布，也取决于分布匹配目标。

## 5.1 Forward KL

定义：

$$
\boxed{
D_{\mathrm{KL}}
\left(
P_T\parallel P_S
\right)
=
\sum_a
P_T(a)
\log
\frac{P_T(a)}{P_S(a)}
}
$$

去除与 Student 无关的 Teacher entropy 后：

$$
D_{\mathrm{KL}}(P_T\parallel P_S)
=
-\sum_a
P_T(a)\log P_S(a)
+
\mathrm{const}
$$

因此等价于用 Teacher soft labels 做交叉熵。

特点：

```text
Mode-covering
倾向覆盖 Teacher 的所有概率质量
Teacher 分布过宽时可能提高 Student entropy
Teacher 的低概率噪声也可能进入 Student
```

---

## 5.2 Reverse KL

定义：

$$
\boxed{
D_{\mathrm{KL}}
\left(
P_S\parallel P_T
\right)
=
\sum_a
P_S(a)
\log
\frac{P_S(a)}{P_T(a)}
}
$$

展开：

$$
D_{\mathrm{KL}}(P_S\parallel P_T)
=
\mathbb E_{a\sim P_S}
[
\log P_S(a)-\log P_T(a)
]
$$

又因为：

$$
H(P_S)
=
-\mathbb E_{a\sim P_S}[\log P_S(a)]
$$

所以最小化 Reverse KL 等价于最大化：

$$
\boxed{
\mathbb E_{a\sim P_S}
[
\log P_T(a)
]
+
H(P_S)
}
$$

它可以理解为：

1. 提高 Student 对 Teacher 高概率动作的偏好；
2. 同时保留一定 Student entropy；
3. 避免直接退化为单一 Hard Label。

特点：

```text
Mode-seeking
弱化 Teacher 低概率模式
适合动作选择接近单一主意图的场景
Teacher 给接近零概率的动作会产生较大惩罚
```

---

## 5.3 Hard Cross-Entropy

Teacher 只提供：

$$
a_T^*
=
\arg\max_a P_T(a)
$$

损失：

$$
\boxed{
\mathcal L_{\mathrm{Hard}}
=
-\log P_S(a_T^*)
}
$$

优点：

```text
简单
便宜
无需保存完整 Teacher logits
```

缺点：

```text
丢失 Teacher 不确定性
多个合理动作被压缩成一个标签
容易过早 entropy collapse
```

---

## 5.4 Jensen–Shannon Divergence

定义：

$$
M
=
\frac{1}{2}(P_T+P_S)
$$

$$
\boxed{
\operatorname{JSD}(P_T,P_S)
=
\frac{1}{2}
D_{\mathrm{KL}}(P_T\parallel M)
+
\frac{1}{2}
D_{\mathrm{KL}}(P_S\parallel M)
}
$$

性质：

```text
对称
有界
支撑集不完全重叠时较稳定
比单向 KL 更温和
```

---

## 5.5 Temperature

Teacher / Student logits：

$$
z_T,\quad z_S
$$

温度分布：

$$
\widetilde P_T
=
\operatorname{Softmax}
\left(
\frac{z_T}{T}
\right)
$$

$$
\widetilde P_S
=
\operatorname{Softmax}
\left(
\frac{z_S}{T}
\right)
$$

当 $T>1$ 时，分布更平滑，可以暴露次优动作之间的相对关系。

常见蒸馏使用：

$$
T^2D(\widetilde P_T,\widetilde P_S)
$$

补偿温度带来的梯度尺度变化。

---

# 6. OPD 与 SFT、KD、RL、DAgger 的关系

| 方法 | 状态由谁产生 | 监督信号 | 是否需要环境 | 核心作用 |
|---|---|---|---:|---|
| SFT / BC | GT / Expert | Hard target | 否 | 学习基础行为 |
| Off-policy KD | GT / Teacher / Replay | Teacher distribution | 否 | 压缩 Teacher |
| Token-prefix OPD | 当前 Student | Teacher token distribution | 否 | 修正自生成 prefix |
| Flow-state OPD | 当前 Student generator | Teacher vector field | 否 | 修正自生成连续状态 |
| DAgger | Student environment rollout | Expert action | 是 | 修正 visited environment states |
| RL | Student rollout | Reward / Advantage | 通常是 | 优化长期回报 |
| Environment-state VLA-OPD | Student environment rollout | Dense Teacher distribution | 是 | 在策略诱导状态上密集纠正 |

## 6.1 OPD 为什么像 RL

Reverse-KL OPD：

$$
\min_\theta
D_{\mathrm{KL}}
\left(
\pi_\theta(\cdot\mid s)
\parallel
q(\cdot\mid s)
\right)
$$

等价于：

$$
\max_\theta
\mathbb E_{a\sim\pi_\theta}
[
\log q(a\mid s)
]
+
H(\pi_\theta(\cdot\mid s))
$$

可以将：

$$
r_T(s,a)
=
\log q(a\mid s)
$$

看成一个密集 Teacher reward。

## 6.2 OPD 为什么不能等同于 RL

RL 优化：

$$
J_{\mathrm{RL}}
=
\mathbb E_{\tau\sim\pi_\theta}
\left[
\sum_t\gamma^tr_t
\right]
$$

OPD 通常优化局部 Teacher matching，不自动解决长期 credit assignment，也不保证超越 Teacher。

---

# 7. 三种不同层级的 On-policy

## 7.1 Token-prefix On-policy

固定输入：

$$
x\sim\mathcal D
$$

Student 生成：

$$
y^S\sim\pi_\theta(\cdot\mid x)
$$

Teacher 在 $(x,y_{<i}^S)$ 上提供 next-token distribution。

不需要 simulator。

## 7.2 Generator-state On-policy

固定条件：

$$
c\sim\mathcal D
$$

Student diffusion / flow model 生成自己的中间状态：

$$
x_k^S
\sim
d_{\phi_S}^{gen}
$$

Teacher 在相同 $x_k^S$ 上提供 denoising、score 或 vector-field correction。

不需要环境转移。

## 7.3 Environment-state On-policy

Student 执行动作：

$$
a_t^S
\sim
\pi_\theta(\cdot\mid s_t)
$$

环境产生：

$$
s_{t+1}
\sim
P(\cdot\mid s_t,a_t^S)
$$

Teacher 在 Student 访问的真实环境状态上提供监督。

需要 simulator 或真实机器人。

## 7.4 术语边界

$$
\boxed{
\text{没有 simulator}
\neq
\text{不能做 OPD}
}
$$

但：

$$
\boxed{
\text{Dataset-only OPD}
\neq
\text{Environment-state OPD}
}
$$

---

# 8. Privileged Teacher 与 Context Distillation

## 8.1 Privileged-context Teacher

Teacher：

$$
P_T
=
q
\left(
\cdot
\mid
x,c^{priv},y_{<i}^S
\right)
$$

Student：

$$
P_S
=
\pi_\theta
\left(
\cdot
\mid
x,y_{<i}^S
\right)
$$

目标：

$$
\boxed{
\mathcal L_{\mathrm{PC\text{-}OPD}}
=
\mathbb E
\left[
D
\left(
P_S,
P_T
\right)
\right]
}
$$

## 8.2 什么特权信息可以迁移

可迁移 privilege 通常满足：

```text
降低训练时推理难度
揭示跨样本共享的行为规律
Student 输入中仍有足够线索恢复该规律
```

## 8.3 什么特权信息可能不可迁移

若 privilege 是实例独有且 Student 输入不可恢复，Student 只能学习：

$$
\pi_\theta(a\mid x)
\approx
\mathbb E_{c^{priv}\mid x}
[
q(a\mid x,c^{priv})
]
$$

不同 privilege 对应冲突动作时，这个平均策略可能模糊甚至错误。

因此：

$$
\boxed{
\text{Privileged Teacher 更强}
\not\Rightarrow
\text{监督一定可迁移}
}
$$

---

# 9. OPD 的核心作用

## 9.1 学习从自己的错误状态恢复

Off-policy SFT 告诉 Student：

> 在正确 prefix 后面应该输出什么。

OPD 告诉 Student：

> 当你已经输出了这个不完美 prefix 后，接下来怎样做更合理。

## 9.2 自动暴露当前错误分布

Student rollout 会不断暴露当前模型真正会犯的错误，因此训练分布会随 Student 能力变化。

## 9.3 提供比 Reward 更密集的反馈

Teacher distribution 在每一步提供完整动作偏好，而不是只有终局 success / failure。

## 9.4 不改变部署结构

Teacher、privileged prompt 和在线评分只在训练期使用，部署仍只加载 Student。

---

# 10. OPD 不能解决什么

## 10.1 不能创造不可观测信息

若两个样本对 Student 完全相同：

$$
x_1=x_2
$$

但 Teacher 因隐藏信息要求不同动作：

$$
a_1\neq a_2
$$

不存在确定性 Student 同时完美满足两者。

## 10.2 Dataset-only OPD 不修正环境状态偏移

固定 FPV 上生成新的文本 prefix，不会产生机器人错误执行后的新 FPV。

## 10.3 Teacher 可能在 Student prefix 上失效

严重异常或逻辑矛盾的 Student prefix 可能超出 Teacher 支持分布。

## 10.4 不能保证超越 Teacher

Teacher 系统性错误会被 Student 学习。

---

# 11. 常见失败模式

## 11.1 Teacher–Student support mismatch

$$
P_S(a)>0,
\qquad
P_T(a)\approx0
$$

Reverse KL 可能产生过大梯度。

## 11.2 Student prefix 过长

错误累积、Teacher OOD 与训练成本都会随 rollout 长度增加。

## 11.3 格式 token 稀释监督

JSON 标点和固定 key 可能比真正的行为 value token 更多。

## 11.4 Privilege Illusion

Teacher 很强，但 Student 输入无法恢复 Teacher 依赖的隐藏信息。

## 11.5 Catastrophic Forgetting

窄任务 OPD 可能破坏原有 reasoning、VQA 或格式能力。

## 11.6 Stale Rollout

旧 Student checkpoint 产生的 rollout 不再代表当前策略分布。

---

# 12. Selective OPD

一般形式：

$$
\boxed{
\mathcal L_{\mathrm{selective}}
=
\frac{
\sum_i
w_i
D(P_{S,i},P_{T,i})
}{
\sum_iw_i+\epsilon
}
}
$$

权重可能来自：

```text
Teacher confidence
Teacher entropy
Teacher–Student disagreement
Token type
Task importance
Teacher validity
```

第一性原理使用顺序：

```text
1. 先实现基础 OPD
2. 观察失败模式
3. 格式 token 稀释 → value-span mask
4. Teacher OOD → reliability filter
5. Easy token 主导 → entropy / disagreement weighting
```

---

# 13. OPD 在 E2E UrbanNav 中的最简实现

当前框架保持不变：

```text
Dynamic Map GUI + FPV + Instruction + Robot State
        ↓
Existing UrbanNav LVLM
        ├── FullCoT
        ├── Decision-only
        └── [TRAJ_0:15]
                    ↓
                CFM
                    ↓
         16-step Ego Trajectory
```

新增训练逻辑只有：

```text
Decision-prefix OPD
CFM Flow-path OPD
```

---

# 14. UrbanNav Decision-prefix OPD

## 14.1 原有 Decision

```json
{
  "selected_action_label": "left_20",
  "speed": "slow",
  "response": "proceed"
}
```

Student 生成：

$$
\hat D^S
\sim
\pi_S
\left(
\cdot
\mid
\mathcal O^S
\right)
$$

## 14.2 相同 Student prefix

$$
P_{S,i}
=
\pi_S
\left(
\cdot
\mid
\mathcal O^S,\hat d_{<i}^S
\right)
$$

$$
P_{T,i}
=
\pi_T
\left(
\cdot
\mid
\mathcal O^T,\hat d_{<i}^S
\right)
$$

若 Student 倾向 `straight`、Teacher 倾向 `left_20`，差异已经直接存在于 $P_{S,i}$ 与 $P_{T,i}$ 中，不需要 Route Head。

## 14.3 Value-span Mask

只保留：

```text
selected_action_label value
speed value
response value
```

$$
m_i^D
=
\mathbb 1
\left[
i\in\mathcal I_{\mathrm{DecisionValue}}
\right]
$$

$$
\boxed{
\mathcal L_{D\text{-}OPD}
=
q^T
\frac{
\sum_i
m_i^D
D_{\mathrm{KL}}
\left(
P_{S,i}\parallel P_{T,i}
\right)
}{
\sum_im_i^D+\epsilon
}
}
$$

## 14.4 多 token action label

若 `left_20` 被拆成多个 token，首版仍逐 token 蒸馏。

只有实验证明拆分不稳定时，才聚合候选字符串 log probability：

$$
s_M(c)
=
\sum_{\ell=1}^{|c|}
\log
P_M
\left(
c_\ell
\mid
\hat d_{<i}^S,c_{<\ell}
\right)
$$

$$
P_M(c)
=
\operatorname{Softmax}_{c\in\mathcal C}
\left(
s_M(c)
\right)
$$

这不是新增 Head。

---

# 15. Conditional Flow Matching 回顾

GT clean trajectory：

$$
x_1
=
\widetilde\tau
$$

Gaussian noise：

$$
x_0
\sim
\mathcal N(0,I)
$$

Flow time：

$$
t
\sim
\mathcal U(0,1)
$$

Linear probability path：

$$
\boxed{
x_t
=
(1-t)x_0+tx_1
}
$$

对时间求导：

$$
\frac{dx_t}{dt}
=
x_1-x_0
$$

目标 vector field：

$$
\boxed{
v^*
=
x_1-x_0
}
$$

GT CFM：

$$
\boxed{
\mathcal L_{\mathrm{CFM}}
=
\mathbb E
\left[
\left\|
v_{\phi_S}(x_t,t,C_S)
-
(x_1-x_0)
\right\|_2^2
\right]
}
$$

---

# 16. UrbanNav CFM Flow-path OPD

## 16.1 Student Flow Rollout

$$
x_0^S
\sim
\mathcal N(0,I)
$$

$$
x_{k+1}^S
=
x_k^S
+
\Delta t
v_S
\left(
x_k^S,t_k,C_S
\right)
$$

收集：

$$
\mathcal X^S
=
\{x_0^S,\ldots,x_K^S\}
$$

## 16.2 Teacher 在相同 Flow State 上评估

$$
v_{S,k}
=
v_S
\left(
\operatorname{sg}(x_k^S),t_k,C_S
\right)
$$

$$
v_{T,k}
=
v_T
\left(
\operatorname{sg}(x_k^S),t_k,C_T
\right)
$$

Teacher 不沿自己的 flow path 决定监督位置；监督位置来自 Student。

## 16.3 Flow OPD Loss

$$
\boxed{
\mathcal L_{F\text{-}OPD}
=
q^T
\frac{
\sum_{k,j}
m_j
\left\|
v_{S,k,j}
-
\operatorname{sg}(v_{T,k,j})
\right\|_2^2
}{
K\sum_jm_j
}
}
$$

## 16.4 与 GT CFM 的关系

GT CFM：

> 在 GT bridge 上学习正确 vector field。

Flow OPD：

> 在 Student 自己访问的 flow state 上学习 Teacher correction。

二者共同使用：

$$
\boxed{
\mathcal L_{\mathrm{CFM}}
+
\lambda_F
\mathcal L_{F\text{-}OPD}
}
$$

---

# 17. UrbanNav 最终目标

$$
\mathcal L_{\mathrm{base}}
=
\lambda_R\mathcal L_{\mathrm{FullCoT}}
+
\lambda_D^{GT}\mathcal L_{\mathrm{Decision}}
+
\lambda_C\mathcal L_{\mathrm{CFM}}
$$

$$
\mathcal L_{\mathrm{OPD}}
=
\lambda_D^{OPD}\mathcal L_{D\text{-}OPD}
+
\lambda_F^{OPD}\mathcal L_{F\text{-}OPD}
$$

$$
\boxed{
\mathcal L_{\mathrm{Student}}
=
\lambda_R\mathcal L_{\mathrm{FullCoT}}
+
\lambda_D^{GT}\mathcal L_{\mathrm{Decision}}
+
\lambda_C\mathcal L_{\mathrm{CFM}}
+
\lambda_D^{OPD}\mathcal L_{D\text{-}OPD}
+
\lambda_F^{OPD}\mathcal L_{F\text{-}OPD}
}
$$

只新增两个 OPD Loss，不增加模型 Head 或新标签。

---

# 18. 完整训练算法

```text
输入：
Existing Student πS
Privileged Teacher πT
Logged UrbanNav Dataset D

每个 batch：

1. 计算原始监督：
   FullCoT SFT
   Decision SFT
   GT CFM

2. Student 生成 Decision rollout

3. Teacher 与 Student 在同一个 Student Decision prefix 上计算 logits

4. 在原 Decision value token span 上计算 Decision-prefix OPD

5. Student / Teacher 分别构造原有 [TRAJ] condition

6. Student CFM 从 noise 开始 rollout，收集 flow states

7. Teacher 与 Student 在相同 Student flow states 上计算 vector field

8. 计算 Flow-path OPD

9. 合并原监督与两个 OPD Loss

10. 只更新 Student
```

---

# 19. 伪代码

```python
for batch in dataloader:
    loss_fullcot = student.fullcot_sft_loss(batch)
    loss_decision_gt = student.decision_sft_loss(batch)
    loss_cfm_gt = student.cfm_gt_loss(batch)

    with torch.no_grad():
        decision_rollout = student.generate_decision(
            batch.student_observation
        )

    value_mask = parse_decision_value_spans(decision_rollout)

    if value_mask.valid:
        student_logits = student.score_prefix(
            batch.student_observation,
            decision_rollout
        )

        with torch.no_grad():
            teacher_logits = teacher.score_prefix(
                batch.teacher_observation,
                decision_rollout
            )

        loss_decision_opd = reverse_kl(
            student_logits,
            teacher_logits,
            mask=value_mask
        )
    else:
        loss_decision_opd = 0.0

    student_condition = student.build_traj_condition(
        batch.student_observation
    )

    with torch.no_grad():
        teacher_condition = teacher.build_traj_condition(
            batch.teacher_observation
        )

    with torch.no_grad():
        flow_states = student.rollout_cfm_states(
            condition=student_condition,
            solver_steps=K
        )

    loss_flow_opd = 0.0

    for state, flow_time in flow_states:
        student_velocity = student.cfm_velocity(
            state=state.detach(),
            flow_time=flow_time,
            condition=student_condition
        )

        with torch.no_grad():
            teacher_velocity = teacher.cfm_velocity(
                state=state.detach(),
                flow_time=flow_time,
                condition=teacher_condition
            )

        loss_flow_opd += masked_mse(
            student_velocity,
            teacher_velocity,
            batch.action_mask
        )

    loss_flow_opd /= K

    loss = (
        lambda_fullcot * loss_fullcot
        + lambda_decision_gt * loss_decision_gt
        + lambda_cfm_gt * loss_cfm_gt
        + lambda_decision_opd * loss_decision_opd
        + lambda_flow_opd * loss_flow_opd
    )

    optimizer.zero_grad()
    loss.backward()
    optimizer.step()
```

---

# 20. 最小实现原则

## 必需

```text
Current Student rollout
Frozen Teacher
Same Student prefix for Teacher and Student
Same Student flow state for Teacher and Student
Original supervised replay
Stop-gradient through rollout states
Teacher validity check
```

## 首版不必需

```text
Entropy selection
Disagreement weighting
Recoverability gate
Mixed GT / Student flow states
Additional value network
Reward critic
New semantic token
New classification head
New hand-written label
FullCoT raw-token OPD
```

## 失败后才增加

```text
Prefix OOD → 缩短 rollout
Teacher support mismatch → Top-k / JSD
Flow state OOD → Mixed state
Flow outlier → Huber
Teacher correction有害 → Recoverability gate
格式 token 稀释 → Value-span mask
```

---

# 21. 训练监控

## Decision OPD

```text
Decision strict parse rate
Teacher / Student action probability
Teacher–Student KL
Teacher probability on Student sampled action
Value-token OPD loss
Direction / Speed / Response subgroup
```

## Flow OPD

```text
Student-path vector MSE
GT-bridge CFM loss
Teacher / Student endpoint difference
Flow vector norm
Per solver-step discrepancy
Turn / Stop subgroup discrepancy
NaN / Inf rate
```

## 能力保持

```text
FullCoT parse retention
Original Decision accuracy
Original CFM ADE / FDE
General VQA retention
FPV-only capability retention
```

---

# 22. 如何验证 OPD 真正有作用

## 22.1 基础消融

| 模型 | Decision OPD | Flow OPD |
|---|---:|---:|
| Existing UrbanNav |  |  |
| + Decision OPD | ✓ |  |
| + Flow OPD |  | ✓ |
| + Dual-space OPD | ✓ | ✓ |

## 22.2 与 Off-policy KD 比较

Off-policy KD：

$$
D
\left(
P_S(\cdot\mid y_{<i}^{GT}),
P_T(\cdot\mid y_{<i}^{GT})
\right)
$$

OPD：

$$
D
\left(
P_S(\cdot\mid y_{<i}^{S}),
P_T(\cdot\mid y_{<i}^{S})
\right)
$$

必须保持 Teacher、Student、数据和 divergence 相同，只改变 prefix 来源。

## 22.3 Map 干预

固定 FPV，替换 Map：

```text
Left Route
Straight Route
Right Route
```

直接检查：

```text
Decision 是否改变
Trajectory lateral trend 是否改变
Endpoint 是否进入对应方向
```

不增加 Route Head。

---

# 23. 研究脉络与最新经验

## 23.1 基础 OPD

GKD / On-Policy Distillation of Language Models 建立了最基础范式：

```text
Student 生成输出序列
Teacher 在 Student sequence 上提供 dense token feedback
```

## 23.2 Privileged Context

On-Policy Context Distillation 让 Teacher 使用额外 context，同时在 Student rollout 上监督 Student，证明 OPD 可用于内化训练期额外上下文。

## 23.3 VLA 与具身智能

VLA-OPD 将 OPD 扩展到 VLA post-training，在 Student 自己诱导的策略状态上使用 Expert Teacher 的密集动作分布，并强调 Reverse KL。

## 23.4 Social Navigation

SOPD-SocialNav 在固定视觉数据上使用 Student-generated responses，说明没有 simulator 也可以实现 response-space OPD。

## 23.5 Flow Teacher

FA-OPD 使用 Flow Matching Teacher 在 Student 环境 rollout 状态上提供动作纠正与 expert-likeness 信号，说明 Flow Teacher 可以参与连续控制 OPD。

## 23.6 最新失败模式

近期研究指出主要风险包括：

```text
Teacher 在 Student prefix 上不可靠
Teacher–Student support 不匹配
Top-k / sampled-token gradient 有偏
privileged information 不可恢复
长 reasoning rollout 中自我修正受到抑制
```

因此推荐：

```text
基础 OPD
→ 诊断具体失败
→ 按失败原因增加最小修复
```

---

# 24. 参考文献

1. Agarwal, R. et al. **On-Policy Distillation of Language Models: Learning from Self-Generated Mistakes.** ICLR 2024. arXiv:2306.13649.
2. Song, M. et al. **A Survey of On-Policy Distillation for Large Language Models.** arXiv:2604.00626, 2026.
3. Ye, T. et al. **On-Policy Context Distillation for Language Models.** arXiv:2602.12275, 2026.
4. Zhao, S. et al. **On-Policy Self-Distillation for Large Language Models.** arXiv:2601.18734, 2026.
5. Zhang, D. et al. **Fast and Effective On-policy Distillation from Reasoning Prefixes.** arXiv:2602.15260, 2026.
6. Fu, Y. et al. **Revisiting On-Policy Distillation: Empirical Failure Modes and a Simple Fix.** arXiv:2603.25562, 2026.
7. Zhu, S. et al. **The Many Faces of On-Policy Distillation: Pitfalls, Mechanisms, and Fixes.** arXiv:2605.11182, 2026.
8. Kaur, S. et al. **Rethinking On-Policy Self-Distillation for Thinking Models.** arXiv:2607.05184, 2026.
9. Yang, S. et al. **OPRD: On-Policy Representation Distillation.** arXiv:2606.06021, 2026.
10. Zhong, Z. et al. **VLA-OPD: Bridging Offline SFT and Online RL for Vision-Language-Action Models via On-Policy Distillation.** arXiv:2603.26666, 2026.
11. Zhang, X. et al. **SOPD-SocialNav: Selective On-Policy Distillation for Vision-Language Social Navigation.** arXiv:2607.19850, 2026.
12. Wan, Z. et al. **Adversarial Dual On-Policy Distillation from Expressive Flow-based Teacher.** arXiv:2605.27095, 2026.
13. Gu, Y. et al. **MiniLLM: Knowledge Distillation of Large Language Models.** ICLR 2024.

---

# 25. 最终理解

可以把 OPD 理解为：

```text
Student 当前策略产生自己的错误状态
        ↓
Teacher 在这些状态上给出密集局部纠正
        ↓
Student 学会在自己真正会遇到的状态上行动
        ↓
更新后的 Student 产生新的状态分布
        ↓
继续纠正
```

核心公式：

$$
\boxed{
\min_\theta
\mathbb E_{s\sim d_{\pi_\theta}}
\left[
D
\left(
\pi_\theta(\cdot\mid s),
q(\cdot\mid s)
\right)
\right]
}
$$

对于 E2E UrbanNav：

$$
\boxed{
\begin{aligned}
&\text{Decision Prefix State}
\rightarrow
\text{Teacher Token Distribution}
\\
&+
\text{Student CFM Flow State}
\rightarrow
\text{Teacher Vector Field}
\\
&+
\text{Original FullCoT / Decision / CFM Replay}
\end{aligned}
}
$$

所以最简本质不是构造更多中间标签，而是：

> **让原有 Teacher 在原有 E2E Policy 自己产生的离散状态和连续状态上纠正 Student。**
