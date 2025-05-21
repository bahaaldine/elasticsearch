/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ElasticScriptParser}.
 */
public interface ElasticScriptListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(ElasticScriptParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(ElasticScriptParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#procedure}.
	 * @param ctx the parse tree
	 */
	void enterProcedure(ElasticScriptParser.ProcedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#procedure}.
	 * @param ctx the parse tree
	 */
	void exitProcedure(ElasticScriptParser.ProcedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_procedure_statement(ElasticScriptParser.Create_procedure_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_procedure_statement(ElasticScriptParser.Create_procedure_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#delete_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void enterDelete_procedure_statement(ElasticScriptParser.Delete_procedure_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#delete_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void exitDelete_procedure_statement(ElasticScriptParser.Delete_procedure_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(ElasticScriptParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(ElasticScriptParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#call_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void enterCall_procedure_statement(ElasticScriptParser.Call_procedure_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#call_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void exitCall_procedure_statement(ElasticScriptParser.Call_procedure_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#print_statement}.
	 * @param ctx the parse tree
	 */
	void enterPrint_statement(ElasticScriptParser.Print_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#print_statement}.
	 * @param ctx the parse tree
	 */
	void exitPrint_statement(ElasticScriptParser.Print_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#break_statement}.
	 * @param ctx the parse tree
	 */
	void enterBreak_statement(ElasticScriptParser.Break_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#break_statement}.
	 * @param ctx the parse tree
	 */
	void exitBreak_statement(ElasticScriptParser.Break_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void enterReturn_statement(ElasticScriptParser.Return_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void exitReturn_statement(ElasticScriptParser.Return_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#expression_statement}.
	 * @param ctx the parse tree
	 */
	void enterExpression_statement(ElasticScriptParser.Expression_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#expression_statement}.
	 * @param ctx the parse tree
	 */
	void exitExpression_statement(ElasticScriptParser.Expression_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#execute_statement}.
	 * @param ctx the parse tree
	 */
	void enterExecute_statement(ElasticScriptParser.Execute_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#execute_statement}.
	 * @param ctx the parse tree
	 */
	void exitExecute_statement(ElasticScriptParser.Execute_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#variable_assignment}.
	 * @param ctx the parse tree
	 */
	void enterVariable_assignment(ElasticScriptParser.Variable_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#variable_assignment}.
	 * @param ctx the parse tree
	 */
	void exitVariable_assignment(ElasticScriptParser.Variable_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#esql_query_content}.
	 * @param ctx the parse tree
	 */
	void enterEsql_query_content(ElasticScriptParser.Esql_query_contentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#esql_query_content}.
	 * @param ctx the parse tree
	 */
	void exitEsql_query_content(ElasticScriptParser.Esql_query_contentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#declare_statement}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_statement(ElasticScriptParser.Declare_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#declare_statement}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_statement(ElasticScriptParser.Declare_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#variable_declaration_list}.
	 * @param ctx the parse tree
	 */
	void enterVariable_declaration_list(ElasticScriptParser.Variable_declaration_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#variable_declaration_list}.
	 * @param ctx the parse tree
	 */
	void exitVariable_declaration_list(ElasticScriptParser.Variable_declaration_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void enterVariable_declaration(ElasticScriptParser.Variable_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void exitVariable_declaration(ElasticScriptParser.Variable_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_statement(ElasticScriptParser.Assignment_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_statement(ElasticScriptParser.Assignment_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void enterIf_statement(ElasticScriptParser.If_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void exitIf_statement(ElasticScriptParser.If_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#elseif_block}.
	 * @param ctx the parse tree
	 */
	void enterElseif_block(ElasticScriptParser.Elseif_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#elseif_block}.
	 * @param ctx the parse tree
	 */
	void exitElseif_block(ElasticScriptParser.Elseif_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(ElasticScriptParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(ElasticScriptParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void enterLoop_statement(ElasticScriptParser.Loop_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void exitLoop_statement(ElasticScriptParser.Loop_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#for_range_loop}.
	 * @param ctx the parse tree
	 */
	void enterFor_range_loop(ElasticScriptParser.For_range_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#for_range_loop}.
	 * @param ctx the parse tree
	 */
	void exitFor_range_loop(ElasticScriptParser.For_range_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#for_array_loop}.
	 * @param ctx the parse tree
	 */
	void enterFor_array_loop(ElasticScriptParser.For_array_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#for_array_loop}.
	 * @param ctx the parse tree
	 */
	void exitFor_array_loop(ElasticScriptParser.For_array_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#while_loop}.
	 * @param ctx the parse tree
	 */
	void enterWhile_loop(ElasticScriptParser.While_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#while_loop}.
	 * @param ctx the parse tree
	 */
	void exitWhile_loop(ElasticScriptParser.While_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#range_loop_expression}.
	 * @param ctx the parse tree
	 */
	void enterRange_loop_expression(ElasticScriptParser.Range_loop_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#range_loop_expression}.
	 * @param ctx the parse tree
	 */
	void exitRange_loop_expression(ElasticScriptParser.Range_loop_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#array_loop_expression}.
	 * @param ctx the parse tree
	 */
	void enterArray_loop_expression(ElasticScriptParser.Array_loop_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#array_loop_expression}.
	 * @param ctx the parse tree
	 */
	void exitArray_loop_expression(ElasticScriptParser.Array_loop_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#try_catch_statement}.
	 * @param ctx the parse tree
	 */
	void enterTry_catch_statement(ElasticScriptParser.Try_catch_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#try_catch_statement}.
	 * @param ctx the parse tree
	 */
	void exitTry_catch_statement(ElasticScriptParser.Try_catch_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#throw_statement}.
	 * @param ctx the parse tree
	 */
	void enterThrow_statement(ElasticScriptParser.Throw_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#throw_statement}.
	 * @param ctx the parse tree
	 */
	void exitThrow_statement(ElasticScriptParser.Throw_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#function_definition}.
	 * @param ctx the parse tree
	 */
	void enterFunction_definition(ElasticScriptParser.Function_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#function_definition}.
	 * @param ctx the parse tree
	 */
	void exitFunction_definition(ElasticScriptParser.Function_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#function_call_statement}.
	 * @param ctx the parse tree
	 */
	void enterFunction_call_statement(ElasticScriptParser.Function_call_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#function_call_statement}.
	 * @param ctx the parse tree
	 */
	void exitFunction_call_statement(ElasticScriptParser.Function_call_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterFunction_call(ElasticScriptParser.Function_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitFunction_call(ElasticScriptParser.Function_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#parameter_list}.
	 * @param ctx the parse tree
	 */
	void enterParameter_list(ElasticScriptParser.Parameter_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#parameter_list}.
	 * @param ctx the parse tree
	 */
	void exitParameter_list(ElasticScriptParser.Parameter_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(ElasticScriptParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(ElasticScriptParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#argument_list}.
	 * @param ctx the parse tree
	 */
	void enterArgument_list(ElasticScriptParser.Argument_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#argument_list}.
	 * @param ctx the parse tree
	 */
	void exitArgument_list(ElasticScriptParser.Argument_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ElasticScriptParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ElasticScriptParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOrExpression(ElasticScriptParser.LogicalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOrExpression(ElasticScriptParser.LogicalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalAndExpression(ElasticScriptParser.LogicalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalAndExpression(ElasticScriptParser.LogicalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpression(ElasticScriptParser.EqualityExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpression(ElasticScriptParser.EqualityExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpression(ElasticScriptParser.RelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpression(ElasticScriptParser.RelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(ElasticScriptParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(ElasticScriptParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(ElasticScriptParser.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(ElasticScriptParser.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpr(ElasticScriptParser.UnaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpr(ElasticScriptParser.UnaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void enterArrayLiteral(ElasticScriptParser.ArrayLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void exitArrayLiteral(ElasticScriptParser.ArrayLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(ElasticScriptParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(ElasticScriptParser.ExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#documentLiteral}.
	 * @param ctx the parse tree
	 */
	void enterDocumentLiteral(ElasticScriptParser.DocumentLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#documentLiteral}.
	 * @param ctx the parse tree
	 */
	void exitDocumentLiteral(ElasticScriptParser.DocumentLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#documentField}.
	 * @param ctx the parse tree
	 */
	void enterDocumentField(ElasticScriptParser.DocumentFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#documentField}.
	 * @param ctx the parse tree
	 */
	void exitDocumentField(ElasticScriptParser.DocumentFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#pairList}.
	 * @param ctx the parse tree
	 */
	void enterPairList(ElasticScriptParser.PairListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#pairList}.
	 * @param ctx the parse tree
	 */
	void exitPairList(ElasticScriptParser.PairListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#pair}.
	 * @param ctx the parse tree
	 */
	void enterPair(ElasticScriptParser.PairContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#pair}.
	 * @param ctx the parse tree
	 */
	void exitPair(ElasticScriptParser.PairContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpression(ElasticScriptParser.PrimaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpression(ElasticScriptParser.PrimaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#bracketExpression}.
	 * @param ctx the parse tree
	 */
	void enterBracketExpression(ElasticScriptParser.BracketExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#bracketExpression}.
	 * @param ctx the parse tree
	 */
	void exitBracketExpression(ElasticScriptParser.BracketExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#simplePrimaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimplePrimaryExpression(ElasticScriptParser.SimplePrimaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#simplePrimaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimplePrimaryExpression(ElasticScriptParser.SimplePrimaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#varRef}.
	 * @param ctx the parse tree
	 */
	void enterVarRef(ElasticScriptParser.VarRefContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#varRef}.
	 * @param ctx the parse tree
	 */
	void exitVarRef(ElasticScriptParser.VarRefContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#datatype}.
	 * @param ctx the parse tree
	 */
	void enterDatatype(ElasticScriptParser.DatatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#datatype}.
	 * @param ctx the parse tree
	 */
	void exitDatatype(ElasticScriptParser.DatatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#array_datatype}.
	 * @param ctx the parse tree
	 */
	void enterArray_datatype(ElasticScriptParser.Array_datatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#array_datatype}.
	 * @param ctx the parse tree
	 */
	void exitArray_datatype(ElasticScriptParser.Array_datatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#persist_clause}.
	 * @param ctx the parse tree
	 */
	void enterPersist_clause(ElasticScriptParser.Persist_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#persist_clause}.
	 * @param ctx the parse tree
	 */
	void exitPersist_clause(ElasticScriptParser.Persist_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#severity}.
	 * @param ctx the parse tree
	 */
	void enterSeverity(ElasticScriptParser.SeverityContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#severity}.
	 * @param ctx the parse tree
	 */
	void exitSeverity(ElasticScriptParser.SeverityContext ctx);
}