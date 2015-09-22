package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;

/**
 * Created by sagar on 4/1/15.
 */
public class SenderPort implements Serializable{
    public String senderPort;
    public void setData(String senderPort){
        this.senderPort=senderPort;
    }
}
