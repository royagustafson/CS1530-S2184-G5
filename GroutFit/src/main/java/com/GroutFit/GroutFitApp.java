package com.GroutFit;

import com.GroutFit.Model.ClothingItem;
import com.GroutFit.Model.Profile;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class GroutFitApp {

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(GroutFitApp.class);
        SessionFactory sf = new Configuration().configure().buildSessionFactory(); // Hibernate
        Session session = sf.openSession();

        staticFiles.location("public");
        HashMap<String, Boolean> loginTable = new HashMap<>();

        // Example routes
        get("/hello", (req, res) -> "Hello World");

        // Api calls
        path("/api", () -> {

            // Example queries
            path("/example", () -> {
                post("/login", (req, res) -> {
                    // http://sparkjava.com/documentation#request
                    return req.attribute("username") != null && req.attribute("password") != null;
                });
                get("/cart", (req, res) -> "This will load all items in shopping cart");
                get("/wishlist", (req, res) -> "This will load all items in wishlist");
                get("/outfits", (req, res) -> "This will load all user outfits/outfit feed");
                get("/outfit/:id", (req, res) -> "This will load an outfit with id" + req.params(":id"));
            });

            // Basic user functionality
            post("/register", (req, res) -> {
                try {
                    List<NameValuePair> pairs = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
                    Map<String, String> params = toMap(pairs);

                    Profile pro = Profile.register(
                            params.get("username"),
                            params.get("password"),
                            null,
                            null,
                            null
                    );
                    session.beginTransaction();
                    session.save(pro);
                    session.getTransaction().commit();

                    res = success(res);
                } catch (Exception e) {
                    res.body("Registration failed");
                    res.status(500); // internal server error
                }
                return res.body();
            });
            post("/login", (req, res) -> {
                try {

                    List<NameValuePair> pairs = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
                    Map<String, String> params = toMap(pairs);

                    Profile user = session.get(Profile.class, params.get("username"));
                    if (user == null) {
                        res.body("Invalid username");
                        res.status(401);
                    } else if (user.login(params.get("password"))) {
                        logger.info(String.format("User %s logged in", user.getEmail()));
                        loginTable.put(user.getEmail(), true);
                        res = success(res);
                    } else {
                        res.body("Invalid password");
                        res.status(401);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                }
                return res.body();
            });
            post("/logout", (req, res) -> {
                try {
                    List<NameValuePair> pairs = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
                    Map<String, String> params = toMap(pairs);

                    String username = params.get("username");
                    if (loginTable.get(username) != null) {
                        logger.info(String.format("User %s logged out", username));
                        loginTable.remove(username);
                        res = success(res);
                    } else {
                        res.body("User is not logged in");
                        res.status(401);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                }
                return res.body();
            });

            // Item functionality
            get("/item/:item_id", (req, res) -> {
                try {
                    List<NameValuePair> pairs = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
                    Map<String, String> params = toMap(pairs);

                    int id = Integer.parseInt(params.get("item_id"));
                    ClothingItem item = session.get(ClothingItem.class, id);
                    if (item != null) {
                        System.out.println("Sending success response");
                        System.out.println(item);
                        res.body(item.toString());
                        res.status(200);
                    } else {
                        res.body(String.format("No results for id %d", id));
                        res.status(200);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                }
                return res.body();
            });
            get("/item/:query", (req, res) -> {
                String regex = "[0-9]{9}";

                res.body("Not implemented");
                res.status(200);
                // To be implemented
                return res.body();
            });
        });
    }

    private static Map<String, String> toMap(List<NameValuePair> pairs){
        Map<String, String> map = new HashMap<>();
        for(int i=0; i<pairs.size(); i++){
            NameValuePair pair = pairs.get(i);
            map.put(pair.getName(), pair.getValue());
        }
        return map;
    }

    // TODO is this sort of thing helpful
    private static Response success(Response res) {
        res.body("Successful");
        res.status(200);
        return res;
    }
}