package com.example.visiblevoice;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        String path = "aa/bb/cc/dd/ee.txt";
        String[] splits = path.split("/");
        File f = new File(path);


        assertEquals("ee", splits[splits.length-1].split("\\.")[0]);
        assertEquals("ee", f.getName().split("\\.")[0]);
    }
}