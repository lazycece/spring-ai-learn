package com.lazycece.springaitoolcalling.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.ZonedDateTime;

/**
 * @author lazycece
 * @date 2026/6/29
 */
public class DateTimeTools {

    @Tool(name = "getCurrentDateTime",
            description = "获取用户时区的当前日期和时间")
    public String getCurrentDateTime() {
        System.out.println("[getCurrentDateTime]");
        return ZonedDateTime.now(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    @Tool(name = "setAlarm",
            description = "在指定 ISO-8601 时间设置闹钟。例如：2026-06-29T15:30:00")
    public void setAlarm(
            @ToolParam(description = "ISO-8601 格式的闹钟时间") String time) {
        System.out.println("[setAlarm]");
        System.out.println("⏰ 闹钟已设置为：" + time);
    }

}
