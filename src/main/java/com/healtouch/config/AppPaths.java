package com.healtouch.config;

import java.io.File;

public final class AppPaths {
    private AppPaths() {}
    public static File dataDirectory() {
        String configured = System.getProperty("healtouch.data.dir");
        File dir = configured == null || configured.trim().isEmpty()
                ? new File(System.getProperty("user.home"), "HealTouch") : new File(configured);
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("无法创建数据目录：" + dir);
        return dir;
    }
    public static File databaseFile() { return new File(dataDirectory(), "healtouch.db"); }
}
