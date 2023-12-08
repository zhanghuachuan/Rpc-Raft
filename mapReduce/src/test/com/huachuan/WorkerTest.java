package com.huachuan;

import com.huachuan.worker.Worker;
import org.junit.Test;

public class WorkerTest {
    @Test
    public void worker() throws Exception {
        Worker worker = new Worker("127.0.0.1:8084");
        worker.start();
    }
}
