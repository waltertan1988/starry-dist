package com.walter.starry.common.core.concurrent;

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

    private ExtendedVirtualThreadExecutorPostProcessorChain postProcessorChain;

    private ExecutorService executorService;

    private ExtendedVirtualThreadExecutorService(){}

    /**
     * 构造一个虚拟线程服务实例。提交执行的虚拟线程数量超出maxVirtualThreads，将抛出RejectedExecutionException
     * @param maxVirtualThreads 允许最大的虚拟线程数量
     * @param virtualThreadName 虚拟线程名称的前缀
     * @param postProcessorChain 后处理调用链
     * @return 虚拟线程池实例
     */
    public static ExtendedVirtualThreadExecutorService of(int maxVirtualThreads, String virtualThreadName, ExtendedVirtualThreadExecutorPostProcessorChain postProcessorChain){
        return ExtendedVirtualThreadExecutorService.of(maxVirtualThreads, virtualThreadName, postProcessorChain,
                (thread, throwable) -> log.error("Virtual thread {}", thread.getName(), throwable));
    }

    /**
     * 构造一个虚拟线程服务实例。提交执行的虚拟线程数量超出maxVirtualThreads，将抛出RejectedExecutionException
     * @param maxVirtualThreads 允许最大的虚拟线程数量
     * @param virtualThreadName 虚拟线程名称的前缀
     * @param postProcessorChain 后处理调用链
     * @param ueh 未捕获异常的处理器
     * @return 虚拟线程池实例
     */
    public static ExtendedVirtualThreadExecutorService of(int maxVirtualThreads, String virtualThreadName, ExtendedVirtualThreadExecutorPostProcessorChain postProcessorChain, Thread.UncaughtExceptionHandler ueh){
        Assert.isTrue(maxVirtualThreads >= 0, "maxVirtualThreads cannot be negative");
        Assert.notNull(postProcessorChain, "postProcessorChain cannot be null");
        Assert.notNull(ueh, "ueh cannot be null");

        ExtendedVirtualThreadExecutorService es = new ExtendedVirtualThreadExecutorService();
        es.semaphore = new Semaphore(maxVirtualThreads);
        es.virtualThreadName = virtualThreadName;
        es.postProcessorChain = postProcessorChain;
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

        Callable<T> proxyTask = postProcessorChain.proxy(task);
        Callable<T> wrapperTask = this.toReleaseSemaphoreProxy(proxyTask);
        return executorService.submit(wrapperTask);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        if(!semaphore.tryAcquire()){
            throw new RejectedExecutionException("permits are exhausted");
        }

        Runnable proxyTask = this.toReleaseSemaphoreProxy(postProcessorChain.proxy(task));
        return executorService.submit(proxyTask, result);
    }

    @Nonnull
    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        if(!semaphore.tryAcquire()){
            throw new RejectedExecutionException("permits are exhausted");
        }

        Runnable proxyTask = this.toReleaseSemaphoreProxy(postProcessorChain.proxy(task));
        return executorService.submit(proxyTask);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Assert.notEmpty(tasks, "tasks cannot be empty");

        if(!semaphore.tryAcquire(tasks.size())){
            throw new RejectedExecutionException("permits are exhausted");
        }

        List<Callable<T>> proxyTasks = tasks.stream().map(postProcessorChain::proxy).map(this::toReleaseSemaphoreProxy).toList();
        return executorService.invokeAll(proxyTasks);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        Assert.notEmpty(tasks, "tasks cannot be empty");

        if(!semaphore.tryAcquire(tasks.size())){
            throw new RejectedExecutionException("permits are exhausted");
        }

        List<Callable<T>> proxyTasks = tasks.stream().map(postProcessorChain::proxy).map(this::toReleaseSemaphoreProxy).toList();
        return executorService.invokeAll(proxyTasks, timeout, unit);
    }

    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Assert.notEmpty(tasks, "tasks cannot be empty");

        if(!semaphore.tryAcquire(tasks.size())){
            throw new RejectedExecutionException("permits are exhausted");
        }

        try{
            List<Callable<T>> proxyTasks = tasks.stream().map(postProcessorChain::proxy).toList();
            return executorService.invokeAny(proxyTasks);
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

        try{
            List<Callable<T>> proxyTasks = tasks.stream().map(postProcessorChain::proxy).toList();
            return executorService.invokeAny(proxyTasks, timeout, unit);
        }finally {
            semaphore.release(tasks.size());
        }
    }

    @Override
    public void execute(@Nonnull Runnable task) {
        if(!semaphore.tryAcquire()){
            throw new RejectedExecutionException("permits are exhausted");
        }

        Runnable proxyTask = this.toReleaseSemaphoreProxy(postProcessorChain.proxy(task));
        executorService.execute(proxyTask);
    }

    @Override
    public void close() {
        // Spring Bean在销毁时，会自动调用AutoCloseable.close()方法
        log.info("Closing virtual thread executor service: {}", this.virtualThreadName);
        ExecutorService.super.close();
        log.info("Finish closing virtual thread executor service: {}", this.virtualThreadName);
    }

    private Runnable toReleaseSemaphoreProxy(Runnable runnable) {
        return () -> {
            try{
                runnable.run();
            }finally {
                semaphore.release();
            }
        };
    }

    private <T> Callable<T> toReleaseSemaphoreProxy(Callable<T> callable) {
        return () -> {
            try{
                return callable.call();
            }finally {
                semaphore.release();
            }
        };
    }
}
