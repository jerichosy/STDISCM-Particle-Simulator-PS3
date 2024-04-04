import com.google.gson.Gson;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Client extends JPanel implements KeyListener {
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    private java.util.List<Particle> particles = new CopyOnWriteArrayList<>(); // Thread-safe ArrayList ideal for occasional writes
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private long lastTime = System.currentTimeMillis();
    private int frames = 0;
    private String fps = "FPS: 0";
    private String particleCount = "Particle Count: 0";
    private boolean isPaused = false;

    private Sprite sprite = new Sprite(Particle.gridWidth , Particle.gridHeight);

    private long lastUpdateTime = System.currentTimeMillis();

    private java.util.List<Thread> threads = new ArrayList<>();


    private String serverAddress;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Client(String serverAddress) throws UnknownHostException {
        this.serverAddress = serverAddress;

        runUI();
        registerWithServer();

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

        scheduleParticleUpdate();

    }

    private void scheduleParticleUpdate() {
        scheduler.scheduleAtFixedRate(this::requestUpdatedParticles, 0, 2, TimeUnit.SECONDS);
    }

    private void requestUpdatedParticles() {
        System.out.println("Requesting updated particles from server...");

        // fetchUpdatedParticlesFromServer();
    }

    private void registerWithServer() throws UnknownHostException {
        // Create a 'new' request ReqResForm with the sprite information
        Gson gson = new Gson();
        Sprite sprite = new Sprite(100, 100); // Example sprite
        String spriteData = gson.toJson(sprite);
        ReqResForm form = new ReqResForm("new", spriteData);

        // Send the request to the server via Port A
        byte[] sendData = gson.toJson(form).getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(serverAddress), Ports.RES_REP.getPortNumber());
        try {
            DatagramSocket socket = new DatagramSocket();
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
