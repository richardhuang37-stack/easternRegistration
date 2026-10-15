package com.example;

/**
 * Main entry point for the Eastern Registration application.
 */
public class EasternRegistration {
    
    private EasternRegistration() {
        // Prevent instantiation
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Opens the application's main menu.
     *
     * @param args not used
     * @throws Exception if the menu cannot retrieve schedule data
     */
    public static void main(String[] args) throws Exception {
        MainMenu menu = new MainMenu();
        menu.open();
    }
}