package com.energy.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Bryan
 */
public class TimeUtil {

    public static String CURRENT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static String DAY_FORMAT_STRING1 = "yyyyMMdd";
    public static String DAY_FORMAT_STRING2 = "yyyy-MM-dd";

    public static String getCurrentTime() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(CURRENT_TIME_FORMAT);
        return sdf.format(date);
    }

    public static Date getYesterDayZeroHour() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
        return calendar.getTime();
    }

    /**
     * 获取几天前的时间
     *
     * @Param d 原始日期
     * @Param day 天数
     * @Return 计算后的时间
     */
    public static Date getDateBefore(Date d, int day) {
        Calendar now = Calendar.getInstance();
        now.setTime(d);
        now.set(Calendar.DATE, now.get(Calendar.DATE) - day);
        return now.getTime();
    }

    /**
     * 获取几天后的时间
     *
     * @Param d 原始日期
     * @Param day 天数
     * @Return 计算后的时间
     */
    public static Date getDateAfter(Date d, int day) {
        Calendar now = Calendar.getInstance();
        now.setTime(d);
        now.set(Calendar.DATE, now.get(Calendar.DATE) + day);
        return now.getTime();
    }

    /**
     * 获取日期格式为数字20181102
     */
    public static Integer getTimeDayToInt(Timestamp timestamp) {
        SimpleDateFormat df = new SimpleDateFormat(DAY_FORMAT_STRING1);
        Date date = new Date(timestamp.getTime());
        return Integer.valueOf(df.format(date));
    }

    public static Integer secondToDate(Integer second) {
        Timestamp ts = new Timestamp(second.longValue() * 1000);
        return getTimeDayToInt(ts);
    }

    /**
     * 获取日期格式为数字20181102
     */
    public static Integer getDateToInt(Date date) {
        SimpleDateFormat df = new SimpleDateFormat(DAY_FORMAT_STRING1);
        return Integer.valueOf(df.format(date));
    }

    /**
     * 获取日期格式为数字 '20181102'
     */
    public static String getDateToStr(Date date) {
        SimpleDateFormat df = new SimpleDateFormat(DAY_FORMAT_STRING2);
        return df.format(date);
    }

}
