package org.cloudbus.mcweb.rules;

public class Message {
    
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void printMessage() {
        System.out.println("Type: " + type);
    }
}
