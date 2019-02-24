package com.integration.demos.model;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Benefitiary {


    @Id
    private String beneId;
    private String phoneNumber;
    private String name;

    public Benefitiary(){

    }

    public Benefitiary(String beneId, String phoneNumber, String name){
        this.beneId = beneId;
        this.phoneNumber = phoneNumber;
        this.name   = name;
    }

    public Benefitiary(String phoneNumber, String name){
        this.phoneNumber = phoneNumber;
        this.name   = name;
    }

    public String getBeneId(){ return this.beneId; }

    public String getPhoneNumber(){
        return this.phoneNumber;
    }

    public String getName(){
        return this.name;
    }

    public void setPhoneNumber(String phoneNumber){
         this.phoneNumber = phoneNumber;
    }

    public void setName(String name){
         this.name = name;
    }

    public void setBeneId(String beneId){ this.beneId = beneId;}
}
