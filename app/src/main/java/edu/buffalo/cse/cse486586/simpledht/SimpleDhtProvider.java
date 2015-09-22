package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {
    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    private final ContentValues newContentValues=new ContentValues();
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    static int numberOfNodes=0;
    static int countOfAVDs=0;
    //reference for treemap: http://www.tutorialspoint.com/java/java_treemap_class.htm
    TreeMap treeMap = new TreeMap();
    String[] hashedPortNumbers;
    String[] portNumbers;
    NodeInformation myPredecessorAndSuccessor=new NodeInformation();
    boolean fileReceived=false;
    String predecessor="";
    String predecessorAfterHashing="";
    String successor="";
    String successorAfterHashing="";
    String currentNode1="";
    String currentNodeAfterHashing1="";
    String portOfMe;
    String senderPortWhileQuery="";
    String keyOfFileWhileQuery="";
    String fileKey="";
    String fileValue="";
    boolean finalValueFound=false;
    ArrayList<String> keysInsertedSoFar = new ArrayList<String>();
    boolean fileIsPresent=false;
    boolean requestForStarSent=false;
    Map<String, String> mapForStar = new HashMap<String, String>();
    //private final ContentResolver mContentResolver=getContentResolver();
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        PackageManager m = getContext().getPackageManager();
        String s = getContext().getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("yourtag", "Error Package name not found ", e);
        }
        try {
            String path = s + "/files";
            File f = new File(path);
            File file[] = f.listFiles();
            for (int i = 0; i < file.length; i++) {
                getContext().deleteFile(file[i].getName());
                //File newFile = getContext().getFileStreamPath(file[i].getName());
                //FileInputStream fis = getContext().openFileInput(file[i].getName());
            }
        }catch (Exception e){
            Log.e(TAG,"Exception");
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        String nameOfFile=values.get("key").toString();
        String contentOfFile=values.get("value").toString();
        try {
            if(!myPredecessorAndSuccessor.currentNode.equals(myPredecessorAndSuccessor.successor)) {
                if (genHash(nameOfFile).compareTo(myPredecessorAndSuccessor.currentNodeAfterHashing) > 0) {
                    new callSuccessor().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, nameOfFile, contentOfFile);
                } else {
                    new callPredecessor().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, nameOfFile, contentOfFile);
                }
            }
            else{
                FileOutputStream fos = getContext().openFileOutput(nameOfFile, Context.MODE_PRIVATE);
                fos.write(contentOfFile.getBytes());
                keysInsertedSoFar.add(nameOfFile);
                fos.close();
                Log.v("insert", values.toString());
            }
        }
        catch (FileNotFoundException e){
            Log.v("errorInInsert","Found File Not Found exception");
        }
        catch (IOException e){
            Log.v("errorInInsert","Found IOexeption");
        }catch (NoSuchAlgorithmException e){
            Log.e(TAG,"NoSuchAlgorithmException");
        }
        //Log.v("insert", values.toString());
        return null;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub

        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        portOfMe=String.valueOf(Integer.parseInt(portStr));
        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }catch(IOException e){
            Log.v("error","error in creating server socket");
        }
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, myPort, myPort);
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // TODO Auto-generated method stub
        try{
            String[] columnNames = {"key", "value"};
            MatrixCursor matrixCursor=new MatrixCursor(columnNames);
            if(selection.equals("\"*\"")){
                // reference: http://stackoverflow.com/questions/5527764/get-application-directory
                PackageManager m = getContext().getPackageManager();
                String s = getContext().getPackageName();
                try {
                    PackageInfo p = m.getPackageInfo(s, 0);
                    s = p.applicationInfo.dataDir;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w("yourtag", "Error Package name not found ", e);
                }
                String path = s+"/files";
                File f = new File(path);
                File file[] = f.listFiles();
                if(file!=null) {
                    for (int i = 0; i < file.length; i++) {
                        int n;
                        File newFile = getContext().getFileStreamPath(file[i].getName());
                        FileInputStream fis = getContext().openFileInput(file[i].getName());
                        StringBuffer fileContent = new StringBuffer("");
                        String stringFileContent;
                        // Reference : http://stackoverflow.com/questions/9095610/android-fileinputstream-read-txt-file-to-string
                        byte[] buffer = new byte[1024];
                        if (newFile.exists()) {
                            while ((n = fis.read(buffer)) != -1) {
                                fileContent.append(new String(buffer, 0, n));
                            }
                        }
                        stringFileContent = fileContent.toString();
                        mapForStar.put(file[i].getName(), stringFileContent);
                        //matrixCursor.addRow(new String[]{file[i].getName(), stringFileContent });
                    }
                }
                //code to get values from other AVDs
                Log.d("Sending request:"+myPredecessorAndSuccessor.currentNode,String.valueOf(requestForStarSent));
                requestForStarSent=true;
                new callSuccessorForStar().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, myPredecessorAndSuccessor.currentNode, myPredecessorAndSuccessor.currentNode);
                do{
                    try {
                        Thread.sleep(200);
                        //Log.d("Sleeping",String.valueOf(requestForStarSent));
                        //Log.d("Sleeping","Sleeping");
                    }catch (InterruptedException l){
                        Log.e(TAG,"InterruptedException");
                    }
                }while (requestForStarSent==true);
                Log.d("Checking size of map:",String.valueOf(mapForStar.size()));
                //logic to copy hashmap values to cursor
                //reference: http://stackoverflow.com/questions/1066589/iterate-through-a-hashmap
                Iterator it = mapForStar.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    Log.d("Key:"+pair.getKey().toString()," Value:"+pair.getValue().toString());
                    matrixCursor.addRow(new String[]{pair.getKey().toString(),pair.getValue().toString()});
                    it.remove(); // avoids a ConcurrentModificationException
                }
                Log.d("Size of cursor:",String.valueOf(matrixCursor.getCount()));
                return matrixCursor;
            }
            else if(selection.equals("\"@\"")){
                PackageManager m = getContext().getPackageManager();
                String s = getContext().getPackageName();
                try {
                    PackageInfo p = m.getPackageInfo(s, 0);
                    s = p.applicationInfo.dataDir;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w("yourtag", "Error Package name not found ", e);
                }
                String path = s+"/files";
                File f = new File(path);
                File file[] = f.listFiles();
                if(file!=null) {
                    for (int i = 0; i < file.length; i++) {
                        int n;
                        File newFile = getContext().getFileStreamPath(file[i].getName());
                        FileInputStream fis = getContext().openFileInput(file[i].getName());
                        StringBuffer fileContent = new StringBuffer("");
                        String stringFileContent;
                        // Reference : http://stackoverflow.com/questions/9095610/android-fileinputstream-read-txt-file-to-string
                        byte[] buffer = new byte[1024];
                        if (newFile.exists()) {
                            while ((n = fis.read(buffer)) != -1) {
                                fileContent.append(new String(buffer, 0, n));
                            }
                        }
                        stringFileContent = fileContent.toString();
                        matrixCursor.addRow(new String[]{file[i].getName(), stringFileContent});
                    }
                }
                return matrixCursor;
            }
            else{
                int n;
                for(String s:keysInsertedSoFar){
                    if(s.equals(selection)){
                        fileIsPresent=true;
                    }
                }
                Log.d("FileIsPresent:",String.valueOf(fileIsPresent));
                if(fileIsPresent==true) {
                    fileIsPresent=false;//setting variable to its default value
                    File file = getContext().getFileStreamPath(selection);
                    FileInputStream fis = getContext().openFileInput(selection);
                    StringBuffer fileContent = new StringBuffer("");
                    String stringFileContent;
                    // Reference : http://stackoverflow.com/questions/9095610/android-fileinputstream-read-txt-file-to-string
                    byte[] buffer = new byte[1024];
                    if (file.exists()) {
                        while ((n = fis.read(buffer)) != -1) {
                            fileContent.append(new String(buffer, 0, n));
                        }
                    }
                    stringFileContent = fileContent.toString();
                    matrixCursor.addRow(new String[]{selection, stringFileContent});
                    Log.d("enteredHere","enteredHere");
                }
                else{
                    Log.d("entered","entered");
                    fileKey=selection;
                    new callSuccessorForQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, myPredecessorAndSuccessor.currentNode, selection);
                    do{
                        try {
                            Thread.sleep(200);
                            //Log.d("Sleeping","Sleeping");
                        }catch (InterruptedException f){
                            Log.e(TAG,"InterruptedException");
                        }
                    }while (finalValueFound==false);
                    matrixCursor.addRow(new String[]{fileKey, fileValue});
                    finalValueFound=false;
                }
                Log.d("NowHere","NowHere");
                return matrixCursor;
            }
        }catch (FileNotFoundException e){
            /*
            Log.d("File does not exist",selection);
            Log.d(String.valueOf(finalValueFound),String.valueOf(finalValueFound));
            new callSuccessorForQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, myPredecessorAndSuccessor.currentNode, selection);
            do{
                try {
                    Thread.sleep(200);
                    //Log.d("Sleeping","Sleeping");
                }catch (InterruptedException f){
                    Log.e(TAG,"InterruptedException");
                }
            }while (finalValueFound==0);
            Log.d("fileKey",fileKey);
            Log.d("fileValue",fileValue);
            matrixCursor.addRow(new String[]{fileKey, fileValue});
            finalValueFound=0;
            return matrixCursor;
            */
            Log.e(TAG,"FileNotFoundException");
        }catch (IOException e){
            Log.e(TAG,"IO Exception occured");
        }
        Log.v("query", selection);
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    //This function is SHA-1 hash function. It takes astring and generates SHA-1 hash as hexadecimal string.
    // These hexadecimal strings are probably to be used as keys
    // We can use standard lexicographical string comparison to determine which one is greater.
    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String senderPort = msgs[0];
                Socket socket;
                OutputStream outputstream;
                ObjectOutputStream objectoutputstream;
                SenderPort sp = new SenderPort();
                sp.setData(senderPort);
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(REMOTE_PORT0));
                outputstream=socket.getOutputStream();
                objectoutputstream=new ObjectOutputStream(outputstream);
                objectoutputstream.writeObject(sp);
            }catch (UnknownHostException e){
                Log.e(TAG, "UnknownHostException");
            }catch (IOException e){
                myPredecessorAndSuccessor.currentNode=portOfMe;
                myPredecessorAndSuccessor.predecessor=portOfMe;
                myPredecessorAndSuccessor.successor=portOfMe;
                Log.e(TAG, "IOException");
            }
            return null;
        }
    }
    private class ClientTask1 extends AsyncTask<TreeMap, Void, Void> {
        @Override
        protected Void doInBackground(TreeMap... msgs) {
            try {
                TreeMap treeMap1 = msgs[0];
                hashedPortNumbers=new String[numberOfNodes];
                portNumbers=new String[numberOfNodes];
                Set set = treeMap.entrySet();
                Iterator i = set.iterator();
                int k=0;
                while(i.hasNext()) {
                    Map.Entry me = (Map.Entry)i.next();
                    hashedPortNumbers[k]=me.getKey().toString();
                    portNumbers[k]=me.getValue().toString();
                    k++;
                }
                Socket socket;
                OutputStream outputstream;
                ObjectOutputStream objectoutputstream;
                NodeInformation nodeInformation;
                if(numberOfNodes==1){
                    nodeInformation = new NodeInformation();
                    nodeInformation.predecessor = portNumbers[0];
                    nodeInformation.predecessorAfterHashing = hashedPortNumbers[0];
                    nodeInformation.successor = portNumbers[0];
                    nodeInformation.successorAfterHashing = hashedPortNumbers[0];
                    nodeInformation.currentNode = portNumbers[0];
                    nodeInformation.currentNodeAfterHashing = hashedPortNumbers[0];
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(portNumbers[0]) * 2);
                    outputstream = socket.getOutputStream();
                    objectoutputstream = new ObjectOutputStream(outputstream);
                    objectoutputstream.writeObject(nodeInformation);
                }
                else if(numberOfNodes>1){
                    for (int j = 0; j < numberOfNodes; j++) {
                        nodeInformation = new NodeInformation();
                        if (j == 0) {
                            nodeInformation.predecessor = portNumbers[numberOfNodes - 1];
                            nodeInformation.predecessorAfterHashing = hashedPortNumbers[numberOfNodes - 1];
                            nodeInformation.successor = portNumbers[j + 1];
                            nodeInformation.successorAfterHashing = hashedPortNumbers[j + 1];
                        } else if (j == numberOfNodes - 1) {
                            nodeInformation.predecessor = portNumbers[j - 1];
                            nodeInformation.predecessorAfterHashing = hashedPortNumbers[j - 1];
                            nodeInformation.successor = portNumbers[0];
                            nodeInformation.successorAfterHashing = hashedPortNumbers[0];
                        } else {
                            nodeInformation.predecessor = portNumbers[j - 1];
                            nodeInformation.predecessorAfterHashing = hashedPortNumbers[j - 1];
                            nodeInformation.successor = portNumbers[j + 1];
                            nodeInformation.successorAfterHashing = hashedPortNumbers[j + 1];
                        }
                        nodeInformation.currentNode = portNumbers[j];
                        nodeInformation.currentNodeAfterHashing = hashedPortNumbers[j];
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(portNumbers[j]) * 2);
                        outputstream = socket.getOutputStream();
                        objectoutputstream = new ObjectOutputStream(outputstream);
                        objectoutputstream.writeObject(nodeInformation);
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "UnknownHostException");
            }catch (IOException e){
                Log.e(TAG,"IOException");
            }
            return null;
        }
    }
    private class callSuccessor extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String nameOfFile=msgs[0];
                String contentOfFile=msgs[1];
                Socket socket;
                OutputStream outputstream;
                ObjectOutputStream objectoutputstream;
                ForwardData forwardData=new ForwardData();
                forwardData.nameOfFile=nameOfFile;
                forwardData.contentOfFile=contentOfFile;
                forwardData.senderAVDPort=currentNode1;
                forwardData.senderAVDPortAfterHashing=currentNodeAfterHashing1;
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(myPredecessorAndSuccessor.successor)*2);
                outputstream=socket.getOutputStream();
                objectoutputstream=new ObjectOutputStream(outputstream);
                objectoutputstream.writeObject(forwardData);
            }catch (Exception e){}
            return null;
        }
    }
    private class callSuccessorForQuery extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String senderPort=msgs[0];
                String keyOfFile=msgs[1];
                Socket socket;
                OutputStream outputstream;
                ObjectOutputStream objectoutputstream;
                ForwardKey forwardKey=new ForwardKey();
                forwardKey.senderPort=senderPort;
                forwardKey.keyOfFile=keyOfFile;
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(myPredecessorAndSuccessor.successor)*2);
                outputstream=socket.getOutputStream();
                objectoutputstream=new ObjectOutputStream(outputstream);
                //Log.d("Sending key to port:",String.valueOf(Integer.parseInt(myPredecessorAndSuccessor.successor))+forwardKey.keyOfFile);
                objectoutputstream.writeObject(forwardKey);
            }catch (Exception e){}
            return null;
        }
    }
    private class callSuccessorForStar extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Log.d("RequestReceived",myPredecessorAndSuccessor.currentNode);
                String senderPort=msgs[0];
                Socket socket;
                OutputStream outputstream;
                ObjectOutputStream objectoutputstream;
                ForwardRequest forwardRequest=new ForwardRequest();
                forwardRequest.senderPort=senderPort;
                forwardRequest.mapForStar=mapForStar;
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(myPredecessorAndSuccessor.successor)*2);
                outputstream=socket.getOutputStream();
                objectoutputstream=new ObjectOutputStream(outputstream);
                objectoutputstream.writeObject(forwardRequest);
                Log.d("Request is forwarded to successor", myPredecessorAndSuccessor.successor);
            }catch (Exception e){}
            return null;
        }
    }
    private class callPredecessor extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String nameOfFile=msgs[0];
                String contentOfFile=msgs[1];
                Socket socket;
                OutputStream outputstream;
                ObjectOutputStream objectoutputstream;
                ForwardData forwardData=new ForwardData();
                forwardData.nameOfFile=nameOfFile;
                forwardData.contentOfFile=contentOfFile;
                forwardData.senderAVDPort=currentNode1;
                forwardData.senderAVDPortAfterHashing=currentNodeAfterHashing1;
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(myPredecessorAndSuccessor.predecessor)*2);
                outputstream=socket.getOutputStream();
                objectoutputstream=new ObjectOutputStream(outputstream);
                objectoutputstream.writeObject(forwardData);
            }catch (Exception e){}
            return null;
        }
    }
    private class callPredecessorForQuery extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String senderPort=msgs[0];
                String valueOfFile=msgs[1];
                Socket socket;
                OutputStream outputstream;
                ObjectOutputStream objectoutputstream;
                ReceiveKey receiveKey=new ReceiveKey();
                receiveKey.valueOfFile=valueOfFile;
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(senderPort)*2);
                outputstream=socket.getOutputStream();
                objectoutputstream=new ObjectOutputStream(outputstream);
                objectoutputstream.writeObject(receiveKey);
            }catch (Exception e){}
            return null;
        }
    }
    private class ServerTask extends AsyncTask<ServerSocket, String, Void>{
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            String[] arrayOfMessages=new String[10];
            int count=0;
            String message="",senderPort="";
            try {
                while(true) {
                    Socket socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    ObjectInputStream ois=new ObjectInputStream(socket.getInputStream());
                    Object receivedData=ois.readObject();
                    if(receivedData instanceof SenderPort){
                        SenderPort sp=(SenderPort)receivedData;
                        String portToBeUsed=String.valueOf(Integer.parseInt(sp.senderPort)/2);
                        //reference : http://www.tutorialspoint.com/java/java_treemap_class.htm
                        treeMap.put(genHash(portToBeUsed),portToBeUsed);
                        numberOfNodes++;
                        //logic to inform all AVDs about their predecessor and successor
                        new ClientTask1().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, treeMap, treeMap);
                    }
                    if(receivedData instanceof NodeInformation){
                        NodeInformation ni=(NodeInformation)receivedData;
                        myPredecessorAndSuccessor.predecessor=ni.predecessor;
                        predecessor=myPredecessorAndSuccessor.predecessor;
                        myPredecessorAndSuccessor.predecessorAfterHashing=ni.predecessorAfterHashing;
                        predecessorAfterHashing=myPredecessorAndSuccessor.predecessorAfterHashing;
                        myPredecessorAndSuccessor.successor=ni.successor;
                        successor=myPredecessorAndSuccessor.successor;
                        myPredecessorAndSuccessor.successorAfterHashing=ni.successorAfterHashing;
                        successorAfterHashing=myPredecessorAndSuccessor.successorAfterHashing;
                        myPredecessorAndSuccessor.currentNode=ni.currentNode;
                        currentNode1=myPredecessorAndSuccessor.currentNode;
                        myPredecessorAndSuccessor.currentNodeAfterHashing=ni.currentNodeAfterHashing;
                        currentNodeAfterHashing1=myPredecessorAndSuccessor.currentNodeAfterHashing;
                    }
                    if(receivedData instanceof ForwardData){
                        ForwardData fd=(ForwardData)receivedData;
                        if(fd.senderAVDPort.equals(myPredecessorAndSuccessor.predecessor) && fd.senderAVDPort.equals(myPredecessorAndSuccessor.successor)){
                            if (genHash(myPredecessorAndSuccessor.currentNode).compareTo(genHash(myPredecessorAndSuccessor.predecessor))>0){
                                if(genHash(fd.nameOfFile).compareTo(genHash(myPredecessorAndSuccessor.currentNode))<0 && genHash(fd.nameOfFile).compareTo(genHash(myPredecessorAndSuccessor.predecessor))>0){
                                    FileOutputStream fos = getContext().openFileOutput(fd.nameOfFile, Context.MODE_PRIVATE);
                                    fos.write(fd.contentOfFile.getBytes());
                                    keysInsertedSoFar.add(fd.nameOfFile);
                                    fos.close();
                                }
                                else{
                                    new callSuccessor().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fd.nameOfFile, fd.contentOfFile);
                                }
                            }
                            else{
                                if(genHash(fd.nameOfFile).compareTo(genHash(myPredecessorAndSuccessor.currentNode))<0 || genHash(fd.nameOfFile).compareTo(genHash(myPredecessorAndSuccessor.predecessor))>0){
                                    FileOutputStream fos = getContext().openFileOutput(fd.nameOfFile, Context.MODE_PRIVATE);
                                    fos.write(fd.contentOfFile.getBytes());
                                    keysInsertedSoFar.add(fd.nameOfFile);
                                    fos.close();
                                }
                                else{
                                    new callSuccessor().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fd.nameOfFile, fd.contentOfFile);
                                }
                            }
                        }
                        else {
                            if (fd.senderAVDPort.equals(myPredecessorAndSuccessor.predecessor)) {
                                if (genHash(fd.senderAVDPort).compareTo(genHash(myPredecessorAndSuccessor.currentNode)) > 0) {
                                    FileOutputStream fos = getContext().openFileOutput(fd.nameOfFile, Context.MODE_PRIVATE);
                                    fos.write(fd.contentOfFile.getBytes());
                                    keysInsertedSoFar.add(fd.nameOfFile);
                                    fos.close();
                                    Log.v("insert", "key=" + fd.nameOfFile + " And value=" + fd.contentOfFile);
                                } else {
                                    if (genHash(myPredecessorAndSuccessor.currentNode).compareTo(genHash(fd.nameOfFile)) > 0) {
                                        FileOutputStream fos = getContext().openFileOutput(fd.nameOfFile, Context.MODE_PRIVATE);
                                        fos.write(fd.contentOfFile.getBytes());
                                        keysInsertedSoFar.add(fd.nameOfFile);
                                        fos.close();
                                        Log.v("insert", "key=" + fd.nameOfFile + " And value=" + fd.contentOfFile);
                                    } else {
                                        new callSuccessor().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fd.nameOfFile, fd.contentOfFile);
                                    }
                                }
                            } else {
                                if (genHash(myPredecessorAndSuccessor.currentNode).compareTo(genHash(fd.senderAVDPort)) > 0) {
                                    new callSuccessor().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fd.nameOfFile, fd.contentOfFile);
                                } else {
                                    if (genHash(myPredecessorAndSuccessor.currentNode).compareTo(genHash(fd.nameOfFile)) > 0) {
                                        new callPredecessor().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fd.nameOfFile, fd.contentOfFile);
                                    } else {
                                        new callSuccessor().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fd.nameOfFile, fd.contentOfFile);
                                    }
                                }
                            }
                        }
                    }
                    if(receivedData instanceof ForwardKey){
                        ForwardKey fk=(ForwardKey)receivedData;
                        senderPortWhileQuery=fk.senderPort;
                        keyOfFileWhileQuery=fk.keyOfFile;
                        //logic to check whether AVD has file with given key
                        //Log.d("here",senderPortWhileQuery+keyOfFileWhileQuery);
                        for(String s:keysInsertedSoFar){
                            if(s.equals(keyOfFileWhileQuery)){
                                fileIsPresent=true;
                            }
                        }
                        if(fileIsPresent==true) {
                            fileIsPresent=false;
                            int n;
                            File file = getContext().getFileStreamPath(fk.keyOfFile);
                            FileInputStream fis = getContext().openFileInput(fk.keyOfFile);
                            StringBuffer fileContent = new StringBuffer("");
                            String stringFileContent;
                            // Reference : http://stackoverflow.com/questions/9095610/android-fileinputstream-read-txt-file-to-string
                            byte[] buffer = new byte[1024];
                            if (file.exists()) {
                                while ((n = fis.read(buffer)) != -1) {
                                    fileContent.append(new String(buffer, 0, n));
                                }
                            }
                            stringFileContent = fileContent.toString();
                            //logic ends
                            new callPredecessorForQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fk.senderPort, stringFileContent);
                        }
                        else{
                            new callSuccessorForQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, senderPortWhileQuery, keyOfFileWhileQuery);
                        }
                    }
                    if(receivedData instanceof ReceiveKey){
                        ReceiveKey rk=(ReceiveKey)receivedData;
                        fileValue=rk.valueOfFile;
                        finalValueFound=true;
                    }
                    if(receivedData instanceof ForwardRequest){
                        ForwardRequest fr=(ForwardRequest)receivedData;
                        Log.d("Receiving request",String.valueOf(requestForStarSent));
                        if(requestForStarSent==false) {
                            PackageManager m = getContext().getPackageManager();
                            String s = getContext().getPackageName();
                            try {
                                PackageInfo p = m.getPackageInfo(s, 0);
                                s = p.applicationInfo.dataDir;
                            } catch (PackageManager.NameNotFoundException e) {
                                Log.w("yourtag", "Error Package name not found ", e);
                            }
                            String path = s + "/files";
                            File f = new File(path);
                            File file[] = f.listFiles();
                            Log.d("Here",myPredecessorAndSuccessor.currentNode);
                            if(file!=null) {
                                for (int i = 0; i < file.length; i++) {
                                    int n;
                                    File newFile = getContext().getFileStreamPath(file[i].getName());
                                    FileInputStream fis = getContext().openFileInput(file[i].getName());
                                    StringBuffer fileContent = new StringBuffer("");
                                    String stringFileContent;
                                    // Reference : http://stackoverflow.com/questions/9095610/android-fileinputstream-read-txt-file-to-string
                                    byte[] buffer = new byte[1024];
                                    if (newFile.exists()) {
                                        while ((n = fis.read(buffer)) != -1) {
                                            fileContent.append(new String(buffer, 0, n));
                                        }
                                    }
                                    stringFileContent = fileContent.toString();
                                    Log.d("Putting file to map,key:" + file[i].getName(), " Value:" + stringFileContent);
                                    mapForStar.put(file[i].getName(), stringFileContent);
                                }
                            }
                            Log.d("Size of map before append:",String.valueOf(mapForStar.size()));
                            mapForStar.putAll(fr.mapForStar);
                            Log.d("Size of map after append:", String.valueOf(mapForStar.size()));
                            new callSuccessorForStar().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, myPredecessorAndSuccessor.currentNode, myPredecessorAndSuccessor.currentNode);
                        }
                        else{
                            Log.d("RoundComplete:",myPredecessorAndSuccessor.currentNode);
                            mapForStar.putAll(fr.mapForStar);
                            Log.d("Setting status to false again:", myPredecessorAndSuccessor.currentNode);
                            requestForStarSent=false;
                        }
                    }
                }

            }catch (FileNotFoundException e){
                Log.e(TAG, "FileNotFoundException");
            }catch (IOException e){
                Log.e(TAG,"IOException");
            }catch (ClassNotFoundException e){
                Log.e(TAG, "ClassNotFoundException");
            }catch(NoSuchAlgorithmException e){
                Log.v("error:","NoSuchAlgorithmException ");
            }

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            return null;
        }
    }
}