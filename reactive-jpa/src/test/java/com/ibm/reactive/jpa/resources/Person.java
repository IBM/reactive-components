/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.resources;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity(name = "PERSON")
@NoArgsConstructor
public class Person {

  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE
  )
  private long id;
  private String name;

  public Person(String name) {
    this.name = name;
  }

  public Person(long id, String name) {
    this.id = id;
    this.name = name;
  }
}
