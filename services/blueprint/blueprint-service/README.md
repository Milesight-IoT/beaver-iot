# 蓝图组件 (Blueprint Component)

## 概述

蓝图组件是一个用于描述资源间组织/依赖关系，并指导资源创建的模板系统。它类似于AWS的CloudFormation，允许用户通过模板文件来定义和管理复杂的资源部署。

## 核心特性

- **模板驱动**: 使用Pebble模板引擎和YAML格式定义蓝图
- **资源管理**: 支持Entity、Dashboard、Workflow等多种资源类型
- **依赖解析**: 自动处理资源间的依赖关系
- **函数支持**: 提供运行时函数用于动态计算属性值
- **状态管理**: 完整的蓝图生命周期管理

## 架构设计

### 核心组件

1. **蓝图节点体系**
   - `BlueprintNode`: 蓝图节点基础接口
   - `OrganizationNode`: 组织节点（TemplateNode、ResourceNode等）
   - `DataNode`: 数据节点（ValueNode、FunctionNode等）

2. **模板解析器**
   - `TemplateParser`: 模板解析接口
   - `BlueprintNodeConstructor`: 节点构建器

3. **部署器**
   - `Deployer`: 蓝图部署器
   - `ResourceManager`: 资源管理器
   - `FunctionExecutor`: 函数执行器

4. **服务层**
   - `BlueprintService`: 蓝图核心服务
   - `BlueprintServiceProvider`: 服务提供者接口

## 使用方法

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.milesight.beaveriot</groupId>
    <artifactId>blueprint-service</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. 启用蓝图组件

```java
@SpringBootApplication
@Import(BlueprintConfiguration.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 使用蓝图服务

```java
@Autowired
private BlueprintServiceProvider blueprintServiceProvider;

// 创建模板加载器
TemplateLoader templateLoader = templatePath -> {
    // 加载模板文件的逻辑
    return inputStream;
};

// 部署蓝图
Map<String, JsonNode> variables = Map.of(
    "entityName", objectMapper.valueToTree("test entity")
);
Blueprint blueprint = blueprintServiceProvider.deployBlueprint(templateLoader, variables);
```

## 模板文件格式

### 1. 变量定义 (variables.peb)

```yaml
version: 1
type: variables
properties:
  entityName: 
    type: string
    default: 'custom entity 1'
```

### 2. 常量定义 (constants.peb)

```yaml
version: 1
type: constants
values:
  tz:
    'UTC': 0
    'Asia/Shanghai': 8
  example_value: 123
```

### 3. 主模板 (index.peb)

```yaml
version: 1
type: template
resources:
  customEntity1:
    type: entity
    integration: system
    name: {{Variables.entityName}}
  customEntity2:
    type: entity
    integration: system
    name: !Concat
      - 'Node: '
      - !GetAttr 'stacks.workflow1.resources.workflow1.node1.id'
imports:
  workflow1:
    template: workflow.peb
    parameters:
      entity1: !GetAttr 'resources.customEntity1.id'
```

## 支持的函数

- `!GetAttr`: 获取资源属性
- `!Concat`: 字符串拼接
- `!TryResolveResourceId`: 尝试解析资源ID

## 数据库表结构

### t_blueprint (蓝图表)
- `id`: 蓝图ID
- `tenant_id`: 租户ID
- `description`: 蓝图描述
- `chart`: 序列化的蓝图节点树
- `status`: 蓝图状态
- `created_at`, `created_by`, `updated_at`, `updated_by`: 审计字段

### t_blueprint_resource (蓝图资源索引表)
- `id`: 资源索引ID
- `resource_type`: 资源类型
- `resource_id`: 资源ID
- `blueprint_id`: 蓝图ID
- `tenant_id`: 租户ID
- `managed`: 是否由蓝图管理
- `created_at`, `created_by`: 审计字段

## 扩展开发

### 添加新的资源类型

1. 实现 `ResourceManager` 接口
2. 注册为Spring Bean
3. 在模板中使用对应的资源类型

### 添加新的函数

1. 创建函数节点类继承 `AbstractFunctionNode`
2. 实现 `FunctionExecutor` 接口
3. 注册为Spring Bean

## 注意事项

1. 模板文件必须使用 `.peb` 扩展名
2. 所有模板文件必须符合YAML和Pebble语法
3. 资源间依赖关系会自动解析，避免循环依赖
4. 蓝图状态变更会记录在数据库中
5. 删除蓝图时会同时删除所有由蓝图创建的资源

## 故障排除

### 常见问题

1. **模板解析失败**: 检查YAML语法和Pebble模板语法
2. **资源创建失败**: 检查资源管理器的实现和配置
3. **依赖解析失败**: 检查资源间的依赖关系是否正确

### 日志配置

```yaml
logging:
  level:
    com.milesight.beaveriot.blueprint: DEBUG
```
