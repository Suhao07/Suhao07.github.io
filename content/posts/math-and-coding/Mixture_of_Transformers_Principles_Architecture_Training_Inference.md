---
title: "MoT"
slug: "mixture-of-transformer"
date: "2026-08-10"
updated: "2026-08-10"
category: "Math & Coding"
tags: "技术笔记"
summary: "MoT原理及相关实践。"
cover: "/legacy-assets/assets/images/background2.png"
top: true
published: true
------
# Mixture-of-Transformers（MoT）原理、架构、训练与推理详解

> 本文以 **Liang et al., “Mixture-of-Transformers: A Sparse and Scalable Architecture for Multi-Modal Foundation Models”, TMLR 2025** 所提出的原始 Mixture-of-Transformers（MoT）为主线，并在最后单独说明它在 2026 年 VLA / World-Action Model 中常见的“Understanding / World / Action Experts”扩展形式。
>
> **最重要的概念：MoT 不是传统 Mixture-of-Experts（MoE）的同义词。** 原始 MoT 不需要学习 Router，而是依据 token 的已知 modality 做确定性路由；不同 modality 拥有独立的 Transformer 参数，但所有 modality 的 token 仍通过 **global self-attention** 相互通信。

---

# 0. 一句话理解 MoT

Mixture-of-Transformers 的核心思想是：

> **“参数分开，注意力相通。”**

即：

- Text token 走 Text Transformer 参数；
- Image token 走 Image Transformer 参数；
- Speech / Action / Video token 可以走各自的 Transformer 参数；
- 但在 Attention 中，所有 modality 的 token 的 Query、Key、Value 被重新合并到同一序列，因此不同专家仍能直接进行全局信息交换。

```text
                    ┌─ Text Transformer parameters ─┐
Text tokens ────────┤                               │
                    │                               ▼
                    │                         Global Attention
                    │                               ▲
Image tokens ───────┤                               │
                    └─ Image Transformer parameters┘
```

它同时获得：

1. **Specialization**：不同 modality 不再被迫共享完全相同的参数；
2. **Cross-modal interaction**：不同 modality 仍通过 global attention 交换信息；
3. **Conditional computation**：每个 token 只激活自己所属 modality 的参数；
4. **Scalability**：增加 modality 时可以增加对应专家，而不是让所有 token 都经过所有参数。

---

# 1. 为什么需要 Mixture-of-Transformers？

## 1.1 Dense Multimodal Transformer 的问题

传统 unified multimodal Transformer 通常将文本、图像、语音等 token 拼接成统一序列：

$$
X=[x_1,x_2,\ldots,x_N]
$$

然后所有 token 共用完全相同的 Transformer 参数：

$$
W_Q,\;W_K,\;W_V,\;W_O,\;W_{FFN}
$$

也就是说：

$$
q_i=x_iW_Q
$$

$$
k_i=x_iW_K
$$

$$
v_i=x_iW_V
$$

无论当前 token 是 text、image、speech 还是 action，都必须通过相同参数空间。

这些 token 的统计结构却明显不同：

```text
Text   : 离散语义、句法关系、长程逻辑
Image  : 二维空间结构、纹理、局部连续性
Video  : 时空动力学、运动连续性
Action : 低维连续控制、运动学约束
```

因此 Dense Transformer 实际上要求同一组参数同时解决多个强异构映射：

$$
f_\theta(x^{text})
$$

$$
f_\theta(x^{image})
$$

$$
f_\theta(x^{action})
$$

这容易产生 **representation interference / gradient interference**。

---

# 2. MoT 与 MoE 的根本区别

## 2.1 Mixture-of-Experts（MoE）

经典 MoE 通常只把 Transformer 中的 FFN 替换为多个 expert：

$$
E_1,E_2,\ldots,E_M
$$

Router 根据 token feature 计算：

$$
p(e\mid x_i)=\operatorname{Softmax}(W_rx_i)
$$

选择 Top-K experts：

$$
\mathcal{E}(x_i)=\operatorname{TopK}\left(p(e\mid x_i)\right)
$$

因此路由是：

```text
token → learned router → Top-K experts
```

## 2.2 Mixture-of-Transformers（MoT）

原始 MoT 中不需要 learned router，因为 token 的 modality 本身就是已知的：

$$
m_i\in\{text,image,speech,\ldots\}
$$

专家直接由 modality 决定：

$$
e_i=m_i
$$

例如：

```text
text token   → text expert
image token  → image expert
speech token → speech expert
```

所以不存在传统 MoE 常见的：

- Router loss；
- Expert load balancing；
- Router collapse；
- Top-K expert selection。

## 2.3 MoE vs MoT

