package protein;
import common.*;
import java.util.*;
import java.io.FileNotFoundException;

public class evalution {
	
	public static double CalculateAUC(ArrayList<Pair<Double,Double>> pointList)
	{
		double AUC = 0;
		Collections.sort(pointList);
		double x1,y1,x2,y2;
		for (int i = 0;i < pointList.size() - 1; i++)
		{
			x1 = pointList.get(i).getFirst();
			x2 = pointList.get(i + 1).getFirst();
			y1 = pointList.get(i).getSecond();
			y2 = pointList.get(i + 1).getSecond();
			AUC += (x2-x1) * (y1+y2)/2;
		}
		return AUC;
	}
	
	public static double CalculateROCAUC(ArrayList<Pair<Integer, Double>> predPair)
	{
		ArrayList<Pair<Double,Double>> pointList = new ArrayList<Pair<Double,Double>>();
		double answer = 0;
		ArrayList<Pair<Integer, Double>> predPair1 = (ArrayList<Pair<Integer, Double>>) predPair.clone();
		Comparator<Pair<Integer,Double>> compar = new Comparator<Pair<Integer,Double>>()
		{
			@Override
			public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
				// TODO Auto-generated method stub
				if (o1.getSecond()>o2.getSecond()) return -1; else 
				if (o1.getSecond()<o2.getSecond()) return 1; else return 0;
			}
		};
		
