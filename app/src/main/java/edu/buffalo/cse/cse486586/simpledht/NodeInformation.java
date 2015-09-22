package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;

/**
 * Created by sagar on 4/1/15.
 */
public class NodeInformation implements Serializable{
    public String predecessor="";
    public String successor="";
    public String predecessorAfterHashing="";
    public String successorAfterHashing="";
    public String currentNode="";
    public String currentNodeAfterHashing="";
    public void setData(String predecessor,String predecessorAfterHashing,String successor,String successorAfterHashing,String currentNode, String currentNodeAfterHashing){
        this.predecessor=predecessor;
        this.predecessorAfterHashing=predecessorAfterHashing;
        this.successor=successor;
        this.successorAfterHashing=successorAfterHashing;
        this.currentNode=currentNode;
        this.currentNodeAfterHashing=currentNodeAfterHashing;
    }
}
