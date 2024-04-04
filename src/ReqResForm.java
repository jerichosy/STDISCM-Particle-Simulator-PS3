import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class ReqResForm {

    private InetAddress address;
    private int port;
    @Expose
    private String type;
    @Expose
    private String data;


    public ReqResForm(InetAddress address, int port, String type, String data) {
        this.address = address;
        this.port = port;
        this.type = type;
        this.data = data;
    }

    public ReqResForm(String type, String data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public String getData() {

        return data;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static ReqResForm createFormFromRequest(DatagramPacket packet){

        // Create Gson instance
        Gson gson = new Gson();

        String receivedData = new String(packet.getData(), 0, packet.getLength());

        ReqResForm form = gson.fromJson(receivedData, ReqResForm.class);
        form.setAddress(packet.getAddress());
        form.setPort(packet.getPort());

        return form;
    }
}
