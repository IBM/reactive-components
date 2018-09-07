/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.implementation;

import com.ibm.reactive.jpa.Execution;
import com.ibm.reactive.jpa.IsolationLevel;
import com.ibm.reactive.jpa.TransactionDefinition;
import com.ibm.reactive.jpa.util.HibernateUtils;
import com.ibm.reactive.jpa.util.JdbcUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import javax.persistence.EntityManager;
import lombok.NonNull;
import lombok.Value;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value
public class DefaultExecution<T> implements Execution<T> {

  private static final Logger logger = LoggerFactory.getLogger(JdbcUtils.class);
  private TransactionDefinition transactionDefinition;
  private SessionFactory sessionFactory;

  @Override
  public T execute(@NonNull Function<EntityManager, T> function) throws SQLException {
    DefaultTransactionData data = null;
    EntityManager entityManager = sessionFactory.createEntityManager();
    try {
      data = checkAndBeginTransaction(entityManager);
      T response = function.apply(entityManager);
      checkAndCommit(data, entityManager);
      entityManager.close();
      return response;
    } catch (Exception e) {
      handleException(entityManager, data);
      throw e;
    }
  }

  private void handleException(EntityManager entityManager,
      DefaultTransactionData data) {
    try {
      checkAndRollback(data, entityManager);
      entityManager.close();
    } catch (Exception ex) {
      // ignore
      logger.warn("Exception thrown while rollback was called", ex);
    }
  }

  private DefaultTransactionData checkAndBeginTransaction(EntityManager entityManager)
      throws SQLException {
    DefaultTransactionData data = null;
    if (isTransactional()) {
      data = beginTransaction(entityManager);
    }
    return data;
  }

  private DefaultTransactionData beginTransaction(EntityManager entityManager) throws SQLException {
    Session session = getSession(entityManager);

    if (transactionDefinition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
      session.getTransaction().setTimeout(transactionDefinition.getTimeout());
    }

    if (transactionDefinition.isReadonly()) {
      handleReadOnly(session);
    }

    IsolationLevel previousIsolationLevel = null;
    if (transactionDefinition.getIsolation() != IsolationLevel.DEFAULT) {
      previousIsolationLevel = setIsolationLevel(session);
    }

    session.getTransaction().begin();

    FlushMode previousFlushMode = HibernateUtils.setFlushMode(session, transactionDefinition);

    return new DefaultTransactionData(previousFlushMode, previousIsolationLevel);

  }

  private IsolationLevel setIsolationLevel(Session session) throws SQLException {
    Connection connection = getConnection(session);
    return JdbcUtils.setIsolationLevel(connection, transactionDefinition.getIsolation());
  }

  private void handleReadOnly(Session session) {
    Connection connection = getConnection(session);
    JdbcUtils.setConnectionReadOnly(connection);
  }


  private boolean isTransactional() {
    return transactionDefinition != null;
  }

  private void checkAndCommit(DefaultTransactionData data, EntityManager entityManager)
      throws SQLException {
    if (isTransactional()) {
      commit(data, entityManager);
    }
  }

  private void commit(DefaultTransactionData data, EntityManager entityManager)
      throws SQLException {
    Session session = getSession(entityManager);
    session.getTransaction().commit();
    restoreTransactionValues(session, data);
  }

  private void checkAndRollback(DefaultTransactionData data, EntityManager entityManager)
      throws SQLException {
    Session session = getSession(entityManager);
    if (isTransactional() && session.getTransaction().isActive()) {
      rollback(data, entityManager);
    }
  }

  private void rollback(DefaultTransactionData data, EntityManager entityManager)
      throws SQLException {
    Session session = getSession(entityManager);
    session.getTransaction().rollback();
    restoreTransactionValues(session, data);
  }

  private void restoreTransactionValues(Session session, DefaultTransactionData data)
      throws SQLException {
    Connection connection = getConnection(session);
    if (connection != null && session.isConnected()) {
      JdbcUtils.resetTransactionValues(connection, data.getIsolationLevel());
    }

    if (data.getFlushMode() != null) {
      HibernateUtils.resetFlushMode(session, data.getFlushMode());
    }
  }

  private Session getSession(EntityManager entityManager) {
    return entityManager.unwrap(Session.class);
  }

  private Connection getConnection(Session session) {
    return ((SessionImpl) session).connection();
  }

  @Value
  private static class DefaultTransactionData {

    private FlushMode flushMode;
    private IsolationLevel isolationLevel;
  }
}
