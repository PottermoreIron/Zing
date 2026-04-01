package com.pot.auth.infrastructure.expression;

import com.pot.auth.domain.authorization.expression.ComplexPermissionExpression;
import com.pot.auth.domain.authorization.expression.CompositePermissionExpression;
import com.pot.auth.domain.authorization.expression.PermissionExpression;
import com.pot.auth.domain.authorization.expression.PermissionExpressionParser;
import com.pot.auth.domain.authorization.expression.SimplePermissionExpression;
import com.pot.auth.domain.shared.enums.Logical;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 基于 Spring SpEL 的权限表达式解析器实现。
 */
@Slf4j
public class DefaultPermissionExpressionParser implements PermissionExpressionParser {

    private static final Pattern SIMPLE_PERMISSION_PATTERN = Pattern.compile("^[a-z][a-z0-9]*([:.][a-z0-9#*]+)*$");
    private static final Pattern COMPLEX_EXPRESSION_PATTERN = Pattern.compile("\\b(AND|OR|NOT)\\b");
    private static final Pattern SPEL_PATTERN = Pattern.compile("^#|@|\\$|\\(");

    private final ExpressionParser spelParser = new SpelExpressionParser();

    @Override
    public PermissionExpression parse(String expressionString) {
        if (expressionString == null || expressionString.isBlank()) {
            throw new IllegalArgumentException("权限表达式不能为空");
        }

        String trimmed = expressionString.trim();

        if (isSimplePermission(trimmed)) {
            return new SimplePermissionExpression(trimmed);
        }

        if (isSpelExpression(trimmed)) {
            return new SpelPermissionExpression(trimmed, spelParser);
        }

        if (isComplexExpression(trimmed)) {
            return new ComplexPermissionExpression(trimmed, this);
        }

        log.warn("[权限表达式] 无法确定表达式类型，当作简单权限处理: {}", trimmed);
        return new SimplePermissionExpression(trimmed);
    }

    @Override
    public PermissionExpression parseMultiple(String[] expressions, Logical logical) {
        if (expressions == null || expressions.length == 0) {
            throw new IllegalArgumentException("权限表达式数组不能为空");
        }

        if (expressions.length == 1) {
            return parse(expressions[0]);
        }

        List<PermissionExpression> parsedExpressions = Arrays.stream(expressions)
                .map(this::parse)
                .toList();

        return new CompositePermissionExpression(parsedExpressions, logical);
    }

    private boolean isSimplePermission(String expression) {
        return SIMPLE_PERMISSION_PATTERN.matcher(expression).matches();
    }

    private boolean isSpelExpression(String expression) {
        return SPEL_PATTERN.matcher(expression).find()
                || expression.contains("hasPermission")
                || expression.contains("hasRole")
                || expression.contains("hasAuthority");
    }

    private boolean isComplexExpression(String expression) {
        return COMPLEX_EXPRESSION_PATTERN.matcher(expression).find()
                || expression.contains("(") && expression.contains(")");
    }
}