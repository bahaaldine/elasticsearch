
# Getting started with PL|ES|QL

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


# PL|ES|QL Declare Statement Handler

## Overview

The Declare Statement Handler in PL|ES|QL allows you to declare variables of various data types and optionally initialize them with values. Declaring variables in procedures is essential to manage intermediate states, store results, or make values reusable within the procedure.

## Syntax

### Simple Declaration
You can declare a variable without initializing it. The variable will have its type but will not hold any value until you explicitly assign one.

```sql
DECLARE variable_name DATATYPE;
```

### Declaration with Initialization
You can declare a variable and initialize it with a value.

```sql
DECLARE variable_name DATATYPE = value;
```

### Multiple Declarations
PL|ES|QL also allows you to declare multiple variables in a single statement, optionally initializing them.

```sql
DECLARE variable_name1 DATATYPE1, variable_name2 DATATYPE2 = value2;
```

## Supported Data Types
- `INT`
- `FLOAT`
- `STRING`
- `DATE`

## Examples

### 1. Simple Declaration

```sql
DECLARE user_id INT;
```
This declares a variable `user_id` of type `INT` without initializing it.

### 2. Declaration with Initialization

```sql
DECLARE user_id INT = 100;
```
This declares a variable `user_id` of type `INT` and initializes it with the value `100`.

### 3. Multiple Declarations

```sql
DECLARE user_id INT, total FLOAT = 50.5;
```
This declares two variables:
- `user_id` of type `INT` without initialization.
- `total` of type `FLOAT` initialized to `50.5`.

### 4. Redeclaration (Unsupported)

Redeclaring the same variable is not allowed and will result in an error.

```sql
DECLARE user_id INT;
DECLARE user_id INT;  -- This will throw an error
```

### 5. Unsupported Data Type

If you declare a variable with an unsupported data type, PL|ES|QL will throw an error.

```sql
DECLARE unsupported_type CHAR;  -- This will throw an error
```

## Error Handling
PL|ES|QL's Declare Statement Handler throws runtime exceptions in the following scenarios:
1. Redeclaring a variable.
2. Using an unsupported data type.



# PL|ESQL Assignment Statement Handler Documentation

## Overview

The **Assignment Statement Handler** in PL|ESQL allows for assigning values to declared variables within a procedure. It supports integers, floats, strings, and various arithmetic operations, offering flexibility in building complex logic in PL|ESQL procedures.

---

## Features and Examples

### 1. Assigning Integer Values
You can declare and assign an integer value to a variable.

**Example:**
```sql
DECLARE myVar INT;
SET myVar = 42;
```

### 2. Assigning Float Values
The handler also supports floating-point numbers.

**Example:**
```sql
DECLARE myVar FLOAT;
SET myVar = 42.5;
```

### 3. Assigning String Values
You can assign a string to a variable.

**Example:**
```sql
DECLARE myVar STRING;
SET myVar = 'hello';
```

### 4. Performing Arithmetic Operations
The Assignment Statement Handler allows you to perform arithmetic operations before assigning values.

**Example:**
```sql
DECLARE myVar FLOAT;
SET myVar = 5 + 3; -- Result: 8.0
```

You can also perform other arithmetic operations like multiplication:

**Example:**
```sql
DECLARE myVar FLOAT;
SET myVar = 6 * 7; -- Result: 42.0
```

### 5. Variable Reference Assignment
Variables can reference other variables.

**Example:**
```sql
DECLARE var1 INT;
DECLARE var2 INT;
SET var1 = 10;
SET var2 = var1; -- var2 will now hold the value of var1, which is 10
```

### 6. Assigning the Result of a Division
You can divide two numbers and assign the result to a variable.

**Example:**
```sql
DECLARE myVar FLOAT;
SET myVar = 42 / 6; -- Result: 7.0
```

### 7. Type Mismatch Handling
If you attempt to assign a float to an integer variable, a type mismatch error will be thrown.

**Example:**
```sql
DECLARE myVar INT;
SET myVar = 42.5; -- This will result in a RuntimeException due to type mismatch
```

### 8. Unsupported Expressions
The handler will throw an error if an unsupported expression is used.

**Example:**
```sql
DECLARE myVar STRING;
SET myVar = unsupported_expression; -- This will result in a RuntimeException
```

---


# IF Statement Handler Documentation

The **IF Statement Handler** in PL|ESQL allows you to evaluate conditions and execute code based on whether the condition evaluates to true or false. You can also add ELSE and ELSEIF blocks for more complex branching logic.

