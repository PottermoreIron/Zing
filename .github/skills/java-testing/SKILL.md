---
name: java-testing
description: "Java 单元测试与集成测试规范：为领域模型、应用服务、基础设施层编写测试。使用 JUnit5、Mockito、AssertJ。Use when writing unit tests, integration tests, mocks, or behavior assertions. 触发词：单元测试、集成测试、Test、@Test、Mockito、mock、断言、assertThat、@Nested、@DisplayName、测试覆盖、测试用例、unit test、integration test。"
argument-hint: "描述要测试的类或场景（如：给 MemberAggregate 的 changePassword 方法写测试）"
---

# Java 测试规范（JUnit5 + Mockito + AssertJ）

## 测试分层策略

| 层次                          | 测试类型              | 使用工具                        | 是否需要 Spring 上下文 |
| ----------------------------- | --------------------- | ------------------------------- | ---------------------- |
| `domain/model/`               | 纯单元测试            | JUnit5 + AssertJ                | ❌ 不需要              |
| `application/service/`        | 单元测试（Mock 依赖） | Mockito + AssertJ               | ❌ 不需要              |
| `infrastructure/persistence/` | 集成测试              | @SpringBootTest 或 @DataJpaTest | ✅ 需要                |
| `interfaces/rest/`            | Web 层测试            | @WebMvcTest                     | 局部需要               |

## 测试文件位置

镜像主代码包结构，放在 `src/test/java/` 下：

```
src/test/java/com/pot/member/service/
├── domain/
│   └── model/
│       └── member/
│           ├── MemberAggregateTest.java    ← 纯单元测试
│           └── MemberProfileTest.java
├── application/
│   └── service/
│       └── MemberApplicationServiceTest.java  ← Mock 测试
└── infrastructure/
    └── event/
        └── RabbitMQEventPublisherAdapterTest.java
```

## 领域模型测试模板

```java
// domain/model/member/MemberAggregateTest.java
@DisplayName("MemberAggregate")
class MemberAggregateTest {

    // ========== 用 @Nested + @DisplayName 组织场景 ==========

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("正常注册：状态为 ACTIVE，无角色，有领域事件")
        void create_happyPath() {
            Nickname nickname = Nickname.of("testuser");
            Email email = Email.of("test@example.com");
            String hash = "$2a$10$abc";

            MemberAggregate member = MemberAggregate.create(nickname, email, hash);

            // 使用 AssertJ，不用 assertEquals
            assertThat(member.getNickname()).isEqualTo(nickname);
            assertThat(member.getEmail()).isEqualTo(email);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getRoleIds()).isEmpty();
            assertThat(member.pullDomainEvents()).hasSize(1);
        }

        @Test
        @DisplayName("空昵称：抛出 IllegalArgumentException")
        void create_blankNickname_throws() {
            assertThatThrownBy(() -> Nickname.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("昵称");
        }
    }

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("修改密码：产生 PasswordChanged 事件")
        void changePassword_producesEvent() {
            MemberAggregate member = buildValidMember();
            PasswordEncoder encoder = raw -> "hashed_" + raw;

            member.changePassword("newSecret", encoder);

            List<MemberDomainEvent> events = member.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(MemberDomainEvent.PasswordChanged.class);
        }
    }

    // ========== 测试辅助工厂方法 ==========
    private MemberAggregate buildValidMember() {
        return MemberAggregate.create(
            Nickname.of("test"),
            Email.of("test@example.com"),
            "$2a$10$hash"
        );
    }
}
```

## 应用服务测试模板（Mockito）

```java
// application/service/MemberApplicationServiceTest.java
@ExtendWith(MockitoExtension.class)          // ← 不启动 Spring
@DisplayName("MemberApplicationService")
class MemberApplicationServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock MemberIdGenerator idGenerator;
    @Mock PasswordEncoder passwordEncoder;
    @Mock DomainEventPublisher eventPublisher;
    @Mock MemberAssembler assembler;

    @InjectMocks MemberApplicationService sut;  // System Under Test

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("邮箱已注册：抛出异常，不保存")
        void register_emailExists_throws() {
            RegisterMemberCommand cmd = new RegisterMemberCommand("test", "dup@x.com", "pass");
            when(memberRepository.existsByEmail(Email.of("dup@x.com"))).thenReturn(true);

            assertThatThrownBy(() -> sut.register(cmd))
                .isInstanceOf(IllegalStateException.class);

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("正常注册：保存聚合，发布事件，返回 DTO")
        void register_success() {
            RegisterMemberCommand cmd = new RegisterMemberCommand("nick", "a@b.com", "pass");
            when(memberRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode("pass")).thenReturn("hashed");
            MemberDTO expectedDto = new MemberDTO(/* ... */);
            when(assembler.toDTO(any())).thenReturn(expectedDto);

            MemberDTO result = sut.register(cmd);

            verify(memberRepository).save(any(MemberAggregate.class));
            verify(eventPublisher, atLeastOnce()).publish(any());
            assertThat(result).isEqualTo(expectedDto);
        }
    }
}
```

## AssertJ 常用断言速查

```java
// 相等
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotEqualTo(unexpected);

// 空/非空
assertThat(list).isEmpty();
assertThat(optional).isPresent().hasValue(expected);
assertThat(str).isNotBlank();

// 集合
assertThat(list).hasSize(3);
assertThat(list).containsExactly(a, b, c);
assertThat(list).extracting(Item::getName).containsOnly("a", "b");

// 异常（替代 assertThrows）
assertThatThrownBy(() -> service.doSomething())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("keyword");

// 类型
assertThat(event).isInstanceOf(MemberDomainEvent.Registered.class);
```

## 测试命名约定

| 格式         | 示例                                           |
| ------------ | ---------------------------------------------- |
| 方法名       | `{methodName}_{scenario}_{expectedResult}`     |
| @DisplayName | 中文描述，清晰表达场景和期望                   |
| 快乐路径     | `register_happyPath()` 或 `register_success()` |
| 异常场景     | `create_blankNickname_throws()`                |

## 检查清单

- [ ] `domain/model/` 测试：无 Spring、无 Mock（直接实例化）
- [ ] `application/service/` 测试：`@ExtendWith(MockitoExtension.class)`，所有外部依赖 Mock
- [ ] 每个测试方法只测一个行为
- [ ] 使用 `@Nested` 按方法/场景分组
- [ ] 使用 AssertJ `assertThat`，不用 `assertEquals`
- [ ] 异常测试用 `assertThatThrownBy`，不用 `try/catch`
- [ ] 验证 Mock 调用用 `verify(mock).method()`

## 参考文件

- [领域模型测试示例](../../member/member-service/src/test/java/com/pot/member/service/domain/model/member/MemberAggregateTest.java)
- [应用服务测试示例](../../member/member-service/src/test/java/com/pot/member/service/application/service/MemberApplicationServiceTest.java)
