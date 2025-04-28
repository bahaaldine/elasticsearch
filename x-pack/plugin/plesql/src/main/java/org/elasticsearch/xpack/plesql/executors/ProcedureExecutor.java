/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.executors;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.evaluators.ExpressionEvaluator;
import org.elasticsearch.xpack.plesql.exceptions.BreakException;
import org.elasticsearch.xpack.plesql.handlers.AssignmentStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.DeclareStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.ExecuteStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.FunctionDefinitionHandler;
import org.elasticsearch.xpack.plesql.handlers.IfStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.LoopStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.PrintStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.ThrowStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.TryCatchStatementHandler;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureBaseVisitor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.FunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

import java.util.ArrayList;
import java.util.List;
/**
 * The ProcedureExecutor class is responsible for executing parsed procedural SQL statements.
 * It extends the PlEsqlProcedureBaseVisitor to traverse the parse tree and execute corresponding actions.
 *
 * Note that expression evaluation has been refactored into a dedicated ExpressionEvaluator.
 */
public class ProcedureExecutor extends PlEsqlProcedureBaseVisitor<Object> {

    private static final Logger LOGGER = LogManager.getLogger(ProcedureExecutor.class);

    private ExecutionContext context;
    private final ThreadPool threadPool;

    // Statement handlers
    private final AssignmentStatementHandler assignmentHandler;
    private final DeclareStatementHandler declareHandler;
    private final IfStatementHandler ifHandler;
    private final LoopStatementHandler loopHandler;
    private final FunctionDefinitionHandler functionDefHandler;
    private final TryCatchStatementHandler tryCatchHandler;
    private final ThrowStatementHandler throwHandler;
    private final ExecuteStatementHandler executeHandler;
    private final PrintStatementHandler printStatementHandler;

    private final CommonTokenStream tokenStream;

    // Expression evaluation is delegated to ExpressionEvaluator.
    private final ExpressionEvaluator expressionEvaluator;

    /**
     * Constructs a ProcedureExecutor with the given execution context and thread pool.
     *
     * @param context    The execution context containing variables, functions, etc.
     * @param threadPool The thread pool for executing asynchronous tasks.
     * @param client     The client for executing queries.
     * @param tokenStream The token stream from the parser.
     */
    @SuppressWarnings("this-escape")
    public ProcedureExecutor(ExecutionContext context, ThreadPool threadPool, Client client, CommonTokenStream tokenStream) {
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
        this.printStatementHandler = new PrintStatementHandler(this);
        this.tokenStream = tokenStream;
        // Initialize ExpressionEvaluator with this executor instance.
        this.expressionEvaluator = new ExpressionEvaluator(this);
    }

    /**
     * Retrieves the current thread pool.
     *
     * @return The current ThreadPool.
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
     * Retrieves the raw text from the context.
     *
     * @param ctx The ParserRuleContext.
     * @return The raw text.
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
        // Start asynchronous execution of the procedure.
        executeProcedureAsync(ctx, listener);
    }

    /**
     * Initiates asynchronous execution of the procedure.
     * Accepts optional arguments to bind to procedure parameters.
     */
    public void executeProcedureAsync(PlEsqlProcedureParser.ProcedureContext ctx, ActionListener<Object> listener) {
        executeStatementsAsync(ctx.statement(), 0, listener);
    }

