import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;

public class ClientGUI extends JFrame {
    private JTextField serverIdField;
    private JPasswordField passwordField;
    private JTextField inputField;
    private JTextArea chatArea;
    private JButton sendButton, attachButton, voiceCallButton, videoCallButton;
    private JCheckBox showPasswordCheckbox;
    private Socket socket;
    private BufferedReader br;
    private PrintStream ps;
    private boolean isConnected;
    private JFrame videoCallFrame;
    private JButton receiveCallButton, endCallButton, cancelCallButton;
    private CameraPanel cameraPanel;

    public ClientGUI() {
        createLoginUI();
    }

    private void createLoginUI() {
        setTitle("Chat Application - Login");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(new Color(240, 248, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel serverLabel = createLabel("Server ID:");
        loginPanel.add(serverLabel, gbc);

        gbc.gridx = 1;
        serverIdField = new JTextField(15);
        loginPanel.add(serverIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel passwordLabel = createLabel("Password:");
        loginPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        loginPanel.add(passwordField, gbc);

        gbc.gridy = 2;
        showPasswordCheckbox = new JCheckBox("Show Password");
        showPasswordCheckbox.setBackground(new Color(240, 248, 255));
        showPasswordCheckbox.addActionListener(e -> passwordField.setEchoChar(
                showPasswordCheckbox.isSelected() ? '\0' : '●'));
        loginPanel.add(showPasswordCheckbox, gbc);

        gbc.gridy = 3;
        JButton loginButton = createButton("Login", new Color(70, 130, 180));
        loginButton.addActionListener(e -> attemptLogin());
        loginPanel.add(loginButton, gbc);

        add(loginPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(25, 25, 112));
        return label;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return button;
    }

    private void attemptLogin() {
        String serverId = serverIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (serverId.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill out all fields.");
            return;
        }
        try {
            socket = new Socket("localhost", 2100);
            ps = new PrintStream(socket.getOutputStream(), true);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            ps.println(serverId);
            ps.println(password);
            String response = br.readLine();
            if ("SUCCESS".equals(response)) {
                isConnected = true;
                JOptionPane.showMessageDialog(this, "Server connected successfully!");
                createMainUI();
            } else {
                JOptionPane.showMessageDialog(this, "Login failed.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to server.");
        }
    }

    private void createMainUI() {
        setTitle("Chat Application");
        setSize(800, 600);
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBackground(new Color(255, 255, 255));
        chatArea.setForeground(new Color(0, 51, 102));
        JScrollPane chatScroll = new JScrollPane(chatArea);

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.addActionListener(e -> sendMessage());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());

        sendButton = createButton("Send", new Color(34, 139, 34));
        sendButton.addActionListener(e -> sendMessage());

        attachButton = createButton("Attach", new Color(255, 165, 0));
        attachButton.addActionListener(e -> sendFile());

        voiceCallButton = createButton("Voice Call", new Color(30, 144, 255));
        voiceCallButton.addActionListener(e -> initiateVoiceCall());

        videoCallButton = createButton("Video Call", new Color(138, 43, 226));
        videoCallButton.addActionListener(e -> initiateVideoCall());

        buttonPanel.add(sendButton);
        buttonPanel.add(attachButton);
        buttonPanel.add(voiceCallButton);
        buttonPanel.add(videoCallButton);

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(chatScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        new Thread(this::receiveMessages).start();
        setVisible(true);
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty() && isConnected) {
            ps.println(msg);
            chatArea.append("You: " + msg + "\n");
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

            receiveCallButton = createButton("Receive Call", new Color(34, 139, 34));
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

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                ps.println("[FILE_TRANSFER]");
                ps.println(file.getName());
                FileInputStream fis = new FileInputStream(file);
                OutputStream os = socket.getOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
                fis.close();
                chatArea.append("File sent: " + file.getName() + "\n");
            } catch (IOException e) {
                chatArea.append("Failed to send file.\n");
            }
        }
    }

    private void receiveMessages() {
        try {
            String line;
            while (isConnected && (line = br.readLine()) != null) {
                String msg = line;
                if ("[FILE_TRANSFER]".equals(msg)) {
                    receiveFile();
                } else {
                    SwingUtilities.invokeLater(() -> chatArea.append("Server: " + msg + "\n"));
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection lost.");
            isConnected = false;
        }
    }

    private void receiveFile() {
        try {
            String fileName = br.readLine();
            File receivedFile = new File(fileName);
            FileOutputStream fos = new FileOutputStream(receivedFile);
            InputStream is = socket.getInputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            chatArea.append("File received: " + fileName + "\n");
        } catch (IOException e) {
            chatArea.append("Failed to receive file.\n");
        }
    }

    private void initiateVoiceCall() {
        chatArea.append("Starting voice call...\n");
        new Thread(() -> {
            try {
                // Implement voice call logic here
            } catch (Exception e) {
                chatArea.append("Voice call failed: " + e.getMessage() + "\n");
            }

        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}
