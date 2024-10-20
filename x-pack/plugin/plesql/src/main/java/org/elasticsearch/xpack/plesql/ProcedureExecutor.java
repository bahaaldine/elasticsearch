/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the
 * Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql;

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
     * Constructs a ProcedureExecutor with the given execution context and exception listener.
     *
     * @param context   The execution context containing variables, functions, etc.
     */
    @SuppressWarnings("this-escape")
    public ProcedureExecutor(ExecutionContext context) {
        this.context = context;
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
     * Visits the entire procedure and executes each statement sequentially.
     *
     * @param ctx The ProcedureContext representing the entire procedure.
     * @return null as procedures do not return values directly.
     */
    @Override
    public Object visitProcedure(PlEsqlProcedureParser.ProcedureContext ctx) {
        // Execute each statement in the procedure
        for (PlEsqlProcedureParser.StatementContext stmtCtx : ctx.statement()) {
            visit(stmtCtx);  // Process each statement
        }
        return null;
    }

    /**
     * Visits each statement and delegates handling to the appropriate handler.
     *
     * @param ctx The StatementContext representing a single statement.
     * @return null as statements do not return values directly.
     */
    @Override
    public Object visitStatement(PlEsqlProcedureParser.StatementContext ctx) {
        if (ctx.declare_statement() != null) {
            declareHandler.handle(ctx.declare_statement());
        } else if (ctx.assignment_statement() != null) {
            assignmentHandler.handle(ctx.assignment_statement());
        } else if (ctx.if_statement() != null) {
            ifHandler.handle(ctx.if_statement());
        } else if (ctx.loop_statement() != null) {
            loopHandler.handle(ctx.loop_statement());
        } else if (ctx.function_definition() != null) {
            functionDefHandler.handle(ctx.function_definition());
        } else if (ctx.try_catch_statement() != null) {
            tryCatchHandler.handle(ctx.try_catch_statement());
        } else if (ctx.throw_statement() != null) {
            throwHandler.handle(ctx.throw_statement());
        } else if (ctx.execute_statement() != null) {
            executeHandler.handle(ctx.execute_statement());
        } else if (ctx.return_statement() != null) {
            visitReturn_statement(ctx.return_statement());
        } else if (ctx.break_statement() != null) {
            // Handle break statement by throwing BreakException
            throw new BreakException("Break encountered");
        } else if (ctx.expression_statement() != null) {
            // Evaluate the expression but ignore the result
            evaluateExpression(ctx.expression_statement().expression());
        }
        return null;
    }

    /**
     * Visits and handles declare statements.
     *
     * @param ctx The Declare_statementContext representing a declare statement.
     * @return null as declare statements do not return values directly.
     */
    @Override
    public Object visitDeclare_statement(PlEsqlProcedureParser.Declare_statementContext ctx) {
        declareHandler.handle(ctx);
        return null;
    }

    /**
     * Visits and handles assignment statements.
     *
     * @param ctx The Assignment_statementContext representing an assignment statement.
     * @return null as assignment statements do not return values directly.
     */
    @Override
    public Object visitAssignment_statement(PlEsqlProcedureParser.Assignment_statementContext ctx) {
        assignmentHandler.handle(ctx);
        return null;
    }

    /**
     * Visits and handles if statements.
     *
     * @param ctx The If_statementContext representing an if statement.
     * @return null as if statements do not return values directly.
     */
    @Override
    public Object visitIf_statement(PlEsqlProcedureParser.If_statementContext ctx) {
        ifHandler.handle(ctx);
        return null;
    }

    /**
     * Visits and handles loop statements.
     *
     * @param ctx The Loop_statementContext representing a loop statement.
     * @return null as loop statements do not return values directly.
     */
    @Override
    public Object visitLoop_statement(PlEsqlProcedureParser.Loop_statementContext ctx) {
        loopHandler.handle(ctx);
        return null;
    }

    /**
     * Visits and handles function definitions.
     *
     * @param ctx The Function_definitionContext representing a function definition.
     * @return null as function definitions do not return values directly.
     */
    @Override
    public Object visitFunction_definition(PlEsqlProcedureParser.Function_definitionContext ctx) {
        functionDefHandler.handle(ctx);
        return null;
    }

    /**
     * Visits and handles try-catch statements.
     *
     * @param ctx The Try_catch_statementContext representing a try-catch statement.
     * @return null as try-catch statements do not return values directly.
     */
    @Override
    public Object visitTry_catch_statement(PlEsqlProcedureParser.Try_catch_statementContext ctx) {
        // Use the tryCatchHandler to handle the try-catch block
        tryCatchHandler.handle(ctx);
        return null;
    }

    /**
     * Visits and handles throw statements.
     *
     * @param ctx The Throw_statementContext representing a throw statement.
     * @return null as throw statements do not return values directly.
     */
    @Override
    public Object visitThrow_statement(PlEsqlProcedureParser.Throw_statementContext ctx) {
        // Use the throwHandler to handle the THROW statement
        throwHandler.handle(ctx);
        return null;
    }

    /**
     * Visits and handles function calls within expressions or as standalone statements.
     *
     * @param ctx The Function_callContext representing a function call.
     * @return The result of the function call.
     */
    @Override
    public Object visitFunction_call(PlEsqlProcedureParser.Function_callContext ctx) {
        String functionName = ctx.ID().getText();
        FunctionDefinition function = context.getFunction(functionName);

        if (function == null) {
            throw new RuntimeException("Function not defined: " + functionName);
        }

        // Evaluate arguments using the current context before changing it
        List<Object> argValues = new ArrayList<>();
        if (ctx.argument_list() != null) {
            List<PlEsqlProcedureParser.ExpressionContext> args = ctx.argument_list().expression();
            int paramCount = function.getParameters().size();
            int argCount = args.size();

            if (argCount != paramCount) {
                throw new RuntimeException("Function '" + functionName + "' expects "
                    + paramCount + " arguments, but got " + argCount + ".");
            }

            for (PlEsqlProcedureParser.ExpressionContext argCtx : args) {
                Object argValue = evaluateExpression(argCtx);
                argValues.add(argValue);
            }
        }

        // Create a new execution context for the function
        ExecutionContext previousContext = context;
        context = new ExecutionContext(previousContext);  // Create a new context with the previous one as parent

        return functionDefHandler.executeFunction(functionName, argValues);
    }

    /**
     * Visits and handles return statements within functions.
     *
     * @param ctx The Return_statementContext representing a return statement.
     * @return A ReturnValue object encapsulating the return value.
     */
    @Override
    public Object visitReturn_statement(PlEsqlProcedureParser.Return_statementContext ctx) {
        Object value = evaluateExpression(ctx.expression());
        throw new ReturnValue(value);
    }

    /**
     * Visits and handles execute statements, which run ES|QL queries.
     *
     * @param ctx The Execute_statementContext representing an execute statement.
     * @return null as execute statements do not return values directly.
     */
    @Override
    public Object visitExecute_statement(PlEsqlProcedureParser.Execute_statementContext ctx) {
        executeHandler.handle(ctx);
        return null;
    }

    /**
     * Visits and handles function call statements.
     *
     * @param ctx The Function_call_statementContext representing a function call statement.
     * @return The result of the function call.
     */
    @Override
    public Object visitFunction_call_statement(PlEsqlProcedureParser.Function_call_statementContext ctx) {
        return visitFunction_call(ctx.function_call());
    }

    /**
     * Visits and handles break statements by throwing a BreakException.
     *
     * @param ctx The Break_statementContext representing a break statement.
     * @return null as break statements do not return values directly.
     */
    @Override
    public Object visitBreak_statement(PlEsqlProcedureParser.Break_statementContext ctx) {
        throw new BreakException("Break statement encountered.");
    }

    // =======================
    // Helper Methods
    // =======================

    /**
     * Evaluates an expression and returns its result.
     *
     * @param ctx The ExpressionContext representing the expression to evaluate.
     * @return The result of the evaluated expression.
     */
    public Object evaluateExpression(PlEsqlProcedureParser.ExpressionContext ctx) {
        if (ctx == null) {
            throw new RuntimeException("Null expression context");
        }
        return evaluateLogicalOrExpression(ctx.logicalOrExpression());
    }

    /**
     * Evaluates a condition and returns its boolean result.
     *
     * @param ctx The condition context to evaluate.
     * @return The boolean result of the condition.
     */
    public boolean evaluateCondition(PlEsqlProcedureParser.ConditionContext ctx) {
        // Implement condition evaluation logic
        // For simplicity, let's handle basic comparisons
        // Expand this method to handle logical operators, nested expressions, etc.

        // Example for simple equality: a = b
        if (ctx.expression() != null) {
            Object result = evaluateExpression(ctx.expression());
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else {
                throw new RuntimeException("Condition does not evaluate to a boolean: " + ctx.getText());
            }
        }

        throw new RuntimeException("Unsupported condition: " + ctx.getText());
    }

    /**
     * Evaluates a logical OR expression.
     *
     * @param ctx The LogicalOrExpressionContext representing the logical OR expression.
     * @return The result of the evaluated logical OR expression.
     */
    private Object evaluateLogicalOrExpression(PlEsqlProcedureParser.LogicalOrExpressionContext ctx) {
        Object result = evaluateLogicalAndExpression(ctx.logicalAndExpression(0));

        for (int i = 1; i < ctx.logicalAndExpression().size(); i++) {
            boolean right = toBoolean(evaluateLogicalAndExpression(ctx.logicalAndExpression(i)));
            result = toBoolean(result) || right;
        }

        return result;
    }

    /**
     * Evaluates a logical AND expression.
     *
     * @param ctx The LogicalAndExpressionContext representing the logical AND expression.
     * @return The result of the evaluated logical AND expression.
     */
    private Object evaluateLogicalAndExpression(PlEsqlProcedureParser.LogicalAndExpressionContext ctx) {
        Object result = evaluateEqualityExpression(ctx.equalityExpression(0));

        for (int i = 1; i < ctx.equalityExpression().size(); i++) {
            boolean right = toBoolean(evaluateEqualityExpression(ctx.equalityExpression(i)));
            result = toBoolean(result) && right;
        }

        return result;
    }

    /**
     * Evaluates an equality expression.
     *
     * @param ctx The EqualityExpressionContext representing the equality expression.
     * @return The result of the evaluated equality expression.
     */
    private Object evaluateEqualityExpression(PlEsqlProcedureParser.EqualityExpressionContext ctx) {
        Object result = evaluateRelationalExpression(ctx.relationalExpression(0));

        for (int i = 1; i < ctx.relationalExpression().size(); i++) {
            String operator = ctx.getChild(2 * i - 1).getText();
            Object right = evaluateRelationalExpression(ctx.relationalExpression(i));

            switch (operator) {
                case "=":
                    result = result.equals(right);
                    break;
                case "!=":
                    result = result.equals(right) == false;
                    break;
                default:
                    throw new RuntimeException("Unknown equality operator: " + operator);
            }
        }

        return result;
    }

    /**
     * Evaluates a relational expression.
     *
     * @param ctx The RelationalExpressionContext representing the relational expression.
     * @return The result of the evaluated relational expression.
     */
    private Object evaluateRelationalExpression(PlEsqlProcedureParser.RelationalExpressionContext ctx) {
        Object left = evaluateAdditiveExpression(ctx.additiveExpression(0));

        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            String operator = ctx.getChild(2 * i - 1).getText();
            Object right = evaluateAdditiveExpression(ctx.additiveExpression(i));

            if ( (left instanceof Number) == false || (right instanceof Number) == false ) {
                throw new RuntimeException("Relational operators can only be applied to numbers.");
            }

            double leftDouble = ((Number) left).doubleValue();
            double rightDouble = ((Number) right).doubleValue();

            switch (operator) {
                case "<":
                    left = leftDouble < rightDouble;
                    break;
                case ">":
                    left = leftDouble > rightDouble;
                    break;
                case "<=":
                    left = leftDouble <= rightDouble;
                    break;
                case ">=":
                    left = leftDouble >= rightDouble;
                    break;
                default:
                    throw new RuntimeException("Unknown relational operator: " + operator);
            }
        }

        return left;
    }

    /**
     * Evaluates an additive expression.
     *
     * @param ctx The AdditiveExpressionContext representing the additive expression.
     * @return The result of the evaluated additive expression.
     */
    private Object evaluateAdditiveExpression(PlEsqlProcedureParser.AdditiveExpressionContext ctx) {
        Object result = evaluateMultiplicativeExpression(ctx.multiplicativeExpression(0));

        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            String operator = ctx.getChild(2 * i - 1).getText();
            Object next = evaluateMultiplicativeExpression(ctx.multiplicativeExpression(i));

            if ( (result instanceof Number) == false || (next instanceof Number) == false ) {
                throw new RuntimeException("Additive operators can only be applied to numbers.");
            }

            switch (operator) {
                case "+":
                    if (result instanceof Double || result instanceof Float ||
                        next instanceof Double || next instanceof Float) {
                        double leftDouble = ((Number) result).doubleValue();
                        double rightDouble = ((Number) next).doubleValue();
                        result = leftDouble + rightDouble;
                    } else {
                        // Both operands are integers; perform integer addition
                        int leftInt = (Integer) result;
                        int rightInt = (Integer) next;
                        result = leftInt + rightInt;
                    }
                    break;
                case "-":
                    if (result instanceof Double || result instanceof Float ||
                        next instanceof Double || next instanceof Float) {
                        double leftDouble = ((Number) result).doubleValue();
                        double rightDouble = ((Number) next).doubleValue();
                        result = leftDouble - rightDouble;
                    } else {
                        // Both operands are integers; perform integer subtraction
                        int leftInt = (Integer) result;
                        int rightInt = (Integer) next;
                        result = leftInt - rightInt;
                    }
                    break;
                default:
                    throw new RuntimeException("Unsupported operator in additive expression: " + operator);
            }
        }

        return result;
    }

    /**
     * Evaluates a multiplicative expression.
     *
     * @param ctx The MultiplicativeExpressionContext representing the multiplicative expression.
     * @return The result of the evaluated multiplicative expression.
     */
    private Object evaluateMultiplicativeExpression(PlEsqlProcedureParser.MultiplicativeExpressionContext ctx) {
        Object result = evaluateUnaryExpr(ctx.unaryExpr(0));

        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            String operator = ctx.getChild(2 * i - 1).getText();
            Object right = evaluateUnaryExpr(ctx.unaryExpr(i));

            if ( (result instanceof Number) == false || (right instanceof Number) == false ) {
                throw new RuntimeException("Multiplicative operators can only be applied to numbers.");
            }

            switch (operator) {
                case "*":
                    // Preserve type: if either operand is floating-point, result is floating-point
                    if (result instanceof Double || result instanceof Float ||
                        right instanceof Double || right instanceof Float) {
                        double leftDouble = ((Number) result).doubleValue();
                        double rightDouble = ((Number) right).doubleValue();
                        result = leftDouble * rightDouble;
                    } else {
                        // Both operands are integers; perform integer multiplication
                        int leftInt = (Integer) result;
                        int rightInt = (Integer) right;
                        result = leftInt * rightInt;
                    }
                    break;
                case "/":
                    // Always perform floating-point division
                    double leftDouble = ((Number) result).doubleValue();
                    double rightDouble = ((Number) right).doubleValue();
                    if (rightDouble == 0.0) {
                        throw new RuntimeException("Division by zero.");
                    }
                    result = leftDouble / rightDouble;
                    break;
                default:
                    throw new RuntimeException("Unknown multiplicative operator: " + operator);
            }
        }

        return result;
    }

    /**
     * Evaluates a unary expression, handling unary minus operations.
     *
     * @param ctx The UnaryExprContext representing the unary expression.
     * @return The result of the evaluated unary expression.
     */
    private Object evaluateUnaryExpr(PlEsqlProcedureParser.UnaryExprContext ctx) {
        if (ctx.getChildCount() == 2 && ctx.getChild(0).getText().equals("-")) {
            // Unary minus detected
            Object operand = evaluateUnaryExpr(ctx.unaryExpr());
            return negate(operand);
        } else {
            // No unary operator, evaluate the primary expression
            return evaluatePrimaryExpression(ctx.primaryExpression());
        }
    }

    /**
     * Evaluates a primary expression, which can be a literal, identifier, function call, or a parenthesized expression.
     *
     * @param ctx The PrimaryExpressionContext representing the primary expression.
     * @return The result of the evaluated primary expression.
     */
    private Object evaluatePrimaryExpression(PlEsqlProcedureParser.PrimaryExpressionContext ctx) {
        if (ctx.LPAREN() != null && ctx.RPAREN() != null) {
            // Parenthesized expression
            return evaluateExpression(ctx.expression());
        } else if (ctx.function_call() != null) {
            // Function call
            return visitFunction_call(ctx.function_call());
        } else if (ctx.INT() != null) {
            // Integer literal
            return Integer.parseInt(ctx.INT().getText());
        } else if (ctx.FLOAT() != null) {
            // Float literal
            return Double.parseDouble(ctx.FLOAT().getText());
        } else if (ctx.STRING() != null) {
            // String literal
            String text = ctx.STRING().getText();
            // Remove the surrounding single quotes and handle escaped characters
            return text.substring(1, text.length() - 1).replace("\\'", "'");
        } else if (ctx.ID() != null) {
            // Identifier (variable)
            String varName = ctx.ID().getText();
            if ( context.hasVariable(varName) == false ) {
                throw new RuntimeException("Variable not declared: " + varName);
            }
            Object varValue = context.getVariable(varName);
            if (varValue == null) {
                throw new RuntimeException("Variable '" + varName + "' is not initialized.");
            }
            return varValue;
        }
        throw new RuntimeException("Unsupported primary expression: " + ctx.getText());
    }

    /**
     * Negates the given operand.
     *
     * @param operand The operand to negate.
     * @return The negated value.
     */
    private Object negate(Object operand) {
        if (operand instanceof Integer) {
            return -((Integer) operand);
        } else if (operand instanceof Double) {
            return -((Double) operand);
        } else if (operand instanceof Float) {
            return -((Float) operand);
        } else {
            throw new RuntimeException("Unsupported operand type for negation: " + operand.getClass().getSimpleName());
        }
    }

    /**
     * Converts an Object to a boolean. Throws an exception if the object is not a boolean.
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
}