| 属性 | Dense Transformer | MoE | Mixture-of-Transformers |
|---|---|---|---|
| Expert routing | 无 | Learned Router | Modality deterministic routing |
| FFN specialization | 否 | 是 | 是 |
| Attention specialization | 否 | 通常否 | 是 |
| Q/K/V projection | Shared | Shared | Modality-specific |
| Output projection | Shared | Shared | Modality-specific |
| LayerNorm | Shared | 通常 Shared | Modality-specific |
| Global cross-modal attention | 是 | 是 | 是 |
| Router auxiliary loss | 无 | 常需要 | 不需要 |
| 主要目的 | 通用建模 | 增大参数容量 | Multimodal specialization + sparse compute |

因此可以把原始 MoT 理解成：

> **“以 modality 为确定性 Router 的 Transformer-level conditional computation。”**

---

# 3. MoT 的整体架构

假设有三个 modality：

$$
\mathcal{M}=\{text,image,speech\}
$$

输入序列：

$$
X=[x_1^{text},x_2^{text},x_3^{image},x_4^{image},x_5^{speech}]
$$

为每个 token 保存 modality ID：

$$
M=[text,text,image,image,speech]
$$

## 3.1 一个 MoT Block 中哪些参数被拆开？

原始 MoT 对几乎所有 **non-embedding Transformer parameters** 做 modality-specific decoupling，包括：

- Query projection；
- Key projection；
- Value projection；
- Output projection；
- Feed-Forward Network；
- LayerNorm / RMSNorm。

第 `l` 层、第 `m` 个 modality 对应：

$$
W_{Q,l}^{(m)},\;W_{K,l}^{(m)},\;W_{V,l}^{(m)},\;W_{O,l}^{(m)}
$$

$$
FFN_l^{(m)}
$$

$$
Norm_l^{(m)}
$$

因此不是所有 token 都经过同一个：

$$
Transformer_l(X)
$$

而是每个 token 使用与自身 modality 对应的参数：

$$
Transformer_l^{(m_i)}(x_i)
$$

但 **Attention interaction 本身仍然是 global 的**。

---

# 4. 数学推导：Modality-Specific QKV + Global Attention

## 4.1 Dense Transformer

普通 Transformer 中：

$$
Q=XW_Q
$$

$$
K=XW_K
$$

$$
V=XW_V
$$

Self-Attention：

$$
A=\operatorname{Softmax}\left(\frac{QK^\top}{\sqrt{d_h}}+M_{attn}\right)
$$

$$
Z=AV
$$

$$
Y=ZW_O
$$

## 4.2 MoT：每种 modality 使用不同 QKV

定义第 `m` 个 modality 的 binary mask：

$$
D_m=\operatorname{diag}\left(\mathbf{1}[m_i=m]\right)
$$

那么 MoT 的 Query 可以写成：

$$
Q=\sum_{m=1}^{M}D_mXW_Q^{(m)}
$$

同理：

$$
K=\sum_{m=1}^{M}D_mXW_K^{(m)}
$$

$$
V=\sum_{m=1}^{M}D_mXW_V^{(m)}
$$

这里实际完成：

```text
Step 1  按 modality 拆 token
Step 2  分别执行不同 Q/K/V projection
Step 3  按原 token 顺序重新 merge
```

```text
Text  : X_text → WQ_text / WK_text / WV_text
Image : X_img  → WQ_img  / WK_img  / WV_img
Speech: X_sp   → WQ_sp   / WK_sp   / WV_sp

                   ↓

              Merge Q/K/V

                   ↓

             Global Attention
```

## 4.3 为什么说 Attention 是 global 的？

完成 modality-specific QKV projection 后：

$$
Q=[Q^{text};Q^{image};Q^{speech}]
$$

$$
K=[K^{text};K^{image};K^{speech}]
$$

$$
V=[V^{text};V^{image};V^{speech}]
$$

接下来仍计算统一 attention matrix：

$$
A=\operatorname{Softmax}\left(\frac{QK^\top}{\sqrt{d_h}}\right)
$$

因此对于一个 Text Query 和 Image Key：

$$
A_{ij}\propto\exp\left(\frac{q_i^{text}(k_j^{image})^\top}{\sqrt{d_h}}\right)
$$

这个 cross-modal attention 项并没有被置零。

因此：

> Text Expert 和 Image Expert 的参数是分开的，但 Text token 仍然能够 attend 到 Image token。

MoT 正是在解决：

```text
想让不同 modality 专门化 → 参数应该分开

但又需要跨模态融合       → 信息不能完全分开
```

最终方案：

```text
Parameter Space → 分开
Attention Space → 共享
```

---

# 5. Modality-Specific Output Projection

Global attention 得到：

$$
Z=AV
$$

不同 token 的输出再次经过自身 modality 的 Output Projection：

$$
y_i=W_O^{(m_i)}z_i
$$

矩阵形式：

$$
Y=\sum_{m=1}^{M}D_mZW_O^{(m)}
$$

因此完整 attention layer 是：

```text
X
↓
Modality-specific WQ/WK/WV
↓
Merge Q/K/V
↓
Global Attention
↓
Modality-specific WO
```

