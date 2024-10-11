
# PL|ES|QL Comprehensive Documentation

## Overview

PL|ES|QL is an extension of ESQL that allows users to define procedures combining ESQL queries and procedural logic. It provides a procedural language for Elasticsearch Query Language (ESQL), adding support for structured programming constructs such as variable declarations, conditionals, loops, error handling, and more.

The goal of PL|ES|QL is to allow developers to create complex workflows that include ESQL queries and handle various conditions using familiar programming constructs.

## Supported Handlers

The following handlers are currently supported in PL|ES|QL:

1. **Variable Declaration (`DECLARE`)**
2. **Assignment (`SET`)**
3. **Conditional Statements (`IF`, `ELSE`, `ELSEIF`)**
4. **Loops (`FOR`, `WHILE`)**
5. **Try-Catch Statements (`TRY`, `CATCH`, `FINALLY`)**
6. **Throw Statement (`THROW`)**
7. **Function Definition and Invocation (`FUNCTION`, `CALL`)**

---

## 1. Variable Declaration (`DECLARE`)

### Description

The `DECLARE` statement is used to declare variables in the procedure. Variables can be of different types: `INT`, `FLOAT`, `STRING`, and `DATE`.

### Syntax

```sql
DECLARE variable_name TYPE [= initial_value];
```

### Example

```sql
BEGIN
    DECLARE x INT = 5, y FLOAT = 10.0;
    -- Multiple declarations in one statement
    DECLARE name STRING;
END
```

### Notes

- Variables must be declared before being used in the procedure.
- Initialization is optional. If not provided, the variable's initial value will be `null`.

---

## 2. Assignment (`SET`)

### Description

The `SET` statement assigns a value to a declared variable.

### Syntax

```sql
SET variable_name = expression;
```

### Example

```sql
BEGIN
    DECLARE x INT, y FLOAT;
    SET x = 10;
    SET y = x + 5.5;
END
```

### Notes

- The expression can involve arithmetic operations or reference other variables.

---

## 3. Conditional Statements (`IF`, `ELSE`, `ELSEIF`)

### Description

Conditional statements allow you to execute code blocks based on certain conditions.

### Syntax

```sql
IF condition THEN
    -- Statements
ELSEIF condition THEN
    -- Statements
ELSE
    -- Statements
END IF
```

### Example

```sql
BEGIN
    DECLARE x INT = 10;
    IF x > 5 THEN
        SET x = x + 1;
    ELSEIF x = 5 THEN
        SET x = 0;
    ELSE
        SET x = -1;
    END IF
END
```

### Notes

- Multiple `ELSEIF` branches can be used.
- The `ELSE` block is optional.

---

## 4. Loops (`FOR`, `WHILE`)

### Description

Loops allow for repeated execution of a code block.

### Syntax

#### `FOR` Loop

```sql
FOR variable_name IN start_value..end_value LOOP
    -- Statements
END LOOP
```

#### `WHILE` Loop

```sql
WHILE condition LOOP
    -- Statements
END LOOP
```

### Example

#### `FOR` Loop

```sql
BEGIN
    DECLARE i INT, sum INT = 0;
    FOR i IN 1..5 LOOP
        SET sum = sum + i;
    END LOOP
END
```

#### `WHILE` Loop

```sql
BEGIN
    DECLARE counter INT = 1;
    WHILE counter < 10 LOOP
        SET counter = counter + 1;
    END LOOP
END
```

### Notes

- `FOR` loops can run in ascending or descending order.
- `WHILE` loops continue until the condition is false.

---

## 5. Try-Catch Statements (`TRY`, `CATCH`, `FINALLY`)

### Description

`TRY-CATCH` statements allow error handling within a procedure.

### Syntax

```sql
TRY
    -- Statements that may throw an exception
CATCH
    -- Statements to handle the exception
FINALLY
    -- Optional statements to execute regardless of whether an error occurred
END TRY
```

### Example

```sql
BEGIN
    DECLARE x INT = 1;
    TRY
        SET x = x / 0; -- This will trigger a division by zero error
    CATCH
        SET x = 0; -- Error handling code
    END TRY
END
```

### Notes

- The `FINALLY` block is optional and is always executed.
- Nested `TRY` blocks are supported.

---

## 6. Throw Statement (`THROW`)

### Description

The `THROW` statement allows you to manually raise an exception.

### Syntax

```sql
THROW 'error_message';
```

### Example

```sql
BEGIN
    DECLARE x INT = 5;
    IF x < 10 THEN
        THROW 'Value is too low';
    END IF
END
```

### Notes

- The exception message is a string and should be enclosed in single quotes.

---

## 7. Function Definition and Invocation (`FUNCTION`, `CALL`)

### Description

Functions allow for defining reusable code blocks.

### Syntax

#### Define a Function

```sql
FUNCTION function_name(parameter1 TYPE, parameter2 TYPE)
    -- Function body
END FUNCTION
```

#### Call a Function

```sql
CALL function_name(argument1, argument2);
```

### Example

```sql
BEGIN
    FUNCTION add(a INT, b INT)
        DECLARE result INT;
        SET result = a + b;
    END FUNCTION;

    DECLARE sum INT;
    CALL add(5, 10);
END
```

### Notes

- Functions can take parameters and use local variables.
- Recursive calls are supported.
