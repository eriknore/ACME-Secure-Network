package com.bnss.securefiletransfer;

/**
 * Created by luceat on 3/3/15.
 */
public class User {
    private int id;
    private String name;
    private String pub_key;

    public User(int id, String name, String pub_key){
        this.id = id;
        this.name = name;
        this.pub_key = pub_key;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getPublicKey(){
        return pub_key;
    }

    @Override
    public String toString(){
        return "id: " + id + " , name: " + name + " , pub_key: " + pub_key;
    }
}
