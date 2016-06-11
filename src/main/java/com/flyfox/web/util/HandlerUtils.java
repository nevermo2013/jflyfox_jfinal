package com.flyfox.web.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.flyfox.util.DateUtils;
import com.flyfox.util.NumberUtils;
import com.flyfox.util.StrUtils;

/**
 * Bean处理
 * 
 * @author flyfox 2012.08.08
 * @email 330627517@qq.com
 */
public class HandlerUtils {

	private HandlerUtils() {
	}

	/**
	 * request赋值给PO
	 * 
	 * @param request
	 * @return 注入数据后的类
	 * @throws Exception
	 */
	public static Object handler(Map<?, ?> map, Object o) {
		try {
			if (o == null) {
				return null;
			}
			Method[] methods = o.getClass().getMethods();
			Class<?> _class = null;
			String vTmp = null;
			for (int i = 0; i < methods.length; i++) {
				Method aMethod = methods[i];
				String name = aMethod.getName();
				if (name.indexOf("set") != 0)
					continue;
				name = StrUtils.toLowerCaseFirst(name.substring(3));
				if (!map.containsKey(name)) { // Bean里有这个set方法同时request里有这个参数
					// ,那么自动注入
					continue;
				}
				_class = aMethod.getParameterTypes()[0];
				try {
					vTmp = handlerType(map, o, _class, aMethod, name);
				} catch (ClassCastException e) {
					throw new RuntimeException("类型转换错误[类名：" + o.getClass().getName() + "，字段名：" + name + "]", e);
				} catch (Exception e1) {
					throw new Exception("[" + name + "][" + vTmp + "]" + e1.getMessage());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return o;
	}

	private static String handlerType(Map<?, ?> map, Object o, Class<?> _class, Method aMethod, String name) throws Exception, IllegalAccessException,
			InvocationTargetException {
		String vTmp;
		Object obj = map.get(name);
		vTmp = obj == null ? "" : obj.toString();
		paresType(o, _class, name, vTmp, aMethod);
		return vTmp;
	}

	public static Object handler(HttpServletRequest request, Object o) {
		try {
			if (o == null) {
				return null;
			}
			Method[] methods = o.getClass().getMethods();
			Class<?> _class = null;
			Map<?, ?> map = request.getParameterMap();
			String vTmp = null;
			
			for (int i = 0; i < methods.length; i++) {
				Method aMethod = methods[i];
				String name = aMethod.getName();
				if (name.indexOf("set") != 0)
					continue;
				name = StrUtils.toLowerCaseFirst(name.substring(3));
				try {
					if (map.containsKey(name.toUpperCase())) { // Bean里有这个set方法同时request里有这个参数
						_class = aMethod.getParameterTypes()[0];
						vTmp = handlerType(request, o, _class, aMethod, name.toUpperCase());
					} else if (map.containsKey(name)) {
						_class = aMethod.getParameterTypes()[0];
						vTmp = handlerType(request, o, _class, aMethod, name);
					} else {
						continue;
					}
				} catch (ClassCastException e) {
					throw new RuntimeException("类型转换错误[类名：" + o.getClass().getName() + "，字段名：" + name + "]", e);
				} catch (Exception e1) {
					throw new Exception("[" + name + "][" + vTmp + "]" + e1.getMessage());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return o;
	}

	private static String handlerType(HttpServletRequest request, Object o, Class<?> _class, Method aMethod, String name) throws Exception,
			IllegalAccessException, InvocationTargetException {
		String vTmp;
		vTmp = StrUtils.trim(request.getParameter(name));
		paresType(o, _class, name, vTmp, aMethod);
		return vTmp;
	}

	/**
	 * 调用src对象 传递给 des对象
	 * 
	 * @param des
	 *            目标对象
	 * @param src
	 *            源对象
	 */
	public static void setByGetter(Object des, Object src) {
		Method[] ms = des.getClass().getMethods();
		Method m;
		Method g;
		String name;
		for (int i = 0; i < ms.length; i++) {
			m = ms[i];
			name = m.getName();
			// PO Index不处理
			if (name.startsWith("set") && !"setIndex".equals(name)) {
				try {
					g = src.getClass().getMethod(name.replaceFirst("s", "g"));
					if (g == null)
						continue;
				} catch (Exception e) {
					continue;
				}
				try {
					m.invoke(des, g.invoke(src));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void paresType(Object o, Class<?> _class, String name, String vTmp, Method aMethod)
			throws IllegalAccessException, InvocationTargetException {
		if ((_class == Float.class || _class == Float.TYPE)) {
			aMethod.invoke(o, new Object[] { NumberUtils.parseFloat(vTmp) });
		} else if (_class == Long.class || _class == Long.TYPE) {
			aMethod.invoke(o, new Object[] { NumberUtils.parseLong(vTmp) });
		} else if (_class == Double.class || _class == Double.TYPE) {
			aMethod.invoke(o, new Object[] { NumberUtils.parseDbl(vTmp) });
		} else if (_class == BigDecimal.class) {
			aMethod.invoke(o, new Object[] { NumberUtils.parseBigDecimal(vTmp) });
		} else if (_class == Integer.class || _class == Integer.TYPE) {
			aMethod.invoke(o, new Object[] { NumberUtils.parseInt(vTmp) });
		} else if (_class == String.class) {
			aMethod.invoke(o, new Object[] { vTmp });
		} else if (_class == Date.class) {
			aMethod.invoke(o, new Object[] { DateUtils.parse(vTmp) });
		} else if (_class == Timestamp.class) {
			aMethod.invoke(o, new Object[] { DateUtils.parseByAll(vTmp) });
		} else if (_class == Boolean.class || _class == Boolean.TYPE) {
			aMethod.invoke(o, new Object[] { parseBoolean(vTmp) });
		} else {
			throw new RuntimeException("不支持的参数类型: " + _class + " | " + name + " | " + vTmp);
		}
	}

	/**
	 * @see 安全处理boolean转换
	 * @param vTmp
	 * @return Boolean
	 * @author zb 330627517@qq.com
	 * @create Mar 29, 2013 4:43:59 PM
	 */
	private static Boolean parseBoolean(String vTmp) {
		if (StrUtils.isEmpty(vTmp)) {
			return false;
		}
		try {
			return Boolean.valueOf(vTmp);
		} catch (Exception e) {
			return false;
		}
	}

}