## Syntax

```sql
IF <condition> THEN
    <statements>
[ELSEIF <condition> THEN
    <statements>]
[ELSE
    <statements>]
END IF;
```

- `<condition>`: An expression that evaluates to a boolean (true or false).
- `<statements>`: One or more statements to be executed if the condition is true.

## Supported Features

### 1. Simple IF Statement

This allows executing a block of code if the condition evaluates to true.

Example:

```sql
BEGIN
    DECLARE myVar INT;
    IF 1 = 1 THEN
        SET myVar = 10;
    END IF;
END;
```

In this example, since `1 = 1` is always true, `myVar` will be set to `10`.

### 2. Simple IF Statement with a False Condition

If the condition is false, the code inside the `IF` block will not execute.

Example:

```sql
BEGIN
    DECLARE myVar INT;
    IF 1 = 2 THEN
        SET myVar = 10;
    END IF;
END;
```

Here, `1 = 2` is false, so `myVar` will remain `null`.

### 3. IF-ELSE Statement

The `ELSE` block allows you to specify what should happen if the `IF` condition is false.

Example:

```sql
BEGIN
    DECLARE myVar INT;
    IF 1 = 2 THEN
        SET myVar = 10;
    ELSE
        SET myVar = 20;
    END IF;
END;
```

In this case, since `1 = 2` is false, the `ELSE` block will execute, and `myVar` will be set to `20`.

### 4. IF-ELSEIF-ELSE Statement

You can chain multiple conditions using `ELSEIF` for more granular control over logic.

Example:

```sql
BEGIN
    DECLARE myVar INT;
    IF 1 = 2 THEN
        SET myVar = 10;
    ELSEIF 1 = 1 THEN
        SET myVar = 20;
    ELSE
        SET myVar = 30;
    END IF;
END;
```

Here, the first condition is false, but the `ELSEIF` condition is true, so `myVar` will be set to `20`.

### 5. Arithmetic Operations in IF Condition

The `IF` condition can evaluate arithmetic expressions.

Example:

```sql
BEGIN
    DECLARE myVar INT;
    IF 5 + 5 = 10 THEN
        SET myVar = 10;
    END IF;
END;
```

Since `5 + 5` equals `10`, the condition is true, and `myVar` will be set to `10`.

### 6. Nested IF Statements

You can nest `IF` statements to handle more complex logic.

Example:

```sql
BEGIN
    DECLARE myVar INT;
    IF 1 = 1 THEN
        IF 2 = 2 THEN
            SET myVar = 10;
        END IF;
    END IF;
END;
```

Here, both conditions are true, so `myVar` will be set to `10`.

### 7. Comparison Operators in IF Condition

The `IF` condition supports comparison operators such as `<`, `>`, `<=`, and `>=`.

Example:

```sql
BEGIN
    DECLARE myVar INT;
    IF 5 > 3 THEN
        SET myVar = 10;
    END IF;
END;
```

Since `5 > 3`, the condition is true, and `myVar` will be set to `10`.

---


# Loop Statement Handler Documentation

## Introduction

The Loop Statement Handler in PL|ESQL allows users to execute a series of statements repeatedly, based on specific loop types, such as `FOR` and `WHILE` loops. These loops can iterate over a range of values or continue until a condition is no longer met.

The following examples provide a detailed overview of how loops can be implemented using PL|ESQL.

---

## Supported Features

### 1. Simple FOR Loop

A `FOR` loop iterates over a range of values. The range can either increment or decrement the variable.

#### Example

```sql
BEGIN
    DECLARE j INT, i INT;
    FOR i IN 1..3 LOOP
        SET j = i + 1;
    END LOOP;
END
```

#### Explanation:
- `i` iterates from 1 to 3.
- `j` is set to `i + 1` for each iteration.
- At the end of the loop, `j` will hold the value 4.

---

### 2. Simple WHILE Loop

A `WHILE` loop continues iterating as long as the specified condition remains true.

#### Example

```sql
BEGIN
    DECLARE i INT = 1;
    WHILE i < 4 LOOP
        SET i = i + 1;
    END LOOP;
END
```

#### Explanation:
- The loop starts with `i` set to 1.
- The loop increments `i` until `i` is no longer less than 4.
- At the end of the loop, `i` will be 4.

---

### 3. Reverse FOR Loop

A `FOR` loop can also iterate in reverse, depending on the specified range.

#### Example

```sql
BEGIN
    DECLARE j INT = 0, i INT;
    FOR i IN 5..3 LOOP
        SET j = j + i;
    END LOOP;
END
```

