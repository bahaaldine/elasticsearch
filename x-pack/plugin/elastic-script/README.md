# elastic-script Language Documentation

## Introduction

**elastic-script** (`escript`) is a procedural extension to ESQL (Elasticsearch Query Language), providing structured programming constructs and advanced workflow capabilities for agents and complex Elasticsearch automation. It enables users to define reusable procedures, control execution flow, manage variables, handle errors, and compose modular logic that integrates seamlessly with ESQL queries and Elasticsearch operations.

elastic-script is designed for:
- **Agents**: Automate multi-step Elasticsearch workflows.
- **Advanced Users**: Orchestrate data processing, error handling, and conditional logic within and beyond ESQL queries.
- **Developers**: Contribute custom functions and extend the language for new use cases.

---

## Table of Contents
- [Language Constructs](#language-constructs)
  - [DECLARE](#declare)
  - [SET](#set)
  - [IF / ELSEIF / ELSE](#if--elseif--else)
  - [FOR Loop](#for-loop)
  - [WHILE Loop](#while-loop)
  - [BREAK](#break)
  - [TRY / CATCH / FINALLY](#try--catch--finally)
  - [THROW](#throw)
  - [FUNCTION / RETURN](#function--return)
  - [CALL / EXECUTE](#call--execute)
  - [PERSIST](#persist)
- [Built-in Functions](#built-in-functions)
- [Modularity & Procedure Calling](#modularity--procedure-calling)
- [Function Contribution and Discovery](#function-contribution-and-discovery)
- [Types & Type System](#types--type-system)
- [Error Handling](#error-handling)

---

## Language Constructs

### DECLARE
**Purpose:** Declare variables of a specific type, optionally with an initial value.

**Syntax:**
```sql
DECLARE var_name TYPE;
DECLARE var_name TYPE = value;
DECLARE var1 TYPE1, var2 TYPE2 = value2;
```

**Supported Types:** `INT`, `FLOAT`, `STRING`, `DATE`

**Examples:**
```sql
DECLARE count INT;
DECLARE message STRING = 'hello';
DECLARE x INT, y FLOAT = 3.14;
```
**Edge Cases:**
```sql
DECLARE count INT;
DECLARE count INT; -- Error: Redeclaration not allowed
DECLARE foo CHAR;  -- Error: CHAR is not a supported type
```

---

### SET
**Purpose:** Assign a value or expression result to a previously declared variable.

**Syntax:**
```sql
SET var_name = expression;
```

**Examples:**
```sql
DECLARE a INT; SET a = 42;
DECLARE b FLOAT; SET b = 3.14 * 2;
DECLARE s STRING; SET s = 'abc';
SET a = a + 1;
```
**Edge Cases:**
```sql
DECLARE i INT; SET i = 1.5; -- Error: Type mismatch
SET x = y + z; -- Error if x is undeclared
```

---

### IF / ELSEIF / ELSE
**Purpose:** Conditional logic; execute code blocks based on conditions.

**Syntax:**
```sql
IF <condition> THEN
    <statements>
[ELSEIF <condition> THEN
    <statements>]
[ELSE
    <statements>]
END IF;
```

**Examples:**
```sql
IF x > 10 THEN
    SET y = 1;
ELSE
    SET y = 2;
END IF;
```
```sql
IF a = 1 THEN
    SET result = 'one';
ELSEIF a = 2 THEN
    SET result = 'two';
ELSE
    SET result = 'other';
END IF;
```

**Edge Cases:**
```sql
IF 0 THEN SET x = 1; END IF; -- Error: Condition must be boolean
```

---

### FOR Loop
**Purpose:** Iterate over a numeric range (inclusive, ascending or descending).

**Syntax:**
```sql
FOR var IN start..end LOOP
    <statements>
END LOOP;
```

**Examples:**
```sql
DECLARE sum INT = 0, i INT;
FOR i IN 1..3 LOOP
    SET sum = sum + i;
END LOOP;
-- sum = 6 after loop
```
```sql
FOR i IN 5..3 LOOP
    SET x = i;
END LOOP;
-- Loops from 5 down to 3 (descending)
```

**Edge Cases:**
```sql
FOR i IN 1..1 LOOP ... END LOOP; -- Executes once
FOR i IN 2..1 LOOP ... END LOOP; -- Executes for i=2,1
```

---

### WHILE Loop
**Purpose:** Repeat a block as long as a condition is true.

**Syntax:**
```sql
WHILE <condition> LOOP
    <statements>
END LOOP;
```

**Examples:**
```sql
DECLARE i INT = 0;
WHILE i < 3 LOOP
    SET i = i + 1;
END LOOP;
```
```sql
WHILE 1 = 1 LOOP
    -- infinite loop; use BREAK to exit
END LOOP;
```

---

### BREAK
**Purpose:** Exit the nearest enclosing loop immediately.

**Syntax:**
```sql
BREAK;
```
**Example:**
```sql
FOR i IN 1..100 LOOP
    IF i > 5 THEN BREAK; END IF;
END LOOP;
```

---

### TRY / CATCH / FINALLY
**Purpose:** Structured error handling for procedure blocks.

**Syntax:**
```sql
TRY
    <statements>
[CATCH
    <statements>]
[FINALLY
    <statements>]
END TRY;
```

**Examples:**
```sql
TRY
    SET x = 1 / 0;
CATCH
    SET x = 0;
END TRY;
```
```sql
TRY
    -- do something
FINALLY
    -- always runs
END TRY;
```

**Edge Cases:**
```sql
TRY SET x = 1; END TRY; -- No error, CATCH is not required
```

---

### THROW
**Purpose:** Raise a custom exception, optionally with a message.

**Syntax:**
```sql
THROW 'error message';
```

**Examples:**
```sql
IF x < 0 THEN
    THROW 'Negative value not allowed';
END IF;
```
```sql
TRY
    THROW 'fail';
CATCH
    SET error = 'caught';
END TRY;
```

**Edge Cases:**
```sql
THROW 'fatal'; -- If uncaught, procedure fails
```

---

### FUNCTION / RETURN
**Purpose:** Define and use reusable procedures with parameters and return values.

**Syntax:**
```sql
FUNCTION fname(arg1 TYPE, arg2 TYPE, ...) BEGIN
    <statements>
    RETURN expr;
END FUNCTION;
```

**Examples:**
```sql
FUNCTION add(a INT, b INT) BEGIN
    RETURN a + b;
END FUNCTION;
SET x = add(2, 3); -- x = 5
```
```sql
FUNCTION fact(n INT) BEGIN
    IF n <= 1 THEN RETURN 1; ELSE RETURN n * fact(n-1); END IF;
END FUNCTION;
SET f = fact(5); -- f = 120
```
**Edge Cases:**
```sql
FUNCTION bad() BEGIN SET x = 1; END FUNCTION; -- Error: No RETURN
```

---

### CALL / EXECUTE
**Purpose:** Invoke a procedure or function, optionally with arguments.

**Syntax:**
```sql
CALL procedure_name(arguments...);
EXECUTE procedure_name(arguments...);
```

**Examples:**
```sql
CALL my_procedure(1, 'abc');
SET x = my_function(2, 3);
```

---

### PERSIST
**Purpose:** Save data or state persistently (agent-specific, may depend on context).

**Syntax:**
```sql
PERSIST <expression>;
```

**Examples:**
```sql
PERSIST result;
PERSIST { "key": value };
```

---

## Built-in Functions

elastic-script provides a set of built-in functions for arithmetic, string, date, and utility operations. These are available in any script and can be called like user-defined functions.

**Examples:**
```sql
SET x = abs(-5);       -- x = 5
SET s = concat('a', 'b'); -- s = 'ab'
SET now = current_date();
SET maxval = max(2, 10);
```

**How to use:**
- Call directly: `SET y = sqrt(9);`
- Use in expressions: `IF is_null(foo) THEN ...`

**Representative Built-ins:**
- `abs(x)`, `ceil(x)`, `floor(x)`, `round(x)`
- `concat(s1, s2)`, `substring(s, start, length)`
- `length(s)`, `to_string(x)`, `to_int(s)`
- `current_date()`, `now()`
- `is_null(x)`, `coalesce(a, b, ...)`
- `max(a, b)`, `min(a, b)`

**Edge Cases:**
```sql
SET x = sqrt(-1); -- May throw error or return null
```

---

## Modularity & Procedure Calling

elastic-script supports modularity via:
- **Functions:** Define reusable logic within a script.
- **Calling Procedures:** Use `CALL` or `EXECUTE` to invoke other procedures, including those defined elsewhere or contributed by plugins.
- **Nested Functions:** Functions can call other functions, including recursively.

**Example:**
```sql
FUNCTION increment(x INT) BEGIN RETURN x + 1; END FUNCTION;
CALL increment(10);
```

**Calling Procedures from Other Procedures:**
```sql
FUNCTION foo() BEGIN RETURN 1; END FUNCTION;
FUNCTION bar() BEGIN RETURN foo() + 1; END FUNCTION;
SET x = bar();
```

---

## Function Contribution and Discovery

elastic-script supports extension via annotated Java methods:
- **@FunctionSpec Annotation:** Functions implemented in Java and annotated with `@FunctionSpec` are auto-discovered and registered as elastic-script built-ins.
- **Discovery:** At runtime, all classes with `@FunctionSpec` methods are scanned and made available.
- **Usage:** Contributed functions are used just like built-ins or user-defined functions.

**Example (Java):**
```java
@FunctionSpec(name = "triple", description = "Multiply by 3")
public static int triple(int x) { return 3 * x; }
```
**In elastic-script:**
```sql
SET y = triple(7); -- y = 21
```

---

## Types & Type System

- **Primitive Types:** `INT`, `FLOAT`, `STRING`, `DATE`
- **Type Coercion:** Where safe, automatic coercion may occur (e.g., assigning INT to FLOAT).
- **Strictness:** Type errors (e.g., assigning FLOAT to INT) result in runtime exceptions.
- **Nulls:** Variables are null by default until assigned. Use `is_null(var)` to check.

**Examples:**
```sql
DECLARE n INT; SET n = null; IF is_null(n) THEN SET n = 1; END IF;
```

---

## Error Handling

- **TRY/CATCH/FINALLY:** Structured error handling as described above.
- **THROW:** Raise custom errors.
- **Runtime Exceptions:** Type errors, redeclaration, missing RETURN, invalid expressions, and other violations result in runtime errors.
- **Error Propagation:** Uncaught exceptions propagate up and may terminate the script.

**Examples:**
```sql
TRY
    SET x = 1 / 0;
CATCH
    SET x = -1;
END TRY;
```

---

## Control Structures Overview

elastic-script supports all standard procedural control structures:
- **Variables:** `DECLARE`, `SET`
- **Conditionals:** `IF`, `ELSEIF`, `ELSE`
- **Loops:** `FOR`, `WHILE`, `BREAK`
- **Error Handling:** `TRY`, `CATCH`, `FINALLY`, `THROW`
- **Functions:** `FUNCTION`, `RETURN`, `CALL`
- **Persistence:** `PERSIST`

All constructs can be nested and combined for advanced workflows.

---

## Example: Complete elastic-script Procedure
```sql
BEGIN
    DECLARE sum INT = 0, i INT;
    FUNCTION square(x INT) BEGIN
        RETURN x * x;
    END FUNCTION;
    FOR i IN 1..5 LOOP
        SET sum = sum + square(i);
    END LOOP;
    IF sum > 50 THEN
        PERSIST sum;
    ELSE
        THROW 'Sum too small';
    END IF;
END
```

---

## See Also
- [ESQL Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql.html)
- [Elasticsearch Agent Framework](https://www.elastic.co/guide/en/elasticsearch/reference/current/agents.html)

