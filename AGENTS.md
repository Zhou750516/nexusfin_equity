# NexusFin - nexusfin-equity (艾博生)

## 项目概述
惠聚项目（NexusFin），本仓库为艾博生权益分发服务。
Java 17 + Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0

## 目录结构
src/main/java/com/nexusfin/equity/
├── controller/    # REST API 入口
├── service/       # 业务逻辑层
├── service/impl/  # 业务实现
├── mapper/        # MyBatis Mapper
├── model/
│   ├── entity/    # 数据库实体
│   ├── dto/       # 数据传输对象
│   ├── vo/        # 视图对象
│   └── enums/     # 枚举类
├── config/        # 配置类
├── common/        # 公共工具
└── integration/   # 外部接口调用（齐为、云卡）

## 构建与运行
- 构建: mvn clean package -DskipTests
- 运行: java -jar target/nexusfin-equity.jar
- 测试: mvn test
- 代码检查: mvn checkstyle:check

## 编码规范
- 所有 REST 接口统一返回 Result<T> 包装类
- 接口路径不包含版本号，如 /api/equity/benefit/create
- 数据库字段使用下划线命名，Java 属性使用驼峰命名
- 所有金额字段使用 Long 类型，单位为分
- 敏感字段（身份证、手机号）必须加密存储，查询使用 hash 索引
- 异常统一通过 GlobalExceptionHandler 处理
- 日志使用 SLF4J，关键业务节点必须打印 traceId + bizOrderNo

## 禁止事项
- 禁止在 Controller 层写业务逻辑
- 禁止硬编码配置值，必须使用 @Value 或 @ConfigurationProperties
- 禁止使用 System.out.println
- 禁止在循环中进行数据库查询
- 禁止捕获 Exception 后不做任何处理

## 完成标准
- 代码编译通过，无 checkstyle 告警
- 单元测试覆盖核心业务方法
- 接口参数有 @Valid 校验注解
