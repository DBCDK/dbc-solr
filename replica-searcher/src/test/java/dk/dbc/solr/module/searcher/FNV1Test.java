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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class FNV1Test {

    public FNV1Test() {
    }


    /**
     * Test of take method, of class FNV1.
     */
    @Test
    public void testOrder() {

        FNV1 yam = FNV1.hash("you and me");
        FNV1 tam = FNV1.hash("thee and me");
        FNV1 may = FNV1.hash("ME AND YOU");

        assertEquals("Expected to be the same regardless of order", yam.take(0x10000000), may.take(0x10000000));
        assertNotEquals("Expected to be different", tam.take(0x10000000), yam.take(0x10000000));
    }
}
