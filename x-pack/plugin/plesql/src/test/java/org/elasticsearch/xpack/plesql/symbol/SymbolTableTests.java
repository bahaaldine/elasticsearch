/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.symbol;

import org.elasticsearch.xpack.plesql.primitives.VariableDefinition;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.functions.FunctionDefinition;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SymbolTableTests {

    private SymbolTable symbolTable;

    @Before
    public void setUp() {
        symbolTable = new SymbolTable();
    }

    @Test
    public void testDeclareAndRetrieveVariable() {
        symbolTable.declareVariable("x", "NUMBER");
        assertTrue(symbolTable.hasVariable("x"));
        VariableDefinition def = symbolTable.getVariableDefinition("x");
        assertNotNull(def);
        assertEquals("NUMBER", def.getType().toString());
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateVariableDeclaration() {
        symbolTable.declareVariable("x", "NUMBER");
        symbolTable.declareVariable("x", "NUMBER");
    }

    @Test
    public void testSetAndGetVariable() {
        symbolTable.declareVariable("y", "STRING");
        symbolTable.setVariable("y", "test");
        Object value = symbolTable.getVariable("y");
        assertEquals("test", value);
    }

    @Test(expected = RuntimeException.class)
    public void testGetUndeclaredVariable() {
        symbolTable.getVariable("nonexistent");
    }

    @Test
    public void testDeclareAndRetrieveFunction() {
        // Create a dummy function definition using an anonymous subclass.
        FunctionDefinition dummy = new FunctionDefinition("dummy",
            Collections.emptyList(),
            Collections.<PlEsqlProcedureParser.StatementContext>emptyList()) {
            public Object execute(List<Object> args) {
                return "result";
            }
        };
        symbolTable.declareFunction("dummy", dummy);
        assertTrue(symbolTable.hasFunction("dummy"));
        FunctionDefinition retrieved = symbolTable.getFunction("dummy");
        assertNotNull(retrieved);
        assertEquals("dummy", retrieved.getName());
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateFunctionDeclaration() {
        FunctionDefinition dummy = new FunctionDefinition("dummy",
            Collections.emptyList(),
            Collections.<PlEsqlProcedureParser.StatementContext>emptyList()) {
            public Object execute(List<Object> args) {
                return "result";
            }
        };
        symbolTable.declareFunction("dummy", dummy);
        // Attempting to declare the same function again should fail.
        symbolTable.declareFunction("dummy", dummy);
    }

    @Test
    public void testGetVariableNames() {
        symbolTable.declareVariable("a", "NUMBER");
        symbolTable.declareVariable("b", "STRING");
        Set<String> names = symbolTable.getVariableNames();
        assertTrue(names.contains("a"));
        assertTrue(names.contains("b"));
    }

    @Test
    public void testGetFunctions() {
        FunctionDefinition dummy = new FunctionDefinition("dummy",
            Collections.emptyList(),
            Collections.<PlEsqlProcedureParser.StatementContext>emptyList()) {
            public Object execute(List<Object> args) {
                return "result";
            }
        };
        symbolTable.declareFunction("dummy", dummy);
        assertEquals(1, symbolTable.getFunctions().size());
    }

    @Test
    public void testClear() {
        symbolTable.declareVariable("a", "NUMBER");
        symbolTable.declareVariable("b", "STRING");
        symbolTable.declareFunction("dummy", new FunctionDefinition("dummy",
            Collections.emptyList(),
            Collections.<PlEsqlProcedureParser.StatementContext>emptyList()) {
            public Object execute(List<Object> args) {
                return "result";
            }
        });
        symbolTable.clear();
        assertFalse(symbolTable.hasVariable("a"));
        assertFalse(symbolTable.hasVariable("b"));
        assertFalse(symbolTable.hasFunction("dummy"));
    }
}
