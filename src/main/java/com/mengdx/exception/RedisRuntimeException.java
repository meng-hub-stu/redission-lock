package com.mengdx.exception;

/**
 * @author Mengdl
 * @date 2022/06/16
 */
public class RedisRuntimeException extends RuntimeException{

    public RedisRuntimeException(){
        super();
    }

    public RedisRuntimeException(String msg){
        super(msg);
    }

    public RedisRuntimeException(Throwable cause) {
        super(cause);
    }

    public RedisRuntimeException(String msg, Throwable cause) {
        super(cause);
    }

}
