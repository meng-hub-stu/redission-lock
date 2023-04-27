package com.mengdx.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.TimeUnit;

/**
 * curator分布式锁
 * @author Mengdl
 * @date 2022/07/14
 */
@Slf4j
public class CuratorLock implements AutoCloseable{

    private RetryPolicy retryPolicy;

    private CuratorFramework client;

    private InterProcessMutex lock;

    public CuratorLock () {
        this.retryPolicy = new ExponentialBackoffRetry(1000, 3);
        this.client = client = CuratorFrameworkFactory.newClient("localhost:2181", this.retryPolicy);
    }

    public boolean getLock(Long maxWait, String lockPath) {
        lock = new InterProcessMutex(this.client, lockPath);
        try {
            if (lock.acquire(maxWait, TimeUnit.SECONDS)){
                log.info("获得curatoer的分布式锁");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        log.info("关闭curator分布式锁");
        lock.release();
    }

}
