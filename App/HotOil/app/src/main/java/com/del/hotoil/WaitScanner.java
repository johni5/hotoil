package com.del.hotoil;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WaitScanner {

    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final InputStream inputStream;

    public WaitScanner(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void processWhile(int seconds, Consumer<String> cb) {
        service.execute(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            long time = System.currentTimeMillis();
            try {
                while (System.currentTimeMillis() - time < TimeUnit.SECONDS.toMillis(seconds)) {
                    if (reader.ready()) {
                        cb.accept(reader.readLine());
                        time = System.currentTimeMillis();
                    }
                    Thread.sleep(50);
                }
            } catch (Exception e) {
                Log.d(Utils.TAG, Utils.nvl(e.getMessage(), "error:processWhile:" + e));
            }
        });

    }

    public Optional<String> waitLine(int seconds) {
        Future<String> f = service.submit(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (!reader.ready()) {
                Thread.sleep(50);
            }
            return reader.ready() ? reader.readLine() : null;
        });
        String result = null;
        try {
            result = f.get(seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.d(Utils.TAG, "Timeout");
        } finally {
            service.shutdownNow();
        }
        return Optional.ofNullable(result);
    }

    public void close() {
        Utils.close(inputStream);
    }
}
