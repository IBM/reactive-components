/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.ibm.reactive.jpa.IsolationLevel;
import com.ibm.reactive.jpa.lombok.LombokTestUtil;
import com.ibm.reactive.jpa.resources.Person;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.stream.Stream;
import javax.persistence.PersistenceException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.internal.StatelessSessionImpl;
import org.hibernate.query.spi.QueryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith({MockitoExtension.class})
public class DefaultStreamerTest {

  @Mock
  SessionFactory factory;

  @Mock
  StatelessSessionImpl session;

  @Mock
  Connection connection;

  @Mock
  Transaction transaction;

  @Mock
  QueryImplementor<Person> query;

  @Mock
  ScrollableResults results;

  public static final Person PERSON = new Person(1, "Javier");
  public static final String QUERY = "Select * from Person";

  @Test
  public void testLombokNullValidations() {
    LombokTestUtil.testLombokNullValidations(Stream.of(
        () -> DefaultStreamer.builder().build(),
        () -> DefaultStreamer.builder().query("").build(),
        () -> DefaultStreamer.<Person>builder().type(Person.class).build(),
        () -> DefaultStreamer.<Person>builder().query("").type(Person.class).build(),
        () -> DefaultStreamer.builder().query("").sessionFactory(factory).build()
    ));
  }


  @Test
  public void testCreation() {
    DefaultStreamer<Person> defaultStreamer = DefaultStreamer.<Person>builder().query("")
        .type(Person.class)
        .sessionFactory(factory).build();
    assertNotNull(defaultStreamer);
    assertNotNull(defaultStreamer.getQuery());
    assertNotNull(defaultStreamer.getType());
    assertNotNull(defaultStreamer.getSessionFactory());
    assertNull(defaultStreamer.getParameters());
    assertNull(defaultStreamer.getIsolationLevel());
    assertEquals(-1, defaultStreamer.getMaxResults());
    assertEquals(defaultStreamer.getFetchSize(), DefaultStreamer.DEFAULT_FETCH_SIZE);
  }


  private static Stream<IsolationLevel> successfulExecutionArguments() {
    return Stream.of(
        IsolationLevel.DEFAULT,
        IsolationLevel.READ_COMMITTED,
        IsolationLevel.READ_UNCOMMITTED,
        IsolationLevel.SERIALIZABLE
    );
  }

  @ParameterizedTest
  @MethodSource("successfulExecutionArguments")
  public void testSuccessfulTransactionalExecution(IsolationLevel level)
      throws SQLException {
    setupTransactionMocks(level);
    DefaultStreamer<Person> defaultStreamer = DefaultStreamer.<Person>builder()
        .query(QUERY)
        .type(Person.class)
        .sessionFactory(factory)
        .isolationLevel(level)
        .fetchSize(DefaultStreamer.DEFAULT_FETCH_SIZE)
        .build();
    Flux<Person> flux = Flux.create(defaultStreamer::stream);
    StepVerifier.create(flux)
        .expectNext(PERSON)
        .expectComplete()
        .verify();
  }

  @Test
  public void testSuccessfulWithParametersExecution()
      throws SQLException {
    setupTransactionMocks(null);
    HashMap<String, Object> parameters = new HashMap<>(1);
    parameters.put("key", "value");
    DefaultStreamer<Person> defaultStreamer = DefaultStreamer.<Person>builder()
        .query(QUERY)
        .type(Person.class)
        .sessionFactory(factory)
        .maxResults(1)
        .parameters(parameters)
        .fetchSize(DefaultStreamer.DEFAULT_FETCH_SIZE)
        .build();
    Flux<Person> flux = Flux.create(defaultStreamer::stream);
    StepVerifier.create(flux)
        .expectNext(PERSON)
        .expectComplete()
        .verify();
  }

