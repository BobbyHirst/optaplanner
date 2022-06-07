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

package org.optaplanner.constraint.streams.bavet.quad;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.optaplanner.constraint.streams.bavet.BavetConstraintFactory;
import org.optaplanner.constraint.streams.bavet.common.AbstractInserter;
import org.optaplanner.constraint.streams.bavet.common.AbstractUpdater;
import org.optaplanner.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import org.optaplanner.constraint.streams.bavet.common.NodeBuildHelper;
import org.optaplanner.core.api.function.QuadPredicate;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.stream.ConstraintStream;

public final class BavetFilterQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> {

    private final BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent;
    private final QuadPredicate<A, B, C, D> predicate;

    public BavetFilterQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent,
            QuadPredicate<A, B, C, D> predicate) {
        super(constraintFactory, parent.getRetrievalSemantics());
        this.parent = parent;
        this.predicate = predicate;
        if (predicate == null) {
            throw new IllegalArgumentException("The predicate (null) cannot be null.");
        }
    }

    @Override
    public boolean guaranteesDistinct() {
        return parent.guaranteesDistinct();
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        parent.collectActiveConstraintStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    @Override
    public ConstraintStream getTupleSource() {
        return parent.getTupleSource();
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        buildHelper.<QuadTuple<A, B, C, D>> putInsertUpdateRetract(this, childStreamList,
                insert -> new ConditionalQuadInserter<>(predicate, insert),
                (update, retract) -> new ConditionalQuadUpdater<>(predicate, update, retract));
    }

    private static final class ConditionalQuadInserter<A, B, C, D> extends AbstractInserter<QuadTuple<A, B, C, D>> {
        private final QuadPredicate<A, B, C, D> predicate;

        public ConditionalQuadInserter(QuadPredicate<A, B, C, D> predicate, Consumer<QuadTuple<A, B, C, D>> insert) {
            super(insert);
            this.predicate = predicate;
        }

        @Override
        protected boolean test(QuadTuple<A, B, C, D> tuple) {
            return predicate.test(tuple.factA, tuple.factB, tuple.factC, tuple.factD);
        }
    }

    private static final class ConditionalQuadUpdater<A, B, C, D> extends AbstractUpdater<QuadTuple<A, B, C, D>> {
        private final QuadPredicate<A, B, C, D> predicate;

        public ConditionalQuadUpdater(QuadPredicate<A, B, C, D> predicate, Consumer<QuadTuple<A, B, C, D>> update,
                Consumer<QuadTuple<A, B, C, D>> retract) {
            super(update, retract);
            this.predicate = predicate;
        }

        @Override
        protected boolean test(QuadTuple<A, B, C, D> tuple) {
            return predicate.test(tuple.factA, tuple.factB, tuple.factC, tuple.factD);
        }
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public int hashCode() {
        return Objects.hash(parent, predicate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof BavetFilterQuadConstraintStream) {
            BavetFilterQuadConstraintStream<?, ?, ?, ?, ?> other = (BavetFilterQuadConstraintStream<?, ?, ?, ?, ?>) o;
            return parent == other.parent
                    && predicate == other.predicate;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Filter() with " + childStreamList.size() + " children";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

}