#### Explanation:
- The loop starts with `i` at 5 and decreases to 3.
- `j` accumulates the sum of `i` during each iteration.
- At the end of the loop, `j` will be 12 (5 + 4 + 3).

---

### 4. WHILE Loop with a False Initial Condition

A `WHILE` loop may not run at all if its initial condition is false.

#### Example

```sql
BEGIN
    DECLARE i INT = 5;
    WHILE i < 4 LOOP
        SET i = i + 1;
    END LOOP;
END
```

#### Explanation:
- Since `i` starts at 5 and the condition `i < 4` is false, the loop never executes.

---

### 5. Nested FOR Loop

PL|ESQL allows for loops to be nested within one another.

#### Example

```sql
BEGIN
    DECLARE i INT;
    DECLARE j INT;
    FOR i IN 1..2 LOOP
        FOR j IN 1..2 LOOP
            SET j = j + 1;
        END LOOP;
    END LOOP;
END
```

#### Explanation:
- The outer loop iterates `i` from 1 to 2.
- The inner loop iterates `j` from 1 to 2, incrementing `j` by 1 in each iteration.
- At the end of the nested loops, `i` will be 2, and `j` will be 3.

---

### 6. Infinite WHILE Loop with Break Condition

You can create an infinite `WHILE` loop and use a `BREAK` condition to exit the loop.

#### Example

```sql
BEGIN
    DECLARE i INT = 1;
    WHILE 1 = 1 LOOP
        SET i = i + 1;
        IF i > 1000 THEN
            BREAK;
        END IF;
    END LOOP;
END
```

#### Explanation:
- The `WHILE 1 = 1` creates an infinite loop.
- The loop increments `i` in each iteration.
- The `BREAK` statement stops the loop when `i` becomes greater than 1000.
- At the end of the loop, `i` will be 1001.

---


# PL/ESQL: Try-Catch Statement Handler Documentation

The **Try-Catch** statement handler in PL/ESQL allows the handling of errors that occur during the execution of a procedure block. The try-catch block is used to wrap statements that may cause an exception and provide a way to handle those exceptions gracefully. This feature mirrors traditional programming constructs and offers structured error handling in PL/ESQL procedures.

## Syntax

```sql
TRY
    -- Statements to execute
CATCH
    -- Statements to handle any errors
FINALLY
    -- Optional block that runs regardless of error occurrence
END TRY;
```

### Basic Example

```sql
BEGIN
    DECLARE j INT;
    TRY
        SET j = 10;
    END TRY;
END;
```

In this example, the value of `j` is successfully set to `10`. Since no error occurs, the CATCH block is not needed, and the procedure completes successfully.

### Example with Error Handling

```sql
BEGIN
    DECLARE j INT;
    TRY
        SET j = 10 / 0;  -- Division by zero error
    CATCH
        SET j = 20;  -- Error is caught and handled
    END TRY;
END;
```

In this example, dividing `10` by `0` triggers an error. However, the error is caught by the **CATCH** block, which sets `j` to `20`.

### Try-Catch-Finally Example

```sql
BEGIN
    DECLARE j INT;
    TRY
        SET j = 10 / 0;  -- Division by zero error
    CATCH
        SET j = 20;  -- Error is caught and handled
    FINALLY
        SET j = 30;  -- This block runs regardless of error occurrence
    END TRY;
END;
```

In this example, even though an error occurs during the **TRY** block, the **FINALLY** block is still executed, and `j` is set to `30`.

### Try-Finally without Catch

```sql
BEGIN
    DECLARE j INT;
    TRY
        SET j = 10;
    FINALLY
        SET j = 20;  -- This block runs regardless of error occurrence
    END TRY;
END;
```

In this example, since no error occurs in the **TRY** block, the **FINALLY** block still runs, and the value of `j` is set to `20`.

## Error Handling in PL/ESQL

The **Try-Catch** statement in PL/ESQL allows developers to manage errors and control the flow of execution based on whether an exception occurs. The **FINALLY** block ensures that critical cleanup or final steps are performed no matter what happens in the **TRY** block.

By utilizing **Try-Catch**, PL/ESQL enables robust error handling, making it suitable for complex procedures and operations where errors need to be managed without terminating the procedure unexpectedly.



# PL|ESQL Throw Statement Handler Documentation

## Introduction

The `THROW` statement in PL|ESQL is used to raise exceptions explicitly within a procedure or block of code. When a `THROW` statement is encountered, the execution of the current block is halted, and the specified error message is returned. If used within a `TRY-CATCH` block, the exception can be caught and handled by the `CATCH` block; otherwise, the exception will propagate out of the procedure.

