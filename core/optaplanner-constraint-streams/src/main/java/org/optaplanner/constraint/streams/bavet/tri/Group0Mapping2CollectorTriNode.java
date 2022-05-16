/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.constraint.streams.bavet.tri;

import java.util.function.Consumer;

import org.optaplanner.constraint.streams.bavet.bi.BiTuple;
import org.optaplanner.constraint.streams.bavet.common.Group;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.tri.TriConstraintCollector;
import org.optaplanner.core.impl.util.Pair;

final class Group0Mapping2CollectorTriNode<OldA, OldB, OldC, A, B, ResultContainerA_, ResultContainerB_>
        extends AbstractGroupTriNode<OldA, OldB, OldC, BiTuple<A, B>, String, Object, Pair<A, B>> {

    private static final String NO_GROUP_KEY = "NO_GROUP";

    private final int outputStoreSize;

    public Group0Mapping2CollectorTriNode(int groupStoreIndex,
            TriConstraintCollector<OldA, OldB, OldC, ResultContainerA_, A> collectorA,
            TriConstraintCollector<OldA, OldB, OldC, ResultContainerB_, B> collectorB,
            Consumer<BiTuple<A, B>> nextNodesInsert, Consumer<BiTuple<A, B>> nextNodesRetract,
            int outputStoreSize) {
        super(groupStoreIndex, mergeCollectors(collectorA, collectorB), nextNodesInsert, nextNodesRetract);
        this.outputStoreSize = outputStoreSize;
    }

    static <OldA, OldB, OldC, A, B, ResultContainerA_, ResultContainerB_>
            TriConstraintCollector<OldA, OldB, OldC, Object, Pair<A, B>> mergeCollectors(
                    TriConstraintCollector<OldA, OldB, OldC, ResultContainerA_, A> collectorA,
                    TriConstraintCollector<OldA, OldB, OldC, ResultContainerB_, B> collectorB) {
        return (TriConstraintCollector<OldA, OldB, OldC, Object, Pair<A, B>>) ConstraintCollectors.compose(collectorA,
                collectorB,
                Pair::of);
    }

    @Override
    protected String createGroupKey(TriTuple<OldA, OldB, OldC> tuple) {
        return NO_GROUP_KEY;
    }

    @Override
    protected BiTuple<A, B> createOutTuple(Group<BiTuple<A, B>, String, Object> group) {
        Object resultContainer = group.resultContainer;
        Pair<A, B> result = finisher.apply(resultContainer);
        A a = result.getKey();
        B b = result.getValue();
        return new BiTuple<>(a, b, outputStoreSize);
    }

    @Override
    public String toString() {
        return "GroupTriNode 0+2";
    }
}