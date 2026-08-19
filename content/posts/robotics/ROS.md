---
title: "ROS"
slug: "ros"
date: "2026-08-12"
updated: "2026-08-12"
category: "机器人"
tags: "技术笔记，机器人🤖"
summary: "这里记录我在机器人通信与操作系统相关领域学习的知识和技术。"
cover: "/uploads/cover.png"
top: false
published: true
---
## 1. ROS2 系统架构

ROS2（Robot Operating System
2）不是传统意义上的操作系统，而是一套面向分布式机器人软件的中间件、工具链和软件组织规范。

一个典型机器人系统可以抽象为：

``` text
Sensors
  │
  ├── Camera
  ├── LiDAR
  ├── IMU
  └── Encoder
  │
  ▼
ROS2 Driver Nodes
  │
  ▼
Perception / Localization
  │
  ▼
Planning
  │
  ▼
Control
  │
  ▼
Robot Hardware
```

ROS2 的核心设计思想是将不同功能拆分为相互独立的 **Node**，节点之间通过
Topic、Service、Action 等机制通信。

------------------------------------------------------------------------

## 2. Node：ROS2 的基本计算单元

Node 可以理解为一个独立的软件模块。

例如：

``` text
camera_node
lidar_node
object_detector
localization_node
planner_node
controller_node
```

每个节点只承担有限职责。

ROS2 系统因此可以表示成一个有向通信图：

$$
G=(V,E)
$$

其中：

-   $V$：ROS2 Nodes
-   $E$：节点之间的通信关系

例如：

``` text
Camera Node
    │
    │ /camera/image
    ▼
Detector
    │
    │ /detections
    ▼
Planner
    │
    │ /waypoint
    ▼
Controller
```

### Python 节点伪代码

``` python
class MyNode(Node):

    def __init__(self):
        super().__init__("my_node")

        self.publisher = self.create_publisher(
            MessageType,
            "/topic",
            qos
        )

    def timer_callback(self):
        msg = MessageType()
        self.publisher.publish(msg)
```

------------------------------------------------------------------------

## 3. Topic：异步数据流

Topic 使用 Publisher / Subscriber 模式。

设发布节点为：

$$
P
$$

订阅节点为：

$$
S_1,S_2,\ldots,S_n
$$

则消息传播关系为：

$$
P \rightarrow \{S_1,S_2,\ldots,S_n\}
$$

Publisher 不需要知道 Subscriber 的具体实现。

例如：

``` text
LiDAR
  │
  │ /points
  ▼
 ┌──────────────┐
 │              │
 ▼              ▼
SLAM        Obstacle Detector
```

典型 Topic：

``` text
/camera/image_raw
/scan
/imu
/odom
/cmd_vel
/tf
```

Topic 特别适合：

-   Camera stream
-   LiDAR point cloud
-   IMU
-   Odometry
-   Detection results
-   Robot state

------------------------------------------------------------------------

## 4. Service：同步请求---响应

Service 更接近传统 RPC。

定义：

$$
y=f(x)
$$

客户端发送 Request：

$$
x
$$

服务器返回 Response：

$$
y
$$

通信结构：

``` text
Client
   │ Request
   ▼
Service Server
   │ Response
   ▼
Client
```

适合：

-   查询地图
-   重置系统
-   切换模式
-   获取机器人状态
-   修改一次性配置

不适合高频传感器数据。

------------------------------------------------------------------------

## 5. Action：长时间任务

Action 用于需要较长时间执行，并且需要反馈和取消的任务。

一个 Action 包含：

``` text
Goal
Feedback
Result
```

例如：

``` text
NavigateToPose
```

执行流程：

``` text
Client
  │
  │ Goal
  ▼
Action Server
  │
  ├── Feedback
  ├── Feedback
  ├── Feedback
  │
  ▼
Result
```

非常适合：

-   Navigation
-   Manipulation
-   Trajectory execution
-   Docking

------------------------------------------------------------------------

## 6. DDS：ROS2 通信的底层基础

ROS1 依赖 ROS Master，而 ROS2 通常建立在 DDS（Data Distribution
Service）之上。

