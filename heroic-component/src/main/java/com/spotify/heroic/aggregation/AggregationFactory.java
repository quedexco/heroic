/*
 * Copyright (c) 2015 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.heroic.aggregation;

import com.spotify.heroic.grammar.ListValue;
import com.spotify.heroic.grammar.Value;

import java.util.Map;

/**
 * Factory to dynamically build aggregations.
 * <p>
 * Used in Query DSL.
 *
 * @author udoprog
 */
public interface AggregationFactory {
    /**
     * Build an aggregation with the given name and arguments.
     *
     * @param name The name of the aggregation.
     * @param args Positional arguments of the aggregation.
     * @param keywords Keyword arguments of the aggregation.
     * @return The built aggregation.
     * @throws MissingAggregation If the given name does not reflect an available aggregation.
     */
    public Aggregation build(String name, ListValue args, Map<String, Value> keywords);
}
