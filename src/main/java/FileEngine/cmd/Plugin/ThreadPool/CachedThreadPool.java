package FileEngine.cmd.Plugin.ThreadPool;

import java.util.concurrent.*;

public class CachedThreadPool {
    private static class ThreadPoolBuilder {
        private static final CachedThreadPool INSTANCE = new CachedThreadPool();
    }

    private CachedThreadPool() {
    }

    public static CachedThreadPool getInstance() {
        return ThreadPoolBuilder.INSTANCE;
    }

    private final ExecutorService cachedThreadPool = new ThreadPoolExecutor(0,
            50,
            60L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>());

    public void executeTask(Runnable todo) {
        cachedThreadPool.execute(todo);
    }

    public void shutdown() {
        cachedThreadPool.shutdownNow();
    }
}
