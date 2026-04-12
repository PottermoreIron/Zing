package com.pot.zing.framework.starter.authorization.expression;

import com.pot.zing.framework.starter.authorization.enums.Logical;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
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

        @ParameterizedTest(name = "expression: {0}")
        @ValueSource(strings = {
                "user.read",
                "member:read",
                "order:list",
                "article:123:edit"
        })
        void whenSimplePermission_thenReturnSimpleExpression(String expr) {
            PermissionExpression expression = parser.parse(expr);
            assertThat(expression).isInstanceOf(SimplePermissionExpression.class);
            assertThat(expression.getExpression()).isEqualTo(expr);
        }
    }

    @Nested
    @DisplayName("complex expressions")
    class ComplexPermission {

        @Test
        @DisplayName("Parenthesised expression is still parsed as a complex expression")
        void whenParenthesizedExpression_thenReturnComplexExpression() {
            PermissionExpression expr = parser.parse("(member:read AND member:write) OR admin:read");
            assertThat(expr).isInstanceOf(ComplexPermissionExpression.class);
        }

        @Test
        @DisplayName("AND expression returns true when all sub-expressions are satisfied")
        void whenMultipleAndAllPresent_thenReturnTrue() {
            PermissionExpression expr = parser.parseMultiple(
                    new String[] { "member:read", "member:write" }, Logical.AND);
            PermissionExpression.EvaluationContext ctx = StandardPermissionEvaluationContext.builder()
                    .permissions(Set.of("member:read", "member:write"))
                    .build();
            assertThat(expr.evaluate(ctx)).isTrue();
        }
    }

    @Nested
    @DisplayName("spel expressions")
    class SpelPermission {

        @Test
        @DisplayName("Role function expression is parsed as SpEL")
        void whenRoleFunction_thenReturnSpelExpression() {
            PermissionExpression expression = parser.parse("hasRole('ROLE_ADMIN')");
            assertThat(expression).isInstanceOf(SpelPermissionExpression.class);
        }

        @Test
        @DisplayName("SpEL expression can assemble resource-level permission from method arguments")
        void whenSpelUsesMethodParameter_thenEvaluateTrue() {
            PermissionExpression expression = parser.parse("hasPermission(#articleId, 'article', 'edit')");
            PermissionExpression.EvaluationContext context = StandardPermissionEvaluationContext.builder()
                    .permissions(Set.of("article:123:edit"))
                    .methodParameters(Map.of("articleId", 123L))
                    .build();

            assertThat(expression.evaluate(context)).isTrue();
        }
    }

    @Nested
    @DisplayName("invalid inputs")
    class InvalidInput {

        @Test
        void whenNull_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void whenBlank_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> parser.parse("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}