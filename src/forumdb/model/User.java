package forumdb.model;

import org.joda.time.DateTime;

/**
 * Created by Przemek on 2014-05-22.
 */
public class User {

    private String login;
    private String city;
    private DateTime joinTime;

    public User() {
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public DateTime getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(DateTime joinTime) {
        this.joinTime = joinTime;
    }
}