ROS2：

``` text
ROS2 API
   ↓
rclcpp / rclpy
   ↓
rcl
   ↓
rmw
   ↓
DDS
   ↓
UDP / Shared Memory
```

常见 DDS 实现包括：

``` text
Fast DDS
Cyclone DDS
Connext DDS
```

ROS2 节点能够通过 DDS discovery 自动发现彼此。

因此通常不存在 ROS1 中唯一的：

``` text
roscore
```

------------------------------------------------------------------------

## 7. QoS：通信质量控制

ROS2 相比 ROS1 一个非常重要的能力是 QoS（Quality of Service）。

QoS 决定：

-   消息是否可靠
-   是否缓存
-   缓存多少
-   新订阅者能否收到旧消息

### Reliability

Reliable：

$$
P(\text{message delivered}) \approx 1
$$

DDS 会尽量重传。

Best Effort：

``` text
send → lost → ignore
```

不保证消息到达。

对于 Camera / LiDAR 等高频数据，通常：

``` text
BEST_EFFORT
```

对于控制、状态等关键消息，则可能采用：

``` text
RELIABLE
```

### History / Depth

Keep Last：

$$
Q=\{m_{t-k+1},...,m_t\}
$$

其中 $k$ 就是 queue depth。

------------------------------------------------------------------------

# 8. ROS2 参数系统

Node 可以通过 Parameter 配置。

例如：

``` yaml
camera:
  ros__parameters:
    width: 1280
    height: 720
    fps: 30
```

程序：

``` python
self.declare_parameter("width", 1280)
width = self.get_parameter("width").value
```

这样可以实现：

``` text
Algorithm
   +
Robot Profile
```

而不是把机器人参数写死在代码里。

这对于多机器人部署尤其重要。

------------------------------------------------------------------------

# 9. Launch 系统

复杂机器人通常需要同时启动多个节点。

例如：

``` text
Camera
LiDAR
SLAM
Detector
Planner
Controller
```

ROS2 Launch 用于描述系统启动图。

伪代码：

``` python
LaunchDescription([
    Node(
        package="camera_driver",
        executable="camera_node",
        parameters=["camera.yaml"]
    ),

    Node(
        package="detector",
        executable="detector_node"
    )
])
```

推荐结构：

``` text
config/
launch/
src/
scripts/
```

而不是大量手写 shell 命令。

------------------------------------------------------------------------

# 10. TF2：机器人坐标系统

TF2 是 ROS2 中极其重要的基础设施。

机器人通常存在多个坐标系：

``` text
map
 │
 ▼
odom
 │
 ▼
base_link
 │
 ├── camera_link
 └── lidar_link
```

TF 本质上描述刚体变换：

$$
{}^{A}T_B
=
\begin{bmatrix}
R & t\\
0 & 1
\end{bmatrix}
$$

其中：

$$
R\in SO(3)
$$

是旋转矩阵，

$$
t\in\mathbb{R}^3
$$

是平移。

一个点：

$$
p_B=
\begin{bmatrix}
x\\y\\z\\1
\end{bmatrix}
$$

转换到 A：

$$
p_A={}^AT_Bp_B
$$

------------------------------------------------------------------------

# 11. 旋转矩阵

二维旋转：

$$
R(\theta)=
\begin{bmatrix}
\cos\theta&-\sin\theta\\
\sin\theta&\cos\theta
\end{bmatrix}
$$

三维旋转通常由：

``` text
Roll
Pitch
Yaw
```

表示。

例如：

$$
R=R_z(\psi)R_y(\theta)R_x(\phi)
$$

------------------------------------------------------------------------

# 12. 四元数

ROS 中姿态经常使用 Quaternion：

$$
q=(x,y,z,w)
$$

单位四元数满足：

$$
x^2+y^2+z^2+w^2=1
$$

绕单位轴：

$$
\mathbf{u}=(u_x,u_y,u_z)
$$

旋转 $\theta$：

$$
q=
\left(
u_x\sin\frac{\theta}{2},
u_y\sin\frac{\theta}{2},
u_z\sin\frac{\theta}{2},
\cos\frac{\theta}{2}
\right)
$$

