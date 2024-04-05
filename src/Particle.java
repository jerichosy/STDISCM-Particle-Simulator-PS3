import com.google.gson.annotations.Expose;

import java.awt.*;

public class Particle {
    @Expose
    private int x;
    @Expose
    private int y; // Position
    @Expose
    private double velocity;
    @Expose
    private double angle; // In degrees


    private int red;
    private int green;
    private int blue;

//    private boolean isInExplorerMode = false;

    public static int gridWidth = Math.round((Client.WINDOW_WIDTH * 1.0f) / Sprite.PERIPHERY_WIDTH);
    public static int gridHeight = Math.round((Client.WINDOW_HEIGHT * 1.0f) / Sprite.PERIPHERY_HEIGHT);


    public Particle(int x, int y, double velocity, double angle, int WINDOW_HEIGHT) {
        this.x = x;
        this.y = WINDOW_HEIGHT - y;
        this.velocity = velocity;
        this.angle = angle;
        this.red = random(255);
        this.green = random(255);
        this.blue = random(255);
    }

    public static int random(int maxRange) {
        return (int) Math.round((Math.random() * maxRange));
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
        } else if (nextX >= Client.WINDOW_WIDTH) {
            angle = 180 - angle;
            nextX = Client.WINDOW_WIDTH; // Correct position
        }

        if (nextY <= 0) {
            angle = 360 - angle;
            nextY = 0; // Correct position to stay within bounds
        } else if (nextY >= Client.WINDOW_HEIGHT) {
            angle = 360 - angle;
            nextY = Client.WINDOW_HEIGHT; // Correct position
        }

        // Update positions after handling collisions
        x = nextX;
        y = nextY;

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

    public void draw(Graphics g, int spriteX, int spriteY, int spriteExX, int spriteExY) {
        // Calculate the drawX and drawY based on the sprite's position
        int drawX = (spriteX) + (x - spriteX + (spriteExX * -1)) * (gridWidth) - (gridWidth / 2);
        int drawY = (spriteY) + (y - spriteY + (spriteExY * -1)) * (gridHeight) - (gridHeight / 2);

        // Check if the calculated coordinates are within the bounds of the window
        if (drawX >= 0 && drawX < Client.WINDOW_WIDTH &&
                drawY >= 0 && drawY < Client.WINDOW_HEIGHT) {
            g.fillOval(drawX, drawY, gridWidth, gridHeight); // Draw particle as a small circle

//                System.out.printf("Particle X: %d, Particle Y: %d%n", x, y);
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


    public static boolean isPointInsideRectangle(int[] point, int[] rectangle) {
        int x = point[0];
        int y = point[1];
        int x1 = rectangle[0];
        int y1 = rectangle[1];
        int x2 = rectangle[2];
        int y2 = rectangle[3];
        return x >= x1 && x <= x2 && y >= y1 && y <= y2;
    }

}