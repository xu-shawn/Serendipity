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

package com.github.bhlangonijr.chesslib.pgn;

/**
 * An observer to PGN loading events, notified when games are extracted from a Portable Game Notation file.
 */
public interface PgnLoadListener {

    /**
     * Method invoked when a game is loaded from a PGN file.
     *
     * @param games the total number of games loaded from the PGN file
     */
    void notifyProgress(int games);

}