相比 Euler Angle，Quaternion 可以避免 Gimbal Lock。

------------------------------------------------------------------------

# 13. Odometry

Odometry 描述机器人局部运动状态：

$$
\mathbf{x}_t=
[x_t,y_t,\theta_t]^T
$$

移动机器人简单运动模型：

$$
x_{t+1}=x_t+v_t\cos\theta_t\Delta t
$$

$$
y_{t+1}=y_t+v_t\sin\theta_t\Delta t
$$

$$
\theta_{t+1}=\theta_t+\omega_t\Delta t
$$

其中：

-   $v$：线速度
-   $\omega$：角速度

ROS2 中常见：

``` text
/odom
```

类型：

``` text
nav_msgs/msg/Odometry
```

------------------------------------------------------------------------

# 14. map / odom / base_link 的区别

这是 ROS Navigation 中非常重要的概念。

### base_link

机器人自身坐标系。

``` text
x → forward
y → left
z → up
```

### odom

局部连续坐标系。

特点：

-   连续
-   短期稳定
-   长期可能 drift

### map

全局坐标系。

特点：

-   全局一致
-   可以被 localization 修正

典型 TF：

``` text
map
 ↓
odom
 ↓
base_link
```

因此：

$$
{}^{map}T_{base}
=
{}^{map}T_{odom}
{}^{odom}T_{base}
$$

------------------------------------------------------------------------

# 15. Sensor Fusion

机器人通常同时使用：

``` text
LiDAR
IMU
Wheel Encoder
Camera
GNSS
```

目标是估计状态：

$$
x_t=
[p_t,v_t,q_t,b_t]
$$

常见方法：

-   EKF
-   UKF
-   Factor Graph
-   Visual-Inertial Odometry
-   LiDAR-Inertial Odometry

------------------------------------------------------------------------

# 16. EKF 基本原理

状态预测：

$$
x_k^-=f(x_{k-1},u_k)
$$

协方差：

$$
P_k^-=F_kP_{k-1}F_k^T+Q_k
$$

观测：

$$
z_k=h(x_k)+v_k
$$

Kalman Gain：

$$
K_k=P_k^-H_k^T
(H_kP_k^-H_k^T+R_k)^{-1}
$$

状态更新：

$$
x_k=x_k^-+K_k(z_k-h(x_k^-))
$$

------------------------------------------------------------------------

# 17. ROS2 Navigation 数据流

一个典型 Navigation Stack：

``` text
Sensors
   ↓
Localization
   ↓
Robot Pose
   ↓
Global Planner
   ↓
Global Path
   ↓
Local Planner
   ↓
Velocity Command
   ↓
Controller
   ↓
Robot
```

可以进一步写成：

``` text
Camera ─────┐
LiDAR ──────┼──> Perception
IMU ────────┤
Encoder ────┘
                │
                ▼
           Localization
                │
                ▼
             Planner
                │
                ▼
            Controller
                │
                ▼
             /cmd_vel
```

------------------------------------------------------------------------

# 18. PID 控制

经典 PID：

$$
e(t)=r(t)-y(t)
$$

控制量：

$$
u(t)
=
K_pe(t)
+
K_i\int_0^t e(\tau)d\tau
+
K_d\frac{de(t)}{dt}
$$

离散形式：

$$
u_k=
K_pe_k+
K_i\sum_{i=0}^{k}e_i\Delta t+
K_d\frac{e_k-e_{k-1}}{\Delta t}
$$

伪代码：

``` python
error = target - current

integral += error * dt

derivative = (error - prev_error) / dt

control = (
    kp * error
    + ki * integral
    + kd * derivative
)

prev_error = error
```

------------------------------------------------------------------------

# 19. URDF

URDF 用于描述机器人结构。

机器人可以表示为一个树：

``` text
base_link
   │
   ├── lidar_link
   │
   ├── camera_link
   │
   └── wheel_link
```

每个 Joint 定义：

``` text
parent
child
origin
axis
type
```

