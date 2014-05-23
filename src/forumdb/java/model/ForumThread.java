package model;

/**
 * Created by Przemek on 2014-05-22.
 */
public class ForumThread {

    private String title;

    public ForumThread() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ForumThread)) return false;

        ForumThread that = (ForumThread) o;

        if (!title.equals(that.title)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return title.hashCode();
    }

    @Override
    public String toString() {
        return "ForumThread{" +
                "title='" + title + '\'' +
                '}';
    }
}
