/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.util;

import static com.ibm.reactive.jpa.IsolationLevel.DEFAULT;
import static com.ibm.reactive.jpa.IsolationLevel.READ_COMMITTED;
import static com.ibm.reactive.jpa.IsolationLevel.READ_UNCOMMITTED;
import static com.ibm.reactive.jpa.IsolationLevel.REPEATABLE_READ;
import static com.ibm.reactive.jpa.IsolationLevel.SERIALIZABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibm.reactive.jpa.IsolationLevel;
import com.ibm.reactive.jpa.lombok.LombokTestUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("MagicConstant")
@ExtendWith({MockitoExtension.class})
public class JdbcUtilTest {

  @Mock
  Connection jdbcConnection;

  static Stream<Arguments> isolationLevelsArguments() {
    return Stream.of(
        Arguments.of(DEFAULT, 0, null),
        Arguments.of(READ_UNCOMMITTED, 1, DEFAULT),
        Arguments.of(READ_COMMITTED, 1, DEFAULT),
        Arguments.of(REPEATABLE_READ, 1, DEFAULT),
        Arguments.of(SERIALIZABLE, 1, DEFAULT)
    );
  }

  static Stream<Arguments> isolationLevelsResetArguments() {
    return Stream.of(
        Arguments.of(READ_UNCOMMITTED, true),
        Arguments.of(null, false),
        Arguments.of(DEFAULT, false),
        Arguments.of(null, true)
    );
  }

  @Test
  public void testConnectionReadOnly() throws SQLException {
    JdbcUtils.setConnectionReadOnly(jdbcConnection);
    verify(jdbcConnection, times(1))
        .setReadOnly(ArgumentMatchers.eq(true));
  }

  @Test
  public void testLombokNullValidations() {
    LombokTestUtil.testLombokNullValidations(Stream.of(
        () -> JdbcUtils.setConnectionReadOnly(null),
        () -> JdbcUtils.setIsolationLevel(null, null),
        () -> JdbcUtils.setIsolationLevel(jdbcConnection, null),
        () -> JdbcUtils.resetTransactionValues(null, null),
        () -> JdbcUtils.getIsolationLevel(null)
    ));

  }

  @Test
  public void testConnectionReadOnlyException() throws SQLException {
    doThrow(new SQLException()).when(jdbcConnection).setReadOnly(true);
    JdbcUtils.setConnectionReadOnly(jdbcConnection);
    verify(jdbcConnection, times(1))
        .setReadOnly(ArgumentMatchers.eq(true));
  }

  @ParameterizedTest
  @MethodSource("isolationLevelsArguments")
  public void testSettingIsolationLevel(IsolationLevel newLevel, int invokedTimes,
      IsolationLevel returnedLevel) throws SQLException {
    IsolationLevel result = JdbcUtils.setIsolationLevel(jdbcConnection, newLevel);
    verify(jdbcConnection, times(invokedTimes)).setTransactionIsolation(ArgumentMatchers.anyInt());
    assertEquals(result, returnedLevel);
  }

  @Test
  public void testSettingIsolationLevelDefault() throws SQLException {
    IsolationLevel level = READ_COMMITTED;
    when(jdbcConnection.getTransactionIsolation()).thenReturn(level.getIsolation());

    IsolationLevel result = JdbcUtils.setIsolationLevel(jdbcConnection, level);
    verify(jdbcConnection, times(0)).setTransactionIsolation(ArgumentMatchers.anyInt());
    assertNull(result);
  }

  @ParameterizedTest
  @MethodSource("isolationLevelsResetArguments")
  public void testResetConnection(IsolationLevel level,
      boolean readOnly) throws SQLException {
    when(jdbcConnection.isReadOnly()).thenReturn(readOnly);
    JdbcUtils.resetTransactionValues(jdbcConnection, level);
    verify(jdbcConnection, times(level != null ? 1 : 0)).setTransactionIsolation(
        ArgumentMatchers.anyInt());
    verify(jdbcConnection, times(readOnly ? 1 : 0)).setReadOnly(false);
  }
}
