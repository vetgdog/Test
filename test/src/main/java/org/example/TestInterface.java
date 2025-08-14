package org.example;

import org.example.dao.Test1;

public class TestInterface implements Test1 {
    @Override
    public void test1() {
        System.out.println("test1");
    }

    public static void main(String[] args) {
        TestInterface test = new TestInterface();
        test.test1();
        test.test2();
    }
}
