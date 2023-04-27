package com.mengdx.utils;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

/**
 * 切面工具类
 *
 * @author Mengdl
 * @date 2020/7/27 2:38 下午
 */
@Slf4j
public class AspectUtil {

	/**
	 * 文件base64字符串长度（估算的5000,字符串长度超出5000就不打印入参日志）
	 * 2022.5.19,改成10000
	 */
	private static final int BASE_64_LENGTH = 10000;

	/**
	 * 入参字符串超长、转成该提示语句
	 */
	private static final String OVER_LENGTH_STR = "该参数字符串超出 " + BASE_64_LENGTH + " 长度, 转换成当前语句, 防止日志刷屏";

	public static String getParams(JoinPoint joinPoint) {
		Object[] args = joinPoint.getArgs();
		if (ArrayUtil.isEmpty(args)) {
			return null;
		}
		try {
			Map<String, Object> paramsMap = getParamsMap(joinPoint, args);
			if (MapUtil.isNotEmpty(paramsMap)) {
				return JSONUtil.toJsonStr(paramsMap);
			}
			// 获取参数
			StringBuilder paramsBuilder = new StringBuilder("{ ");
			Arrays.stream(args).forEach(arg -> {
				if (arg != null) {
					if (arg instanceof MultipartFile) {
						arg = ((MultipartFile) arg).getOriginalFilename();
					} else if (!ObjectUtil.isBasicType(arg)) {
						arg = JSONUtil.toJsonStr(arg);
						if (arg != null && StrUtil.length((CharSequence) arg) > BASE_64_LENGTH) {
							// 文件base64忽略
							arg = OVER_LENGTH_STR;
						}
					}
					paramsBuilder.append(arg).append(" ");
				}
			});
			return paramsBuilder + "} ";

		} catch (Throwable e) {
			log.warn("日志切面参数处理异常: {}, 参数为: {}", e.getMessage(), args);
			return "此条请求参数处理异常" + e.getMessage();
		}
	}

	/**
	 * 获取参数列表Map
	 *
	 * @param joinPoint : 切面对象
	 * @param args      : 参数值数组
	 * @return 参数列表Map JSONStr
	 */
	private static Map<String, Object> getParamsMap(JoinPoint joinPoint, Object[] args) {
		Map<String, Object> paramMap = MapUtil.newHashMap();
		Signature signature = joinPoint.getSignature();
		if (signature instanceof MethodSignature) {
			MethodSignature methodSignature = (MethodSignature) signature;
			String[] parameterNames = methodSignature.getParameterNames();
			if (ArrayUtil.isNotEmpty(parameterNames)) {
				for (int i = 0; i < parameterNames.length; i++) {
					if (args[i] instanceof HttpServletResponse) {
						continue;
					}
					if (StrUtil.length(JSONUtil.toJsonStr(args[i])) > BASE_64_LENGTH) {
						// 文件base64忽略
						args[i] = OVER_LENGTH_STR;
					}
					if (args[i] instanceof MultipartFile) {
						args[i] = ((MultipartFile) args[i]).getOriginalFilename();
					}
					paramMap.put(parameterNames[i], args[i]);
				}
			}
		}
		return paramMap;
	}
}
