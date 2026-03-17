/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

package com.lms.librarymanagementsystem.librarian;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 *
 * @author PROSPERITY
 */

public class BookIssue extends javax.swing.JFrame {
    private static final java.util.logging.Logger logger =
        java.util.logging.Logger.getLogger(Reports.class.getName());

    Connection conn;
    PreparedStatement pst;
    ResultSet rs;

    public BookIssue() {
        initComponents();
        connect();
        loadIssuedBooks();
        jButton2.setEnabled(false); // Issue Book disabled until search
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

    private void loadIssuedBooks() {
    try {
        pst = conn.prepareStatement(
            "SELECT bi.issue_id, bi.book_id, b.title, bi.user_id, " +
            "bi.issue_date, bi.due_date, bi.return_date, b.status " +
            "FROM book_issue bi JOIN books b ON bi.book_id = b.book_id " +
            "WHERE bi.return_date IS NULL " +
            "ORDER BY bi.issue_date DESC"
        );
        rs = pst.executeQuery();

        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("issue_id"),
                rs.getInt("book_id"),
                rs.getString("title"),
                rs.getInt("user_id"),
                rs.getDate("issue_date"),
                rs.getDate("due_date"),
                rs.getDate("return_date"),
                rs.getString("status")
            });
        }

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Failed to load issued books");
        e.printStackTrace();
    }
}
  
    private void issueBook() {
    try {
        if (txtBookId.getText().isEmpty() || txtUserId.getText().isEmpty() || 
            txtIssueDate.getText().isEmpty() || txtDueDate.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields");
            return;
        }

        int bookId = Integer.parseInt(txtBookId.getText());
        int userId = Integer.parseInt(txtUserId.getText());
        
        String issueDateStr = txtIssueDate.getText();
        String dueDateStr = txtDueDate.getText();
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date utilIssueDate = sdf.parse(issueDateStr);
        java.util.Date utilDueDate = sdf.parse(dueDateStr);
        
        Date issueDate = new Date(utilIssueDate.getTime());
        Date dueDate = new Date(utilDueDate.getTime());

        pst = conn.prepareStatement(
            "SELECT b.status FROM books b " +
            "LEFT JOIN book_issue bi ON b.book_id = bi.book_id AND bi.return_date IS NULL " +
            "WHERE b.book_id = ?"
        );
        pst.setInt(1, bookId);
        rs = pst.executeQuery();
        
        if (rs.next()) {
            String status = rs.getString("status");
            if (!"AVAILABLE".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(this, "Book is not available for issue");
                return;
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Book ID");
            return;
        }

        pst = conn.prepareStatement(
            "SELECT user_id FROM users WHERE user_id = ? AND role IN ('STUDENT', 'FACULTY')"
        );
        pst.setInt(1, userId);
        rs = pst.executeQuery();
        
        if (!rs.next()) {
            JOptionPane.showMessageDialog(this, "Invalid User ID. User must be a student or faculty member.");
            return;
        }

        pst = conn.prepareStatement(
            "INSERT INTO book_issue (book_id, user_id, issue_date, due_date) " +
            "VALUES (?, ?, ?, ?)"
        );
        pst.setInt(1, bookId);
        pst.setInt(2, userId);
        pst.setDate(3, issueDate);
        pst.setDate(4, dueDate);
        pst.executeUpdate();

        pst = conn.prepareStatement(
            "UPDATE books SET status = 'ISSUED' WHERE book_id = ?"
        );
        pst.setInt(1, bookId);
        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Book issued successfully");
        loadIssuedBooks();
        clearFields();

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Please enter valid numeric IDs");
    } catch (ParseException e) {
        JOptionPane.showMessageDialog(this, "Invalid date format. Use DD/MM/YYYY");
    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error issuing book: " + e.getMessage());
    }
}

    private void searchBook() {
    if (txtBookId.getText().isEmpty()) {
        JOptionPane.showMessageDialog(this, "Enter Book ID");
        return;
    }

    int bookId;
    try {
        bookId = Integer.parseInt(txtBookId.getText());
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Book ID must be numeric");
        return;
    }

    try {
        pst = conn.prepareStatement(
            "SELECT b.status, bi.return_date " +
            "FROM books b " +
            "LEFT JOIN book_issue bi ON b.book_id = bi.book_id AND bi.return_date IS NULL " +
            "WHERE b.book_id = ?"
        );
        pst.setInt(1, bookId);
        rs = pst.executeQuery();

        if (rs.next()) {
            String status = rs.getString("status");
            Date returnDate = rs.getDate("return_date");
            
            if ("AVAILABLE".equalsIgnoreCase(status) || returnDate != null) {
                JOptionPane.showMessageDialog(this, "Book is available for issue");
                jButton2.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(this, "Book is currently issued and not returned yet");
                jButton2.setEnabled(false);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Book ID");
            jButton2.setEnabled(false);
        }

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error searching book: " + e.getMessage());
        e.printStackTrace();
    }
}
  
        // ================= RETURN BOOK =================
    private void returnBook() {

        int row = jTable1.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a record");
            return;
        }

        int issueId = Integer.parseInt(jTable1.getValueAt(row, 0).toString());
        int bookId = Integer.parseInt(jTable1.getValueAt(row, 1).toString());

        try {
            pst = conn.prepareStatement(
                "UPDATE book_issue SET return_date=CURDATE() WHERE issue_id=?"
            );
            pst.setInt(1, issueId);
            pst.executeUpdate();

            pst = conn.prepareStatement(
                "UPDATE books SET status='AVAILABLE' WHERE book_id=?"
            );
            pst.setInt(1, bookId);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Book returned");
            loadIssuedBooks();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error returning book");
        }
    }
// ================= CLEAR =================
    private void clearFields() {
        txtBookId.setText("");
        txtUserId.setText("");
        txtIssueDate.setText("");
        txtDueDate.setText("");
        jTable1.clearSelection();
        jButton2.setEnabled(false);
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton6 = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtBookId = new javax.swing.JTextField();
        txtUserId = new javax.swing.JTextField();
        txtIssueDate = new javax.swing.JTextField();
        txtDueDate = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();

        jButton6.setText("jButton6");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "issue_id", "book_id", "book_Title", "User ID", "Issue date", "Due date", "Return date", "Status"
            }
        ));
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jPanel1.setBackground(new java.awt.Color(204, 204, 204));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 255)));
        jPanel1.setForeground(new java.awt.Color(102, 102, 255));

        jLabel2.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel2.setText("Book Issue");

        jLabel3.setText("Book Id");

        txtIssueDate.addActionListener(this::txtIssueDateActionPerformed);

        txtDueDate.addActionListener(this::txtDueDateActionPerformed);

        jLabel4.setText("User id");

        jLabel5.setText("Issue Date");

        jLabel6.setText("Return date");

        jButton2.setText("Issue Book");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        jButton3.setText("Clear");
        jButton3.addActionListener(this::jButton3ActionPerformed);

        jButton5.setText("Return Book");
        jButton5.addActionListener(this::jButton5ActionPerformed);

        jButton4.setText("Back");
        jButton4.addActionListener(this::jButton4ActionPerformed);

        jButton1.setText("Search");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(26, 26, 26)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(txtDueDate, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                                    .addComponent(txtUserId)
                                    .addComponent(txtBookId)
                                    .addComponent(txtIssueDate)))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(98, 98, 98)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(126, 126, 126)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtBookId, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtUserId, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtIssueDate, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtDueDate, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(41, 41, 41)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(37, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(16, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 591, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 437, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtIssueDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtIssueDateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtIssueDateActionPerformed

    private void txtDueDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDueDateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDueDateActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
         issueBook();
         
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
int choice = JOptionPane.showConfirmDialog(
    this,
    "Are you sure you want to go back?",
    "Confirm",
    JOptionPane.YES_NO_OPTION
);

if (choice == JOptionPane.YES_OPTION) {
    this.dispose();
    new MainLibrarian().setVisible(true);
}
     
       
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
        // TODO add your handling code here:
             DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
    int selectIndex = jTable1.getSelectedRow();
    
    if (selectIndex >= 0) {
        // Clear all fields first
        txtBookId.setText("");
        txtUserId.setText("");
        txtIssueDate.setText("");
        txtDueDate.setText("");
        
        // Get the values from the selected row
        // Note: You might want to populate these fields differently depending on what you're trying to do
        // Currently, it's trying to populate with wrong column indices
        txtBookId.setText(model.getValueAt(selectIndex, 1).toString());  // book_id is column 1
        txtUserId.setText(model.getValueAt(selectIndex, 3).toString());  // user_id is column 3
        txtIssueDate.setText(model.getValueAt(selectIndex, 4).toString()); // issue_date is column 4
        txtDueDate.setText(model.getValueAt(selectIndex, 5).toString());  // due_date is column 5
        
        // Disable issue button when viewing an already issued book
        jButton2.setEnabled(false);
    }
    }//GEN-LAST:event_jTable1MouseClicked

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
      returnBook();
       
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    
  clearFields();

    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

    searchBook();
   // TODO add your handling code here:
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
        java.awt.EventQueue.invokeLater(() -> new BookIssue().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField txtBookId;
    private javax.swing.JTextField txtDueDate;
    private javax.swing.JTextField txtIssueDate;
    private javax.swing.JTextField txtUserId;
    // End of variables declaration//GEN-END:variables
}