### Syntax

```sql
THROW 'error_message';
```

- **error_message**: A string representing the error message that will be raised when the `THROW` statement is executed.

### Example Usage

#### Basic `THROW` Statement

```sql
BEGIN
    THROW 'Error occurred';
END
```

This will immediately raise an exception with the message "Error occurred."

#### Complex Error Message

```sql
BEGIN
    THROW 'Complex error: something went wrong with details #$%!';
END
```

You can use complex strings, including special characters, as part of the error message.

### Using `THROW` with `TRY-CATCH`

When the `THROW` statement is used inside a `TRY-CATCH` block, the exception can be caught and handled within the `CATCH` block.

#### Example: `THROW` Inside a `TRY-CATCH`

```sql
BEGIN
    DECLARE v INT = 1;
    TRY
        THROW 'Exception in TRY block';
    CATCH
        SET v = 10;
    END TRY;
END
```

In this example, the exception raised by the `THROW` statement in the `TRY` block is caught by the `CATCH` block, and the variable `v` is set to 10.

### Nested `TRY-CATCH` with `THROW`

You can also use `THROW` within nested `TRY-CATCH` blocks to propagate exceptions further up.

#### Example: Nested `TRY-CATCH` with `THROW`

```sql
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
```

In this case, the inner `THROW` raises an exception that is handled by the inner `CATCH`, which then raises another exception using `THROW`. The outer `CATCH` block handles the second exception.

### Uncaught `THROW`

If a `THROW` statement is used outside of a `TRY-CATCH` block, the exception is unhandled and propagates out of the procedure.

#### Example: Uncaught `THROW`

```sql
BEGIN
    THROW 'Uncaught exception';
END
```

This will raise an uncaught exception with the message "Uncaught exception," which will propagate to the calling environment.

### Error Handling and Best Practices

- **Catching Exceptions**: Always use `TRY-CATCH` blocks when there is a possibility of exceptions being raised by `THROW` statements, especially when performing critical operations.
- **Exception Propagation**: You can use nested `TRY-CATCH` blocks with `THROW` to re-raise exceptions if necessary, allowing outer blocks to handle them.


# Function Definition Handler

The Function Definition Handler in PL/ESQL allows for the creation and execution of functions within a procedure block. It supports defining functions with parameters, return values, and invoking these functions within the flow of a procedure.

## Syntax
```
FUNCTION function_name(parameter1 TYPE, parameter2 TYPE, ...) BEGIN
    -- Function body
    RETURN expression;
END FUNCTION;
```

## Features

### 1. Function without Parameters

Functions can be defined without parameters, and they simply return a value.

#### Example
```
FUNCTION myFunction() BEGIN
    RETURN 10;
END FUNCTION;
```
Here, the function `myFunction` returns the value `10` when invoked.

### 2. Function with Parameters

Functions can also accept parameters and perform operations on them before returning a result.

#### Example
```
FUNCTION add(a INT, b INT) BEGIN
    RETURN a + b;
END FUNCTION;
```

### 3. Function with Multiple Parameters

You can define functions with multiple parameters.

#### Example
```
FUNCTION multiply(a INT, b INT, c INT) BEGIN
    RETURN a * b * c;
END FUNCTION;
```

### 4. Function Call within a Procedure Block

Functions can be invoked from within a procedure block, and their results can be assigned to variables.

#### Example
```
BEGIN
    DECLARE result INT;
    FUNCTION add(a INT, b INT) BEGIN
        RETURN a + b;
    END FUNCTION;
    SET result = add(3, 4);  -- result = 7
END
```

### 5. Function Call within a Loop

Functions can be invoked within loops to perform iterative calculations.

#### Example
```
BEGIN
    DECLARE total INT, i INT;
    FUNCTION add(a INT, b INT) BEGIN
        RETURN a + b;
    END FUNCTION;
    SET total = 0;
    FOR i IN 1..3 LOOP
        SET total = add(total, i);  -- result accumulates values
    END LOOP;
END
```

### 6. Function Call within IF Conditions

Functions can be invoked within an IF condition to determine the flow of the procedure.

#### Example
```
BEGIN
    DECLARE result INT;
    FUNCTION add(a INT, b INT) BEGIN
        RETURN a + b;
    END FUNCTION;
    SET result = add(5, 10);
    IF result > 10 THEN
        SET result = add(result, 5);
    END IF;
END
```

