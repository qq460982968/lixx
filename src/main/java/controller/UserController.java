package controller;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Arrays;
import java.util.List;

@RestController
public class UserController {
    private static final String FILE_PATH = "accesses.txt";

    @PostMapping("/admin/addUser")
    public String addUserAccess(@RequestBody UserAccess userAccess, HttpServletRequest request) {
        AuthUtil.UserInfo userInfo = AuthUtil.decodeUserInfo(request);
        if (!"admin".equals(userInfo.role)) {
            return "Only admins can grant access";
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(userAccess.userId + "=" + String.join(",", userAccess.endpoint));
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            return "Failed to grant access: " + e.getMessage();
        }
        return "Access granted successfully";
    }

    @GetMapping("/user/{resource}")
    public String accessResource(@PathVariable String resource, HttpServletRequest request) {
        AuthUtil.UserInfo userInfo = AuthUtil.decodeUserInfo(request);
        if ("admin".equals(userInfo.role)) {
            return "Admins have access to all resources";
        }

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts[0].equals(String.valueOf(userInfo.userId))) {
                    List<String> resources = Arrays.asList(parts[1].split(","));
                    if (resources.contains(resource)) {
                        return "Access granted";
                    }
                }
            }
        } catch (IOException e) {
            return "Failed to verify access: " + e.getMessage();
        }
        return "Access denied";
    }

    public static class UserAccess {
        public int userId;
        public List<String> endpoint;
    }
}