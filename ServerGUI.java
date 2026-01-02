import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import javax.swing.*;

public class ServerGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton, fileButton, voiceCallButton, videoCallButton;
    private JButton receiveCallButton, endCallButton, cancelCallButton;
    private JLabel statusLabel;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader br;
    private PrintStream ps;
    private String connectionId, connectionPassword;
    private JFrame videoCallFrame;
    private CameraPanel cameraPanel;

    public ServerGUI() {
        initializeGUI();
        startServer();
    }

    private void initializeGUI() {
        setTitle("Server Control Panel");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(230, 230, 250));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        statusLabel = new JLabel("Server starting...");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(new Color(25, 25, 112));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        voiceCallButton = createButton("Voice Call", new Color(30, 144, 255));
        videoCallButton = createButton("Video Call", new Color(138, 43, 226));
        videoCallButton.addActionListener(e -> initiateVideoCall());

        JPanel callPanel = new JPanel();
        callPanel.setBackground(new Color(230, 230, 250));
        callPanel.add(voiceCallButton);
        callPanel.add(videoCallButton);
        statusPanel.add(callPanel, BorderLayout.EAST);

        add(statusPanel, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setForeground(new Color(0, 51, 102));
        chatArea.setBackground(Color.WHITE);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        add(chatScroll, BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.addActionListener(e -> sendMessage());

        sendButton = createButton("Send", new Color(34, 139, 34));
        sendButton.addActionListener(e -> sendMessage());

        fileButton = createButton("Send File", new Color(255, 165, 0));
        fileButton.addActionListener(e -> sendFile());

        JPanel controlPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(230, 230, 250));
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);

        controlPanel.add(inputField, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.EAST);
        add(controlPanel, BorderLayout.SOUTH);

        JPanel videoCallPanel = new JPanel();
        videoCallPanel.setBackground(new Color(245, 245, 245));
        receiveCallButton = createButton("Receive Call", new Color(34, 139, 34));
        receiveCallButton.addActionListener(e -> receiveVideoCall());

        endCallButton = createButton("End Call", new Color(244, 67, 54));
        endCallButton.addActionListener(e -> endVideoCall());
        endCallButton.setEnabled(false);

        cancelCallButton = createButton("Cancel Call", new Color(255, 69, 0));
        cancelCallButton.addActionListener(e -> cancelVideoCall());

        videoCallPanel.add(receiveCallButton);
        videoCallPanel.add(endCallButton);
        videoCallPanel.add(cancelCallButton);
        add(videoCallPanel, BorderLayout.EAST);

        setVisible(true);
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return button;
    }

    private void startServer() {
        new Thread(() -> {
            try {
                generateCredentials();
                serverSocket = new ServerSocket(2100);
                updateStatus("Server running on port 2100", new Color(34, 139, 34));
                displayMessage("Server started successfully\nConnection ID: " + connectionId + "\nPassword: " + connectionPassword);

                while (true) {
                    displayMessage("Waiting for client connection...");
                    clientSocket = serverSocket.accept();
                    String clientIP = clientSocket.getInetAddress().getHostAddress();
                    displayMessage("Client connected from IP: " + clientIP);
                    updateStatus("Client connected from IP: " + clientIP, new Color(76, 175, 80));
                    displayMessage("Client started successfully");
                    displayMessage("Client connected from IP: " + clientIP);

                    br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    ps = new PrintStream(clientSocket.getOutputStream(), true);

                    handleClientConnection(clientIP);
                }
            } catch (IOException e) {
                displayMessage("Error: " + e.getMessage());
                updateStatus("Server error", Color.RED);
            }
        }).start();
    }

    private void generateCredentials() {
        Random random = new Random();
        connectionId = "ID" + (random.nextInt(9000) + 1000);
        connectionPassword = "PASS" + (random.nextInt(9000) + 1000);
    }

    private void handleClientConnection(String clientIP) {
        try {
            String id = br.readLine();
            String password = br.readLine();

            if (validateCredentials(id, password)) {
                ps.println("SUCCESS");
                displayMessage("Client authenticated from IP: " + clientIP + "\nWaiting for messages...");
                displayMessage("Client authenticated from IP: " + clientIP);
                displayMessage("Waiting for messages...");

                String message;
                while ((message = br.readLine()) != null) {
                    if ("[VIDEO_CALL_REQUEST]".equals(message)) {
                        displayMessage("Incoming video call from " + clientIP);
                    } else if ("[FILE_TRANSFER]".equals(message)) {
                        receiveFile();
                    } else {
                        String finalMessage = message;
                        SwingUtilities.invokeLater(() -> chatArea.append("Client: " + finalMessage + "\n"));
                    }
                }
            } else {
                ps.println("INVALID_CREDENTIALS");
                clientSocket.close();
                updateStatus("Authentication failed for IP: " + clientIP, new Color(244, 67, 54));
                displayMessage("Invalid credentials from IP: " + clientIP + "\nConnection closed");
            }
        } catch (IOException e) {
            displayMessage("Client disconnected");
        }
    }

    private boolean validateCredentials(String id, String password) {
        return id.equals(connectionId) && password.equals(connectionPassword);
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            ps.println(message);
            displayMessage("Server: " + message);
            inputField.setText("");
        }
    }

    private void initiateVideoCall() {
        chatArea.append("Starting video call...\n");
        SwingUtilities.invokeLater(() -> {
            videoCallFrame = new JFrame("Video Call");
            videoCallFrame.setSize(800, 600);
            videoCallFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            videoCallFrame.setLayout(new BorderLayout());

            JLabel callStatus = new JLabel("Connecting...");
            callStatus.setHorizontalAlignment(SwingConstants.CENTER);
            callStatus.setFont(new Font("Segoe UI", Font.BOLD, 16));
            videoCallFrame.add(callStatus, BorderLayout.CENTER);

            JPanel callControlPanel = new JPanel(new FlowLayout());

            receiveCallButton.addActionListener(e -> {
                callStatus.setText("Call Connected");
                startCamera();
            });

            endCallButton = createButton("End Call", new Color(255, 69, 0));
            endCallButton.addActionListener(e -> {
                callStatus.setText("Call Ended");
                videoCallFrame.dispose();
            });

            cancelCallButton = createButton("Cancel Call", new Color(255, 165, 0));
            cancelCallButton.addActionListener(e -> {
                callStatus.setText("Call Canceled");
                videoCallFrame.dispose();
            });

            callControlPanel.add(receiveCallButton);
            callControlPanel.add(endCallButton);
            callControlPanel.add(cancelCallButton);

            videoCallFrame.add(callControlPanel, BorderLayout.SOUTH);
            videoCallFrame.setVisible(true);
        });
    }

    private void startCamera() {
        cameraPanel = new CameraPanel();
        videoCallFrame.getContentPane().add(cameraPanel, BorderLayout.CENTER);
        cameraPanel.startCamera();
    }

    private class CameraPanel extends JPanel {
        private Dimension cameraResolution = new Dimension(640, 480);
        private boolean isRunning = false;

        public void startCamera() {
            isRunning = true;
            new Thread(() -> {
                while (isRunning) {
                    repaint();
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (isRunning) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    private void receiveVideoCall() {
        displayMessage("Video call accepted");
        endCallButton.setEnabled(true);
        receiveCallButton.setEnabled(false);
    }

    private void endVideoCall() {
        displayMessage("Video call ended");
        endCallButton.setEnabled(false);
        receiveCallButton.setEnabled(true);
    }

    private void cancelVideoCall() {
        ps.println("[VIDEO_CALL_CANCEL]");
        displayMessage("Video call canceled");
    }

    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                ps.println("[FILE_TRANSFER]");
                ps.println(file.getName());
                FileInputStream fis = new FileInputStream(file);
                OutputStream os = clientSocket.getOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
                fis.close();
                displayMessage("File sent: " + file.getName());
            } catch (IOException e) {
                displayMessage("Failed to send file: " + e.getMessage());
            }
        }
    }

    private void receiveFile() {
        try {
            String fileName = br.readLine();
            File receivedFile = new File(fileName);
            FileOutputStream fos = new FileOutputStream(receivedFile);
            InputStream is = clientSocket.getInputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            displayMessage("File received: " + fileName);
        } catch (IOException e) {
            displayMessage("Failed to receive file: " + e.getMessage());
        }
    }

    private void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    private void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Server Status: " + status);
            statusLabel.setForeground(color);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerLoginGUI());
    }
}

class ServerLoginGUI extends JFrame {
    private JTextField idField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private static final String SERVER_ID = "admin";
    private static final String SERVER_PASSWORD = "password";

    public ServerLoginGUI() {
        setTitle("Server Login");
        setSize(400, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel idLabel = new JLabel("Server ID:");
        idField = new JTextField();

        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField();

        loginButton = new JButton("Login");
        loginButton.addActionListener(e -> verifyLogin());

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);

        panel.add(idLabel);
        panel.add(idField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(new JLabel());
        panel.add(loginButton);

        add(panel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void verifyLogin() {
        String enteredId = idField.getText().trim();
        String enteredPassword = new String(passwordField.getPassword()).trim();

        if (enteredId.equals(SERVER_ID) && enteredPassword.equals(SERVER_PASSWORD)) {
            JOptionPane.showMessageDialog(this, "Server Login Successful", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new ServerGUI();
        } else {
            statusLabel.setText("Invalid Credentials. Try Again!");
        }
    }
}
