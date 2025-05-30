package org.example.model;

public class Patient {
    private int id;
    private String name;
    private String surname;
    private String email;
    private String cnp;
    private String phone;
    private byte[] xray;

    public Patient(int id, String name, String surname, String cnp, String email, String phone, byte[] xray) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.cnp = cnp;
        this.email = email;
        this.phone = phone;
        this.xray = xray;
    }

    public Patient(){

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCnp() {
        return cnp;
    }

    public void setCnp(String cnp) {
        this.cnp = cnp;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public byte[] getXray() {
        return xray;
    }

    public void setXray(byte[] xray) {
        this.xray = xray;
    }
}
