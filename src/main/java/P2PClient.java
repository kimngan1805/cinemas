import java.io.*;
import java.net.*;
import java.util.Scanner;

public class P2PClient {
    public static void main(String[] args) {
        // Sửa lại chỗ này thành System.in nha Ngân
        Scanner sc = new Scanner(System.in);

        System.out.print("🔗 Nhập địa chỉ Ngrok (vd: 0.tcp.ap.ngrok.io): ");
        String host = sc.nextLine();

        System.out.print("🔢 Nhập Port Ngrok (vd: 12345): ");
        int port = sc.nextInt();

        try (Socket socket = new Socket(host, port)) {
            System.out.println("🚀 [CLIENT] Đang kết nối tới máy Ngân qua Internet...");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("Chào Ngân nhé, mình kết nối xuyên mạng được rồi nè!");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("📡 [CLIENT] Ngân trả lời: " + in.readLine());
        } catch (IOException e) {
            System.err.println("❌ Lỗi: Không thể kết nối. Kiểm tra lại Ngrok xem có bị tắt nửa chừng không nha!");
        }
    }
}