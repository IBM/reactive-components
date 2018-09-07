/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa;

import reactor.core.publisher.FluxSink;

@FunctionalInterface
public interface Streamer<T> {

  void stream(FluxSink<T> sink);

}
