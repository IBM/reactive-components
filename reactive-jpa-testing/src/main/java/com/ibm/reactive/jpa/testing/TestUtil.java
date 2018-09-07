/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.testing;

import com.ibm.reactive.jpa.Database;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestUtil {

  private static final ConcurrentHashMap<List<String>, Database> databasesCache =
      new ConcurrentHashMap<>();

  private TestUtil() {
  }


  public static Database getDatabase(String resourcesPackage) {
    return getDatabase(Collections.singletonList(resourcesPackage));
  }

  public static Database getDatabase(List<String> resourcesPackages) {
    Database database = databasesCache.get(resourcesPackages);
    if (database == null) {
      synchronized (TestUtil.class) {
        database = databasesCache.get(resourcesPackages);
        if (database != null) {
          return database;
        }

        Map<String, String> settings = new HashMap<>();
        settings.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        settings.put("hibernate.connection.url",
            "jdbc:hsqldb:mem:temp;readonly=true;sql.syntax_db2=true");
        settings.put("hibernate.connection.username", "sa");
        settings.put("hibernate.show_sql", "true");
        settings.put("hibernate.format_sql", "true");
        settings.put("hibernate.hbm2ddl.auto", "create");

        settings.put("hibernate.max_fetch_depth", "5");

        settings.put("hibernate.cache.region_prefix", "hibernate.test");
        settings.put("hibernate.cache.region.factory_class",
            "org.hibernate.testing.cache.CachingRegionFactory");

        settings.put("hibernate.session.events.log", "true");

        database = new Database(settings, resourcesPackages);
        databasesCache.put(resourcesPackages, database);
      }
    }

    return database;
  }

  public static void removeDatabase(String resourcesPackage) {
    synchronized (databasesCache) {
      databasesCache.remove(Collections.singletonList(resourcesPackage));
    }
  }

}
