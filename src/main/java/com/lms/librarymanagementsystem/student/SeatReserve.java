/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.lms.librarymanagementsystem.student;
import com.lms.librarymanagementsystem.student.MainStudent;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class SeatReserve extends javax.swing.JFrame {

    private final int studentId;
    private Timer refreshTimer;
    private Connection conn;
    
     public SeatReserve(int studentId) {
    this.studentId = studentId;
    initComponents();
    connect();

    // Initialize seats if table is empty
    initializeSeats();

    if (!isStudent(studentId)) {
        JOptionPane.showMessageDialog(
            this,
            "Seat reservation is allowed for STUDENTS only"
        );
        dispose();
        return;
    }

    loadSeatStatus();
    loadMySeat();
    startAutoRefresh();
}
     
     
    /* ================= BOOK SEAT ================= */

   private void bookSeat() {
    int seatNo = Integer.parseInt(jComboBox1.getSelectedItem().toString());

    try {
        // First free any expired seats
        freeExpiredSeats();
        
        // Check if student already has an active seat
        String check = "SELECT 1 FROM seat_reservation WHERE student_id=? AND time_out > NOW()";
        PreparedStatement pst = conn.prepareStatement(check);
        pst.setInt(1, studentId);

        if (pst.executeQuery().next()) {
            JOptionPane.showMessageDialog(this, "You already have an active seat reservation");
            return;
        }

        // Check if seat is currently occupied (not expired)
        check = "SELECT 1 FROM seat_reservation WHERE seat_no=? AND time_out > NOW()";
        pst = conn.prepareStatement(check);
        pst.setInt(1, seatNo);

        if (pst.executeQuery().next()) {
            JOptionPane.showMessageDialog(this, "Seat is currently occupied");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime out = now.plusHours(2);

        // If seat exists but expired, update it
        check = "SELECT 1 FROM seat_reservation WHERE seat_no=?";
        pst = conn.prepareStatement(check);
        pst.setInt(1, seatNo);
        
        if (pst.executeQuery().next()) {
            // Update existing seat (was expired)
            String update = "UPDATE seat_reservation SET status='OCCUPIED', time_in=?, time_out=?, student_id=? WHERE seat_no=?";
            pst = conn.prepareStatement(update);
            pst.setTimestamp(1, Timestamp.valueOf(now));
            pst.setTimestamp(2, Timestamp.valueOf(out));
            pst.setInt(3, studentId);
            pst.setInt(4, seatNo);
            pst.executeUpdate();
        } else {
            // Insert new seat
            String insert = "INSERT INTO seat_reservation (seat_no, status, time_in, time_out, student_id) VALUES (?,?,?,?,?)";
            pst = conn.prepareStatement(insert);
            pst.setInt(1, seatNo);
            pst.setString(2, "OCCUPIED");
            pst.setTimestamp(3, Timestamp.valueOf(now));
            pst.setTimestamp(4, Timestamp.valueOf(out));
            pst.setInt(5, studentId);
            pst.executeUpdate();
        }

        JOptionPane.showMessageDialog(this, "Seat booked successfully for 2 hours");

        loadSeatStatus();
        loadMySeat();
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error booking seat: " + e.getMessage());
    }
}
    
    /*
    @Override
public void dispose() {
    if (refreshTimer != null) {
        refreshTimer.stop();
    }
    try {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    super.dispose();
}
    */

 
    private void startAutoRefresh() {
    refreshTimer = new Timer(60000, e -> { // Refresh every minute (60,000 milliseconds)
        try {
            // Free expired seats first
            freeExpiredSeats();
            
            // Then refresh both tables
            loadSeatStatus();
            loadMySeat();
        } catch (Exception ex) {
            System.err.println("Error in auto-refresh: " + ex.getMessage());
        }
    });
    refreshTimer.start();
}
    /* ================= DATABASE ================= */

    private void connect() {
        try {
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/library_db",
                "root",
                "1234"
            );
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed");
        }
    }


 private void freeExpiredSeats() {
    try {
        String sql = "UPDATE seat_reservation SET status='FREE', student_id=NULL WHERE time_out < NOW() AND status != 'FREE'";
        PreparedStatement pst = conn.prepareStatement(sql);
        int freed = pst.executeUpdate();
        
        if (freed > 0) {
            System.out.println("Freed " + freed + " expired seats");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
 
 private void loadSeatStatus() {
    try {
        DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
        model.setRowCount(0);
        
        // First free any expired seats
        freeExpiredSeats();

        // Only show seats that are currently OCCUPIED
        String sql = "SELECT seat_no, status, time_out, student_id FROM seat_reservation WHERE status = 'OCCUPIED'";
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            String status = rs.getString("status");
            Timestamp timeOut = rs.getTimestamp("time_out");
            String timeDisplay;
            
            if (timeOut == null) {
                timeDisplay = "N/A";
            } else if (timeOut.toLocalDateTime().isBefore(LocalDateTime.now())) {
                timeDisplay = "TIME UP";
            } else {
                timeDisplay = timeOut.toString();
            }

            model.addRow(new Object[]{
                rs.getInt("seat_no"),
                status,
                timeDisplay,
                rs.getInt("student_id")
            });
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
 
 private void loadMySeat() {
    try {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        // Only show seats that are currently OCCUPIED by this student
        String sql = "SELECT seat_no, time_in, time_out FROM seat_reservation WHERE student_id=? AND status='OCCUPIED'";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, studentId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Timestamp out = rs.getTimestamp("time_out");
            Timestamp in = rs.getTimestamp("time_in");
            LocalDateTime outTime = out.toLocalDateTime();
            LocalDateTime now = LocalDateTime.now();
            
            String timeRemaining;
            if (now.isAfter(outTime)) {
                timeRemaining = "TIME UP";
            } else {
                long minsLeft = ChronoUnit.MINUTES.between(now, outTime);
                timeRemaining = minsLeft + " mins";
            }

            model.addRow(new Object[]{
                rs.getInt("seat_no"),
                in,
                out,
                timeRemaining
            });
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
 

 private void initializeSeats() {
    try {
        // Check if seats are initialized
        String checkSql = "SELECT COUNT(*) as count FROM seat_reservation";
        PreparedStatement checkPst = conn.prepareStatement(checkSql);
        ResultSet rs = checkPst.executeQuery();
        
        if (rs.next() && rs.getInt("count") == 0) {
            // Initialize 30 seats
            String insertSql = "INSERT INTO seat_reservation (seat_no, status) VALUES (?, 'FREE')";
            PreparedStatement insertPst = conn.prepareStatement(insertSql);
            
            for (int i = 1; i <= 30; i++) {
                insertPst.setInt(1, i);
                insertPst.addBatch();
            }
            insertPst.executeBatch();
            System.out.println("Initialized 30 seats");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
 
 private boolean isStudent(int userId) {
    try {
        // First check if it's a valid user
        String checkUserSql = "SELECT user_id FROM users WHERE user_id=?";
        PreparedStatement checkUserPst = conn.prepareStatement(checkUserSql);
        checkUserPst.setInt(1, userId);
        ResultSet userRs = checkUserPst.executeQuery();
        
        if (!userRs.next()) {
            JOptionPane.showMessageDialog(this, "Invalid user ID");
            return false;
        }
        
        // Now check if they're a student
        String sql = "SELECT s.student_id FROM student s WHERE s.user_id=?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();

        return rs.next(); // Returns true if they exist in student table
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}
    /* ================= UI EVENT
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jToggleButton2 = new javax.swing.JToggleButton();
        jLabel3 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(102, 102, 0));

        jTable1.setBackground(new java.awt.Color(102, 102, 0));
        jTable1.setForeground(new java.awt.Color(0, 102, 102));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "seat number", "Time in", "Time out", "Time remaining"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jPanel1.setBackground(new java.awt.Color(102, 102, 0));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 102, 102)));
        jPanel1.setForeground(new java.awt.Color(0, 102, 102));

        jLabel2.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel2.setText("SEAT RESERVATION");

        jButton1.setText("Back");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(154, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(143, 143, 143)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(50, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(102, 102, 0));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 102, 102)));

        jToggleButton2.setText("book a seat");
        jToggleButton2.addActionListener(this::jToggleButton2ActionPerformed);

        jLabel3.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 8, 8));
        jLabel3.setText("Choose Seat");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30" }));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(80, 80, 80)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jToggleButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                    .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jToggleButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(69, 69, 69))))
        );

        jTable2.setBackground(new java.awt.Color(102, 102, 0));
        jTable2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 102, 102)));
        jTable2.setForeground(new java.awt.Color(0, 102, 102));
        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "seat number", "status", "time to be free", "student_id "
            }
        ));
        jScrollPane2.setViewportView(jTable2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane2))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jToggleButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton2ActionPerformed
         bookSeat();        // TODO add your handling code here:
    }//GEN-LAST:event_jToggleButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to go back?",
            "Confirm",
            JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            this.dispose();
            new MainStudent(studentId).setVisible(true);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        int loggedInStudentId = 1; // replace with login session value
        SwingUtilities.invokeLater(() ->
            new SeatReserve(loggedInStudentId).setVisible(true)
        );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JToggleButton jToggleButton2;
    // End of variables declaration//GEN-END:variables
}
