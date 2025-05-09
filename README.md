# AI衣搭 - 智能穿搭推荐应用

## 项目概述
AI衣搭是一款基于AI技术的智能穿衣搭配推荐应用，为用户提供个性化的着装建议和风格指导。应用整合了用户的服装库、身材数据和场景需求，通过智能算法生成最适合的穿搭方案，同时融合社区互动功能，让用户分享、评论穿搭成果，并在模拟4G/5G网络环境下提供高效的通信体验。

## 项目背景
随着人们对个人形象和穿着搭配的重视，传统的穿搭方式往往费时费力且缺乏专业建议。AI衣搭旨在解决用户的"穿什么"难题，同时通过社区功能促进用户间交流，实现移动通信课程设计的实践要求，构建高性能SIP应用平台。

## 项目管理方法论
采用敏捷开发框架(Scrum)结合DevOps实践，实现持续集成/持续部署(CI/CD)流程。每个迭代周期(Sprint)为2周，通过每日站会(Daily Scrum)、迭代计划会议(Sprint Planning)、迭代评审(Sprint Review)和迭代回顾(Sprint Retrospective)确保项目的顺利进行。

## 产品愿景
打造一款领先的AI驱动的个人穿搭助手，通过技术创新解决用户日常穿搭决策问题，同时构建活跃的时尚社区生态系统，实现用户价值与商业价值的双赢。

