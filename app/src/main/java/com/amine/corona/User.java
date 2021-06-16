package com.amine.corona;

public class User {
    public String email, phone, password, status;
    public boolean isChecked, isVisible, isAnonyme;
    public int visible_days;

    public User() {

    }

    public User(String email, String phone, String password, boolean isChecked, int visible_days, boolean isVisible, boolean isAnonyme, String status) {
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.isChecked = isChecked;
        this.visible_days = visible_days;
        this.isVisible = isVisible;
        this.isAnonyme = isAnonyme;
        this.status = status;
    }
}
