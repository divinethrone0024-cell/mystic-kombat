package mystic.net;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetSession implements Closeable {
    public static final int PORT = 7777;
    public static final int DELAY = 3; // input delay in frames

    public final boolean isHost;
    public volatile boolean connected = true;
    public volatile boolean desync = false;
    public volatile long lastPingMs = 0;

    // match setup (host fills, client receives)
    public volatile int p1Char, p2Char, arena;
    public volatile long seed;
    public volatile boolean setupReceived = false;

    private final Socket sock;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final ConcurrentHashMap<Integer, Integer> remoteInputs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> localInputs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> remoteHashes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> localHashes = new ConcurrentHashMap<>();

    private NetSession(Socket s, boolean host) throws IOException {
        sock = s;
        isHost = host;
        s.setTcpNoDelay(true);
        in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        for (int f = 0; f < DELAY; f++) {
            localInputs.put(f, 0);
            remoteInputs.put(f, 0);
        }
        Thread t = new Thread(this::readLoop, "net-reader");
        t.setDaemon(true);
        t.start();
    }

    // ---- connect ----
    public static NetSession host() throws IOException { // blocks until client joins
        try (ServerSocket ss = new ServerSocket(PORT)) {
            return new NetSession(ss.accept(), true);
        }
    }

    public static NetSession join(String ip) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(ip, PORT), 5000);
        return new NetSession(s, false);
    }

    // ---- send ----
    public synchronized void sendSetup(int p1, int p2, int arenaId, long seed) {
        try {
            out.writeByte(0);
            out.writeInt(p1);
            out.writeInt(p2);
            out.writeInt(arenaId);
            out.writeLong(seed);
            out.flush();
        } catch (IOException e) {
            connected = false;
        }
    }

    /** call once per frame with this frame's local input bitmask */
    public synchronized void sendInput(int frame, int bits) {
        int target = frame + DELAY;
        localInputs.put(target, bits);
        try {
            out.writeByte(1);
            out.writeInt(target);
            out.writeInt(bits);
            out.flush();
        } catch (IOException e) {
            connected = false;
        }
    }

    public synchronized void sendHash(int frame, long hash) {
        localHashes.put(frame, hash);
        Long theirs = remoteHashes.get(frame);
        if (theirs != null && theirs != hash)
            desync = true;
        try {
            out.writeByte(2);
            out.writeInt(frame);
            out.writeLong(hash);
            out.flush();
        } catch (IOException e) {
            connected = false;
        }
    }

    // ---- read ----
    private void readLoop() {
        try {
            while (true) {
                int type = in.readUnsignedByte();
                if (type == 0) {
                    p1Char = in.readInt();
                    p2Char = in.readInt();
                    arena = in.readInt();
                    seed = in.readLong();
                    setupReceived = true;
                } else if (type == 1) {
                    int f = in.readInt();
                    remoteInputs.put(f, in.readInt());
                } else if (type == 2) {
                    int f = in.readInt();
                    long h = in.readLong();
                    remoteHashes.put(f, h);
                    Long mine = localHashes.get(f);
                    if (mine != null && mine != h)
                        desync = true;
                }
            }
        } catch (IOException e) {
            connected = false;
        }
    }

    // ---- query ----
    public boolean ready(int frame) {
        return remoteInputs.containsKey(frame) && localInputs.containsKey(frame);
    }

    public int local(int frame) {
        return localInputs.remove(frame);
    }

    public int remote(int frame) {
        return remoteInputs.remove(frame);
    }

    @Override
    public void close() {
        connected = false;
        try {
            sock.close();
        } catch (IOException ignored) {
        }
    }
}