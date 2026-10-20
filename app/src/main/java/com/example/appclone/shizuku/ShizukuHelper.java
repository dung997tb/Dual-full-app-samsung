
package com.example.appclone.shizuku;

import android.content.pm.PackageManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public final class ShizukuHelper {

    private ShizukuHelper() {}

    public static final class CommandResult {
        public final String stdout;
        public final String stderr;
        public final int exitCode;
        public final boolean timedOut;

        public CommandResult(
                String stdout,
                String stderr,
                int exitCode,
                boolean timedOut) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
            this.timedOut = timedOut;
        }

        public boolean isSuccess() {
            return !timedOut && exitCode == 0;
        }

        public String allOutput() {
            if (stderr.isEmpty()) return stdout;
            return stdout + "\n" + stderr;
        }
    }

    public static boolean isBinderAlive() {
        try {
            return Shizuku.pingBinder();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasPermission() {
        try {
            return isBinderAlive()
                    && Shizuku.checkSelfPermission()
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }

    public static void requestPermission(int requestCode) {
        Shizuku.requestPermission(requestCode);
    }

    /**
     * Thực thi lệnh qua Shizuku.
     *
     * Sử dụng ShizukuRemoteProcess.waitForTimeout()
     * thay vì Process.waitFor(timeout).
     *
     * Phải gọi trên background thread.
     */
    public static CommandResult runCommand(String cmd)
            throws Exception {

        if (!isBinderAlive()) {
            throw new IllegalStateException(
                    "Shizuku chưa được khởi động");
        }

        if (!hasPermission()) {
            throw new SecurityException(
                    "Chưa cấp quyền Shizuku cho AppClone");
        }

        ShizukuRemoteProcess p;

        try {
            Method method = Shizuku.class.getDeclaredMethod(
                    "newProcess",
                    String[].class,
                    String[].class,
                    String.class
            );

            method.setAccessible(true);

            p = (ShizukuRemoteProcess) method.invoke(
                    null,
                    new Object[]{
                            new String[]{"sh", "-c", cmd},
                            null,
                            null
                    }
            );
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new Exception(
                    "Shizuku không thể chạy lệnh: " + cause,
                    cause
            );
        } catch (ReflectiveOperationException
                 | SecurityException e) {
            throw new Exception(
                    "Không truy cập được Shizuku.newProcess: "
                            + e.getMessage(),
                    e
            );
        }

        if (p == null) {
            throw new IllegalStateException(
                    "Shizuku không tạo được process");
        }

        StreamGobbler outG =
                new StreamGobbler(p.getInputStream());
        StreamGobbler errG =
                new StreamGobbler(p.getErrorStream());

        outG.start();
        errG.start();

        boolean finished;

        try {
            // Quan trọng: hàm riêng của ShizukuRemoteProcess.
            finished = p.waitForTimeout(
                    60,
                    TimeUnit.SECONDS
            );
        } catch (InterruptedException e) {
            p.destroy();
            Thread.currentThread().interrupt();
            throw e;
        }

        if (!finished) {
            p.destroy();
            outG.join(3000);
            errG.join(3000);

            return new CommandResult(
                    outG.text(),
                    errG.text(),
                    -1,
                    true
            );
        }

        outG.join(5000);
        errG.join(5000);

        return new CommandResult(
                outG.text(),
                errG.text(),
                p.exitValue(),
                false
        );
    }

    private static class StreamGobbler extends Thread {
        private final InputStream in;
        private final ByteArrayOutputStream buf =
                new ByteArrayOutputStream();

        StreamGobbler(InputStream in) {
            this.in = in;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                byte[] tmp = new byte[8192];
                int n;

                while ((n = in.read(tmp)) != -1) {
                    buf.write(tmp, 0, n);
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }

        String text() {
            try {
                return buf.toString("UTF-8");
            } catch (Exception e) {
                return "";
            }
        }
    }
}
