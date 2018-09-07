/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa;

import static com.ibm.reactive.jpa.IsolationLevel.DEFAULT;
import static com.ibm.reactive.jpa.IsolationLevel.READ_COMMITTED;
import static com.ibm.reactive.jpa.IsolationLevel.READ_UNCOMMITTED;
import static com.ibm.reactive.jpa.IsolationLevel.REPEATABLE_READ;
import static com.ibm.reactive.jpa.IsolationLevel.SERIALIZABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IsolationLevelTest {

  static Stream<Arguments> isolationLevelsWrapperArguments() {
    return Stream.of(
        Arguments.of(Connection.TRANSACTION_READ_UNCOMMITTED, READ_UNCOMMITTED,
            Connection.TRANSACTION_READ_UNCOMMITTED),
        Arguments.of(Connection.TRANSACTION_READ_COMMITTED, READ_COMMITTED,
            Connection.TRANSACTION_READ_COMMITTED),
        Arguments.of(Connection.TRANSACTION_REPEATABLE_READ, REPEATABLE_READ,
            Connection.TRANSACTION_REPEATABLE_READ),
        Arguments.of(Connection.TRANSACTION_SERIALIZABLE, SERIALIZABLE,
            Connection.TRANSACTION_SERIALIZABLE),
        Arguments.of(Connection.TRANSACTION_NONE, DEFAULT, -1),
        Arguments.of(-1, DEFAULT, -1),
        Arguments.of(Integer.MAX_VALUE, DEFAULT, -1)
    );
  }

  @ParameterizedTest
  @MethodSource("isolationLevelsWrapperArguments")
  public void testIsolationLevelWrapper(int isolationNativeValue, IsolationLevel isolationWrapper,
      int expectedIsolationNativeValue) {
    IsolationLevel wrapper = IsolationLevel.of(isolationNativeValue);
    assertEquals(wrapper, isolationWrapper);
    assertEquals(wrapper.getIsolation(), expectedIsolationNativeValue);
  }
}
