package im.boddy.iotester.unit_tests;

import im.boddy.iotester.*;

import java.util.*;
import java.net.*;
import java.io.*;

import static org.junit.Assert.*;
import org.junit.*;

public class NetworkIOTests
{
    private static final int DEFAULT_PORT = 6666;
    private static final int THREAD_COUNT = 5;
    private static final int DURATION_MS = 10000;

    @Test public void IOTest() throws IOException
    {
        InetSocketAddress serverAddress = new InetSocketAddress("localhost", DEFAULT_PORT);
        InetSocketAddress[] clientAddresses = {serverAddress};

        NetworkIOTester tester = new NetworkIOTester(serverAddress, clientAddresses, THREAD_COUNT, DURATION_MS, IOTester.DEFAULT_BUFFER_LENGTH);
        new Thread(tester).start();

        while(! tester.isClosed())
            try
            {
                Thread.sleep(500);
            } catch (InterruptedException ie){}


        float deltaCount = Math.abs((float) tester.totalReadCount() / (float) tester.totalWriteCount() - 1.f);

        assertTrue("I/O accounting", deltaCount < 1.e-3f);

    }



}
