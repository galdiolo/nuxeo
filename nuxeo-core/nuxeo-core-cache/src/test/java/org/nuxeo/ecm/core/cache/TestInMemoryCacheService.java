/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     mhilaire
 *
 */

package org.nuxeo.ecm.core.cache;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Maxime Hilaire
 */
@RunWith(FeaturesRunner.class)
@Features({ CacheFeature.class, InMemoryCacheFeature.class })
public class TestInMemoryCacheService {

    @Inject
    @Named(CacheFeature.DEFAULT_TEST_CACHE_NAME)
    Cache defaultCache;

    @Inject
    @Named(InMemoryCacheFeature.MAXSIZE_TEST_CACHE_NAME)
    Cache maxSizeCache;

    @Test
    public void getGuavaCache() {
        InMemoryCacheImpl guavaCache = CacheFeature.unwrapImpl(InMemoryCacheImpl.class, defaultCache);
        Assert.assertNotNull(guavaCache);
    }

    @Test
    public void maxSizeZero() throws IOException {
        maxSizeCache.put("key", "val");
        Assert.assertNull(maxSizeCache.get("key"));
    }

    @Test
    public void maxSizeExceeded() throws IOException {
        // Default test config set to 3 the maxSize, and the cache already
        // contains the key1
        defaultCache.put("key2", "val2");
        defaultCache.put("key3", "val3");

        // Value inserted afterwards will remove the first inserted (size-based
        // eviction system)
        defaultCache.put("key4", "val4");
        defaultCache.put("key5", "val5");

        // Check that new values have been stored
        Assert.assertNotNull(defaultCache.get("key4"));
        Assert.assertNotNull(defaultCache.get("key5"));

        // Check that the oldest values have been evicted
        Assert.assertNull(defaultCache.get("key1"));
        Assert.assertNull(defaultCache.get("key2"));
    }

    // protected class TestCacheConcurrency implements Runnable
    // {
    // protected boolean oneThreadDone = false;
    //
    // @Override
    // public void run() {
    //
    // Random rand = new Random();
    //
    // int id = rand.nextInt(100) + 1;
    //
    // Cache currentCache = cacheService.getCache(DEFAULT_TEST_CACHE_NAME);
    // currentCache.put("key-concurrent-"+id,"val-"+id);
    // System.out.println(id);
    // //int sec = rand.nextInt((max - min) + 1) + min;
    // //Thread.sleep((long)(Math.random() * 1000));
    // String value = (String) currentCache.get("key-concurrent-"+id);
    // synchronized (this) {
    // if(!oneThreadDone)
    // {
    // Assert.assertNotNull(value);
    // }else
    // {
    // Assert.assertNull(value);
    // }
    // }
    // }
    //
    // }
    // @Test
    // public void concurrencyExceeded() {
    // //Default config set to only one thread the concurrency level
    // ExecutorService es = Executors.newFixedThreadPool(10);
    // es.execute(new TestCacheConcurrency());
    // es.shutdown();
    // while (!es.isTerminated()) {
    // }
    //
    // }

}
