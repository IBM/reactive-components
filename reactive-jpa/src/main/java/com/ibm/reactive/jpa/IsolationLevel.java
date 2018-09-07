/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa;

import java.sql.Connection;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IsolationLevel {
  READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
  READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
  REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
  SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE),
  DEFAULT(-1);

  private int isolation;

  public static IsolationLevel of(int isolation) {
    switch (isolation) {
      case Connection.TRANSACTION_READ_UNCOMMITTED:
        return READ_UNCOMMITTED;
      case Connection.TRANSACTION_READ_COMMITTED:
        return READ_COMMITTED;
      case Connection.TRANSACTION_REPEATABLE_READ:
        return REPEATABLE_READ;
      case Connection.TRANSACTION_SERIALIZABLE:
        return SERIALIZABLE;
      default:
        return DEFAULT;
    }
  }

}