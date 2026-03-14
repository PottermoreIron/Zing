package com.pot.auth.domain;

import com.pot.auth.domain.authorization.expression.*;
import com.pot.auth.domain.shared.enums.Logical;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PermissionExpressionParser 单元测试
 *
 * <p>
 * 验证：
 * <ul>
 * <li>简单权限识别：单一权限字符串解析为 SimplePermissionExpression</li>
 * <li>复杂表达式识别：含 AND/OR/NOT 的表达式解析为 ComplexPermissionExpression</li>
 * <li>SpEL表达式识别：含 hasPermission/# 的表达式解析为 SpelPermissionExpression</li>
 * <li>空字符串/null 时抛出 IllegalArgumentException</li>
 * <li>parseMultiple：单表达式直接返回，多表达式返回 CompositePermissionExpression</li>
 * <li>表达式评估正确性</li>
 * </ul>
 *
 * @author pot
 */
@DisplayName("PermissionExpressionParser 单元测试")
class PermissionExpressionParserTest {

    private PermissionExpressionParser parser;

    @BeforeEach
    void setUp() {
        parser = new PermissionExpressionParser();
    }

    // ================================================================
    // parse - 简单权限
    // ================================================================

    @Nested
    @DisplayName("简单权限表达式解析")
    class SimplePermission {

        @ParameterizedTest(name = "表达式: {0}")
        @ValueSource(strings = {
                "user.read",
                "member:read",
                "order:list",
                "article:123:edit",
        })
        @DisplayName("合法的简单权限表达式，解析为 SimplePermissionExpression")
        void whenSimplePermission_thenReturnSimpleExpression(String expr) {
            PermissionExpression expression = parser.parse(expr);
            assertThat(expression).isInstanceOf(SimplePermissionExpression.class);
            assertThat(expression.getExpression()).isEqualTo(expr);
        }

        @Test
        @DisplayName("用户拥有该权限，evaluate 返回 true")
        void whenUserHasPermission_thenEvaluateReturnsTrue() {
            PermissionExpression expr = parser.parse("member:read");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read", "member:write"));
            assertThat(expr.evaluate(ctx)).isTrue();
        }

        @Test
        @DisplayName("用户不拥有该权限，evaluate 返回 false")
        void whenUserLacksPermission_thenEvaluateReturnsFalse() {
            PermissionExpression expr = parser.parse("admin:delete");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read"));
            assertThat(expr.evaluate(ctx)).isFalse();
        }
    }

    // ================================================================
    // parse - 复杂表达式
    // ================================================================

    @Nested
    @DisplayName("复杂权限表达式解析")
    class ComplexPermission {

        @Test
        @DisplayName("AND 表达式，解析为 ComplexPermissionExpression")
        void whenAndExpression_thenReturnComplexExpression() {
            PermissionExpression expr = parser.parse("user.read AND user.write");
            assertThat(expr).isInstanceOf(ComplexPermissionExpression.class);
        }

        @Test
        @DisplayName("OR 表达式，解析为 ComplexPermissionExpression")
        void whenOrExpression_thenReturnComplexExpression() {
            PermissionExpression expr = parser.parse("member:read OR admin:read");
            assertThat(expr).isInstanceOf(ComplexPermissionExpression.class);
        }

        @Test
        @DisplayName("AND 逻辑：两个权限都有时返回 true")
        void whenAndBothPresent_thenReturnTrue() {
            PermissionExpression expr = parser.parse("member:read AND member:write");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read", "member:write"));
            assertThat(expr.evaluate(ctx)).isTrue();
        }

        @Test
        @DisplayName("AND 逻辑：缺少一个权限时返回 false")
        void whenAndOneMissing_thenReturnFalse() {
            PermissionExpression expr = parser.parse("member:read AND admin:delete");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read"));
            assertThat(expr.evaluate(ctx)).isFalse();
        }

        @Test
        @DisplayName("OR 逻辑：只拥有其中一个权限时返回 true")
        void whenOrOnePresent_thenReturnTrue() {
            PermissionExpression expr = parser.parse("member:read OR admin:delete");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read"));
            assertThat(expr.evaluate(ctx)).isTrue();
        }

        @Test
        @DisplayName("OR 逻辑：两个权限都没有时返回 false")
        void whenOrBothMissing_thenReturnFalse() {
            PermissionExpression expr = parser.parse("admin:read OR admin:write");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read"));
            assertThat(expr.evaluate(ctx)).isFalse();
        }
    }

    // ================================================================
    // parse - SpEL表达式
    // ================================================================

    @Nested
    @DisplayName("SpEL 权限表达式解析")
    class SpelPermission {

        @ParameterizedTest(name = "SpEL表达式: {0}")
        @ValueSource(strings = {
                "hasPermission('user', 'read')",
                "hasRole('ADMIN')",
        })
        @DisplayName("包含 hasPermission/hasRole 的表达式，解析为 SpelPermissionExpression")
        void whenSpelExpression_thenReturnSpelExpression(String expr) {
            PermissionExpression result = parser.parse(expr);
            assertThat(result).isInstanceOf(SpelPermissionExpression.class);
        }
    }

    // ================================================================
    // parse - 异常
    // ================================================================

    @Nested
    @DisplayName("非法输入处理")
    class InvalidInput {

        @Test
        @DisplayName("null 表达式，抛出 IllegalArgumentException")
        void whenNull_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白字符串，抛出 IllegalArgumentException")
        void whenBlank_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> parser.parse("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ================================================================
    // parseMultiple
    // ================================================================

    @Nested
    @DisplayName("parseMultiple()")
    class ParseMultiple {

        @Test
        @DisplayName("单表达式数组，直接返回该表达式，不包装为Composite")
        void whenSingleExpression_thenReturnDirectly() {
            PermissionExpression expr = parser.parseMultiple(new String[] { "member:read" }, Logical.AND);
            assertThat(expr).isInstanceOf(SimplePermissionExpression.class);
        }

        @Test
        @DisplayName("多表达式 + AND 逻辑，返回 CompositePermissionExpression")
        void whenMultipleExpressionsAndLogic_thenReturnComposite() {
            PermissionExpression expr = parser.parseMultiple(
                    new String[] { "member:read", "member:write" }, Logical.AND);
            assertThat(expr).isInstanceOf(CompositePermissionExpression.class);
        }

        @Test
        @DisplayName("多表达式 AND 逻辑：全部权限满足时返回 true")
        void whenMultipleAndAllPresent_thenReturnTrue() {
            PermissionExpression expr = parser.parseMultiple(
                    new String[] { "member:read", "member:write" }, Logical.AND);
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read", "member:write"));
            assertThat(expr.evaluate(ctx)).isTrue();
        }

        @Test
        @DisplayName("多表达式 OR 逻辑：其中一个满足时返回 true")
        void whenMultipleOrOnePresent_thenReturnTrue() {
            PermissionExpression expr = parser.parseMultiple(
                    new String[] { "member:read", "admin:write" }, Logical.OR);
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read"));
            assertThat(expr.evaluate(ctx)).isTrue();
        }

        @Test
        @DisplayName("空数组，抛出 IllegalArgumentException")
        void whenEmptyArray_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> parser.parseMultiple(new String[] {}, Logical.AND))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null数组，抛出 IllegalArgumentException")
        void whenNullArray_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> parser.parseMultiple(null, Logical.AND))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
