package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;

/**
 * Created by sagar on 4/1/15.
 */
public class Ring implements Serializable{
    public String nextNode;
    public String nextNodeHash;
    public String previousNode;
    public String previousNodeHash;
    public String currentNode;
    public String currentNodeHash;
}
