package com.mengdx.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * zookeeper分布式锁
 * @Author Mengdx
 * @Date 2022/07/13
 **/
@Slf4j
public class ZkLock implements AutoCloseable, Watcher {

    private ZooKeeper zooKeeper;
    private String znode;

    public ZkLock () throws IOException {
        this.zooKeeper = new ZooKeeper("localhost:2181", 10000, this);
    }

    public boolean getLock(String businessCode) {
        try {
            //创建这个目录
            Stat stat = zooKeeper.exists("/" + businessCode, false);
            if (stat == null) {
                zooKeeper.create("/" + businessCode, businessCode.getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
            //创建顺时子节点目录
            znode = zooKeeper.create("/" + businessCode + "/" + businessCode + "_", businessCode.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);
            List<String> childrenNodes = zooKeeper.getChildren("/" + businessCode, false);
            Collections.sort(childrenNodes);
            String firstNode = childrenNodes.get(0);
            //正好是第一个节点
            if (znode.endsWith(firstNode)) {
                log.info("第一个节点获得zookeeper分布式锁");
                return true;
            }
            //如果不是第一个节点
            String lastNode = firstNode;
            for (String node : childrenNodes) {
                //消除的是上一个节点
                if (znode.endsWith(node)) {
                    zooKeeper.exists("/" + businessCode + "/" + lastNode, true);
                    break;
                } else {
                    lastNode = node;
                }
            }
            //进行等待，等待删除节点
            synchronized(this) {
                wait();
            }
            log.info("等待之后获得zookeeper分布式锁");
            return true;
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        log.info("关闭分布式锁，删除锁的顺势有序节点");
        zooKeeper.delete(znode, -1);
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getType() == Event.EventType.NodeDeleted) {
            synchronized(this) {
                notify();
            }
        }
    }

}
