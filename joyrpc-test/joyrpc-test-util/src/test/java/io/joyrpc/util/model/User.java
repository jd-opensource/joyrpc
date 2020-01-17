package io.joyrpc.util.model;

public class User {

    private String name;
    private boolean man;
    private int _age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMan() {
        return man;
    }

    public void setMan(boolean man) {
        this.man = man;
    }

    public int getAge() {
        return _age;
    }

    public void setAge(int age) {
        this._age = age;
    }

    public static User getInstance() {
        return new User();
    }
}
