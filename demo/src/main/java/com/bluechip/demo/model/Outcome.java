package com.bluechip.demo.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Outcome {
    private String name; // e.g. (Over/Under)
    private int price; // e.g. (+150/-150)
    private Double point; // e.g. 57.5 as in O/U 57.5 points

}
