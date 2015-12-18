package jiff;

import org.junit.Test;
import org.junit.Assert;

import java.io.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.skyscreamer.jsonassert.JSONAssert;

public class SpeedCompareTest {

    private static final int numComparisons=500;
    
    private String read() throws Exception {
        String fname=System.getProperty("file");
        if(fname!=null) {
            FileReader reader=new FileReader(fname);
            int i;
            StringBuffer buf=new StringBuffer();
            while((i=reader.read())>=0)
                buf.append((char)i);
            reader.close();
            return buf.toString();
        }
        return null;
    }
    
    @Test
    public void testJsonAssert() throws Exception {
        String json=read();
        if(json!=null) {
            long l=System.currentTimeMillis();
            for(int i=0;i<numComparisons;i++)
                JSONAssert.assertEquals(json, json, false);
            System.out.println("JSONAssert "+numComparisons+" comparisons:"+(System.currentTimeMillis()-l)+" msecs");
        }
    }

    @Test
    public void testJiff() throws Exception {
        String json=read();
        if(json!=null) {
            ObjectMapper mapper=new ObjectMapper();
            JsonNode node=mapper.readTree(json);
            JsonDiff diff=new JsonDiff(JsonDiff.Option.ARRAY_ORDER_INSIGNIFICANT,JsonDiff.Option.RETURN_LEAVES_ONLY);
            long l=System.currentTimeMillis();
            for(int i=0;i<numComparisons;i++)
                diff.computeDiff(node,node);
            System.out.println("Jiff "+numComparisons+" comparisons (toJson called once):"+(System.currentTimeMillis()-l)+" msecs");
        }
    }

    @Test
    public void testJiffStr() throws Exception {
        String json=read();
        if(json!=null) {
            JsonDiff diff=new JsonDiff(JsonDiff.Option.ARRAY_ORDER_INSIGNIFICANT,JsonDiff.Option.RETURN_LEAVES_ONLY);
            long l=System.currentTimeMillis();
            for(int i=0;i<numComparisons;i++)
                diff.computeDiff(json,json);
            System.out.println("Jiff "+numComparisons+" comparisons:"+(System.currentTimeMillis()-l)+" msecs");
        }
    }
}
