/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpressionEvaluator {
    private final ProcedureExecutor executor;
    private final ThreadPool threadPool;
    private final ExecutionContext context;

    public ExpressionEvaluator(ProcedureExecutor executor) {
        this.executor = executor;
        this.threadPool = executor.getThreadPool();
        this.context = executor.getContext();
    }

    public void evaluateExpressionAsync(PlEsqlProcedureParser.ExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx == null) {
            listener.onFailure(new RuntimeException("Null expression context"));
            return;
        }
        evaluateLogicalOrExpressionAsync(ctx.logicalOrExpression(), listener);
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
        if (ctx.relationalExpression().size() == 1) {
            evaluateRelationalExpressionAsync(ctx.relationalExpression(0), listener);
        } else {
            ActionListener<Object> leftListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object leftResult) {
                    ActionListener<Object> rightListener = new ActionListener<Object>() {
                        @Override
                        public void onResponse(Object rightResult) {
                            String operator = ctx.getChild(1).getText();
                            boolean result;
                            try {
                                double leftDouble = ((Number) leftResult).doubleValue();
                                double rightDouble = ((Number) rightResult).doubleValue();
                                switch (operator) {
                                    case "=":
                                        result = leftDouble == rightDouble;
                                        break;
                                    case "<>":
                                        result = leftDouble != rightDouble;
                                        break;
                                    default:
                                        listener.onFailure(new RuntimeException("Unknown equality operator: " + operator));
                                        return;
                                }
                                listener.onResponse(result);
                            } catch (ClassCastException e) {
                                listener.onFailure(new RuntimeException("Relational operations require numeric operands."));
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    };
                    ActionListener<Object> rightLogger = ActionListenerUtils.withLogging(rightListener, getClass().getName(),
                        "Evaluate-Relational Expression (Right): " + ctx.relationalExpression(1));
                    evaluateRelationalExpressionAsync(ctx.relationalExpression(1), rightLogger);
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };
            ActionListener<Object> leftLogger = ActionListenerUtils.withLogging(leftListener, getClass().getName(),
                "Evaluate-Relational Expression (Left): " + ctx.relationalExpression(0));
            evaluateRelationalExpressionAsync(ctx.relationalExpression(0), leftLogger);
        }
    }

    private void evaluateRelationalExpressionAsync(PlEsqlProcedureParser.RelationalExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx.additiveExpression().size() == 1) {
            evaluateAdditiveExpressionAsync(ctx.additiveExpression(0), listener);
        } else {
            ActionListener<Object> leftListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object leftResult) {
                    ActionListener<Object> rightListener = new ActionListener<Object>() {
                        @Override
                        public void onResponse(Object rightResult) {
                            String operator = ctx.getChild(1).getText();
                            boolean result;
                            try {
                                double leftDouble = ((Number) leftResult).doubleValue();
                                double rightDouble = ((Number) rightResult).doubleValue();
                                switch (operator) {
                                    case "<":
                                        result = leftDouble < rightDouble;
                                        break;
                                    case "<=":
                                        result = leftDouble <= rightDouble;
                                        break;
                                    case ">":
                                        result = leftDouble > rightDouble;
                                        break;
                                    case ">=":
                                        result = leftDouble >= rightDouble;
                                        break;
                                    default:
                                        listener.onFailure(new RuntimeException("Unknown relational operator: " + operator));
                                        return;
                                }
                                listener.onResponse(result);
                            } catch (Exception e) {
                                listener.onFailure(new RuntimeException("Relational operations require numeric operands."));
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    };
                    ActionListener<Object> rightLogger = ActionListenerUtils.withLogging(rightListener, getClass().getName(),
                        "Evaluate-Additive Expression (Right): " + ctx.additiveExpression(1));
                    evaluateAdditiveExpressionAsync(ctx.additiveExpression(1), rightLogger);
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };
            ActionListener<Object> leftLogger = ActionListenerUtils.withLogging(leftListener, getClass().getName(),
                "Evaluate-Additive Expression (Left): " + ctx.additiveExpression(0));
            evaluateAdditiveExpressionAsync(ctx.additiveExpression(0), leftLogger);
        }
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
        ActionListener<Object> evalOperandListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object rightValue) {
                try {
                    if (operator.equals("+")) {
                        if (leftValue instanceof String || rightValue instanceof String) {
                            Object result = leftValue.toString() + rightValue.toString();
                            evaluateAdditiveOperandsAsync(ctx, index + 1, result, listener);
                        } else {
                            double leftDouble = ((Number) leftValue).doubleValue();
                            double rightDouble = ((Number) rightValue).doubleValue();
                            double result = leftDouble + rightDouble;
                            evaluateAdditiveOperandsAsync(ctx, index + 1, result, listener);
                        }
                    } else if (operator.equals("-")) {
                        if (leftValue instanceof Number && rightValue instanceof Number) {
                            double leftDouble = ((Number) leftValue).doubleValue();
                            double rightDouble = ((Number) rightValue).doubleValue();
                            double result = leftDouble - rightDouble;
                            evaluateAdditiveOperandsAsync(ctx, index + 1, result, listener);
                        } else {
                            listener.onFailure(new RuntimeException("Subtraction requires numeric operands."));
                        }
                    } else {
                        listener.onFailure(new RuntimeException("Unknown additive operator: " + operator));
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
        PlEsqlProcedureParser.UnaryExprContext currentExpr = ctx.unaryExpr(index);
        String operator = ctx.getChild(2 * index - 1).getText();
        ActionListener<Object> rightListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object rightValue) {
                try {
                    double leftDouble = ((Number) leftValue).doubleValue();
                    double rightDouble = ((Number) rightValue).doubleValue();
                    double result;
                    switch (operator) {
                        case "*":
                            result = leftDouble * rightDouble;
                            break;
                        case "/":
                            if (rightDouble == 0) {
                                listener.onFailure(new RuntimeException("Division by zero."));
                                return;
                            }
                            result = leftDouble / rightDouble;
                            break;
                        default:
                            listener.onFailure(new RuntimeException("Unknown multiplicative operator: " + operator));
                            return;
                    }
                    evaluateMultiplicativeOperandsAsync(ctx, index + 1, result, listener);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };
        ActionListener<Object> rightLogger = ActionListenerUtils.withLogging(rightListener, getClass().getName(),
            "Evaluate-Multiplicative Right Operand: " + currentExpr);
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

        if (ctx.simplePrimaryExpression().LPAREN() != null && ctx.simplePrimaryExpression().RPAREN() != null) {
            evaluateExpressionAsync(ctx.simplePrimaryExpression().expression(), processResult);
        } else if (ctx.simplePrimaryExpression().function_call() != null) {
            // Delegate function call evaluation to the executor's handler
            executor.visitFunctionCallAsync(ctx.simplePrimaryExpression().function_call(), processResult);
        } else if (ctx.simplePrimaryExpression().INT() != null) {
            try {
                processResult.onResponse(Double.valueOf(ctx.simplePrimaryExpression().INT().getText()));
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
        } else if (ctx.simplePrimaryExpression().arrayLiteral() != null) {
            if (ctx.simplePrimaryExpression().arrayLiteral().expressionList() != null) {
                evaluateExpressionList(ctx.simplePrimaryExpression().arrayLiteral().expressionList().expression(), processResult);
            } else {
                processResult.onResponse(new ArrayList<>());
            }
        } else if (ctx.simplePrimaryExpression().ID() != null) {
            String varName = ctx.simplePrimaryExpression().ID().getText();
            if ( context.hasVariable(varName) == false ) {
                listener.onFailure(new RuntimeException("Variable not declared: " + varName));
                return;
            }
            Object varValue = context.getVariable(varName);
            if (varValue == null) {
                listener.onFailure(new RuntimeException("Variable '" + varName + "' is not initialized."));
                return;
            }
            processResult.onResponse(varValue);
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
                } else {
                    listener.onFailure(new RuntimeException("Attempted to access field '" + key +
                        "' on non-document type: " + (current != null ? current.getClass().getName() : "null")));
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

    public void evaluateConditionAsync(PlEsqlProcedureParser.ConditionContext ctx, ActionListener<Object> listener) {
        if (ctx.expression() != null) {
            ActionListener<Object> condListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    if (result instanceof Boolean) {
                        listener.onResponse(result);
                    } else {
                        listener.onFailure(new RuntimeException("Condition does not evaluate to a boolean: " + ctx.getText()));
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> condLogger = ActionListenerUtils.withLogging(condListener, getClass().getName(),
                "Evaluate-Condition: " + ctx.expression());
            evaluateExpressionAsync(ctx.expression(), condLogger);
        } else {
            listener.onFailure(new RuntimeException("Unsupported condition: " + ctx.getText()));
        }
    }

    public void evaluateArgumentsAsync(List<PlEsqlProcedureParser.ExpressionContext> argContexts, ActionListener<List<Object>> listener) {
        List<Object> argValues = new ArrayList<>();
        evaluateArgumentAsync(argContexts, 0, argValues, listener);
    }

    private void evaluateArgumentAsync(List<PlEsqlProcedureParser.ExpressionContext> argContexts, int index, List<Object> argValues, ActionListener<List<Object>> listener) {
        if (index >= argContexts.size()) {
            listener.onResponse(argValues);
            return;
        }
        ActionListener<Object> argListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object value) {
                argValues.add(value);
                evaluateArgumentAsync(argContexts, index + 1, argValues, listener);
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };
        ActionListener<Object> argLogger = ActionListenerUtils.withLogging(argListener, getClass().getName(),
            "Eval-Argument: " + argContexts.get(index));
        evaluateExpressionAsync(argContexts.get(index), argLogger);
    }

    private boolean toBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        throw new RuntimeException("Expected a boolean value, but got: " + obj);
    }
}
