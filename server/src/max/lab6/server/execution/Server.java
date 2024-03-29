package max.lab6.server.execution;

import MainPackage.Pair;
import max.lab6.server.collection.filecollection.Collection;
import max.lab6.server.commands.ComandFactory;
import max.lab6.server.commands.Comandable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server implements Runnable {

    private Selector selector;
    private FileWorks.collection.CollectionManager manager;
    private static int id = 0;
    private static Map<Integer, SelectionKey> connections = new HashMap<>();

    public Server(int port) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.bind(new InetSocketAddress("localhost", port));
        channel.configureBlocking(false);
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_ACCEPT);
        manager = new Collection("collection.csv");
        manager.updateCollection();

    }

    @Override
    public void run() {
        while (true) {
            try {

                if (selector.select() == 0) continue;

                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    try {
                        if (key.attachment() == null) {
                            handleNewConnection(key);
                        } else {
                            handleCommand(key);
                        }
                    } catch (IOException e) {
                        key.cancel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleNewConnection(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        int curId = id++;
        connections.put(curId, channel.register(selector, SelectionKey.OP_READ, curId));
    }

    private void handleCommand(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        channel.read(buffer);
        buffer.flip();
        String command = new String(buffer.array(), 0, buffer.limit());
        String clearCommand = command.trim();
        Pair<Comandable, String> pair = ComandFactory.createComand(clearCommand);
        String respond;
        if (pair.getKey() != null) {
            respond = pair.getKey().run(pair.getValue(), manager, (Integer) key.attachment());
        } else {
            if (pair.getValue() == null) {
                respond = "Неверная команда!";
            } else {
                respond = pair.getValue();
            }
        }
        buffer.clear();
        buffer.put(respond.getBytes());
        buffer.flip();
        channel.write(buffer);
    }

/*    private String deleteSpaces(String command) {
        if (command.length() == 0) return command;
        String result = command.replaceAll(" {2,}", " ");
        if (result.charAt(0) == ' ') {
            result = result.substring(1, result.length());
        }
        if (result.length() == 0) return result;
        if (result.charAt(result.length() - 1) == ' ') {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }*/

    public static Map<Integer, SelectionKey> getConnections() {
        return connections;
    }

    public Selector getSelector() {
        return selector;
    }
}
