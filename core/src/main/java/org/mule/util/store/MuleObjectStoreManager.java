/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.store;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.api.store.PartitionableExpirableObjectStore;
import org.mule.api.store.PartitionableObjectStore;
import org.mule.util.concurrent.DaemonThreadFactory;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleObjectStoreManager
    implements ObjectStoreManager, MuleContextAware, Initialisable, Disposable
{
    MuleContext muleContext;
    ConcurrentMap<String, ObjectStore<?>> stores = new ConcurrentHashMap<String, ObjectStore<?>>();
    protected ScheduledThreadPoolExecutor scheduler;
    private static Log logger = LogFactory.getLog(MuleObjectStoreManager.class);

    @Override
    public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name)
    {
        return this.<T> getObjectStore(name, false);
    }

    @Override
    public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name, boolean isPersistent)
    {
        return this.<T> getObjectStore(name, isPersistent, 0, 0, 0);
    }
    
    @SuppressWarnings({"unchecked"})
    synchronized public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name,
                                                                                         boolean isPersistent,
                                                                                         int maxEntries,
                                                                                         int entryTTL,
                                                                                         int expirationInterval)
    {
        if (stores.containsKey(name))
        {
            return (T) stores.get(name);
        }
        ListableObjectStore<?> baseStore;
        if (isPersistent)
        {
            baseStore = (ListableObjectStore<?>) muleContext.getRegistry().lookupObject(
                MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME);
        }
        else
        {
            baseStore = (ListableObjectStore<?>) muleContext.getRegistry().lookupObject(
                MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME);
        }
        T store=this.<T>getPartitionFromBaseObjectStore(baseStore,name);

        if (maxEntries == 0)
        {
            return putInStoreMap(name,store);
        }
        else
        {
            return getMonitorablePartition(name,baseStore,store,entryTTL,maxEntries,expirationInterval);
        }
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T extends ObjectStore<? extends Serializable>> T getPartitionFromBaseObjectStore(ListableObjectStore baseStore, String partitionName)
    {
        if (baseStore instanceof PartitionableObjectStore)
        {
            return (T) new ObjectStorePartition(partitionName, (PartitionableObjectStore) baseStore);
        }
        else
        {
            return (T) new PartitionedObjectStoreWrapper(partitionName, muleContext, (ListableObjectStore) baseStore);
        }
    }
    
    private <T extends ObjectStore<? extends Serializable>> T putInStoreMap(String name,T store)
    {
        @SuppressWarnings("unchecked")
        T previous = (T) stores.putIfAbsent(name, store);
        if (previous == null)
        {
            return (T) store;
        }
        else
        {
            return previous;
        }
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T extends ObjectStore<? extends Serializable>> T getMonitorablePartition(String name,ListableObjectStore baseStore,T store, int entryTTL, int maxEntries, int expirationInterval)
    {
        if (baseStore instanceof PartitionableExpirableObjectStore)
        {
            T previous = (T) stores.putIfAbsent(name, store);
            if (previous == null)
            {
                Monitor m = new Monitor(name, (PartitionableExpirableObjectStore) baseStore, entryTTL,
                    maxEntries);
                scheduler.scheduleWithFixedDelay(m, 0, expirationInterval, TimeUnit.MILLISECONDS);
                return (T) store;
            }
            else
            {
                return previous;
            }
        }
        else
        {
            MonitoredObjectStoreWrapper monObjectStore;
            // Using synchronization here in order to avoid initialising the
            // monitored object store wrapper for nothing and having to dispose
            // or putting an uninitialised ObjectStore
            synchronized (this)
            {
                if (stores.containsKey(name))
                {
                    return (T) stores.get(name);
                }
                monObjectStore = new MonitoredObjectStoreWrapper((ListableObjectStore) store, maxEntries,
                    entryTTL, expirationInterval);
                try
                {
                    monObjectStore.initialise();
                }
                catch (InitialisationException e)
                {
                    throw new MuleRuntimeException(e);
                }
                stores.put(name, monObjectStore);
            }
            return (T) monObjectStore;
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void clearStoreCache()
    {
        stores.clear();
    }

    @Override
    public void dispose()
    {
        scheduler.shutdown();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.setThreadFactory(new DaemonThreadFactory("ObjectStoreManager-Monitor", this.getClass()
            .getClassLoader()));
    }

    class Monitor implements Runnable
    {
        private final String partitionName;
        private final PartitionableExpirableObjectStore<? extends Serializable> store;
        private final int entryTTL;
        private final int maxEntries;

        public Monitor(String partitionName,
                       PartitionableExpirableObjectStore<? extends Serializable> store,
                       int entryTTL,
                       int maxEntries)
        {
            this.partitionName = partitionName;
            this.store = store;
            this.entryTTL = entryTTL;
            this.maxEntries = maxEntries;
        }

        @Override
        public void run()
        {
            try
            {
                store.expire(entryTTL, maxEntries, partitionName);
            }
            catch (Exception e)
            {
                MuleObjectStoreManager.logger.warn("Running expirty on partition " + partitionName + " of "
                                                   + store + " threw " + e + ":" + e.getMessage());
            }
        }

    }

    @Override
    public void disposeStore(ObjectStore<? extends Serializable> store) throws ObjectStoreException
    {
        if(store instanceof ObjectStorePartition)
        {
            ObjectStorePartition partition=(ObjectStorePartition)store;
            partition.getBaseStore().disposePartition(partition.getPartitionName());
        }else
        {
            if(store instanceof ListableObjectStore)
            {
                ListableObjectStore listableStore=(ListableObjectStore)store;
                while(true)
                {
                    List<Serializable> keys=listableStore.allKeys();
                    if(keys.size()==0)
                    {
                        break;
                    }else
                    {
                        for(Serializable key:keys)
                        {
                            listableStore.remove(key);
                        }
                    }
                }
            }else
            {
                //there is nothing we can do                
            }
        }
    }
}
