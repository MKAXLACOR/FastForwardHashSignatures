package org.kunstemi.fastforwardsignatures.ffh;

/*
 * The MIT License

Copyright (c) 2015 Axlacor Ltd, (Michael Kunstel)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

public class RecurrenceCalculatorTest
{
    @Test
    public void simpleTest()
    {
        RecurrenceCalculator calculator = new RecurrenceCalculator(1, 1, 1, 1, 1, 1, 100);
        
        this.calculateAndAssert(calculator, 0, 0, 0, 1, 2, 2);
        this.calculateAndAssert(calculator, 0, 0, 0, 2, 7, 7);
        this.calculateAndAssert(calculator, 0, 0, 0, 3, 18, 18);
        this.calculateAndAssert(calculator, 0, 0, 0, 4, 41, 41);
        this.calculateAndAssert(calculator, 0, 0, 0, 5, 88, 88);
        this.calculateAndAssert(calculator, 0, 0, 0, 6, 83, 83);
        this.calculateAndAssert(calculator, 0, 0, 0, 7, 74, 74);
        this.calculateAndAssert(calculator, 0, 0, 0, 8, 57, 57);
        
        this.calculateAndAssert(calculator, 41, 41, 4, 4, 57, 57);
    }
    
    @Test
    public void testABCDFG()
    {
        RecurrenceCalculator calculator = new RecurrenceCalculator(1, 2, 3, 4, 5, 6, 1000);

        this.calculateAndAssert(calculator, 0, 0, 0, 1, 4, 6);
        this.calculateAndAssert(calculator, 0, 0, 0, 2, 133, 192);
        this.calculateAndAssert(calculator, 0, 0, 0, 3, 127, 948);
        this.calculateAndAssert(calculator, 0, 0, 0, 4, 706, 34);
        this.calculateAndAssert(calculator, 0, 0, 0, 5, 150, 890);
        this.calculateAndAssert(calculator, 0, 0, 0, 6, 779, 876);
        this.calculateAndAssert(calculator, 0, 0, 0, 7, 673, 832);
        this.calculateAndAssert(calculator, 0, 0, 0, 8, 352, 718);
        
        this.calculateAndAssert(calculator, 706, 34, 4, 4, 352, 718);
    }
    
    @Test
    public void testPQ()
    {
        RecurrenceCalculator calculator = new RecurrenceCalculator(1, 1, 1, 1, 1, 1, 100);
        
        this.calculateAndAssert(calculator, 1, 1, 0, 1, 4, 4);
        this.calculateAndAssert(calculator, 1, 5, 0, 1, 8, 8);
    }
    
    @Test
    public void testConsistency()
    {
        RecurrenceCalculator calculator = new RecurrenceCalculator(1, 2, 3, 4, 5, 6, 10000);
        
        PQPair directPair = calculator.calculate(7, 8, 10000, 10000);
        
        PQPair relayPair = new PQPair();
        relayPair.setP(BigInteger.valueOf(7));
        relayPair.setQ(BigInteger.valueOf(8));
        for(int n = 10000;n < 20000;n += 100)
        {
            relayPair = calculator.calculate(relayPair.getP(), relayPair.getQ(), BigInteger.valueOf(n), BigInteger.valueOf(100));
        }
        
        Assert.assertTrue(directPair.getP().equals(relayPair.getP()));
        Assert.assertTrue(directPair.getQ().equals(relayPair.getQ()));
        
        PQPair singleStepPair = new PQPair();
        singleStepPair.setP(BigInteger.valueOf(7));
        singleStepPair.setQ(BigInteger.valueOf(8));
        for(int n = 10000;n < 20000;n++)
        {
            singleStepPair = calculator.calculate(singleStepPair.getP(), singleStepPair.getQ(), BigInteger.valueOf(n), BigInteger.ONE);
        }
        
        Assert.assertTrue(directPair.getP().equals(singleStepPair.getP()));
        Assert.assertTrue(directPair.getQ().equals(singleStepPair.getQ()));
    }
    
    @Test
    public void testRequirement()
    {
        RecurrenceCalculator calculator = new RecurrenceCalculator(911, 691, 2733, 2073, -1357, 2468, 8191);
        this.testRequirement(calculator, 3);
        this.testRequirement(calculator, 7);
        this.testRequirement(calculator, 120);
        this.testRequirement(calculator, 4096);
        this.testRequirement(calculator, 8192);
        this.testRequirement(calculator, 8193);
    }
    
    private void testRequirement(RecurrenceCalculator calculator, int targetN)
    {
        PQPair directPair = calculator.calculate(7, 8, 0, targetN);
        
        int firstStep = targetN/2;
        PQPair twoStepPair = calculator.calculate(7, 8, 0, firstStep);
        PQPair twoStepPair2 = calculator.calculate(twoStepPair.getP(), twoStepPair.getQ(), BigInteger.valueOf(firstStep), BigInteger.valueOf(targetN-firstStep));
        Assert.assertTrue(directPair.getP().equals(twoStepPair2.getP()));
        Assert.assertTrue(directPair.getQ().equals(twoStepPair2.getQ()));
        
        PQPair singleStepPair = new PQPair();
        singleStepPair.setP(BigInteger.valueOf(7));
        singleStepPair.setQ(BigInteger.valueOf(8));
        for(int n = 0;n < targetN;n++)
        {
            singleStepPair = calculator.calculate(singleStepPair.getP(), singleStepPair.getQ(), BigInteger.valueOf(n), BigInteger.ONE);
        }
        
        Assert.assertTrue(directPair.getP().equals(singleStepPair.getP()));
        Assert.assertTrue(directPair.getQ().equals(singleStepPair.getQ()));
    }
    
    @Test
    public void testPerformance()
    {
        BigInteger two = BigInteger.valueOf(2);
        BigInteger a = two.pow(9999);
        BigInteger b = two.pow(9999);
        BigInteger c = two.pow(9999);
        BigInteger d = two.pow(9999);
        BigInteger f = two.pow(9999);
        BigInteger g = two.pow(9999);
        BigInteger m = two.pow(99999);
        BigInteger p = two.pow(9999);
        BigInteger q = two.pow(9999);
        BigInteger n = two.pow(9999);
        BigInteger nPlus = two.pow(9999);
        
        RecurrenceCalculator calculator = new RecurrenceCalculator(a, b, c, d, f, g, m);
        calculator.calculate(p, q, n, nPlus);        
        long startTime = System.currentTimeMillis();
        for (int ii = 0; ii < 1000; ++ii) {
        	calculator.calculate(p, q, n, nPlus);
        }
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime) + "ms");
    }
           
    
    private void calculateAndAssert(RecurrenceCalculator calculator, int p, int q, int n, int nPlus, int expectedP, int expectedQ)
    {
        PQPair pair = calculator.calculate(p, q, n, nPlus);
        Assert.assertTrue(pair.getP().intValue() == expectedP);
        Assert.assertTrue(pair.getQ().intValue() == expectedQ);
    }
}
