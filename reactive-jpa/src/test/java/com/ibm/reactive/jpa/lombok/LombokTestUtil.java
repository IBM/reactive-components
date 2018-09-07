/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.lombok;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.function.Executable;

public class LombokTestUtil {

  public static final String VALIDATION_MESSAGE_EXTRACT = "is null";

  public static void testLombokNullValidations(Stream<Executable> validations) {
    testLombokNullValidations(validations, IllegalArgumentException.class);
  }

  public static void testLombokNullValidations(Stream<Executable> validations,
      Class<? extends Exception> clazz) {
    validations.forEach(executable -> {
      Exception e = assertThrows(clazz, executable);
      assertTrue(e.getMessage().contains(VALIDATION_MESSAGE_EXTRACT));
    });
  }
}
