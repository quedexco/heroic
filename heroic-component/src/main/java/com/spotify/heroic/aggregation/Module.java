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

import com.google.common.collect.ImmutableList;
import com.spotify.heroic.HeroicModule;
import com.spotify.heroic.common.Duration;
import com.spotify.heroic.dagger.LoadingComponent;
import com.spotify.heroic.grammar.DurationExpression;
import com.spotify.heroic.grammar.FunctionExpression;
import dagger.Component;
import eu.toolchain.serializer.SerializerFramework;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;

public class Module implements HeroicModule {
    @Override
    public Entry setup(LoadingComponent loading) {
        return DaggerModule_C.builder().loadingComponent(loading).build().entry();
    }

    @Component(dependencies = LoadingComponent.class)
    interface C {
        E entry();
    }

    static class E implements HeroicModule.Entry {
        private final AggregationRegistry c;
        private final AggregationFactory factory;
        private final SerializerFramework s;

        @Inject
        public E(
            AggregationRegistry c, AggregationFactory factory,
            @Named("common") SerializerFramework s
        ) {
            super();
            this.c = c;
            this.factory = factory;
            this.s = s;
        }

        @Override
        public void setup() {
            c.register(Empty.NAME, Empty.class, EmptyInstance.class, args -> Empty.INSTANCE);

            c.register(Group.NAME, Group.class, GroupInstance.class,
                new GroupingAggregationBuilder(factory) {
                    @Override
                    protected Aggregation build(
                        Optional<List<String>> over, Optional<Aggregation> each
                    ) {
                        return new Group(over, each);
                    }
                });

            c.register(Collapse.NAME, Collapse.class, CollapseInstance.class,
                new GroupingAggregationBuilder(factory) {
                    @Override
                    protected Aggregation build(
                        Optional<List<String>> over, Optional<Aggregation> each
                    ) {
                        return new Collapse(over, each);
                    }
                });

            c.register(Chain.NAME, Chain.class, ChainInstance.class,
                new AbstractAggregationDSL(factory) {
                    @Override
                    public Aggregation build(final AggregationArguments args) {
                        final List<Aggregation> chain = ImmutableList.copyOf(args
                            .takeArguments(FunctionExpression.class)
                            .stream()
                            .map(this::asAggregation)
                            .iterator());

                        return new Chain(chain);
                    }
                });

            c.register(Options.NAME, Options.class, AggregationInstance.class,
                new AbstractAggregationDSL(factory) {
                    @Override
                    public Aggregation build(final AggregationArguments args) {
                        final Optional<Aggregation> child = args
                            .getNext("aggregation", FunctionExpression.class)
                            .map(this::asAggregation);

                        final Optional<Duration> size = args
                            .keyword("size", DurationExpression.class)
                            .map(DurationExpression::toDuration);
                        final Optional<Duration> extent = args
                            .keyword("extent", DurationExpression.class)
                            .map(DurationExpression::toDuration);

                        final Optional<SamplingQuery> sampling;

                        if (size.isPresent() || extent.isPresent()) {
                            sampling = Optional.of(new SamplingQuery(size, extent));
                        } else {
                            sampling = Optional.empty();
                        }

                        return new Options(sampling, child);
                    }
                });
        }
    }
}
