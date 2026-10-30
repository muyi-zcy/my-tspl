package tech.muyi.tspl.boot;

import org.junit.Test;

public class TsplPrinterManagerTest {

    @Test(expected = IllegalArgumentException.class)
    public void blankHostRejected() {
        TsplPrinterManager manager = new TsplPrinterManager();
        try {
            manager.get("  ", 9100);
        } finally {
            manager.close();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void closedManagerRejectsGet() {
        TsplPrinterManager manager = new TsplPrinterManager();
        manager.close();
        manager.get("127.0.0.1", 9100);
    }

    @Test
    public void evictUnknownIsNoOp() {
        TsplPrinterManager manager = new TsplPrinterManager();
        try {
            manager.evict("127.0.0.1", 9100);
        } finally {
            manager.close();
        }
    }
}
