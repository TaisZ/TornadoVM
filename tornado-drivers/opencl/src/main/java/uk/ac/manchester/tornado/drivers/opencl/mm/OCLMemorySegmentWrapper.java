package uk.ac.manchester.tornado.drivers.opencl.mm;

import static uk.ac.manchester.tornado.runtime.common.Tornado.info;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.tornado.api.collections.types.VectorFloat2;
import uk.ac.manchester.tornado.api.collections.types.VectorFloat3;
import uk.ac.manchester.tornado.api.collections.types.VectorFloat4;
import uk.ac.manchester.tornado.api.data.nativetypes.ByteArray;
import uk.ac.manchester.tornado.api.data.nativetypes.CharArray;
import uk.ac.manchester.tornado.api.data.nativetypes.DoubleArray;
import uk.ac.manchester.tornado.api.data.nativetypes.FloatArray;
import uk.ac.manchester.tornado.api.data.nativetypes.IntArray;
import uk.ac.manchester.tornado.api.data.nativetypes.LongArray;
import uk.ac.manchester.tornado.api.data.nativetypes.ShortArray;
import uk.ac.manchester.tornado.api.exceptions.TornadoInternalError;
import uk.ac.manchester.tornado.api.exceptions.TornadoMemoryException;
import uk.ac.manchester.tornado.api.exceptions.TornadoOutOfMemoryException;
import uk.ac.manchester.tornado.api.memory.ObjectBuffer;
import uk.ac.manchester.tornado.drivers.opencl.OCLDeviceContext;
import uk.ac.manchester.tornado.runtime.common.Tornado;

public class OCLMemorySegmentWrapper implements ObjectBuffer {

    private final OCLDeviceContext deviceContext;

    private long bufferId;
    private long bufferOffset;
    private boolean onDevice;
    private static final int INIT_VALUE = -1;

    private final long batchSize;

    private long bufferSize;

    private long subregionSize;

    private final long panamaHeaderSize;

    public OCLMemorySegmentWrapper(OCLDeviceContext deviceContext, long batchSize, long panamaHeaderSize) {
        this.deviceContext = deviceContext;
        this.batchSize = batchSize;
        this.bufferSize = INIT_VALUE; // this is in bytes, should it be in elements?
        this.bufferId = INIT_VALUE;
        this.bufferOffset = 0;
        this.panamaHeaderSize = panamaHeaderSize;
        onDevice = false;
    }

    public OCLMemorySegmentWrapper(FloatArray floatSegment, OCLDeviceContext deviceContext, long batchSize, long panamaHeaderSize) {
        this.deviceContext = deviceContext;
        this.batchSize = batchSize;
        this.bufferSize = floatSegment.getSegment().byteSize(); // this is in bytes, should it be in elements?
        this.bufferId = INIT_VALUE;
        this.bufferOffset = 0;
        this.panamaHeaderSize = panamaHeaderSize;
        onDevice = false;
    }

    @Override
    public long toBuffer() {
        // guarantee(deviceContext.getSegmentToBufferMap().containsKey(segment), "Should
        // contain the segment by this point");
        return this.bufferId; // deviceContext.getSegmentToBufferMap().get(segment).getBufferId();
    }

    @Override
    public void setBuffer(ObjectBufferWrapper bufferWrapper) {
        this.bufferId = bufferWrapper.buffer;
        this.bufferOffset = bufferWrapper.bufferOffset;

        bufferWrapper.bufferOffset += bufferSize;
    }

    @Override
    public long getBufferOffset() {
        return bufferOffset;
    }

    @Override
    public void read(final Object reference) {
        read(reference, 0, null, false);
    }

    @Override
    public int read(final Object reference, long hostOffset, int[] events, boolean useDeps) {
        MemorySegment segment;
        if (reference instanceof IntArray) {
            segment = ((IntArray) reference).getSegment();
        } else if (reference instanceof FloatArray) {
            segment = ((FloatArray) reference).getSegment();
        } else if (reference instanceof DoubleArray) {
            segment = ((DoubleArray) reference).getSegment();
        } else if (reference instanceof LongArray) {
            segment = ((LongArray) reference).getSegment();
        } else if (reference instanceof ShortArray) {
            segment = ((ShortArray) reference).getSegment();
        } else if (reference instanceof ByteArray) {
            segment = ((ByteArray) reference).getSegment();
        } else if (reference instanceof CharArray) {
            segment = ((CharArray) reference).getSegment();
        } else if (reference instanceof VectorFloat2) {
            segment = ((VectorFloat2) reference).getArray().getSegment();
        } else if (reference instanceof VectorFloat3) {
            segment = ((VectorFloat3) reference).getArray().getSegment();
        } else if (reference instanceof VectorFloat4) {
            segment = ((VectorFloat4) reference).getArray().getSegment();
        } else {
            segment = (MemorySegment) reference;
        }

        final long numBytes = getSizeSubRegionSize() > 0 ? getSizeSubRegionSize() : bufferSize;
        final int returnEvent = deviceContext.readBuffer(toBuffer(), bufferOffset + panamaHeaderSize, numBytes - panamaHeaderSize, segment.address(), hostOffset, (useDeps) ? events : null);
        return useDeps ? returnEvent : -1;
    }

