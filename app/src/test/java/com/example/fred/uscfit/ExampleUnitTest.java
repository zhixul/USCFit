package com.example.fred.uscfit;

import com.example.db.DBController;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    public DBController db = new DBController();

    @Test
    public void testAddNewUser() {
        db.addNewUser("zhixul0927@gmail.com","12345678");
        //assertEquals(4, 2 + 2);
    }
}