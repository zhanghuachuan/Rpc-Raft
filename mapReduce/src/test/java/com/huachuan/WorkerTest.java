package com.huachuan;

import com.huachuan.worker.Worker;
import org.junit.Test;

public class WorkerTest {
    @Test
    public void worker1() throws Exception {
        Worker worker = new Worker("127.0.0.1:8084");
        worker.start();
    }

    @Test
    public void worker2() throws Exception {
        Worker worker = new Worker("127.0.0.1:8085");
        worker.start();
    }
    @Test
    public void worker3() throws Exception {
        Worker worker = new Worker("127.0.0.1:8086");
        worker.start();
    }
}