    @Override
    public void write(Object reference) {
        MemorySegment seg;
        if (reference instanceof IntArray) {
            seg = ((IntArray) reference).getSegment();
        } else if (reference instanceof FloatArray) {
            seg = ((FloatArray) reference).getSegment();
        } else if (reference instanceof DoubleArray) {
            seg = ((DoubleArray) reference).getSegment();
        } else if (reference instanceof LongArray) {
            seg = ((LongArray) reference).getSegment();
        } else if (reference instanceof ShortArray) {
            seg = ((ShortArray) reference).getSegment();
        } else if (reference instanceof ByteArray) {
            seg = ((ByteArray) reference).getSegment();
        } else if (reference instanceof CharArray) {
            seg = ((CharArray) reference).getSegment();
        } else if (reference instanceof VectorFloat2) {
            seg = ((VectorFloat2) reference).getArray().getSegment();
        } else if (reference instanceof VectorFloat3) {
            seg = ((VectorFloat3) reference).getArray().getSegment();
        } else if (reference instanceof VectorFloat4) {
            seg = ((VectorFloat4) reference).getArray().getSegment();
        } else {
            seg = (MemorySegment) reference;
        }
        // buildArrayHeader(seg.byteSize()).write();
        deviceContext.writeBuffer(toBuffer(), bufferOffset + panamaHeaderSize, bufferSize - panamaHeaderSize, seg.address(), 0, null);
        onDevice = true;
    }

    @Override
    public int enqueueRead(Object reference, long hostOffset, int[] events, boolean useDeps) {

        MemorySegment seg;
        if (reference instanceof IntArray) {
            seg = ((IntArray) reference).getSegment();
        } else if (reference instanceof FloatArray) {
            seg = ((FloatArray) reference).getSegment();
        } else if (reference instanceof DoubleArray) {
            seg = ((DoubleArray) reference).getSegment();
        } else if (reference instanceof LongArray) {
            seg = ((LongArray) reference).getSegment();
        } else if (reference instanceof ShortArray) {
            seg = ((ShortArray) reference).getSegment();
        } else if (reference instanceof ByteArray) {
            seg = ((ByteArray) reference).getSegment();
        } else if (reference instanceof CharArray) {
            seg = ((CharArray) reference).getSegment();
        } else if (reference instanceof VectorFloat2) {
            seg = ((VectorFloat2) reference).getArray().getSegment();
        } else if (reference instanceof VectorFloat3) {
            seg = ((VectorFloat3) reference).getArray().getSegment();
        } else if (reference instanceof VectorFloat4) {
            seg = ((VectorFloat4) reference).getArray().getSegment();
        } else {
            seg = (MemorySegment) reference;
        }

        final int returnEvent = deviceContext.enqueueReadBuffer(toBuffer(), bufferOffset + panamaHeaderSize, bufferSize - panamaHeaderSize, seg.address(), hostOffset, (useDeps) ? events : null);
        return useDeps ? returnEvent : -1;
    }

    @Override
    public List<Integer> enqueueWrite(Object reference, long batchSize, long hostOffset, int[] events, boolean useDeps) {
        List<Integer> returnEvents = new ArrayList<>();
        MemorySegment seg;
        if (reference instanceof IntArray) {
            seg = ((IntArray) reference).getSegment();
        } else if (reference instanceof FloatArray) {
            seg = ((FloatArray) reference).getSegment();
        } else if (reference instanceof DoubleArray) {
            seg = ((DoubleArray) reference).getSegment();
        } else if (reference instanceof LongArray) {
            seg = ((LongArray) reference).getSegment();
        } else if (reference instanceof ShortArray) {
            seg = ((ShortArray) reference).getSegment();
        } else if (reference instanceof ByteArray) {
            seg = ((ByteArray) reference).getSegment();
        } else if (reference instanceof CharArray) {
            seg = ((CharArray) reference).getSegment();
        } else if (reference instanceof VectorFloat2) {
            seg = ((VectorFloat2) reference).getArray().getSegment();
        } else if (reference instanceof VectorFloat3) {
            seg = ((VectorFloat3) reference).getArray().getSegment();
        } else if (reference instanceof VectorFloat4) {
            seg = ((VectorFloat4) reference).getArray().getSegment();
        } else {
            seg = (MemorySegment) reference;
        }

        // We first write the header for the object, and then we write actual buffer
        final int headerEvent;
        if (batchSize <= 0) {
            headerEvent = buildArrayHeader((int) (bufferSize - panamaHeaderSize)).enqueueWrite((useDeps) ? events : null);
        } else {
            headerEvent = buildArrayHeaderBatch((int) batchSize).enqueueWrite((useDeps) ? events : null);
        }

        int internalEvent = deviceContext.enqueueWriteBuffer(toBuffer(), bufferOffset + panamaHeaderSize, bufferSize - panamaHeaderSize, seg.address(), hostOffset, (useDeps) ? events : null);
        returnEvents.add(headerEvent);
        returnEvents.add(internalEvent);
        onDevice = true;
        return useDeps ? returnEvents : null;
    }

