/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.implementation;

import com.ibm.reactive.jpa.IsolationLevel;
import com.ibm.reactive.jpa.Streamer;
import com.ibm.reactive.jpa.util.JdbcUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.internal.StatelessSessionImpl;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;

@Builder
@Getter
public class DefaultStreamer<T> implements Streamer<T> {

  public static final int DEFAULT_FETCH_SIZE = 5;

  @NonNull
  private final String query;

  private Map<String, Object> parameters;

  private List<Object> parameterList;

  @NonNull
  private final Class<T> type;

  @NonNull
  private final SessionFactory sessionFactory;

  private IsolationLevel isolationLevel;

  @Builder.Default
  private int fetchSize = DEFAULT_FETCH_SIZE;

  @Builder.Default
  private int maxResults = -1;

  @Builder.Default
  private int firstResult = -1;

  private static Logger logger = LoggerFactory.getLogger(JdbcUtils.class);

  public void stream(FluxSink<T> sink) {
    StatelessSession session = null;
    ScrollableResults results = null;
    IsolationLevel previousLevel = null;
    try {
      session = openSession();
      previousLevel = startTransaction(session);
      results = createScroll(session);
      streamRows(results, sink);
      commit(session, previousLevel);
    } catch (Exception e) {
      handleException(session, previousLevel);
      sink.error(e);
    } finally {
      closeResources(session, results);
    }
  }

  private Query<T> createQuery(StatelessSession session) {
    Query<T> compiledQuery = session.createQuery(query, type);
    setParameters(compiledQuery);
    if (maxResults > 0) {
      compiledQuery.setMaxResults(maxResults);
    }

    if (firstResult >= 0) {
      compiledQuery.setFirstResult(firstResult);
    }

    return compiledQuery;
  }

  private void setParameters(Query<T> compiledQuery) {
    if (parameterList != null) {
      for (int position = 0; position < parameterList.size(); position++) {
        compiledQuery.setParameter(position + 1, parameterList.get(position));
      }
    } else if (parameters != null) {
      parameters.forEach(compiledQuery::setParameter);
    }

  }

  private Connection getConnection(StatelessSession session) {
    return ((StatelessSessionImpl) session).connection();
  }

  private StatelessSession openSession() {
    return sessionFactory.openStatelessSession();
  }

  private ScrollableResults createScroll(StatelessSession session) {
    return createQuery(session).setReadOnly(true)
        .setFetchSize(fetchSize)
        .scroll(ScrollMode.FORWARD_ONLY);
  }

  private void closeResources(StatelessSession session, ScrollableResults results) {
    if (results != null) {
      results.close();
    }
    if (session != null) {
      session.close();
    }
  }

  private void streamRows(ScrollableResults results, FluxSink<T> sink) {
    while (results.next()) {
      sink.next(type.cast(results.get(0)));
    }

    sink.complete();
  }

  private void handleException(StatelessSession session, IsolationLevel previousLevel) {
    try {
      rollback(session, previousLevel);
    } catch (Exception ex) {
      // ignore
      logger.warn("Exception thrown while rollback was called", ex);
    }
  }

  private IsolationLevel startTransaction(StatelessSession session) throws SQLException {
    if (isTransactional()) {
      Connection connection = getConnection(session);
      IsolationLevel previousIsolationLevel = JdbcUtils
          .setIsolationLevel(connection, isolationLevel);
      session.getTransaction().begin();
      return previousIsolationLevel;
    }
    return null;
  }

  private void commit(StatelessSession session, IsolationLevel previousLevel) throws SQLException {
    if (isTransactional()) {
      session.getTransaction().commit();
      resetConnection(session, previousLevel);
    }
  }

  private void rollback(StatelessSession session, IsolationLevel previousLevel)
      throws SQLException {
    if (isTransactional() && session.getTransaction().isActive()) {
      session.getTransaction().rollback();
      resetConnection(session, previousLevel);
    }
  }

  private void resetConnection(StatelessSession session, IsolationLevel previousLevel)
      throws SQLException {
    Connection connection = getConnection(session);
    JdbcUtils.resetTransactionValues(connection, previousLevel);
  }

  private boolean isTransactional() {
    return isolationLevel != null;
  }
}
