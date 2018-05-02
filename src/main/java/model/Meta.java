package model;

public class Meta {
    private final boolean has_next;

    public Meta(boolean has_next) {
        this.has_next = has_next;
    }

    public boolean hasNext() {
        return has_next;
    }
}
