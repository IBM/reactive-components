/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa;

import java.sql.SQLException;
import java.util.function.Function;
import javax.persistence.EntityManager;

@FunctionalInterface
public interface Execution<T> {

  T execute(Function<EntityManager, T> function) throws SQLException;
}
