package Main;

import java.io.FileInputStream;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import liblinear.InvalidInputDataException;
import protein.GoSet;
import protein.proteinCommon;
import protein.proteinSet;

public class learning {
	
	public static GoSet aGoSet = new GoSet();
	public static String GoDirectory;
	public static String Access2NameDirectory;
	public static String TrainDirectory;
	public static String MeaDirectory;
	public static String blastResult;
	public static String Task;
	public static int ThreadNum;
	public static double liblinearC;
	public static String modelDir;

	public static String predResultFile;
	public static String resultOutPattern;
	public static String resultOutFile;
	public static liblinear.Parameter liblinearParam   = new liblinear.Parameter(liblinear.SolverType.L2R_LR, 1, 1000, 0.05);

	
	public static int FeatureSize = 8000+400+4;
	public static DecimalFormat doubleFormat;
	public static Properties config;
	
	public static void ReadConfig() throws IOException
	{
		FileInputStream in = new FileInputStream("../bin/config.in");
		config = new Properties();
		config.load(in);
		doubleFormat = new DecimalFormat( "0.00000"); 
		GoDirectory = config.getProperty("GoDirectory");
		Access2NameDirectory = config.getProperty("Access2NameDirectory");
		TrainDirectory = config.getProperty("TrainDirectory");
		MeaDirectory   = config.getProperty("MeaDirectory");
		blastResult = config.getProperty("blastResult");
		Task = config.getProperty("Task");
		ThreadNum = Integer.parseInt(config.getProperty("ThreadNum"));
		liblinearC = Double.parseDouble(config.getProperty("liblinearC"));
		modelDir = config.getProperty("modelDir");
		predResultFile = config.getProperty("predResultFile");
		resultOutPattern = config.getProperty("resultOutPattern");
		resultOutFile = config.getProperty("resultOutFile");
	}
	public static void main(String[] args) throws IOException, InvalidInputDataException 
	{
		
		learning.ReadConfig();
		
		
		learning.aGoSet.Load(GoDirectory);
		//����������Ϣ
		System.out.println("TrainSet = " + TrainDirectory);
		System.out.println("MeasureSet = " + MeaDirectory);
		proteinSet.LoadAccess2NameMap(Access2NameDirectory);
		//���뵰����access�����ֵ�ӳ���ϵ
		
		
		proteinSet measure = new proteinSet();   //�����ʲ��Լ�
		measure.AddAnnotation(MeaDirectory + "Ann");    //���뵰���ʱ�ע
		//measure.filterCAFA2Species();
		System.out.println("Measure size = " + measure.size());
		measure.removeGoNotIn(learning.aGoSet);				//�Ƴ��������GO�����еı�ע
		measure.addFather(learning.aGoSet);					//���ݹ��ܵĴ��ݹ�ϵ���븸�׽ڵ�
		measure.removeAnnotation(8150,3674,5575);			//�Ƴ�MFO��BPO��CCO�������ڵ�
		
		
		proteinSet train =   new proteinSet();
		train.AddAnnotation(TrainDirectory + "Ann");
		//train.filterCAFA2Species();
		System.out.println("Train size = " + train.size());
		train.removeGoNotIn(learning.aGoSet);
		train.addFather(learning.aGoSet);
		train.removeAnnotation(8150,3674,5575);
		
		
		
		
		
		
		if (Task.equals("NaiveSpecies"))
		{
			ArrayList<String> SpeciesList = new ArrayList<String>(Arrays.asList
					("_ARATH","_BACSU","_DANRE","_DICDI","_DROME","_ECOLI",
					 "_HUMAN","_MOUSE","_PSEAE","_RAT"  ,"_SCHPO","_XENLA","_YEAST"));
			measure.naiveSpeciesBaseline(train,SpeciesList); 
		}
		if (Task.equals("Naive"))
		{
			measure.naiveBaseline(train);
		}
		if (Task.equals("LiblinearTrain"))
		{
			liblinearParam   = new liblinear.Parameter(liblinear.SolverType.L2R_LR, liblinearC, 1000, 0.05);
			train.setPredList(train);
			train.OutputPredList("predList");
			
			train.loadFastaSequence(TrainDirectory + "Seq");
			train.tranSequence2TriSparseFeature();
			//train.outputLibTrainFile("TrainSparseFeature", 1);
			train.setLiblinearFeatureFromSparseFeature();
			train.libLinearTrain(modelDir,ThreadNum); 
		}
		if (Task.equals("LiblinearPred"))
		{
			measure.loadFastaSequence(MeaDirectory + "Seq");
			measure.tranSequence2TriSparseFeature();
			//measure.outputLibTrainFile("MeasureSparseFeature", 1);
			measure.setLiblinearFeatureFromSparseFeature();   
			
			measure.setPredList(train);
			measure.sortPredListBaseFrequency();
			measure.libLinearPredict(modelDir);
			
			//measure.analyPredScore(learning.aGoSet);
			
			measure.removeLowPred(2000);
			measure.OutputPredScore(predResultFile);
		}
		
		if (Task.equals("L2R"))
		{
			String L2RTrainblastResult = config.getProperty("L2RTrainblastResult");
			String L2RTrainDirectory = config.getProperty("L2RTrainDirectory");
			
			proteinSet L2RTrain = new proteinSet();
			L2RTrain.AddAnnotation(L2RTrainDirectory + "Ann");
			L2RTrain.removeGoNotIn(learning.aGoSet);
			L2RTrain.addFather(learning.aGoSet);
			L2RTrain.removeAnnotation(8150,3674,5575);
			
			System.out.println("Begin learning to rank");
			
			L2RTrain.addBlastResultBitScore(L2RTrainblastResult);
			
			L2RTrain.GOtchaBaseline(train);
			
			L2RTrain.OutputPredScore("../OutFile/BlastKnnResult");
			
			
			L2RTrain.addTopK_L2RCandidate(50, 'F');
			L2RTrain.addTopK_L2RCandidate(100, 'P');
			L2RTrain.addTopK_L2RCandidate(50, 'C');
			
			L2RTrain.loadFastaSequence(L2RTrainDirectory + "Seq");
			L2RTrain.tranSequence2TriSparseFeature();
			L2RTrain.setLiblinearFeatureFromSparseFeature();
			L2RTrain.setPredList(train);
			
			L2RTrain.sortPredListBaseFrequency();
			L2RTrain.clearPredResult();
			L2RTrain.libLinearPredict(modelDir);
			L2RTrain.OutputPredScore("../OutFile/LiblinearResult");
			
			L2RTrain.addTopK_L2RCandidate(50, 'F');
			L2RTrain.addTopK_L2RCandidate(100, 'P');
			L2RTrain.addTopK_L2RCandidate(50, 'C');
			
			L2RTrain.recordliblinearScore();
			L2RTrain.clearPredResult();
			
			System.out.println("Begin blastKnn again");
			
			L2RTrain.GOtchaBaseline(train);
			L2RTrain.recordblastKnnScore();
			L2RTrain.clearPredResult();
			
			System.out.println("Begin blast again");
			
			L2RTrain.addBlastResultBitScore(blastResult);
			L2RTrain.recordblastScore();
			L2RTrain.clearPredResult();
			
			System.out.println("Begin output ranklib file");
			
			L2RTrain.OutputRanklibFile("TrainMFORanklib.txt",'F');
			L2RTrain.OutputRanklibFile("TrainBPORanklib.txt",'P');
			L2RTrain.OutputRanklibFile("TrainCCORanklib.txt",'C');	
		}
		if (Task.equals("Blast"))
		{
			measure.addBlastResultBitScore(blastResult);
			measure.blastBaseline(train);
		}
		
		if (Task.equals("BlastKnn"))
		{
			System.out.println("Begin Blast Weight Knn Score");
			measure.addBlastResultBitScore(blastResult);	//����Blast��Ԥ����
			measure.GOtchaBaseline(train);	//���д�Ȩ�ص�KNNԤ�⡣
			measure.OutputPredScore(predResultFile);
		}
		
		if (Task.equals("knn"))
		{
			System.out.println("Begin Knn Score");
			measure.addSimiliar("../InFile/PubMed/Similiar");	//����Blast��Ԥ����
			measure.knn(train);	//���д�Ȩ�ص�KNNԤ�⡣
		}
		
		//measure.addBlastResultSimility("../InFile/blastResult/psiBlastResult.out");
		//measure.readMSAlignResult("../InFile/blastResult/psiBlastResult.outfmt4");
		//measure.outputMSA("error.out");
		
		
		System.out.println(Task);
		//measure.evalutionEveryProtein("everyProteinResult","MFO");
		//�ֱ�����ÿ�����׵�Ԥ��Ч��
		
		//measure.setPredList(train);      	//��ѵ�����л����ҪԤ��ı�ǩ������
											//���ĳ��labelû����ѵ�����г��֣��Ͳ���Ԥ��
											//�Զ�����label���ֵ�Ƶ�ʴӸߵ�������
		
		//measure.addNoScoreLabelRandom();	//���ڲ��Լ���û�д�ֵ�label�Զ�����һ��������ҵ�����ʹ�ֵ�ֵ
		
		measure.evalutionFmaxAndAUPR(resultOutFile,resultOutPattern,"all");
		
		
		//measure.evalutionSpecies(resultOutFile,"_HUMAN","_MOUSE","_ARATH","_ECOLI","_RAT","_PSEAE","_YEAST");
		//�ֱ������������ֵ�Ԥ��Ч��
		
		
		
		measure.setPredList(train);
		measure.evalEveryLabelAUC(Task + "EveryLabelResult");
		//����2�зֱ�����ÿ��label��Ԥ��Ч��
		//����label��ѵ�����е�Ƶ��һ�μ���
		
		//measure.CalTopkRecall(10,50,100);
		   
	}
}
