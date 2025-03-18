/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
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
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

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
    private final CommonTokenStream tokenStream;

    /**
     * Constructs a ProcedureExecutor with the given execution context and thread pool.
     *
     * @param context    The execution context containing variables, functions, etc.
     * @param threadPool The thread pool for executing asynchronous tasks.
     */
    @SuppressWarnings("this-escape")
    public ProcedureExecutor(ExecutionContext context, ThreadPool threadPool
        , Client client, CommonTokenStream tokenStream) {
        this.context = context;
        this.threadPool = threadPool;
        this.executeHandler = new ExecuteStatementHandler(this, client);
        this.assignmentHandler = new AssignmentStatementHandler(this);
        this.declareHandler = new DeclareStatementHandler(this);
        this.ifHandler = new IfStatementHandler(this);
        this.loopHandler = new LoopStatementHandler(this);
        this.functionDefHandler = new FunctionDefinitionHandler(this);
        this.tryCatchHandler = new TryCatchStatementHandler(this);
        this.throwHandler = new ThrowStatementHandler(this);
        this.tokenStream = tokenStream;
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
     * Retrieves the raw text from the context
     *
     * @return The raw text from the context
     */
    public String getRawText(ParserRuleContext ctx) {
        return tokenStream.getText(ctx);
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
     * Asynchronously visits the entire procedure and executes each statement.
     *
     * @param ctx      The ProcedureContext representing the entire procedure.
     * @param listener The ActionListener to notify upon completion or failure.
     */
    public void visitProcedureAsync(PlEsqlProcedureParser.ProcedureContext ctx, ActionListener<Object> listener) {
        // Start asynchronous execution
        executeProcedureAsync(ctx, listener);
    }

    /**
     * Synchronously visits the entire procedure.
     * Since execution is asynchronous, this method returns immediately.
     *
     * @param ctx The ProcedureContext representing the entire procedure.
     * @return null as procedures do not return values directly.
     */
    @Override
    public Object visitProcedure(PlEsqlProcedureParser.ProcedureContext ctx) {

        ActionListener<Object> visitProcedureListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // Execution completed successfully
            }

            @Override
            public void onFailure(Exception e) {
                // Handle execution error
            }
        };

        ActionListener<Object> visitProcedureLogger = ActionListenerUtils.withLogging(visitProcedureListener, this.getClass().getName(),
            "Visit-Procedure: " + ctx.getText());

        visitProcedureAsync(ctx, visitProcedureLogger);
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

        ActionListener<Object> procedureExecutorExecuteStatementListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object o) {
                if (o instanceof ReturnValue) {
                    System.out.println("Returned value  :");
                    System.out.println(((ReturnValue) o).getValue());
                    listener.onResponse(((ReturnValue) o).getValue());
                } else {
                    executeStatementsAsync(statements, index + 1, listener);
                }
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
        };

        ActionListener<Object> procedureExecutorExecuteStatementLogger =
            ActionListenerUtils.withLogging(procedureExecutorExecuteStatementListener, this.getClass().getName(),
                "Execute-Statement-Async");

        visitStatementAsync(statement, procedureExecutorExecuteStatementLogger);
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
            ActionListener<Object> expressionEvalListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object value) {
                    listener.onResponse(value);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> expressionEvalLogger = ActionListenerUtils.withLogging(expressionEvalListener,
                this.getClass().getName(),
                "Expression-Eval:" + ctx.expression_statement().expression());

            evaluateExpressionAsync(ctx.expression_statement().expression(), expressionEvalLogger);
        } else if (ctx.function_call_statement() != null) {
            // Handle function call statements
            ActionListener<Object> functionCallListener= new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    listener.onResponse(null); // Function call completed
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> functionCallLogger = ActionListenerUtils.withLogging(functionCallListener, this.getClass().getName(),
                "Function-Call: " + ctx.function_call_statement().function_call());

            visitFunctionCallAsync(ctx.function_call_statement().function_call(), functionCallLogger);
        } else {
            listener.onResponse(null);
        }
    }

    /**
     * Handles return statements asynchronously.
     */
    private void visitReturn_statementAsync(PlEsqlProcedureParser.Return_statementContext ctx, ActionListener<Object> listener) {
        ActionListener<Object> visitReturnStatementListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object value) {
                listener.onResponse(new ReturnValue(value)); // Signal return value
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> visitReturnStatementLogger = ActionListenerUtils.withLogging(visitReturnStatementListener,
            this.getClass().getName(),
            "Visit-Return-Statement: " + ctx.expression().getText());

        evaluateExpressionAsync(ctx.expression(), visitReturnStatementLogger );
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

        ActionListener<Object> evaluateLogicalOrOperandsAsyncListener = new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                boolean booleanResult = toBoolean(result);
                if (booleanResult) {
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
        };

        ActionListener<Object> evaluateLogicalOrOperandsAsyncLogger =
            ActionListenerUtils.withLogging(evaluateLogicalOrOperandsAsyncListener,
                this.getClass().getName(),
            "Evaluate-Logical-Or-Operands: " + operands.get(index));

        evaluateLogicalAndExpressionAsync(operands.get(index), evaluateLogicalOrOperandsAsyncLogger);
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

        ActionListener<Object> evaluateLogicalAndOperandsAsyncListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                boolean booleanResult = toBoolean(result);
                if ( booleanResult == false ) {
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
        };

        ActionListener<Object> evaluateLogicalAndOperandsAsyncLogger =
            ActionListenerUtils.withLogging(evaluateLogicalAndOperandsAsyncListener, this.getClass().getName(),
                "Evaluate-Logical-And-Operands: " + operands.get(index));

        evaluateEqualityExpressionAsync(operands.get(index), evaluateLogicalAndOperandsAsyncLogger);
    }

    /**
     * Evaluates an equality expression asynchronously.
     */
    /**
     * Evaluates an equality expression asynchronously.
     */
    private void evaluateEqualityExpressionAsync(
        PlEsqlProcedureParser.EqualityExpressionContext ctx,
        ActionListener<Object> listener
    ) {
        if (ctx.relationalExpression().size() == 1) {
            // Only one operand
            evaluateRelationalExpressionAsync(ctx.relationalExpression(0), listener);
        } else {
            // Evaluate both operands asynchronously

            ActionListener<Object> evaluateLeftOperandListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object leftResult) {
                    System.out.println("Left Expression :  " + leftResult);

                    ActionListener<Object> evaluateRightOperandListener = new ActionListener<Object>() {
                        @Override
                        public void onResponse(Object rightResult) {
                            String operator = ctx.getChild(1).getText();
                            boolean result;

                            try {
                                // Normalize both operands to Double
                                double leftDouble = ((Number) leftResult).doubleValue();
                                double rightDouble = ((Number) rightResult).doubleValue();

                                switch (operator) {
                                    case "=":
                                        result = leftDouble == rightDouble;

                                        System.out.println("Evaluating EqualityExpression: " + leftDouble + " " + operator + " "
                                            + rightDouble);
                                        System.out.println("       Result:  " + result);

                                        break;
                                    case "<>":
                                        result = leftDouble != rightDouble;
                                        break;
                                    default:
                                        listener.onFailure(new RuntimeException("Unknown equality operator: " + operator));
                                        return;
                                }
                                System.out.println("       Result:  " + result);
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

                    ActionListener<Object> evaluateRightOperandLogger =
                        ActionListenerUtils.withLogging(evaluateRightOperandListener, this.getClass().getName(),
                            "Evaluate-Logical-And-Operands: " + ctx.relationalExpression(1));

                    evaluateRelationalExpressionAsync(ctx.relationalExpression(1), evaluateRightOperandLogger);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> evaluateLeftOperandLgogger =
                ActionListenerUtils.withLogging(evaluateLeftOperandListener, this.getClass().getName(),
                    "Evaluate-Left-Operand: " + ctx.relationalExpression(0));
            evaluateRelationalExpressionAsync(ctx.relationalExpression(0), evaluateLeftOperandLgogger);
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

            ActionListener<Object> evaluateLeftOperandListener = new ActionListener<>() {
                @Override
                public void onResponse(Object leftResult) {

                    ActionListener<Object> evaluateRightOperandListener = new ActionListener<>() {
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

                    ActionListener<Object> evaluateRightOperandLogger =
                        ActionListenerUtils.withLogging(evaluateRightOperandListener, this.getClass().getName(),
                            "Evaluate-Additive-Expression-Right-Operand: " + ctx.additiveExpression(1));
                    evaluateAdditiveExpressionAsync(ctx.additiveExpression(1), evaluateRightOperandLogger);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> evaluateLeftOperandLogger =
                ActionListenerUtils.withLogging(evaluateLeftOperandListener, this.getClass().getName(),
                    "Evaluate-Additive-Expression-Left-Operand: " + ctx.additiveExpression(0));
            evaluateAdditiveExpressionAsync(ctx.additiveExpression(0), evaluateLeftOperandLogger);
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
            // Evaluate operands sequentially with accumulated value
            // Initialize with the first operand
            ActionListener<Object> evaluateOperandListener = new ActionListener<>() {
                @Override
                public void onResponse(Object initialValue) {
                    evaluateAdditiveOperandsAsync(ctx, 1, initialValue, listener);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> evaluateOperandLogger =
                ActionListenerUtils.withLogging(evaluateOperandListener, this.getClass().getName(),
                    "Evaluate-Additive-Expression: " + ctx.multiplicativeExpression(0));

            evaluateMultiplicativeExpressionAsync(ctx.multiplicativeExpression(0), evaluateOperandLogger);
        }
    }

    /**
     * Helper method to evaluate additive operands asynchronously.
     * This method has been corrected to accept four arguments: ctx, index, leftValue, listener.
     */
    private void evaluateAdditiveOperandsAsync(
        PlEsqlProcedureParser.AdditiveExpressionContext ctx,
        int index,
        Object leftValue,
        ActionListener<Object> listener
    ) {
        if (index >= ctx.multiplicativeExpression().size()) {
            listener.onResponse(leftValue); // All operands evaluated
            return;
        }

        PlEsqlProcedureParser.MultiplicativeExpressionContext currentExpr = ctx.multiplicativeExpression(index);
        // Correctly extract the operator based on the grammar. This assumes the operator is at position 2*index - 1.
        String operator = ctx.getChild(2 * index - 1).getText();

        ActionListener<Object> evaluateAdditiveOperand = new ActionListener<>() {
            @Override
            public void onResponse(Object rightValue) {
                try {
                    if (operator.equals("+")) {
                        // If either operand is a String, perform string concatenation.
                        if (leftValue instanceof String || rightValue instanceof String) {
                            Object result = leftValue.toString() + rightValue.toString();
                            evaluateAdditiveOperandsAsync(ctx, index + 1, result, listener);
                        } else {
                            // Otherwise, both operands are numbers, so perform numeric addition.
                            double leftDouble = ((Number) leftValue).doubleValue();
                            double rightDouble = ((Number) rightValue).doubleValue();
                            double result = leftDouble + rightDouble;
                            evaluateAdditiveOperandsAsync(ctx, index + 1, result, listener);
                        }
                    } else if (operator.equals("-")) {
                        // Subtraction only supports numeric operands.
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

        ActionListener<Object> evaluateOperandLogger =
            ActionListenerUtils.withLogging(evaluateAdditiveOperand, this.getClass().getName(),
                "Evaluate-Additive-Operands: " + currentExpr);

        evaluateMultiplicativeExpressionAsync(currentExpr, evaluateOperandLogger);
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
            // Evaluate operands sequentially with accumulated value
            // Initialize with the first operand

            ActionListener<Object> evalMultiplicativeExpressionListener = new ActionListener<>() {
                @Override
                public void onResponse(Object initialValue) {
                    evaluateMultiplicativeOperandsAsync(ctx, 1, initialValue, listener);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> evalMultiplicativeExpressionLogger =
                ActionListenerUtils.withLogging(evalMultiplicativeExpressionListener, this.getClass().getName(),
                    "Evaluate-Multiplicative-Expression: " + ctx.unaryExpr(0));

            evaluateUnaryExpressionAsync(ctx.unaryExpr(0), evalMultiplicativeExpressionLogger);
        }
    }

    /**
     * Helper method to evaluate multiplicative operands asynchronously.
     */
    private void evaluateMultiplicativeOperandsAsync(PlEsqlProcedureParser.MultiplicativeExpressionContext ctx, int index, Object leftValue,
                                                     ActionListener<Object> listener) {
        if (index >= ctx.unaryExpr().size()) {
            listener.onResponse(leftValue); // All operands evaluated
            return;
        }

        PlEsqlProcedureParser.UnaryExprContext currentExpr = ctx.unaryExpr(index);
        String operator = ctx.getChild(2 * index - 1).getText(); // Adjusted to correctly fetch the operator

        ActionListener<Object> evalMultiplicativeRightOperandListener = new ActionListener<>() {
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
                    // Recursively evaluate the next operand with the new accumulated result
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

        ActionListener<Object> evalMultiplicativeRightOperandLogger =
            ActionListenerUtils.withLogging(evalMultiplicativeRightOperandListener, this.getClass().getName(),
                "Evaluate-Multiplicative-Right-Operand: " + currentExpr);

        evaluateUnaryExpressionAsync(currentExpr, evalMultiplicativeRightOperandLogger);
    }

    /**
     * Evaluates a unary expression asynchronously.
     */
    @SuppressWarnings("checkstyle:DescendantToken")
    private void evaluateUnaryExpressionAsync(PlEsqlProcedureParser.UnaryExprContext ctx, ActionListener<Object> listener) {
        if (ctx.primaryExpression() != null) {
            evaluatePrimaryExpressionAsync(ctx.primaryExpression(), listener);
        } else if (ctx.unaryExpr() != null) {
            // Unary operator
            String operator = ctx.getChild(0).getText();


            ActionListener<Object> evalUnaryExpressionListener = new ActionListener<>() {
                @SuppressWarnings("checkstyle:DescendantToken")
                @Override
                public void onResponse(Object result) {
                    try {
                        if (operator.equals("-")) {
                            if (result instanceof Number) {
                                double value = ((Number) result).doubleValue();
                                double negatedValue = -value;
                                listener.onResponse(negatedValue);
                            } else {
                                listener.onFailure(new RuntimeException("Unary minus can only be applied to numbers."));
                            }
                        } else if (operator.equals("NOT")) {
                            if (result instanceof Boolean) {
                                boolean boolResult = ((Boolean) result).booleanValue();
                                boolean negatedBool = !boolResult;
                                listener.onResponse(negatedBool);
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

            ActionListener<Object> evalUnaryExpressionLogger =
                ActionListenerUtils.withLogging(evalUnaryExpressionListener, this.getClass().getName(),
                    "Evaluate-Unary-Expression: " + ctx.unaryExpr());

            evaluateUnaryExpressionAsync(ctx.unaryExpr(), evalUnaryExpressionLogger);
        } else {
            listener.onFailure(new RuntimeException("Unsupported unary expression: " + ctx.getText()));
        }
    }

    /**
     * Evaluates a primary expression asynchronously.
     * <p>
     * This method evaluates the base expression (e.g., literals, identifiers, function calls, etc.) and then checks for
     * any bracket expressions attached (to support DOCUMENT field access using bracket notation). If bracket expressions
     * exist, they are processed via {@code evaluateDocumentFieldAccessRecursive}; otherwise, the base value is returned.
     *
     * @param ctx      the PrimaryExpressionContext representing the expression.
     * @param listener the ActionListener to receive the evaluated result.
     */
    private void evaluatePrimaryExpressionAsync(PlEsqlProcedureParser.PrimaryExpressionContext ctx, ActionListener<Object> listener) {
        // Create a helper listener to process bracket expressions if they exist.
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
            // Parenthesized expression
            evaluateExpressionAsync(ctx.simplePrimaryExpression().expression(), processResult);
        } else if (ctx.simplePrimaryExpression().function_call() != null) {
            // Function call
            visitFunctionCallAsync(ctx.simplePrimaryExpression().function_call(), processResult);
        } else if (ctx.simplePrimaryExpression().INT() != null) {
            // Integer literal
            try {
                processResult.onResponse(Double.valueOf(ctx.simplePrimaryExpression().INT().getText()));
            } catch (NumberFormatException e) {
                listener.onFailure(new RuntimeException("Invalid integer literal: " + ctx.simplePrimaryExpression().INT().getText()));
            }
        } else if (ctx.simplePrimaryExpression().FLOAT() != null) {
            // Float literal
            try {
                processResult.onResponse(Double.valueOf(ctx.simplePrimaryExpression().FLOAT().getText()));
            } catch (NumberFormatException e) {
                listener.onFailure(new RuntimeException("Invalid float literal: " + ctx.simplePrimaryExpression().FLOAT().getText()));
            }
        } else if (ctx.simplePrimaryExpression().STRING() != null) {
            // String literal
            String text = ctx.simplePrimaryExpression().STRING().getText();
            // Remove the surrounding single quotes and handle escaped characters
            String processedString = text.substring(1, text.length() - 1).replace("\\'", "'");
            processResult.onResponse(processedString);
        } else if (ctx.simplePrimaryExpression().arrayLiteral() != null) {
            // Evaluate array literal.
            if (ctx.simplePrimaryExpression().arrayLiteral().expressionList() != null) {
                evaluateExpressionList(ctx.simplePrimaryExpression().arrayLiteral().expressionList().expression(), processResult);
            } else {
                processResult.onResponse(new java.util.ArrayList<>());
            }
        } else if (ctx.simplePrimaryExpression().ID() != null) {
            // Identifier (variable)
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


    /**
     * Recursively evaluates bracket expressions attached to a primary expression.
     * <p>
     * For a DOCUMENT type variable (represented as a Map), each bracket expression is evaluated to a string key,
     * which is then used to retrieve the nested field. This supports chained access such as:
     * {@code myvar['field1']['field2']}.
     *
     * @param current      the current value (starting with the base DOCUMENT value)
     * @param bracketExprs the list of bracket expression contexts
     * @param index        the current index in the bracket expressions list
     * @param listener     the ActionListener to be notified with the final value after all bracket accesses
     */
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
                if (current instanceof java.util.Map) {
                    Object newValue = ((java.util.Map<?, ?>) current).get(key);
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

    /**
     * Evaluates a list of expression contexts sequentially and collects their evaluated
     * results into a Java List.
     * <p>
     * This method is a convenience wrapper that initializes the accumulator (an empty List)
     * and calls the recursive helper to process each expression in order.
     *
     * @param exprs    the list of ExpressionContext objects representing the array literal elements
     * @param listener the ActionListener to be notified when evaluation is complete, with the resulting List as its response
     */
    private void evaluateExpressionList(List<PlEsqlProcedureParser.ExpressionContext> exprs,
                                        ActionListener<Object> listener) {
        evaluateExpressionList(exprs, 0, new java.util.ArrayList<>(), listener);
    }

    /**
     * Recursively evaluates a list of expression contexts starting from the specified index,
     * accumulating the results in the provided list.
     * <p>
     * Once all expressions have been evaluated, the listener is notified with the complete list
     * of evaluation results.
     *
     * @param exprs    the list of ExpressionContext objects to be evaluated
     * @param index    the current index in the list from which to start evaluation
     * @param results  an accumulator List that stores the evaluated result of each expression
     * @param listener the ActionListener to be notified with the final List of evaluation results
     */
    private void evaluateExpressionList(List<PlEsqlProcedureParser.ExpressionContext> exprs, int index,
                                        java.util.List<Object> results, ActionListener<Object> listener) {
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

    /**
     * Evaluates a condition asynchronously.
     */
    public void evaluateConditionAsync(PlEsqlProcedureParser.ConditionContext ctx, ActionListener<Object> listener) {
        if (ctx.expression() != null) {
            System.out.println("Expression : " + ctx.expression().getText());

            ActionListener<Object> evalConditionListener = new ActionListener<>() {
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

            ActionListener<Object> evalConditionLogger =
                ActionListenerUtils.withLogging(evalConditionListener, this.getClass().getName(),
                    "Evaluate-Condition: " + ctx.expression());

            evaluateExpressionAsync(ctx.expression(), evalConditionLogger);
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
            // If the function is not defined, treat it as an unsupported expression
            listener.onFailure(new RuntimeException("Unsupported expression"));
            return;
        }

        // Evaluate arguments asynchronously
        List<PlEsqlProcedureParser.ExpressionContext> argContexts =
            ctx.argument_list() != null ? ctx.argument_list().expression() : new ArrayList<>();

        ActionListener<List<Object>> visitFunctionCallListener = new ActionListener<>() {
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
        };

        ActionListener<List<Object>> visitFunctionCallLogger =
            ActionListenerUtils.withLogging(visitFunctionCallListener, this.getClass().getName(),
                "Visit-Function-Call: " + argContexts);

        evaluateArgumentsAsync(argContexts, visitFunctionCallLogger);
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

        ActionListener<Object> evalArgumentListener = new ActionListener<>() {
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

        ActionListener<Object> evalArgumentLogger =
            ActionListenerUtils.withLogging(evalArgumentListener, this.getClass().getName(),
                "Eval-Argument: " + argContexts.get(index));

        evaluateExpressionAsync(argContexts.get(index), evalArgumentLogger);
    }

    // =======================
    // Helper Methods
    // =======================

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