---

# 6. Modality-Specific Feed-Forward Network

普通 Transformer：

$$
FFN(x)=W_2\sigma(W_1x)
$$

MoT：

$$
FFN^{(m)}(x)=W_2^{(m)}\sigma(W_1^{(m)}x)
$$

第 `i` 个 token：

$$
z_i=FFN^{(m_i)}(y_i)
$$

矩阵形式：

$$
FFN_{MoT}(Y)=\sum_{m=1}^{M}D_mFFN^{(m)}(Y)
$$

因此：

```text
Text tokens   → Text FFN
Image tokens  → Image FFN
Speech tokens → Speech FFN
```

不会执行其他 modality 的 FFN。

---

# 7. 一个完整 MoT Transformer Block

用 Pre-Norm 表示：

## Step 1：Modality-Specific Normalization

$$
\tilde{x}_i=Norm_{attn}^{(m_i)}(x_i)
$$

## Step 2：Modality-Specific QKV

$$
q_i=\tilde{x}_iW_Q^{(m_i)}
$$

$$
k_i=\tilde{x}_iW_K^{(m_i)}
$$

$$
v_i=\tilde{x}_iW_V^{(m_i)}
$$

## Step 3：Global Self-Attention

$$
\alpha_{ij}=\frac{\exp(q_ik_j^\top/\sqrt{d_h})}{\sum_r\exp(q_ik_r^\top/\sqrt{d_h})}
$$

$$
z_i=\sum_j\alpha_{ij}v_j
$$

## Step 4：Modality-Specific Output Projection

$$
u_i=x_i+W_O^{(m_i)}z_i
$$

## Step 5：Modality-Specific FFN

$$
\tilde{u}_i=Norm_{ffn}^{(m_i)}(u_i)
$$

$$
x_i^{next}=u_i+FFN^{(m_i)}(\tilde{u}_i)
$$

最终：

$$
X^{l+1}=MoTBlock_l(X^l,M)
$$

---

# 8. 为什么这种结构有效？

## 8.1 减少跨 modality 参数冲突

Dense Transformer 中：

$$
\nabla_\theta\mathcal{L}_{text}
$$

和：

$$
\nabla_\theta\mathcal{L}_{image}
$$

都会更新同一个参数：

$$
\theta
$$

如果两个任务需要的表示方向冲突，可能出现：

$$
\left\langle\nabla_\theta\mathcal{L}_{text},\nabla_\theta\mathcal{L}_{image}\right\rangle<0
$$

MoT 则将参数拆成：

$$
\theta=\{\theta_{text},\theta_{image},\theta_{speech}\}
$$

这样 text-specific 计算主要使用：

$$
\theta_{text}
$$

image-specific 计算主要使用：

$$
\theta_{image}
$$

从而减少不同 modality 在同一组 projection / FFN 参数上的竞争。

## 8.2 但又没有牺牲跨模态信息交流

如果完全使用两个独立 Transformer：

```text
Text Transformer

Image Transformer
```

还必须额外增加 Cross-Attention。

MoT 每层天然允许：

$$
Attention(Q^{text},K^{image},V^{image})
$$

因此在保持参数 specialization 的同时保留跨模态通信。

## 8.3 Sparse / Conditional Computation

设有 `M` 个 modality experts。

第 `i` 个 token 只激活：

$$
\theta^{(m_i)}
$$

而不是：

$$
\{\theta^{(1)},\theta^{(2)},\ldots,\theta^{(M)}\}
$$

因此 active computation 是 sparse 的。

但必须注意：

> **如果每个 expert 与原 Dense Transformer 等宽，则 MoT 会增加总参数量，但单个 token 仍只执行一个 expert，因此单 token 的 expert-side FLOPs 不会乘以 expert 数。**

MoT 的本质不是“复制参数之后自动让一次 forward 变成原来的几分之一”，而是：

> 在给定总参数容量或目标性能时，通过 conditional computation 获得更大的 modality-specific capacity，而不要求每个 token 激活全部参数。

---

# 9. 参数量和 FLOPs 推导

假设隐藏维度：

$$
d
$$

FFN expansion ratio：

$$
r
$$

sequence length：

$$
N
$$

忽略 bias 与 normalization 参数。

## 9.1 Dense Transformer 每层参数量

Attention projections：

$$
P_{attn}\approx4d^2
$$

FFN：

$$
P_{ffn}\approx2rd^2
$$

总参数量：

$$
P_{dense}\approx(4+2r)d^2
$$

如果：

$$
r=4
$$

则：

$$
P_{dense}\approx12d^2
$$

## 9.2 MoT 参数量

如果有 `M` 个完整 modality experts，并且每个 expert 与 Dense block 等宽：

$$
P_{MoT}\approx M(4+2r)d^2
$$

所以：

$$
P_{MoT}\approx MP_{dense}
$$

