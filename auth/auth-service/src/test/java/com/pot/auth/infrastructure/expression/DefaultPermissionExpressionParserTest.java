package com.pot.auth.infrastructure.expression;

import com.pot.auth.domain.authorization.expression.ComplexPermissionExpression;
import com.pot.auth.domain.authorization.expression.CompositePermissionExpression;
import com.pot.auth.domain.authorization.expression.PermissionExpression;
import com.pot.auth.domain.authorization.expression.PermissionExpressionParser;
import com.pot.auth.domain.authorization.expression.SimplePermissionExpression;
import com.pot.auth.domain.authorization.expression.StandardPermissionEvaluationContext;
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

@DisplayName("DefaultPermissionExpressionParser")
class DefaultPermissionExpressionParserTest {

    private PermissionExpressionParser parser;

    @BeforeEach
    void setUp() {
        parser = new DefaultPermissionExpressionParser();
    }

    @Nested
    @DisplayName("simple expressions")
    class SimplePermission {

        @ParameterizedTest(name = "表达式: {0}")
        @ValueSource(strings = {
                "user.read",
                "member:read",
                "order:list",
                "article:123:edit",
        })
        @DisplayName("合法简单权限表达式解析为 SimplePermissionExpression")
        void whenSimplePermission_thenReturnSimpleExpression(String expr) {
            PermissionExpression expression = parser.parse(expr);
            assertThat(expression).isInstanceOf(SimplePermissionExpression.class);
            assertThat(expression.getExpression()).isEqualTo(expr);
        }

        @Test
        @DisplayName("用户拥有该权限时 evaluate 返回 true")
        void whenUserHasPermission_thenEvaluateReturnsTrue() {
            PermissionExpression expr = parser.parse("member:read");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read", "member:write"));
            assertThat(expr.evaluate(ctx)).isTrue();
        }

        @Test
        @DisplayName("用户缺少该权限时 evaluate 返回 false")
        void whenUserLacksPermission_thenEvaluateReturnsFalse() {
            PermissionExpression expr = parser.parse("admin:delete");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read"));
            assertThat(expr.evaluate(ctx)).isFalse();
        }
    }

    @Nested
    @DisplayName("complex expressions")
    class ComplexPermission {

        @Test
        @DisplayName("AND 表达式解析为 ComplexPermissionExpression")
        void whenAndExpression_thenReturnComplexExpression() {
            PermissionExpression expr = parser.parse("user.read AND user.write");
            assertThat(expr).isInstanceOf(ComplexPermissionExpression.class);
        }

        @Test
        @DisplayName("OR 表达式解析为 ComplexPermissionExpression")
        void whenOrExpression_thenReturnComplexExpression() {
            PermissionExpression expr = parser.parse("member:read OR admin:read");
            assertThat(expr).isInstanceOf(ComplexPermissionExpression.class);
        }

        @Test
        @DisplayName("AND 逻辑在权限齐全时返回 true")
        void whenAndBothPresent_thenReturnTrue() {
            PermissionExpression expr = parser.parse("member:read AND member:write");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read", "member:write"));
            assertThat(expr.evaluate(ctx)).isTrue();
        }

        @Test
        @DisplayName("AND 逻辑缺少一个权限时返回 false")
        void whenAndOneMissing_thenReturnFalse() {
            PermissionExpression expr = parser.parse("member:read AND admin:delete");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read"));
            assertThat(expr.evaluate(ctx)).isFalse();
        }

        @Test
        @DisplayName("OR 逻辑命中一个权限时返回 true")
        void whenOrOnePresent_thenReturnTrue() {
            PermissionExpression expr = parser.parse("member:read OR admin:delete");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read"));
            assertThat(expr.evaluate(ctx)).isTrue();
        }

        @Test
        @DisplayName("OR 逻辑都不命中时返回 false")
        void whenOrBothMissing_thenReturnFalse() {
            PermissionExpression expr = parser.parse("admin:read OR admin:write");
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read"));
            assertThat(expr.evaluate(ctx)).isFalse();
        }
    }

    @Nested
    @DisplayName("spel expressions")
    class SpelPermission {

        @ParameterizedTest(name = "SpEL表达式: {0}")
        @ValueSource(strings = {
                "hasPermission('user', 'read')",
                "hasRole('ADMIN')",
        })
        @DisplayName("SpEL 表达式解析为基础设施层实现")
        void whenSpelExpression_thenReturnSpelExpression(String expr) {
            PermissionExpression result = parser.parse(expr);
            assertThat(result).isInstanceOf(SpelPermissionExpression.class);
        }
    }

    @Nested
    @DisplayName("invalid inputs")
    class InvalidInput {

        @Test
        @DisplayName("null 表达式抛出 IllegalArgumentException")
        void whenNull_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白表达式抛出 IllegalArgumentException")
        void whenBlank_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> parser.parse("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("parseMultiple()")
    class ParseMultiple {

        @Test
        @DisplayName("单表达式数组直接返回该表达式")
        void whenSingleExpression_thenReturnDirectly() {
            PermissionExpression expr = parser.parseMultiple(new String[] { "member:read" }, Logical.AND);
            assertThat(expr).isInstanceOf(SimplePermissionExpression.class);
        }

        @Test
        @DisplayName("多表达式 AND 逻辑返回 CompositePermissionExpression")
        void whenMultipleExpressionsAndLogic_thenReturnComposite() {
            PermissionExpression expr = parser.parseMultiple(
                    new String[] { "member:read", "member:write" }, Logical.AND);
            assertThat(expr).isInstanceOf(CompositePermissionExpression.class);
        }

        @Test
        @DisplayName("多表达式 AND 逻辑全部满足时返回 true")
        void whenMultipleAndAllPresent_thenReturnTrue() {
            PermissionExpression expr = parser.parseMultiple(
                    new String[] { "member:read", "member:write" }, Logical.AND);
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read", "member:write"));
            assertThat(expr.evaluate(ctx)).isTrue();
        }

        @Test
        @DisplayName("多表达式 OR 逻辑命中一个时返回 true")
        void whenMultipleOrOnePresent_thenReturnTrue() {
            PermissionExpression expr = parser.parseMultiple(
                    new String[] { "member:read", "admin:write" }, Logical.OR);
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext
                    .fromPermissions(Set.of("member:read"));
            assertThat(expr.evaluate(ctx)).isTrue();
        }

        @Test
        @DisplayName("空数组抛出 IllegalArgumentException")
        void whenEmptyArray_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> parser.parseMultiple(new String[] {}, Logical.AND))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null 数组抛出 IllegalArgumentException")
        void whenNullArray_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> parser.parseMultiple(null, Logical.AND))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}