  @Test
  public void testUnsuccessfulExecution()
      throws SQLException {

    when(factory.openStatelessSession()).thenReturn(session);
    when(session.createQuery(QUERY, Person.class)).thenReturn(query);
    when(query.setFetchSize(DefaultStreamer.DEFAULT_FETCH_SIZE)).thenReturn(query);
    when(query.setReadOnly(true)).thenReturn(query);
    when(query.scroll(ScrollMode.FORWARD_ONLY)).thenReturn(results);
    when(results.next()).thenThrow(new RuntimeException());
    DefaultStreamer<Person> defaultStreamer = DefaultStreamer.<Person>builder()
        .query(QUERY)
        .type(Person.class)
        .sessionFactory(factory)
        .fetchSize(DefaultStreamer.DEFAULT_FETCH_SIZE)
        .build();
    Flux<Person> flux = Flux.create(defaultStreamer::stream);
    StepVerifier.create(flux)
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  public void testUnsuccessfulExecutionWithScrollNull() {

    when(factory.openStatelessSession()).thenReturn(session);
    when(session.createQuery(QUERY, Person.class)).thenThrow(RuntimeException.class);
    DefaultStreamer<Person> defaultStreamer = DefaultStreamer.<Person>builder()
        .query(QUERY)
        .type(Person.class)
        .sessionFactory(factory)
        .fetchSize(DefaultStreamer.DEFAULT_FETCH_SIZE)
        .build();
    Flux<Person> flux = Flux.create(defaultStreamer::stream);
    StepVerifier.create(flux)
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  public void testUnsuccessfulExecutionWithSessionNull() {
    when(factory.openStatelessSession()).thenThrow(RuntimeException.class);
    DefaultStreamer<Person> defaultStreamer = DefaultStreamer.<Person>builder()
        .query(QUERY)
        .type(Person.class)
        .sessionFactory(factory)
        .fetchSize(DefaultStreamer.DEFAULT_FETCH_SIZE)
        .build();
    Flux<Person> flux = Flux.create(defaultStreamer::stream);
    StepVerifier.create(flux)
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  public void testUnsuccessfulTransactionalExecution()
      throws SQLException {

    when(factory.openStatelessSession()).thenReturn(session);
    when(session.createQuery(QUERY, Person.class)).thenReturn(query);
    when(query.setFetchSize(DefaultStreamer.DEFAULT_FETCH_SIZE)).thenReturn(query);
    when(query.setReadOnly(true)).thenReturn(query);
    when(query.scroll(ScrollMode.FORWARD_ONLY)).thenReturn(results);
    when(results.next()).thenThrow(new RuntimeException());
    when(session.getTransaction()).thenReturn(transaction);
    when(session.connection()).thenReturn(connection);
    when(transaction.isActive()).thenReturn(true);
    when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_NONE);
    doThrow(PersistenceException.class).when(transaction).rollback();
    DefaultStreamer<Person> defaultStreamer = DefaultStreamer.<Person>builder()
        .query(QUERY)
        .type(Person.class)
        .sessionFactory(factory)
        .isolationLevel(IsolationLevel.READ_COMMITTED)
        .fetchSize(DefaultStreamer.DEFAULT_FETCH_SIZE)
        .build();
    Flux<Person> flux = Flux.create(defaultStreamer::stream);
    StepVerifier.create(flux)
        .expectError(RuntimeException.class)
        .verify();

    Mockito.reset(transaction);
    when(transaction.isActive()).thenReturn(true);
    flux = Flux.create(defaultStreamer::stream);
    StepVerifier.create(flux)
        .verifyError(RuntimeException.class);
  }

  private void setupTransactionMocks(IsolationLevel level) throws SQLException {
    when(factory.openStatelessSession()).thenReturn(session);
    when(session.createQuery(QUERY, Person.class)).thenReturn(query);
    when(query.setFetchSize(DefaultStreamer.DEFAULT_FETCH_SIZE)).thenReturn(query);
    when(query.setReadOnly(true)).thenReturn(query);
    when(query.scroll(ScrollMode.FORWARD_ONLY)).thenReturn(results);
    when(results.next()).thenReturn(true).thenReturn(false);
    when(results.get(0)).thenReturn(PERSON);
    if (level != null) {
      when(session.getTransaction()).thenReturn(transaction);
      when(session.connection()).thenReturn(connection);
      if (level != IsolationLevel.DEFAULT) {
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_NONE);
      }
    }
  }


}
