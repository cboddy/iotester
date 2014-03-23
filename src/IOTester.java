package com.boddy.iotester;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.io.*;

public abstract class IOTester implements Runnable
{
    static final int BUFFER_LENGTH = 64*1024;
    static Histogram readHistogram, writeHistogram;
    static String histFileName;
    static int sleepTick;

    protected final AtomicLong readCount, writeCount;
    protected final int duration, bufferSize;
    protected final Random random = new Random();
    public IOTester(int duration, int bufferSize)
    {
        this.readCount = new AtomicLong();
        this.writeCount = new AtomicLong();

        this.duration = duration;
        this.bufferSize = bufferSize;
    }

    abstract void init();

    abstract void close();

    public void run()
    {
        init();

        long previousTime  = System.currentTimeMillis(); 
        long startTime = previousTime;

        while(true)
        {
            try
            {
                Thread.sleep(sleepTick);
            } catch (InterruptedException ie){}

            long time = System.currentTimeMillis(); 
            float deltaTime = (float) (time -  previousTime);  

            float readRate = toMBperSec(readCount, deltaTime);
            float writeRate = toMBperSec(writeCount, deltaTime);

            System.out.println ("read rate "+ readRate +" Mb/sec, write rate "+ writeRate + " Mb/sec");
            previousTime = time;

            readHistogram.add(readRate);
            writeHistogram.add(writeRate); 

            if (duration > 0 && time-startTime > duration)
                break;
        } 

        close();
    } 

    public static float toMBperSec(AtomicLong byteCount, float timeMillis)
    {
        return ((float) (byteCount.getAndSet(0) / 1024)) / timeMillis;
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

    public static void main(String[] args) throws IOException
    {
        Map<String,String> map = argMap(args);
        String s = null;

        InetSocketAddress serverAddress = socketAddressFromString(map.get("serverAddress"));

        int threadCount =(s = map.get("threadCount")) != null ? Integer.parseInt(s) : 1;
        int duration = (s = map.get("duration")) != null ? Integer.parseInt(s) * 1000 : -1;
        int bufferSize = (s = map.get("windowSize")) != null ? Integer.parseInt(s) : BUFFER_LENGTH;

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



        float xMin = (s= map.get("xMin")) != null ? Float.parseFloat(s) : 0; 
        float xMax = (s= map.get("xMax")) != null ? Float.parseFloat(s) : 10000; // 5 seconds
        int nBins = (s = map.get("nBins")) != null ? Integer.parseInt(s) : 1000;


        IOTester.readHistogram = new Histogram(nBins, xMin, xMax);
        IOTester.readHistogram.setTitles("read hist","time","rate");
        IOTester.writeHistogram = new Histogram(nBins, xMin, xMax);
        IOTester.writeHistogram.setTitles("write hist","time","rate");

        IOTester.sleepTick = (s = map.get("tick")) != null ? Integer.parseInt(s) : 100; // 1 second 
        IOTester.histFileName = (s = map.get("histFile")) != null ? s : "hist.txt"; // 1 second 


        if (filePath != null)
        {
            long maxSize = (s = map.get("maxFileSize")) != null ? Long.parseLong(s) : 0x100000;
            boolean reading = (s = map.get("reading")) != null ? Boolean.parseBoolean(s) : false;
            boolean randomAccess = (s = map.get("randomAccess")) != null ? Boolean.parseBoolean(s) : false;

            new Thread(new DiskIOTester(duration, bufferSize, filePath, reading, maxSize, randomAccess)).start();
        }




        if (serverAddress == null && clientAddresses == null)
            return;

        System.out.println("Starting with duration "+ duration + " ms, windowSize "+ bufferSize +" bytes and "+ threadCount +" threads for each client connection.");
        new Thread(new NetworkIOTester(serverAddress, clientAddresses, threadCount, duration, bufferSize)).start();

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

    }

}
