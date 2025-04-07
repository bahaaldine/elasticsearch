/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link PlEsqlProcedureParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface PlEsqlProcedureVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#procedure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure(PlEsqlProcedureParser.ProcedureContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(PlEsqlProcedureParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#print_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrint_statement(PlEsqlProcedureParser.Print_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#break_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreak_statement(PlEsqlProcedureParser.Break_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#return_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_statement(PlEsqlProcedureParser.Return_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#expression_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_statement(PlEsqlProcedureParser.Expression_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#execute_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_statement(PlEsqlProcedureParser.Execute_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#variable_assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_assignment(PlEsqlProcedureParser.Variable_assignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#esql_query_content}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEsql_query_content(PlEsqlProcedureParser.Esql_query_contentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#declare_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_statement(PlEsqlProcedureParser.Declare_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#variable_declaration_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declaration_list(PlEsqlProcedureParser.Variable_declaration_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#variable_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declaration(PlEsqlProcedureParser.Variable_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#assignment_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_statement(PlEsqlProcedureParser.Assignment_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#if_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_statement(PlEsqlProcedureParser.If_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#elseif_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseif_block(PlEsqlProcedureParser.Elseif_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondition(PlEsqlProcedureParser.ConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#loop_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_statement(PlEsqlProcedureParser.Loop_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#for_range_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_range_loop(PlEsqlProcedureParser.For_range_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#for_array_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_array_loop(PlEsqlProcedureParser.For_array_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#while_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile_loop(PlEsqlProcedureParser.While_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#range_loop_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_loop_expression(PlEsqlProcedureParser.Range_loop_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#array_loop_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_loop_expression(PlEsqlProcedureParser.Array_loop_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#try_catch_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTry_catch_statement(PlEsqlProcedureParser.Try_catch_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#throw_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrow_statement(PlEsqlProcedureParser.Throw_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#function_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_definition(PlEsqlProcedureParser.Function_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#function_call_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call_statement(PlEsqlProcedureParser.Function_call_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call(PlEsqlProcedureParser.Function_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#parameter_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_list(PlEsqlProcedureParser.Parameter_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(PlEsqlProcedureParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#argument_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument_list(PlEsqlProcedureParser.Argument_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(PlEsqlProcedureParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOrExpression(PlEsqlProcedureParser.LogicalOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalAndExpression(PlEsqlProcedureParser.LogicalAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#equalityExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpression(PlEsqlProcedureParser.EqualityExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpression(PlEsqlProcedureParser.RelationalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(PlEsqlProcedureParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(PlEsqlProcedureParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#unaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpr(PlEsqlProcedureParser.UnaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#arrayLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayLiteral(PlEsqlProcedureParser.ArrayLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#expressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionList(PlEsqlProcedureParser.ExpressionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#documentLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDocumentLiteral(PlEsqlProcedureParser.DocumentLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#pairList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPairList(PlEsqlProcedureParser.PairListContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#pair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPair(PlEsqlProcedureParser.PairContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpression(PlEsqlProcedureParser.PrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#bracketExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracketExpression(PlEsqlProcedureParser.BracketExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#simplePrimaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimplePrimaryExpression(PlEsqlProcedureParser.SimplePrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatype(PlEsqlProcedureParser.DatatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#array_datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_datatype(PlEsqlProcedureParser.Array_datatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#persist_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPersist_clause(PlEsqlProcedureParser.Persist_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#severity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeverity(PlEsqlProcedureParser.SeverityContext ctx);
}