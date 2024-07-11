package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.Assert;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private MockHttpServletRequest mockRequest;

    private final String filePath = "accesses.txt";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

    }

    @Test
    public void addUserAccessAsAdminShouldGrantAccess() throws IOException {
        // Prepare
        AuthUtil.UserInfo userInfo = new AuthUtil.UserInfo();
        userInfo.role = "admin";
        Mockito.doReturn("Basic " + encode(userInfo)).when(mockRequest).getHeader("Authorization");

        UserController.UserAccess userAccess = new UserController.UserAccess();
        userAccess.userId = 1;
        userAccess.endpoint = Arrays.asList("resource A", "resource B");

        // Execute
        String result = userController.addUserAccess(userAccess, mockRequest);

        // Verify
        Assert.assertEquals("Access granted successfully", result);
        Assert.assertTrue(new File(filePath).exists());
        List<String> lines = readLinesFromFile();
        Assert.assertEquals("1=resource A,resource B", lines.get(0));
    }

    @Test
    public void addUserAccessNotAsAdminShouldFail() throws IOException {
        // Prepare
        AuthUtil.UserInfo userInfo = new AuthUtil.UserInfo();
        userInfo.role = "user";
        Mockito.doReturn("Basic " + encode(userInfo)).when(mockRequest).getHeader("Authorization");

        UserController.UserAccess userAccess = new UserController.UserAccess();
        userAccess.userId = 2;
        userAccess.endpoint = Arrays.asList("resource A", "resource B");

        // Execute
        String result = userController.addUserAccess(userAccess, mockRequest);

        // Verify
        Assert.assertEquals("Only admins can grant access", result);
        Assert.assertFalse(new File(filePath).exists());
    }


    @Test
    public void testAccessResourceAsAdmin() throws Exception {
        AuthUtil.UserInfo userInfo = new AuthUtil.UserInfo();
        userInfo.role = "admin";
        Mockito.doReturn("Basic " + encode(userInfo)).when(mockRequest).getHeader("Authorization");
        Assert.assertEquals("Admins have access to all resources", userController.accessResource("anyResource", mockRequest));
    }


    @Test
    public void testAccessResourceWithInvalidAuth() throws Exception {
        AuthUtil.UserInfo userInfo = new AuthUtil.UserInfo();
        userInfo.role = "user";
        userInfo.userId = 1;
        Mockito.doReturn("Basic " + encode(userInfo)).when(mockRequest).getHeader("Authorization");
        Assert.assertEquals("Access denied", userController.accessResource("resource C", mockRequest));
    }

    @Test
    public void testAccessGrantedForUser() throws Exception {
        AuthUtil.UserInfo userInfo = new AuthUtil.UserInfo();
        userInfo.role = "user";
        userInfo.userId = 1;
        Mockito.doReturn("Basic " + encode(userInfo)).when(mockRequest).getHeader("Authorization");
        // Mock file content should reflect user access rights (this part is tricky without mocking file I/O or changing approach)
        Assert.assertEquals("Access granted", userController.accessResource("resource A", mockRequest));
    }

    @Test
    public void testAccessDeniedForUser() throws Exception {
        AuthUtil.UserInfo userInfo = new AuthUtil.UserInfo();
        userInfo.role = "user";
        userInfo.userId = 2;
        Mockito.doReturn("Basic " + encode(userInfo)).when(mockRequest).getHeader("Authorization");
        Assert.assertEquals("Access denied", userController.accessResource("resource C", mockRequest));
    }

    private String encode(AuthUtil.UserInfo userInfo) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(userInfo);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode userInfo", e);
        }
    }

    private List<String> readLinesFromFile() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        List<String> lines = Arrays.asList(reader.readLine());
        reader.close();
        return lines;
    }


}