但这是 **total parameters**，不是每个 token 实际激活的参数。

每个 token 只使用：

$$
P_{active/token}\approx(4+2r)d^2
$$

而不是：

$$
M(4+2r)d^2
$$

## 9.3 Dense Transformer 每层主要 FLOPs

线性 projection：

$$
C_{proj}\approx4Nd^2
$$

FFN：

$$
C_{ffn}\approx2rNd^2
$$

Attention score 与 value aggregation：

$$
C_{attn}\approx2N^2d
$$

因此：

$$
C_{dense}\approx(4+2r)Nd^2+2N^2d
$$

## 9.4 MoT active FLOPs

不同 modality token 不会执行所有 experts：

$$
\sum_{m=1}^{M}N_m=N
$$

expert-side computation：

$$
C_{expert}\approx\sum_m(4+2r)N_md^2
$$

因此：

$$
C_{expert}\approx(4+2r)Nd^2
$$

Global Attention 仍然是：

$$
C_{global-attn}\approx2N^2d
$$

所以在“每个 expert 与 Dense 等宽”的情况下：

$$
C_{MoT}\approx(4+2r)Nd^2+2N^2d
$$

即同宽复制专家主要提升 **parameter capacity / specialization**，并不会使一次 forward 自动比同宽 Dense Transformer 更便宜。

原始论文报告的是 **达到相当模型质量所需训练 FLOPs 的减少**：

- Chameleon text-image 7B 设置达到 Dense 质量时约使用 55.8% FLOPs；
- 加入 speech 后达到可比 speech 质量时约使用 37.2% FLOPs；
- Transfusion 7B 设置达到相近 image quality 时约使用 Dense 的三分之一 FLOPs。

因此不要将论文结论误解成：

```text
相同 hidden size 的 MoT 单次 forward 必然只有 Dense 的 1/M FLOPs
```

这个结论不成立。

---

# 10. MoT Training：数据如何进入模型？

训练样本首先转换成 unified token sequence，例如：

```text
[BOS]
text token
text token
image token
image token
image token
text token
[EOS]
```

同时建立：

```text
modality_ids =
[text, text, text, image, image, image, text, text]
```

或 binary masks：

```text
text_mask
image_mask
speech_mask
```

满足：

$$
\sum_mD_m=I
$$

意味着每个 token 恰好属于一个 expert。

---

# 11. MoT 的训练目标

MoT 是一种 **architecture**，并不规定唯一 objective。

统一可以写成：

$$
\mathcal{L}=\sum_{m=1}^{M}\lambda_m\mathcal{L}_m
$$

## 11.1 Autoregressive Text / Discrete Image

例如 text 与离散 image token 均使用 next-token prediction：

$$
\mathcal{L}_{AR}=-\sum_t\log p_\theta(x_t\mid x_{<t})
$$

## 11.2 Text AR + Image Diffusion

如果文本使用 AR，而连续 image latent 使用 diffusion / flow objective：

$$
\mathcal{L}=\lambda_{text}\mathcal{L}_{AR}+\lambda_{image}\mathcal{L}_{diff}
$$

例如 velocity prediction：

$$
\mathcal{L}_{diff}=\mathbb{E}\left[\left\|v_\theta(z_t,t,c)-v^\star\right\|_2^2\right]
$$

因此可以形成：

```text
Text token  → Text Transformer parameters  → AR loss
Image token → Image Transformer parameters → Diffusion loss

                         ↓

                  Global Attention
```

这使 MoT 天然适合 heterogeneous objectives。

---

# 12. 梯度是怎么传播的？

这是 MoT 很容易被忽略的关键点。

假设一个 text token：

$$
q_i^{text}
$$

attend 到 image token：

$$
k_j^{image},v_j^{image}
$$

则：

$$
z_i=\sum_j\alpha_{ij}v_j
$$

因此，即使当前 loss 主要来自 text output，梯度也可能通过 global attention 回传到 image 的 K/V projection：

$$
\frac{\partial\mathcal{L}_{text}}{\partial W_V^{image}}\neq0
$$

只要 text token 对 image token 存在有效 attention。

所以 MoT 并不是：

```text
不同 experts 完全独立训练
```

而是：

```text
参数路径 specialized
+
attention interaction coupled
```

---

# 13. MoT 训练伪代码

```python
def train_step(batch, model, optimizer):
    # 1. Construct multimodal tokens
    tokens = build_interleaved_tokens(
        text=batch.text,
        images=batch.images,
        speech=batch.speech,
    )

    modality_ids = build_modality_ids(tokens)
    attention_mask = build_attention_mask(tokens)

    # 2. Forward through MoT
    hidden = model.embed(tokens)

    for layer in model.layers:
        hidden = layer(
            hidden,
            modality_ids=modality_ids,
            attention_mask=attention_mask,
        )

    # 3. Modality-specific objectives
    loss_text = text_objective(hidden, batch)
    loss_image = image_objective(hidden, batch)
    loss_speech = speech_objective(hidden, batch)

    loss = (
        lambda_text * loss_text
        + lambda_image * loss_image
        + lambda_speech * loss_speech
    )

    # 4. Backprop
    optimizer.zero_grad()
    loss.backward()
    optimizer.step()

    return loss
```

