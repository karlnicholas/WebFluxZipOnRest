package com.example.fluxplay;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

import java.io.OutputStream;

public class OutputStreamToDataBuffer extends OutputStream {
    private final DataBufferFactory dataBufferFactory;
    private DataBuffer db;

    public OutputStreamToDataBuffer(DataBufferFactory dataBufferFactory) {
        this.dataBufferFactory = dataBufferFactory;
        db = null;
    }

    @Override
    public void write(int b)  {
        if ( db == null ) {
            db = dataBufferFactory.allocateBuffer(8192);
        }
        db.write((byte) b);
    }

    public void resetDataBuffer() {
        db = null;
    }

    public DataBuffer getDataBuffer() {
        return db;
    }
}
