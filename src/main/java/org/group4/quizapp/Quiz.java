package org.group4.quizapp;

public class Quiz {
    private Long id;
    private String name;
    private boolean public_field;
    private Long user_id;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getPublic() {
        return public_field;
    }

    public void setPublic(boolean public_field) {
        this.public_field = public_field;
    }

    public boolean isPublic() {
        return public_field;
    }

    public Long getUserId() {
        return user_id;
    }

    public void setUserId(Long user_id) {
        this.user_id = user_id;
    }
}
