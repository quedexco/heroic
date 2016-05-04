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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.spotify.heroic.common.DateRange;
import com.spotify.heroic.common.Statistics;
import com.spotify.heroic.metric.Event;
import com.spotify.heroic.metric.Metric;
import com.spotify.heroic.metric.MetricCollection;
import com.spotify.heroic.metric.MetricGroup;
import com.spotify.heroic.metric.MetricType;
import com.spotify.heroic.metric.Point;
import com.spotify.heroic.metric.Spread;
import lombok.Data;
import lombok.ToString;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

@Data
public class EmptyInstance implements AggregationInstance {
    public static final Map<String, String> EMPTY_GROUP = ImmutableMap.of();
    public static final EmptyInstance INSTANCE = new EmptyInstance();

    @Override
    public long estimate(DateRange range) {
        return 0;
    }

    @Override
    public AggregationTraversal session(
        final List<AggregationState> states, final DateRange range
    ) {
        final Map<Map<String, String>, SubSession> sessions = new HashMap<>(states.size());

        for (final AggregationState s : states) {
            sessions.put(s.getKey(), new SubSession());
        }

        return new AggregationTraversal(states, new CollectorSession(sessions));
    }

    @Override
    public long cadence() {
        return 0;
    }

    @Override
    public ReducerSession reducer(final DateRange range) {
        return new CollectorReducerSession();
    }

    /**
     * A trivial session that collects all values provided to it.
     */
    @Data
    @ToString(of = {})
    private static final class CollectorSession implements AggregationSession {
        final Map<Map<String, String>, SubSession> sessions;

        @Override
        public void updatePoints(
            Map<String, String> group, List<Point> values
        ) {
            sessions.get(group).points.add(values);
        }

        @Override
        public void updateEvents(
            Map<String, String> group, List<Event> values
        ) {
            sessions.get(group).events.add(values);
        }

        @Override
        public void updateSpreads(
            Map<String, String> group, List<Spread> values
        ) {
            sessions.get(group).spreads.add(values);
        }

        @Override
        public void updateGroup(
            Map<String, String> group, List<MetricGroup> values
        ) {
            sessions.get(group).groups.add(values);
        }

        @Override
        public AggregationResult result() {
            final ImmutableList.Builder<AggregationData> groups = ImmutableList.builder();

            for (final Map.Entry<Map<String, String>, SubSession> e : sessions.entrySet()) {
                final Map<String, String> group = e.getKey();
                final SubSession sub = e.getValue();

                if (!sub.groups.isEmpty()) {
                    groups.add(collectGroup(group, sub.groups, MetricType.GROUP.comparator(),
                        MetricCollection::groups));
                }

                if (!sub.points.isEmpty()) {
                    groups.add(collectGroup(group, sub.points, MetricType.POINT.comparator(),
                        MetricCollection::points));
                }

                if (!sub.events.isEmpty()) {
                    groups.add(collectGroup(group, sub.events, MetricType.EVENT.comparator(),
                        MetricCollection::events));
                }

                if (!sub.spreads.isEmpty()) {
                    groups.add(collectGroup(group, sub.spreads, MetricType.SPREAD.comparator(),
                        MetricCollection::spreads));
                }
            }

            return new AggregationResult(groups.build(), Statistics.empty());
        }

        private <T extends Metric> AggregationData collectGroup(
            final Map<String, String> group, final ConcurrentLinkedQueue<List<T>> collected,
            final Comparator<? super T> comparator,
            final Function<List<T>, MetricCollection> builder
        ) {
            final ImmutableList.Builder<List<T>> iterables = ImmutableList.builder();

            for (final List<T> d : collected) {
                iterables.add(d);
            }

            /* no need to merge, single results are already sorted */
            if (collected.size() == 1) {
                return new AggregationData(group,
                    builder.apply(iterables.build().iterator().next()));
            }

            final ImmutableList<Iterator<T>> iterators =
                ImmutableList.copyOf(iterables.build().stream().map(Iterable::iterator).iterator());
            final Iterator<T> metrics = Iterators.mergeSorted(iterators, comparator);

            return new AggregationData(group, builder.apply(ImmutableList.copyOf(metrics)));
        }
    }

    @Data
    @ToString(of = {})
    private static final class CollectorReducerSession implements ReducerSession {
        private final ConcurrentLinkedQueue<Collected<Point>> points =
            new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<Collected<Event>> events =
            new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<Collected<Spread>> spreads =
            new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<Collected<MetricGroup>> groups =
            new ConcurrentLinkedQueue<>();

        @Override
        public void updatePoints(Map<String, String> group, List<Point> values) {
            points.add(new Collected<Point>(group, values));
        }

        @Override
        public void updateEvents(Map<String, String> group, List<Event> values) {
            events.add(new Collected<Event>(group, values));
        }

        @Override
        public void updateSpreads(Map<String, String> group, List<Spread> values) {
            spreads.add(new Collected<Spread>(group, values));
        }

        @Override
        public void updateGroup(Map<String, String> group, List<MetricGroup> values) {
            groups.add(new Collected<MetricGroup>(group, values));
        }

        @Override
        public ReducerResult result() {
            final ImmutableList.Builder<MetricCollection> groups = ImmutableList.builder();

            if (!this.groups.isEmpty()) {
                groups.add(collectGroup(this.groups, MetricType.GROUP.comparator(),
                    MetricCollection::groups));
            }

            if (!this.points.isEmpty()) {
                groups.add(collectGroup(this.points, MetricType.POINT.comparator(),
                    MetricCollection::points));
            }

            if (!this.events.isEmpty()) {
                groups.add(collectGroup(this.events, MetricType.EVENT.comparator(),
                    MetricCollection::events));
            }

            if (!this.spreads.isEmpty()) {
                groups.add(collectGroup(this.spreads, MetricType.SPREAD.comparator(),
                    MetricCollection::spreads));
            }

            return new ReducerResult(groups.build(), Statistics.empty());
        }

        private <T extends Metric> MetricCollection collectGroup(
            final ConcurrentLinkedQueue<Collected<T>> collected,
            final Comparator<? super T> comparator,
            final Function<List<T>, MetricCollection> builder
        ) {
            final ImmutableList.Builder<List<T>> iterables = ImmutableList.builder();

            for (final Collected<T> d : collected) {
                iterables.add(d.getValues());
            }

            /* no need to merge, single results are already sorted */
            if (collected.size() == 1) {
                return builder.apply(iterables.build().iterator().next());
            }

            final ImmutableList<Iterator<T>> iterators =
                ImmutableList.copyOf(iterables.build().stream().map(Iterable::iterator).iterator());
            final Iterator<T> metrics = Iterators.mergeSorted(iterators, comparator);
            return builder.apply(ImmutableList.copyOf(metrics));
        }

        @Data
        private static final class Collected<T extends Metric> {
            private final Map<String, String> group;
            private final List<T> values;
        }
    }

    static class SubSession {
        private final ConcurrentLinkedQueue<List<Point>> points = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<List<Event>> events = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<List<Spread>> spreads = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<List<MetricGroup>> groups =
            new ConcurrentLinkedQueue<>();
    }
}
