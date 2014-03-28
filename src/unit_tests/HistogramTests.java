package im.boddy.iotester.unit_tests;
import im.boddy.iotester.Histogram;

import java.util.*;

import static org.junit.Assert.*;
import org.junit.*;

public class HistogramTests
{
    private static final Random random = new Random(666);
    private static final float DELTA_TOLERANCE = 1.e-6f;

    @Test public void paramTest()
    {
        int nBins = Math.max(random.nextInt(1000),10);
        float xMin = random.nextFloat();
        float xMax = random.nextFloat() * 10;
        float binWidth = (xMax - xMin) / nBins;
        
        Histogram histo  = new Histogram(nBins, xMin, xMax, null, null, null);
        
        assertTrue("binWidth must be > 0", histo.binWidth() > 0 );
        assertEquals("binWidth correctly computed", binWidth, histo.binWidth(), DELTA_TOLERANCE);
    }

    @Test public void addTest()
    {
        int nBins = Math.max(random.nextInt(1000),10);
        float xMin = random.nextFloat();
        float xMax = random.nextFloat() * 10;
        Histogram histo  = new Histogram(nBins, xMin, xMax, null, null, null);

        float val = random.nextFloat() * (xMax - xMin);
        float weight = random.nextFloat();

        histo.add(val, weight);

        assertEquals("bin weight is correctly calculated", histo.binYval(histo.getBin(val)), weight, DELTA_TOLERANCE);

        try
        {
            histo.add(val, -1.f);
            fail();
        } catch (IllegalArgumentException ile) {}
    }

    @Test public void averageTest()
    {
        int nBins = Math.max(random.nextInt(1000),10);
        float xMin = random.nextFloat();
        float xMax = random.nextFloat() * 10;
        Histogram histo  = new Histogram(nBins, xMin, xMax, null, null, null);
        float valSum = 0;
        int nVals = Math.max(100, random.nextInt(1000));
        for (int i=0; i < nVals ; i++)
        {

            float val = xMin + random.nextFloat() * (xMax - xMin);
            histo.add(val);
            valSum += histo.binXval(histo.getBin(val));
        }
        float average = valSum / nVals;
        assertEquals("Average must compute correctly", average, histo.getAverage(), DELTA_TOLERANCE);
    }
}
