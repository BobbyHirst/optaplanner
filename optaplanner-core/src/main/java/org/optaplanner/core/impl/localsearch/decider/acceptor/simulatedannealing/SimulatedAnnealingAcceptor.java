/*
 * Copyright 2010 JBoss Inc
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

package org.optaplanner.core.impl.localsearch.decider.acceptor.simulatedannealing;

import org.optaplanner.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchMoveScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchSolverPhaseScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchStepScope;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.impl.score.ScoreUtils;

/**
 * The time gradient implementation of simulated annealing.
 */
public class SimulatedAnnealingAcceptor extends AbstractAcceptor {

    protected Score startingTemperature;

    protected int levelsLength = -1;
    protected double[] startingTemperatureLevels;
    // No protected Score temperature do avoid rounding errors when using Score.multiply(double)
    protected double[] temperatureLevels;

    protected double temperatureMinimum = 1.0E-100; // Double.MIN_NORMAL is E-308

    public void setStartingTemperature(Score startingTemperature) {
        this.startingTemperature = startingTemperature;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchSolverPhaseScope phaseScope) {
        super.phaseStarted(phaseScope);
        for (double startingTemperatureLevel : ScoreUtils.extractLevelDoubles(startingTemperature)) {
            if (startingTemperatureLevel < 0.0) {
                throw new IllegalArgumentException("The startingTemperature (" + startingTemperature
                        + ") cannot have negative level (" + startingTemperatureLevel + ").");
            }
        }
        startingTemperatureLevels = ScoreUtils.extractLevelDoubles(startingTemperature);
        temperatureLevels = startingTemperatureLevels;
        levelsLength = startingTemperatureLevels.length;
    }

    @Override
    public void phaseEnded(LocalSearchSolverPhaseScope phaseScope) {
        super.phaseEnded(phaseScope);
        startingTemperatureLevels = null;
        temperatureLevels = null;
        levelsLength = -1;
    }

    public boolean isAccepted(LocalSearchMoveScope moveScope) {
        LocalSearchSolverPhaseScope localSearchSolverPhaseScope = moveScope.getStepScope().getPhaseScope();
        Score lastStepScore = localSearchSolverPhaseScope.getLastCompletedStepScope().getScore();
        Score score = moveScope.getScore();
        if (score.compareTo(lastStepScore) >= 0) {
            return true;
        }
        Score scoreDifference = lastStepScore.subtract(score);
        double[] scoreDifferenceLevels = ScoreUtils.extractLevelDoubles(scoreDifference);
        double acceptChance = 1.0;
        for (int i = 0; i < levelsLength; i++) {
            double scoreDifferenceLevel = scoreDifferenceLevels[i];
            double temperatureLevel = temperatureLevels[i];
            double acceptChanceLevel;
            if (scoreDifferenceLevel <= 0.0) {
                // In this level, score is better than the lastStepScore, so do not disrupt the acceptChance
                acceptChanceLevel = 1.0;
            } else {
                acceptChanceLevel = Math.exp(-scoreDifferenceLevel / temperatureLevel);
            }
            acceptChance *= acceptChanceLevel;
        }
        if (moveScope.getWorkingRandom().nextDouble() < acceptChance) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void stepEnded(LocalSearchStepScope stepScope) {
        super.stepEnded(stepScope);
        double timeGradient = stepScope.getTimeGradient();
        double reverseTimeGradient = 1.0 - timeGradient;
        temperatureLevels = new double[levelsLength];
        for (int i = 0; i < levelsLength; i++) {
            temperatureLevels[i] = startingTemperatureLevels[i] * reverseTimeGradient;
            if (temperatureLevels[i] < temperatureMinimum) {
                temperatureLevels[i] = temperatureMinimum;
            }
        }
        // TODO implement reheating
    }

}