---

# 14. MoT Layer 伪代码

```python
class MoTLayer:

    def forward(self, x, modality_ids, attention_mask):

        # 1. Modality-specific Q/K/V projection
        Q = empty_like_q(x)
        K = empty_like_k(x)
        V = empty_like_v(x)

        for m in modalities:
            idx = modality_ids == m
            x_m = x[idx]

            x_m = self.attn_norm[m](x_m)

            Q[idx] = self.Wq[m](x_m)
            K[idx] = self.Wk[m](x_m)
            V[idx] = self.Wv[m](x_m)

        # 2. GLOBAL attention
        A = softmax(
            Q @ K.transpose(-1, -2)
            / sqrt(head_dim)
            + attention_mask
        )

        Z = A @ V

        # 3. Modality-specific output projection
        attn_out = empty_like(x)

        for m in modalities:
            idx = modality_ids == m
            attn_out[idx] = self.Wo[m](Z[idx])

        x = x + attn_out

        # 4. Modality-specific FFN
        ffn_out = empty_like(x)

        for m in modalities:
            idx = modality_ids == m
            x_m = self.ffn_norm[m](x[idx])
            ffn_out[idx] = self.ffn[m](x_m)

        x = x + ffn_out

        return x
```

---

# 15. 为什么必须“先 Merge QKV，再做 Attention”？

错误实现：

```text
Text tokens  → Text self-attention
Image tokens → Image self-attention
```

这相当于：

$$
A=\begin{bmatrix}A_{text,text}&0\\0&A_{image,image}\end{bmatrix}
$$

Cross-modal attention 被完全切断。

而 MoT 的目标是：

$$
A=\begin{bmatrix}A_{text,text}&A_{text,image}\\A_{image,text}&A_{image,image}\end{bmatrix}
$$

因此必须：

```text
Modality-specific projections
          ↓
       Merge QKV
          ↓
   Global Self-Attention
```

这是整个 MoT 最核心的实现细节。

---

# 16. Inference：MoT 推理过程

推理阶段不需要 Router，因为每个 token 的 modality 是已知的。

每个 token 根据自己的 modality 调用：

$$
W_Q^{(m_i)},\;W_K^{(m_i)},\;W_V^{(m_i)}
$$

随后进入 global attention。

因此与 Dense Transformer 相比，最大区别是：

> **QKV / FFN / Norm 的参数选择由 modality ID 决定。**

---

# 17. Autoregressive 推理 + KV Cache

对于 autoregressive generation，历史 token 的 Key / Value 可以缓存。

第 `t` 步新 token：

$$
x_t
$$

如果其 modality 为：

$$
m_t
$$

则：

$$
q_t=x_tW_Q^{(m_t)}
$$

$$
k_t=x_tW_K^{(m_t)}
$$

$$
v_t=x_tW_V^{(m_t)}
$$

追加到 cache：

$$
K_{cache}\leftarrow[K_{cache};k_t]
$$

$$
V_{cache}\leftarrow[V_{cache};v_t]
$$

然后：

$$
z_t=\operatorname{softmax}\left(\frac{q_tK_{cache}^\top}{\sqrt{d_h}}\right)V_{cache}
$$

因为所有 modality 的 K/V 都被投影到兼容 attention space，因此 Text Query 可以直接访问缓存中的 Image Key / Value。

---

# 18. MoT Incremental Inference 伪代码

```python
def decode_step(token, modality_id, kv_cache, model):

    x = model.embed(token, modality_id)

    for layer_id, layer in enumerate(model.layers):

        # Select expert deterministically
        expert = layer.experts[modality_id]

        # Modality-specific projection
        x_norm = expert.attn_norm(x)

        q = expert.Wq(x_norm)
        k = expert.Wk(x_norm)
        v = expert.Wv(x_norm)

        # Append K/V to global multimodal KV cache
        kv_cache[layer_id].append(k, v)

        K = kv_cache[layer_id].K
        V = kv_cache[layer_id].V

        # Global attention over all historical modalities
        z = attention(q, K, V)

        # Modality-specific output projection
        x = x + expert.Wo(z)

        # Modality-specific FFN
        x = x + expert.ffn(
            expert.ffn_norm(x)
        )

    return x, kv_cache
```

---

# 19. 为什么比“多个独立 Transformer + Cross Attention”更自然？

另一种方案是：

```text
Text Transformer
      ↕ Cross Attention
Image Transformer
```

如果有多个 modality，潜在 pair 数约为：

$$
\frac{M(M-1)}{2}
$$

