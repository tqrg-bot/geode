/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gemstone.gemfire.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Properties;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.distributed.DistributedSystem;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

/**
 * Keep calling DistributedSystem.connect over and over again
 * with a locator configured. Since the locator is not running
 * expect the connect to fail.
 * See if threads leak because of the repeated calls
 * @author Darrel Schneider
 * @since 5.0
 */
@Category(IntegrationTest.class)
public class Bug42039JUnitTest {
  
  /**
   * Keep calling DistributedSystem.connect over and over again
   * with a locator configured. Since the locator is not running
   * expect the connect to fail.
   * See if threads leak because of the repeated calls
   */
  @Test
  public void testBug42039() throws Exception {
    Properties p = new Properties();
    p.setProperty("mcast-port", "0");
    p.setProperty("locators", "localhost[6666]");
    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    for (int i=0; i < 2; i++) {
      try {
        DistributedSystem.connect(p);
        fail("expected connect to fail");
      } catch (Exception expected) {
      }
    }
    int initialThreadCount = threadBean.getThreadCount();
    for (int i=0; i < 5; i++) {
      try {
        DistributedSystem.connect(p);
        fail("expected connect to fail");
      } catch (Exception expected) {
      }
    }
    Thread.sleep(1000); // give chance for thread to exit
    int endThreadCount = threadBean.getThreadCount();
    if (endThreadCount > initialThreadCount) {
      assertEquals(initialThreadCount, endThreadCount);
    }
  }
}