### 7. Multiple Function Definitions and Calls

You can define and call multiple functions within the same procedure block.

#### Example
```
BEGIN
    DECLARE sum INT, product INT;
    FUNCTION add(a INT, b INT) BEGIN
        RETURN a + b;
    END FUNCTION;
    FUNCTION multiply(a INT, b INT) BEGIN
        RETURN a * b;
    END FUNCTION;
    SET sum = add(2, 3);
    SET product = multiply(sum, 4);  -- product = (2 + 3) * 4 = 20
END
```

### 8. Recursive Function

PL/ESQL supports recursive function calls, allowing you to perform complex calculations like factorial.

#### Example
```
BEGIN
    DECLARE factorialResult INT;
    FUNCTION factorial(n INT) BEGIN
        IF n <= 1 THEN
            RETURN 1;
        ELSE
            RETURN n * factorial(n - 1);
        END IF;
    END FUNCTION;
    SET factorialResult = factorial(5);  -- factorialResult = 120
END
```

### 9. Type Coercion in Function Return

PL/ESQL automatically handles type coercion when necessary.

#### Example
```
BEGIN
    DECLARE result FLOAT;
    FUNCTION add(a INT, b INT) BEGIN
        RETURN a + b;
    END FUNCTION;
    SET result = add(2, 3);  -- result is automatically coerced to 5.0 (FLOAT)
END
```

### 10. Function Return STRING in Condition

Functions returning a STRING can be used within conditions.

#### Example
```
BEGIN
    DECLARE message STRING;
    FUNCTION getMessage(code INT) BEGIN
        IF code = 1 THEN
            RETURN 'Success';
        ELSE
            RETURN 'Failure';
        END IF;
    END FUNCTION;
    SET message = getMessage(1);
    IF message = 'Success' THEN
        SET message = 'Operation was successful.';
    ELSE
        SET message = 'Operation failed.';
    END IF;
END
```

### 11. Function Calling Another Function

One function can call another function inside its body.

#### Example
```
BEGIN
    DECLARE result INT;
    FUNCTION add(a INT, b INT) BEGIN
        RETURN a + b;
    END FUNCTION;
    FUNCTION addThreeNumbers(a INT, b INT, c INT) BEGIN
        RETURN add(a, add(b, c));
    END FUNCTION;
    SET result = addThreeNumbers(1, 2, 3);  -- result = 6
END
```

### 12. Error Handling for Functions

If a function does not contain a `RETURN` statement, an error will be thrown during execution.

#### Example
```
BEGIN
    DECLARE result INT;
    FUNCTION faultyFunction(a INT, b INT) BEGIN
        SET result = a + b;
    END FUNCTION;
    SET result = faultyFunction(1, 2);  -- Error: No RETURN statement in function
END
```

### 13. Recursive Function

Recursive functions are supported, allowing complex calculations like factorial.

#### Example
```
BEGIN
    DECLARE factorialResult INT;
    FUNCTION factorial(n INT) BEGIN
        IF n <= 1 THEN
            RETURN 1;
        ELSE
            RETURN n * factorial(n - 1);
        END IF;
    END FUNCTION;
    SET factorialResult = factorial(5);  -- factorialResult = 120
END
```

### 14. Type Coercion

PL/ESQL supports automatic type coercion where necessary.

#### Example
```
BEGIN
    DECLARE result FLOAT;
    FUNCTION add(a INT, b INT) BEGIN
        RETURN a + b;
    END FUNCTION;
    SET result = add(2, 3);  -- Result is automatically coerced to FLOAT
END
```

### 15. Functions Without Parameters

PL/ESQL functions can be created without any parameters.

#### Example
```
BEGIN
    DECLARE greeting STRING;
    FUNCTION sayHello() BEGIN
        RETURN 'Hello, World!';
    END FUNCTION;
    SET greeting = sayHello();
END
```

### 16. Returning String and Using in Condition

Functions returning strings can be used inside conditional statements.

#### Example
```
BEGIN
    DECLARE message STRING;
    FUNCTION getMessage(status INT) BEGIN
        IF status = 1 THEN
            RETURN 'Success';
        ELSE
            RETURN 'Failure';
        END IF;
    END FUNCTION;
    SET message = getMessage(1);
    IF message = 'Success' THEN
        SET message = 'Operation was successful.';
    ELSE
        SET message = 'Operation failed.';
    END IF;
END
```

## Error Handling

PL/ESQL ensures proper error handling for function definitions and execution, throwing runtime errors for missing `RETURN` statements or type mismatches.
