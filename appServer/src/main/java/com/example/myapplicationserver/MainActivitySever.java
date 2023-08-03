package com.example.myapplicationserver;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivitySever extends AppCompatActivity {
    // Khai báo các view
    private TextView tvServerName, tvServerPort, tvStatus, tvReceivedMessage;
    private String serverIP = "26.198.200.168"; // ĐỊA CHỈ IP MÁY
    private int serverPort = 1234; // PORT
    private Button bntStart, bntStop, bntSend;
    private EditText edMessage;
    private ServerThread serverThread;
    // Sử dụng Handler để làm việc với giao diện trong Thread
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_sever);

        // Ánh xạ view
        tvServerName = findViewById(R.id.tvsevername);
        tvServerPort = findViewById(R.id.tvServerpory);
        tvStatus = findViewById(R.id.tvstatus);
        tvReceivedMessage = findViewById(R.id.tv_nhantinnhan);
        edMessage = findViewById(R.id.edtinhandi);
        bntStart = findViewById(R.id.bntstart);
        bntStop = findViewById(R.id.bntstop);
        bntSend = findViewById(R.id.bnt_sent);

        // Hiển thị địa chỉ IP và cổng của Server lên giao diện

        tvServerName.setText(serverIP);
        tvServerPort.setText(String.valueOf(serverPort));
    }
    // Xử lý sự kiện nút "Bắt đầu Server"
    public void onClickStartServe(View view) {
        serverThread = new ServerThread();
        Toast.makeText(this, "SERVER ĐÃ CHẠY", Toast.LENGTH_SHORT).show();
        serverThread.startServer();
    }
    // Xử lý sự kiện nút "Dừng Server"

    public void onClickStopServe(View view) {
        if (serverThread != null) {
            serverThread.stopServer();
            Toast.makeText(this, "SERVER Dừng", Toast.LENGTH_SHORT).show();
        }
    }
    // Xử lý nút gửi
    public void onClickSend(View view) {
        // Lấy nội dung tin nhắn từ EditText "edMessage" và lưu vào biến "messageToSend"
        String messageToSend = edMessage.getText().toString();

        // Kiểm tra xem "messageToSend" có rỗng không và "serverThread" có tồn tại không
        if (!messageToSend.isEmpty() && serverThread != null) {
            // Gửi tin nhắn tới tất cả các client thông qua đối tượng "serverThread" bằng cách gọi phương thức "sendMessageToClients"
            serverThread.sendMessageToClients(messageToSend);
            // Xóa nội dung trong EditText "edMessage" để chuẩn bị cho việc nhập tin nhắn mới
            edMessage.setText("");
        }
    }

    // Lớp ServerThread chạy trong Thread riêng biệt để lắng nghe các kết nối từ Client
    class ServerThread extends Thread {
        private boolean serverRunning;
        private ServerSocket serverSocket;
        // Phương thức start
        public void startServer() {
            serverRunning = true;
            start();
        }
        // Phương stop /
        public void stopServer() {
            // Đánh dấu là Server không còn hoạt động bằng cách đặt biến "serverRunning" thành false
            serverRunning = false;

            // Kiểm tra xem "serverSocket" có tồn tại hay không
            if (serverSocket != null) {
                try {
                    // Nếu tồn tại, đóng kết nối serverSocket để dừng Server
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Gửi một Runnable tới luồng giao diện (UI thread) để cập nhật giao diện sau khi dừng Server
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // Hiển thị văn bản "Server stopped" trong TextView "tvStatus"
                    tvStatus.setText("Server stopped");
                }
            });
        }

        // Phương thức gửi tin nhắn đến tất cả các Client đã kết nối
        public void sendMessageToClients(final String message) {
            // Kiểm tra xem "serverSocket" có tồn tại hay không
            if (serverSocket != null) {
                // Tạo một luồng mới để gửi tin nhắn tới tất cả các client
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Duyệt qua danh sách các ClientHandler và gửi tin nhắn tới từng client
                        for (ClientHandler client : clientsList) {
                            client.sendMessageToClient(message);
                        }
                    }
                }).start();
            }
        }



        private ArrayList<ClientHandler> clientsList = new ArrayList<>();

        @Override
        public void run() {
            try {
                // Tạo Socket Server
                serverSocket = new ServerSocket(serverPort);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Waiting for Clients");
                    }
                });

                while (serverRunning) {
                    // Chấp nhận các kết nối từ Client
                    Socket socket = serverSocket.accept();
                    // Xử lý Client kết nối mới trong một luồng riêng biệt

                    ClientHandler client = new ClientHandler(socket);
                    client.start();
                    clientsList.add(client);
                    // Cập nhật giao diện khi có Client kết nối thành công

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Connected to: " + socket.getInetAddress() + " : " + socket.getLocalPort());
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Lớp ClientHandler xử lý việc gửi/nhận tin nhắn cho từng Client kết nối

        class ClientHandler extends Thread {
            private Socket clientSocket;
            private BufferedReader br_input;
            private PrintWriter output_Client;

            public ClientHandler(Socket socket) {
                clientSocket = socket;
                try {
                    // Lấy luồng đầu vào và luồng đầu ra của Client
                    br_input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    output_Client = new PrintWriter(clientSocket.getOutputStream(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Phương thức gửi tin nhắn đến Client
            public void sendMessageToClient(String message) {
                output_Client.println(message);
            }

            @Override
            public void run() {
                try {
                    // Hiển thị tin nhắn từ Client lên giao diện
                    String messageFromClient;
                    while ((messageFromClient = br_input.readLine()) != null) {
                        // Hiển thị tin nhắn từ Client lên giao diện
                        final String finalMessage = messageFromClient;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // Set tin nhắn lên texview giao diện
                                tvReceivedMessage.setText("Tin nhắn từ client: " + finalMessage);
                                // thông báocuarar lab 7
                                // thông báo co tin nhắn mới
                                final Dialog dialog = new Dialog(MainActivitySever.this);
                                // set layout dialog
                                dialog.setContentView(R.layout.dialogthongbao);
                                TextView tvmes=dialog.findViewById(R.id.tvmess);
                                // set tin nhắn lên texviewở dialog
                                tvmes.setText(""+finalMessage);
                                dialog.setCancelable(true);
                                // show dialog
                                dialog.show();

                                // set dialog sau 4s thì ẩn
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
        }
    }
}
