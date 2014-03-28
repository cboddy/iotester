package im.boddy.iotester;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.io.*;

public abstract class IOTester implements Runnable
{
    public static final int DEFAULT_BUFFER_LENGTH = 64*1024;
    
    private static Histogram readHistogram, writeHistogram;
    private static String histFileName;
    private static int sleepTick = 100;
    
    protected volatile boolean isClosed;
    protected final CountDownLatch latch;

    protected final AtomicLong readCount, writeCount, totalReadCount, totalWriteCount;
    protected final int duration, bufferSize;
    protected final Random random;
    
    public IOTester(int duration, int bufferSize)
    {
        this.readCount = new AtomicLong();
        this.writeCount = new AtomicLong();
        this.totalReadCount = new AtomicLong();
        this.totalWriteCount = new AtomicLong();

        this.random = new Random();

        this.latch = new CountDownLatch(1);

        this.duration = duration;
        this.bufferSize = bufferSize;
    }

    abstract void init();

    protected synchronized void close()
    {
        isClosed = true;
    }

    public void run()
    {
        init();
        
        long previousTime  = System.currentTimeMillis(); 
        long startTime = previousTime;
        latch.countDown();

        while(! isClosed)
        {
            try
            {
                Thread.sleep(sleepTick);
            } catch (InterruptedException ie){}

            long time = System.currentTimeMillis(); 
            float deltaTime = (float) (time -  previousTime);  

            long deltaRead = readCount.getAndSet(0);
            long deltaWrite = writeCount.getAndSet(0); 

            totalReadCount.addAndGet(deltaRead);
            totalWriteCount.addAndGet(deltaWrite);

            float readRate = toMBperSec(deltaRead, deltaTime);
            float writeRate = toMBperSec(deltaWrite, deltaTime);


            System.out.println ("read rate "+ readRate +" MB/sec, write rate "+ writeRate + " MB/sec");
            previousTime = time;

            if (readHistogram != null)
                readHistogram.add(readRate);
            if (writeHistogram != null)
                writeHistogram.add(writeRate); 

            if (duration > 0 && time-startTime > duration)
                break;
        } 

        close();
    } 

    public static float toMBperSec(long byteCount, float timeMillis)
    {
        return ((float) (byteCount / 1024)) / timeMillis;
    }

    public static InetSocketAddress socketAddressFromString(String s)
    {
        if (s == null)
            return null;

        String[] split = s.split(":");
        if (split.length != 2)
            return null;
        String address = split[0].trim();
        int port = Integer.parseInt(split[1].trim());
        return new InetSocketAddress (address, port);
    }

    public static Map<String, String> argMap(String[] args)
    {
        Map<String,String> map = new HashMap<String,String>();

        for (int iArg=0;iArg < args.length; iArg++)
            if (args[iArg].startsWith("-") && iArg < args.length-1)
                map.put(args[iArg].substring(1), args[++iArg]);
        return map; 
    }

