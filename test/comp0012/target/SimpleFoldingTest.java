package comp0012.target;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import comp0012.target.SimpleFolding;

/**
 * test simple folding
 */
public class SimpleFoldingTest {

    SimpleFolding sf = new SimpleFolding();
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    
    @Before
    public void setUpStreams()
    {
        System.setOut(new PrintStream(outContent));
    }
    
    @After
    public void cleanUpStreams()
    {
        System.setOut(null);
    }

    @Test
    public void testSimple(){
        sf.simple();
        String s = outContent.toString();
//        for(char c : s.toCharArray()) {
//        	System.err.println("C: " + (int)c);
//        }
        assertEquals("12412" + System.lineSeparator(), s);
    }

}
