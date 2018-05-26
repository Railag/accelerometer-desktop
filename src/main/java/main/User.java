package main;


import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class User {

    private static User user;

    private final static String ID_KEY = "id";
    private final static String LOGIN_KEY = "login";
    private final static String EMAIL_KEY = "email";
    private final static String TIME_KEY = "time";
    private final static String AGE_KEY = "age";
    private final static String TOKEN_KEY = "token";

    private long id;
    private String login;
    private String email;
    private String token;
    private String name;
    private int age;
    private int time;
    private String password;

    public static User get() {
        if (user == null)
            user = loadUser();

        return user;
    }

    public static void save(UserResult result) {
        User user = get();
        user.id = result.id;
        user.login = result.login;
        user.email = result.email;
        user.token = result.token;
        user.time = result.time;
        user.age = result.age;

        Preferences prefs = prefs();
        prefs.putLong(ID_KEY, user.id);
        prefs.put(LOGIN_KEY, user.login);
        prefs.put(EMAIL_KEY, user.email);
        prefs.put(TOKEN_KEY, user.token);
        prefs.putInt(TIME_KEY, user.time);
        prefs.putInt(AGE_KEY, user.age);
        //    prefs.sync();
    }

    public static void logout() {
        user = null;
        try {
            prefs().clear();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private static Preferences prefs() {
        return Preferences.userRoot().node(User.class.getSimpleName());
    }

    private static User loadUser() {
        Preferences prefs = prefs();

        String token = prefs.get(TOKEN_KEY, "");
        if (TextUtils.isEmpty(token)) {
            return new User();
        } else {
            User user = new User();
            user.id = prefs.getLong(ID_KEY, -1L);
            user.login = prefs.get(LOGIN_KEY, "");
            user.email = prefs.get(EMAIL_KEY, "");
            user.token = prefs.get(TOKEN_KEY, "");
            user.age = prefs.getInt(AGE_KEY, 0);
            user.time = prefs.getInt(TIME_KEY, 0);
            return user;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getTime() {
        return this.time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}