import java.awt.*;

// TODO: Invert Y
// TODO: Check angle
public class Particle implements Runnable {
    private int x, y; // Position
    private double velocity;
    private double angle; // In degrees

    public Particle(int x, int y, double velocity, double angle) {
        this.x = x;
        this.y = y;
        this.velocity = velocity;
        this.angle = angle;
    }

    @Override
    public void run() {
        // The run method now invokes update for using with ExecutorService
        update();
    }

    public void update() {
        // Update the position based on velocity and angle
        x += (int) (velocity * Math.cos(Math.toRadians(angle)));
        y -= (int) (velocity * Math.sin(Math.toRadians(angle)));

        // Check and handle collisions with the window boundaries
        if(x <= 0 || x >= ParticleSimulatorGUI.WINDOW_WIDTH) {
            angle = 180 - angle;
        }
        if(y <= 0 || y >= ParticleSimulatorGUI.WINDOW_HEIGHT) {
            angle = 360 - angle;
        }

        for(Wall wall: ParticleSimulatorGUI.getWalls()){
            if(x == wall.getX1() && y >= wall.getY2() && y <= wall.getY1()){
                angle = 180 - angle;
            }

            if(y == wall.getY1() && x >= wall.getX1() && x <= wall.getX2()){
                angle = 360 - angle;
            }

        }


    }

    public void draw(Graphics g) {
        g.fillOval(x, y, 5, 5); // Draw particle as a small circle
    }

    // Getter methods to use in the GUI for drawing
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}