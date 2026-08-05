---
title: "文章标题"
slug: "article-slug"
date: "YYYY-MM-DD"
updated: "YYYY-MM-DD"
category: "技术笔记"
tags: "标签一, 标签二"
summary: "用一两句话介绍文章内容。"
cover: "/uploads/cover.png"
top: false
published: false
---

# 文章标题

这里是文章摘要或开头。

## 1. 第一章节

章节内容。

### 1.1 子章节

子章节内容。

## 2. 第二章节

章节内容。

## 3. 公式与代码示例

行内公式使用一对美元符号，例如：$v_t(x)$。

独立公式使用两对美元符号：

$$
x_{t+\Delta t}=x_t+\Delta t\,v_t(x_t)
$$

代码围栏必须单独占行，并在前后保留空行：

```python
x = torch.randn(batch_size, horizon, action_dim)

for i in range(num_steps):
    t = torch.full((batch_size,), i / num_steps, device=x.device)
    velocity = model(x, t, condition)
    x = x + velocity / num_steps

trajectory = x
```

## 4. 总结

总结内容。
