/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.evaluators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.operators.primitives.BinaryOperatorHandler;
import org.elasticsearch.xpack.plesql.operators.OperatorHandlerRegistry;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ExpressionEvaluator {
    private final ProcedureExecutor executor;
    private final ThreadPool threadPool;
    private final ExecutionContext context;
    private static final Logger LOGGER = LogManager.getLogger(ExpressionEvaluator.class);

    public ExpressionEvaluator(ProcedureExecutor executor) {
        this.executor = executor;
        this.threadPool = executor.getThreadPool();
        this.context = executor.getContext();
    }

    /**
     * Evaluates an expression asynchronously.
     * If the expression contains concatenation (i.e. multiple logicalOrExpressions separated by '||'),
     * then each operand is evaluated and the results are concatenated as strings.
     *
     * @param ctx the ExpressionContext from the parse tree.
     * @param listener an ActionListener to receive the evaluated result.
     */
    public void evaluateExpressionAsync(PlEsqlProcedureParser.ExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx == null) {
            listener.onFailure(new RuntimeException("Null expression context"));
            return;
        }
        List<PlEsqlProcedureParser.LogicalOrExpressionContext> operands = ctx.logicalOrExpression();
        if (operands.size() == 1) {
            // No concatenation operator present – evaluate normally.
            evaluateLogicalOrExpressionAsync(operands.get(0), listener);
        } else {
            // Evaluate each operand and concatenate their results as strings.
            evaluateOperandsAndConcatenate(operands, 0, new ArrayList<>(), listener);
        }
    }

    private void evaluateOperandsAndConcatenate(List<PlEsqlProcedureParser.LogicalOrExpressionContext> operands,
                                                int index, List<Object> results, ActionListener<Object> listener) {
        if (index >= operands.size()) {
            // Concatenate all results into one string.
            StringBuilder sb = new StringBuilder();
            for (Object res : results) {
                sb.append(String.valueOf(res));
            }
            listener.onResponse(sb.toString());
            return;
        }
        // Evaluate each operand asynchronously.
        evaluateLogicalOrExpressionAsync(operands.get(index), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                results.add(result);
                evaluateOperandsAndConcatenate(operands, index + 1, results, listener);
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void evaluateLogicalOrExpressionAsync(PlEsqlProcedureParser.LogicalOrExpressionContext ctx, ActionListener<Object> listener) {
        List<PlEsqlProcedureParser.LogicalAndExpressionContext> andExprs = ctx.logicalAndExpression();
        if (andExprs.size() == 1) {
            evaluateLogicalAndExpressionAsync(andExprs.get(0), listener);
        } else {
            evaluateLogicalOrOperandsAsync(andExprs, 0, listener);
        }
    }

    private void evaluateLogicalOrOperandsAsync(List<PlEsqlProcedureParser.LogicalAndExpressionContext> operands,
                                                int index, ActionListener<Object> listener) {
        if (index >= operands.size()) {
            listener.onResponse(false);
            return;
        }
        ActionListener<Object> l = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                boolean booleanResult = toBoolean(result);
                if (booleanResult) {
                    listener.onResponse(true);
                } else {
                    evaluateLogicalOrOperandsAsync(operands, index + 1, listener);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };
        ActionListener<Object> logger = ActionListenerUtils.withLogging(l, getClass().getName(),
            "Evaluate-Logical-Or-Operands: " + operands.get(index));
        evaluateLogicalAndExpressionAsync(operands.get(index), logger);
    }

    private void evaluateLogicalAndExpressionAsync(PlEsqlProcedureParser.LogicalAndExpressionContext ctx, ActionListener<Object> listener) {
        List<PlEsqlProcedureParser.EqualityExpressionContext> eqExprs = ctx.equalityExpression();
        if (eqExprs.size() == 1) {
            evaluateEqualityExpressionAsync(eqExprs.get(0), listener);
        } else {
            evaluateLogicalAndOperandsAsync(eqExprs, 0, listener);
        }
    }

    private void evaluateLogicalAndOperandsAsync(List<PlEsqlProcedureParser.EqualityExpressionContext> operands,
                                                 int index, ActionListener<Object> listener) {
        if (index >= operands.size()) {
            listener.onResponse(true);
            return;
        }
        ActionListener<Object> l = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                boolean booleanResult = toBoolean(result);
                if ( booleanResult == false ) {
                    listener.onResponse(false);
                } else {
                    evaluateLogicalAndOperandsAsync(operands, index + 1, listener);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };
        ActionListener<Object> logger = ActionListenerUtils.withLogging(l, getClass().getName(),
            "Evaluate-Logical-And-Operands: " + operands.get(index));
        evaluateEqualityExpressionAsync(operands.get(index), logger);
    }

    private void evaluateEqualityExpressionAsync(PlEsqlProcedureParser.EqualityExpressionContext ctx, ActionListener<Object> listener) {
        // Single operand: delegate to relational
        if (ctx.relationalExpression().size() == 1) {
            evaluateRelationalExpressionAsync(ctx.relationalExpression(0), listener);
            return;
        }
        LOGGER.info("Running on thread [{}]", Thread.currentThread().getName());
        // Use operator registry for '==' and '<>'
        String operator = ctx.getChild(1).getText();
        OperatorHandlerRegistry registry = new OperatorHandlerRegistry();
        BinaryOperatorHandler handler = registry.getHandler(operator);
        // Evaluate both sides asynchronously using named listeners for clarity
        final Object[] leftResultHolder = new Object[1];
        ActionListener<Object> rightListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object right) {
                try {
            if (leftResultHolder[0] == null || right == null) {
                listener.onResponse(leftResultHolder[0] == right);
            } else if (handler.isApplicable(leftResultHolder[0], right)) {
                listener.onResponse(handler.apply(leftResultHolder[0], right));
            } else {
                listener.onFailure(new RuntimeException(
                    "Operator '" + operator + "' not applicable for types: "
                    + leftResultHolder[0].getClass().getSimpleName() + ", " + right.getClass().getSimpleName()));
            }
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> leftListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object left) {
                leftResultHolder[0] = left;
                evaluateRelationalExpressionAsync(ctx.relationalExpression(1), rightListener);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        evaluateRelationalExpressionAsync(ctx.relationalExpression(0), leftListener);
    }

    private void evaluateRelationalExpressionAsync(PlEsqlProcedureParser.RelationalExpressionContext ctx, ActionListener<Object> listener) {
        // Single operand: delegate to additive
        if (ctx.additiveExpression().size() == 1) {
            evaluateAdditiveExpressionAsync(ctx.additiveExpression(0), listener);
            return;
        }
        // Use operator registry for '<', '<=', '>', '>='
        String operator = ctx.getChild(1).getText();
        OperatorHandlerRegistry registry = new OperatorHandlerRegistry();
        BinaryOperatorHandler handler = registry.getHandler(operator);
        // Evaluate both sides asynchronously
        evaluateAdditiveExpressionAsync(ctx.additiveExpression(0), ActionListener.wrap(
            left -> evaluateAdditiveExpressionAsync(ctx.additiveExpression(1), ActionListener.wrap(
                right -> {
                    try {
                        if (left == null || right == null) {
                            listener.onResponse(false);
                        } else if (handler.isApplicable(left, right)) {
                            listener.onResponse(handler.apply(left, right));
                        } else {
                            listener.onFailure(new RuntimeException(
                                "Operator '" + operator + "' not applicable for types: "
                                + left.getClass().getSimpleName() + ", " + right.getClass().getSimpleName()));
                        }
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                }, listener::onFailure)),
            listener::onFailure));
    }

    private void evaluateAdditiveExpressionAsync(PlEsqlProcedureParser.AdditiveExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx.multiplicativeExpression().size() == 1) {
            evaluateMultiplicativeExpressionAsync(ctx.multiplicativeExpression(0), listener);
        } else {
            ActionListener<Object> operandListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object initialValue) {
                    evaluateAdditiveOperandsAsync(ctx, 1, initialValue, listener);
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };
            ActionListener<Object> logger = ActionListenerUtils.withLogging(operandListener, getClass().getName(),
                "Evaluate-Additive Expression: " + ctx.multiplicativeExpression(0));
            evaluateMultiplicativeExpressionAsync(ctx.multiplicativeExpression(0), logger);
        }
    }

    private void evaluateAdditiveOperandsAsync(PlEsqlProcedureParser.AdditiveExpressionContext ctx,
                                               int index, Object leftValue, ActionListener<Object> listener) {
        if (index >= ctx.multiplicativeExpression().size()) {
            listener.onResponse(leftValue);
            return;
        }
        PlEsqlProcedureParser.MultiplicativeExpressionContext currentExpr = ctx.multiplicativeExpression(index);
        String operator = ctx.getChild(2 * index - 1).getText();

        // Instantiate the registry (or use a cached instance)
        OperatorHandlerRegistry registry = new OperatorHandlerRegistry();
        BinaryOperatorHandler handler = registry.getHandler(operator);

        ActionListener<Object> evalOperandListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object rightValue) {
                try {
                    Object result = handler.apply(leftValue, rightValue);
                    evaluateAdditiveOperandsAsync(ctx, index + 1, result, listener);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };
        ActionListener<Object> evalLogger = ActionListenerUtils.withLogging(evalOperandListener, getClass().getName(),
            "Evaluate-Additive Operands: " + currentExpr);
        evaluateMultiplicativeExpressionAsync(currentExpr, evalLogger);
    }

    private void evaluateMultiplicativeExpressionAsync(PlEsqlProcedureParser.MultiplicativeExpressionContext ctx,
                                                       ActionListener<Object> listener) {
        if (ctx.unaryExpr().size() == 1) {
            evaluateUnaryExpressionAsync(ctx.unaryExpr(0), listener);
        } else {
            ActionListener<Object> initListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object initialValue) {
                    evaluateMultiplicativeOperandsAsync(ctx, 1, initialValue, listener);
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };
            ActionListener<Object> initLogger = ActionListenerUtils.withLogging(initListener, getClass().getName(),
                "Evaluate-Multiplicative Expression: " + ctx.unaryExpr(0));
            evaluateUnaryExpressionAsync(ctx.unaryExpr(0), initLogger);
        }
    }

    private void evaluateMultiplicativeOperandsAsync(PlEsqlProcedureParser.MultiplicativeExpressionContext ctx,
                                                     int index, Object leftValue, ActionListener<Object> listener) {
        if (index >= ctx.unaryExpr().size()) {
            listener.onResponse(leftValue);
            return;
        }
        // Compute using the operator registry for '*', '/', '%'
        PlEsqlProcedureParser.UnaryExprContext currentExpr = ctx.unaryExpr(index);
        String operator = ctx.getChild(2 * index - 1).getText();
        OperatorHandlerRegistry registry = new OperatorHandlerRegistry();
        BinaryOperatorHandler handler = registry.getHandler(operator);
        // Evaluate the right operand asynchronously
        ActionListener<Object> rightListener = ActionListener.wrap(
            rightValue -> {
                try {
                    if (handler.isApplicable(leftValue, rightValue)) {
                        Object result = handler.apply(leftValue, rightValue);
                        evaluateMultiplicativeOperandsAsync(ctx, index + 1, result, listener);
                    } else {
                        listener.onFailure(new RuntimeException(
                            "Operator '" + operator + "' not applicable for types: "
                            + leftValue.getClass().getSimpleName() + ", "
                            + (rightValue != null ? rightValue.getClass().getSimpleName() : "null")));
                    }
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            },
            listener::onFailure
        );
        ActionListener<Object> rightLogger = ActionListenerUtils.withLogging(
            rightListener, getClass().getName(),
            "Evaluate-Multiplicative Right Operand: " + currentExpr
        );
        evaluateUnaryExpressionAsync(currentExpr, rightLogger);
    }

    @SuppressWarnings("checkstyle:DescendantToken")
    private void evaluateUnaryExpressionAsync(PlEsqlProcedureParser.UnaryExprContext ctx, ActionListener<Object> listener) {
        if (ctx.primaryExpression() != null) {
            evaluatePrimaryExpressionAsync(ctx.primaryExpression(), listener);
        } else if (ctx.unaryExpr() != null) {
            String operator = ctx.getChild(0).getText();
            ActionListener<Object> unaryListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    try {
                        if (operator.equals("-")) {
                            if (result instanceof Number) {
                                double value = ((Number) result).doubleValue();
                                listener.onResponse(-value);
                            } else {
                                listener.onFailure(new RuntimeException("Unary minus can only be applied to numbers."));
                            }
                        } else if (operator.equals("NOT")) {
                            if (result instanceof Boolean) {
                                listener.onResponse(!((Boolean) result));
                            } else {
                                listener.onFailure(new RuntimeException("NOT operator can only be applied to boolean values."));
                            }
                        } else {
                            listener.onFailure(new RuntimeException("Unknown unary operator: " + operator));
                        }
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };
            ActionListener<Object> unaryLogger = ActionListenerUtils.withLogging(unaryListener, getClass().getName(),
                "Evaluate-Unary Expression: " + ctx.unaryExpr());
            evaluateUnaryExpressionAsync(ctx.unaryExpr(), unaryLogger);
        } else {
            listener.onFailure(new RuntimeException("Unsupported unary expression: " + ctx.getText()));
        }
    }

    private void evaluatePrimaryExpressionAsync(PlEsqlProcedureParser.PrimaryExpressionContext ctx, ActionListener<Object> listener) {
        ActionListener<Object> processResult = new ActionListener<Object>() {
            @Override
            public void onResponse(Object baseValue) {
                if (ctx.bracketExpression() != null && ctx.bracketExpression().isEmpty() == false ) {
                    evaluateDocumentFieldAccessRecursive(baseValue, ctx.bracketExpression(), 0, listener);
                } else {
                    listener.onResponse(baseValue);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        LOGGER.info("Evaluating primary expression for {} ", ctx.getText());

        if (ctx.simplePrimaryExpression().LPAREN() != null && ctx.simplePrimaryExpression().RPAREN() != null) {
            evaluateExpressionAsync(ctx.simplePrimaryExpression().expression(), processResult);
        } else if (ctx.simplePrimaryExpression().call_procedure_statement() != null) {
            // Delegate function call evaluation to the executor's function call handler.
            executor.visitCallProcedureAsync(ctx.simplePrimaryExpression().call_procedure_statement(), processResult);
        }  else if (ctx.simplePrimaryExpression().function_call() != null) {
            // Delegate function call evaluation to the executor's function call handler.
            executor.visitFunctionCallAsync(ctx.simplePrimaryExpression().function_call(), processResult);
        } else if (ctx.simplePrimaryExpression().INT() != null) {
            try {
                processResult.onResponse(Integer.valueOf(ctx.simplePrimaryExpression().INT().getText()));
            } catch (NumberFormatException e) {
                listener.onFailure(new RuntimeException("Invalid integer literal: " + ctx.simplePrimaryExpression().INT().getText()));
            }
        } else if (ctx.simplePrimaryExpression().FLOAT() != null) {
            try {
                processResult.onResponse(Double.valueOf(ctx.simplePrimaryExpression().FLOAT().getText()));
            } catch (NumberFormatException e) {
                listener.onFailure(new RuntimeException("Invalid float literal: " + ctx.simplePrimaryExpression().FLOAT().getText()));
            }
        } else if (ctx.simplePrimaryExpression().STRING() != null) {
            String text = ctx.simplePrimaryExpression().STRING().getText();
            String processedString = text.substring(1, text.length() - 1).replace("\\'", "'");
            processResult.onResponse(processedString);
        } else if (ctx.simplePrimaryExpression().BOOLEAN() != null) {
            String boolText = ctx.simplePrimaryExpression().BOOLEAN().getText();
            processResult.onResponse(Boolean.parseBoolean(boolText));
        } else if (ctx.simplePrimaryExpression().arrayLiteral() != null) {
            if (ctx.simplePrimaryExpression().arrayLiteral().expressionList() != null) {
                evaluateExpressionList(ctx.simplePrimaryExpression().arrayLiteral().expressionList().expression(), processResult);
            } else {
                processResult.onResponse(new ArrayList<>());
            }
        } else if (ctx.simplePrimaryExpression().documentLiteral() != null) {
            PlEsqlProcedureParser.DocumentLiteralContext docCtx = ctx.simplePrimaryExpression().documentLiteral();
            evaluateDocumentLiteralAsync(docCtx, processResult);
            return;
        } else if (ctx.simplePrimaryExpression().NULL() != null) {
            processResult.onResponse(null);
        } else if (ctx.simplePrimaryExpression().ID() != null) {
            String id = ctx.simplePrimaryExpression().ID().getText();
            Object var = context.getVariable(id);
            if (var == null) {
                listener.onFailure(new RuntimeException("Variable not declared: " + id));
            } else {
                // if there are bracketExpressions, delegate into your recursive lookup
                List<PlEsqlProcedureParser.BracketExpressionContext> brackets = ctx.bracketExpression();
                if ( brackets != null && brackets.isEmpty() == false ) {
                    evaluateDocumentFieldAccessRecursive(var, brackets, 0, listener);
                } else {
                    listener.onResponse(var);
                }
            }

        } else {
            listener.onFailure(new RuntimeException("Unsupported primary expression: " + ctx.getText()));
        }
    }

    private void evaluateDocumentFieldAccessRecursive(Object current, List<PlEsqlProcedureParser.BracketExpressionContext> bracketExprs,
                                                      int index, ActionListener<Object> listener) {

        if (index >= bracketExprs.size()) {
            listener.onResponse(current);
            return;
        }

        evaluateExpressionAsync(bracketExprs.get(index).expression(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object keyObj) {
                if ( (keyObj instanceof String) == false ) {
                    listener.onFailure(new RuntimeException("Document field access requires a string key, but got: " + keyObj));
                    return;
                }
                String key = (String) keyObj;
                if (current instanceof Map) {
                    Object newValue = ((Map<?, ?>) current).get(key);
                    evaluateDocumentFieldAccessRecursive(newValue, bracketExprs, index + 1, listener);
                } else if (current instanceof List) {
                    // list indexing by numeric key
                    List<?> list = (List<?>) current;
                    int idx;
                    try {
                        idx = ((Number) keyObj).intValue();
                    } catch (ClassCastException e) {
                        listener.onFailure(new RuntimeException("List index must be numeric, but got: " + keyObj));
                        return;
                    }
                    if (idx < 0 || idx >= list.size()) {
                        listener.onFailure(new RuntimeException("List index out of bounds: " + idx));
                        return;
                    }
                    Object newValue = list.get(idx);
                    evaluateDocumentFieldAccessRecursive(newValue, bracketExprs, index + 1, listener);
                } else {
                    listener.onFailure(new RuntimeException("Attempted to index into unsupported type: " +
                        (current != null ? current.getClass().getName() : "null")));
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void evaluateExpressionList(List<PlEsqlProcedureParser.ExpressionContext> exprs, ActionListener<Object> listener) {
        evaluateExpressionList(exprs, 0, new ArrayList<>(), listener);
    }

    private void evaluateExpressionList(List<PlEsqlProcedureParser.ExpressionContext> exprs,
                                        int index, List<Object> results, ActionListener<Object> listener) {
        if (index >= exprs.size()) {
            listener.onResponse(results);
            return;
        }
        evaluateExpressionAsync(exprs.get(index), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                results.add(result);
                evaluateExpressionList(exprs, index + 1, results, listener);
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    public void evaluateDocumentLiteralAsync(PlEsqlProcedureParser.DocumentLiteralContext ctx, ActionListener<Object> listener) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<PlEsqlProcedureParser.DocumentFieldContext> fields = ctx.documentField();

        if (fields.isEmpty()) {
            listener.onResponse(result);  // return empty document
            return;
        }

        AtomicInteger remaining = new AtomicInteger(fields.size());
        AtomicBoolean failed = new AtomicBoolean(false);

        for (PlEsqlProcedureParser.DocumentFieldContext fieldCtx : fields) {
            String fieldName = stripQuotes(fieldCtx.STRING().getText());
            evaluateExpressionAsync(fieldCtx.expression(), ActionListener.wrap(
                value -> {
                    result.put(fieldName, value);
                    if (remaining.decrementAndGet() == 0 && failed.get() == false ) {
                        listener.onResponse(result);
                    }
                },
                e -> {
                    if (failed.compareAndSet(false, true)) {
                        listener.onFailure(e);
                    }
                }
            ));
        }
    }

    private String stripQuotes(String quotedString) {
        return quotedString.substring(1, quotedString.length() - 1); // remove surrounding quotes
    }

    private boolean toBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        throw new RuntimeException("Expected a boolean value, but got: " + obj);
    }
}
