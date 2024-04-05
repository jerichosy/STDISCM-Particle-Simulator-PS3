import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server extends JPanel {
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    private List<Particle> particles = new CopyOnWriteArrayList<>(); // Thread-safe ArrayList ideal for occasional writes
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private long lastTime = System.currentTimeMillis();
    private int frames = 0;
    private String fps = "FPS: 0";
    private String particleCount = "Particle Count: 0";
    private String clientCount = "Client connected: 0";
    private boolean isPaused = false;

    private long lastUpdateTime = System.currentTimeMillis();

    private List<Thread> threads = new ArrayList<>();


    private Map<String, Sprite> clients = new HashMap<String, Sprite>();
    private Map<ClientKey, String> clientAddresses = new HashMap<>();


    public Server() {

        runUI();
        runClientListener();


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

    private void runClientListener(){
        Thread listener = new Thread(new Runnable() {
            private final BlockingQueue<ReqResForm> requests = new LinkedBlockingQueue<>();

            @Override
            public void run() {
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket(Ports.RES_REP.getPortNumber());

                    Thread listener = new Thread(new FormListener(requests, socket));
                    listener.start();
                  
                  Thread handler = new Thread(new FormHandler(requests, socket, clients, clientAddresses, particles));
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
      
        private final Map<String, Sprite> clients;
        private final Map<ClientKey, String> clientAddresses;
      private final List<Particle> particleList;
      
      public FormHandler(BlockingQueue<ReqResForm> requests, DatagramSocket socket, Map<String, Sprite> clients, Map<ClientKey, String> clientAddresses, List<Particle> particles) {
            this.requests = requests;
            this.socket = socket;
            this.clients = clients;
            this.clientAddresses = clientAddresses;
            this.particleList = particles;
        }


        @Override
        public void run() {
            while (true){
                try {
                    ReqResForm form = requests.take();
                    switch (form.getType()){
                        case "new": executor.submit(() -> performNewClientProcedure(form));
                        case "update_sprite": executor.submit(() -> performUpdateSprite(form));
                        case "synch": executor.submit(() -> performSynchParticles(form));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        private void performSynchParticles(ReqResForm form) {
            if (!particleSendingGoing.compareAndSet(false, true)) {
                try {
                    // Create a Gson object
                    Gson gson = new GsonBuilder()
                            .excludeFieldsWithoutExposeAnnotation()
                            .create();

                    // Submit particle synchronization tasks using ExecutorService
                    for (Particle particle : this.particleList) {
                        executor.submit(() -> {
                            try {
                                String data = gson.toJson(particle);
                                String jsonString = gson.toJson(new ReqResForm("synch", data));
                                byte[] sendData = jsonString.getBytes();
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, form.getAddress(), Ports.FOR_PARTICLE.getPortNumber());
                                socket.send(sendPacket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } finally {
                    particleSendingGoing.set(false);
                }
            } else {
                // If particle sending is already in progress, add the form back to the queue for later processing
                requests.add(form);
            }
        }




        private void performUpdateSprite(ReqResForm form) {


//            // Extract the updated sprite information from the form data
//            Gson gson = new Gson();
//            Sprite updatedSprite = gson.fromJson(form.getData(), Sprite.class);
//
//            // Update the sprite in the clients map
//            clients.put(updatedSprite.getClientId(), updatedSprite);
//
//            // Broadcast the updated sprite information to all connected clients
//            broadcastUpdatedSprite(updatedSprite);
        }

        private void broadcastUpdatedSprite(Sprite updatedSprite) {




//            // Create an 'update' response ReqResForm with the updated sprite information
//            Gson gson = new Gson();
//            String spriteData = gson.toJson(updatedSprite);
//            ReqResForm responseForm = new ReqResForm("update", spriteData);
//            byte[] sendData = gson.toJson(responseForm).getBytes();
//
//            System.out.println("Broadcasting updated sprite to all clients");
//
//            // Send the updated sprite information to all connected clients except the one that sent the update
//            for (Map.Entry<String, Sprite> entry : clients.entrySet()) {
//                String clientId = entry.getKey();
//                System.out.println("Broadcasting to Client ID: " + clientId);
//                if (!clientId.equals(updatedSprite.getClientId())) {
//                    ClientKey clientKey = getClientKey(clientId);
//                    System.out.println("Client Key: " + clientKey);
//                    Sprite sprite = entry.getValue();
//                    if (clientKey != null) {
//                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientKey.address, sprite.getPort());
//                        try {
//                            socket.send(sendPacket);
//                            System.out.println("Sent sprite update to client: " + clientId);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
        }

        private ClientKey getClientKey(String clientId) {
            for (Map.Entry<ClientKey, String> entry : clientAddresses.entrySet()) {
                if (entry.getValue().equals(clientId)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        private void performNewClientProcedure(ReqResForm form) {

            // Extract the sprite information from the form data
            Gson gson = new Gson();
            Sprite sprite = gson.fromJson(form.getData(), Sprite.class);


            // TODO: add a request='synch' to requestsQueue

            // TODO: send a response='update' to that client
            responseUpdateSprites(sprite.getClientId());

            // TODO: send a response='new' to other clients

                //TODO: get all clients
            broadcastNewSprite(sprite, );


            // Add the sprite to the clients HashMap using the client's address as the key
            clients.put(sprite.getClientId(), sprite);
//
            // Add the client's address and port to the clientAddresses HashMap
            ClientKey clientKey = new ClientKey(form.getAddress(), sprite.getPort()); // Use the assigned port number
            clientAddresses.put(clientKey, sprite.getClientId());

//            // Send a response to the client to confirm the registration
//            String responseData = gson.toJson("OK");
//            ReqResForm responseForm = new ReqResForm("new", responseData);
//            byte[] sendData = gson.toJson(responseForm).getBytes();
//            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, form.getAddress(), form.getPort());
//            try {
//                socket.send(sendPacket);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        private void broadcastNewSprite(Sprite sprite, List<Sprite> clientSprites) {

            // TODO: convert sprite to string data
            // TODO: create a reqresform type='new' data=data
            // TODO: convert reqresform to string JSON
            // TODO: send that JSON to all clients

        }

        private void responseUpdateSprites(String clientID) {
          // TODO: get all sprites from clients as a list

          // TODO: convert list to JSON via GSON

            // TODO: send list back to the requesting client

        }


    }

    private static class ClientKey {
        private InetAddress address;
        private int port;

        public ClientKey(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
      
      @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ClientKey other = (ClientKey) obj;
            return port == other.port && Objects.equals(address, other.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, port);
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
//                    System.out.println(frames + " frames in the last " + delta + " ms");
                    System.out.println(clients.values());
                    frames = 0; // Reset frame count
                    lastTime = currentTime;
                }).start();
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

            // Update client count string with the current size of the client hashmap
            clientCount = "Clients connected: " + clients.size();

            repaint(); // Re-draw GUI with updated particle positions
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (Particle particle : particles) {
            particle.draw(g); // Let each particle draw itself
        } // At 60k particles, this takes 110-120ms

        // Render each client's sprite
        for (Sprite sprite : clients.values()) {
            sprite.drawServer(g); // call the method to draw it from the server's perspective
        }


        frames++; // Increment frame count

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

        // Draw a semi-transparent background for the Client Count counter for better readability
        g.setColor(new Color(0, 0, 0, 128)); // Black with 50% opacity
        g.fillRect(5, 55, 150, 20); // Adjust the size according to your needs

        // Set the color for the Particle Count text
        g.setColor(Color.WHITE); // White color for the text
        g.drawString(clientCount, 10, 70); // Draw Client Count on screen

//        // Display pause state with dynamic color
//        g.setColor(isPaused ? Color.RED : new Color(0, 255, 0)); // Red if paused, Green if not
//        g.fillRect(5, 60, 150, 20); // Adjust size as needed
//
//        // display pause state
//        g.setColor(Color.WHITE);
//        g.drawString("Renderer Paused: " + isPaused, 10, 75);
    }

    public void addParticlesLinear(int n, Point startPoint, Point endPoint, double velocity, double angle) {
        double deltaX = (double)(endPoint.x - startPoint.x) / (n - 1);
        double deltaY = (double)(endPoint.y - startPoint.y) / (n - 1);

        for (int i = 0; i < n; i++) {
            int x = startPoint.x + (int)(i * deltaX);
            int y = startPoint.y + (int)(i * deltaY);
            particles.add(new Particle(x, y, velocity, angle, WINDOW_HEIGHT));
        }
    }

    public void addParticlesAngular(int n, Point startPoint, double velocity, double startAngle, double endAngle) {
        double deltaAngle = (endAngle - startAngle) / (n - 1);

        for (int i = 0; i < n; i++) {
            double angle = startAngle + i * deltaAngle;
            particles.add(new Particle(startPoint.x, startPoint.y, velocity, angle, WINDOW_HEIGHT));
        }
    }

    public void addParticlesVelocity(int n, Point startPoint, double startVelocity, double endVelocity, double angle) {
        double deltaVelocity = (endVelocity - startVelocity) / (n - 1);

        for (int i = 0; i < n; i++) {
            double velocity = startVelocity + i * deltaVelocity;
            particles.add(new Particle(startPoint.x, startPoint.y, velocity, angle, WINDOW_HEIGHT));
        }
    }


    private void setupControlPanel(JPanel panel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Section for Adding Linear Particles
        JPanel panelLinear = createPanelForLinearParticles();
        panel.add(panelLinear);

        // Section for Adding Angular Distribution Particles
        JPanel panelAngular = createPanelForAngularParticles();
        panel.add(panelAngular);

        // Section for Adding Velocity Distribution Particles
        JPanel panelVelocity = createPanelForVelocityParticles();
        panel.add(panelVelocity);

        JPanel panelToggle = createPanelForClearAndPause();
        panel.add(panelToggle);

    }

//    private void togglePause(JButton pauseButton){
//        isPaused = !isPaused;
//        pauseButton.setText(isPaused ? "Resume Renderer" : "Pause Renderer"); // Update button text based on pause state
//        repaint();
//    }


    private void clearParticles(){
        particles.clear();
        repaint();
    }

    // TODO: Add input validation
    private JPanel createPanelForLinearParticles() {
        JPanel panel = new JPanel(new FlowLayout());

        JTextField nField = new JTextField("200", 5);
        JTextField startXField = new JTextField("0", 5);
        JTextField startYField = new JTextField("300", 5);
        JTextField endXField = new JTextField("10", 5);
        JTextField endYField = new JTextField("400", 5);
        JTextField velocityField = new JTextField("2000", 5);
        JTextField angleField = new JTextField("0", 5);
        JButton addButton = new JButton("Add Linear");

        addButton.addActionListener(e -> {
            int n = Integer.parseInt(nField.getText());
            Point startPoint = new Point(Integer.parseInt(startXField.getText()), Integer.parseInt(startYField.getText()));
            Point endPoint = new Point(Integer.parseInt(endXField.getText()), Integer.parseInt(endYField.getText()));
            double velocity = Double.parseDouble(velocityField.getText());
            double angle = Double.parseDouble(angleField.getText());

            addParticlesLinear(n, startPoint, endPoint, velocity, angle); // Ensure this method exists and is properly called
        });

        panel.add(new JLabel("N:"));
        panel.add(nField);
        panel.add(new JLabel("Start X:"));
        panel.add(startXField);
        panel.add(new JLabel("Start Y:"));
        panel.add(startYField);
        panel.add(new JLabel("End X:"));
        panel.add(endXField);
        panel.add(new JLabel("End Y:"));
        panel.add(endYField);
        panel.add(new JLabel("Velocity:"));
        panel.add(velocityField);
        panel.add(new JLabel("Angle:"));
        panel.add(angleField);
        panel.add(addButton);

        return panel;
    }

    // TODO: Add input validation
    private JPanel createPanelForAngularParticles() {
        JPanel panel = new JPanel(new FlowLayout());

        JTextField nField = new JTextField("50", 5);
        JTextField startXField = new JTextField("640", 5); // Center X
        JTextField startYField = new JTextField("360", 5); // Center Y
        JTextField startAngleField = new JTextField("0", 5);
        JTextField endAngleField = new JTextField("360", 5);
        JTextField velocityField = new JTextField("750", 5);
        JButton addButton = new JButton("Add Angular");

        addButton.addActionListener(e -> {
            int n = Integer.parseInt(nField.getText());
            Point startPoint = new Point(Integer.parseInt(startXField.getText()), Integer.parseInt(startYField.getText()));
            double startAngle = Double.parseDouble(startAngleField.getText());
            double endAngle = Double.parseDouble(endAngleField.getText());
            double velocity = Double.parseDouble(velocityField.getText());

            addParticlesAngular(n, startPoint, velocity, startAngle, endAngle);
            repaint();
        });

        panel.add(new JLabel("N: "));
        panel.add(nField);
        panel.add(new JLabel("Start X: "));
        panel.add(startXField);
        panel.add(new JLabel("Start Y: "));
        panel.add(startYField);
        panel.add(new JLabel("Start Angle: "));
        panel.add(startAngleField);
        panel.add(new JLabel("End Angle: "));
        panel.add(endAngleField);
        panel.add(new JLabel("Velocity: "));
        panel.add(velocityField);
        panel.add(addButton);

        return panel;
    }

    // TODO: Add input validation
    private JPanel createPanelForVelocityParticles() {
        JPanel panel = new JPanel(new FlowLayout());

        JTextField nField = new JTextField("50", 5);
        JTextField startXField = new JTextField("0", 5); // Center X
        JTextField startYField = new JTextField("0", 5); // Center Y
        JTextField startVelocityField = new JTextField("500", 5);
        JTextField endVelocityField = new JTextField("1000", 5);
        JTextField angleField = new JTextField("45", 5); // Straight up
        JButton addButton = new JButton("Add Velocity");

        addButton.addActionListener(e -> {
            int n = Integer.parseInt(nField.getText());
            Point startPoint = new Point(Integer.parseInt(startXField.getText()), Integer.parseInt(startYField.getText()));
            double startVelocity = Double.parseDouble(startVelocityField.getText());
            double endVelocity = Double.parseDouble(endVelocityField.getText());
            double angle = Double.parseDouble(angleField.getText());

            addParticlesVelocity(n, startPoint, startVelocity, endVelocity, angle);
            repaint();
        });

        panel.add(new JLabel("N: "));
        panel.add(nField);
        panel.add(new JLabel("Start X: "));
        panel.add(startXField);
        panel.add(new JLabel("Start Y: "));
        panel.add(startYField);
        panel.add(new JLabel("Start Vel: "));
        panel.add(startVelocityField);
        panel.add(new JLabel("End Vel: "));
        panel.add(endVelocityField);
        panel.add(new JLabel("Angle: "));
        panel.add(angleField);
        panel.add(addButton);

        return panel;
    }

    private JPanel createPanelForClearAndPause(){
        JPanel panel = new JPanel(new FlowLayout());

        // Button to clear Particles
        JButton clearParticlesButton = new JButton("Clear Particles");
        clearParticlesButton.addActionListener(e->clearParticles());
        clearParticlesButton.setPreferredSize(new Dimension(120,30));
        panel.add(clearParticlesButton);

//        // Pause btn
//        JButton pauseButton = new JButton("Pause Renderer");
//        pauseButton.addActionListener(e->togglePause(pauseButton));
//        pauseButton.setPreferredSize(new Dimension(140,30));
//        panel.add(pauseButton);

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Particle Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Create a container panel with BoxLayout along the Y_AXIS
            JPanel containerPanel = new JPanel();
            containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));

            Server simulatorGUI = new Server();
            simulatorGUI.setPreferredSize(new Dimension(Server.WINDOW_WIDTH, Server.WINDOW_HEIGHT));

            // Setup and add the control panel at the top
            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
            simulatorGUI.setupControlPanel(controlPanel);

            // Add the control panel and simulatorGUI to the containerPanel
            containerPanel.add(controlPanel);
            containerPanel.add(simulatorGUI);

            frame.add(containerPanel);  // Add the containerPanel to the frame

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}