机器人运动学关系可以通过 TF 发布。

------------------------------------------------------------------------

# 20. Xacro

Xacro 是 URDF 的宏系统。

例如：

``` xml
<xacro:property name="wheel_radius" value="0.1"/>
```

可以避免大量重复 XML。

------------------------------------------------------------------------

# 21. rosbag2

rosbag2 可以记录 Topic：

``` text
Camera
LiDAR
IMU
Odometry
TF
```

形成可重放数据集。

典型用途：

``` text
Real Robot
   ↓
rosbag
   ↓
Offline Replay
   ↓
Algorithm Development
```

这对于机器人算法调试极其重要。

------------------------------------------------------------------------

# 22. Lifecycle Node

普通 Node：

``` text
start
 ↓
running
```

Lifecycle Node：

``` text
Unconfigured
      ↓
Inactive
      ↓
Active
      ↓
Finalized
```

这样可以控制：

-   初始化
-   激活
-   停止
-   清理

对于真实机器人尤其重要。

------------------------------------------------------------------------

# 23. 实物机器人推荐软件架构

建议将机器人系统拆成：

``` text
robot_project/
│
├── config/
│   ├── robot.yaml
│   ├── camera.yaml
│   └── lidar.yaml
│
├── launch/
│
├── perception/
│
├── localization/
│
├── planning/
│
├── control/
│
├── scripts/
│
└── docs/
```

进一步将硬件配置抽象成 Robot Profile：

``` text
Algorithm
    │
    ▼
Robot Interface
    │
    ▼
Robot Profile
    │
    ├── Robot A
    ├── Robot B
    └── Robot C
```

这样算法本身不依赖具体硬件。

------------------------------------------------------------------------

# 24. 实物部署安全原则

真实机器人系统中建议严格区分：

``` text
Perception
Planning
Control
```

特别是：

``` text
Perception-only
      ↓
Dry-run Planning
      ↓
Test Waypoint
      ↓
Real Controller
```

不要直接：

``` text
AI Model
   ↓
/cmd_vel
```

而应该：

``` text
AI Policy
   ↓
Validated Action / Waypoint
   ↓
Safety Layer
   ↓
Local Controller
   ↓
Robot
```

------------------------------------------------------------------------

# 25. ROS2 调试思路

推荐从通信图开始：

``` text
Node
 ↓
Topic exists?
 ↓
Publisher exists?
 ↓
Message arrives?
 ↓
QoS compatible?
 ↓
frame correct?
 ↓
timestamp correct?
 ↓
data numerically correct?
```

常用检查对象：

``` text
ros2 node list
ros2 topic list
ros2 topic info
ros2 topic echo
ros2 topic hz
ros2 param list
ros2 param get
ros2 service list
ros2 action list
```

------------------------------------------------------------------------

# 26. 总结

理解 ROS2 时，不应该只记命令，而应该建立下面这套系统模型：

``` text
                    ROS2
                     │
       ┌─────────────┼─────────────┐
       │             │             │
   Communication   Geometry     Execution
       │             │             │
 Topic/Service      TF2          Launch
 Action/QoS       URDF         Lifecycle
       │             │             │
       └─────────────┼─────────────┘
                     │
                     ▼
              Robot System
                     │
       ┌─────────────┼─────────────┐
       ↓             ↓             ↓
   Perception   Localization    Planning
                                  │
                                  ▼
                               Control
                                  │
                                  ▼
                                Robot
```

真正掌握 ROS2 的关键是理解：

1.  **Node 如何组织机器人软件**
2.  **DDS / Topic / QoS 如何传递数据**
3.  **TF2 如何建立空间关系**
4.  **Localization 如何产生机器人状态**
5.  **Planner 如何生成运动目标**
6.  **Controller 如何把目标变成实际控制**
7.  **Launch / Parameter / Profile 如何让系统可复现和可迁移**
8.  **rosbag / logging 如何支持离线调试**
9.  **真实机器人中如何建立安全边界**

ROS2
本身不是导航算法或控制算法，而是把这些模块组织成一个可靠、可组合、可部署机器人系统的基础设施。
