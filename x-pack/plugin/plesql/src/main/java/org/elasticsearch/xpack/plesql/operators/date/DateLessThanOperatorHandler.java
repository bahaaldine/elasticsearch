/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.operators.date;

import org.elasticsearch.xpack.plesql.operators.primitives.BinaryOperatorHandler;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Handles date {@literal <} comparisons by comparing ISO‑formatted dates.
 */
public class DateLessThanOperatorHandler implements BinaryOperatorHandler {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public boolean isApplicable(Object left, Object right) {
        // Case 1: direct LocalDate comparison
        if (left instanceof LocalDate && right instanceof LocalDate) {
            return true;
        }
        // Case 2: String inputs that parse as ISO dates
        if (left instanceof String && right instanceof String) {
            try {
                FORMATTER.parse((String) left);
                FORMATTER.parse((String) right);
                return true;
            } catch (DateTimeParseException e) {
                // not a valid date, so not applicable
            }
        }
        return false;
    }

    @Override
    public Object apply(Object left, Object right) {
        LocalDate leftDate = parseDate(left);
        LocalDate rightDate = parseDate(right);
        return leftDate.isBefore(rightDate);
    }

    private LocalDate parseDate(Object obj) {
        if (obj instanceof LocalDate) {
            return (LocalDate) obj;
        }
        try {
            return LocalDate.parse(obj.toString(), FORMATTER);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid date format, expected ISO_LOCAL_DATE: " + obj, e);
        }
    }
}
