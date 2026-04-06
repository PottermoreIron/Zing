---
name: ddd-java
description: "DDD 架构最佳实践：创建聚合根、值对象、仓储接口、应用服务、领域事件、出站端口。Use when modeling aggregates, value objects, repositories, application services, DDD refactoring, or domain behavior. 触发词：聚合、值对象、仓储、领域服务、应用服务、DDD、聚合根、领域事件、aggregate、value object、repository、application service、port、assembler。"
argument-hint: "描述你要创建或重构的领域概念（如：创建 Order 聚合，给 Member 添加积分行为）"
---

# DDD Java 架构最佳实践

## 分层结构速查

```
com.pot.{module}.service/
├── domain/
│   ├── model/{aggregate}/      ← 聚合根、值对象（无 Spring 注解）
│   ├── repository/             ← 仓储接口（interface，领域层定义）
│   ├── service/                ← 领域服务（复杂跨聚合逻辑）
│   ├── event/                  ← 领域事件 record/class
│   └── port/                   ← 出站端口（PasswordEncoder、IdGenerator）
├── application/
│   ├── service/                ← 应用服务（@Service，编排，@Transactional）
│   ├── command/                ← 写操作入参（record 或 class）
│   ├── query/                  ← 读操作入参
│   ├── dto/                    ← 输出 DTO（record 或 class）
│   └── assembler/              ← 领域对象 ↔ DTO 转换
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/             ← MyBatis-Plus PO（@TableName）
│   │   ├── mapper/             ← Mapper 接口（extends BaseMapper<PO>）
│   │   └── repository/        ← 仓储实现（implements 领域仓储接口）
│   ├── config/
│   ├── event/                  ← 事件发布适配器（RabbitMQ 等）
│   └── id/                     ← ID 生成适配器
└── interfaces/
    ├── rest/                   ← REST Controller
    └── rest/internal/          ← 内部 Feign 实现
```

## 聚合根模板

```java
// domain/model/{aggregate}/{Name}Aggregate.java
@Getter
public class MemberAggregate {

    private MemberId id;
    private Nickname nickname;
    // ... 其他字段

    private final List<MemberDomainEvent> domainEvents = new ArrayList<>();

    private MemberAggregate() {}

    /** 创建新聚合（写操作） */
    public static MemberAggregate create(Nickname nickname, Email email, String passwordHash) {
        MemberAggregate agg = new MemberAggregate();
        agg.nickname = nickname;
        agg.email = email;
        agg.passwordHash = passwordHash;
        agg.status = MemberStatus.ACTIVE;
        // 产生领域事件
        agg.domainEvents.add(new MemberDomainEvent.Registered(agg.id));
        return agg;
    }

    /** 从持久化层重建（读操作，不产生事件） */
    public static MemberAggregate reconstitute(MemberId id, Nickname nickname, ...) {
        MemberAggregate agg = new MemberAggregate();
        agg.id = id;
        // ... 赋值
        return agg;
    }

    /** 业务行为：产生事件 */
    public void changePassword(String newHash, PasswordEncoder encoder) {
        this.passwordHash = encoder.encode(newHash);
        domainEvents.add(new MemberDomainEvent.PasswordChanged(this.id));
    }

    public List<MemberDomainEvent> pullDomainEvents() {
        List<MemberDomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}
```

## 值对象模板

```java
// 不可变，静态工厂方法 of(...)，包含校验
public record Email(String value) {
    public static Email of(String value) {
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("无效邮箱: " + value);
        }
        return new Email(value.trim().toLowerCase());
    }
}
```

## 仓储接口模板（领域层）

```java
// domain/repository/MemberRepository.java
public interface MemberRepository {
    void save(MemberAggregate member);
    Optional<MemberAggregate> findById(MemberId id);
    Optional<MemberAggregate> findByEmail(Email email);
    boolean existsByEmail(Email email);
}
```

## 应用服务模板

```java
// application/service/MemberApplicationService.java
@Service
@RequiredArgsConstructor
public class MemberApplicationService {

    private final MemberRepository memberRepository;
    private final MemberIdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventPublisher eventPublisher;
    private final MemberAssembler assembler;

    @Transactional
    public MemberDTO register(RegisterMemberCommand cmd) {
        // 1. 前置校验（唯一性等）
        if (memberRepository.existsByEmail(Email.of(cmd.email()))) {
            throw new IllegalStateException("邮箱已注册");
        }
        // 2. 创建聚合根
        MemberAggregate member = MemberAggregate.create(
            Nickname.of(cmd.nickname()),
            Email.of(cmd.email()),
            passwordEncoder.encode(cmd.password())
        );
        // 3. 持久化
        memberRepository.save(member);
        // 4. 发布领域事件
        member.pullDomainEvents().forEach(eventPublisher::publish);
        // 5. 返回 DTO
        return assembler.toDTO(member);
    }
}
```

## 仓储实现模板（基础设施层）

```java
// infrastructure/persistence/repository/MemberRepositoryImpl.java
@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberMapper memberMapper;

    @Override
    public void save(MemberAggregate member) {
        Member po = toPO(member);
        if (memberMapper.selectById(po.getId()) == null) {
            memberMapper.insert(po);
        } else {
            memberMapper.updateById(po);
        }
    }

    @Override
    public Optional<MemberAggregate> findById(MemberId id) {
        Member po = memberMapper.selectById(id.value());
        return Optional.ofNullable(po).map(this::toDomain);
    }

    // PO → 领域对象
    private MemberAggregate toDomain(Member po) {
        return MemberAggregate.reconstitute(
            MemberId.of(po.getMemberId()),
            Nickname.of(po.getNickname()),
            // ...
        );
    }

    // 领域对象 → PO
    private Member toPO(MemberAggregate agg) {
        Member po = new Member();
        po.setMemberId(agg.getId().value());
        po.setNickname(agg.getNickname().value());
        // ...
        return po;
    }
}
```

## 关键规则清单

| 规则          | ✅ 正确                                | ❌ 错误                  |
| ------------- | -------------------------------------- | ------------------------ |
| 聚合根继承    | 不继承任何类                           | extends BaseEntity       |
| 聚合根创建    | `create()` / `reconstitute()` 工厂方法 | `new` + setter           |
| domain 层依赖 | 只依赖 domain 内部 + Lombok            | Spring @Service、MyBatis |
| ID 生成       | ID 生成器创建                          | 数据库自增               |
| 用户名字段    | `nickname` 显示名                      | `username`（已弃用）     |
| 事务边界      | 应用服务 `@Transactional`              | 领域服务或仓储           |
| 依赖注入      | 构造器注入                             | 字段 `@Autowired`        |
| DTO 转换      | Assembler 类负责                       | Controller 直接转换      |

## 参考文件

- [领域层示例](../../member/member-service/src/main/java/com/pot/member/service/domain/model/member/MemberAggregate.java)
- [应用服务示例](../../member/member-service/src/main/java/com/pot/member/service/application/service/MemberApplicationService.java)
- [仓储实现示例](../../member/member-service/src/main/java/com/pot/member/service/infrastructure/persistence/repository/MemberRepositoryImpl.java)
