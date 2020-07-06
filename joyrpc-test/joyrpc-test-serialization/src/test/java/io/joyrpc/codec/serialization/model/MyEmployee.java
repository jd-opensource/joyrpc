package io.joyrpc.codec.serialization.model;

public class MyEmployee extends Employee {

    protected String name;

    public MyEmployee() {
    }

    public MyEmployee(long id, String name, int age, int height, double weight) {
        super(id, null, age, height, weight);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MyEmployee employee = (MyEmployee) o;

        if (id != employee.id) {
            return false;
        }
        if (age != employee.age) {
            return false;
        }
        if (height != employee.height) {
            return false;
        }
        if (Double.compare(employee.weight, weight) != 0) {
            return false;
        }
        return name != null ? name.equals(employee.name) : employee.name == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + age;
        result = 31 * result + height;
        temp = Double.doubleToLongBits(weight);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

}
