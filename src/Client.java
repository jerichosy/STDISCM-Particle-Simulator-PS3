import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client extends JPanel implements KeyListener {
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    private java.util.List<Particle> particles = new CopyOnWriteArrayList<>(); // Thread-safe ArrayList ideal for occasional writes
    private java.util.List<Sprite> otherSprites = new CopyOnWriteArrayList<>(); // Thread-safe ArrayList ideal for occasional writes
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private long lastTime = System.currentTimeMillis();
    private int frames = 0;
    private String fps = "FPS: 0";
    private String particleCount = "Particle Count: 0";
    private boolean isPaused = false;

    private Sprite sprite = new Sprite(Particle.gridWidth , Particle.gridHeight, UUID.randomUUID().toString());

    private long lastUpdateTime = System.currentTimeMillis();

    private java.util.List<Thread> threads = new ArrayList<>();


    private String serverAddress;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    private DatagramSocket socket;

    public Client(String serverAddress) throws UnknownHostException, SocketException{
        this.serverAddress = serverAddress;

        runUI();
        registerWithServer();
        runServerListener();
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
                    socket = new DatagramSocket(Ports.RES_REP.getPortNumber());

                    Thread listener = new Thread(new Client.FormListener(requests, socket));
                    listener.start();

                    Thread handler = new Thread(new Client.FormHandler(requests, socket, serverAddress, particles));
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
                    synchronized (requests){
                        requests.add(ReqResForm.createFormFromRequest(receivePacket));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
    
    private static class FormHandler implements Runnable{

        private final BlockingQueue<ReqResForm> requests;

        private final DatagramSocket socket;
        private final AtomicBoolean particleSendingGoing = new AtomicBoolean(false);

        private final ExecutorService executor = Executors.newFixedThreadPool(8);

        private final java.util.List<Particle> particleList;

        private final String serverAddress;

        private final List<Particle> tempParticleList;

        public FormHandler(BlockingQueue<ReqResForm> requests, DatagramSocket socket,  String serverAddress, java.util.List<Particle> particles) {
            this.requests = requests;
            this.socket = socket;
            this.serverAddress = serverAddress;
            this.particleList = particles;
        }


        @Override
        public void run() {
            while (true){
                try {
                    ReqResForm form = requests.take();
                    switch (form.getType()){
                        case "new": executor.submit(() -> addNewSpriteToList(form));
                        case "update": executor.submit(() -> performUpdateSpriteList(form));
                        case "particle": executor.submit(() -> performParticleUpdate(form));
                        case "sync_start": executor.submit(() -> syncStart(form));
                        case "sync_end": executor.submit(() -> syncEnd(form));

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

    }


















    private void registerWithServer() throws UnknownHostException, SocketException {

        // Create a DatagramSocket with a random available port
        this.socket = new DatagramSocket();
        int localPort = socket.getLocalPort();

        // Create a 'new' request ReqResForm with the sprite information
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        sprite.setPort(localPort);
        String spriteData = gson.toJson(sprite);
        ReqResForm form = new ReqResForm("new", spriteData);

        // Send the request to the server via Port A
        byte[] sendData = gson.toJson(form).getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(serverAddress), Ports.RES_REP.getPortNumber());
        try {
            socket.send(sendPacket);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    public Client(String serverAddress) throws UnknownHostException, SocketException {
//        this.serverAddress = serverAddress;
//
//        runUI();
//        registerWithServer();
//        receiveUpdatedSprites();
//
//        this.addKeyListener(this);
//        this.setFocusable(true); // Set the JPanel as focusable
//        this.requestFocusInWindow(); // Request focus for the JPanel
//
//        for (Thread thread : threads) {
//            try {
//                thread.join();
//            } catch (InterruptedException e) {
//                // Restore interrupted status
//                Thread.currentThread().interrupt(); // Restore the interrupted status
//                System.err.println("Thread interrupted while waiting for completion: " + e.getMessage());
//            }
//        }
//
//        scheduleParticleUpdate();
//
//    }
//
//    private void scheduleParticleUpdate() {
//        scheduler.scheduleAtFixedRate(this::requestUpdatedParticles, 0, 2, TimeUnit.SECONDS);
//    }
//
//    private void requestUpdatedParticles() {
//        System.out.println("Requesting updated particles from server...");
//
//         fetchUpdatedParticlesFromServer();
//    }
//
//    private void fetchUpdatedParticlesFromServer() {
//        try {
//            DatagramSocket serverSocket;
//            serverSocket = new DatagramSocket(4991);
//
//            byte[] buffer = new byte[2048];
//            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//
////            particles = unpackParticles(packet.getData(), packet.getLength());
//
//        } catch (SocketException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void registerWithServer() throws UnknownHostException, SocketException {
//        // new Sprite(Particle.gridWidth , Particle.gridHeight, UUID.randomUUID().toString());
//
//        // Create a DatagramSocket with a random available port
//        DatagramSocket socket = new DatagramSocket();
//        int localPort = socket.getLocalPort();
//
//        // Create a 'new' request ReqResForm with the sprite information
//        Gson gson = new Gson();
//        sprite.setPort(localPort);
//        String spriteData = gson.toJson(sprite);
//        ReqResForm form = new ReqResForm("new", spriteData);
//
//        // Send the request to the server via Port A
//        byte[] sendData = gson.toJson(form).getBytes();
//        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(serverAddress), Ports.RES_REP.getPortNumber());
//        try {
//            socket.send(sendPacket);
//            socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void receiveUpdatedSprites() {
//        Thread receiverThread = new Thread(() -> {
//            try {
//                DatagramSocket socket = new DatagramSocket();
//                byte[] buffer = new byte[1024];
//
//                while (true) {
//                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                    socket.receive(packet);
//
//                    String receivedData = new String(packet.getData(), 0, packet.getLength());
//                    System.out.println("Received sprite update: " + receivedData);
//
//                    Gson gson = new Gson();
//                    ReqResForm form = gson.fromJson(receivedData, ReqResForm.class);
//
//                    if (form.getType().equals("update")) {
//                        Sprite updatedSprite = gson.fromJson(form.getData(), Sprite.class);
//                        updateLocalSprite(updatedSprite);
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//
//        receiverThread.start();
//    }
//
//    private void updateLocalSprite(Sprite updatedSprite) {
//        System.out.println("Updating sprite: " + updatedSprite);
//
//        if (updatedSprite.getClientId().equals(sprite.getClientId())) {
//            // Update the client's own sprite
//            sprite = updatedSprite;
//        } else {
//            boolean found = false;
//            for (int i = 0; i < otherSprites.size(); i++) {
//                Sprite localSprite = otherSprites.get(i);
//                if (localSprite.getClientId().equals(updatedSprite.getClientId())) {
//                    // TODO: Check if this if-statement is correct
//                    otherSprites.set(i, updatedSprite);
//                    found = true;
//                    break;
//                }
//            }
//            if (!found) {
//                otherSprites.add(updatedSprite);
//            }
//        }
//        repaint();
//    }

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
                    System.out.println(otherSprites.size() + " sprites in the list");
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

//        // Draw a background with dynamic color for the pause state for better readability
//        g.setColor(isPaused ? Color.RED : new Color(0, 255, 0)); // Red if paused, Green if not
//        g.fillRect(5, 55, 150, 20); // Adjust size as needed
//        // Set the color for the pause state text
//        g.setColor(Color.WHITE);
//        g.drawString("Renderer Paused: " + isPaused, 10, 70);

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

//    private void setupControlPanel(JPanel panel) {
//        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//
//        JPanel panelToggle = createPanelForClearAndPause();
//        panel.add(panelToggle);
//    }



//    private JPanel createPanelForClearAndPause(){
//        JPanel panel = new JPanel(new FlowLayout());
//        // Pause btn
//        JButton pauseButton = new JButton("Pause Renderer");
//        pauseButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                togglePause(pauseButton);
//            }
//        });
//        pauseButton.setPreferredSize(new Dimension(140,30));
//        panel.add(pauseButton);
//
//        System.out.println(isPaused);
//
//        return panel;
//    }



//    private void togglePause(JButton pauseButton){
//        isPaused = !isPaused;
//        pauseButton.setText(isPaused ? "Resume Renderer" : "Pause Renderer"); // Update button text based on pause state
//        repaint();  // This is needed for the pause state to be updated on the screen
////
//        if (!isPaused){
//            this.setFocusable(true); // Set the JPanel as focusable
//            this.requestFocusInWindow(); // Request focus for the JPanel
//        }
//    }






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

                //            // Setup and add the control panel at the top
                //            JPanel controlPanel = new JPanel();
                //            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
                //            simulatorGUI.setupControlPanel(controlPanel);


                //
                //            simulatorGUI.setFocusable(true);
                //            simulatorGUI.requestFocusInWindow();
                containerPanel.add(simulatorGUI);

                //            // Add the control panel and simulatorGUI to the containerPanel
                //            containerPanel.add(controlPanel);


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
        int displacement = 10;

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

//            repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }


}
