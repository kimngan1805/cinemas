import java.io.*;
import java.net.*;

public class P2PHost {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("🚀 [HOST] Đang đợi bạn kết nối tại cổng 5000...");
            System.out.println("👉 Bây giờ hãy mở Ngrok: 'ngrok tcp 5000'");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("✅ [HOST] Đã có một bạn ở mạng khác chui vào phòng!");

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msg = in.readLine();
                System.out.println("📩 [HOST] Bạn gửi: " + msg);

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Hố hố hố, chào bạn! Tui nhận được tin rồi.");
                socket.close();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}