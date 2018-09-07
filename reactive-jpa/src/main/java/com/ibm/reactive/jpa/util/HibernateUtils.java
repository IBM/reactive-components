/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.util;

import com.ibm.reactive.jpa.PoolConfiguration;
import com.ibm.reactive.jpa.TransactionDefinition;
import java.util.Map;
import lombok.NonNull;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public abstract class HibernateUtils {

  private HibernateUtils() {
  }

  static void addHikariSettings(Map<String, String> settings,
      PoolConfiguration configuration) {
    // Maximum waiting time for a connection from the pool
    settings.putIfAbsent("hibernate.hikari.connectionTimeout",
        String.valueOf(configuration.getConnectionTimeout()));
    // Minimum number of idle connections in the pool
    settings.putIfAbsent("hibernate.hikari.minimumIdle",
        String.valueOf(configuration.getMinPoolSize()));
    // Maximum number of actual connection in the pool
    settings.putIfAbsent("hibernate.hikari.maximumPoolSize",
        String.valueOf(configuration.getMaxPoolSize()));
    // Maximum time that a connection is allowed to sit idle in the pool
    settings.putIfAbsent("hibernate.hikari.idleTimeout",
        String.valueOf(configuration.getIdleTimeout()));
  }


  public static StandardServiceRegistry createServiceRegistry(@NonNull Map<String, String> settings,
      @NonNull PoolConfiguration configuration) {
    // HikariCP settings
    addHikariSettings(settings, configuration);

    return new StandardServiceRegistryBuilder()
        .applySettings(settings)
        .build();
  }

  public static SessionFactory getSessionFactory(@NonNull MetadataSources sources) {
    try {
      Metadata metadata = sources.getMetadataBuilder().build();
      return metadata.getSessionFactoryBuilder().build();
    } catch (Exception e) {
      if (sources.getServiceRegistry() != null) {
        StandardServiceRegistryBuilder.destroy(sources.getServiceRegistry());
      }
      throw e;
    }
  }


  public static FlushMode setFlushMode(@NonNull Session session,
      @NonNull TransactionDefinition transactionDefinition) {
    FlushMode previousFlushMode = session.getHibernateFlushMode();
    if (transactionDefinition.isReadonly()
        && needChangeFlushModeForReadOnly(previousFlushMode)) {
      session.setHibernateFlushMode(FlushMode.MANUAL);
      return previousFlushMode;
    } else if (!transactionDefinition.isReadonly() && needChangeFlushMode(previousFlushMode)) {
      session.setHibernateFlushMode(FlushMode.AUTO);
      return previousFlushMode;
    }
    return null;
  }

  public static void resetFlushMode(@NonNull Session session, FlushMode flushMode) {
    session.setHibernateFlushMode(flushMode);
  }

  private static boolean needChangeFlushModeForReadOnly(FlushMode current) {
    return !FlushMode.MANUAL.equals(current);
  }

  private static boolean needChangeFlushMode(FlushMode current) {
    return current.lessThan(FlushMode.COMMIT);
  }

}
