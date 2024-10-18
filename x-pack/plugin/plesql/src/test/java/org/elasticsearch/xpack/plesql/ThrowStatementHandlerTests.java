/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class ThrowStatementHandlerTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;

    @Before
    public void setup() {
        context = new ExecutionContext();
        executor = new ProcedureExecutor(context);
    }

    // Helper method to parse a BEGIN ... END block
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();
    }

    // Test 1: Basic THROW statement inside a procedure
    @Test
    public void testBasicThrowStatementInProcedure() {
        String blockQuery = """
            BEGIN
                THROW 'Error occurred';
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        // Expect the procedure execution to throw a RuntimeException with the specified message
        Exception exception = assertThrows(RuntimeException.class, () -> executor.visitProcedure(blockContext));
        assert exception.getMessage().equals("Error occurred");
    }

    // Test 2: THROW statement with a complex error message inside a procedure
    @Test
    public void testThrowStatementWithComplexMessageInProcedure() {
        String blockQuery = """
            BEGIN
                THROW 'Complex error: something went wrong with details #$%!';
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        // Expect the procedure execution to throw a RuntimeException with the specified message
        Exception exception = assertThrows(RuntimeException.class, () -> executor.visitProcedure(blockContext));
        assert exception.getMessage().equals("Complex error: something went wrong with details #$%!");
    }

    // Test 3: THROW statement in a TRY block with a CATCH block
    @Test
    public void testThrowInTryCatchBlock() {
        String blockQuery = """
            BEGIN
                DECLARE v INT = 1;
                TRY
                    THROW 'Exception in TRY block';
                CATCH
                    SET v = 10;
                END TRY;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        // Execute the procedure and check that the exception is caught and the CATCH block is executed
        executor.visitProcedure(blockContext);
        assert context.getVariable("v").equals(10);
    }

    // Test 4: THROW statement in a nested TRY-CATCH block
    @Test
    public void testNestedTryCatchWithThrow() {
        String blockQuery = """
            BEGIN
                DECLARE v INT = 1;
                TRY
                    TRY
                        THROW 'Inner exception';
                    CATCH
                        SET v = 20;
                        THROW 'Outer exception';
                    END TRY;
                CATCH
                    SET v = 30;
                END TRY;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        // Execute the procedure and check that the outer CATCH block is executed
        executor.visitProcedure(blockContext);
        assert context.getVariable("v").equals(30);
    }

    // Test 5: THROW statement outside of any TRY-CATCH block
    @Test
    public void testThrowOutsideTryCatch() {
        String blockQuery = """
            BEGIN
                THROW 'Uncaught exception';
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        // Expect the procedure execution to throw a RuntimeException with the specified message
        Exception exception = assertThrows(RuntimeException.class, () -> executor.visitProcedure(blockContext));
        assert exception.getMessage().equals("Uncaught exception");
    }
}
