package model;

public class Course {
    private final String title;
    private final int learners_count;

    public Course(String title, int learners_count) {
        this.title = title;
        this.learners_count = learners_count;
    }

    public String getTitle() {
        return title;
    }

    public int getLearnersCount() {
        return learners_count;
    }
}
