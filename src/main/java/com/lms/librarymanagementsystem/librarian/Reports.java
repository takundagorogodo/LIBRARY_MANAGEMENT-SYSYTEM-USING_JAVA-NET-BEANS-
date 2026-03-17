/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.lms.librarymanagementsystem.librarian;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;

public class Reports extends javax.swing.JFrame {
    private static final java.util.logging.Logger logger =
        java.util.logging.Logger.getLogger(Reports.class.getName());

    Connection conn;
    PreparedStatement pst;
    ResultSet rs;

    public Reports() {
        initComponents();
        connect();
    }

    // ================= DATABASE CONNECTION =================
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
    
    private void formatAsParagraph() {
    String selected = jComboBox1.getSelectedItem().toString();
    
    try {
        switch (selected) {
            case "books":
                pst = conn.prepareStatement(
                    "SELECT COUNT(*) as total, " +
                    "SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END) as available, " +
                    "SUM(CASE WHEN status = 'BORROWED' THEN 1 ELSE 0 END) as borrowed " +
                    "FROM books"
                );
                rs = pst.executeQuery();
                if (rs.next()) {
                    jTextPane1.setText("BOOKS REPORT\n\n" +
                        "Total books in library: " + rs.getInt("total") + "\n" +
                        "Available for borrowing: " + rs.getInt("available") + "\n" +
                        "Currently borrowed: " + rs.getInt("borrowed") + "\n\n" +
                        "The library maintains a collection of books covering various subjects. " +
                        "The current inventory shows healthy availability for students and faculty."
                    );
                }
                break;
                
            case "faculty":
                pst = conn.prepareStatement(
                    "SELECT COUNT(*) as total, " +
                    "GROUP_CONCAT(DISTINCT u.department) as departments " +
                    "FROM users u " +
                    "INNER JOIN faculty f ON u.user_id = f.user_id " +
                    "WHERE u.role = 'FACULTY'"
                );
                rs = pst.executeQuery();
                if (rs.next()) {
                    String depts = rs.getString("departments");
                    jTextPane1.setText("FACULTY REPORT\n\n" +
                        "Total faculty members: " + rs.getInt("total") + "\n" +
                        "Departments represented: " + (depts != null ? depts : "N/A") + "\n\n" +
                        "Faculty members are registered library users with borrowing privileges. " +
                        "They contribute to the academic resources and help maintain the library collection."
                    );
                }
                break;
                
            case "students":
                pst = conn.prepareStatement(
                    "SELECT COUNT(*) as total " +
                    "FROM users u " +
                    "INNER JOIN student s ON u.user_id = s.user_id " +
                    "WHERE u.role = 'STUDENT'"
                );
                rs = pst.executeQuery();
                if (rs.next()) {
                    jTextPane1.setText("STUDENTS REPORT\n\n" +
                        "Total registered students: " + rs.getInt("total") + "\n\n" +
                        "Students are active library users who utilize resources for academic purposes. " +
                        "They have borrowing rights, seat reservation privileges, and access to study materials."
                    );
                }
                break;
                
            case "fines":
                pst = conn.prepareStatement(
                    "SELECT " +
                    "(SELECT SUM(fines) FROM faculty) as faculty_fines, " +
                    "(SELECT SUM(fines) FROM student) as student_fines"
                );
                rs = pst.executeQuery();
                if (rs.next()) {
                    double facultyFines = rs.getDouble("faculty_fines");
                    double studentFines = rs.getDouble("student_fines");
                    double totalFines = facultyFines + studentFines;
                    
                    jTextPane1.setText("FINES REPORT\n\n" +
                        "Total fines collected: ₹" + String.format("%.2f", totalFines) + "\n" +
                        "Faculty fines: ₹" + String.format("%.2f", facultyFines) + "\n" +
                        "Student fines: ₹" + String.format("%.2f", studentFines) + "\n\n" +
                        "The fine system helps maintain timely returns of library materials. " +
                        "Fines are imposed for overdue books and damaged materials to ensure proper library management."
                    );
                }
                break;
                
            case "overdue":
                pst = conn.prepareStatement(
                    "SELECT COUNT(*) as total_overdue, " +
                    "AVG(DATEDIFF(CURDATE(), due_date)) as avg_days_overdue, " +
                    "MAX(DATEDIFF(CURDATE(), due_date)) as max_days_overdue " +
                    "FROM book_issue " +
                    "WHERE return_date IS NULL AND due_date < CURDATE()"
                );
                rs = pst.executeQuery();
                if (rs.next()) {
                    jTextPane1.setText("OVERDUE BOOKS REPORT\n\n" +
                        "Total overdue books: " + rs.getInt("total_overdue") + "\n" +
                        "Average days overdue: " + String.format("%.1f", rs.getDouble("avg_days_overdue")) + " days\n" +
                        "Maximum days overdue: " + rs.getInt("max_days_overdue") + " days\n\n" +
                        "Overdue books affect library circulation and availability for other users. " +
                        "Reminders are sent to users with overdue materials to ensure timely returns."
                    );
                }
                break;
        }
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error generating paragraph report: " + e.getMessage());
        e.printStackTrace();
    }
}

