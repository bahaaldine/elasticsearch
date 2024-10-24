/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.exceptions.BreakException;
import org.elasticsearch.xpack.plesql.handlers.AssignmentStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.DeclareStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.ExecuteStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.FunctionDefinitionHandler;
import org.elasticsearch.xpack.plesql.handlers.IfStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.LoopStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.ThrowStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.TryCatchStatementHandler;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureBaseVisitor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.FunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;

import java.util.ArrayList;
import java.util.List;

/**
 * The ProcedureExecutor class is responsible for executing parsed procedural SQL statements.
 * It extends the PlEsqlProcedureBaseVisitor to traverse the parse tree and execute corresponding actions.
 */
public class ProcedureExecutor extends PlEsqlProcedureBaseVisitor<Object> {
    private ExecutionContext context;
    private final ThreadPool threadPool;

    // Instantiate handlers
    private final AssignmentStatementHandler assignmentHandler;
    private final DeclareStatementHandler declareHandler;
    private final IfStatementHandler ifHandler;
    private final LoopStatementHandler loopHandler;
    private final FunctionDefinitionHandler functionDefHandler;
    private final TryCatchStatementHandler tryCatchHandler;
    private final ThrowStatementHandler throwHandler;
    private final ExecuteStatementHandler executeHandler;

    /**
     * Constructs a ProcedureExecutor with the given execution context and thread pool.
     *
     * @param context    The execution context containing variables, functions, etc.
     * @param threadPool The thread pool for executing asynchronous tasks.
     */
    public ProcedureExecutor(ExecutionContext context, ThreadPool threadPool) {
        this.context = context;
        this.threadPool = threadPool;
        this.executeHandler = new ExecuteStatementHandler(this);
        this.assignmentHandler = new AssignmentStatementHandler(this);
        this.declareHandler = new DeclareStatementHandler(this);
        this.ifHandler = new IfStatementHandler(this);
        this.loopHandler = new LoopStatementHandler(this);
        this.functionDefHandler = new FunctionDefinitionHandler(this);
        this.tryCatchHandler = new TryCatchStatementHandler(this);
        this.throwHandler = new ThrowStatementHandler(this);
    }

    /**
     * Retrieves the current thread pool.
     *
     * @return The current thread pool.
     */
    public ThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * Retrieves the current execution context.
     *
     * @return The current ExecutionContext.
     */
    public ExecutionContext getContext() {
        return context;
    }

    /**
     * Updates the execution context to a new context.
     *
     * @param newContext The new ExecutionContext to set.
     */
    public void setContext(ExecutionContext newContext) {
        this.context = newContext;
    }

