package im.boddy.iotester;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.io.*;

public class NetworkIOTester extends IOTester 
{

    class Server implements Runnable
    {
        final InetSocketAddress address;
        final ServerSocket ssocket;

        Server(InetSocketAddress address) throws IOException
        {
            this.address = address;
            this.ssocket = new ServerSocket();
            this.ssocket.bind(address);
        }

        public void run()
        {
            while(true)
            {
                try
                {
                    Socket s = ssocket.accept();
                    setSocketParams(s);                    
                    ConnectionHandler handler = new ConnectionHandler(s);
                    System.out.println("Starting handler.");
                    new Thread(handler).start();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    abstract class ConnectionEndPoint implements Runnable
    {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        final byte[] buffer;
        volatile boolean isClosed;

        ConnectionEndPoint(Socket s) throws IOException
        {
            this.socket = s;

            //this.in = new DataInputStream(new BufferedInputStream(s.getInputStream(), bufferSize *2));
            //this.out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream(), bufferSize *2));

            this.in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            this.out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            //this.in = new DataInputStream(s.getInputStream());
            //this.out = new DataOutputStream(s.getOutputStream());

            this.buffer = new byte[bufferSize];
        }

        abstract void doIO() throws IOException;

        void close() 
        {

            isClosed = true;
            try
            {
                socket.close();
            } catch (IOException ioe) {}
        }

        public void run()
        {

            while(! isClosed)
            {
                try
                {
                    doIO(); 
                } catch (SocketException se) {
                    se.printStackTrace();
                    close();
                } catch (EOFException eofe) {
                    eofe.printStackTrace();
                    close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    class ConnectionHandler extends ConnectionEndPoint 
    {
        ConnectionHandler(Socket s) throws IOException
        {
            super(s);
        }

        void doIO() throws IOException
        {
            in.readFully(buffer);    
            readCount.addAndGet(buffer.length); 
        }
    }

    class Client extends ConnectionEndPoint
    {
        Client (Socket s) throws IOException
        {
            super(s);
            random.nextBytes(buffer);
        }

        void doIO() throws IOException
        {
            out.write(buffer);
            out.flush();
            writeCount.addAndGet(buffer.length);
        }
    }

    final Server server;
    final InetSocketAddress[] clientAddresses; 
    final Client[] clients;
    final int threadCount;

    NetworkIOTester(InetSocketAddress serverAddress, InetSocketAddress[] clientAddresses, int threadCount, int duration, int bufferSize) throws IOException
    {
        super(duration, bufferSize);


        this.threadCount = threadCount;
        if (serverAddress == null)
            this.server = null;
        else
            this.server = new Server(serverAddress); 

        if (clientAddresses == null)
        {
            this.clients = null;
            this.clientAddresses = null;
        }
        else
        {
            this.clientAddresses = clientAddresses;
            this.clients = new Client[clientAddresses.length * threadCount];
        }

    }   

    void initClients() 
    {
        for (int iClient=0; iClient < clientAddresses.length; iClient++)
        {
            InetSocketAddress address = clientAddresses[iClient];
            if (address == null)
                continue;
            for (int iThread=0;iThread < threadCount; iThread++)
            {
                Client client = null;
                try
                {
                    Socket s = new Socket();
                    setSocketParams(s);   
                    s.connect(address);   

                    client = new Client(s);
                    clients[iClient * threadCount + iThread] = client; 
                    System.out.println("Starting client to "+ address);
                    new Thread(client).start();
                } catch (Exception e) {
                    e.printStackTrace();
                    client.close();
                }
            }
        }
    } 
    
    public void init()
    {
        if (server != null)
        {
            System.out.println("Starting server.");
            new Thread(server).start();
        }
        if (clients != null)
        {
            System.out.println("Starting clients.");
            initClients();
        }
    }


    public void setSocketParams(Socket s) throws IOException
    {
        s.setReceiveBufferSize(bufferSize);
        s.setSendBufferSize(bufferSize);
        s.setTcpNoDelay(false);

        /*
           int connectionTime = 0;
           int latency = 0;
           int bandwidth = 1;
           s.setPerformancePreferences(connectionTime, latency, bandwidth);
           */
        //System.out.println("Using socket parameters send/receive buffer size :"+ s.getSendBufferSize() +" / "+s.getReceiveBufferSize() +" no-delay ?" + s.getTcpNoDelay());
    }

    public void close()
    {
        System.out.println("Ran for "+ (duration / 1000) +" seconds, exiting.");
        System.exit(0);
    }
}
