package com.cinemates.utils;

import java.net.URL;

public class DriveUtils {
    // Hàm biến ID thành Link Ảnh xem được ngay
    public static String getGoogleImageLink(String fileId) {
        // Đây là đường dẫn "thần thánh" để load ảnh từ Drive cực nhanh
        return "https://drive.google.com/uc?export=view&id=" + fileId;
    }
    public static String getGoogleVideoLink(String input) {
        // --- CHẾ ĐỘ TEST ĐỒNG BỘ (BẬT) ---
        // Bất kể Database đưa cái gì, tui cũng bắt nó mở file video1.mp4 lên
        try {
            // Dấu "/" nghĩa là tìm từ thư mục resources
            URL resource = DriveUtils.class.getResource("/videos/video1.mp4");

            if (resource != null) {
                System.out.println("⚠️ ĐANG TEST: Load file nội bộ video1.mp4");
                return resource.toExternalForm();
            } else {
                System.out.println("❌ Lỗi: Không tìm thấy file video1.mp4 trong resources/videos/");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- NẾU MUỐN QUAY VỀ CHẾ ĐỘ THẬT (GOOGLE DRIVE) THÌ MỞ LẠI ĐOẠN DƯỚI NÀY ---
        /*
        if (input != null && input.startsWith("http")) {
            return input;
        }
        return "https://drive.google.com/uc?export=download&id=" + input;
        */

        return ""; // Trả về rỗng nếu lỗi
    }
}