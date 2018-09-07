/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.ibm.reactive.jpa.implementation.DefaultExecution;
import com.ibm.reactive.jpa.implementation.DefaultStreamer;
import com.ibm.reactive.jpa.implementation.DefaultStreamerTest;
import com.ibm.reactive.jpa.lombok.LombokTestUtil;
import com.ibm.reactive.jpa.resources.Person;
import com.ibm.reactive.jpa.util.ReactiveUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.internal.SessionImpl;
import org.hibernate.internal.StatelessSessionImpl;
import org.hibernate.query.spi.QueryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith({MockitoExtension.class})
public class DatabaseTest {

  @Mock
  private SessionFactory sessionFactory;

  @Mock
  EntityManager entityManager;

  @Mock
  SessionImpl session;

  @Mock
  StatelessSessionImpl statelessSession;

  @Mock
  QueryImplementor<Person> query;

  @Mock
  ScrollableResults results;

  @Mock
  Connection connection;

  @Mock
  Transaction transaction;

  @Test
  public void testLombokNullValidations() {
    ExecutorService service = Executors.newFixedThreadPool(1);
    TransactionDefinition definition = TransactionDefinition.builder().build();
    Database database = new Database(sessionFactory, 1);
    LombokTestUtil.testLombokNullValidations(Stream.of(
        () -> ReactiveUtils.execute(null, null, null),
        () -> ReactiveUtils.execute(service, null, null),
        () -> ReactiveUtils.execute(service,
            new DefaultExecution<>(definition, sessionFactory), null),
        () -> ReactiveUtils.stream(null, null),
        () -> ReactiveUtils.stream(service, null),
        () -> database.stream("", Person.class).addParameter(null, null),
        () -> database.stream("", Person.class).addParameters(null)
    ));

  }

  @Test
  public void testDatabaseDefaultConfig() {
    Database database = createDatabase(null);
    assertNotNull(database);
    assertEquals(((ThreadPoolExecutor) database.getService()).getMaximumPoolSize(), 5);
    assertNotNull(database.getSessionFactory());
  }

  private Database createDatabase(PoolConfiguration poolConfiguration) {
    HashMap<String, String> settings = new HashMap<>();
    settings.put("hibernate.dialect", "org.hibernate.dialect.DB2Dialect");
    settings.put("hibernate.connection.url", "jdbc:hsqldb:mem:test");

    List<String> packages = new ArrayList<>(1);
    packages.add("com.ibm.sets.es.esa.reactive.jpa.resources");
    if (poolConfiguration == null) {
      return new Database(settings, packages);
    }
    return new Database(settings, packages, poolConfiguration);
  }

  @Test
  public void testDatabasePoolConfiguration() {
    Database database = createDatabase(PoolConfiguration.builder().maxPoolSize(10).build());
    assertNotNull(database);
    assertEquals(((ThreadPoolExecutor) database.getService()).getMaximumPoolSize(), 10);
  }

  @Test
  public void testDatabaseWithSessionFactory() {
    Database database = new Database(sessionFactory, 1);
    assertNotNull(database);
    assertEquals(((ThreadPoolExecutor) database.getService()).getMaximumPoolSize(), 1);
  }

  private void setupTransactionMocks(TransactionDefinition definition) throws SQLException {
    if (definition != null) {
      when(sessionFactory.createEntityManager()).thenReturn(entityManager);
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

  @Test
  public void testMonoExecution() throws SQLException {

    TransactionDefinition definition = TransactionDefinition.builder()
        .timeout(10)
        .isolation(IsolationLevel.SERIALIZABLE)
        .isReadonly(true)
        .build();

    setupTransactionMocks(definition);

    Database database = new Database(sessionFactory, 1);

    StepVerifier.create(database.execute(entityManager -> 1).mono())
        .expectNext(1)
        .verifyComplete();

    StepVerifier.create(database.execute(entityManager -> 1)
        .transaction(definition)
        .mono())
        .expectNext(1)
        .verifyComplete();
  }

  @Test
  public void testFluxExecution() throws SQLException {

    TransactionDefinition definition = TransactionDefinition.builder()
        .timeout(10)
        .isolation(IsolationLevel.SERIALIZABLE)
        .isReadonly(true)
        .build();

    setupTransactionMocks(definition);

    List<String> list = new ArrayList<>(3);
    list.add("a");
    list.add("b");
    list.add("c");

    Database database = new Database(sessionFactory, 1);

    StepVerifier.create(database.execute(entityManager -> list).flux())
        .expectNextSequence(list)
        .verifyComplete();

    StepVerifier.create(database.execute(entityManager -> 1)
        .transaction(definition)
        .flux())
        .expectNext(1)
        .verifyComplete();
  }

  @Test
  public void testExceptionallyExecution() throws SQLException {

    Database database = new Database(sessionFactory, 1);

    StepVerifier.create(database.execute(entityManager -> {
      throw new RuntimeException();
    }).mono())
        .verifyError();

  }

  @Test
  public void testStreamExecution() throws SQLException {

    setupStreamTransactionMocks(IsolationLevel.READ_COMMITTED);

    Database database = new Database(sessionFactory, 1);
    Flux<Person> stream = database
        .stream(DefaultStreamerTest.QUERY, Person.class)
        .isolationLevel(IsolationLevel.READ_COMMITTED)
        .addParameter("key", "value")
        .addParameters(Collections.emptyMap())
        .fetchSize(5)
        .flux();

    StepVerifier.withVirtualTime(() -> stream)
        .expectNext(DefaultStreamerTest.PERSON)
        .verifyComplete();

  }


  private void setupStreamTransactionMocks(IsolationLevel level) throws SQLException {
    when(sessionFactory.openStatelessSession()).thenReturn(statelessSession);
    when(statelessSession.createQuery(DefaultStreamerTest.QUERY, Person.class)).thenReturn(query);
    when(query.setFetchSize(DefaultStreamer.DEFAULT_FETCH_SIZE)).thenReturn(query);
    when(query.setReadOnly(true)).thenReturn(query);
    when(query.scroll(ScrollMode.FORWARD_ONLY)).thenReturn(results);
    when(results.next()).thenReturn(true).thenReturn(false);
    when(results.get(0)).thenReturn(DefaultStreamerTest.PERSON);
    if (level != null) {
      when(statelessSession.getTransaction()).thenReturn(transaction);
      when(statelessSession.connection()).thenReturn(connection);
      if (level != IsolationLevel.DEFAULT) {
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_NONE);
      }
    }
  }


}
