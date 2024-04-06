import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class Sprite {
    transient Random rand = new Random();
    @Expose
    private String clientId;
    @Expose
    private int x = Client.WINDOW_WIDTH / 2;                              // spawn at center
//    private int x = rand.nextInt(Client.WINDOW_WIDTH / 2 + 1);            // spawn at random loc
    @Expose
    private int y = Client.WINDOW_HEIGHT / 2;                             // spawn at center
//    private int y = rand.nextInt(Client.WINDOW_HEIGHT / 2 + 1);      // spawn at random loc

    @Expose
    private int width;
    @Expose
    private int height;

    private boolean willSpawn = false;

    @Expose
    private int excessX = 0;
    @Expose
    private int excessY = 0;

    public static final int PERIPHERY_WIDTH = 33;
    public static final int PERIPHERY_HEIGHT = 19;

    private final int MID_PERIPHERAL_WIDTH = (int) Math.floor(PERIPHERY_WIDTH / 2.0f);
    private final int MID_PERIPHERAL_HEIGHT = (int) Math.floor(PERIPHERY_HEIGHT / 2.0f);

    private int drawX = Particle.gridWidth *  MID_PERIPHERAL_WIDTH;
    private int drawY = Particle.gridHeight * MID_PERIPHERAL_HEIGHT;
    @Expose
    private int red;
    @Expose
    private int green;
    @Expose
    private int blue;
    private final int SERVER_PORT = 4990;
    private final String SERVER_ADDRESS = "localhost";

    public static int gridWidth = Math.round((Client.WINDOW_WIDTH * 1.0f) / Sprite.PERIPHERY_WIDTH);
    public static int gridHeight = Math.round((Client.WINDOW_HEIGHT * 1.0f) / Sprite.PERIPHERY_HEIGHT);

    @Expose
    private int port;

    public Sprite(int width, int height, String clientId) {
        this.width = width;
        this.height = height;
        this.clientId = clientId;

        this.red = random(225);
        this.green = random(225);
        this.blue = random(225);
    }

    public String getClientId() {
        return clientId;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public static int random(int maxRange) {
        return (int) Math.round((Math.random() * maxRange));
    }

    public void drawClient(Graphics g){
//        image.paintIcon(observer, g, drawX, drawY);
        g.setColor(new Color(red, green, blue));
        g.fillOval(drawX, drawY, width, height);
    }

    public void drawOtherClient(Graphics g, int spriteX, int spriteY, int spriteExX, int spriteExY) {
        // Calculate the drawX and drawY based on the sprite's position
//        int drawX = (spriteX) + (x - spriteX + (spriteExX * -1)) * (gridWidth) - (gridWidth / 2);
//        int drawY = (spriteY) + (y - spriteY + (spriteExY * -1)) * (gridHeight) - (gridHeight / 2);

        int relativeX = x - spriteX;
        int relativeY = y - spriteY;
        int scaledX = relativeX * gridWidth + Client.WINDOW_WIDTH / 2;
        int scaledY = relativeY * gridHeight + Client.WINDOW_HEIGHT / 2;
        drawX = scaledX - (excessX * gridWidth);
        drawY = scaledY - (excessY * gridHeight);
        drawX = drawX - (width / 2);
        drawY = drawY - (height / 2);

//        printPosition();
//        System.out.printf("Draw X: %d, Draw Y: %d%n", drawX, drawY);
        System.out.println("Width: " + width + ", Height: " + height);
//        System.out.println(excessX + ", " + excessY);

        // Check if the calculated coordinates are within the bounds of the window
        if (drawX >= 0 && drawX < Client.WINDOW_WIDTH &&
                drawY >= 0 && drawY < Client.WINDOW_HEIGHT) {
            g.setColor(new Color(red, green, blue));
            g.fillOval(drawX, drawY, width, height); // Draw the other sprite

//            System.out.println("Other sprite drawn.");
        }
    }

    public void drawServer(Graphics g) {
        g.setColor(new Color(red, green, blue));
        g.fillOval(x, y, 10, 10);
    }

        public  void move(int dx, int dy){
        drawX += dx;
        drawY += dy;
    }

    public void updatePosition(int x, int y){

        this.x += x;
        excessX = Math.max(Math.min(0, this.x - MID_PERIPHERAL_WIDTH), (this.x + MID_PERIPHERAL_WIDTH) - Client.WINDOW_WIDTH );

        this.y += y;
        excessY = Math.max(Math.min(0, this.y - MID_PERIPHERAL_HEIGHT), (this.y + MID_PERIPHERAL_HEIGHT) - Client.WINDOW_HEIGHT);


        sendDataToServer();
    }

    // TODO: If may time, move this code to Client for separation of concerns
    private void sendDataToServer() {
        try {
            DatagramSocket serverSocket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            String data = gson.toJson(this);
            String jsonString = gson.toJson(new ReqResForm("update_sprite", data));

            DatagramPacket packet = new DatagramPacket(jsonString.getBytes(), jsonString.length(), serverAddress, SERVER_PORT);

            ReqResForm.createFormFromRequest(packet);

            serverSocket.send(packet);

            serverSocket.close();

//            System.out.println("sent to server: " + jsonString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    private void sendDataToServer(){
//        ReqResForm reqResForm = new ReqResForm(InetAddress.getByAddress(SERVER_ADDRESS), 4990, "update_sprite", );
//    }

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

    @Override
    public String toString() {
        return "Sprite{" +
                "clientId='" + clientId + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}

// UL -> X: 623,  Y: 350
// LL -> X: 623,  Y: 369
// UR -> X: 656,  Y: 350
// LR -> X: 656,  Y: 369