		Collections.sort(predPair1, compar);
		int N = 0;
		int P = 0;
		int FP = 0;
		int TP = 0;
		double FPR = 0;
		double TPR = 0;
		for (Pair<Integer, Double> e:predPair1)
		{
			if (e.getFirst() == 1) P += 1; else N += 1;
		}
		if ((P == 0) || (N == 0)) return 1.0;
		pointList.add(new Pair<Double,Double>(FPR,TPR));
		for (int i = 0;i<predPair1.size();i++)
		{
			int ans = predPair1.get(i).getFirst();
			if (ans == 1) TP += 1; else FP += 1;
			FPR = (double)FP/N;
			TPR = (double)TP/P;
			pointList.add(new Pair<Double,Double>(FPR,TPR));
		}
		answer = evalution.CalculateAUC(pointList);
		return answer;
	}
	
	public static void CalTopKrecall(ArrayList<HashSet<Integer>> answer,
			ArrayList<ArrayList<Pair<Integer,Double>>> predictor,int k
			)
	{
		double TP = 0;
		double resum = 0;
		double presum = 0;
		for (int index = 0; index<answer.size(); index++)
		{
			TP = 0;
			HashSet<Integer> an = answer.get(index);
			ArrayList<Pair<Integer,Double>> pre = predictor.get(index);
			ArrayList<Pair<Double,Integer>> linshi = new ArrayList<Pair<Double,Integer>>();
			for (int i = 0;i<pre.size();i++) linshi.add(new Pair<Double,Integer>(pre.get(i).getSecond()
					,pre.get(i).getFirst()));
			Collections.sort(linshi);
			for (int i = linshi.size() - 1; i>=Math.max(0, linshi.size()-k);i--)
			{
				if (an.contains(linshi.get(i).getSecond()))
					TP += 1;
			}
			resum += TP/an.size();
			presum += TP/k;
		}
		presum = presum / answer.size();
		resum = resum / answer.size();
		System.out.printf("top %d recall = %.4f  precsion = %.4f   f = %.4f \n",k,resum,presum,2*resum*presum/(resum + presum));
	}
	public static Pair<Double,Double> GetFMeasureMax(ArrayList<HashSet<Integer>> answer,
			ArrayList<ArrayList<Pair<Integer,Double>>> predictor) 
	{
		ArrayList<Pair<Double,Double>> PreReCallPair = new ArrayList<Pair<Double,Double>>();
		return GetFMeasureMax(answer, predictor, PreReCallPair);
	}
	public static Pair<Double,Double> GetFMeasureMax(ArrayList<HashSet<Integer>> answer,
			ArrayList<ArrayList<Pair<Integer,Double>>> predictor,
			ArrayList<Pair<Double,Double>> PreReCallPair) 
	{
		
		ArrayList<labelResult> Result = new ArrayList<labelResult>();
		PreReCallPair.clear();
		
		for (int i = 0; i< answer.size(); i++)
		{
			while (answer.get(i).size() == 0)
			{
				answer.remove(i);
				predictor.remove(i);
			}
		}
		for (int i = 0; i< answer.size(); i++)
		{
            for (Pair<Integer,Double> node : predictor.get(i))
            {
            	int first = node.getFirst();
            	if (answer.get(i).contains(first))
            		Result.add(new labelResult(i,true,node.getSecond()));
            	else
            		Result.add(new labelResult(i,false,node.getSecond()));
            } 
		}
		Collections.sort(Result);
		
		int cutIndex = 0;
		double threshhold = 0;
		double Fmax = 0;
		int [] predNum = new int[answer.size()];
		int [] TpNum = new int[answer.size()];
		double recall,presion;
	    int prenum,recallnum;
	    double presum,recallsum,Fmeasure;
	    
		for (int i=0;i<predNum.length;i++) 
		{
			predNum[i] = 0;
			TpNum[i] = 0;
		}
		
		while (cutIndex < Result.size())
		{
			int beginIndex = cutIndex;
			int endIndex = cutIndex + 1;
			while (endIndex < Result.size() && 
				(Result.get(beginIndex).getValue() - Result.get(endIndex).getValue()) < Parameter.resolution)
				endIndex++;
			cutIndex = endIndex;
			for (int i = beginIndex;i < endIndex; i++)
			{
				labelResult node = Result.get(i); 
				predNum[node.getIndex()]++;
				if (Result.get(i).getResult()) TpNum[node.getIndex()]++;
			}
	        prenum = 0; recallnum = predNum.length;
	        presum = 0; recallsum = 0;
	        for (int i=0;i<predNum.length;i++) 
	        {
	        	recall = (double)TpNum[i] / answer.get(i).size();
	        	presion = (double)TpNum[i] / predNum[i];
	        	recallsum += recall;
	        	if (predNum[i] > 0) 
	        	{
	        		prenum++;
	        		presum += presion;
	        	}
	        }
	        recallsum /= recallnum;
	        if (prenum > 0) presum /= prenum; else presum = 0;
	        if (presum + recallsum == 0) Fmeasure = 0; else Fmeasure = 2 * presum * recallsum / (presum+recallsum);
	        //System.out.println("Fmax = " + Fmeasure + "Cut" + Result.get(endIndex - 1).getValue());
	        
	        PreReCallPair.add(new Pair<Double,Double>(presum,recallsum));
	        if (Fmeasure > Fmax)
	        {
	            Fmax = Fmeasure;
	            threshhold = Result.get(endIndex - 1).getValue();
	        }
		}
		return new Pair<Double,Double>(Fmax,threshhold);
	}
	
	
	
	public static Pair<Double,Double> GetFMeasureMaxSimpleVersion(ArrayList<HashSet<Integer>> answer,
									ArrayList<ArrayList<Pair<Integer,Double>>> predictor)
	{
	    double pre,recall,fmax = 0;
	    double threshhold = 0;
	    int prenum,recallnum;
	    double presum,recallsum,fmeasure;
	    for (int o=1;o<=99;o++)
	    {
	        double cut = (double)o/100;
	        prenum = 0; recallnum = 0;
	        presum = 0; recallsum = 0;
	        
	        HashSet<Integer> golist = new HashSet<Integer>();
	        HashSet<Integer> predlist = new HashSet<Integer>();
	        ArrayList<Pair<Integer,Double>> predpair = new ArrayList<Pair<Integer,Double>>();
	        int jiaocha;
	        for (int i = 0,j = 0; i< answer.size(); i++,j++)
	        {
	            predpair = predictor.get(j);
	            golist = answer.get(i);
	            for(Pair<Integer,Double> iter:predpair)
	            {
	            	if (iter.getSecond() >= cut) predlist.add(iter.getFirst());
	            }
	            jiaocha = 0;
	            for (Integer iter:golist)
	            {
	            	if (predlist.contains(iter)) jiaocha++;
	            }
	            if (predlist.size()>0)
	            {
	                pre = (double)jiaocha/predlist.size();
	                prenum++;
	                presum += pre;
	            }
	            recallnum++;
	            recall = (double)jiaocha/golist.size();
	            recallsum += recall;
	        }
	        if (prenum > 0) presum /= prenum; else presum = 0;
	        recallsum /=recallnum;
	        if (presum + recallsum == 0) fmeasure = 0; else fmeasure = 2 * presum * recallsum / (presum+recallsum);
	        //System.out.println(cut + "\t" + fmeasure);
	        if (fmeasure > fmax)
	        {
	            fmax = fmeasure;
	            threshhold = cut;
	        }
	        predlist.clear();
	    }
	    
	    Pair<Double,Double> return_pair = new Pair<Double,Double>(fmax,threshhold);  
		return return_pair;
	}
	

}
