package org.group4.quizapp;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;

import javax.sql.DataSource;
import java.sql.*;

@Controller
@RequestMapping("/register")
public class RegisterController {

    @Autowired
    private DataSource dataSource;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register-page"; // Thymeleaf template for registration
    }

    @PostMapping
    public String registerUser(@ModelAttribute("user") @Valid User user, BindingResult result, Model model) {
        // Check for validation errors
        if (result.hasErrors()) {
            return "register-page"; // Stay on the registration page if there are validation errors
        }

        sanitizeInput(user);

        // Check if username or email already exists in the database
        if (usernameExists(user.getUsername())) {
            model.addAttribute("error", "Username already exists.");
            return "register-page"; // Stay on the registration page
        }

        if (emailExists(user.getEmail())) {
            model.addAttribute("error", "Email already exists.");
            return "register-page"; // Stay on the registration page
        }

        // Encrypt the password
        String encryptedPassword = passwordEncoder.encode(user.getPassword());

        // Insert the new user into the database
        try (Connection connection = dataSource.getConnection()) {

            String query = "INSERT INTO public.users (username, email, password, full_name) VALUES (?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, user.getUsername());
                preparedStatement.setString(2, user.getEmail());
                preparedStatement.setString(3, encryptedPassword);
                preparedStatement.setString(4, user.getName() != null ? user.getName() : "");
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            model.addAttribute("error", "Registration failed. Please try again.");
            return "register-page"; // Stay on the registration page
        }

        return "redirect:/login?success=registration"; // Redirect to login after successful registration
    }

    private void sanitizeInput(User user) {
        // Sanitize username and email to prevent SQL injection or invalid characters
        user.setUsername(user.getUsername().replaceAll("[^a-zA-Z0-9]", ""));
        user.setEmail(user.getEmail().replaceAll("[^a-zA-Z0-9@._-]", ""));
    }

    private boolean usernameExists(String username) {
        // Check if the username already exists in the database
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT COUNT(*) FROM public.users WHERE username = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean emailExists(String email) {
        // Check if the email already exists in the database
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT COUNT(*) FROM public.users WHERE username = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, email);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
