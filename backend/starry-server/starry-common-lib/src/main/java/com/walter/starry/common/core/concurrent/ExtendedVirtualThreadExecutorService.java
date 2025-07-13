package com.walter.starry.common.core.concurrent;

import com.walter.starry.common.util.MdcUtil;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * 虚拟线程服务
 * @Author: walter.tan
 * @DateTime: 2023-10-15 13:03:19
 */
@Slf4j
public class ExtendedVirtualThreadExecutorService implements ExecutorService {
    private Semaphore semaphore;

    private String virtualThreadName;

    private ExecutorService executorService;

    private ExtendedVirtualThreadExecutorService(){}

    /**
     * 构造一个虚拟线程服务实例。提交执行的虚拟线程数量超出maxVirtualThreads，将抛出RejectedExecutionException
     * @param maxVirtualThreads 允许最大的虚拟线程数量
     * @param virtualThreadName 虚拟线程名称的前缀
     * @return
     */
    public static ExtendedVirtualThreadExecutorService of(int maxVirtualThreads, String virtualThreadName){
        return ExtendedVirtualThreadExecutorService.of(maxVirtualThreads, virtualThreadName,
                (thread, throwable) -> log.error("Virtual thread {}", thread.getName(), throwable));
    }

    /**
     * 构造一个虚拟线程服务实例。提交执行的虚拟线程数量超出maxVirtualThreads，将抛出RejectedExecutionException
     * @param maxVirtualThreads 允许最大的虚拟线程数量
     * @param virtualThreadName 虚拟线程名称的前缀
     * @param ueh 未捕获异常的处理器
     * @return
     */
    public static ExtendedVirtualThreadExecutorService of(int maxVirtualThreads, String virtualThreadName, Thread.UncaughtExceptionHandler ueh){
        Assert.isTrue(maxVirtualThreads >= 0, "maxVirtualThreads cannot be negative");
        Assert.notNull(ueh, "ueh cannot be null");

        ExtendedVirtualThreadExecutorService es = new ExtendedVirtualThreadExecutorService();
        es.semaphore = new Semaphore(maxVirtualThreads);
        es.virtualThreadName = virtualThreadName;
        es.executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                .name(virtualThreadName, 1)
                .uncaughtExceptionHandler(ueh)
                .factory());
        return es;
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        if(!semaphore.tryAcquire()){
            throw new RejectedExecutionException("permits are exhausted");
        }

        String parentTraceId = MdcUtil.getTraceId();

        Callable<T> wrapperTask = () -> {
            try{
                return MdcUtil.toMdcCallable(parentTraceId, task).call();
            }finally {
                semaphore.release();
            }
        };
        return executorService.submit(wrapperTask);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        if(!semaphore.tryAcquire()){
            throw new RejectedExecutionException("permits are exhausted");
        }

        String parentTraceId = MdcUtil.getTraceId();

        Runnable wrapperTask = () -> {
            try{
                MdcUtil.toMdcRunnable(parentTraceId, task).run();
            }finally {
                semaphore.release();
            }
        };
        return executorService.submit(wrapperTask, result);
    }

    @Nonnull
    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        if(!semaphore.tryAcquire()){
            throw new RejectedExecutionException("permits are exhausted");
        }

        String parentTraceId = MdcUtil.getTraceId();

        Runnable wrapperTask = () -> {
            try{
                MdcUtil.toMdcRunnable(parentTraceId, task).run();
            }finally {
                semaphore.release();
            }
        };
        return executorService.submit(wrapperTask);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Assert.notEmpty(tasks, "tasks cannot be empty");

        if(!semaphore.tryAcquire(tasks.size())){
            throw new RejectedExecutionException("permits are exhausted");
        }

        String parentTraceId = MdcUtil.getTraceId();

        Collection<? extends Callable<T>> wrapperTasks = tasks.stream().map(task -> (Callable<T>) () -> {
            try{
                return MdcUtil.toMdcCallable(parentTraceId, task).call();
            }finally {
                semaphore.release();
            }
        }).toList();

        return executorService.invokeAll(wrapperTasks);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        Assert.notEmpty(tasks, "tasks cannot be empty");

        if(!semaphore.tryAcquire(tasks.size())){
            throw new RejectedExecutionException("permits are exhausted");
        }

        String parentTraceId = MdcUtil.getTraceId();

        Collection<? extends Callable<T>> wrapperTasks = tasks.stream().map(task -> (Callable<T>) () -> {
            try{
                return MdcUtil.toMdcCallable(parentTraceId, task).call();
            }finally {
                semaphore.release();
            }
        }).toList();

        return executorService.invokeAll(wrapperTasks, timeout, unit);
    }

    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Assert.notEmpty(tasks, "tasks cannot be empty");

        if(!semaphore.tryAcquire(tasks.size())){
            throw new RejectedExecutionException("permits are exhausted");
        }

        String parentTraceId = MdcUtil.getTraceId();

        try{
            return executorService.invokeAny(tasks.stream().map(t -> MdcUtil.toMdcCallable(parentTraceId, t)).toList());
        }finally {
            semaphore.release(tasks.size());
        }
    }

    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Assert.notEmpty(tasks, "tasks cannot be empty");

        if(!semaphore.tryAcquire(tasks.size())){
            throw new RejectedExecutionException("permits are exhausted");
        }

        String parentTraceId = MdcUtil.getTraceId();

        try{
            return executorService.invokeAny(tasks.stream().map(t -> MdcUtil.toMdcCallable(parentTraceId, t)).toList(), timeout, unit);
        }finally {
            semaphore.release(tasks.size());
        }
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        if(!semaphore.tryAcquire()){
            throw new RejectedExecutionException("permits are exhausted");
        }

        String parentTraceId = MdcUtil.getTraceId();

        Runnable wrapperCommand = () -> {
            try{
                MdcUtil.toMdcRunnable(parentTraceId, command).run();
            }finally {
                semaphore.release();
            }
        };

        executorService.execute(wrapperCommand);
    }

    @Override
    public void close() {
        // Spring Bean在销毁时，会自动调用AutoCloseable.close()方法
        log.info("Closing virtual thread executor service: {}", this.virtualThreadName);
        ExecutorService.super.close();
        log.info("Finish closing virtual thread executor service: {}", this.virtualThreadName);
    }
}
