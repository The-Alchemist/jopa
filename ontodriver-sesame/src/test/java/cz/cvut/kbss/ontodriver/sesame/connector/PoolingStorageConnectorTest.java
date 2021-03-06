/**
 * Copyright (C) 2016 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.ontodriver.sesame.connector;

import cz.cvut.kbss.ontodriver.sesame.Transaction;
import cz.cvut.kbss.ontodriver.sesame.TransactionState;
import cz.cvut.kbss.ontodriver.sesame.environment.TestUtils;
import cz.cvut.kbss.ontodriver.sesame.exceptions.SesameDriverException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PoolingStorageConnectorTest {

    @Mock
    private StorageConnector centralMock;
    @Mock
    private Lock readLock;
    @Mock
    private Lock writeLock;

    private Transaction transaction;

    private PoolingStorageConnector connector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.connector = new PoolingStorageConnector(centralMock);
        final Field transactionField = AbstractConnector.class.getDeclaredField("transaction");
        transactionField.setAccessible(true);
        this.transaction = (Transaction) transactionField.get(connector);
        TestUtils.setMock("READ", PoolingStorageConnector.class, readLock);
        TestUtils.setMock("WRITE", PoolingStorageConnector.class, writeLock);
    }

    @Test
    public void testBegin() throws Exception {
        assertFalse(transaction.isActive());
        connector.begin();
        assertTrue(transaction.isActive());
    }

    @Test
    public void executeSelectOutsideTransactionRunsOnCentralConnector() throws Exception {
        final String query = "Some query";
        connector.executeSelectQuery(query);

        InOrder inOrder = inOrder(readLock, centralMock);
        inOrder.verify(readLock).lock();
        inOrder.verify(centralMock).executeSelectQuery(query);
        inOrder.verify(readLock).unlock();
    }

    @Test
    public void executeSelectInTransactionRunsOnTransactionalRepositoryConnection() throws Exception {
        final String query = "SELECT Some query";
        final RepositoryConnection conn = mock(RepositoryConnection.class);
        final TupleQuery tq = mock(TupleQuery.class);
        when(conn.prepareTupleQuery(QueryLanguage.SPARQL, query)).thenReturn(tq);
        when(centralMock.acquireConnection()).thenReturn(conn);
        connector.begin();
        connector.executeSelectQuery(query);

        verify(conn).prepareTupleQuery(QueryLanguage.SPARQL, query);
        verify(tq).evaluate();
    }

    @Test(expected = SesameDriverException.class)
    public void testUnlockWhenExecuteQueryThrowsException() throws Exception {
        final String query = "Some query";
        when(centralMock.executeSelectQuery(query)).thenThrow(new SesameDriverException());
        try {
            connector.executeSelectQuery(query);
        } finally {
            verify(readLock).lock();
            verify(readLock).unlock();
        }
    }

    @Test
    public void executeBooleanQueryRunsOnCentralConnectionWhenNoTransactionIsActive() throws Exception {
        final String query = "ASK some query";
        connector.executeBooleanQuery(query);

        InOrder inOrder = inOrder(readLock, centralMock);
        inOrder.verify(readLock).lock();
        inOrder.verify(centralMock).executeBooleanQuery(query);
        inOrder.verify(readLock).unlock();
    }

    @Test
    public void executeBooleanRunsOnTransactionalConnectionWhenTransactionIsActive() throws Exception {
        final String query = "ASK some query";
        final RepositoryConnection conn = mock(RepositoryConnection.class);
        final BooleanQuery bq = mock(BooleanQuery.class);
        when(conn.prepareBooleanQuery(QueryLanguage.SPARQL, query)).thenReturn(bq);
        when(centralMock.acquireConnection()).thenReturn(conn);
        connector.begin();
        connector.executeBooleanQuery(query);

        verify(conn).prepareBooleanQuery(QueryLanguage.SPARQL, query);
        verify(bq).evaluate();
    }

    @Test(expected = SesameDriverException.class)
    public void unlocksReadLockWhenExecuteBooleanQueryThrowsException() throws Exception {
        final String query = "ASK some query";
        when(centralMock.executeBooleanQuery(query)).thenThrow(new SesameDriverException());

        try {
            connector.executeBooleanQuery(query);
        } finally {
            verify(readLock).unlock();
        }
    }

    @Test
    public void testExecuteUpdate() throws Exception {
        connector.begin();
        final String query = "Some query";
        connector.executeUpdate(query);

        InOrder inOrder = inOrder(writeLock, centralMock);
        inOrder.verify(writeLock).lock();
        inOrder.verify(centralMock).executeUpdate(query);
        inOrder.verify(writeLock).unlock();
    }

    @Test(expected = SesameDriverException.class)
    public void testUnlockWhenExecuteUpdateThrowsException() throws Exception {
        connector.begin();
        final String query = "Some query";
        doThrow(new SesameDriverException()).when(centralMock).executeUpdate(query);
        try {
            connector.executeUpdate(query);
        } finally {
            verify(writeLock).unlock();
        }
    }

    @Test
    public void testGetContexts() throws Exception {
        connector.getContexts();
        verify(readLock).lock();
        verify(centralMock).getContexts();
        verify(readLock).unlock();
    }

    @Test
    public void testCommit() throws Exception {
        connector.begin();
        connector.commit();
        verify(writeLock).lock();
        verify(centralMock).begin();
        verify(centralMock).addStatements(any(Collection.class));
        verify(centralMock).removeStatements(any(Collection.class));
        verify(centralMock).commit();
        verify(writeLock).unlock();
        assertFalse(transaction.isActive());
    }

    @Test(expected = SesameDriverException.class)
    public void testUnlockWhenCommitThrowsException() throws Exception {
        doThrow(new SesameDriverException()).when(centralMock).commit();
        connector.begin();
        try {
            connector.commit();
        } finally {
            verify(centralMock).begin();
            verify(centralMock).addStatements(any(Collection.class));
            verify(centralMock).removeStatements(any(Collection.class));
            verify(centralMock).commit();
            verify(writeLock).unlock();
            assertEquals(TransactionState.ABORTED, transaction.getState());
        }
    }

    @Test
    public void testRollback() throws Exception {
        connector.begin();
        connector.rollback();
        assertEquals(TransactionState.ABORTED, transaction.getState());
    }

    @Test(expected = IllegalStateException.class)
    public void testAddStatementsInactiveTransaction() {
        final List<Statement> statements = getStatements();
        connector.addStatements(statements);
    }

    private List<Statement> getStatements() {
        final Statement stmt = mock(Statement.class);
        return Collections.singletonList(stmt);
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveStatementsInactiveTransaction() throws Exception {
        final List<Statement> statements = getStatements();
        connector.removeStatements(statements);
    }

    @Test
    public void testClose() throws Exception {
        assertTrue(connector.isOpen());
        connector.close();
        assertFalse(connector.isOpen());
    }

    @Test
    public void unwrapReturnsItselfWhenClassMatches() throws Exception {
        assertSame(connector, connector.unwrap(PoolingStorageConnector.class));
    }

    @Test
    public void transactionBeginAcquiresRepositoryConnection() throws Exception {
        connector.begin();
        verify(centralMock).acquireConnection();
    }

    @Test
    public void transactionCommitReleasesRepositoryConnection() throws Exception {
        final RepositoryConnection conn = mock(RepositoryConnection.class);
        when(centralMock.acquireConnection()).thenReturn(conn);
        connector.begin();
        connector.commit();
        InOrder order = inOrder(centralMock);
        order.verify(centralMock).acquireConnection();
        order.verify(centralMock).releaseConnection(conn);
    }

    @Test
    public void transactionRollbackReleasesRepositoryConnection() throws Exception {
        final RepositoryConnection conn = mock(RepositoryConnection.class);
        when(centralMock.acquireConnection()).thenReturn(conn);
        connector.begin();
        connector.rollback();
        InOrder order = inOrder(centralMock);
        order.verify(centralMock).acquireConnection();
        order.verify(centralMock).releaseConnection(conn);
    }

    @Test
    public void findStatementsReusesRepositoryConnectionDuringTransaction() throws Exception {
        final ValueFactory vf = SimpleValueFactory.getInstance();
        final RepositoryConnection conn = mock(RepositoryConnection.class);
        when(conn.getStatements(any(Resource.class), any(IRI.class), any(Value.class), anyBoolean()))
                .thenReturn(new RepositoryResult<>(mock(CloseableIteration.class)));
        when(centralMock.acquireConnection()).thenReturn(conn);
        final Resource res = vf.createIRI(TestUtils.randomUri());
        final IRI property = vf.createIRI(TestUtils.randomUri());
        connector.begin();
        connector.findStatements(res, property, null, false);
        verify(centralMock).acquireConnection();
        verify(conn).getStatements(res, property, null, false);
    }

    @Test(expected = SesameDriverException.class)
    public void exceptionInFindStatementsCausesTransactionRollback() throws Exception {
        final ValueFactory vf = SimpleValueFactory.getInstance();
        final RepositoryConnection conn = mock(RepositoryConnection.class);
        when(conn.getStatements(any(Resource.class), any(IRI.class), any(Value.class), anyBoolean()))
                .thenThrow(new RepositoryException());
        when(centralMock.acquireConnection()).thenReturn(conn);
        final Resource res = vf.createIRI(TestUtils.randomUri());
        final IRI property = vf.createIRI(TestUtils.randomUri());
        final Connector spy = spy(connector);
        doCallRealMethod().when(spy).begin();
        spy.begin();
        when(spy.findStatements(any(Resource.class), any(IRI.class), any(Value.class), anyBoolean()))
                .thenCallRealMethod();
        try {
            spy.findStatements(res, property, null, false);
        } finally {
            verify(spy).rollback();
        }
    }

    @Test
    public void closeReleasesActiveConnection() throws Exception {
        final RepositoryConnection conn = mock(RepositoryConnection.class);
        when(centralMock.acquireConnection()).thenReturn(conn);
        connector.begin();
        connector.close();
        verify(centralMock).releaseConnection(conn);
    }

    @Test
    public void repeatedCloseIsHandled() throws Exception {
        final RepositoryConnection conn = mock(RepositoryConnection.class);
        when(centralMock.acquireConnection()).thenReturn(conn);
        connector.begin();
        connector.close();
        connector.close();
        verify(centralMock).releaseConnection(conn);
    }
}
