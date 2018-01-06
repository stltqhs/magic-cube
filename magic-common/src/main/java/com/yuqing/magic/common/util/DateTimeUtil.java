package com.yuqing.magic.common.util;

import org.apache.commons.lang.StringUtils;

import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 日期工具类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class DateTimeUtil {

    public static final int STRIP_LEADING_ZERO_FIELD_STYLE = 0x01;

    public static final int STRIP_TAILING_ZERO_FIELD_STYLE = 0x02;

    public static final int RECURSIVE_STRIP_FLAG = 0x04;

    public static final int RECURSIVE_STRIP_LEADING_ZERO_FIELD_STYLE = STRIP_LEADING_ZERO_FIELD_STYLE | RECURSIVE_STRIP_FLAG; // 0x01 | 0x04

    public static final int DEFAULT_STYLE = 0x00;

    public static final Time COMPARE_TIME = new Time(24, 0, 0);

    public static final int MILLI_SECONDS_ONE_DAY = 24 * 60 * 60 * 1000;

    private static ThreadLocal<Map<String, DateFormat>> threadDateFormat = new ThreadLocal() {
        @Override
        protected Object initialValue() {
            return new HashMap<>();
        }
    };

    /**
     * 获取的时间为：当前时间的字段加上diff，diff可以是正数和负数，如果diff为0，表示获取当前时间。
     * getDate(Calendar.MINUTE, -10)表示获取十分钟前的时间
     * @param field
     * @param diff
     * @return
     */
    public static Date getDate(int field, int diff) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(field, diff);
        return calendar.getTime();
    }

    /**
     *
     * @param time
     * @param field Calendar的field
     * @return
     *
     * @see Calendar
     */
    public static int getField(Date time, int field) {
        Calendar inst = Calendar.getInstance();
        inst.setTime(time);
        return inst.get(field);
    }

    /**
     *
     * @param time
     * @param field Calendar的field
     * @param val
     * @return
     *
     * @see Calendar
     */
    public static Date setField(Date time, int field, int val) {
        Calendar inst = Calendar.getInstance();
        inst.setTime(time);
        inst.set(field, val);
        return inst.getTime();
    }

    /**
     * 获取时间的毫秒数
     * @param time
     * @return
     */
    public static long getMilliSecondsOfDay(Time time) {
        return 1000 * (time.getHours() * 60 * 60 + time.getMinutes() * 60 + time.getSeconds());
    }

    /**
     * 获取时间的毫秒数
     * @param time
     * @return
     */
    public static long getMilliSecondsOfDay(Date time) {
        return 1000 * (time.getHours() * 60 * 60 + time.getMinutes() * 60 + time.getSeconds());
    }

    /**
     * 格式化时间
     * @param time
     * @return
     */
    public static String format(Time time) {
        return format(time, "%02h:%02m:%02s");
    }

    /**
     * 格式化时间
     * @param time
     * @param pattern second2Human()的format参数
     * @return
     *
     */
    public static String format(Time time, String pattern) {
        int hour = time.getHours();
        int minute = time.getMinutes();
        int second = time.getSeconds();

        if (hour == 0 && time.getTime() >= COMPARE_TIME.getTime()) {
            hour = 24;
        }

        return second2Human(hour * 60 * 60 + minute * 60 + second, pattern, DEFAULT_STYLE);
    }

    /**
     * 获取某一天的开始时间
     * @param startTime
     * @return
     */
    public static Date getStartTimeOf(Date startTime){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Date start = calendar.getTime();
        return start;

    }

    /**
     *  获取某一天的结束时间
     * @param endTime
     * @return
     */
    public static Date getEndTimeOf(Date endTime){
        Calendar calendar = Calendar.getInstance();
        if(null==endTime){
            endTime = new Date();
        }
        calendar.setTime(endTime);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.SECOND, -1);

        Date end = calendar.getTime();
        return end;
    }

    /**
     * 获取与线程绑定的DateFormat来解决SimpleDateFormat不是线程安全
     * @param pattern
     * @return
     */
    public static DateFormat getDateFormat(String pattern) {
        Map<String, DateFormat> dateFormatMap = threadDateFormat.get();
        DateFormat df = dateFormatMap.get(pattern);
        if (df != null) return df;
        df = new SimpleDateFormat(pattern);
        dateFormatMap.put(pattern, df);

        return df;
    }

    /**
     * 将秒转换为人类可读的时间，比如
     * 183秒转换后是"3分3秒"。
     * 可以使用%02s这样的格式，%可作为%的转义符号
     * @param second 需要转换的秒数
     * @param format 类型与String.format，支持的占位符有%s（秒），%m（分），%h（小时）
     * @param style
     * @return
     */
    public static String second2Human(int second, String format, int style) {
        int s = 0,m = 0,h = 0, tmp = 0;

        tmp = second;
        s = tmp % 60;

        tmp /= 60;
        m = tmp % 60;

        tmp /= 60;
        h = tmp;

        StringBuilder content = new StringBuilder();
        StringBuilder buffer = new StringBuilder();
        boolean flag = false;
        int value = -1;
        char pre = '\0';

        List<String> partitionContent = new LinkedList<>();
        List<Integer> partitionValue = new LinkedList<>();

        for (int i = 0; i <= format.length(); i++) {
            if (i == format.length()) {
                // 已经结束
                content.append(buffer);
                addNewPartition(partitionContent, partitionValue, content.toString(), value);
                break;
            }
            char c = format.charAt(i);
            buffer.append(c);
            if (c == '%') {
                if (flag) {
                    if (pre == c) {
                        content.append(c);
                        flag = false;
                        buffer.setLength(0);
                        pre = c;
                        continue;
                    } else {
                        // %xxx%
                        buffer.setLength(buffer.length() - 1);
                        content.append(buffer);
                        buffer.setLength(0);
                        buffer.append(c);
                        flag = true;
                        pre = c;
                        continue;
                    }
                } else {
                    flag = true;
                    if (buffer.length() > 0) {
                        content.append(buffer.substring(0, buffer.length() - 1));
                        buffer.setLength(0);
                        buffer.append(c);
                    }
                    pre = c;
                    continue;
                }
            }
            if (flag && (c == 's' || c == 'm' || c == 'h')) {
                addNewPartition(partitionContent, partitionValue, content.toString(), value);
                value = -1;
                content.setLength(0);
            }
            if (c == 's' && flag) {
                value = parseSecond2HumanField(c, s, m, h, content, buffer);
                buffer.setLength(0);
                flag = !flag;
            } else if (c == 'm' && flag) {
                value = parseSecond2HumanField(c, s, m, h, content, buffer);
                buffer.setLength(0);
                flag = !flag;
            } else if (c == 'h' && flag) {
                value = parseSecond2HumanField(c, s, m, h, content, buffer);
                buffer.setLength(0);
                flag = !flag;
            }
            pre = c;
        }

        return applyStyleForSecond2Human(partitionContent, partitionValue, style);
    }

    private static void addNewPartition(List<String> partitionContent, List<Integer> partitionValue, String content, int value) {
        if (content.length() == 0 && value == -1) return;
        partitionContent.add(content);
        partitionValue.add(value);
    }

    private static String applyStyleForSecond2Human(List<String> partitionContent, List<Integer> partitionValue, int style) {
        StringBuilder text = new StringBuilder();
        int size = Math.max(partitionContent.size(), partitionValue.size());
        String contents[] = new String[size];
        Integer values[] = new Integer[size];

        // 复制
        partitionContent.toArray(contents);
        partitionValue.toArray(values);

        // 不够填充
        for (int i = partitionContent.size(); i < size; i++) {
            contents[i] = "";
        }

        for (int i = partitionValue.size(); i < size; i++) {
            values[i] = -1;
        }

        int leadingZeroIndex = -1;
        int tailingZeroIndex = -1;

        for (int i = 0; i < size; i++) {
            if (style == DEFAULT_STYLE) {
                text.append(contents[i]);
                continue;
            }
            boolean ignore = false;

            if (values[i].intValue() > 0)
                leadingZeroIndex = -2;
            else if (leadingZeroIndex > -2 && values[i].intValue() == 0)
                leadingZeroIndex++;

            for (int j = size - 1; j >= i; j--) {
                if (values[j].intValue() > 0)
                    tailingZeroIndex = -1;
                else if (values[j].intValue() == 0)
                    tailingZeroIndex++;
            }

            if ((style & STRIP_LEADING_ZERO_FIELD_STYLE) > 0
                    && (
                    leadingZeroIndex == 0
                            || (leadingZeroIndex > 0 && (style & RECURSIVE_STRIP_FLAG) > 0)
            ))
                ignore = true;
            if ((style & STRIP_TAILING_ZERO_FIELD_STYLE) > 0
                    && (
                    tailingZeroIndex == 0
                            || (tailingZeroIndex > 0 && (style & RECURSIVE_STRIP_FLAG) > 0)
            ))
                ignore = true;

            if (!ignore) {
                text.append(contents[i]);
            }
        }

        if (text.length() == 0) {
            // 没有发现任何有效数据
            for (int i = size - 1; i >= 0; i--) {
                if (values[i] >= 0) {
                    text.append(contents[i]);
                    break;
                }
            }
        }

        return text.toString();
    }

    private static int parseSecond2HumanField(char field, int s, int m, int h, StringBuilder appender, StringBuilder format) {
        String ft = format.substring(1, format.length() - 1);
        String pattern = "%" + ft + "d";
        int v = -1;
        switch(field) {
            case 's':
                if (ft.equals("") || StringUtils.isNumeric(ft)) {
                    v = s;
                }
                break;
            case 'm':
                if (ft.equals("") || StringUtils.isNumeric(ft)) {
                    v = m;
                }
                break;
            case 'h':
                if (ft.equals("") || StringUtils.isNumeric(ft)) {
                    v = h;
                }
                break;
        }
        if (v != -1)
            appender.append(String.format(pattern, v));
        else
            appender.append(format.toString());

        return v;
    }
}
