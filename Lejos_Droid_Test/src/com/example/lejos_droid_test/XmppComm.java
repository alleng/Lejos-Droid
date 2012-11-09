package com.example.lejos_droid_test;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.util.Log;

public class XmppComm extends Thread {
    ControlComm control;
    public boolean isRunning = false;
    XMPPConnection connection;
    String username = "poisonarcher1010@gmail.com";
    String password = "stevenson@9741";
    String sender = "";

    public XmppComm(ControlComm control) {
        this.control = control;
    }

    @Override
    public void run() {
        isRunning = true;
        String host = "talk.google.com";
        String port = "5222";
        String service = "gmail.com";
        // Create a connection
        ConnectionConfiguration connConfig = new ConnectionConfiguration(host,
                Integer.parseInt(port), service);
        connection = new XMPPConnection(connConfig);

        try {
            connection.connect();
            Log.i("XMPPClient", "[SettingsDialog] Connected to " + connection.getHost());
            isRunning = true;
        } catch (XMPPException ex) {
            Log.e("XMPPClient", "[SettingsDialog] Failed to connect to " + connection.getHost());
            Log.e("XMPPClient", ex.toString());
        }

        try {
            connection.login(username, password);
            Log.i("XMPPClient", "Logged in as " + connection.getUser());

            // Set the status to available
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
            if (connection != null) {
                // Add a packet listener to get messages sent to us
                PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
                connection.addPacketListener(new PacketListener() {
                    public void processPacket(Packet packet) {
                        Message message = (Message) packet;
                        if (message.getBody() != null) {
                            sender = message.getFrom();
                            String fromName = StringUtils.parseBareAddress(message.getFrom());
                            Log.i("XMPPClient", "Got text [" + message.getBody() + "] from ["
                                    + fromName + "]");
                            System.out.println(message.getBody());
                            parseMessage(message.getBody());
                        }
                    }
                }, filter);
            }
        } catch (XMPPException ex) {
            Log.e("XMPPClient", "[SettingsDialog] Failed to log in as " + username);
            Log.e("XMPPClient", ex.toString());
        }

        // Message msg = new Message(toAddress, Message.Type.chat);
        // msg.setBody("HELLO WORLD");
        // connection.sendPacket(msg);

    }

    /**
     * Parses message: First command is Header, subsequent are the numbers for
     * use in the float[] data array. An example Message is something like
     * "ROTATE 90" or "DESTINATION 0 20" (obviously without the quotation marks)
     * 
     * @param message
     *            Message received over XMPP
     */
    public void parseMessage(String message) {
        float[] data = { 0, 0, 0 };
        String[] messages;
        messages = message.split("\\s");
        messages[0] = messages[0].toUpperCase();
        int header = Header.valueOf(messages[0]).ordinal();

        for (int i = 1; i < messages.length; i++) {
            int messageInt = Integer.parseInt(messages[i]);
            data[i - 1] = messageInt;
        }
        try {
            control.send(header, data, true);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void sendMessage(int header, float[] data) {
        String message = Header.values()[header].toString();
        message += " " + data[0];
        message += " " + data[1];
        message += " " + data[2];
        Message msg = new Message(sender, Message.Type.chat);
        msg.setBody(message);
        connection.sendPacket(msg);
    }
}