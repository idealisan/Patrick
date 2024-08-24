package org.eu.redfolder.patrick.threadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
    public static final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
}
