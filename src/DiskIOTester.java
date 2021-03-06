package im.boddy.iotester;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.io.*;
import java.nio.*;


import java.nio.channels.*;
////test comment from android

public class DiskIOTester extends IOTester
{
    /**
     * Thread safe
     */ 
    private class Handler implements Runnable
    {
        private volatile boolean isClosed;
        private long lastPosition;

        private final byte[] buffer = new byte[bufferSize];
        private final RandomAccessFile rFile;
        private final FileChannel channel;

        Handler() throws IOException
        {
            this.rFile = new RandomAccessFile(f, "rw");
            this.channel = rFile.getChannel(); 
            random.nextBytes(buffer);
        }

        public void run()
        {
            while(true)
            {
                try
                {
                    //
                    // Dont start I/O operations until the timer is started
                    //
                    latch.await();
                    break;
                } catch (InterruptedException ie){}
            }

            ByteBuffer readBuffer = ByteBuffer.allocate(buffer.length);
            
            while (! isClosed)
            {
                try
                {
                    synchronized(this)
                    {
                        long startPos = nextPosition();
                        if (reading)
                        {
                            int nReadTotal = 0;	
                            while(readBuffer.hasRemaining())
                            {
                                int nRead = channel.read(readBuffer, startPos + nReadTotal);
                                if (nRead >0)
                                    nReadTotal += nRead;
                                else if (nRead == -1)
                                    break;
                            }
                            readBuffer.flip();
                            readCount.addAndGet(nReadTotal);
                        }
                        else
                        {
                            int nWrittenTotal =0;
                            ByteBuffer writeBuffer = ByteBuffer.wrap(buffer);
                            while (writeBuffer.hasRemaining())
                            {
                                int nWritten = channel.write(writeBuffer, startPos + nWrittenTotal);
                                if (nWritten >0)
                                    nWrittenTotal += nWritten;
                            }

                            writeCount.addAndGet(nWrittenTotal);
                        }
                    }

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        public synchronized void close()
        {
            if (isClosed)
                return;
            isClosed = true;
            try
            {
                if (channel != null)
                    channel.close();
            } catch (IOException ioe){
                ioe.printStackTrace();
            }

            try
            {
                if (rFile != null)
                    rFile.close();
            } catch (IOException ioe){
                ioe.printStackTrace();
            }
        }

        private synchronized long nextPosition()
        {
            long pos = lastPosition;
            if (! randomAccess)
                pos += buffer.length;
            else
                pos = random.nextLong();    

            if (pos < 0)
                pos *= -1;

            pos %= (maxSize - buffer.length);
            lastPosition = pos;
            return pos; 
        }
    }

    private Handler handler;

    private final File f;
    private final boolean reading, randomAccess;
    private final long maxSize;


    DiskIOTester(int duration, int bufferSize, String filePath, boolean reading, long maxSize, boolean randomAccess) throws IOException
    {
        super(duration, bufferSize);

        this.f = new File(filePath);
        this.reading = reading;
        this.randomAccess = randomAccess;
        this.maxSize = maxSize;

        if (reading)
        {
            if (! f.exists())
                throw new IOException("Cannot use file "+ f + ": file already exists.");
        }
        else
        {
            f.delete();
            f.createNewFile();
            if (! f.exists())
                throw new IOException("Cannot use file "+ f + ": could not create file."); 
        }
    }    

    public synchronized void init()
    {
        if (handler != null)
            return;
        try
        {
            handler = new Handler();
            new Thread(handler).start();
        } catch (IOException ioe) { 
            close();
            throw new IllegalStateException(ioe);
        }
    }
    public synchronized void close()
    {
        super.close();
        handler.close();
    }


}