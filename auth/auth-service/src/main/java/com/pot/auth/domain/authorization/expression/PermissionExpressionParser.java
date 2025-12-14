package com.pot.auth.domain.authorization.expression;

import com.pot.auth.domain.shared.enums.Logical;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 权限表达式解析器
 *
 * <p>
 * 支持多种表达式格式：
 * <ul>
 * <li>简单权限：{@code "user.read"}</li>
 * <li>资源级权限：{@code "article:123:edit"}</li>
 * <li>逻辑表达式：{@code "(user.read AND user.write) OR role.admin"}</li>
 * <li>SpEL表达式：{@code "hasPermission(#userId, 'user', 'read')"}</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Component
public class PermissionExpressionParser {

    private static final Pattern SIMPLE_PERMISSION_PATTERN = Pattern.compile("^[a-z][a-z0-9]*([:.][a-z0-9#*]+)*$");
    private static final Pattern COMPLEX_EXPRESSION_PATTERN = Pattern.compile("\\b(AND|OR|NOT)\\b");
    private static final Pattern SPEL_PATTERN = Pattern.compile("^#|@|\\$|\\(");

    private final ExpressionParser spelParser = new SpelExpressionParser();

    /**
     * 解析权限表达式
     *
     * @param expressionString 表达式字符串
     * @return 解析后的表达式
     */
    public PermissionExpression parse(String expressionString) {
        if (expressionString == null || expressionString.isBlank()) {
            throw new IllegalArgumentException("权限表达式不能为空");
        }

        String trimmed = expressionString.trim();

        // 1. 判断是否为简单权限
        if (isSimplePermission(trimmed)) {
            return new SimplePermissionExpression(trimmed);
        }

        // 2. 判断是否为SpEL表达式
        if (isSpelExpression(trimmed)) {
            return new SpelPermissionExpression(trimmed, spelParser);
        }

        // 3. 判断是否为复杂表达式
        if (isComplexExpression(trimmed)) {
            return new ComplexPermissionExpression(trimmed, this);
        }

        // 4. 默认当作简单权限处理
        log.warn("[权限表达式] 无法确定表达式类型，当作简单权限处理: {}", trimmed);
        return new SimplePermissionExpression(trimmed);
    }

    /**
     * 判断是否为简单权限
     */
    private boolean isSimplePermission(String expression) {
        return SIMPLE_PERMISSION_PATTERN.matcher(expression).matches();
    }

    /**
     * 判断是否为SpEL表达式
     */
    private boolean isSpelExpression(String expression) {
        return SPEL_PATTERN.matcher(expression).find() ||
                expression.contains("hasPermission") ||
                expression.contains("hasRole") ||
                expression.contains("hasAuthority");
    }

    /**
     * 判断是否为复杂表达式
     */
    private boolean isComplexExpression(String expression) {
        return COMPLEX_EXPRESSION_PATTERN.matcher(expression).find() ||
                expression.contains("(") && expression.contains(")");
    }

    /**
     * 批量解析权限表达式
     *
     * @param expressions 表达式数组
     * @param logical     逻辑关系（AND/OR）
     * @return 组合表达式
     */
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
}
