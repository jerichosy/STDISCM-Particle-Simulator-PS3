import javax.swing.*;
import java.awt.*;

public class Sprite {
    private ImageIcon image;
    private int x = Client.WINDOW_WIDTH / 2;
    private int y = Client.WINDOW_HEIGHT / 2;
    private int width;
    private int height;

    private boolean willSpawn = false;

    private int excessX = 0;
    private int excessY = 0;

    public static final int PERIPHERY_WIDTH = 33;
    public static final int PERIPHERY_HEIGHT = 19;

    private final int MID_PERIPHERAL_WIDTH = (int) Math.floor(PERIPHERY_WIDTH / 2.0f);
    private final int MID_PERIPHERAL_HEIGHT = (int) Math.floor(PERIPHERY_HEIGHT / 2.0f);

    private int drawX = Particle.gridWidth *  MID_PERIPHERAL_WIDTH;
    private int drawY = Particle.gridHeight * MID_PERIPHERAL_HEIGHT;


    public Sprite(String imagePath, int width, int height) {
        this.image = new ImageIcon(imagePath);
        this.image.setImage(this.image.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT));
        this.width = width;
        this.height = height;
    }

    public void draw(Graphics g, Component component){
//        image.paintIcon(observer, g, drawX, drawY);
        g.setColor(Color.RED);
        g.fillOval(drawX, drawY, width, height);
    }

    public  void move(int dx, int dy){
        drawX += dx;
        drawY += dy;
    }

    public void updatePosition(int x, int y){

        System.out.println("HELLO");

        this.x += x;
        excessX = Math.max(Math.min(0, this.x - MID_PERIPHERAL_WIDTH), (this.x + MID_PERIPHERAL_WIDTH) - Client.WINDOW_WIDTH );

        this.y += y;
        excessY = Math.max(Math.min(0, this.y - MID_PERIPHERAL_HEIGHT), (this.y + MID_PERIPHERAL_HEIGHT) - Client.WINDOW_HEIGHT);

        printPosition();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }


    public void printPosition(){
        System.out.printf(
                "Sprite (X: %d, Y: %d, User Y: %d), Excess (X: %d, Y: %d)%n", x, y, Client.WINDOW_HEIGHT - y, excessX, excessY
        );
    }

    public int getExcessX() {
        return excessX;
    }

    public int getExcessY() {
        return excessY;
    }

    public boolean isWillSpawn() {
        return willSpawn;
    }

    public void setWillSpawn(boolean willSpawn) {
        this.willSpawn = willSpawn;
    }

}

// UL -> X: 623,  Y: 350
// LL -> X: 623,  Y: 369
// UR -> X: 656,  Y: 350
// LR -> X: 656,  Y: 369
