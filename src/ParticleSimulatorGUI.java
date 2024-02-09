import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParticleSimulatorGUI extends JPanel {
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    private List<Particle> particles = new ArrayList<>();
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public ParticleSimulatorGUI() {
        // Generate initial particles
        for(int i = 0; i < 100; i++) {
            particles.add(new Particle(
                    (int)(Math.random() * WINDOW_WIDTH),
                    (int)(Math.random() * WINDOW_HEIGHT),
                    5, // Velocity
                    Math.random() * 360)); // Angle
        }

        // Swing Timer for animation
        new Timer(16, e -> updateAndRepaint()).start(); // ~60 FPS
    }

    private void updateAndRepaint() {
        // Submit each particle's run method for parallel execution
        particles.forEach(executor::submit);

        repaint(); // Re-draw GUI with updated particle positions
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Particle particle : particles) {
            particle.draw(g); // Let each particle draw itself
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Particle Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ParticleSimulatorGUI simulatorGUI = new ParticleSimulatorGUI();
            simulatorGUI.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            frame.add(simulatorGUI);
            frame.pack();
            frame.setVisible(true);
        });
    }
}