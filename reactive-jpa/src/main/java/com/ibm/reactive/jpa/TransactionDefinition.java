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
public class TransactionDefinition {

  public static final int TIMEOUT_DEFAULT = 30;

  private boolean isReadonly;
  @Builder.Default
  private IsolationLevel isolation = IsolationLevel.READ_UNCOMMITTED;
  @Builder.Default
  private int timeout = TIMEOUT_DEFAULT;

}
