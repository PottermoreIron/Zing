package com.pot.zing.framework.starter.security.expression;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Permission expression backed by infix logical operators (AND, OR, NOT).
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class ComplexPermissionExpression implements PermissionExpression {

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\\(|\\)|\\bAND\\b|\\bOR\\b|\\bNOT\\b|[a-z][a-z0-9:.#*]*");

    private final String expression;
    private final PermissionExpressionParser parser;

    @Override
    public boolean evaluate(EvaluationContext context) {
        try {
            List<String> tokens = tokenize(expression);
            List<String> postfix = infixToPostfix(tokens);
            boolean result = evaluatePostfix(postfix, context);
            log.debug("[Permission] Complex expression evaluated — expression={}, result={}", expression, result);
            return result;
        } catch (Exception e) {
            log.error("[Permission] Complex expression evaluation failed — expression={}, error={}", expression,
                    e.getMessage(), e);
            return false;
        }
    }

    private List<String> tokenize(String raw) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(raw);
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private List<String> infixToPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();
        for (String token : tokens) {
            if ("(".equals(token)) {
                operators.push(token);
                continue;
            }
            if (")".equals(token)) {
                while (!operators.isEmpty() && !"(".equals(operators.peek())) {
                    output.add(operators.pop());
                }
                if (!operators.isEmpty()) operators.pop();
                continue;
            }
            if (isOperator(token)) {
                while (!operators.isEmpty() && !"(".equals(operators.peek())
                        && precedence(operators.peek()) >= precedence(token)) {
                    output.add(operators.pop());
                }
                operators.push(token);
                continue;
            }
            output.add(token);
        }
        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }
        return output;
    }

    private boolean evaluatePostfix(List<String> postfix, EvaluationContext context) {
        Stack<Boolean> stack = new Stack<>();
        for (String token : postfix) {
            if (isOperator(token)) {
                switch (token) {
                    case "AND" -> { boolean r = stack.pop(); stack.push(stack.pop() && r); }
                    case "OR"  -> { boolean r = stack.pop(); stack.push(stack.pop() || r); }
                    case "NOT" -> stack.push(!stack.pop());
                    default    -> throw new IllegalStateException("Unexpected operator: " + token);
                }
                continue;
            }
            stack.push(parser.parse(token).evaluate(context));
        }
        return !stack.isEmpty() && stack.pop();
    }

    private boolean isOperator(String token) {
        return "AND".equals(token) || "OR".equals(token) || "NOT".equals(token);
    }

    private int precedence(String operator) {
        return switch (operator) {
            case "NOT" -> 3;
            case "AND" -> 2;
            case "OR"  -> 1;
            default    -> 0;
        };
    }
}