随着 modality 增加，pairwise cross-attention 结构会迅速复杂。

MoT 则统一成：

$$
GlobalAttention(Q,K,V)
$$

新增 modality 时主要增加：

```text
new QKV projections
new output projection
new FFN
new norms
```

而不需要为每一对 modality 单独设计 cross-attention。

---

# 20. MoT 的核心作用总结

## 20.1 Modality Specialization

$$
\theta_{text},\;\theta_{image},\;\theta_{action}
$$

不再强迫所有 token 竞争一个参数空间。

## 20.2 保留全局跨模态融合

$$
A=Softmax(QK^\top)
$$

实现任意 modality 间 token interaction。

## 20.3 Conditional Computation

每个 token 只激活：

$$
\theta^{(m_i)}
$$

## 20.4 支持 heterogeneous objectives

```text
Text   → Autoregressive loss
Video  → Diffusion / Flow Matching loss
Action → Flow Matching / Diffusion Policy loss
```

---

# 21. 原始 MoT 与 2026 VLA 中“MoT”的区别

原始 MoT 的 expert 通常按 **modality** 拆分：

```text
Text Expert
Image Expert
Speech Expert
```

近年的 VLA / World-Action Model 经常把 MoT 泛化成按 **functional stream / task stream** 拆分：

```text
Understanding Expert
World / Video Expert
Action Expert
```

例如：

- Understanding Expert：语言与视觉语义理解；
- Video / World Expert：future visual dynamics；
- Action Expert：continuous action generation。

它们继承同一个核心思想：

> **specialized Transformer parameters + shared / joint attention space**

但具体 attention mask、KV cache、异步执行方式并不一定与原始 Meta MoT 完全相同。

---

# 22. VLA 中为什么尤其适合 MoT？

VLA 的几个能力存在明显 distribution mismatch。

## 22.1 Understanding

```text
输入：RGB + Language
目标：semantic reasoning / instruction following / commonsense reasoning
```

通常接近 autoregressive VLM。

## 22.2 World Modeling

```text
输入输出：visual latent / future latent / video latent
目标：diffusion / flow matching / video generation
```

## 22.3 Action

```text
输出：continuous robot action / trajectory / waypoint / joint command
目标：behavior cloning / flow matching / diffusion policy
```

如果全部塞入同一个 Transformer：

$$
\theta_{shared}
$$

需要同时优化：

$$
\mathcal{L}_{language},\;\mathcal{L}_{video},\;\mathcal{L}_{action}
$$

MoT 可以拆成：

$$
\theta_U,\;\theta_W,\;\theta_A
$$

分别承担 Understanding、World Prediction 和 Action，同时通过 joint attention 连接。

---

# 23. VLA-MoT 的统一数学形式

假设有三种 stream：

$$
S=\{U,W,A\}
$$

第 `l` 层：

$$
Q_s^l=X_s^lW_{Q,s}^l
$$

$$
K_s^l=X_s^lW_{K,s}^l
$$

$$
V_s^l=X_s^lW_{V,s}^l
$$

合并：

$$
Q^l=Concat(Q_U^l,Q_W^l,Q_A^l)
$$

$$
K^l=Concat(K_U^l,K_W^l,K_A^l)
$$

$$
V^l=Concat(V_U^l,V_W^l,V_A^l)
$$

然后：

$$
Z^l=Attention(Q^l,K^l,V^l)
$$

再按 stream 拆开：

$$
Z_U^l,\;Z_W^l,\;Z_A^l=Split(Z^l)
$$

分别进入：

$$
FFN_U^l,\;FFN_W^l,\;FFN_A^l
$$

---

# 24. VLA 中的训练目标

一种典型形式：

$$
\mathcal{L}_{total}=\lambda_U\mathcal{L}_{understanding}+\lambda_W\mathcal{L}_{world}+\lambda_A\mathcal{L}_{action}
$$

Understanding：

$$
\mathcal{L}_{understanding}=-\sum_t\log p(y_t\mid y_{<t},O,I)
$$

World Model：

$$
\mathcal{L}_{world}=\mathbb{E}\left[\|v_W-v_W^\star\|_2^2\right]
$$

Action：

$$
\mathcal{L}_{action}=\mathbb{E}\left[\|v_A-v_A^\star\|_2^2\right]
$$

这种设计的优势是不同 experts 可以保持适合自己任务的 representation 和 objective，同时 action expert 可以通过 attention 读取 understanding / world expert 信息。

---

# 25. Asynchronous MoT

机器人控制有一个现实问题：

```text
高层 reasoning 不需要 20 Hz
低层 action 通常必须高频运行
```

因此可以让：

```text
Understanding Expert → 低频
Action Expert        → 高频
```

如果每一步都重跑完整 VLM：

$$
C_{total}=T(C_U+C_A)
$$

Asynchronous MoT 可以近似为：

$$
C_{total}\approx\frac{T}{K}C_U+TC_A
$$

