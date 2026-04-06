---
name: mybatis-mapper
description: "MyBatis Mapper XML 与 PO 维护规范：新增表映射、修复 resultMap type 错误、添加自定义 SQL、维护 Mapper 接口。Use when creating mapper XML, fixing resultMap issues, maintaining persistence entities, or updating BaseMapper contracts. 触发词：Mapper XML、resultMap、PO、persistence entity、MyBatis-Plus、@TableName、BaseMapper、mapper文件、xml映射、MyBatis、SQL。"
argument-hint: "描述要操作的表或 Mapper（如：给 order 表创建 Mapper，修复 resultMap 报错）"
---

# MyBatis Mapper XML + PO 维护规范

## 文件位置约定

| 类型        | 路径                                                  |
| ----------- | ----------------------------------------------------- |
| Mapper XML  | `src/main/resources/mapper/{Name}Mapper.xml`          |
| Mapper 接口 | `infrastructure/persistence/mapper/{Name}Mapper.java` |
| PO 实体类   | `infrastructure/persistence/entity/{Name}.java`       |

## 最常见错误：resultMap type 包路径错误

**错误**（旧 MVC 路径）：

```xml
<resultMap type="com.pot.member.service.entity.Member">
```

**正确**（DDD 路径）：

```xml
<resultMap type="com.pot.member.service.infrastructure.persistence.entity.Member">
```

批量修复命令：

```bash
sed -i '' 's|com\.pot\.member\.service\.entity\.|com.pot.member.service.infrastructure.persistence.entity.|g' \
  src/main/resources/mapper/*.xml
```

## PO 模板（@TableName + @TableId）

```java
// infrastructure/persistence/entity/{Name}.java
@Data
@TableName("member")                         // 表名（单数）
public class Member implements Serializable {

    @TableId(type = IdType.INPUT)            // 业务 ID，非自增
    private Long id;

    private String memberId;                 // 业务主键（对应 domain MemberId）
    private String nickname;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtUpdatedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtDeletedAt;      // 逻辑删除时间（null = 未删除）
}
```

## Mapper 接口模板

```java
// infrastructure/persistence/mapper/MemberMapper.java
@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    // 自定义查询（非 BaseMapper 提供的）
    @Select("SELECT * FROM member WHERE member_id = #{memberId} AND gmt_deleted_at IS NULL")
    Member selectByMemberId(@Param("memberId") String memberId);

    // 复杂查询放 XML 中
    List<Member> selectWithRoles(@Param("memberId") String memberId);
}
```

## Mapper XML 模板

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.pot.{module}.service.infrastructure.persistence.mapper.MemberMapper">

    <!-- resultMap type 必须用 infrastructure.persistence.entity 全路径 -->
    <resultMap id="BaseResultMap"
               type="com.pot.{module}.service.infrastructure.persistence.entity.Member">
        <id     column="id"             property="id"/>
        <result column="member_id"      property="memberId"/>
        <result column="nickname"       property="nickname"/>
        <result column="email"          property="email"/>
        <result column="password_hash"  property="passwordHash"/>
        <result column="first_name"     property="firstName"/>
        <result column="last_name"      property="lastName"/>
        <result column="status"         property="status"/>
        <result column="gmt_created_at" property="gmtCreatedAt"/>
        <result column="gmt_updated_at" property="gmtUpdatedAt"/>
        <result column="gmt_deleted_at" property="gmtDeletedAt"/>
    </resultMap>

    <sql id="Base_Column_List">
        id, member_id, nickname, email, password_hash,
        first_name, last_name, status,
        gmt_created_at, gmt_updated_at, gmt_deleted_at
    </sql>

    <!-- 自定义复杂查询 -->
    <select id="selectWithRoles" resultMap="BaseResultMap">
        SELECT m.*
        FROM member m
        LEFT JOIN member_role mr ON m.member_id = mr.member_id
        WHERE m.member_id = #{memberId}
          AND m.gmt_deleted_at IS NULL
    </select>

</mapper>
```

## 新增 Mapper 检查清单

- [ ] PO 类放在 `infrastructure/persistence/entity/`
- [ ] `@TableId(type = IdType.INPUT)` — 非自增
- [ ] `@TableName` 使用**单数**表名
- [ ] Mapper 接口 extends `BaseMapper<PO类>`
- [ ] XML `namespace` 指向 `infrastructure.persistence.mapper` 包
- [ ] XML `resultMap type` 指向 `infrastructure.persistence.entity` 包（全路径）
- [ ] XML 文件名与 Mapper 接口名一致（如 `MemberMapper.xml`）

## 参考文件

- [Member PO](../../member/member-service/src/main/java/com/pot/member/service/infrastructure/persistence/entity/Member.java)
- [MemberMapper 接口](../../member/member-service/src/main/java/com/pot/member/service/infrastructure/persistence/mapper/MemberMapper.java)
- [MemberMapper XML](../../member/member-service/src/main/resources/mapper/MemberMapper.xml)
