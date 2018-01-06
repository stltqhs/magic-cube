package com.yuqing.magic.common.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * 数值对象工具类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class NumberUtil {

    private static Logger logger = LoggerFactory.getLogger(NumberUtil.class);

    public static Long safeParseLong(String longText) {
        if (StringUtils.isBlank(longText)) return null;
        Long tmp = null;
        if (StringUtils.isNumeric(longText))
            try {
                tmp = Long.parseLong(longText);
            } catch (NumberFormatException e) {
                logger.error(longText, e);
            }
        else if (isHexNumber(longText)) { // 处理十六进制
            tmp = new BigInteger(longText, 16).longValue();
        }
        return tmp;
    }

    public static boolean isNumber(String text) {
        return StringUtils.isNumeric(text);
    }

    /**
     * 是否为十六进制数字
     * @param text
     * @return
     */
    public static boolean isHexNumber(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') continue;
            if (c >= 'a' && c <= 'f') continue;
            if (c >= 'A' && c <= 'F') continue;
            return false;
        }
        return true;
    }

    public static Long safeParseLong(String longText, long defaultVal) {
        Long tmp = safeParseLong(longText);
        if (tmp == null) return defaultVal;
        return tmp;
    }

    public static Integer safeParseInteger(String longText) {
        if (StringUtils.isBlank(longText)) return null;
        Integer tmp = null;
        try {
            tmp = Integer.parseInt(longText);
        } catch (NumberFormatException e) {
            logger.error(longText, e);
        }
        return tmp;
    }

    public static Integer safeParseInteger(String longText, int defaultVal) {
        Integer tmp = safeParseInteger(longText);
        if (tmp == null) return defaultVal;
        return tmp;
    }

    public static Double safeParseDouble(String longText, double defaultVal) {
        Double tmp = safeParseDouble(longText);
        if (tmp == null) return defaultVal;
        return tmp;
    }

    public static Double safeParseDouble(String longText) {
        if (StringUtils.isBlank(longText)) return null;
        Double tmp = null;
        try {
            tmp = Double.parseDouble(longText);
        } catch (NumberFormatException e) {
            logger.error(longText, e);
        }
        return tmp;
    }

    public static BigDecimal roundPrice(BigDecimal bigDecimal) {
        return roundPrice(bigDecimal, 2);
    }

    public static BigDecimal roundPrice(BigDecimal bigDecimal, int scale) {
        return round(bigDecimal, scale, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal roundEnergy(BigDecimal bigDecimal) {
        return roundEnergy(bigDecimal, 0);
    }

    public static BigDecimal roundEnergy(BigDecimal bigDecimal, int scale) {
        return round(bigDecimal, scale, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal round(BigDecimal bigDecimal, int scale, int type) {
        return bigDecimal.setScale(scale, type);
    }

    public static float parseFloat(String price) {
        float f = 0;
        try {
            f = Float.parseFloat(price);
        } catch (NumberFormatException e) {
            logger.error(price, e);
        }
        return f;
    }

    /**
     * 阻抗不匹配时使用
     * @param map
     * @param key
     * @return
     */
    public static BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object object = map.get(key);
        if (object == null) return null;
        if (object instanceof BigDecimal) return (BigDecimal)object;
        if (object instanceof Number) return new BigDecimal(object.toString());
        return null;
    }

    public static Long getLongValue(Map<String, Object> map, String key) {
        Object object = map.get(key);
        if (object == null) return null;
        if (object instanceof BigDecimal) return ((BigDecimal)object).longValue();
        if (object instanceof Number) return ((Number) object).longValue();
        return null;
    }

    public static Integer getInteger(Map<String, Object> map, String key) {
        Object object = map.get(key);
        if (object == null) return null;
        if (object instanceof BigDecimal) return ((BigDecimal)object).intValue();
        if (object instanceof Number) return ((Number) object).intValue();
        return null;
    }

    public static float getValue(Float in) {
        if (in == null) {
            return 0;
        }

        return in;
    }

}
