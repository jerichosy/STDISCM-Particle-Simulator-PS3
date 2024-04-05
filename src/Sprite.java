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
    @Expose
    private int red;
    @Expose
    private int green;
    @Expose
    private int blue;
    private final int SERVER_PORT = 4990;
    private final String SERVER_ADDRESS = "localhost";

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

    public void drawServer(Graphics g) {
        g.setColor(new Color(red, green, blue));
        g.fillOval(x, y, 5, 5);
    }

        public  void move(int dx, int dy){
        drawX += dx;
        drawY += dy;
    }

    public void updatePosition(int x, int y){

//        System.out.println("HELLO");

        this.x += x;
        excessX = Math.max(Math.min(0, this.x - MID_PERIPHERAL_WIDTH), (this.x + MID_PERIPHERAL_WIDTH) - Client.WINDOW_WIDTH );

        this.y += y;
        excessY = Math.max(Math.min(0, this.y - MID_PERIPHERAL_HEIGHT), (this.y + MID_PERIPHERAL_HEIGHT) - Client.WINDOW_HEIGHT);

        printPosition();
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

            System.out.println("sent to server: " + jsonString);


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
