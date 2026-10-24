package com.healtouch.ui;

import com.healtouch.app.AppTasks;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Modal progress uses JavaFX's nested event loop; SQL and hashing run on the database worker. */
public final class DbWork {
  private DbWork() {}
  public static Window owner;
  public static <T> T call(Callable<T> work) {
    if (!Platform.isFxApplicationThread()) {
      try { return work.call(); } catch (RuntimeException e) { throw e; }
      catch (Exception e) { throw new IllegalStateException("操作未完成", e); }
    }
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    if (owner != null && owner.isShowing()) dialog.initOwner(owner);
    dialog.setTitle("正在处理");
    dialog.getDialogPane().setContent(new VBox(12, new Label("正在读取或保存本地数据，请稍候…"), new ProgressIndicator()));
    dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
    dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
    final boolean[] done = {false};
    dialog.setOnCloseRequest(event -> { if (!done[0]) event.consume(); });
    Task<T> task = new Task<T>() { @Override protected T call() throws Exception { return work.call(); } };
    task.setOnSucceeded(event -> { done[0]=true; dialog.close(); });
    task.setOnFailed(event -> { done[0]=true; dialog.close(); });
    AppTasks.DATABASE.execute(task);
    dialog.showAndWait();
    try { return task.get(); }
    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("操作被中断",e); }
    catch (ExecutionException e) {
      Throwable failure=e.getCause();
      if(failure instanceof RuntimeException) throw (RuntimeException)failure;
      if(failure instanceof Error) throw (Error)failure;
      throw new IllegalStateException("操作未完成",failure);
    }
  }
  public interface Read0<R> { R run() throws Exception; }
  public static <R> R read(Read0<R> work) { return call(() -> work.run()); }
  public interface Read1<A,R> { R run(A v0) throws Exception; }
  public static <A,R> R read(Read1<A,R> work, A v0) { return call(() -> work.run(v0)); }
  public interface Write1<A> { void run(A v0) throws Exception; }
  public static <A> void write(Write1<A> work, A v0) { call(() -> { work.run(v0); return null; }); }
  public interface Read2<A,B,R> { R run(A v0, B v1) throws Exception; }
  public static <A,B,R> R read(Read2<A,B,R> work, A v0, B v1) { return call(() -> work.run(v0, v1)); }
  public interface Write2<A,B> { void run(A v0, B v1) throws Exception; }
  public static <A,B> void write(Write2<A,B> work, A v0, B v1) { call(() -> { work.run(v0, v1); return null; }); }
  public interface Read3<A,B,C,R> { R run(A v0, B v1, C v2) throws Exception; }
  public static <A,B,C,R> R read(Read3<A,B,C,R> work, A v0, B v1, C v2) { return call(() -> work.run(v0, v1, v2)); }
  public interface Write3<A,B,C> { void run(A v0, B v1, C v2) throws Exception; }
  public static <A,B,C> void write(Write3<A,B,C> work, A v0, B v1, C v2) { call(() -> { work.run(v0, v1, v2); return null; }); }
  public interface Read4<A,B,C,D,R> { R run(A v0, B v1, C v2, D v3) throws Exception; }
  public static <A,B,C,D,R> R read(Read4<A,B,C,D,R> work, A v0, B v1, C v2, D v3) { return call(() -> work.run(v0, v1, v2, v3)); }
  public interface Write4<A,B,C,D> { void run(A v0, B v1, C v2, D v3) throws Exception; }
  public static <A,B,C,D> void write(Write4<A,B,C,D> work, A v0, B v1, C v2, D v3) { call(() -> { work.run(v0, v1, v2, v3); return null; }); }
  public interface Read5<A,B,C,D,E,R> { R run(A v0, B v1, C v2, D v3, E v4) throws Exception; }
  public static <A,B,C,D,E,R> R read(Read5<A,B,C,D,E,R> work, A v0, B v1, C v2, D v3, E v4) { return call(() -> work.run(v0, v1, v2, v3, v4)); }
  public interface Write5<A,B,C,D,E> { void run(A v0, B v1, C v2, D v3, E v4) throws Exception; }
  public static <A,B,C,D,E> void write(Write5<A,B,C,D,E> work, A v0, B v1, C v2, D v3, E v4) { call(() -> { work.run(v0, v1, v2, v3, v4); return null; }); }
  public interface Read6<A,B,C,D,E,F,R> { R run(A v0, B v1, C v2, D v3, E v4, F v5) throws Exception; }
  public static <A,B,C,D,E,F,R> R read(Read6<A,B,C,D,E,F,R> work, A v0, B v1, C v2, D v3, E v4, F v5) { return call(() -> work.run(v0, v1, v2, v3, v4, v5)); }
  public interface Write6<A,B,C,D,E,F> { void run(A v0, B v1, C v2, D v3, E v4, F v5) throws Exception; }
  public static <A,B,C,D,E,F> void write(Write6<A,B,C,D,E,F> work, A v0, B v1, C v2, D v3, E v4, F v5) { call(() -> { work.run(v0, v1, v2, v3, v4, v5); return null; }); }
  public interface Read7<A,B,C,D,E,F,G,R> { R run(A v0, B v1, C v2, D v3, E v4, F v5, G v6) throws Exception; }
  public static <A,B,C,D,E,F,G,R> R read(Read7<A,B,C,D,E,F,G,R> work, A v0, B v1, C v2, D v3, E v4, F v5, G v6) { return call(() -> work.run(v0, v1, v2, v3, v4, v5, v6)); }
  public interface Write7<A,B,C,D,E,F,G> { void run(A v0, B v1, C v2, D v3, E v4, F v5, G v6) throws Exception; }
  public static <A,B,C,D,E,F,G> void write(Write7<A,B,C,D,E,F,G> work, A v0, B v1, C v2, D v3, E v4, F v5, G v6) { call(() -> { work.run(v0, v1, v2, v3, v4, v5, v6); return null; }); }
}
