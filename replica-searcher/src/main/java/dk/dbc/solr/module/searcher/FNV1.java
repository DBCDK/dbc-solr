/*
 * Copyright (C) 2017 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-solr-modules-replica-searcher
 *
 * dbc-solr-modules-replica-searcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-solr-modules-replica-searcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.solr.module.searcher;

/**
 * https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
 *
 * @author DBC {@literal <dbc.dk>}
 */
class FNV1 {

    private long hash, next;


    private FNV1() {
        this.hash = 0xcbf29ce484222325L;
    }

    private void add(int i) {
        hash = hash * 0x100000001b3L;
        next = hash = hash ^ i & 0xff;
    }

    void reset() {
        next = hash;
    }

    long take(long max) {
        next = Long.rotateRight(next, 1);
        return Long.remainderUnsigned(next, max);
    }

    static FNV1 hash(String text) {
        FNV1 hash = new FNV1();
        text.chars()
                .filter(Character::isLetterOrDigit)
                .map(Character::toLowerCase)
                .sorted()
                .forEachOrdered(hash::add);
        return hash;
    }
}
