package org.group4.quizapp;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/home")
    public String showHomePage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        String email = (String) session.getAttribute("userEmail");

        if (username == null) {
            return "redirect:/login"; // Redirect to log in if session is invalid
        }

        model.addAttribute("welcomeMessage", "Welcome back, " + username + "!");


        return "Home-Page"; // Ensure this matches your Thymeleaf template name
    }


}

