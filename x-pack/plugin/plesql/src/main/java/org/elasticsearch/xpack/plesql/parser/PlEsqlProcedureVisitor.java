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
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#execute_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_statement(PlEsqlProcedureParser.Execute_statementContext ctx);
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
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#loop_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_statement(PlEsqlProcedureParser.Loop_statementContext ctx);
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
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondition(PlEsqlProcedureParser.ConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(PlEsqlProcedureParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatype(PlEsqlProcedureParser.DatatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlEsqlProcedureParser#comparison_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison_operator(PlEsqlProcedureParser.Comparison_operatorContext ctx);
}