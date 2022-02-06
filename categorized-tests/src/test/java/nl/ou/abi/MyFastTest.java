package nl.ou.abi;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({FastTests.class, AllTests.class})
public class MyFastTest {

    @Test
    public void testFast() {
        System.out.println("Fast test.");
    }

}
