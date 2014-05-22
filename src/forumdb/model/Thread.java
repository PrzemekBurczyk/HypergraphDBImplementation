package forumdb.model;

import org.joda.time.DateTime;

/**
 * Created by Przemek on 2014-05-22.
 */
public class Thread {

    private String title;
    private DateTime createTime;

    public Thread() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(DateTime createTime) {
        this.createTime = createTime;
    }
}
