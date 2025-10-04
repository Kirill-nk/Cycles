package com.example.myapplication.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PersonDao {
    @Insert
    long insert(Person person);

    @Query("SELECT * FROM people ORDER BY id DESC")
    List<Person> getAll();

    @Query("SELECT * FROM people WHERE id = :id")
    Person getById(long id);

    @Update
    void update(Person person);

    @Query("DELETE FROM people WHERE id = :id")
    void deleteById(long id);
}

