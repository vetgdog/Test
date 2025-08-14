package org.example.dao;

public interface Test1 {
    void test1();
    default void test2() {
        System.out.println("test2");
    }
}