其中 `K` 表示 Understanding Expert 每隔多少个 action step 更新一次。

```text
t = 0 : Understanding RUN  + Action RUN
t = 1 : Understanding CACHE + Action RUN
t = 2 : Understanding CACHE + Action RUN
...
t = K : Understanding RUN  + Action RUN
```

这是 fast-slow inference 的基本思想。

---

# 26. Asynchronous VLA-MoT 推理伪代码

```python
semantic_cache = None

for t in control_loop:

    observation = get_observation()

    # Slow branch
    if t % reasoning_interval == 0:
        semantic_tokens = understanding_expert(
            observation,
            instruction,
        )

        semantic_cache = build_kv_cache(
            semantic_tokens
        )

    # Fast branch
    action_tokens = action_expert(
        observation,
        robot_state,
        semantic_kv=semantic_cache,
    )

    action = action_decoder(action_tokens)
    robot.execute(action)
```

这属于 **Asynchronous MoT / task-expert MoT**，不是原始 2025 MoT 自动自带的机制。

---

# 27. MoT 的主要优势

## 27.1 减少 heterogeneous representation conflict

尤其适合：

```text
Language
Vision
Video
Action
Spatial tokens
```

## 27.2 保留 pretrained expert

例如：

```text
Understanding Expert ← pretrained VLM
World Expert         ← pretrained Video DiT
Action Expert        ← lightweight trainable DiT
```

## 27.3 更适合多目标训练

$$
\mathcal{L}_{LM},\;\mathcal{L}_{Video},\;\mathcal{L}_{Action}
$$

可以主要作用于不同 expert。

## 27.4 支持高效 action-only inference

部署时可采用：

```text
World generation expert → 关闭或降频
Understanding expert    → 缓存或降频
Action expert           → 高频运行
```

---

# 28. MoT 的局限

## 28.1 Global Attention 仍然是二次复杂度

$$
O(N^2d)
$$

因此 video token 很多时，global attention 仍可能成为瓶颈。

## 28.2 参数量随 expert 数增长

若每个 expert 等宽：

$$
P_{total}\propto M
$$

因此 sparse FLOPs 不代表 sparse memory。

## 28.3 原始 MoT 不是 content-adaptive routing

原始 MoT 是：

$$
e_i=m_i
$$

而不是：

$$
e_i=\arg\max_ep(e\mid x_i)
$$

所以同一 modality 内部不会自动进一步细分 semantic / geometry / motion token。

## 28.4 Modality 定义本身成为架构设计问题

在机器人里：

```text
RGB token
BEV token
Map token
Action token
Trajectory token
Reasoning token
World token
```

哪些应该属于同一个 expert，并不是 MoT 自动解决的。

## 28.5 Expert 数据不平衡

如果：

$$
N_{text}\gg N_{action}
$$

则 Action Expert 容易欠训练，需要 sampling balance、loss weighting、expert-specific learning rate 或 staged training。

---

# 29. 对 VLA / Navigation 架构的启示

对于同时包含：

```text
VLM semantic reasoning
Map / BEV spatial reasoning
Trajectory generation
Flow-Matching action generation
```

的系统，一个自然的 MoT 结构是：

```text
                 ┌──────────────────────┐
RGB + Language → │ Understanding Expert │
                 └─────────┬────────────┘
                           │
                     Joint Attention
                           │
BEV / Map ─────────→ Spatial Expert
                           │
                     Joint Attention
                           │
Robot State ───────→ Action Expert
                           │
                           ▼
                   Flow Matching Head
                           │
                           ▼
                      Trajectory
```

可以写成：

$$
X=[X_{sem},X_{spatial},X_{action}]
$$

分别通过：

$$
T_{sem},\;T_{spatial},\;T_{action}
$$

但 joint attention 保持：

$$
A=Attention(Q_{all},K_{all},V_{all})
$$

从而同时实现 semantic、spatial、action specialization 和 cross-space information exchange。

---

# 30. 与单 Transformer + Action Head 的区别

传统 VLA：

```text
Vision + Language
      ↓
Single VLM Transformer
      ↓
hidden state
      ↓
Action Head / DiT
```

MoT：

```text
Understanding Transformer
        ↕
   Joint Attention
        ↕
Action Transformer
        ↓
Action
```

Action Expert 自身拥有完整 Transformer capacity，而不是只有一个小 MLP head。

可以把它理解成：

> **Action Head 升级成 Action Transformer Expert，并通过 Joint Attention 与 VLM Expert 深层交互。**

---

# 31. 与简单 Cross-Attention 的区别

简单 Cross-Attention：

```text
VLM hidden states
      ↓
Cross Attention
      ↓
Action Decoder
```

通常是单向 Encoder → Decoder。

数学上：

$$
Q=Q_A
$$

$$
K=K_U
$$

$$
V=V_U
$$

Action 读取 Understanding。