    /**
     * Visits the entire procedure and executes each statement asynchronously.
     *
     * @param ctx The ProcedureContext representing the entire procedure.
     * @return null as procedures do not return values directly.
     */
    @Override
    public Object visitProcedure(PlEsqlProcedureParser.ProcedureContext ctx) {
        // Start asynchronous execution
        executeProcedureAsync(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                // Execution completed successfully
                // You can handle the result if needed
            }

            @Override
            public void onFailure(Exception e) {
                // Handle execution error
                // You can log the error or propagate it
            }
        });
        return null; // Return immediately as execution is asynchronous
    }

    /**
     * Initiates asynchronous execution of the procedure.
     */
    private void executeProcedureAsync(PlEsqlProcedureParser.ProcedureContext ctx, ActionListener<Object> listener) {
        // Execute statements asynchronously
        executeStatementsAsync(ctx.statement(), 0, listener);
    }

    /**
     * Executes a list of statements asynchronously.
     */
    public void executeStatementsAsync(List<PlEsqlProcedureParser.StatementContext> statements,
                                       int index, ActionListener<Object> listener) {
        if (index >= statements.size()) {
            listener.onResponse(null); // Execution completed
            return;
        }

        PlEsqlProcedureParser.StatementContext statement = statements.get(index);

        // Visit the statement asynchronously
        visitStatementAsync(statement, new ActionListener<>() {
            @Override
            public void onResponse(Object o) {
                executeStatementsAsync(statements, index + 1, listener);
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof ReturnValue) {
                    // Handle function return value
                    listener.onFailure(e);
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }

    /**
     * Visits a statement asynchronously.
     */
    public void visitStatementAsync(PlEsqlProcedureParser.StatementContext ctx, ActionListener<Object> listener) {
        if (ctx.declare_statement() != null) {
            declareHandler.handleAsync(ctx.declare_statement(), listener);
        } else if (ctx.assignment_statement() != null) {
            assignmentHandler.handleAsync(ctx.assignment_statement(), listener);
        } else if (ctx.if_statement() != null) {
            ifHandler.handleAsync(ctx.if_statement(), listener);
        } else if (ctx.loop_statement() != null) {
            loopHandler.handleAsync(ctx.loop_statement(), listener);
        } else if (ctx.function_definition() != null) {
            functionDefHandler.handleAsync(ctx.function_definition(), listener);
        } else if (ctx.try_catch_statement() != null) {
            tryCatchHandler.handleAsync(ctx.try_catch_statement(), listener);
        } else if (ctx.throw_statement() != null) {
            throwHandler.handleAsync(ctx.throw_statement(), listener);
        } else if (ctx.execute_statement() != null) {
            executeHandler.handleAsync(ctx.execute_statement(), listener);
        } else if (ctx.return_statement() != null) {
            visitReturn_statementAsync(ctx.return_statement(), listener);
        } else if (ctx.break_statement() != null) {
            // Handle break statement
            listener.onFailure(new BreakException("Break encountered"));
        } else if (ctx.expression_statement() != null) {
            // Evaluate the expression asynchronously but ignore the result
            evaluateExpressionAsync(ctx.expression_statement().expression(), new ActionListener<>() {
                @Override
                public void onResponse(Object value) {
                    listener.onResponse(null);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        } else if (ctx.function_call_statement() != null) {
            // Handle function call statements
            visitFunctionCallAsync(ctx.function_call_statement().function_call(), new ActionListener<>() {
                @Override
                public void onResponse(Object result) {
                    listener.onResponse(null); // Function call completed
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        } else {
            listener.onResponse(null);
        }
    }

    /**
     * Handles return statements asynchronously.
     */
    private void visitReturn_statementAsync(PlEsqlProcedureParser.Return_statementContext ctx, ActionListener<Object> listener) {
        evaluateExpressionAsync(ctx.expression(), new ActionListener<>() {
            @Override
            public void onResponse(Object value) {
                listener.onFailure(new ReturnValue(value)); // Signal return value
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    // =======================
    // Asynchronous Evaluation Methods
    // =======================

    /**
     * Evaluates an expression asynchronously.
     */
    public void evaluateExpressionAsync(PlEsqlProcedureParser.ExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx == null) {
            listener.onFailure(new RuntimeException("Null expression context"));
            return;
        }
        evaluateLogicalOrExpressionAsync(ctx.logicalOrExpression(), listener);
    }

    /**
     * Evaluates a logical OR expression asynchronously.
     */
    private void evaluateLogicalOrExpressionAsync(PlEsqlProcedureParser.LogicalOrExpressionContext ctx, ActionListener<Object> listener) {
        List<PlEsqlProcedureParser.LogicalAndExpressionContext> andExprs = ctx.logicalAndExpression();
        if (andExprs.size() == 1) {
            // Only one operand
            evaluateLogicalAndExpressionAsync(andExprs.get(0), listener);
        } else {
            // Evaluate operands one by one
            evaluateLogicalOrOperandsAsync(andExprs, 0, listener);
        }
    }

    /**
     * Helper method to evaluate logical OR operands asynchronously.
     */
    private void evaluateLogicalOrOperandsAsync(List<PlEsqlProcedureParser.LogicalAndExpressionContext> operands, int index,
                                                ActionListener<Object> listener) {
        if (index >= operands.size()) {
            listener.onResponse(false); // No operands evaluated to true
            return;
        }

        evaluateLogicalAndExpressionAsync(operands.get(index), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                if (toBoolean(result)) {
                    listener.onResponse(true); // Short-circuit: operand is true
                } else {
                    // Proceed to the next operand
                    evaluateLogicalOrOperandsAsync(operands, index + 1, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Evaluates a logical AND expression asynchronously.
     */
    private void evaluateLogicalAndExpressionAsync(PlEsqlProcedureParser.LogicalAndExpressionContext ctx, ActionListener<Object> listener) {
        List<PlEsqlProcedureParser.EqualityExpressionContext> eqExprs = ctx.equalityExpression();
        if (eqExprs.size() == 1) {
            // Only one operand
            evaluateEqualityExpressionAsync(eqExprs.get(0), listener);
        } else {
            // Evaluate operands one by one
            evaluateLogicalAndOperandsAsync(eqExprs, 0, listener);
        }
    }

    /**
     * Helper method to evaluate logical AND operands asynchronously.
     */
    private void evaluateLogicalAndOperandsAsync(List<PlEsqlProcedureParser.EqualityExpressionContext> operands, int index,
                                                 ActionListener<Object> listener) {
        if (index >= operands.size()) {
            listener.onResponse(true); // All operands evaluated to true
            return;
        }

        evaluateEqualityExpressionAsync(operands.get(index), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                if ( toBoolean(result) == false ) {
                    listener.onResponse(false); // Short-circuit: operand is false
                } else {
                    // Proceed to the next operand
                    evaluateLogicalAndOperandsAsync(operands, index + 1, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Evaluates an equality expression asynchronously.
     */
    private void evaluateEqualityExpressionAsync(PlEsqlProcedureParser.EqualityExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx.relationalExpression().size() == 1) {
            // Only one operand
            evaluateRelationalExpressionAsync(ctx.relationalExpression(0), listener);
        } else {
            // Evaluate both operands
            evaluateRelationalExpressionAsync(ctx.relationalExpression(0), new ActionListener<>() {
                @Override
                public void onResponse(Object leftResult) {
                    evaluateRelationalExpressionAsync(ctx.relationalExpression(1), new ActionListener<>() {
                        @Override
                        public void onResponse(Object rightResult) {
                            String operator = ctx.getChild(1).getText();
                            boolean result;
                            switch (operator) {
                                case "=":
                                    result = leftResult.equals(rightResult);
                                    break;
                                case "<>":
                                    result = leftResult.equals(rightResult) == false;
                                    break;
                                default:
                                    listener.onFailure(new RuntimeException("Unknown equality operator: " + operator));
                                    return;
                            }
                            listener.onResponse(result);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        }
    }

    /**
     * Evaluates a relational expression asynchronously.
     */
    private void evaluateRelationalExpressionAsync(PlEsqlProcedureParser.RelationalExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx.additiveExpression().size() == 1) {
            // Only one operand
            evaluateAdditiveExpressionAsync(ctx.additiveExpression(0), listener);
        } else {
            // Evaluate both operands
            evaluateAdditiveExpressionAsync(ctx.additiveExpression(0), new ActionListener<>() {
                @Override
                public void onResponse(Object leftResult) {
                    evaluateAdditiveExpressionAsync(ctx.additiveExpression(1), new ActionListener<>() {
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
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        }
    }

    /**
     * Evaluates an additive expression asynchronously.
     */
    private void evaluateAdditiveExpressionAsync(PlEsqlProcedureParser.AdditiveExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx.multiplicativeExpression().size() == 1) {
            // Only one operand
            evaluateMultiplicativeExpressionAsync(ctx.multiplicativeExpression(0), listener);
        } else {
            // Evaluate operands sequentially
            evaluateAdditiveOperandsAsync(ctx, 0, listener);
        }
    }

    /**
     * Helper method to evaluate additive operands asynchronously.
     */
    private void evaluateAdditiveOperandsAsync(PlEsqlProcedureParser.AdditiveExpressionContext ctx, int index,
                                               ActionListener<Object> listener) {
        if (index >= ctx.multiplicativeExpression().size()) {
            listener.onFailure(new RuntimeException("Additive expression evaluation error."));
            return;
        }

        evaluateMultiplicativeExpressionAsync(ctx.multiplicativeExpression(index), new ActionListener<>() {
            @Override
            public void onResponse(Object leftResult) {
                if (index + 1 >= ctx.multiplicativeExpression().size()) {
                    listener.onResponse(leftResult);
                    return;
                }

                String operator = ctx.getChild(2 * index + 1).getText();

                evaluateAdditiveOperandsAsync(ctx, index + 1, new ActionListener<>() {
                    @Override
                    public void onResponse(Object rightResult) {
                        try {
                            if (leftResult instanceof Number && rightResult instanceof Number) {
                                Number result;
                                switch (operator) {
                                    case "+":
                                        result = ((Number) leftResult).doubleValue() + ((Number) rightResult).doubleValue();
                                        break;
                                    case "-":
                                        result = ((Number) leftResult).doubleValue() - ((Number) rightResult).doubleValue();
                                        break;
                                    default:
                                        listener.onFailure(new RuntimeException("Unknown additive operator: " + operator));
                                        return;
                                }
                                listener.onResponse(result);
                            } else {
                                listener.onFailure(new RuntimeException("Additive operations require numeric operands."));
                            }
                        } catch (Exception e) {
                            listener.onFailure(e);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Evaluates a multiplicative expression asynchronously.
     */
    private void evaluateMultiplicativeExpressionAsync(PlEsqlProcedureParser.MultiplicativeExpressionContext ctx,
                                                       ActionListener<Object> listener) {
        if (ctx.unaryExpr().size() == 1) {
            // Only one operand
            evaluateUnaryExpressionAsync(ctx.unaryExpr(0), listener);
        } else {
            // Evaluate operands sequentially
            evaluateMultiplicativeOperandsAsync(ctx, 0, listener);
        }
    }

    /**
     * Helper method to evaluate multiplicative operands asynchronously.
     */
    private void evaluateMultiplicativeOperandsAsync(PlEsqlProcedureParser.MultiplicativeExpressionContext ctx, int index,
                                                     ActionListener<Object> listener) {
        if (index >= ctx.unaryExpr().size()) {
            listener.onFailure(new RuntimeException("Multiplicative expression evaluation error."));
            return;
        }

        evaluateUnaryExpressionAsync(ctx.unaryExpr(index), new ActionListener<>() {
            @Override
            public void onResponse(Object leftResult) {
                if (index + 1 >= ctx.unaryExpr().size()) {
                    listener.onResponse(leftResult);
                    return;
                }

                String operator = ctx.getChild(2 * index + 1).getText();

                evaluateMultiplicativeOperandsAsync(ctx, index + 1, new ActionListener<>() {
                    @Override
                    public void onResponse(Object rightResult) {
                        try {
                            if (leftResult instanceof Number && rightResult instanceof Number) {
                                Number result;
                                switch (operator) {
                                    case "*":
                                        result = ((Number) leftResult).doubleValue() * ((Number) rightResult).doubleValue();
                                        break;
                                    case "/":
                                        result = ((Number) leftResult).doubleValue() / ((Number) rightResult).doubleValue();
                                        break;
                                    default:
                                        listener.onFailure(new RuntimeException("Unknown multiplicative operator: " + operator));
                                        return;
                                }
                                listener.onResponse(result);
                            } else {
                                listener.onFailure(new RuntimeException("Multiplicative operations require numeric operands."));
                            }
                        } catch (Exception e) {
                            listener.onFailure(e);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Evaluates a unary expression asynchronously.
     */
    private void evaluateUnaryExpressionAsync(PlEsqlProcedureParser.UnaryExprContext ctx, ActionListener<Object> listener) {
        if (ctx.primaryExpression() != null) {
            evaluatePrimaryExpressionAsync(ctx.primaryExpression(), listener);
        } else if (ctx.getChild(0) != null) {
            // Unary operator
            evaluateUnaryExpressionAsync(ctx.unaryExpr(), new ActionListener<>() {
                @Override
                public void onResponse(Object result) {
                    try {
                        String operator = ctx.getChild(0).getText();
                        if (operator.equals("-") && result instanceof Number) {
                            Number value = (Number) result;
                            listener.onResponse(-value.doubleValue());
                        } else if (operator.equals("NOT")) {
                            listener.onResponse( toBoolean(result) == false );
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
            });
        }
    }

    /**
     * Evaluates a primary expression asynchronously.
     */
    private void evaluatePrimaryExpressionAsync(PlEsqlProcedureParser.PrimaryExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx.LPAREN() != null && ctx.RPAREN() != null) {
            // Parenthesized expression
            evaluateExpressionAsync(ctx.expression(), listener);
        } else if (ctx.function_call() != null) {
            // Function call
            visitFunctionCallAsync(ctx.function_call(), listener);
        } else if (ctx.INT() != null) {
            // Integer literal
            listener.onResponse(Integer.parseInt(ctx.INT().getText()));
        } else if (ctx.FLOAT() != null) {
            // Float literal
            listener.onResponse(Double.parseDouble(ctx.FLOAT().getText()));
        } else if (ctx.STRING() != null) {
            // String literal
            String text = ctx.STRING().getText();
            // Remove the surrounding single quotes and handle escaped characters
            listener.onResponse(text.substring(1, text.length() - 1).replace("\\'", "'"));
        } else if (ctx.ID() != null) {
            // Identifier (variable)
            String varName = ctx.ID().getText();
            if ( context.hasVariable(varName) == false ) {
                listener.onFailure(new RuntimeException("Variable not declared: " + varName));
                return;
            }
            Object varValue = context.getVariable(varName);
            if (varValue == null) {
                listener.onFailure(new RuntimeException("Variable '" + varName + "' is not initialized."));
                return;
            }
            listener.onResponse(varValue);
        } else {
            listener.onFailure(new RuntimeException("Unsupported primary expression: " + ctx.getText()));
        }
    }

    /**
     * Evaluates a condition asynchronously.
     */
    public void evaluateConditionAsync(PlEsqlProcedureParser.ConditionContext ctx, ActionListener<Object> listener) {
        if (ctx.expression() != null) {
            evaluateExpressionAsync(ctx.expression(), new ActionListener<>() {
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
            });
        } else {
            listener.onFailure(new RuntimeException("Unsupported condition: " + ctx.getText()));
        }
    }

    // =======================
    // Function Call Handling
    // =======================

    /**
     * Visits a function call asynchronously.
     *
     * @param ctx      The Function_callContext representing the function call.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    public void visitFunctionCallAsync(PlEsqlProcedureParser.Function_callContext ctx, ActionListener<Object> listener) {
        String functionName = ctx.ID().getText();
        FunctionDefinition function = context.getFunction(functionName);

        if (function == null) {
            listener.onFailure(new RuntimeException("Function not defined: " + functionName));
            return;
        }

        // Evaluate arguments asynchronously
        List<PlEsqlProcedureParser.ExpressionContext> argContexts =
            ctx.argument_list() != null ? ctx.argument_list().expression() : new ArrayList<>();

        evaluateArgumentsAsync(argContexts, new ActionListener<>() {
            @Override
            public void onResponse(List<Object> argValues) {
                // Execute the function asynchronously
                functionDefHandler.executeFunctionAsync(functionName, argValues, new ActionListener<>() {
                    @Override
                    public void onResponse(Object result) {
                        listener.onResponse(result);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Evaluates a list of arguments asynchronously.
     */
    private void evaluateArgumentsAsync(List<PlEsqlProcedureParser.ExpressionContext> argContexts, ActionListener<List<Object>> listener) {
        List<Object> argValues = new ArrayList<>();
        evaluateArgumentAsync(argContexts, 0, argValues, listener);
    }

    private void evaluateArgumentAsync(List<PlEsqlProcedureParser.ExpressionContext> argContexts, int index, List<Object> argValues,
                                       ActionListener<List<Object>> listener) {
        if (index >= argContexts.size()) {
            listener.onResponse(argValues);
            return;
        }

        evaluateExpressionAsync(argContexts.get(index), new ActionListener<>() {
            @Override
            public void onResponse(Object value) {
                argValues.add(value);
                evaluateArgumentAsync(argContexts, index + 1, argValues, listener);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    // =======================
    // Helper Methods
    // =======================

    private Object negate(Object operand) {
        if (operand instanceof Number) {
            if (operand instanceof Integer) {
                return -((Integer) operand);
            } else {
                return -((Number) operand).doubleValue();
            }
        }
        throw new RuntimeException("Unary minus can only be applied to numbers.");
    }

    /**
     * Converts an Object to a boolean. Throws an exception if the object is not a boolean.
     */
    private boolean toBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        throw new RuntimeException("Expected a boolean value, but got: " + obj);
    }

    // =======================
    // Thread Pool Execution
    // =======================

    /**
     * Executes a runnable asynchronously using the thread pool.
     */
    private void executeAsync(Runnable runnable) {
        threadPool.generic().execute(runnable);
    }
}
