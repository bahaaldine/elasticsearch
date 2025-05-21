/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.interfaces;

import org.elasticsearch.action.ActionListener;
import java.util.List;

public interface AsyncBuiltInFunction {
    void apply(List<Object> args, ActionListener<Object> listener);
}
