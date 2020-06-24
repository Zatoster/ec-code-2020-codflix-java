package com.codflix.backend.features.user;

import com.codflix.backend.core.Conf;
import com.codflix.backend.core.Template;
import com.codflix.backend.models.User;
import com.codflix.backend.utils.URLUtils;
import com.google.protobuf.ByteString;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import javafx.util.converter.ByteStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserDao userDao = new UserDao();

    public String login(Request request, Response response) throws NoSuchAlgorithmException {
        if (request.requestMethod().equals("GET")) {
            Map<String, Object> model = new HashMap<>();
            return Template.render("auth_login.html", model);
        }

        // Get parameters
        Map<String, String> query = URLUtils.decodeQuery(request.body());
        String email = query.get("email");
        String password = query.get("password");
        byte[] passwordSHA = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));

        // Authenticate user
        User user = userDao.getUserByCredentials(email, bytesToHex(passwordSHA).toUpperCase());
        if (user == null) {
            logger.info("User not found. Redirect to login");
            response.removeCookie("session");
            response.redirect("/login");
            return "KO";
        }

        // Create session
        Session session = request.session(true);
        session.attribute("user_id", user.getId());
        response.cookie("/", "user_id", "" + user.getId(), 3600, true);

        // Redirect to medias page
        response.redirect(Conf.ROUTE_LOGGED_ROOT);
        return "OK";
    }

    public String signUp(Request request, Response response) {
        Map<String, Object> model = new HashMap<>();
        return Template.render("auth_signup.html", model);
    }

    public String logout(Request request, Response response) {
        Session session = request.session(false);
        if (session != null) {
            session.invalidate();
        }
        response.removeCookie("session");
        response.removeCookie("JSESSIONID");
        response.redirect("/");

        return "";
    }

    private static String bytesToHex(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
