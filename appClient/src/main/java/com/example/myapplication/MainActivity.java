package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    // Khai báo các view
    private TextView tvReceivedData, tvshowtinnhan;
    private EditText etServerName, etServerPort, edTinNhan;
    private Button bntClientConnect, bntSend;
    private String serverName;
    private int serverPort;
    private Socket socket;
    private BufferedReader br_input;
    private PrintWriter output_Client;
    private boolean connected = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Ánh xạ view
        tvReceivedData = findViewById(R.id.tvshowReactxt);
        tvshowtinnhan = findViewById(R.id.tvshotinhan);
        etServerName = findViewById(R.id.edsevrname);
        etServerPort = findViewById(R.id.edseverport);
        bntClientConnect = findViewById(R.id.bntclient);
        edTinNhan = findViewById(R.id.editTextText);
        bntSend = findViewById(R.id.bntsenserver);
        // Xử lý sự kiện nút "Connect to Server"
        // Đoạn code dưới đây định nghĩa sự kiện khi người dùng nhấn vào nút "bntClientConnect"
        bntClientConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Kiểm tra nếu chưa kết nối (connected = false)
                if (!connected) {
                    // Lấy thông tin tên server từ EditText "etServerName" và chuyển đổi thông tin cổng server từ EditText "etServerPort" sang kiểu Integer
                    serverName = etServerName.getText().toString();
                    serverPort = Integer.parseInt(etServerPort.getText().toString());
                    // Kết nối tới Server bằng cách gọi hàm "connectToServer" và truyền thông tin serverName và serverPort
                    connectToServer(serverName, serverPort);
                } else {
                    // Ngược lại, nếu đã kết nối, ngắt kết nối với Server bằng cách gọi hàm "disconnectFromServer"
                    disconnectFromServer();
                }
            }
        });

// Đoạn code dưới đây định nghĩa sự kiện khi người dùng nhấn vào nút "bntSend"
        bntSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lấy nội dung tin nhắn từ EditText "edTinNhan" và lưu vào biến "messageToSend"
                String messageToSend = edTinNhan.getText().toString();
                // Kiểm tra xem "messageToSend" có rỗng không và trạng thái "connected" có là "true" hay không
                if (!messageToSend.isEmpty() && connected) {
                    // Gửi tin nhắn tới Server bằng cách gọi hàm "sendMessageToServer" và truyền thông tin "messageToSend"
                    sendMessageToServer(messageToSend);
                    // Xóa nội dung trong EditText "edTinNhan" để chuẩn bị cho việc nhập tin nhắn mới
                    edTinNhan.setText("");
                }
            }
        });

    }
    // Phương thức kết nối tới Server
    // Phương thức để kết nối tới Server
    private void connectToServer(final String serverName, final int serverPort) {
        // Tạo một luồng mới để thực hiện công việc kết nối tới Server
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Tạo kết nối tới Server với địa chỉ serverName và cổng serverPort
                    socket = new Socket(serverName, serverPort);
                    // Tạo đối tượng BufferedReader để đọc dữ liệu từ Server
                    br_input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    // Tạo đối tượng PrintWriter để gửi dữ liệu tới Server
                    output_Client = new PrintWriter(socket.getOutputStream(), true);

                    // Cập nhật giao diện sau khi kết nối thành công
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Hiển thị văn bản "Connected to Server" trong TextView "tvReceivedData"
                            tvReceivedData.setText("Connected to Server");
                            // Đặt văn bản của nút "bntClientConnect" thành "Disconnect"
                            bntClientConnect.setText("Disconnect");
                            // Đánh dấu là đã kết nối (biến "connected" được đặt thành true)
                            connected = true;
                        }
                    });

                    // Nhận tin nhắn từ Server và hiển thị lên giao diện
                    String messageFromServer;
                    while ((messageFromServer = br_input.readLine()) != null) {
                        final String finalMessage = messageFromServer;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // Hiển thị tin nhắn từ Server trong TextView "tvshowtinnhan"
                                tvshowtinnhan.setText("Tin nhắn từ sever: " + finalMessage);

                                // Hiển thị thông báo trong Dialog khi có tin nhắn mới
                                final Dialog dialog = new Dialog(MainActivity.this);
                                // trỏ id
                                dialog.setContentView(R.layout.dialog_thongbao);
                                TextView tvmes = dialog.findViewById(R.id.tvtin);
                                // set tin nhắn lên dialog
                                tvmes.setText("" + finalMessage);
                                dialog.setCancelable(true);
                                dialog.show();

                                // Tự đóng Dialog sau 4 giây
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (dialog.isShowing()) {
                                            dialog.dismiss();
                                        }
                                    }
                                }, 4000);
                            }
                        });
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Phương thức ngắt kết nối với Server
    private void disconnectFromServer() {
        try {
            // Kiểm tra xem có kết nối socket hay không
            if (socket != null) {
                // Đóng kết nối socket
                socket.close();
                // Đánh dấu là đã ngắt kết nối (biến "connected" được đặt thành false)
                connected = false;
                // Đặt văn bản của nút "bntClientConnect" thành "Connect to Server"
                bntClientConnect.setText("Connect to Server");
                // Đặt văn bản của TextView "tvReceivedData" thành "Disconnected"
                tvReceivedData.setText("Disconnected");
                // Xóa nội dung trong TextView "tvshowtinnhan" (để không hiển thị tin nhắn từ Server nữa)
                tvshowtinnhan.setText("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Phương thức gửi tin nhắn tới Server
    private void sendMessageToServer(final String message) {
        if (output_Client != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    output_Client.println(message);
                }
            }).start();
        }
    }
}