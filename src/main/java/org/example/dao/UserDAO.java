package org.example.dao;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.example.database.Database;
import org.example.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /**
     * Create a new user account
     * @param user User object with username, password, and role
     * @return true if successful, false otherwise
     */
    public static boolean createUser(User user) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = Database.getMasterConnection(); // Always use master for writes
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash the password before storing
            String hashedPassword = BCrypt.withDefaults().hashToString(12, user.getPassword().toCharArray());

            stmt.setString(1, user.getUsername());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, user.getRole());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user password
     * @param username Username to update
     * @param newPassword New password (will be hashed)
     * @return true if successful, false otherwise
     */
    public static boolean updatePassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = Database.getMasterConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());

            stmt.setString(1, hashedPassword);
            stmt.setString(2, username);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a user account
     * @param username Username to delete
     * @return true if successful, false otherwise
     */
    public static boolean deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";

        try (Connection conn = Database.getMasterConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if username already exists
     * @param username Username to check
     * @return true if exists, false otherwise
     */
    public static boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error checking if user exists: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get all users (without passwords for security)
     * @return List of all users
     */
    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, role FROM users ORDER BY username";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                // Don't include password for security
                users.add(user);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving users: " + e.getMessage());
            e.printStackTrace();
        }

        return users;
    }

    /**
     * Get user by username (without password for security)
     * @param username Username to find
     * @return User object without password, null if not found
     */
    public static User getUserByUsername(String username) {
        String sql = "SELECT id, username, role FROM users WHERE username = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                return user;
            }

        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Search users by username or role
     * @param query Search query
     * @return List of matching users
     */
    public static List<User> searchUsers(String query) {
        List<User> users = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return getAllUsers();
        }

        String sql = "SELECT id, username, role FROM users WHERE " +
                "LOWER(username) LIKE LOWER(?) OR " +
                "LOWER(role) LIKE LOWER(?) " +
                "ORDER BY username";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + query.trim() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                users.add(user);
            }

        } catch (SQLException e) {
            System.err.println("Error searching users: " + e.getMessage());
            e.printStackTrace();
        }

        return users;
    }

    /**
     * Generate a random password
     * @param length Length of password
     * @return Random password string
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }

        return password.toString();
    }

    /**
     * Get total number of users
     * @return Total user count
     */
    public static int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error getting user count: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Get user count by role
     * @param role Role to count
     * @return Count of users with the specified role
     */
    public static int getUserCountByRole(String role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error getting user count by role: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }
}