/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.sql.*;

public class TestDataBase {
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/library_db", 
                "root", 
                "1234"
            );
            
            System.out.println("✅ Database Connected!");
            
            // Check if tables exist
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "users", null);
            if (tables.next()) {
                System.out.println("✅ Users table exists");
            } else {
                System.out.println("❌ Users table doesn't exist");
            }
            
            tables = meta.getTables(null, null, "student", null);
            if (tables.next()) {
                System.out.println("✅ Student table exists");
            } else {
                System.out.println("❌ Student table doesn't exist");
            }
            
            conn.close();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }
}