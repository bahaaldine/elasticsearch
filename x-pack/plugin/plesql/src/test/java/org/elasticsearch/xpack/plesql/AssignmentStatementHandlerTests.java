/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.plesql.handlers.DeclareStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.handlers.AssignmentStatementHandler;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class AssignmentStatementHandlerTests {

    private ExecutionContext context;
    private AssignmentStatementHandler assignmentHandler;
    private DeclareStatementHandler declareHandler;

    @Before
    public void setup() {
        context = new ExecutionContext(); // Assuming you have a real implementation of ExecutionContext
        assignmentHandler = new AssignmentStatementHandler(context);
        declareHandler = new DeclareStatementHandler(context);
    }

    // Helper method to parse a query and return the necessary context
    private PlEsqlProcedureParser.Assignment_statementContext parseAssignment(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.assignment_statement();
    }

    // Helper method to parse a query for a declaration
    private PlEsqlProcedureParser.Declare_statementContext parseDeclaration(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.declare_statement();
    }



    // Test 1: Declare a variable and assign a simple integer value
    @Test
    public void testDeclareAndAssignInteger() {
        String declareQuery = "DECLARE myVar INT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        declareHandler.handle(declareContext);

        String assignQuery = "SET myVar = 42;";
        PlEsqlProcedureParser.Assignment_statementContext assignContext = parseAssignment(assignQuery);
        assignmentHandler.handle(assignContext);

        assertEquals(42, context.getVariable("myVar"));
    }

    // Test 2: Declare a variable and assign a simple float value
    @Test
    public void testDeclareAndAssignFloat() {
        String declareQuery = "DECLARE myVar FLOAT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        declareHandler.handle(declareContext);

        String assignQuery = "SET myVar = 42.5;";
        PlEsqlProcedureParser.Assignment_statementContext assignContext = parseAssignment(assignQuery);
        assignmentHandler.handle(assignContext);

        assertEquals(42.5, context.getVariable("myVar"));
    }

    // Test 3: Declare a variable and assign a simple string value
    @Test
    public void testDeclareAndAssignString() {
        String declareQuery = "DECLARE myVar STRING;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        declareHandler.handle(declareContext);

        String assignQuery = "SET myVar = 'hello';";
        PlEsqlProcedureParser.Assignment_statementContext assignContext = parseAssignment(assignQuery);
        assignmentHandler.handle(assignContext);

        assertEquals("hello", context.getVariable("myVar"));
    }

    // Test 4: Declare a variable and assign the result of an arithmetic operation
    @Test
    public void testDeclareAndAssignArithmeticOperation() {
        String declareQuery = "DECLARE myVar FLOAT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        declareHandler.handle(declareContext);

        String assignQuery = "SET myVar = 5 + 3;";
        PlEsqlProcedureParser.Assignment_statementContext assignContext = parseAssignment(assignQuery);
        assignmentHandler.handle(assignContext);

        assertEquals(8.0, context.getVariable("myVar"));
    }

    // Test 5: Declare a variable and assign the result of a multiplication operation
    @Test
    public void testDeclareAndAssignMultiplication() {
        String declareQuery = "DECLARE myVar FLOAT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        declareHandler.handle(declareContext);

        String assignQuery = "SET myVar = 6 * 7;";
        PlEsqlProcedureParser.Assignment_statementContext assignContext = parseAssignment(assignQuery);
        assignmentHandler.handle(assignContext);

        assertEquals(42.0, context.getVariable("myVar"));
    }

    // Test 6: Variable reference assignment (assigning one variable to another)
    @Test
    public void testVariableReferenceAssignment() {
        // Declare two variables
        String declareQuery1 = "DECLARE var1 INT;";
        String declareQuery2 = "DECLARE var2 INT;";
        declareHandler.handle(parseDeclaration(declareQuery1));
        declareHandler.handle(parseDeclaration(declareQuery2));

        // Assign a value to var1
        String assignQuery1 = "SET var1 = 10;";
        assignmentHandler.handle(parseAssignment(assignQuery1));

        // Assign the value of var1 to var2
        String assignQuery2 = "SET var2 = var1;";
        assignmentHandler.handle(parseAssignment(assignQuery2));

        // Verify that var2 was assigned the value of var1
        assertEquals(10, context.getVariable("var2"));
    }

    // Test 7: Unsupported expression throws an exception
    @Test(expected = RuntimeException.class)
    public void testUnsupportedExpressionThrowsError() {
        String declareQuery = "DECLARE myVar STRING;";
        declareHandler.handle(parseDeclaration(declareQuery));

        String assignQuery = "SET myVar = unsupported_expression;";
        assignmentHandler.handle(parseAssignment(assignQuery));
    }

    // Test 8: Type mismatch error (assigning a float to an INT variable)
    @Test(expected = RuntimeException.class)
    public void testTypeMismatchError() {
        String declareQuery = "DECLARE myVar INT;";
        declareHandler.handle(parseDeclaration(declareQuery));

        String assignQuery = "SET myVar = 42.5;";
        assignmentHandler.handle(parseAssignment(assignQuery));  // Should throw a RuntimeException
    }

    // Test 9: Assigning result of division
    @Test
    public void testDeclareAndAssignDivision() {
        String declareQuery = "DECLARE myVar FLOAT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        declareHandler.handle(declareContext);

        String assignQuery = "SET myVar = 42 / 6;";
        PlEsqlProcedureParser.Assignment_statementContext assignContext = parseAssignment(assignQuery);
        assignmentHandler.handle(assignContext);

        assertEquals(7.0, context.getVariable("myVar"));
    }

}
