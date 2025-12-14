package com.pot.auth.domain.authorization.expression;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 复杂权限表达式
 *
 * <p>
 * 用于处理包含逻辑运算符的复杂表达式，如：
 * <ul>
 * <li>{@code "(user.read AND user.write) OR role.admin"}</li>
 * <li>{@code "user.read AND (user.write OR user.delete)"}</li>
 * <li>{@code "NOT user.read"}</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class ComplexPermissionExpression implements PermissionExpression {

    private final String expression;
    private final PermissionExpressionParser parser;

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\\(|\\)|\\bAND\\b|\\bOR\\b|\\bNOT\\b|[a-z][a-z0-9:.#*]*");

    @Override
    public boolean evaluate(PermissionExpression.EvaluationContext context) {
        try {
            List<String> tokens = tokenize(expression);
            List<String> postfix = infixToPostfix(tokens);
            boolean result = evaluatePostfix(postfix, context);

            log.debug("[权限评估] 复杂表达式: expression={}, result={}", expression, result);
            return result;

        } catch (Exception e) {
            log.error("[权限评估] 复杂表达式评估失败: expression={}, error={}",
                    expression, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 分词
     */
    private List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(expr);

        while (matcher.find()) {
            String token = matcher.group().trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    /**
     * 中缀表达式转后缀表达式（调度场算法）
     */
    private List<String> infixToPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();

        for (String token : tokens) {
            if (token.equals("(")) {
                operators.push(token);
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }
                if (!operators.isEmpty()) {
                    operators.pop(); // 移除 "("
                }
            } else if (isOperator(token)) {
                while (!operators.isEmpty() && !operators.peek().equals("(") &&
                        precedence(operators.peek()) >= precedence(token)) {
                    output.add(operators.pop());
                }
                operators.push(token);
            } else {
                // 权限名称
                output.add(token);
            }
        }

        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }

        return output;
    }

    /**
     * 评估后缀表达式
     */
    private boolean evaluatePostfix(List<String> postfix, PermissionExpression.EvaluationContext context) {
        Stack<Boolean> stack = new Stack<>();

        for (String token : postfix) {
            if (isOperator(token)) {
                switch (token) {
                    case "AND" -> {
                        boolean b = stack.pop();
                        boolean a = stack.pop();
                        stack.push(a && b);
                    }
                    case "OR" -> {
                        boolean b = stack.pop();
                        boolean a = stack.pop();
                        stack.push(a || b);
                    }
                    case "NOT" -> {
                        boolean a = stack.pop();
                        stack.push(!a);
                    }
                }
            } else {
                // 权限名称，直接评估
                PermissionExpression expr = parser.parse(token);
                stack.push(expr.evaluate(context));
            }
        }

        return stack.isEmpty() ? false : stack.pop();
    }

    /**
     * 判断是否为操作符
     */
    private boolean isOperator(String token) {
        return token.equals("AND") || token.equals("OR") || token.equals("NOT");
    }

    /**
     * 操作符优先级
     */
    private int precedence(String operator) {
        return switch (operator) {
            case "NOT" -> 3;
            case "AND" -> 2;
            case "OR" -> 1;
            default -> 0;
        };
    }

    @Override
    public String toString() {
        return "ComplexPermission(" + expression + ")";
    }
}
