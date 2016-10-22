package com.goldenpie.devs.videowatchface.model;

import java.util.ArrayList;

import lombok.Data;

@Data
public class DataModel {

    private ArrayList<BytesPart> listOfBytes = new ArrayList<>();
    private int fullPart;
    private String author;
    private String description;

}
