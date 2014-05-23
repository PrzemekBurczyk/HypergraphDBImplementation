package model;

import org.hypergraphdb.HGPlainLink;
import org.joda.time.DateTime;

/**
 * Created by Przemek on 2014-05-22.
 */
public class User {

    private String login;
    private String city;
    private DateTime joinTime;
    private int postCount;
    private String rank;

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

    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;

        User user = (User) o;

        if (!login.equals(user.login)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return login.hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "login='" + login + '\'' +
                ", city='" + city + '\'' +
                ", joinTime=" + joinTime +
                ", postCount=" + postCount +
                ", rank='" + rank + '\'' +
                '}';
    }
}
