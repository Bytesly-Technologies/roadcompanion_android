package net.bytesly.roadcompanion.data;

public class ParkingCodeEntity {

    int id;
    String code;

    public ParkingCodeEntity() {
    }

    public ParkingCodeEntity(int id, String code) {
        this.id = id;
        this.code = code;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


}
