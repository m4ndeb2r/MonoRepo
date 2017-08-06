package nl.ou.abi;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SlowTests.class, AllTests.class})
public class MySlowTest {

    @Test
    public void testSlow() {
        System.out.println("Slow test.");
    }

}
