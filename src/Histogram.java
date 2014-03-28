package im.boddy.iotester;

public class Histogram
{
    /**
     * Thread safe
     */ 
    private final float[] vals;
    private final float xMin, xMax, binWidth;
    private final int nBins, underflowBin, overflowBin;
    private final String title, xTitle, yTitle; 

    public Histogram(int nBins, float xMin, float xMax, String title, String xTitle, String yTitle)
    {
        if (nBins <=0)
            throw new IllegalArgumentException("Number of bins must be greater than zero.");

        this.binWidth = (xMax - xMin) / nBins;
        if (binWidth <= 0)
            throw new IllegalArgumentException("xMin must be less than xMax");

        this.nBins = nBins;
        this.vals = new float[nBins+2];
        this.xMin = xMin; 
        this.xMax = xMax;
        this.underflowBin = vals.length-2;
        this.overflowBin = vals.length-1;

        this.title = title;
        this.xTitle = xTitle;
        this.yTitle = yTitle;
    }
    
    public int getBin(float val)
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
        if (weight <=0)
           throw new IllegalArgumentException("weight must be >=0 ."); 
        vals[getBin(val)] += weight;
    }

    public float binXval(int iBin)
    {
        if (iBin < 0 || iBin >= nBins)
            throw new IllegalArgumentException("require 0 <= bin < "+ nBins);

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

    public float xMin(){return xMin;}
    public float xMax(){return xMax;}
    public float binWidth(){return binWidth;}
    public String title(){return title;}
    public String xTitle(){return xTitle;}
    public String yTitle(){return yTitle;}

    public synchronized float getUnderflow(){return vals[underflowBin];}
    public synchronized float getOverflow(){return vals[overflowBin];}


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
    
    public synchronized float[] getOneSigmaInterval()
    {
        return getConfidenceLevelInterval(0.68269f);
    }

    public synchronized float[] getTwoSigmaInterval()
    {
        return getConfidenceLevelInterval(0.95450f);
    }

    public synchronized float[] getConfidenceLevelInterval(float coverage)
    {
        if (coverage < 1e-3 || 1. < coverage)
            throw new IllegalArgumentException("require 0 < coverage < 1"); 
        float[] CLInterval = new float[2];
        //first element is lower bound
        //second element is upper bound
        
        float totalWeight = 0;
        for (int i=0;i < nBins; i++)
            totalWeight += binYval(i);

        if (totalWeight < 1.e-9)
            return CLInterval;

        float targetWeight = (1.f - coverage) /2.f;

        float weight = 0;
        for (int i=0;i < nBins; i++)
            if ((weight += binYval(i)) >= targetWeight)
            {
                CLInterval[0] = binXval(i);
                break;
            }

        weight = 0; 
        for (int i=nBins-1;i >=0 ; i--)
            if ((weight += binYval(i)) >= targetWeight)
            {
                CLInterval[1] = binXval(i);
                break;
            }

        return CLInterval;
    }


    public synchronized String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("title : "+ title+"\nxTitle : "+ xTitle +"\nyTitle : "+ yTitle+"\n");
        sb.append("underflow : "+ getUnderflow() + "\noverflow :" + getOverflow() +"\n\n");
        sb.append("average : "+ getAverage() +"\n");
        sb.append("variance : "+ getVariance() +"\n");
        float[] twoSigmaCLInterval = getTwoSigmaInterval(); 
        sb.append("twoSigmaLowerBound : "+ twoSigmaCLInterval[0]+"\n");
        sb.append("twoSigmaUpperBound : "+ twoSigmaCLInterval[1]+"\n");
        for (int iBin=0;iBin < nBins;iBin++)
            sb.append(binXval(iBin) +" "+ binYval(iBin) +"\n");

        return sb.toString();
    }

    public synchronized String toJSONString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"title\" : \""+title+"\", ");
        sb.append("\"xTitle\" : \""+xTitle+"\", ");
        sb.append("\"yTitle\" : \""+yTitle+"\", ");
        sb.append("\"underflow\" : "+ getUnderflow() +", ");
        sb.append("\"overflow\" : "+ getOverflow() +", ");
        sb.append("\"average\" : "+getAverage()+", ");
        sb.append("\"variance\" : "+getVariance()+", ");
        float[] twoSigmaCLInterval = getTwoSigmaInterval(); 
        sb.append("\"twoSigmaLowerBound\" : "+ twoSigmaCLInterval[0]+", ");
        sb.append("\"twoSigmaUpperBound\" : "+ twoSigmaCLInterval[1]+", ");
        sb.append("\"binVals\" : [ \n");
        for (int iBin=0;iBin < nBins;iBin++)
        {
            sb.append("{\"xVal\" : "+ binXval(iBin) +", \"yVal\" : "+ binYval(iBin) +"}");
            if (iBin != nBins-1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("]}"); 
        return sb.toString();
    }

}