    private void generateReport() {
    String selected = jComboBox1.getSelectedItem().toString();
    jTextPane1.setText("");
    
    try {
        switch (selected) {
            case "books":
                pst = conn.prepareStatement(
                    "SELECT book_id, title, author, status FROM books ORDER BY title"
                );
                rs = pst.executeQuery();
                jTextPane1.setText("📚 BOOKS REPORT\n\n");
                while (rs.next()) {
                    jTextPane1.setText(jTextPane1.getText()
                            + "• ID: " + rs.getInt("book_id")
                            + " | Title: " + rs.getString("title")
                            + " | Author: " + rs.getString("author")
                            + " | Status: " + rs.getString("status")
                            + "\n");
                }
                break;
                
            case "faculty":
                pst = conn.prepareStatement(
                    "SELECT u.user_id, u.full_name, u.email, u.department, f.phone, f.fines " +
                    "FROM users u " +
                    "INNER JOIN faculty f ON u.user_id = f.user_id " +
                    "WHERE u.role = 'FACULTY' ORDER BY u.full_name"
                );
                rs = pst.executeQuery();
                jTextPane1.setText("👨‍🏫 FACULTY REPORT\n\n");
                while (rs.next()) {
                    jTextPane1.setText(jTextPane1.getText()
                            + "• User ID: " + rs.getInt("user_id")
                            + " | Name: " + rs.getString("full_name")
                            + " | Email: " + rs.getString("email")
                            + " | Department: " + rs.getString("department")
                            + " | Phone: " + rs.getString("phone")
                            + " | Fines: ₹" + rs.getDouble("fines")
                            + "\n");
                }
                break;
                
            case "students":
                pst = conn.prepareStatement(
                    "SELECT u.user_id, u.full_name, u.email, u.department, s.phone, s.fines, s.reserved_seat " +
                    "FROM users u " +
                    "INNER JOIN student s ON u.user_id = s.user_id " +
                    "WHERE u.role = 'STUDENT' ORDER BY u.full_name"
                );
                rs = pst.executeQuery();
                jTextPane1.setText("👨‍🎓 STUDENTS REPORT\n\n");
                while (rs.next()) {
                    jTextPane1.setText(jTextPane1.getText()
                            + "• User ID: " + rs.getInt("user_id")
                            + " | Name: " + rs.getString("full_name")
                            + " | Email: " + rs.getString("email")
                            + " | Department: " + rs.getString("department")
                            + " | Phone: " + rs.getString("phone")
                            + " | Fines: ₹" + rs.getDouble("fines")
                            + " | Seat Reserved: " + (rs.getInt("reserved_seat") == 1 ? "Yes" : "No")
                            + "\n");
                }
                break;
                
            case "fines":
                // Remain same as before...
                break;
                
            case "overdue":
                // Remain same as before...
                break;
        }
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error generating report: " + e.getMessage());
        e.printStackTrace();
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(153, 153, 153));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 255)));
        jPanel1.setForeground(new java.awt.Color(153, 153, 255));

        jLabel1.setFont(new java.awt.Font("Times New Roman", 1, 24)); // NOI18N
        jLabel1.setText("GENERATE REPORTS");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "books", "faculty", "students", "fines", "overdue" }));
        jComboBox1.addActionListener(this::jComboBox1ActionPerformed);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel2.setText("Select Report");

        jButton1.setText("Generate");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jButton2.setText("Back");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(44, 44, 44))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(73, 73, 73)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 273, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(172, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jTextPane1);

        jLabel3.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel3.setText("Report Details");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 646, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(217, 217, 217)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(13, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 333, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(26, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
         // Use this for paragraph format:
    formatAsParagraph();
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
 generateReport();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to go back?",
            "Confirm",
            JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            this.dispose();
            new MainLibrarian().setVisible(true);
        }        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new Reports().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables
}
