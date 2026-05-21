package roomescape.domain;

public class Store {
    private final Long id;
    private final String name;
    private final Long ownerId;

    public Store(Long id, String name, Long ownerId) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getMemberId() {
        return ownerId;
    }
}
