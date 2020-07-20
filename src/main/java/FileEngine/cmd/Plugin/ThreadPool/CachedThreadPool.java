package FileEngine.cmd.Plugin.ThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CachedThreadPool {
    private static class ThreadPoolBuilder {
        private static final CachedThreadPool INSTANCE = new CachedThreadPool();
    }

    private CachedThreadPool() {
    }

    public static CachedThreadPool getInstance() {
        return ThreadPoolBuilder.INSTANCE;
    }

    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    public void executeTask(Runnable todo) {
        cachedThreadPool.execute(todo);
    }

    public void shutdown() {
        cachedThreadPool.shutdownNow();
    }
}
