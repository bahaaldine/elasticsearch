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
	 * Enter a parse tree produced by {@link PlEsqlProcedureParser#comparison_operator}.
	 * @param ctx the parse tree
	 */
	void enterComparison_operator(PlEsqlProcedureParser.Comparison_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlEsqlProcedureParser#comparison_operator}.
	 * @param ctx the parse tree
	 */
	void exitComparison_operator(PlEsqlProcedureParser.Comparison_operatorContext ctx);
}