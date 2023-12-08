package com.huachuan;

import com.huachuan.master.Master;
import org.junit.Test;

public class MasterTest {
    @Test
    public void test() throws Exception {
        Master ms = new Master();
        ms.start();
    }

}
