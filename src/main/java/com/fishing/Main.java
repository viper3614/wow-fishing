package com.fishing;

/**
 * @author viper
 * @create 2024-07-27-18:50
 */
public class Main {
    public static void main(String[] args) throws Exception {
        AutoFish fish = new AutoFish();
        try {
            fish.goFish();
        } finally {
            fish.close();
        }
    }
}
