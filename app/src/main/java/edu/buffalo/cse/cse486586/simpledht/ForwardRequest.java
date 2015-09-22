package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sagar on 4/3/15.
 */
public class ForwardRequest implements Serializable{
    public String senderPort="";
    public Map<String, String> mapForStar = new HashMap<String, String>();
}
