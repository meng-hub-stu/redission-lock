package com.mengdx.entity.enums;

/**
 * 锁的模式
 * @author Mengdl
 * @date 2022/06/16
 */
public enum LockModel {
    /**
     * 可重入锁
     */
    _REENTRANT,
    /**
     * 公平锁
     */
    _FAIR,
    /**
     * 联锁
     */
    _MULTIPLE,
    /**
     * 红锁
     */
    _RED_LOCK,
    /**
     * 读锁
     */
    _READ,
    /**
     * 写锁
     */
    _WRITE;

}
