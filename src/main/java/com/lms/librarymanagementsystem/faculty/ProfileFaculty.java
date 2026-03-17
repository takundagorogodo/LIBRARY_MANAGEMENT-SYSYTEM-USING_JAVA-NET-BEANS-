/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.lms.librarymanagementsystem.faculty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;

public class ProfileFaculty extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFaculty.class.getName());

    Connection conn;
    PreparedStatement pst;
    ResultSet rs;

    // PASS LOGGED-IN FACULTY ID HERE
    private int userId;

public ProfileFaculty(int userId) {
    this.userId = userId;
    initComponents();
    makeFieldsReadOnly();
    connect();
    checkDatabaseData();  // Add this line
    loadFacultyProfile();
}
 
 
private void checkDatabaseData() {
    try {
        System.out.println("=== Checking data for user_id: " + userId + " ===");
        
        // Check users table
        pst = conn.prepareStatement(
            "SELECT user_id, full_name, department, role FROM users WHERE user_id = ?"
        );
        pst.setInt(1, userId);
        rs = pst.executeQuery();
        
        if (rs.next()) {
            System.out.println("Users table:");
            System.out.println("  User ID: " + rs.getInt("user_id"));
            System.out.println("  Full Name: " + rs.getString("full_name"));
            System.out.println("  Department: " + rs.getString("department"));
            System.out.println("  Role: " + rs.getString("role"));
        }
        rs.close();
        pst.close();
        
        // Check faculty table
        pst = conn.prepareStatement(
            "SELECT * FROM faculty WHERE user_id = ?"
        );
        pst.setInt(1, userId);
        rs = pst.executeQuery();
        
        if (rs.next()) {
            System.out.println("Faculty table:");
            System.out.println("  Faculty ID: " + rs.getInt("faculty_id"));
            System.out.println("  Phone: " + rs.getString("phone"));
            System.out.println("  Fines: " + rs.getDouble("fines"));
            System.out.println("  Issued Books: " + rs.getInt("issued_books"));
        } else {
            System.out.println("No record in faculty table for this user!");
        }
        rs.close();
        pst.close();
        
    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void loadFacultyProfile() {
    try {
        // Get all data from users table
        pst = conn.prepareStatement(
            "SELECT * FROM users WHERE user_id = ? AND role = 'FACULTY'"
        );
        pst.setInt(1, userId);
        rs = pst.executeQuery();

        if (!rs.next()) {
            JOptionPane.showMessageDialog(this, "Faculty not found! Make sure you're logged in as faculty.");
            return;
        }
        
        // Get data from users table
        String fullName = rs.getString("full_name");
        String department = rs.getString("department");
        int user_id = rs.getInt("user_id");
        
        rs.close();
        pst.close();

        // Try to get faculty_id from faculty table
        int facultyId = 0;
        pst = conn.prepareStatement(
            "SELECT faculty_id FROM faculty WHERE user_id = ?"
        );
        pst.setInt(1, userId);
        rs = pst.executeQuery();

        if (rs.next()) {
            facultyId = rs.getInt("faculty_id");
        } else {
            // If no faculty record exists, use user_id as faculty_id
            facultyId = userId;
        }
        
        // Set the basic values
        jTextField2.setText(String.valueOf(facultyId));
        jTextField7.setText(fullName);
        jTextField4.setText(department);
        
        rs.close();
        pst.close();

        // ISSUED BOOK COUNT - Check both tables
        int issuedCount = 0;
        
        // First check faculty.issued_books
        pst = conn.prepareStatement(
            "SELECT issued_books FROM faculty WHERE user_id = ?"
        );
        pst.setInt(1, userId);
        rs = pst.executeQuery();
        
        if (rs.next()) {
            issuedCount = rs.getInt("issued_books");
        }
        rs.close();
        pst.close();
        
        // If faculty table doesn't have count or it's 0, calculate from book_issue
        if (issuedCount == 0) {
            pst = conn.prepareStatement(
                "SELECT COUNT(*) FROM book_issue " +
                "WHERE user_id = ? AND return_date IS NULL"
            );
            pst.setInt(1, userId);
            rs = pst.executeQuery();

            if (rs.next()) {
                issuedCount = rs.getInt(1);
            }
            rs.close();
            pst.close();
        }
        
        jTextField6.setText(String.valueOf(issuedCount));

        // CURRENT FINE - Check both tables
        double totalFine = 0;
        
        // First check faculty.fines
        pst = conn.prepareStatement(
            "SELECT fines FROM faculty WHERE user_id = ?"
        );
        pst.setInt(1, userId);
        rs = pst.executeQuery();
        
        if (rs.next()) {
            totalFine = rs.getDouble("fines");
        }
        rs.close();
        pst.close();
        
        // If faculty table doesn't have fines or it's 0, calculate from book_issue
        if (totalFine == 0) {
            pst = conn.prepareStatement(
                "SELECT IFNULL(SUM(DATEDIFF(CURDATE(), due_date) * 5), 0) as total_fine " +
                "FROM book_issue " +
                "WHERE user_id = ? " +
                "AND return_date IS NULL AND due_date < CURDATE()"
            );
            pst.setInt(1, userId);
            rs = pst.executeQuery();

            if (rs.next()) {
                totalFine = rs.getDouble("total_fine");
            }
            rs.close();
            pst.close();
        }
        
        jTextField5.setText("₹ " + String.format("%.2f", totalFine));

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error loading faculty profile: " + e.getMessage());
        e.printStackTrace();
    } finally {
        // Clean up resources
        try {
            if (rs != null) rs.close();
            if (pst != null) pst.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
    // ================= DB CONNECTION =================
    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/library_db?useSSL=false&serverTimezone=UTC",
                "root",
                "1234"
            );
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database connection failed");
        }
    }

    // ================= MAKE FIELDS READ ONLY =================
    private void makeFieldsReadOnly() {
        jTextField2.setEditable(false); // Emp ID
        jTextField7.setEditable(false); // Name
        jTextField4.setEditable(false); // Department
        jTextField5.setEditable(false); // Current fines
        jTextField6.setEditable(false); // Issued books count
    }




    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton2 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();

        jButton2.setText("jButton2");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(0, 102, 102));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 255)));
        jPanel1.setForeground(new java.awt.Color(0, 0, 255));

        jLabel2.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel2.setText("FACULTY PROFILE");

        jTextField4.addActionListener(this::jTextField4ActionPerformed);

        jLabel4.setText("Emp ID");

        jLabel5.setText("Name");

        jButton1.setText("Back");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jLabel8.setText("Department");

        jLabel10.setText("Current Fines");

        jLabel9.setText("Issued Books Count");

        jTextField7.addActionListener(this::jTextField7ActionPerformed);

        jTextField5.addActionListener(this::jTextField5ActionPerformed);

        jTextField6.addActionListener(this::jTextField6ActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(83, 83, 83)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel9))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField5)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(2, 2, 2)))
                .addGap(70, 70, 70))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(28, 28, 28)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE))
                .addGap(40, 40, 40)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(23, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField6ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField6ActionPerformed

    private void jTextField5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField5ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField5ActionPerformed

    private void jTextField7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField7ActionPerformed

    private void jTextField4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField4ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField4ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

               this.dispose();// TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
         java.awt.EventQueue.invokeLater(() -> {
            new ProfileFaculty(1).setVisible(true); // test faculty id
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    // End of variables declaration//GEN-END:variables
}
