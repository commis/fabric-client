package com.energy.justsdk.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Bryan
 * @date 2019-12-07
 */
public class DateTools {

    /**
     * 将日期转换为字符串
     *
     * @param date date日期
     * @return 日期字符串
     */
    public static String parseDateFormat(Date date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA);
            return sdf.format(date);
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * 取得当前时间的字符串
     */
    public static String getNowTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new Date());
    }

    /**
     * 判断两个日期相差的秒数
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 时间相差的秒数
     */
    public static long diffDateInSeconds(Date startDate, Date endDate) {
        long milliseconds1 = startDate.getTime();
        long milliseconds2 = endDate.getTime();
        long diff = milliseconds2 - milliseconds1;
        long diffSeconds = diff / 1000;
        return diffSeconds;
    }

    /**
     * 判断两个日期相差的秒数
     *
     * @param startDate 开始日期字符串
     * @param endDate 结束日期字符串
     * @return 日期相差的秒数
     */
    public static long diffDateInSeconds(String startDate, String endDate) {
        Date sDate = DateTools.strToDate(startDate, "yyyy-MM-dd HH:mm:ss");
        Date eDate = DateTools.strToDate(endDate, "yyyy-MM-dd HH:mm:ss");
        return diffDateInSeconds(sDate, eDate);
    }

    /**
     * 字符串转日期
     *
     * @param str
     * @param strFormat yyyy-MM-dd HH:mm:ss
     */
    public static Date strToDate(String str, String strFormat) {
        DateFormat format = new SimpleDateFormat(strFormat);
        Date date = null;
        try {
            date = format.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
}
