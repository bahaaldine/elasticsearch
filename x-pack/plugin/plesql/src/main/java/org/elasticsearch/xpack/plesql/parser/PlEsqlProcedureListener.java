/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PlEsqlProcedureParser}.
 */
public interface PlEsqlProcedureListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#procedure}.
	 * @param ctx the parse tree
	 */
	void enterProcedure(PlEsqlProcedureParser.ProcedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#procedure}.
	 * @param ctx the parse tree
	 */
	void exitProcedure(PlEsqlProcedureParser.ProcedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(PlEsqlProcedureParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(PlEsqlProcedureParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#break_statement}.
	 * @param ctx the parse tree
	 */
	void enterBreak_statement(PlEsqlProcedureParser.Break_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#break_statement}.
	 * @param ctx the parse tree
	 */
	void exitBreak_statement(PlEsqlProcedureParser.Break_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void enterReturn_statement(PlEsqlProcedureParser.Return_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void exitReturn_statement(PlEsqlProcedureParser.Return_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#expression_statement}.
	 * @param ctx the parse tree
	 */
	void enterExpression_statement(PlEsqlProcedureParser.Expression_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#expression_statement}.
	 * @param ctx the parse tree
	 */
	void exitExpression_statement(PlEsqlProcedureParser.Expression_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#execute_statement}.
	 * @param ctx the parse tree
	 */
	void enterExecute_statement(PlEsqlProcedureParser.Execute_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#execute_statement}.
	 * @param ctx the parse tree
	 */
	void exitExecute_statement(PlEsqlProcedureParser.Execute_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#variable_assignment}.
	 * @param ctx the parse tree
	 */
	void enterVariable_assignment(PlEsqlProcedureParser.Variable_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#variable_assignment}.
	 * @param ctx the parse tree
	 */
	void exitVariable_assignment(PlEsqlProcedureParser.Variable_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#esql_query_content}.
	 * @param ctx the parse tree
	 */
	void enterEsql_query_content(PlEsqlProcedureParser.Esql_query_contentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#esql_query_content}.
	 * @param ctx the parse tree
	 */
	void exitEsql_query_content(PlEsqlProcedureParser.Esql_query_contentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#declare_statement}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_statement(PlEsqlProcedureParser.Declare_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#declare_statement}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_statement(PlEsqlProcedureParser.Declare_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#variable_declaration_list}.
	 * @param ctx the parse tree
	 */
	void enterVariable_declaration_list(PlEsqlProcedureParser.Variable_declaration_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#variable_declaration_list}.
	 * @param ctx the parse tree
	 */
	void exitVariable_declaration_list(PlEsqlProcedureParser.Variable_declaration_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void enterVariable_declaration(PlEsqlProcedureParser.Variable_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void exitVariable_declaration(PlEsqlProcedureParser.Variable_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_statement(PlEsqlProcedureParser.Assignment_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_statement(PlEsqlProcedureParser.Assignment_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void enterIf_statement(PlEsqlProcedureParser.If_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void exitIf_statement(PlEsqlProcedureParser.If_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#elseif_block}.
	 * @param ctx the parse tree
	 */
	void enterElseif_block(PlEsqlProcedureParser.Elseif_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#elseif_block}.
	 * @param ctx the parse tree
	 */
	void exitElseif_block(PlEsqlProcedureParser.Elseif_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(PlEsqlProcedureParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(PlEsqlProcedureParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void enterLoop_statement(PlEsqlProcedureParser.Loop_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void exitLoop_statement(PlEsqlProcedureParser.Loop_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#for_range_loop}.
	 * @param ctx the parse tree
	 */
	void enterFor_range_loop(PlEsqlProcedureParser.For_range_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#for_range_loop}.
	 * @param ctx the parse tree
	 */
	void exitFor_range_loop(PlEsqlProcedureParser.For_range_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#for_array_loop}.
	 * @param ctx the parse tree
	 */
	void enterFor_array_loop(PlEsqlProcedureParser.For_array_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#for_array_loop}.
	 * @param ctx the parse tree
	 */
	void exitFor_array_loop(PlEsqlProcedureParser.For_array_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#while_loop}.
	 * @param ctx the parse tree
	 */
	void enterWhile_loop(PlEsqlProcedureParser.While_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#while_loop}.
	 * @param ctx the parse tree
	 */
	void exitWhile_loop(PlEsqlProcedureParser.While_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#range_loop_expression}.
	 * @param ctx the parse tree
	 */
	void enterRange_loop_expression(PlEsqlProcedureParser.Range_loop_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#range_loop_expression}.
	 * @param ctx the parse tree
	 */
	void exitRange_loop_expression(PlEsqlProcedureParser.Range_loop_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#array_loop_expression}.
	 * @param ctx the parse tree
	 */
	void enterArray_loop_expression(PlEsqlProcedureParser.Array_loop_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#array_loop_expression}.
	 * @param ctx the parse tree
	 */
	void exitArray_loop_expression(PlEsqlProcedureParser.Array_loop_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#try_catch_statement}.
	 * @param ctx the parse tree
	 */
	void enterTry_catch_statement(PlEsqlProcedureParser.Try_catch_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#try_catch_statement}.
	 * @param ctx the parse tree
	 */
	void exitTry_catch_statement(PlEsqlProcedureParser.Try_catch_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#throw_statement}.
	 * @param ctx the parse tree
	 */
	void enterThrow_statement(PlEsqlProcedureParser.Throw_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#throw_statement}.
	 * @param ctx the parse tree
	 */
	void exitThrow_statement(PlEsqlProcedureParser.Throw_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#function_definition}.
	 * @param ctx the parse tree
	 */
	void enterFunction_definition(PlEsqlProcedureParser.Function_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#function_definition}.
	 * @param ctx the parse tree
	 */
	void exitFunction_definition(PlEsqlProcedureParser.Function_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#function_call_statement}.
	 * @param ctx the parse tree
	 */
	void enterFunction_call_statement(PlEsqlProcedureParser.Function_call_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#function_call_statement}.
	 * @param ctx the parse tree
	 */
	void exitFunction_call_statement(PlEsqlProcedureParser.Function_call_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterFunction_call(PlEsqlProcedureParser.Function_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitFunction_call(PlEsqlProcedureParser.Function_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#parameter_list}.
	 * @param ctx the parse tree
	 */
	void enterParameter_list(PlEsqlProcedureParser.Parameter_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#parameter_list}.
	 * @param ctx the parse tree
	 */
	void exitParameter_list(PlEsqlProcedureParser.Parameter_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(PlEsqlProcedureParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(PlEsqlProcedureParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#argument_list}.
	 * @param ctx the parse tree
	 */
	void enterArgument_list(PlEsqlProcedureParser.Argument_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#argument_list}.
	 * @param ctx the parse tree
	 */
	void exitArgument_list(PlEsqlProcedureParser.Argument_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(PlEsqlProcedureParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(PlEsqlProcedureParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOrExpression(PlEsqlProcedureParser.LogicalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOrExpression(PlEsqlProcedureParser.LogicalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalAndExpression(PlEsqlProcedureParser.LogicalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalAndExpression(PlEsqlProcedureParser.LogicalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpression(PlEsqlProcedureParser.EqualityExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpression(PlEsqlProcedureParser.EqualityExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpression(PlEsqlProcedureParser.RelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpression(PlEsqlProcedureParser.RelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(PlEsqlProcedureParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(PlEsqlProcedureParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(PlEsqlProcedureParser.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(PlEsqlProcedureParser.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpr(PlEsqlProcedureParser.UnaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpr(PlEsqlProcedureParser.UnaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void enterArrayLiteral(PlEsqlProcedureParser.ArrayLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void exitArrayLiteral(PlEsqlProcedureParser.ArrayLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(PlEsqlProcedureParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(PlEsqlProcedureParser.ExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#documentLiteral}.
	 * @param ctx the parse tree
	 */
	void enterDocumentLiteral(PlEsqlProcedureParser.DocumentLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#documentLiteral}.
	 * @param ctx the parse tree
	 */
	void exitDocumentLiteral(PlEsqlProcedureParser.DocumentLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#pairList}.
	 * @param ctx the parse tree
	 */
	void enterPairList(PlEsqlProcedureParser.PairListContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#pairList}.
	 * @param ctx the parse tree
	 */
	void exitPairList(PlEsqlProcedureParser.PairListContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#pair}.
	 * @param ctx the parse tree
	 */
	void enterPair(PlEsqlProcedureParser.PairContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#pair}.
	 * @param ctx the parse tree
	 */
	void exitPair(PlEsqlProcedureParser.PairContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpression(PlEsqlProcedureParser.PrimaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpression(PlEsqlProcedureParser.PrimaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#datatype}.
	 * @param ctx the parse tree
	 */
	void enterDatatype(PlEsqlProcedureParser.DatatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#datatype}.
	 * @param ctx the parse tree
	 */
	void exitDatatype(PlEsqlProcedureParser.DatatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#array_datatype}.
	 * @param ctx the parse tree
	 */
	void enterArray_datatype(PlEsqlProcedureParser.Array_datatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#array_datatype}.
	 * @param ctx the parse tree
	 */
	void exitArray_datatype(PlEsqlProcedureParser.Array_datatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#persist_clause}.
	 * @param ctx the parse tree
	 */
	void enterPersist_clause(PlEsqlProcedureParser.Persist_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#persist_clause}.
	 * @param ctx the parse tree
	 */
	void exitPersist_clause(PlEsqlProcedureParser.Persist_clauseContext ctx);
}