    /**
     * Executes a list of statements asynchronously.
     *
     * @param statements The list of statements.
     * @param index      The current statement index.
     * @param listener   The ActionListener to notify upon completion.
     */
    public void executeStatementsAsync(List<PlEsqlProcedureParser.StatementContext> statements,
                                       int index, ActionListener<Object> listener) {
        if (index >= statements.size()) {
            listener.onResponse(null); // Execution completed.
            return;
        }

        PlEsqlProcedureParser.StatementContext statement = statements.get(index);

        ActionListener<Object> statementListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object o) {
                if (o instanceof ReturnValue) {
                    listener.onResponse(o);
                } else {
                    executeStatementsAsync(statements, index + 1, listener);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> statementLogger =
            ActionListenerUtils.withLogging(statementListener, this.getClass().getName(),
                "Execute-Statement-Async");

        visitStatementAsync(statement, statementLogger);
    }

    /**
     * Visits a statement asynchronously and delegates handling to the appropriate handler.
     *
     * @param ctx      The StatementContext.
     * @param listener The ActionListener for asynchronous callbacks.
     */
    public void visitStatementAsync(PlEsqlProcedureParser.StatementContext ctx, ActionListener<Object> listener) {

        LOGGER.info("Context statement {}", ctx.getText() );

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
        } else if (ctx.print_statement() != null) {
            printStatementHandler.execute(ctx.print_statement(), listener);
        } else if (ctx.return_statement() != null) {
            visitReturn_statementAsync(ctx.return_statement(), listener);
        } else if (ctx.break_statement() != null) {
            // Handle break statement.
            listener.onFailure(new BreakException("Break encountered"));
        } else if (ctx.expression_statement() != null) {
            // Evaluate the expression asynchronously and ignore the result.
            ActionListener<Object> exprListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object value) {
                    listener.onResponse(value);
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> exprLogger = ActionListenerUtils.withLogging(exprListener,
                this.getClass().getName(),
                "Expression-Eval: " + ctx.expression_statement().expression());

            evaluateExpressionAsync(ctx.expression_statement().expression(), exprLogger);
        } else if (ctx.function_call_statement() != null) {
            // Handle function call statement.
            ActionListener<Object> funcCallListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    listener.onResponse(null); // Function call completed.
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> funcCallLogger = ActionListenerUtils.withLogging(funcCallListener, this.getClass().getName(),
                "Function-Call: " + ctx.function_call_statement().function_call());

            visitFunctionCallAsync(ctx.function_call_statement().function_call(), funcCallLogger);
        } else {
            listener.onResponse(null);
        }
    }

    /**
     * Handles return statements asynchronously.
     *
     * @param ctx      The Return_statementContext.
     * @param listener The ActionListener to notify with the return value.
     */
    private void visitReturn_statementAsync(PlEsqlProcedureParser.Return_statementContext ctx, ActionListener<Object> listener) {
        ActionListener<Object> returnListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object value) {
                listener.onResponse(new ReturnValue(value)); // Signal return value.
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> returnLogger = ActionListenerUtils.withLogging(returnListener,
            this.getClass().getName(),
            "Visit-Return-Statement: " + ctx.expression().getText());

        evaluateExpressionAsync(ctx.expression(), returnLogger);
    }

    /**
     * Delegates expression evaluation to the ExpressionEvaluator.
     *
     * @param ctx      The ExpressionContext to evaluate.
     * @param listener The ActionListener to receive the evaluated result.
     */
    public void evaluateExpressionAsync(PlEsqlProcedureParser.ExpressionContext ctx, ActionListener<Object> listener) {
        expressionEvaluator.evaluateExpressionAsync(ctx, listener);
    }

    /**
     * Evaluates a condition asynchronously.
     *
     * @param ctx      The ConditionContext.
     * @param listener The ActionListener to receive the boolean result.
     */
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

            ActionListener<Object> condLogger = ActionListenerUtils.withLogging(condListener, this.getClass().getName(),
                "Evaluate-Condition: " + ctx.expression());

            evaluateExpressionAsync(ctx.expression(), condLogger);
        } else {
            listener.onFailure(new RuntimeException("Unsupported condition: " + ctx.getText()));
        }
    }

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
            listener.onFailure(new RuntimeException("Unsupported expression"));
            return;
        }

        List<PlEsqlProcedureParser.ExpressionContext> argContexts =
            ctx.argument_list() != null ? ctx.argument_list().expression() : new ArrayList<>();

        ActionListener<List<Object>> funcCallListener = new ActionListener<List<Object>>() {
            @Override
            public void onResponse(List<Object> argValues) {
                functionDefHandler.executeFunctionAsync(functionName, argValues, new ActionListener<Object>() {
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

        ActionListener<List<Object>> funcCallLogger = ActionListenerUtils.withLogging(funcCallListener, this.getClass().getName(),
            "Visit-Function-Call: " + argContexts);

        evaluateArgumentsAsync(argContexts, funcCallLogger);
    }

    /**
     * Evaluates a list of arguments asynchronously.
     *
     * @param argContexts The list of ExpressionContexts representing arguments.
     * @param listener    The ActionListener to receive the list of evaluated arguments.
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
        ActionListener<Object> argLogger = ActionListenerUtils.withLogging(argListener, this.getClass().getName(),
            "Eval-Argument: " + argContexts.get(index));
        evaluateExpressionAsync(argContexts.get(index), argLogger);
    }

    /**
     * Helper method that converts an Object to a boolean.
     *
     * @param obj The object to convert.
     * @return The boolean value.
     */
    private boolean toBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        throw new RuntimeException("Expected a boolean value, but got: " + obj);
    }

    /**
     * Executes a runnable asynchronously using the thread pool.
     *
     * @param runnable The runnable to execute.
     */
    private void executeAsync(Runnable runnable) {
        threadPool.generic().execute(runnable);
    }
}