Joint Attention：

$$
Q=[Q_U;Q_A]
$$

$$
K=[K_U;K_A]
$$

$$
V=[V_U;V_A]
$$

可以允许双向交互，也可以由 mask 限制成单向。

---

# 32. Attention Mask 决定 MoT 的信息流

MoT 中另一个关键变量是：

$$
M_{attn}
$$

如果完全 global：

$$
M_{attn}(i,j)=0
$$

所有 token 可以互相访问。

如果做 asymmetric interaction：

$$
M_{attn}(A,U)=0
$$

$$
M_{attn}(U,A)=-\infty
$$

则：

```text
Action can read Understanding
Understanding cannot read Action
```

这对于保留 frozen VLM representation 非常有用。

因此实际 VLA-MoT 设计时要同时回答：

1. 哪些参数应该 expert-specific？
2. 哪些 token 之间允许 attention？

---

# 33. 一个完整 VLA-MoT Training 伪代码

```python
def train_vla_mot(batch):

    # 1. Encode heterogeneous inputs
    semantic_tokens = encode_vlm_input(
        batch.rgb,
        batch.language,
    )

    world_tokens = encode_video_latents(
        batch.future_video,
    )

    action_tokens = encode_actions(
        batch.actions,
    )

    tokens = concat(
        semantic_tokens,
        world_tokens,
        action_tokens,
    )

    stream_ids = build_stream_ids(
        semantic_tokens,
        world_tokens,
        action_tokens,
    )

    # 2. MoT forward
    hidden = tokens

    for layer in mot_layers:
        hidden = layer(
            hidden,
            modality_ids=stream_ids,
            attention_mask=joint_attention_mask,
        )

    # 3. Split outputs
    h_sem, h_world, h_action = split_by_stream(
        hidden,
        stream_ids,
    )

    # 4. Objectives
    loss_sem = language_loss(h_sem)

    loss_world = world_flow_matching_loss(
        h_world,
        batch.future_video,
    )

    loss_action = action_flow_matching_loss(
        h_action,
        batch.actions,
    )

    loss = (
        lambda_sem * loss_sem
        + lambda_world * loss_world
        + lambda_action * loss_action
    )

    # 5. Optimization
    optimizer.zero_grad()
    loss.backward()
    optimizer.step()

    return loss
```

---

# 34. 最终总结

Mixture-of-Transformers 的本质不是：

```text
多个 Transformer 做 ensemble
```

也不是：

```text
MoE 换一个名字
```

而是：

> **将 Transformer 的参数空间按 modality / functional stream 解耦，同时在 attention space 中重新统一。**

最核心公式：

$$
Q=\sum_mD_mXW_Q^{(m)}
$$

$$
K=\sum_mD_mXW_K^{(m)}
$$

$$
V=\sum_mD_mXW_V^{(m)}
$$

然后：

$$
A=Softmax\left(\frac{QK^\top}{\sqrt{d_h}}\right)
$$

$$
Z=AV
$$

再通过 modality-specific：

$$
W_O^{(m)}
$$

和：

$$
FFN^{(m)}
$$

完成当前层计算。

因此它实现：

```text
Specialized Parameters
        +
Global Information Exchange
        +
Conditional Computation
```

对于 VLA / World-Action Models，它进一步演化成：

```text
Understanding Expert
        ↕
World Expert
        ↕
Action Expert
```

并可结合：

```text
Joint Attention
KV Cache
Fast-Slow Execution
Asynchronous Inference
Flow Matching
Diffusion Action Expert
```

从而同时处理 semantic reasoning、world dynamics modeling、continuous action generation 和 real-time control efficiency。

---

# 35. 推荐记住的 6 个关键点

1. **MoT ≠ MoE。**
2. **原始 MoT 使用 deterministic modality routing，不需要 learned router。**
3. **Q/K/V、Output Projection、FFN、Norm 都可以 modality-specific。**
4. **QKV projection 后重新 merge，执行 global self-attention，这是 MoT 的灵魂。**
5. **总参数可以很大，但每个 token 只激活自己的 expert。**
6. **在 VLA 中，MoT 常进一步变成 Understanding / World / Action specialized Transformers，并通过 joint attention 或 KV cache 协同。**

---

# 参考文献

- Liang, W. et al. **Mixture-of-Transformers: A Sparse and Scalable Architecture for Multi-Modal Foundation Models.** Transactions on Machine Learning Research, 2025. arXiv:2411.04996.
- Bi, H. et al. **Motus: A Unified Latent Action World Model.** arXiv:2512.13030.
- Huang, W. et al. **AutoMoT: A Unified Vision-Language-Action Model with Asynchronous Mixture-of-Transformers for End-to-End Autonomous Driving.** arXiv:2603.14851.
- Cai, J. et al. **InternVLA-A1: Unifying Understanding, Generation and Action for Robotic Manipulation.** arXiv:2601.02456.
