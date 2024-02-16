import java.awt.*;

// TODO: Invert Y
// TODO: Check angle
//public class Particle implements Runnable {
public class Particle {
    private int x, y; // Position
    private double velocity;
    private double angle; // In degrees

    public Particle(int x, int y, double velocity, double angle) {
        this.x = x;
        this.y = y;
        this.velocity = velocity;
        this.angle = angle;
    }

//    @Override
//    public void run() {
//        // The run method now invokes update for using with ExecutorService
//        update();
//    }

    public void update(double deltaTime) {
        // Convert velocity from pixels per second to pixels per update
        double velocityPerUpdate = velocity * deltaTime;

        // Update the position based on velocity and angle
        int nextX = x + (int) (velocityPerUpdate * Math.cos(Math.toRadians(angle)));
        int nextY = y - (int) (velocityPerUpdate * Math.sin(Math.toRadians(angle)));

        // Check and handle collisions with the window boundaries
        // Ensure the particle bounces off the window boundaries properly
        // Check the nextX and nextY for collisions, not the current x and y
        if (nextX <= 0) {
            angle = 180 - angle;
            nextX = 0; // Correct position to stay within bounds
        } else if (nextX >= ParticleSimulatorGUI.WINDOW_WIDTH) {
            angle = 180 - angle;
            nextX = ParticleSimulatorGUI.WINDOW_WIDTH; // Correct position
        }

        if (nextY <= 0) {
            angle = 360 - angle;
            nextY = 0; // Correct position to stay within bounds
        } else if (nextY >= ParticleSimulatorGUI.WINDOW_HEIGHT) {
            angle = 360 - angle;
            nextY = ParticleSimulatorGUI.WINDOW_HEIGHT; // Correct position
        }

////        COMPUTATION FOR COLLISION HERE
//        for(Wall wall: ParticleSimulatorGUI.getWalls()){
//            if(x == wall.getX1() && y >= wall.getY2() && y <= wall.getY1()){
//                angle = 180 - angle;
//            }
//            if(y == wall.getY1() && x >= wall.getX1() && x <= wall.getX2()){
//                angle = 180 - angle;
//            }
//        }

        for(Wall wall: ParticleSimulatorGUI.getWalls()) {
            // Assuming a very simple check that the wall is either vertical or horizontal
            // Also assuming the particle size is negligible; otherwise, include the radius in these calculations
            boolean collidesWithVerticalWall = (x <= wall.getX1() && nextX >= wall.getX1()) || (x >= wall.getX1() && nextX <= wall.getX1());
            boolean collidesWithHorizontalWall = (y <= wall.getY1() && nextY >= wall.getY1()) || (y >= wall.getY1() && nextY <= wall.getY1());

            if (collidesWithVerticalWall && nextY >= Math.min(wall.getY1(), wall.getY2()) && nextY <= Math.max(wall.getY1(), wall.getY2())) {
                angle = 180 - angle; // Reflect angle horizontally
                nextX = x; // Cancel the horizontal movement for this update
            }
            if (collidesWithHorizontalWall && nextX >= Math.min(wall.getX1(), wall.getX2()) && nextX <= Math.max(wall.getX1(), wall.getX2())) {
                angle = 360 - angle; // Reflect angle vertically
                nextY = y; // Cancel the vertical movement for this update
            }
        }

        // Update positions after handling collisions
        x = nextX;
        y = nextY;

//        // Keep the updated angle within bounds (0 to 360 degrees)
//        while (angle < 0) angle += 360;
//        while (angle > 360) angle -= 360;

        // Normalize the angle
        angle = (angle + 360) % 360;
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