    public static String usage()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("IOTester Usage:\n\n");
        sb.append("Two modes : profile TCP performance and profile the file-system performance.\n\nTo use the network:\n");
        sb.append("java -jar IOTester.jar -serverAddress <ServerAddress (default null)> -clientAddresses <Address1,Address2 (default null)> -threadCount <number of client threads to each server address (default 1)> -duration <length of test in seconds (default infinite)> -windowSize <TCP window size (default 64K)> -filePath <path to file for disk I/O (default null)>\n");
        sb.append("eg. java NetworkIOTester -serverAddress 192.168.2.43:1337 -clientAddresses 192.168.2.43:1337,192.168.2.2:1337 -threadCount 5 -duration 120\n");
        sb.append("\nTo use the file-system:\n");
        sb.append("java -jar IOTester.jar -filePath /path/to/file -maxFileSize <Maximum size the file can grow to (default 1MB)> -reading <true/false if true, will readfrom file, if false will write to file (default false)> -randomAccess <true/false (defalut false)> -windowSize <Individual read/write size (default 64K)>\n");
        sb.append("eg. java -jar IOTester.jar -filePath /path/to/file -reading false -maxFileSize 1000000000");
        return sb.toString();
    }
    public long totalReadCount(){return totalReadCount.get();}
    public long totalWriteCount(){return totalWriteCount.get();}
    public synchronized boolean isClosed(){return isClosed;}

    public static void main(String[] args) throws IOException
    {
        Map<String,String> map = argMap(args);
        String s = null;

        float xMin = (s= map.get("xMin")) != null ? Float.parseFloat(s) : 0; 
        float xMax = (s= map.get("xMax")) != null ? Float.parseFloat(s) : 10000; // 5 seconds
        int nBins = (s = map.get("nBins")) != null ? Integer.parseInt(s) : 1000;

        IOTester.readHistogram = new Histogram(nBins, xMin, xMax,"Read","Sampled I/O rate","Frequency");
        IOTester.writeHistogram = new Histogram(nBins, xMin, xMax,"Write","Sampled I/O rate","Frequency");

        IOTester.sleepTick = (s = map.get("tick")) != null ? Integer.parseInt(s) : 100; // 1 second 
        IOTester.histFileName = (s = map.get("histFile")) != null ? s : "hist.txt"; // 1 second 

        int threadCount =(s = map.get("threadCount")) != null ? Integer.parseInt(s) : 1;
        int duration = (s = map.get("duration")) != null ? Integer.parseInt(s) * 1000 : -1;
        int bufferSize = (s = map.get("windowSize")) != null ? Integer.parseInt(s) : DEFAULT_BUFFER_LENGTH;

        InetSocketAddress serverAddress = socketAddressFromString(map.get("serverAddress"));

        InetSocketAddress[] clientAddresses = null;
        String clients = map.get("clientAddresses");
        if (clients != null)
        {
            String[] split = clients.split(",");
            if (split.length > 0)
            {
                clientAddresses = new InetSocketAddress[split.length];
                for (int i=0;i< split.length; i++)
                    clientAddresses[i] = socketAddressFromString(split[i]);
            }
        }


        String filePath =  map.get("filePath");
        if (filePath == null && serverAddress == null && clientAddresses == null)
        {
            System.out.println(usage());
            System.exit(1);
        }


        //
        // Shutdown hook to write histograms 
        //
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
            public void run()
        {

            File f = new File(System.getProperty("user.dir"), IOTester.histFileName);
            try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            try
        {
            writer.write(readHistogram.toString()); 
            writer.write(writeHistogram.toString()); 
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            writer.close();
        }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        }}));



        if (filePath != null)
        {
            
            long maxSize = (s = map.get("maxFileSize")) != null ? Long.parseLong(s) : 0x100000;
            boolean reading = (s = map.get("reading")) != null ? Boolean.parseBoolean(s) : false;
            boolean randomAccess = (s = map.get("randomAccess")) != null ? Boolean.parseBoolean(s) : false;

            StringBuilder sb = new StringBuilder();
            sb.append("Starting Disk I/O test with file "+ filePath +" with duration "+ duration + " ms, I/O operation size "+ bufferSize +" and max file-size "+ maxSize);
            if (randomAccess)
                sb.append(" in random-access mode.");
            else
                sb.append(" in sequential-position mode.");

            System.out.println(sb.toString());

            new Thread(new DiskIOTester(duration, bufferSize, filePath, reading, maxSize, randomAccess)).start();
        }

        if (serverAddress == null && clientAddresses == null)
            return;

        System.out.println("Starting TCP Network test with duration "+ duration + " ms, windowSize "+ bufferSize +" bytes and "+ threadCount +" threads for each client connection.");
        new Thread(new NetworkIOTester(serverAddress, clientAddresses, threadCount, duration, bufferSize)).start();
        }

}
