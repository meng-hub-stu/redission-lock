package com.mengdx.cache;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Mengdl
 * @date 2022/06/20
 */
@Data
@AllArgsConstructor
public class CascadeKey {
    /**
     * 缓存名称
     */
    private String cacheName;
    /**
     * keys
     */
    private String[] keys;

    public CascadeKey(Map<String, Object> params) {
        this.cacheName = null != params.get("cacheName") ? params.get("cacheName").toString() : null;
        Object keys = params.get("keys");
        if (null != keys) {
            this.keys = keys instanceof List ? ((List<String>) keys).toArray(new String[0]) : (String[]) keys;
        }
    }

    @Override
    public String toString() {
        return cacheName + "：" + Arrays.stream(keys).collect(Collectors.joining("，"));
    }

}
