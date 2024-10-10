
# PL|ES|QL Documentation

## What is PL|ES|QL?

PL|ES|QL (Procedural Language for ESQL) is an extension of ESQL (Elasticsearch Query Language) that introduces procedural language constructs. This enables users to write scripts that combine control flow structures like loops, conditionals, and error handling, along with ESQL queries to perform complex data processing tasks. PL|ES|QL aims to enhance the capabilities of ESQL, providing a robust environment for building reusable scripts and automating data operations.

## Supported Features

PL|ES|QL supports various procedural language features, including:

- **Variable Declarations**: Allows declaring and initializing variables.
- **Assignment Statements**: Enables setting variable values.
- **Conditional Statements**: Supports IF-ELSEIF-ELSE blocks for conditional execution.
- **Looping Constructs**: FOR and WHILE loops for iteration.
- **Error Handling**: TRY-CATCH-FINALLY blocks for managing exceptions.
- **Function Definitions**: Supports creating reusable functions.
- **Integration with ESQL Queries**: Allows executing ESQL queries as part of the procedure.

## Language Elements

### Variable Declarations

Variables can be declared with types such as `INT`, `FLOAT`, `STRING`, and `DATE`. For example:

```plaintext
DECLARE myVar INT;
DECLARE anotherVar FLOAT = 3.14;
```

### Assignment Statements

Values can be assigned to variables using the `SET` keyword:

```plaintext
SET myVar = 10;
SET anotherVar = myVar + 5.5;
```

### Conditional Statements

PL|ES|QL supports conditional statements using `IF`, `ELSEIF`, and `ELSE`:

```plaintext
IF myVar > 0 THEN
    SET anotherVar = 20;
ELSEIF myVar < 0 THEN
    SET anotherVar = -20;
ELSE
    SET anotherVar = 0;
END IF
```

### Looping Constructs

#### FOR Loop

Iterates over a range of values:

```plaintext
FOR i IN 1..10 LOOP
    SET myVar = myVar + i;
END LOOP
```

#### WHILE Loop

Executes as long as a condition is true:

```plaintext
WHILE myVar < 100 LOOP
    SET myVar = myVar * 2;
END LOOP
```

### Error Handling

PL|ES|QL provides error handling using `TRY`, `CATCH`, and `FINALLY` blocks:

```plaintext
TRY
    SET myVar = 10 / 0;  // This will cause an exception
CATCH
    SET myVar = -1;  // Handle the error
FINALLY
    SET anotherVar = 100;  // Always execute this
END TRY
```

### Function Definitions

Functions can be defined and invoked within a procedure:

```plaintext
FUNCTION addNumbers(a INT, b INT)
    DECLARE result INT;
    SET result = a + b;
    RETURN result;
END FUNCTION
```

### ESQL Integration

PL|ES|QL supports executing ESQL queries inside procedures:

```plaintext
EXECUTE (ROW a = 1, b = 2 | EVAL sum = a + b);
```

## Examples

### Example 1: Basic Procedure

```plaintext
BEGIN
    DECLARE myVar INT = 0;
    FOR i IN 1..5 LOOP
        SET myVar = myVar + i;
    END LOOP
END
```

### Example 2: Using IF and TRY-CATCH

```plaintext
BEGIN
    DECLARE myVar INT = 10;
    TRY
        IF myVar = 10 THEN
            SET myVar = myVar / 0;  // Will cause an error
        END IF
    CATCH
        SET myVar = -1;  // Handle division by zero
    FINALLY
        SET anotherVar = 100;  // Always execute
    END TRY
END
```

### Example 3: Combining PL|ES|QL with ESQL Queries

```plaintext
BEGIN
    EXECUTE (ROW a = [1, 2, 3] | MV_EXPAND a | SORT a);
END
```

## Conclusion

PL|ES|QL extends ESQL by introducing procedural language constructs, enabling more complex data manipulation workflows. It provides features like loops, conditionals, error handling, and function definitions, allowing users to automate data processing tasks and create more reusable scripts.
