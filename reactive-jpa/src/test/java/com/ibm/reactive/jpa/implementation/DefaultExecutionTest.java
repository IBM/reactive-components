/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibm.reactive.jpa.IsolationLevel;
import com.ibm.reactive.jpa.TransactionDefinition;
import com.ibm.reactive.jpa.lombok.LombokTestUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.internal.SessionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
public class DefaultExecutionTest {

  @Mock
  EntityManager entityManager;

  @Mock
  SessionFactory factory;

  @Mock
  SessionImpl session;

  @Mock
  Connection connection;

  @Mock
  Transaction transaction;

  @Test
  public void testLombokNullValidations() {
    DefaultExecution<Integer> execution = new DefaultExecution<>(
        TransactionDefinition.builder().build(), factory);
    LombokTestUtil.testLombokNullValidations(Stream.of(
        () -> execution.execute(null)
    ));

  }

  @Test
  public void testLombokMethods() {
    DefaultExecution execution = new DefaultExecution(
        TransactionDefinition.builder().build(), factory);

    assertNotNull(execution.getSessionFactory());

    assertNotNull(execution.getTransactionDefinition());

  }

  @Test
  public void testCreation() {
    assertNotNull(new DefaultExecution(TransactionDefinition.builder().build(), factory));
  }

  private static Stream<TransactionDefinition> successfulExecutionArguments() {
    return Stream.of(
        TransactionDefinition.builder().isReadonly(true).build(),
        null,
        TransactionDefinition.builder().isolation(IsolationLevel.DEFAULT).build(),
        TransactionDefinition.builder().isolation(IsolationLevel.SERIALIZABLE).build(),
        TransactionDefinition.builder().isReadonly(true).isolation(IsolationLevel.SERIALIZABLE)
            .build(),
        TransactionDefinition.builder().timeout(1).build()
    );
  }

  @ParameterizedTest
  @MethodSource("successfulExecutionArguments")
  void testSuccessfulExecution(TransactionDefinition definition)
      throws SQLException {
    setupTransactionMocks(definition);
    DefaultExecution<Integer> execution = new DefaultExecution<>(definition,
        factory);
    Integer result = execution.execute(entityManager -> 1);
    assertEquals(result.intValue(), 1);
    if (definition != null) {
      assertTransactionInvocations(definition);
    }
  }

  @Test
  public void testSuccessfulExecutionConnectionClosed()
      throws SQLException {
    TransactionDefinition definition = TransactionDefinition.builder().isReadonly(true).build();
    when(factory.createEntityManager()).thenReturn(entityManager);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.getHibernateFlushMode()).thenReturn(FlushMode.AUTO);
    when(session.getTransaction()).thenReturn(transaction);
    when(session.connection()).thenReturn(connection);

    DefaultExecution<Integer> execution = new DefaultExecution<>(definition,
        factory);
    Integer result = execution.execute(entityManager -> {
      when(session.connection()).thenReturn(null);
      return 1;
    });
    assertEquals(result.intValue(), 1);
    verify(connection, times(1)).setReadOnly(true);
    verify(connection, times(0)).setReadOnly(false);

    // restore
    when(session.connection()).thenReturn(connection);

    result = execution.execute(entityManager -> {
      when(session.isConnected()).thenReturn(false);
      return 1;
    });

    assertEquals(result.intValue(), 1);
    verify(connection, times(2)).setReadOnly(true);
    verify(connection, times(0)).setReadOnly(false);
  }

  private void setupTransactionMocks(TransactionDefinition definition) throws SQLException {
    when(factory.createEntityManager()).thenReturn(entityManager);
    if (definition != null) {
      when(entityManager.unwrap(Session.class)).thenReturn(session);
      when(session.getHibernateFlushMode()).thenReturn(FlushMode.AUTO);
      when(session.getTransaction()).thenReturn(transaction);
      when(session.connection()).thenReturn(connection);
      if (definition.getIsolation() != IsolationLevel.DEFAULT) {
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_NONE);
      }
      if (definition.isReadonly()) {
        when(connection.isReadOnly()).thenReturn(true);
        when(session.isConnected()).thenReturn(true);
      }
    }
  }

  private void assertTransactionInvocations(TransactionDefinition definition)
      throws SQLException {
    if (definition.isReadonly()) {
      verify(connection, times(1)).setReadOnly(true);
      verify(connection, times(1)).setReadOnly(false);
    }
    verify(transaction, times(1)).commit();
    verify(transaction, times(1)).begin();
    verify(entityManager, times(1)).close();
  }


  private void assertUnsuccessfulTransactionInvocations() {
    verify(transaction, times(3)).begin();
    verify(transaction, times(2)).rollback();
    verify(entityManager, times(2)).close();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUnsuccessfulExecutionWithTransaction()
      throws SQLException {
    TransactionDefinition definition = TransactionDefinition.builder()
        .isolation(IsolationLevel.SERIALIZABLE).build();
    setupTransactionMocks(definition);
    when(transaction.isActive()).thenReturn(true);
    DefaultExecution execution = new DefaultExecution(definition,
        factory);
    assertThrows(RuntimeException.class, () -> execution.execute(entityManager -> {
      throw new RuntimeException();
    }));
    when(transaction.isActive()).thenReturn(false);
    assertThrows(RuntimeException.class, () -> execution.execute(entityManager -> {
      throw new RuntimeException();
    }));

    doThrow(RuntimeException.class).when(transaction).rollback();
    when(transaction.isActive()).thenReturn(true);
    assertThrows(RuntimeException.class, () -> execution.execute(entityManager -> {
      throw new RuntimeException();
    }));

    assertUnsuccessfulTransactionInvocations();

  }

  @Test
  public void testUnsuccessfulExecutionWithoutTransaction() {
    DefaultExecution<Integer> execution = new DefaultExecution<>(null,
        factory);
    assertThrows(RuntimeException.class, () -> execution.execute(entityManager -> {
      throw new RuntimeException();
    }));

  }

}
