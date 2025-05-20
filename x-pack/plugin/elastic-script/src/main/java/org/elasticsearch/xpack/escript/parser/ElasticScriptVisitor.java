/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ElasticScriptParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ElasticScriptVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(ElasticScriptParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#procedure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure(ElasticScriptParser.ProcedureContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_procedure_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_procedure_statement(ElasticScriptParser.Create_procedure_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#delete_procedure_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_procedure_statement(ElasticScriptParser.Delete_procedure_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(ElasticScriptParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#call_procedure_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCall_procedure_statement(ElasticScriptParser.Call_procedure_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#print_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrint_statement(ElasticScriptParser.Print_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#break_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreak_statement(ElasticScriptParser.Break_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#return_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_statement(ElasticScriptParser.Return_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#expression_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_statement(ElasticScriptParser.Expression_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#execute_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_statement(ElasticScriptParser.Execute_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#variable_assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_assignment(ElasticScriptParser.Variable_assignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#esql_query_content}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEsql_query_content(ElasticScriptParser.Esql_query_contentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#declare_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_statement(ElasticScriptParser.Declare_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#variable_declaration_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declaration_list(ElasticScriptParser.Variable_declaration_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#variable_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declaration(ElasticScriptParser.Variable_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#assignment_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_statement(ElasticScriptParser.Assignment_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#if_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_statement(ElasticScriptParser.If_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#elseif_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseif_block(ElasticScriptParser.Elseif_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondition(ElasticScriptParser.ConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#loop_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_statement(ElasticScriptParser.Loop_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#for_range_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_range_loop(ElasticScriptParser.For_range_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#for_array_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_array_loop(ElasticScriptParser.For_array_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#while_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile_loop(ElasticScriptParser.While_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#range_loop_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_loop_expression(ElasticScriptParser.Range_loop_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#array_loop_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_loop_expression(ElasticScriptParser.Array_loop_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#try_catch_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTry_catch_statement(ElasticScriptParser.Try_catch_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#throw_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrow_statement(ElasticScriptParser.Throw_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#function_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_definition(ElasticScriptParser.Function_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#function_call_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call_statement(ElasticScriptParser.Function_call_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call(ElasticScriptParser.Function_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#parameter_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_list(ElasticScriptParser.Parameter_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(ElasticScriptParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#argument_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument_list(ElasticScriptParser.Argument_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(ElasticScriptParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOrExpression(ElasticScriptParser.LogicalOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalAndExpression(ElasticScriptParser.LogicalAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#equalityExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpression(ElasticScriptParser.EqualityExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpression(ElasticScriptParser.RelationalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(ElasticScriptParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(ElasticScriptParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#unaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpr(ElasticScriptParser.UnaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#arrayLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayLiteral(ElasticScriptParser.ArrayLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#expressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionList(ElasticScriptParser.ExpressionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#documentLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDocumentLiteral(ElasticScriptParser.DocumentLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#documentField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDocumentField(ElasticScriptParser.DocumentFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#pairList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPairList(ElasticScriptParser.PairListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#pair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPair(ElasticScriptParser.PairContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpression(ElasticScriptParser.PrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#bracketExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracketExpression(ElasticScriptParser.BracketExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#simplePrimaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimplePrimaryExpression(ElasticScriptParser.SimplePrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#varRef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarRef(ElasticScriptParser.VarRefContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatype(ElasticScriptParser.DatatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#array_datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_datatype(ElasticScriptParser.Array_datatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#persist_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPersist_clause(ElasticScriptParser.Persist_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#severity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeverity(ElasticScriptParser.SeverityContext ctx);
}