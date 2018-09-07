/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PoolConfiguration {

  public static final int CONNECTION_TIMEOUT = 20_000;
  public static final int MIN_POOL_SIZE = 5;
  public static final int MAX_POOL_SIZE = 5;
  public static final int IDLE_TIMEOUT = 30_000;

  private static final PoolConfiguration DEFAULT_CONFIG = new PoolConfiguration(CONNECTION_TIMEOUT,
      IDLE_TIMEOUT, MAX_POOL_SIZE, MIN_POOL_SIZE);

  private int connectionTimeout;
  private int idleTimeout;
  private int maxPoolSize;
  private int minPoolSize;


  public static PoolConfiguration defaultConfiguration() {
    return DEFAULT_CONFIG;
  }


}