## 项目结构
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/aioutfitapp/
│   │   │   ├── activity/                - 活动界面
│   │   │   │   ├── MainActivity.java    - 主界面
│   │   │   │   ├── LoginActivity.java   - 登录界面
│   │   │   │   ├── ProfileActivity.java - 个人主页
│   │   │   │   ├── CallActivity.java    - 通话界面
│   │   │   │   └── ...
│   │   │   ├── adapter/                 - 适配器
│   │   │   │   ├── OutfitCardAdapter.java - 搭配卡片适配器
│   │   │   │   └── ...
│   │   │   ├── fragment/                - 片段
│   │   │   ├── model/                   - 数据模型
│   │   │   ├── network/                 - 网络请求
│   │   │   │   ├── SIPManager.java      - SIP协议管理
│   │   │   │   ├── WebRTCHelper.java    - WebRTC通信实现
│   │   │   │   ├── CallManager.java     - 通话管理服务
│   │   │   │   ├── SignalingService.java - 信令服务
│   │   │   │   ├── NetworkSimulator.java - 网络模拟器
│   │   │   │   └── ...
│   │   │   ├── utils/                   - 工具类
│   │   │   └── ai/                      - AI算法
│   │   ├── res/
│   │   │   ├── drawable/                - 矢量图标和资源
│   │   │   ├── drawable-xxhdpi/         - 高分辨率图片资源
│   │   │   ├── layout/                  - 界面布局文件
│   │   │   │   ├── activity_main.xml    - 主界面布局
│   │   │   │   ├── activity_login.xml   - 登录界面布局
│   │   │   │   ├── profile.xml          - 个人主页布局
│   │   │   │   ├── activity_call.xml    - 通话界面布局
│   │   │   │   ├── item_outfit_card.xml - 搭配卡片布局
│   │   │   │   ├── item_outfit_record.xml - 穿搭记录布局
│   │   │   │   └── ...
│   │   │   ├── menu/                    - 菜单配置文件
│   │   │   ├── values/                  - 应用配置值（颜色、字符串等）
│   │   │   ├── xml/                     - XML配置文件
│   │   │   │   └── network_security_config.xml - 网络安全配置
│   │   │   └── ...
│   ├── server/                          - 服务器端代码
│   │   ├── sip/                         - SIP服务实现
│   │   ├── api/                         - 后端API
│   │   └── ai/                          - AI算法服务
```

## SIP与WebRTC实现状态

### SIP功能状态
- **实现进度**：基础框架已完成
- **主要组件**：SIPManager.java、CallManager.java、IncomingCallReceiver.java
- **功能完整性**：
  - 已实现SIP注册、呼叫、应答和终止等基本功能
  - 已集成通话状态监听与回调机制
  - 已实现麦克风、扬声器控制
- **待完善项**：
  - 需要配置实际SIP服务器地址和账号信息
  - 需要处理Android 12+废弃SIP API的兼容性问题
  - 实际通话质量和延迟优化

### WebRTC功能状态
- **实现进度**：基础框架已完成
- **主要组件**：WebRTCHelper.java、SignalingService.java、CallManager.java
- **功能完整性**：
  - 已实现PeerConnection创建与管理
  - 已实现视频采集、渲染与传输
  - 已集成信令服务与ICE服务器配置
  - 已实现摄像头切换、视频开关等功能
- **待完善项**：
  - 需要配置实际WebRTC信令服务器地址
  - 需要完善实际环境下的网络传输优化
  - 网络穿透(ICE、STUN、TURN)服务配置

### 通信综合管理
- **实现进度**：基础框架已完成
- **主要组件**：CallManager.java、CallActivity.java
- **功能完整性**：
  - 已整合SIP和WebRTC功能，提供统一的通话管理接口
  - 已实现音视频通话UI界面
  - 已集成网络质量监测基础功能
  - 已实现从主界面到通话界面的导航功能
- **待完善项**：
  - 实际服务器环境下的通话测试与调优
  - 复杂网络条件下的传输稳定性优化
  - 通话录制与回放等高级功能

## 网络安全配置
- **功能说明**：已配置Android网络安全策略，允许HTTP明文通信到指定的本地开发服务器。
- **配置文件**：`app/src/main/res/xml/network_security_config.xml`
- **支持地址**：
  - 10.0.2.2（Android模拟器访问主机的地址）
  - 10.0.0.2（真机测试时的本地服务器地址）
  - localhost（本地回环地址）
- **使用方法**：
  - 模拟器测试：使用默认10.0.2.2地址（已在ApiClient中配置）
  - 真机测试：确保手机与开发电脑在同一网络，并使用开发电脑的实际IP地址
- **注意事项**：
  - 在`AndroidManifest.xml`中通过`android:networkSecurityConfig`属性引用
  - Android 9.0（API 28）及以上版本，HTTP明文通信默认被禁止，必须使用此配置
  - 生产环境应移除此配置，全部使用HTTPS加密通信

## 功能列表

### 已完成功能
- [x] 登录/注册界面设计与实现
- [x] 用户认证基本流程
- [x] 主界面布局设计与实现
- [x] 搭配推荐卡片UI组件
- [x] 底部导航栏实现
- [x] 应用图标与基础资源
- [x] 个人主页布局设计与实现
  - [x] 用户资料展示区域
  - [x] 风格标签系统
  - [x] 身材数据展示
  - [x] 穿搭记录列表
  - [x] 相关功能入口
- [x] 主页到个人主页的导航功能
- [x] 通信功能基础框架
  - [x] SIP音频通话框架实现
  - [x] WebRTC视频通话框架实现
  - [x] 网络模拟器实现
  - [x] 通话活动界面实现
  - [x] 信令服务框架实现
  - [x] 主界面通话入口功能实现
- [x] 网络安全配置（支持HTTP明文通信）

### 待完成功能
#### 核心功能
- [ ] 用户管理系统
  - [ ] 个人信息维护（身材数据、风格偏好等）
  - [ ] 用户认证与授权
  - [ ] 用户设置管理

- [ ] 虚拟衣柜管理
  - [ ] 添加服装功能（拍照或从相册选择）
  - [ ] 服装分类与标签
  - [ ] 服装属性编辑（颜色、款式、季节等）
  - [ ] 服装库浏览与搜索

- [ ] AI搭配推荐系统
  - [ ] 基于用户特征的推荐算法
  - [ ] 场景化穿搭建议（约会、工作、休闲等）
  - [ ] 流行趋势分析与推荐
  - [ ] 个性化风格建议
  - [ ] 天气适应性穿搭推荐

- [ ] 社区互动功能
  - [ ] 穿搭分享与展示
  - [ ] 点赞与评论系统
  - [ ] 用户关注机制
  - [ ] 热门穿搭排行榜
  - [ ] 私信交流功能

- [ ] 移动通信网络功能（框架已实现，需要完善）
  - [ ] SIP服务器配置与连接
  - [ ] WebRTC信令服务器配置与连接
  - [ ] 优化音视频质量与稳定性
  - [ ] 完善通信加密与安全措施
  - [ ] 实时通话功能的实际环境测试

- [ ] 其他辅助功能
  - [ ] 购物清单管理
  - [ ] 穿搭日历
  - [ ] 穿搭建议历史记录
  - [ ] 离线模式支持

## 项目配置信息

### 版本控制配置
项目使用Git进行版本控制，已配置.gitignore文件忽略以下内容：
- Android构建输出文件（APK、AAB等）
- Java类文件与日志
- Gradle构建目录
- 本地配置文件
- IDE生成的文件（Android Studio/IntelliJ IDEA）
- 密钥与证书文件
- 各操作系统特定的临时文件
- 前端Node.js相关依赖与日志

这确保了代码库的整洁与安全，避免了敏感信息、二进制文件与临时文件的提交。

## 技术架构
- **前端**：
  - Java Android原生开发
  - Material Design UI组件
  - 矢量图标与动画
  - ViewPager2 + CardView实现卡片滑动效果
  - CircleImageView实现圆形头像
  - ConstraintLayout高性能界面布局
  - Retrofit2 + OkHttp3网络请求
  - Glide图片加载与缓存
  - Room数据持久化

- **后端**：
  - SpringBoot框架
  - MySQL数据库
  - Redis缓存
  - SIP协议栈实现
  - RESTful API设计
  - JWT认证

- **AI技术**：
  - TensorFlow Lite移动端推理
  - 服装特征提取
  - 风格匹配算法
  - 个性化推荐模型

- **通信技术**：
  - SIP协议实现
  - WebRTC实时通信
  - 4G/5G网络模拟器
  - 网络传输优化策略

## 迭代开发计划

### 迭代0 (Sprint 0): 项目启动与环境搭建 (1周) - 已完成
**目标**: 确立项目基础架构和开发环境
- **任务**:
  - T0.1: 需求分析与用例图绘制
  - T0.2: 技术栈选型与论证
  - T0.3: 开发环境配置与代码库初始化
  - T0.4: CI/CD流程设计与工具链配置
  - T0.5: 架构设计文档编写
- **交付物**:
  - 需求规格说明书(SRS)
  - 系统架构设计文档(SAD)
  - 初始化的代码仓库
  - 开发环境配置指南
  - 自动化构建流程

### 迭代1 (Sprint 1): 用户认证系统开发 (2周) - 已完成
**目标**: 实现基础用户认证功能和UI框架
- **任务**:
  - T1.1: 用户数据模型设计与实现
  - T1.2: 数据库Schema设计与版本控制
  - T1.3: 用户注册与登录API实现
  - T1.4: 登录界面UI组件开发
  - T1.5: JWT认证机制实现
  - T1.6: 用户信息安全存储机制
  - T1.7: 单元测试与集成测试编写
- **交付物**:
  - 用户认证微服务
  - 登录/注册界面
  - 数据库迁移脚本
  - API文档
  - 测试报告

### 迭代2 (Sprint 2): 虚拟衣柜核心功能 (2周) - 进行中
**目标**: 实现虚拟衣柜的基础CRUD功能
- **任务**:
  - T2.1: 服装数据模型设计
  - T2.2: 服装分类体系设计
  - T2.3: 服装CRUD API实现
  - T2.4: 图片上传与处理服务
  - T2.5: 虚拟衣柜界面UI组件
  - T2.6: 衣物管理功能实现
  - T2.7: 本地数据缓存机制
  - T2.8: 性能测试与优化
- **交付物**:
  - 服装管理微服务
  - 虚拟衣柜界面
  - 图片处理服务
  - API文档更新
  - 性能测试报告

### 迭代3 (Sprint 3): AI模型集成与推荐系统 (3周) - 未开始
**目标**: 实现基础AI推荐功能
- **任务**:
  - T3.1: 服装特征提取算法研发
  - T3.2: 用户偏好建模系统设计
  - T3.3: 推荐算法选型与实现
  - T3.4: 模型训练流程设计
  - T3.5: 模型评估指标确定
  - T3.6: 服务端推理API实现
  - T3.7: 客户端推荐结果展示界面
  - T3.8: A/B测试框架搭建
- **交付物**:
  - 推荐系统微服务
  - 模型训练流程
  - 推荐结果展示界面
  - 模型评估报告
  - A/B测试基础设施

### 迭代4 (Sprint 4): SIP服务平台与网络通信 (3周) - 框架已实现，细节待完善
**目标**: 构建SIP通信基础设施
- **任务**:
  - T4.1: SIP协议栈选型与设计 - 已完成
  - T4.2: SIP服务器架构设计 - 已完成
  - T4.3: 用户注册与会话管理实现 - 已完成
  - T4.4: 4G/5G网络模拟器开发 - 已完成
  - T4.5: 网络质量监测系统 - 基础实现
  - T4.6: 客户端SIP协议集成 - 已完成
  - T4.7: 通信安全机制实现 - 待完善
  - T4.8: 负载测试与容错设计 - 待完成
  - T4.9: 主界面通话功能入口集成 - 已完成
- **交付物**:
  - SIP通信服务 - 框架已完成
  - 网络模拟器 - 已完成
  - 通信协议文档 - 部分完成
  - 网络性能监测面板 - 基础实现
  - 负载测试报告 - 待完成
  - 通话功能入口 - 已完成

### 迭代5 (Sprint 5): 社区互动功能 (2周) - 未开始
**目标**: 实现基础社区功能
- **任务**:
  - T5.1: 社区数据模型设计
  - T5.2: 内容发布与展示API
  - T5.3: 社交互动API（点赞、评论）
  - T5.4: 内容推荐算法实现
  - T5.5: 社区界面UI组件开发
  - T5.6: 内容审核机制设计
  - T5.7: 用户权限管理系统
  - T5.8: 性能优化与缓存策略
- **交付物**:
  - 社区微服务
  - 社区界面
  - 内容审核系统
  - 推荐算法文档
  - 性能报告

## 项目进展

### 当前完成进度
- 基础UI设计与实现: 75%
- 用户认证系统: 30%
- 虚拟衣柜功能: 10%
- AI推荐系统: 5%
- 社区互动功能: 5%
- SIP服务平台: 15%

### 开发进度跟踪
- **2025-03-22**: 完成项目基础架构搭建和登录界面设计
- **2025-03-23**: 完成主界面布局、卡片组件和底部导航实现
- **2025-03-27**: 完成个人主页布局设计及功能实现
- **2025-04-05**: 完成社区页面基础布局及功能框架
- **2025-04-12**: 完成通话功能入口及界面跳转功能实现
- **计划中**: 虚拟衣柜开发、AI算法集成、SIP与WebRTC服务优化

### 下一步计划 
1. 完善SIP和WebRTC实现
   - 配置实际服务器地址
   - 优化音视频质量和网络传输
   - 完善通信加密和安全措施
   - 实现实际环境测试

2. 虚拟衣柜功能开发
   - 设计并实现服装数据模型（服装类型、属性、季节、品牌等）
   - 开发服装添加界面（拍照/从相册选择）
   - 实现服装分类与标签系统
   - 开发服装列表和详情页面

## 质量保证计划

### 代码质量保证
- 代码规范遵循Google Java编程规范
- 静态代码分析工具：SonarQube
- 代码审查要求：每个PR至少需要2名团队成员审查通过
- 单元测试覆盖率要求：核心业务逻辑≥90%，其他模块≥80%
- 集成测试覆盖率要求：≥75%

### 性能目标
- 应用启动时间：冷启动≤3秒，热启动≤1秒
- API响应时间：P95≤200ms
- 图片加载时间：P95≤500ms
- 内存占用：峰值≤150MB
- 电池消耗：每小时≤5%

### 安全保障措施
- 数据传输全程HTTPS加密
- 用户密码采用PBKDF2算法存储
- 敏感数据加密存储
- 定期安全审计与渗透测试
- 遵循OWASP Top 10防护原则

## 风险管理矩阵

| 风险ID | 风险描述 | 概率(1-5) | 影响(1-5) | 风险值 | 缓解策略 | 责任人 |
|--------|----------|-----------|-----------|---------|----------|--------|
| R-01 | 推荐算法准确度不足 | 4 | 5 | 20 | 多渠道收集用户反馈，建立模型评估指标，预留模型替换接口 | AI团队负责人 |
| R-02 | SIP通信稳定性问题 | 3 | 5 | 15 | 采用成熟SIP框架，构建完善的监控系统，设计故障转移机制 | 网络团队负责人 |
| R-03 | 用户隐私数据泄露 | 2 | 5 | 10 | 实施严格访问控制策略，数据脱敏处理，定期安全审计 | 安全团队负责人 |
| R-04 | 系统扩展性瓶颈 | 3 | 4 | 12 | 采用微服务架构，设计水平扩展能力，预留性能优化空间 | 架构师 |
| R-05 | 用户采纳率低于预期 | 3 | 4 | 12 | 提前进行用户研究，建立用户反馈渠道，灵活调整功能优先级 | 产品经理 |

## 技术债务管理
建立技术债务跟踪机制，每个迭代预留20%时间用于技术债务清理。定期进行代码重构与架构优化，确保系统持续演进能力。

## 项目成功指标
- 用户留存率：30天留存≥40%
- 日活跃用户增长率：≥5%/月
- 用户推荐满意度：≥4.2/5分
- 社区内容生产活跃度：人均月发布内容≥3条
- 应用商店评分：≥4.5/5分