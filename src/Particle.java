import java.awt.*;

public class Particle {
    private int x, y; // Position
    private double velocity;
    private double angle; // In degrees

    public Particle(int x, int y, double velocity, double angle, int WINDOW_HEIGHT) {
        this.x = x;
        this.y = WINDOW_HEIGHT - y;
        this.velocity = velocity;
        this.angle = angle;
    }

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

        // New collision logic for diagonal walls
        for(Wall wall: ParticleSimulatorGUI.getWalls()) {
            if (checkIntersection(x, y, nextX, nextY, wall.getX1(), wall.getY1(), wall.getX2(), wall.getY2())) {
                // Calculate wall vector components
                double wallDX = wall.getX2() - wall.getX1();
                double wallDY = wall.getY2() - wall.getY1();
                // Calculate wall normal (perpendicular vector)
                double wallNormalX = -wallDY;
                double wallNormalY = wallDX;

                // Normalize the wall normal
                double normLength = Math.sqrt(wallNormalX * wallNormalX + wallNormalY * wallNormalY);
                wallNormalX /= normLength;
                wallNormalY /= normLength;

                // Calculate the velocity vector
                double velocityX = velocityPerUpdate * Math.cos(Math.toRadians(angle));
                double velocityY = -velocityPerUpdate * Math.sin(Math.toRadians(angle));

                // Calculate the dot product of velocity and wall normal
                double dotProduct = velocityX * wallNormalX + velocityY * wallNormalY;

                // Calculate the reflection vector
                double reflectX = velocityX - 2 * dotProduct * wallNormalX;
                double reflectY = velocityY - 2 * dotProduct * wallNormalY;

                // Convert reflection vector to angle
                double newAngleRadians = Math.atan2(-reflectY, reflectX);
                angle = Math.toDegrees(newAngleRadians);

                // Normalize the angle
                angle = (angle + 360) % 360;

                // Recalculate nextX and nextY based on the new angle
                nextX = x + (int) (Math.cos(newAngleRadians) * velocityPerUpdate);
                nextY = y - (int) (Math.sin(newAngleRadians) * velocityPerUpdate);

                break; // Handle one collision per update
            }
        }

        // Update positions after handling collisions
        x = nextX;
        y = nextY;

//        // Normalize the angle
//        angle = (angle + 360) % 360;
    }

    private boolean checkIntersection(
            int x1, int y1, int x2, int y2, // Line segment 1 (particle's movement)
            int x3, int y3, int x4, int y4) { // Line segment 2 (wall)
        // Calculate parts of the equations to check intersection
        int den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (den == 0) return false; // Lines are parallel

        int tNum = (x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4);
        int uNum = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3));

        double t = tNum / (double) den;
        double u = uNum / (double) den;

        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            return true; // Intersection occurs
        }
        return false;
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