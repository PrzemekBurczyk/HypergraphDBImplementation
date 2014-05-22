package model;

import org.joda.time.DateTime;

/**
 * Created by Przemek on 2014-05-22.
 */
public class Post {

    private String content;
    private DateTime createTime;
    private String title;

    public Post() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(DateTime createTime) {
        this.createTime = createTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
