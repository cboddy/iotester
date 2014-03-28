package im.boddy.iotester.unit_tests;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class AllTests
{
    public static void main(String[] args)
    {
        testClass(HistogramTests.class);
        testClass(NetworkIOTests.class);
    }

    private static void testClass(Class c)
    {
        Result result = JUnitCore.runClasses(c);
        for (Failure failure : result.getFailures()) 
            System.out.println(failure.toString());
        
    }
}