    @Override
    public void allocate(Object reference, long batchSize) throws TornadoOutOfMemoryException, TornadoMemoryException {
        MemorySegment memref;
        if (reference instanceof IntArray) {
            memref = ((IntArray) reference).getSegment();
        } else if (reference instanceof FloatArray) {
            memref = ((FloatArray) reference).getSegment();
        } else if (reference instanceof DoubleArray) {
            memref = ((DoubleArray) reference).getSegment();
        } else if (reference instanceof LongArray) {
            memref = ((LongArray) reference).getSegment();
        } else if (reference instanceof ShortArray) {
            memref = ((ShortArray) reference).getSegment();
        } else if (reference instanceof ByteArray) {
            memref = ((ByteArray) reference).getSegment();
        } else if (reference instanceof CharArray) {
            memref = ((CharArray) reference).getSegment();
        } else if (reference instanceof VectorFloat2) {
            memref = ((VectorFloat2) reference).getArray().getSegment();
        } else if (reference instanceof VectorFloat3) {
            memref = ((VectorFloat3) reference).getArray().getSegment();
        } else if (reference instanceof VectorFloat4) {
            memref = ((VectorFloat4) reference).getArray().getSegment();
        } else {
            memref = (MemorySegment) reference;
        }

        if (batchSize <= 0) {
            bufferSize = memref.byteSize();
        } else {
            bufferSize = batchSize;
        }

        if (bufferSize <= 0) {
            throw new TornadoMemoryException("[ERROR] Bytes Allocated <= 0: " + bufferSize);
        }

        bufferId = deviceContext.getBufferProvider().getBufferWithSize(bufferSize);

        if (Tornado.FULL_DEBUG) {
            info("allocated: %s", toString());
        }
    }

    /*
     * Retrieves a buffer that will contain the contents of the array header. The
     * header is also populated using the header from the given array.
     */
    private OCLByteBuffer buildArrayHeader(final int arraySize) {
        final OCLByteBuffer header = getArrayHeader();
        for (int i = 0; i < (panamaHeaderSize - 4L); i++) {
            header.buffer.put((byte) 0);
        }
        header.buffer.putInt(arraySize);
        return header;
    }

    private OCLByteBuffer getArrayHeader() {
        final OCLByteBuffer header = new OCLByteBuffer(deviceContext, bufferId, bufferOffset, panamaHeaderSize);
        header.buffer.clear();
        return header;
    }

    private OCLByteBuffer buildArrayHeaderBatch(final int arraySize) {
        final OCLByteBuffer header = getArrayHeader();
        for (int i = 0; i < (panamaHeaderSize - 4L); i++) {
            header.buffer.put((byte) 0);
        }
        header.buffer.putInt(arraySize);
        return header;
    }

    // TODO: Check if correct
    @Override
    public void deallocate() throws TornadoMemoryException {
        TornadoInternalError.guarantee(bufferId != INIT_VALUE, "Fatal error: trying to deallocate an invalid buffer");
        // long bufferSize = memref.byteSize();
        deviceContext.getBufferProvider().markBufferReleased(bufferId, bufferSize);
        bufferId = INIT_VALUE;
        bufferSize = INIT_VALUE;

        if (Tornado.FULL_DEBUG) {
            info("deallocated: %s", toString());
        }
    }

    @Override
    public long size() {
        return bufferSize;
    }

    @Override
    public void setSizeSubRegion(long batchSize) {
        this.subregionSize = batchSize;
    }

    @Override
    public long getSizeSubRegionSize() {
        return subregionSize;
    }

    public long getBatchSize() {
        return batchSize;
    }
}
