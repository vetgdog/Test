package org.example;

import java.util.concurrent.*;

public class TestThreadPool {
    public static void main(String[] args) {
        ExecutorService executor = new ThreadPoolExecutor(
                2,
                3,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.execute(()->{
            System.out.println(Thread.currentThread().getName());
        });
        executor.execute(()->{
            System.out.println(Thread.currentThread().getName());
        });
        executor.execute(()->{
            System.out.println(Thread.currentThread().getName());
        });
        executor.execute(()->{
            System.out.println(Thread.currentThread().getName());
        });
        executor.execute(()->{
            System.out.println(Thread.currentThread().getName());
        });executor.execute(()->{
            System.out.println(Thread.currentThread().getName());
        });executor.execute(()->{
            System.out.println(Thread.currentThread().getName());
        });executor.execute(()->{
            System.out.println(Thread.currentThread().getName());
        });executor.execute(()->{
            System.out.println(Thread.currentThread().getName());
        });




    }
}
