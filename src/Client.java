import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.*;
import java.util.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Client extends JPanel implements KeyListener {
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    private List<Particle> particles = new CopyOnWriteArrayList<>(); // Thread-safe ArrayList ideal for occasional writes
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private long lastTime = System.currentTimeMillis();
    private int frames = 0;
    private String fps = "FPS: 0";
    private String particleCount = "Particle Count: 0";
    private String playerCount = "Player Count: 0";
    private boolean isPaused = false;

    private Sprite sprite = new Sprite(Particle.gridWidth , Particle.gridHeight, UUID.randomUUID().toString());

    private ConcurrentHashMap<String, Sprite> otherClients = new ConcurrentHashMap<>();

    private long lastUpdateTime = System.currentTimeMillis();

    private List<Thread> threads = new ArrayList<>();


    private String serverAddress;



    private DatagramSocket socket;

    private int localPort;

    private InetAddress address;

    public Client(String serverAddress) throws UnknownHostException, SocketException{
        this.serverAddress = serverAddress;

        runUI();
        registerWithServer();
        runServerListener();

//        receiveUpdatedSprites();

        this.addKeyListener(this);
        this.setFocusable(true); // Set the JPanel as focusable
        this.requestFocusInWindow(); // Request focus for the JPanel

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // Restore interrupted status
                Thread.currentThread().interrupt(); // Restore the interrupted status
                System.err.println("Thread interrupted while waiting for completion: " + e.getMessage());
            }
        }
    }
    private void runServerListener(){
        Thread listener = new Thread(new Runnable() {
            private final BlockingQueue<ReqResForm> requests = new LinkedBlockingQueue<>();

            @Override
            public void run() {
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket(localPort, address);

                    Thread listener = new Thread(new Client.FormListener(requests, socket));
                    listener.start();

                    Thread handler = new Thread(new Client.FormHandler(requests, socket, serverAddress, particles, otherClients));
                    handler.start();


                } catch (SocketException e) {
                    e.printStackTrace();
                }

            }
        });
        threads.add(listener);
        listener.start();
    }

    private static class FormListener implements Runnable{

        private final BlockingQueue<ReqResForm> requests;
        private DatagramSocket socket;

        public FormListener(BlockingQueue<ReqResForm> requests, DatagramSocket socket) {
            this.requests = requests;
            this.socket = socket;
        }

        @Override
        public void run() {
            while (true){
                try {
                    byte[] receiveBuffer = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);
                    requests.put(ReqResForm.createFormFromRequest(receivePacket));
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
    
    private static class FormHandler implements Runnable {

        private final BlockingQueue<ReqResForm> requests;

        private final DatagramSocket socket;
        private final AtomicBoolean particleSendingGoing = new AtomicBoolean(false);

        private final ExecutorService executor = Executors.newFixedThreadPool(8);

        private final List<Particle> particleList;

        private final String serverAddress;

        private List<Particle> tempParticleList;

        private ConcurrentHashMap<String, Sprite> otherClients;

        private AtomicInteger particlesSize = new AtomicInteger(0);
        private AtomicInteger particlesNum = new AtomicInteger(0);

        public FormHandler(BlockingQueue<ReqResForm> requests, DatagramSocket socket, String serverAddress, List<Particle> particles, ConcurrentHashMap<String, Sprite> otherClients ) {
            this.requests = requests;
            this.socket = socket;
            this.serverAddress = serverAddress;
            this.particleList = particles;
            this.tempParticleList = new CopyOnWriteArrayList<>();
            this.otherClients = otherClients;
        }


        @Override
        public void run() {
            while (true) {
                try {
                    ReqResForm form = requests.take();
                    System.out.println("Handling form type = " + form.getType());
                    switch (form.getType()) {
                        case "new":
                            executor.submit(() -> addNewSpriteToList(form));
                            break;
                        case "update":
                            executor.submit(() -> performUpdateSpriteList(form));
                            break;
                        case "remove":
                            executor.submit(() -> removeClientSprite(form));
                            break;
                        case "particle":
                            executor.submit(() -> performParticleUpdate(form));
                            break;
                        case "sync_start":
                            executor.submit(() -> syncStart(form));
                            break;
                        case "sync_end":
                            executor.submit(() -> syncEnd(form));
                            break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        private void removeClientSprite(ReqResForm form) {
            String data = form.getData();
            Gson gson = new Gson();
            Sprite newSprite = gson.fromJson(data, Sprite.class);


            otherClients.remove(newSprite.getClientId());
        }

        private void performUpdateSpriteList(ReqResForm form) {
            Gson gson = new Gson();
            Sprite updatedSprite = gson.fromJson(form.getData(), Sprite.class);

            // Update the otherSprites list with the received sprite information
            otherClients.put(updatedSprite.getClientId(), updatedSprite);
        }

        private void performParticleUpdate(ReqResForm form) {
            Gson gson = new Gson();
            Particle particle = gson.fromJson(form.getData(), Particle.class);
            tempParticleList.add(particle);
        }

        private void addNewSpriteToList(ReqResForm form) {
            String data = form.getData();
            Gson gson = new Gson();
            Sprite newSprite = gson.fromJson(data, Sprite.class);

            otherClients.put(newSprite.getClientId(), newSprite);
        }

        private void syncStart(ReqResForm form) {
            tempParticleList.clear();

            // Create a Gson object
            Gson gson = new Gson();

            // Parse the JSON string and convert it to an integer
            int size = gson.fromJson(form.getData(), int.class);
            particlesSize.set(size);
        }

        private void syncEnd(ReqResForm form) {

            particlesNum.set(0);
            particleList.clear();
            particleList.addAll(tempParticleList);

        }

    }

    private void registerWithServer() throws UnknownHostException, SocketException {

        // Create a DatagramSocket with a random available port
        this.socket = new DatagramSocket();
        localPort = socket.getLocalPort();
        address = socket.getLocalAddress();

        System.out.println("Local address: " + address);
        System.out.println("Local port: " + localPort);
        System.out.println("ID: " + sprite.getClientId());

        // Create a 'new' request ReqResForm with the sprite information
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        sprite.setPort(localPort);
        String spriteData = gson.toJson(sprite);
        ReqResForm form = new ReqResForm("new", spriteData);

        System.out.println("Client Sprite data: " + spriteData);

        // Send the request to the server via Port A
        byte[] sendData = gson.toJson(form).getBytes();
        InetAddress address = InetAddress.getByName(serverAddress);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, Ports.RES_REP.getPortNumber());
        try {
            socket.send(sendPacket);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runUI(){
        Thread uiThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Swing Timer for animation
                // Analogy: This is just like specifying an FPS limit in a video game instead of uncapped FPS.
                new Timer(13, e -> updateAndRepaint()).start(); // ~60 FPS

                // Timer to update FPS counter every 0.5 seconds
                new Timer(500, e -> {
                    long currentTime = System.currentTimeMillis();
                    long delta = currentTime - lastTime;
                    fps = String.format("FPS: %.1f", frames * 1000.0 / delta);
                    //System.out.println(frames + " frames in the last " + delta + " ms");
                    frames = 0; // Reset frame count
                    lastTime = currentTime;
                }).start();

                sprite.setWillSpawn(true);
                sprite.printPosition();
            }
        });
        threads.add(uiThread);
        uiThread.start();

    }

    private void updateAndRepaint() {
        if (!isPaused) {
            long currentTime = System.currentTimeMillis();
            double deltaTime = (currentTime - lastUpdateTime) / 1000.0; // Time in seconds
            lastUpdateTime = currentTime;

            // Submit each particle's run method for parallel execution
            particles.forEach(particle -> executor.submit(() -> particle.update(deltaTime))); // At 60k particles, this takes ~3ms

            // Update particle count string with the current size of the particles list
            particleCount = "Particle Count: " + particles.size();

            // Update player count string with the current size of the otherSprites list
            playerCount = "Other Sprites Count: " + otherClients.size();

            repaint(); // Re-draw GUI with updated particle positions
        }
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw a thin border on the canvas
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        for (Particle particle : particles) {
            particle.draw(g, sprite.getX(), sprite.getY(), sprite.getExcessX(), sprite.getExcessY()); // Let each particle draw itself
        } // At 60k particles, this takes 110-120ms

        for (Sprite otherSprite : otherClients.values()) {
            otherSprite.drawOtherClient(g, sprite.getX(), sprite.getY(), sprite.getExcessX(), sprite.getExcessY());
        }


        frames++; // Increment frame count


        if (sprite.isWillSpawn()) {
            int excessX = sprite.getExcessX() * Particle.gridHeight ;
            int excessY = sprite.getExcessY() * Particle.gridHeight ;

            // Fill the areas outside the visible canvas with black color
            if (excessY < 0) {
                g.fillRect(0, 0, getWidth(), -excessY);
            }
            if (excessX < 0) {
                g.fillRect(0, 0, -excessX + 16, getHeight());
            }
            if (excessY > 0) {
                g.fillRect(0, getHeight() - excessY - 32, getWidth(), excessY);
            }
            if (excessX > 0) {
                g.fillRect(getWidth() - excessX - 8, 0, excessX, getHeight());
            }

            sprite.drawClient(g);
        }

        // Draw a semi-transparent background for the FPS counter for better readability
        g.setColor(new Color(0, 0, 0, 128)); // Black with 50% opacity
        g.fillRect(5, 5, 100, 20); // Adjust the size according to your needs
        // Set the color for the FPS text
        g.setColor(Color.WHITE); // White color for the text
        g.drawString(fps, 10, 20); // Draw FPS counter on screen

        // Draw a semi-transparent background for the Particle Count counter for better readability
        g.setColor(new Color(0, 0, 0, 128)); // Black with 50% opacity
        g.fillRect(5, 30, 150, 20); // Adjust the size according to your needs
        // Set the color for the Particle Count text
        g.setColor(Color.WHITE); // White color for the text
        g.drawString(particleCount, 10, 45); // Draw Particle Count on screen


        // Draw a semi-transparent background for the Sprite position coords for better readability
        g.setColor(new Color(0, 0, 0, 128)); // Black with 50% opacity
        g.fillRect(5, 55, 150, 20); // Adjust size as needed
        // Set the color for the Sprite position coords text
        g.setColor(Color.WHITE);
        g.drawString(playerCount, 10, 70);

        // Draw a semi-transparent background for the Developer/Explorer mode for better readability
        g.setColor(new Color(0, 0, 0, 128)); // Black with 50% opacity
        g.fillRect(5, 85, 150, 20); // Adjust size as needed
        // Set the color for the Developer/Explorer mode text
        g.setColor(Color.WHITE);
        g.drawString("Explorer Mode", 10, 100);


        // Draw a semi-transparent background for the Sprite position coords for better readability
        g.setColor(new Color(0, 0, 0, 128)); // Black with 50% opacity
        g.fillRect(5, 110, 150, 20); // Adjust size as needed
        // Set the color for the Sprite position coords text
        g.setColor(Color.CYAN);
        String spriteCoordinates = String.format("Sprite X: %d, Y: %d", sprite.getX(), WINDOW_HEIGHT - sprite.getY());
        g.drawString(spriteCoordinates, 10, 125);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String serverAddress = JOptionPane.showInputDialog("Enter the server's IP address:");
            if (serverAddress != null && !serverAddress.isEmpty()) {

                JFrame frame = new JFrame("Particle Simulator");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                // Disable default button action
                frame.getRootPane().setDefaultButton(null);

                // Create a container panel with BoxLayout along the Y_AXIS
                JPanel containerPanel = new JPanel();
                containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));

                Client simulatorGUI = null;
                try {
                    simulatorGUI = new Client(serverAddress);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                } catch (SocketException e) {
                    throw new RuntimeException(e);
                }
                simulatorGUI.setPreferredSize(new Dimension(Client.WINDOW_WIDTH, Client.WINDOW_HEIGHT));
                containerPanel.add(simulatorGUI);


                frame.add(containerPanel);  // Add the containerPanel to the frame


                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setResizable(false);
                frame.setVisible(true);
            } else {
                System.out.println("Server address not provided. Exiting.");
                System.exit(0);
            }
        });
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        int displacement = 1;

        if(sprite != null) {
            switch (keyCode){
                case KeyEvent.VK_UP:
                    if(sprite.getY() > 0){
                        //sprite.move(0, -displacementY);
                        sprite.updatePosition(0, -displacement);
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if(sprite.getY() < WINDOW_HEIGHT){
                        //sprite.move(0, displacementY);
                        sprite.updatePosition(0, displacement);
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    if(sprite.getX() > 0){
                        //sprite.move(-displacementX, 0);
                        sprite.updatePosition(-displacement, 0);
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if(sprite.getX() < WINDOW_WIDTH) {
                        //sprite.move(displacementX, 0);
                        sprite.updatePosition(displacement, 0);
                    }
                    break;
            }

//            if(!otherClients.isEmpty()){
//                sprite.printPosition();
//                for (Sprite client: otherClients.values()){
//                    client.printPosition();
//                }
//            }

        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }


}
