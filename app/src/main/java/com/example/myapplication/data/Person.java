package com.example.myapplication.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "people")
public class Person {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;

    // Dates stored as dd.MM.yyyy strings for simplicity in Java-only project
    public String lastCycleStartDate; // дата последней менструации
    public String previousCycleStartDate; // предыдущая дата

    public Integer daysToOvulation; // дней до овуляции от начала
    public Integer age; // возраст

    public Person(@NonNull String name, String lastCycleStartDate, String previousCycleStartDate, Integer daysToOvulation, Integer age) {
        this.name = name;
        this.lastCycleStartDate = lastCycleStartDate;
        this.previousCycleStartDate = previousCycleStartDate;
        this.daysToOvulation = daysToOvulation;
        this.age = age;
    }
}

