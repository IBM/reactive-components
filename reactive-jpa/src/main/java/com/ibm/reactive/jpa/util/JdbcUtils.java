/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.util;

import com.ibm.reactive.jpa.IsolationLevel;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("MagicConstant")
public abstract class JdbcUtils {

  private static final Logger logger = LoggerFactory.getLogger(JdbcUtils.class);

  private JdbcUtils() {
  }

  public static void setConnectionReadOnly(@NonNull Connection connection) {
    try {
      connection.setReadOnly(true);
    } catch (SQLException e) {
      // ignore the ex, is just a hint to the driver
      logger.warn("Connection.setReadOnly(true) fails", e);
    }
  }

  public static IsolationLevel setIsolationLevel(@NonNull Connection connection,
      @NonNull IsolationLevel level) throws SQLException {
    if (!useDriverDefaultIsolation(level)) {
      IsolationLevel previousIsolationLevel = getIsolationLevel(connection);
      if (previousIsolationLevel != level) {
        connection.setTransactionIsolation(level.getIsolation());
        return previousIsolationLevel;
      }
    }
    return null;
  }

  public static void resetTransactionValues(@NonNull Connection connection, IsolationLevel level)
      throws SQLException {
    if (level != null) {
      connection.setTransactionIsolation(level.getIsolation());
    }
    if (connection.isReadOnly()) {
      connection.setReadOnly(false);
    }
  }

  private static boolean useDriverDefaultIsolation(IsolationLevel level) {
    return level == IsolationLevel.DEFAULT;
  }

  public static IsolationLevel getIsolationLevel(@NonNull Connection connection)
      throws SQLException {
    int defaultIsolation = connection.getTransactionIsolation();
    return IsolationLevel.of(defaultIsolation);
  }
}
