
//package ru.bloof.NatTester;
// Определяем тим нат
// https://habr.com/company/oleg-bunin/blog/428217/
// Если у вас открытый Full cone NAT, то в ответах STUN сервер будет один и тот же порт. При Restricted cone NAT у вас на каждый запрос к STUN будут приходить разные порты.

import org.ice4j.StunException;
import org.ice4j.TransportAddress;
import org.ice4j.attribute.Attribute;
import org.ice4j.attribute.XorMappedAddressAttribute;
import org.ice4j.message.Message;
import org.ice4j.message.MessageFactory;
import org.ice4j.message.Request;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Random;

public class NatTester {
    private static final InetSocketAddress TEST1 = new InetSocketAddress("videostun1.mycdn.me", 80);
    private static final InetSocketAddress TEST2 = new InetSocketAddress("videostun1.mycdn.me", 3478);
    private static final InetSocketAddress TEST3 = new InetSocketAddress("videostun2.mycdn.me", 80);
    private static final InetSocketAddress TEST4 = new InetSocketAddress("videostun2.mycdn.me", 3478);

    private static final Random rnd = new Random();
    private static final byte[] buf = new byte[65536];
    private static final DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);

    public static void main(String[] args) throws Exception {
        int port1 = rnd.nextInt(10000) + 20000;
        int port2 = rnd.nextInt(10000) + 40000;
        System.out.println("port1=" + port1 + " port2=" + port2);
        try (DatagramSocket ds = new DatagramSocket(port1); DatagramSocket ds2 = new DatagramSocket(port2)) {
            ds.setSoTimeout(5000);
            ds2.setSoTimeout(5000);
            System.out.println(sendRequest(ds, TEST1));
            System.out.println(sendRequest(ds, TEST2));
            System.out.println(sendRequest(ds, TEST3));
            System.out.println(sendRequest(ds, TEST4));
            System.out.println(sendRequest(ds2, TEST1));
            System.out.println(sendRequest(ds2, TEST2));
            System.out.println(sendRequest(ds2, TEST3));
            System.out.println(sendRequest(ds2, TEST4));
        }
    }

    private static TransportAddress sendRequest(DatagramSocket socket, InetSocketAddress address)
            throws StunException, IOException {
        Request request = MessageFactory.createBindingRequest();
        byte[] tId = new byte[12];
        rnd.nextBytes(tId);
        request.setTransactionID(tId);
        byte[] data1 = request.encode(null);
        socket.send(new DatagramPacket(data1, data1.length, address));
        socket.receive(recvPacket);
        Message decoded = Message.decode(recvPacket.getData(), (char) recvPacket.getOffset(),
                (char) recvPacket.getLength());
        if (decoded.getMessageType() != Message.BINDING_SUCCESS_RESPONSE
                || !Arrays.equals(tId, decoded.getTransactionID())) {
            return null;
        }
        for (Attribute attr : decoded.getAttributes()) {
            if (attr instanceof XorMappedAddressAttribute) {
                return ((XorMappedAddressAttribute) attr).getAddress(tId);
            }
        }
        return null;
    }
}