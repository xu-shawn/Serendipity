/*
 * Copyright 2017 Ben-Hur Carlos Vieira Langoni Junior
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.bhlangonijr.chesslib.game;

/**
 * The types of a chess event.
 */
public enum EventType {

    /**
     * Normal match event type.
     */
    NORMAL_MATCH,
    /**
     * Round-robin event type.
     */
    ROUND_ROBIN,
    /**
     * Round-robin carousel event type.
     */
    ROUND_ROBIN_CAROUSEL,
    /**
     * Swiss event type.
     */
    SWISS,
    /**
     * Gauntlet event type.
     */
    GAUNTLET,
    /**
     * Single elimination event type.
     */
    SINGLE_ELIMINATION,
    /**
     * Double elimination event type.
     */
    DOUBLE_ELIMINATION,
    /**
     * Analyses event type.
     */
    ANALYSES
}
