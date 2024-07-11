package controller;

import java.util.Base64;
import javax.servlet.http.HttpServletRequest;  
import com.fasterxml.jackson.databind.ObjectMapper;  
  
public class AuthUtil {  
  
    public static UserInfo decodeUserInfo(HttpServletRequest request) {  
        String authHeader = request.getHeader("Authorization");  
        if (authHeader == null || !authHeader.startsWith("Basic ")) {  
            throw new RuntimeException("Authorization header is missing or invalid");  
        }  
  
        String base64Credentials = authHeader.substring("Basic ".length());  
        String credentials = new String(Base64.getDecoder().decode(base64Credentials));  
  
        try {  
            return new ObjectMapper().readValue(credentials, UserInfo.class);  
        } catch (Exception e) {  
            throw new RuntimeException("Failed to parse credentials", e);  
        }  
    }  
  
    public static class UserInfo {  
        public int userId;  
        public String accountName;  
        public String role;  
    }  
}