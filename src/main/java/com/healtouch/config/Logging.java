package com.healtouch.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.*;

/** Install before the first SLF4J logger: GUI launches have no usable stderr console. */
public final class Logging {
  private Logging() {}
  private static Handler handler;
  public static synchronized void initialize() throws IOException {
    if(handler!=null) return;
    File directory=new File(AppPaths.dataDirectory(),"logs");
    if(!directory.isDirectory() && !directory.mkdirs()) throw new IOException("无法创建日志目录："+directory);
    Logger logger=Logger.getLogger("HealTouch.file");logger.setUseParentHandlers(false);
    handler=new FileHandler(new File(directory,"healtouch-%g.log").getAbsolutePath(),2*1024*1024,5,true);
    handler.setEncoding("UTF-8");handler.setFormatter(new SimpleFormatter());logger.addHandler(handler);
    System.setErr(new PrintStream(new OutputStream() {
      private final ByteArrayOutputStream line=new ByteArrayOutputStream();
      public synchronized void write(int value) {
        if(value=='\n') flush(); else line.write(value);
      }
      public synchronized void flush() {
        if(line.size()>0) {logger.warning(new String(line.toByteArray(),StandardCharsets.UTF_8));line.reset();handler.flush();}
      }
    },true,"UTF-8"));
    Thread.setDefaultUncaughtExceptionHandler((thread,error)->logger.log(Level.SEVERE,"未捕获异常，线程："+thread.getName(),error));
  }
}
