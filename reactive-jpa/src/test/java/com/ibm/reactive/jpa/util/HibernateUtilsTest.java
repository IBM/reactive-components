/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ibm.reactive.jpa.PoolConfiguration;
import com.ibm.reactive.jpa.TransactionDefinition;
import com.ibm.reactive.jpa.lombok.LombokTestUtil;
import java.util.HashMap;
import java.util.stream.Stream;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
public class HibernateUtilsTest {

  @Mock
  private Session session;

  @Mock
  private MetadataSources sources;


  final SessionFactory factory = getSessionFactory();

  @Test
  public void testLombokNullValidations() {
    LombokTestUtil.testLombokNullValidations(Stream.of(
        () -> HibernateUtils.createServiceRegistry(null, null),
        () -> HibernateUtils.createServiceRegistry(new HashMap<>(), null),
        () -> HibernateUtils.getSessionFactory(null),
        () -> HibernateUtils.setFlushMode(null, null),
        () -> HibernateUtils.setFlushMode(session, null),
        () -> HibernateUtils.resetFlushMode(null, null)
    ));

  }

  @Test
  public void testPoolConfiguration() {
    PoolConfiguration defaultConfiguration = PoolConfiguration.defaultConfiguration();
    PoolConfiguration configuration = PoolConfiguration.builder()
        .connectionTimeout(PoolConfiguration.CONNECTION_TIMEOUT)
        .idleTimeout(PoolConfiguration.IDLE_TIMEOUT)
        .maxPoolSize(PoolConfiguration.MAX_POOL_SIZE)
        .minPoolSize(PoolConfiguration.MIN_POOL_SIZE)
        .build();
    assertEquals(defaultConfiguration, configuration);
  }

  @Test
  public void testHikariSettings() {
    HashMap<String, String> settings = new HashMap<>();
    settings.put("a", "b");
    settings.put("hibernate.hikari.minimumIdle", "1000");
    PoolConfiguration configuration = PoolConfiguration.builder().maxPoolSize(99).build();
    HibernateUtils.addHikariSettings(settings, configuration);

    // Four attributes are added if they do not exist, in this scenario one already exist before
    assertEquals(settings.size(), 5);

    // skipped in the method
    assertEquals(settings.get("hibernate.hikari.minimumIdle"), "1000");

    // Validate the value sent in configuration is used
    assertEquals(settings.get("hibernate.hikari.maximumPoolSize"),
        String.valueOf(configuration.getMaxPoolSize()));
  }

  private static StandardServiceRegistry getServiceRegistry() {
    HashMap<String, String> settings = new HashMap<>();
    settings.put("hibernate.dialect", "org.hibernate.dialect.DB2Dialect");
    settings.put("hibernate.connection.url", "jdbc:hsqldb:mem:test");
    return HibernateUtils
        .createServiceRegistry(settings, PoolConfiguration.defaultConfiguration());
  }

  @Test
  public void testServiceRegistryCreation() {
    StandardServiceRegistry serviceRegistry = getServiceRegistry();
    assertNotNull(serviceRegistry);
  }

  private static SessionFactory getSessionFactory() {
    StandardServiceRegistry serviceRegistry = getServiceRegistry();
    MetadataSources sources = new MetadataSources(serviceRegistry);
    return HibernateUtils.getSessionFactory(sources);
  }

  @Test
  public void testSessionFactory() {
    assertNotNull(getSessionFactory());
  }

  @Test
  public void testSessionFactoryExceptionally() {
    StandardServiceRegistry serviceRegistry = getServiceRegistry();
    when(sources.getMetadataBuilder()).thenThrow(new RuntimeException());
    assertThrows(RuntimeException.class, () -> HibernateUtils.getSessionFactory(sources));
    assertTrue(((StandardServiceRegistryImpl) serviceRegistry).isActive());

    when(sources.getServiceRegistry()).thenReturn(serviceRegistry);
    assertThrows(RuntimeException.class, () -> HibernateUtils.getSessionFactory(sources));
    assertFalse(((StandardServiceRegistryImpl) serviceRegistry).isActive());

  }

  private static Stream<Arguments> flushModeArguments() {
    return Stream.of(
        Arguments
            .of(FlushMode.MANUAL, TransactionDefinition.builder().isReadonly(true).build(), null,
                FlushMode.MANUAL),
        Arguments.of(FlushMode.AUTO, TransactionDefinition.builder().isReadonly(true).build(),
            FlushMode.AUTO, FlushMode.MANUAL),
        Arguments.of(FlushMode.MANUAL, TransactionDefinition.builder().build(), FlushMode.MANUAL,
            FlushMode.AUTO)
    );
  }

  @ParameterizedTest
  @MethodSource("flushModeArguments")
  public void testFlushMode(FlushMode previous, TransactionDefinition definition,
      FlushMode returned, FlushMode settled) {
    Session session = factory.openSession();
    session.setHibernateFlushMode(previous);
    FlushMode result = HibernateUtils.setFlushMode(session, definition);
    assertEquals(result, returned);
    assertEquals(session.getHibernateFlushMode(), settled);
  }

  @Test
  public void testResetFlushMode() {
    Session session = factory.openSession();
    HibernateUtils.resetFlushMode(session, FlushMode.ALWAYS);
    assertEquals(FlushMode.ALWAYS, session.getHibernateFlushMode());
  }
}
