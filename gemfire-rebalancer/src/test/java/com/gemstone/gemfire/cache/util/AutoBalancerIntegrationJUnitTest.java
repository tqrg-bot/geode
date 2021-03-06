package com.gemstone.gemfire.cache.util;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.GemFireConfigException;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.util.AutoBalancer.CacheOperationFacade;
import com.gemstone.gemfire.cache.util.AutoBalancer.GeodeCacheFacade;
import com.gemstone.gemfire.distributed.DistributedLockService;
import com.gemstone.gemfire.distributed.internal.InternalDistributedSystem;
import com.gemstone.gemfire.distributed.internal.locks.DLockService;
import com.gemstone.gemfire.internal.HostStatSampler;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

/**
 * IntegrationTests for AutoBalancer that include usage of Cache, StatSampler
 * and DistributedLockService.
 */
@Category(IntegrationTest.class)
public class AutoBalancerIntegrationJUnitTest {

  private static final int TIMEOUT_SECONDS = 5;

  private GemFireCacheImpl cache;

  @Before
  public void setUpCacheAndDLS() {
    cache = createBasicCache();
  }

  @After
  public void destroyCacheAndDLS() {
    if (DLockService.getServiceNamed(AutoBalancer.AUTO_BALANCER_LOCK_SERVICE_NAME) != null) {
      DLockService.destroy(AutoBalancer.AUTO_BALANCER_LOCK_SERVICE_NAME);
    }

    if (cache != null && !cache.isClosed()) {
      try {
        final HostStatSampler statSampler = ((InternalDistributedSystem) cache.getDistributedSystem()).getStatSampler();
        cache.close();
        // wait for the stat sampler to stand down
        await().atMost(TIMEOUT_SECONDS, SECONDS).until(isAlive(statSampler), equalTo(false));
      } finally {
        cache = null;
      }
    }
  }

  @Test
  public void testAutoRebalaceStatsOnLockSuccess() throws InterruptedException {
    assertEquals(0, cache.getResourceManager().getStats().getAutoRebalanceAttempts());
    AutoBalancer balancer = new AutoBalancer();
    balancer.getOOBAuditor().execute();
    assertEquals(1, cache.getResourceManager().getStats().getAutoRebalanceAttempts());
  }

  @Test
  public void testAutoRebalaceStatsOnLockFailure() throws InterruptedException {
    acquireLockInDifferentThread(1);
    assertEquals(0, cache.getResourceManager().getStats().getAutoRebalanceAttempts());
    AutoBalancer balancer = new AutoBalancer();
    balancer.getOOBAuditor().execute();
    assertEquals(0, cache.getResourceManager().getStats().getAutoRebalanceAttempts());
  }

  @Test
  public void testAutoBalanceStatUpdate() {
    assertEquals(0, cache.getResourceManager().getStats().getAutoRebalanceAttempts());
    new GeodeCacheFacade().incrementAttemptCounter();
    assertEquals(1, cache.getResourceManager().getStats().getAutoRebalanceAttempts());
  }

  @Test
  public void testLockSuccess() throws InterruptedException {
    acquireLockInDifferentThread(1);
    DistributedLockService dls = new GeodeCacheFacade().getDLS();
    assertFalse(dls.lock(AutoBalancer.AUTO_BALANCER_LOCK, 0, -1));
  }

  @Test
  public void canReacquireLock() throws InterruptedException {
    acquireLockInDifferentThread(2);
    DistributedLockService dls = new GeodeCacheFacade().getDLS();
    assertFalse(dls.lock(AutoBalancer.AUTO_BALANCER_LOCK, 0, -1));
  }

  @Test
  public void testLockAlreadyTakenElsewhere() throws InterruptedException {
    DistributedLockService dls = new GeodeCacheFacade().getDLS();
    assertTrue(dls.lock(AutoBalancer.AUTO_BALANCER_LOCK, 0, -1));

    final AtomicBoolean success = new AtomicBoolean(true);

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        CacheOperationFacade cacheFacade = new GeodeCacheFacade();
        success.set(cacheFacade.acquireAutoBalanceLock());
      }
    });
    thread.start();
    thread.join();

    assertFalse(success.get());
  }

  @Test
  public void testInitializerCacheXML() {
    String configStr = "<cache xmlns=\"http://schema.pivotal.io/gemfire/cache\"                          "
        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"                                      "
        + " xsi:schemaLocation=\"http://schema.pivotal.io/gemfire/cache http://schema.pivotal.io/gemfire/cache/cache-9.0.xsd\""
        + " version=\"9.0\">                                                                             "
        + "   <initializer>                                                                              "
        + "     <class-name>com.gemstone.gemfire.cache.util.AutoBalancer</class-name>                    "
        + "     <parameter name=\"schedule\">                                                            "
        + "       <string>* * * * * ? </string>                                                          "
        + "     </parameter>                                                                             "
        + "   </initializer>                                                                             "
        + " </cache>";

    cache.loadCacheXml(new ByteArrayInputStream(configStr.getBytes()));
  }

  @Test(expected = GemFireConfigException.class)
  public void testInitFailOnMissingScheduleConf() {
    String configStr = "<cache xmlns=\"http://schema.pivotal.io/gemfire/cache\"                          "
        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"                                      "
        + " xsi:schemaLocation=\"http://schema.pivotal.io/gemfire/cache http://schema.pivotal.io/gemfire/cache/cache-9.0.xsd\""
        + " version=\"9.0\">                                                                             "
        + "   <initializer>                                                                              "
        + "     <class-name>com.gemstone.gemfire.cache.util.AutoBalancer</class-name>                    "
        + "   </initializer>                                                                             "
        + " </cache>";

    cache.loadCacheXml(new ByteArrayInputStream(configStr.getBytes()));
  }

  private GemFireCacheImpl createBasicCache() {
    return (GemFireCacheImpl) new CacheFactory().set("mcast-port", "0").create();
  }

  private Callable<Boolean> isAlive(final HostStatSampler statSampler) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return statSampler.isAlive();
      }
    };
  }

  private void acquireLockInDifferentThread(final int num) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(num);
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        CacheOperationFacade cacheFacade = new GeodeCacheFacade();
        for (int i = 0; i < num; i++) {
          boolean result = cacheFacade.acquireAutoBalanceLock();
          if (result) {
            latch.countDown();
          }
        }
      }
    });
    thread.start();
    assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
  }
}
