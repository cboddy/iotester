package com.boddy.iotester;

public class Histogram
{
    final float[] vals;
    final float xMin, xMax, binWidth;
    final int nBins, underflowBin, overflowBin;
    private String title, xTitle, yTitle; 


    public Histogram(int nBins, float xMin, float xMax)
    {
        this.binWidth = (xMax - xMin) / nBins;
        if (binWidth < 0)
            throw new IllegalArgumentException();

        this.nBins = nBins;
        this.vals = new float[nBins+2];
        this.xMin = xMin; 
        this.xMax = xMax;
        this.underflowBin = vals.length-2;
        this.overflowBin = vals.length-1;
    }

    public synchronized void setTitles(String title, String xTitle, String yTitle)
    {
        this.title = title;
        this.xTitle = xTitle;
        this.yTitle = yTitle;
    }

    public synchronized float getAverage()
    {
        float totalWeight = 0, totalVal = 0;
        for (int i=0;i < nBins; i++)
        {
            totalWeight += binYval(i);
            totalVal += binXval(i) * binYval(i);
        }

        if (totalWeight == 0)
            return 0;

        return totalVal / totalWeight;
    }

    public synchronized float getVariance()
    {
        float ave = getAverage();
        float totalWeight = 0, variance = 0;

        for (int i=0;i < nBins; i++)
        {
            totalWeight += binYval(i);
            variance += binYval(i) * (binXval(i) - ave)*(binXval(i) - ave);
        }

        if (totalWeight == 0)
            return 0;

        variance /= totalWeight;
        return variance;
    }

    private int getBin(float val)
    {
        if (val < xMin)
            return underflowBin;
        if (val > xMax)
            return overflowBin;

        return (int) ((val - xMin) / binWidth);
    } 


    public synchronized void add(float val)
    {
        add(val,1.f);
    }

    public synchronized void add(float val, float weight)
    {
        vals[getBin(val)] += weight;
    }

    public float binXval(int iBin)
    {
        if (iBin < 0 || iBin >= nBins)
            throw new IllegalArgumentException();

        return xMin + binWidth/2 + iBin * binWidth;
    }
    public synchronized float binYval(int iBin)
    {
        if (iBin < 0)
            return vals[underflowBin];
        if (iBin >= nBins)
            return vals[overflowBin];

        return vals[iBin];
    }

    public synchronized String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("title : "+ title+"\nxTitle : "+ xTitle +"\nyTitle : "+ yTitle+"\n");
        sb.append("underflow : "+ vals[underflowBin] + "\noverflow :" +vals[overflowBin]+"\n\n");
        sb.append("average : "+ getAverage() +"\n");
        sb.append("variance : "+ getVariance() +"\n");
        for (int iBin=0;iBin < nBins;iBin++)
            sb.append(binXval(iBin) +" "+ binYval(iBin) +"\n");

        return sb.toString();
    }

}
