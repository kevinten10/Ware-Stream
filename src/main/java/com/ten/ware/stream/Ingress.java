package com.ten.ware.stream;

import java.util.ArrayList;
import java.util.List;

public class Ingress {
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        list.stream().forEach(integer -> {
            System.out.println(integer);
        });
    }
}
