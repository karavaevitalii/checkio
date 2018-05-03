package model;

import java.util.List;

public class StepikResponsePage {
    private final Meta meta;
    private final List<Course> courses;

    public StepikResponsePage(Meta meta, List<Course> courses) {
        this.meta = meta;
        this.courses = courses;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public boolean hasNextPage() {
        return meta.has_next;
    }

    private class Meta {
        final boolean has_next;

        Meta(boolean has_next) {
            this.has_next = has_next;
        }
    }

}
