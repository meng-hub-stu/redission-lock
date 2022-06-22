package com.mengdx.cache;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 缓存key
 * @author mengdx
 */
@Data
@AllArgsConstructor
public class CacheKey {

	private String key;
	private List<CascadeKey> cascadeKeys;

	public CacheKey(Map<String, Object> params) {
		this.key = null != params.get("key") ? params.get("key").toString() : null;
		this.cascadeKeys = null != params.get("cascadeKeys") ? (List<CascadeKey>) params.get("cascadeKeys") : null;
	}

	@Override
	public String toString() {
		return key + "，cascadeKeys：" +
			cascadeKeys.stream().map(CascadeKey::toString).collect(Collectors.joining("_"));
	}

}
