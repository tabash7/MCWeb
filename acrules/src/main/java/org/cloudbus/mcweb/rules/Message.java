package org.cloudbus.mcweb.rules;

public class Message {
    
    public Message(String type) {
        this.type = type;
    }

    public Message() {
        this(null);
    }
    
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
