package model;

public class Meta {
    private final int page;
    private final boolean has_next;

    public Meta(int page, boolean has_next) {
        this.page = page;
        this.has_next = has_next;
    }

    public int getPage() {
        return page;
    }

    public boolean hasNext() {
        return has_next;
    }
}
