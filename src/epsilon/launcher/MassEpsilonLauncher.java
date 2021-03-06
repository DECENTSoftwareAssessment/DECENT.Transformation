package epsilon.launcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.emf.EmfUtil;
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.EolOperation;
import org.eclipse.epsilon.eol.EolOperations;
import org.eclipse.epsilon.eol.IEolExecutableModule;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.tools.MathTool;
import org.eclipse.epsilon.etl.EtlModule;
import org.eclipse.epsilon.profiling.Profiler;
import org.eclipse.epsilon.profiling.ProfilerTargetSummary;
import org.eclipse.epsilon.profiling.ProfilingExecutionListener;

import DECENT.DECENTPackage;
import MG.MGPackage;
import resource.tools.DECENTResourceTool;
import resource.tools.MGResourceTool;
import util.MathTools;
import epsilon.launcher.EpsilonStandaloneLauncher;

@SuppressWarnings("unused")
public class MassEpsilonLauncher {
	
	private Properties properties = new Properties();
	private DECENTEpsilonModelHandler modelHandler = new DECENTEpsilonModelHandler();
	protected boolean finished = false;
	private boolean singleRun = false;
	
	public static void main(String[] args) throws Exception {
		MassEpsilonLauncher launcher = new MassEpsilonLauncher();
		launcher.listen();
		launcher.loadProperties(args);
		launcher.registerMetaModels();
		launcher.executeSteps();
        launcher.finished = true;
        launcher.close();

	}
	
	//TODO: can be generalised for other applications as well 
	public void listen() {
		Runnable serverTask = new Runnable() {
			@Override
			public void run() {
				ServerSocket serverSocket;
				try {
					serverSocket = new ServerSocket(9090);
					try {
						while (!finished)
						{
							Socket clientSocket = serverSocket.accept();
							String input = IOUtils.toString(clientSocket
									.getInputStream());
							//TODO: differentiate between different inputs
							if (input.contains("=")) {
								String[] setting = input.split("=");
								System.setProperty(setting[0], setting[1]);
							}
						}
					} catch (IOException e) {
						System.err.println("Unable to process client request");
						e.printStackTrace();
					} finally {
						serverSocket.close();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		};
		Thread listenerThread = new Thread(serverTask);
		listenerThread.start();
	}
	
	public void close() {
		try {
			Socket s;
			s = new Socket("localhost", 9090);
			try {
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				out.print("close");
				out.flush();
			} finally {
				s.close();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("ERROR: "+e.getMessage());
		}
	}

	public IEolExecutableModule createModule() {
		return new EtlModule();
	}

	public void loadProperties(String[] arguments) throws Exception {
		if (arguments.length < 1) {
			System.out.println("No configuration provided! Usage: <CONFIGURATION> [<STEPS>]");
			System.exit(0);
		}
		System.out.println("INIT: Loading settings...");
		String propertiesFilename = arguments[0];
		properties.load(new FileInputStream(propertiesFilename));
		modelHandler.setUseDECENTBinary(Boolean.parseBoolean(properties.getProperty("useDECENTBinary","false")));
		modelHandler.setUseCFABinary(Boolean.parseBoolean(properties.getProperty("useCFABinary","false")));
		modelHandler.setUseARFFxBinary(Boolean.parseBoolean(properties.getProperty("useARFFxBinary","false")));
		modelHandler.setUseDECENTDB(Boolean.parseBoolean(properties.getProperty("useDECENTDB","false")));
		modelHandler.setUseNeoDECENT(Boolean.parseBoolean(properties.getProperty("useNeoDECENT","false")));
		modelHandler.setUseMGBinary(Boolean.parseBoolean(properties.getProperty("useMGBinary","false")));
		if (arguments.length > 1) {
			properties.setProperty("steps", arguments[1]);
		}
		if (arguments.length > 2) {
			//TODO: check supported types
			properties.setProperty("decent2arffx.type", arguments[2]);
		}
		if (arguments.length > 3) {
			//TODO: check supported types
			properties.setProperty("project", arguments[3]);
		}
		
		//TODO: extract and generalize steps as configuration files or models
		//with description, source, required models, required steps, accepted arguments, dependencies, etc.
		System.setProperty("epsilon.logLevel", properties.getProperty("logLevel", "1"));
		System.setProperty("epsilon.logToFile", properties.getProperty("logToFile", "false"));
		System.setProperty("epsilon.logFileAvailable", "false");
		System.setProperty("epsilon.transformation.extra2cfa.factor", properties.getProperty("extra2cfa.factor", "Fix"));
		System.setProperty("epsilon.transformation.trace2cfa.factor", properties.getProperty("trace2cfa.factor", "SZZ,IssueCount,UsersPerIssue,CommentsPerIssue"));
		System.setProperty("epsilon.transformation.shared2cfa.strategy", properties.getProperty("shared2cfa.strategy", "Shared"));
		System.setProperty("epsilon.transformation.cfastats.factor", properties.getProperty("cfastats.factor", "BugFix,BugFix.Shared,Fix,Fix.Shared"));
		System.setProperty("epsilon.transformation.cfastats.folds", properties.getProperty("cfastats.folds", "1,4,10"));
		System.setProperty("epsilon.transformation.cfastats.detailed", properties.getProperty("cfastats.detailed", "true"));
		System.setProperty("epsilon.transformation.temporals.resolution", properties.getProperty("temporals.resolution", "86400000"));
//		System.setProperty("epsilon.transformation.temporals.groups", properties.getProperty("temporals.groups", "Basic,Frequencies,Variances,Sets,CFA.Variances,CFA.Sets"));
		System.setProperty("epsilon.transformation.temporals.groups", properties.getProperty("temporals.groups", "Basic,Frequencies,Variances,CFA.Variances"));
//		System.setProperty("epsilon.transformation.cfa2decent.factor", properties.getProperty("cfa2decent.factor", "BugFix,BugFix.Shared,Fix,Fix.Shared"));
		System.setProperty("epsilon.transformation.cfa2decent.factor", properties.getProperty("cfa2decent.factor", "BugFix"));
		System.setProperty("epsilon.transformation.arffx.smallContributorActivityCount", properties.getProperty("arffx.smallContributorActivityCount", "100"));
		System.setProperty("epsilon.transformation.arffx.confidenceWindowFilter", properties.getProperty("arffx.confidenceWindowFilter", "true"));
		System.setProperty("epsilon.transformation.arffx.useFilter", properties.getProperty("arffx.useFilter", "false"));
		System.setProperty("epsilon.transformation.defaultBranch", properties.getProperty("defaultBranch", "master"));
		System.setProperty("epsilon.transformation.decent2arffx.skipSource", properties.getProperty("decent2arffx.skipSource", "false"));
		System.setProperty("epsilon.transformation.decent2arffx.types", properties.getProperty("decent2arffx.types", "code,Class,Method,Function,Module"));
		System.setProperty("epsilon.transformation.decent2arffx.skipArtifacts", properties.getProperty("decent2arffx.skipArtifacts", "true"));
		System.setProperty("epsilon.transformation.temporals.twostagecfa", properties.getProperty("temporals.twostagecfa", "true"));
		singleRun = Boolean.parseBoolean(properties.getProperty("temporals.twostagecfa.singlerun", "false"));
		
	}

	public void executeSteps() throws Exception {
		String dataLocation = properties.getProperty("dataLocation");
		String project = properties.getProperty("project");
		String location = dataLocation+project;
		
		String phases = properties.getProperty("phases", "");
		if (phases.length() < 1) {
			System.out.println("Executing steps: "+properties.getProperty("steps")+"...");
			for (String step : properties.getProperty("steps").split(",")) {
				System.out.println("  "+step);
				executeTransformation(step.replace("steps/", ""), location);
			}
		} else {
			System.out.println("Executing phases: "+phases+"...");
			for (String phase : phases.split(",")) {
				String p = properties.getProperty("P"+phase, "");
				if (p.length()<1) {
					System.out.println("ERROR: Unknown phase P"+phase+"; Skipping...");
				} else {
					for (String step : p.split(",")) {
						executeTransformation(step.replace("steps/", ""), location);
					}
				}
			}
		}
		
	}	
	public void executeTransformation(String step, String location) {
		try {
			switch (step) {
			case "MG2NORMALIZEDHUNKS":
				executeMG2NORMALIZEDHUNKS(location);
				break;
			case "MG2DECENT":
				executeMG2DECENT(location);
				break;
			case "MG2CFA":
				executeMG2CFA(location);
				break;
			case "TRACE2CFA":
				executeTRACE2CFA(location);
				break;
			case "EXTRA2CFA":
				executeEXTRA2CFA(location);
				break;
			case "SHARED2CFA":
				executeSHARED2CFA(location);
				break;
			case "DECENT2CFA":
				executeDECENT2CFA(location);
				break;
			case "CFA2DECENT":
				executeCFA2DECENT(location);
				break;
			case "CFA2DECENTSimple":
				executeCFA2DECENTSimple(location);
				break;
			case "CFATEMPORALS2DECENT":
				executeCFATEMPORALS2DECENT(location);
				break;
			case "CFATEMPORALS2DECENTS1":
				executeCFATEMPORALS2DECENTS1(location);
				break;
			case "CFATEMPORALS2DECENTS2":
				executeCFATEMPORALS2DECENTS2(location);
				break;
			case "DAG2DECENT":
				executeDAG2DECENT(location);
				break;
			case "DUDE2DECENT":
				executeDUDE2DECENT(location);
				break;
			case "EXPERIENCE2DECENT":
				executeEXPERIENCE2DECENT(location);
				break;
			case "TEMPORAL2DECENT":
				executeTEMPORAL2DECENT(location);
				break;
			case "UPDATEATTRIBUTES":
				executeUPDATEATTRIBUTES(location);
				break;
			case "CFASTATS":
				executeCFASTATS(location);
				break;
			case "DELTA2DECENT":
				executeDELTA2DECENT(location);
				break;
			case "COLLABORATION2DECENT":
				executeCOLLABORATION2DECENT(location);
				break;
			case "PATTERN2DECENT":
				executePATTERN2DECENT(location);
				break;
			case "BZ2TRACE":
				executeBZ2TRACE(location);
				break;
			case "TRACE2DECENT":
				executeTRACE2DECENT(location);
				break;
			case "FAMIX2DECENT":
				String lowerBound = properties.getProperty("famixLower");
				String upperBound = properties.getProperty("famixUpper");
				executeFAMIX2DECENT(location, lowerBound, upperBound); //reloads only famix model instances
				break;
			case "HITS2DECENT":
				executeHITS2DECENT(location);
				break;
			case "DECENT2ARFFx":
				executeDECENT2ARFFx(location);
				break;
			case "ARFFx2ARFF":
				executeARFFx2ARFF(location);
				break;
			case "SZZ":
				executeSZZ(location);
				break;
			case "binDECENT2ARFFx":
				modelHandler.setUseDECENTBinary(true);
				modelHandler.setUseARFFxBinary(true);
				executeDECENT2ARFFx(location);
				break;
			case "binARFFx2ARFF":
				modelHandler.setUseDECENTBinary(true);
				modelHandler.setUseARFFxBinary(true);
				executeARFFx2ARFF(location);
				break;
			case "QUERY":
				executeQUERY(location);
				break;
			case "STATS":
				executeSTATS(location);
				break;
			case "FIX":
				executeFIX(location);
				break;
			case "BACKUP":
				executeBACKUP(location);
				break;
			case "ROLLBACK":
				executeROLLBACK(location);
				break;
			case "DECENTLIVE":
				executeDECENTLIVE(location);
				break;
			case "CFALIVE":
				executeCFALIVE(location);
				break;
			case "ARFFxLIVE":
				executeARFFxLIVE(location);
				break;
			case "BIN2DECENT":
				//duplicates RT functionality
				executeBIN2DECENT(location);
				break;
			case "DECENT2BIN":
				//duplicates RT functionality
				executeDECENT2BIN(location);
				break;
			case "BIN2ARFFx":
				//duplicates RT functionality
				executeBIN2ARFFx(location);
				break;
			case "ARFFx2BIN":
				//duplicates RT functionality
				executeARFFx2BIN(location);
				break;
			case "DECENT2DB":
				//duplicates RT functionality
				executeDECENT2DB(location);
				break;
			default:
				System.out.println("ERROR: Unknown step "+step);
				break;
			}
			getProfilingSummary();
		} catch (EolModelLoadingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EolRuntimeException e) {
			// TODO Auto-generated catch block
			String path = e.getAst().getUri().getPath();
			System.out.println("  Epsilon Runtime Exception:" +
					"\n\tReason: "+e.getReason()+
					"\n\tWhere:  "+e.getAst().getUri()+" at "+e.getLine() + " : "+e.getColumn() +
					"\n\tLink:   ("+path.substring(path.lastIndexOf("/")+1)+":"+e.getLine()+")");
//			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			close();
		}
	}
	
	public void executeBIN2DECENT(String location) {
		if (new File(location+"/model.decent"+"bin").exists()) {
			modelHandler.convertDECENTModelToXMI(location);
		}
	}

	public void executeDECENT2BIN(String location) {
		if (new File(location+"/model.decent").exists()) {
			modelHandler.convertDECENTModelToBinary(location);
		}
	}

	public void executeBIN2ARFFx(String location) {
		if (new File(location+"/model.arffx"+"bin").exists()) {
			modelHandler.convertARFFxModelToXMI(location);
		}
	}

	public void executeARFFx2BIN(String location) {
		if (new File(location+"/model.arffx").exists()) {
			modelHandler.convertARFFxModelToBinary(location);
		}
	}

	
	public void executeDECENT2DB(String location) {
		if (new File(location+"/model.decent").exists()) {
			modelHandler.convertDECENTModelToDB(location);
		}
	}

	
	private void registerMetaModels() throws Exception {
		String metaModelsPath = "../DECENT.Meta/model/";
		File metaModelsLocation = new File(metaModelsPath);
		for (File file : metaModelsLocation.listFiles()) {
			if (file.getName().endsWith(".ecore")) {
				EmfUtil.register(URI.createFileURI(file.getAbsolutePath()), EPackage.Registry.INSTANCE);
			}
		}
	}


	private void executeTRACE2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/trace2decent3.eol";
		IEolExecutableModule module = loadModule(source);
		IModel decentModel = modelHandler.getDECENTModel(location, true, true);
		IModel traceModel = modelHandler.getTRACEModel(location, true, false);
		module.getContext().getModelRepository().addModel(traceModel);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		traceModel.dispose();
		//can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
	}

	
	private void executeBZ2TRACE(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/bz2trace.etl";
		IEolExecutableModule module = loadModule(source);
		IModel mgModel = modelHandler.getMGModel(location, true, false);
		IModel bzModel = modelHandler.getBZModel(location);
		IModel traceModel = modelHandler.getTRACEModel(location, false, true);
		module.getContext().getModelRepository().addModel(mgModel);
		module.getContext().getModelRepository().addModel(bzModel);
		module.getContext().getModelRepository().addModel(traceModel);
		execute(module, location);
		bzModel.dispose();
		mgModel.dispose();
		//can be stored and retained alternatively
		traceModel.dispose();
		module.reset();
	}

	private void executeSZZ(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/szz.eol";
		IEolExecutableModule module = loadModule(source);
		IModel mgModel = modelHandler.getMGModel(location, true, false);
		IModel bzModel = modelHandler.getBZModel(location);
		IModel traceModel = modelHandler.getTRACEModel(location, false, true);
		module.getContext().getModelRepository().addModel(mgModel);
		module.getContext().getModelRepository().addModel(bzModel);
		module.getContext().getModelRepository().addModel(traceModel);
		execute(module, location);
		bzModel.dispose();
		mgModel.dispose();
		//can be stored and retained alternatively
		traceModel.dispose();
		module.reset();
	}

	
	private void executeDAG2DECENT(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/dag2decent3.etl";
		IEolExecutableModule module = loadModule(source);
		IModel decentModel = modelHandler.getDECENTModel(location, true, true);
		IModel dagModel = modelHandler.getDAGModel(location);
		module.getContext().getModelRepository().addModel(dagModel);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		dagModel.dispose();
		// can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
	}

	
	private void executeDUDE2DECENT(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/dude2decent3.eol";
		IEolExecutableModule module = loadModule(source);
		IModel decentModel = modelHandler.getDECENTModel(location, true, true);
		IModel dudeModel = modelHandler.getDUDEModel(location);
		module.getContext().getModelRepository().addModel(dudeModel);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		dudeModel.dispose();
		// can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
	}
	
	private void executeCFA2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/cfa2decent3.etl";
		IEolExecutableModule module = loadModule(source);
		IModel cfaModel = modelHandler.getCFAModel(location, true, false);
		IModel decentModel = modelHandler.getDECENTModel(location, true, true);
		//TODO: consider removing reliance on MG especially if it is only needed in one line
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		cfaModel.dispose();
		//can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
	}

	private void executeCFA2DECENTSimple(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {

		boolean binary = modelHandler.isUseDECENTBinary();
		if (binary) {
			modelHandler.convertDECENTModelToXMI(location);
			modelHandler.setUseDECENTBinary(false);
		}
		String source = "epsilon/transform/cfa2decent3simple.etl";
		IEolExecutableModule module = loadModule(source);
		IModel cfaModel = modelHandler.getCFAModel(location, true, false);
		IModel decentModel = modelHandler.getDECENTModel(location, true, true);
		// TODO: consider removing reliance on MG especially if it is only
		// needed in one line
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		cfaModel.dispose();
		// can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
		if (binary) {
			modelHandler.convertDECENTModelToBinary(location);
			modelHandler.setUseDECENTBinary(binary);
		}
	}

	private void executeCFATEMPORALS2DECENTS2(String location) throws Exception, URISyntaxException,
		EolModelLoadingException, EolRuntimeException {
		//TODO: force Binary or other
		//TODO: make configurable?
		boolean binary = modelHandler.isUseDECENTBinary();
		if (!binary) {
			modelHandler.convertDECENTModelToBinary(location);
			modelHandler.setUseDECENTBinary(true);
		}
		String source = "epsilon/transform/cfa_temporals2decent3stage2.eol";
		IEolExecutableModule module = loadModule(source);
		IModel decentModel = modelHandler.getDECENTModel(location, true, true);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		decentModel.dispose();
		module.reset();
		if (!binary) {
			modelHandler.convertDECENTModelToXMI(location);
			modelHandler.setUseDECENTBinary(binary);
		}

	}

	
	private void executeCFATEMPORALS2DECENTS1(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		//TODO: spaghetti monster..
		//TODO: force XMI
		boolean binary = modelHandler.isUseDECENTBinary();
		//stage 1
		if (binary) {
			modelHandler.convertDECENTModelToXMI(location);
			modelHandler.setUseDECENTBinary(false);
		}
		String source = "epsilon/transform/cfa_temporals2decent3.etl";
		IEolExecutableModule module = loadModule(source);
		IModel cfaModel = modelHandler.getCFAModel(location, true, false);
		IModel decentModel = modelHandler.getDECENTModel(location, true, false);
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		cfaModel.dispose();
		if (singleRun) {
			decentModel.dispose();
		}
		module.reset();
//		if (binary) {
//			modelHandler.convertDECENTModelToBinary(location);
//		}
		modelHandler.setUseDECENTBinary(binary);
	}

	
	private void executeCFATEMPORALS2DECENT(String location) throws Exception, URISyntaxException,
		EolModelLoadingException, EolRuntimeException {
		boolean binary = modelHandler.isUseDECENTBinary();
		if (binary) {
			modelHandler.convertDECENTModelToXMI(location);
			modelHandler.setUseDECENTBinary(false);
		}
		String source = "epsilon/transform/cfa_temporals2decent3.etl";
		IEolExecutableModule module = loadModule(source);
		IModel cfaModel = modelHandler.getCFAModel(location, true, false);
		IModel decentModel = modelHandler.getDECENTModel(location, true, true);
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		cfaModel.dispose();
		//can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
		if (binary) {
			modelHandler.convertDECENTModelToBinary(location);
			modelHandler.setUseDECENTBinary(binary);
		}
	}

	private void executeDECENT2CFA(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		boolean binary = modelHandler.isUseDECENTBinary();
		if (binary) {
			modelHandler.convertDECENTModelToXMI(location);
			modelHandler.setUseDECENTBinary(false);
		}
		String source = "epsilon/transform/decent2cfa.etl";
		IEolExecutableModule module = loadModule(source);
		IModel cfaModel = modelHandler.getCFAModel(location, true, true);
		IModel decentModel = modelHandler.getDECENTModel(location, true, false);
		// TODO: consider removing reliance on MG especially if it is only
		// needed in one line
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		cfaModel.dispose();
		// can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
		if (binary) {
			modelHandler.convertDECENTModelToBinary(location);
			modelHandler.setUseDECENTBinary(binary);
		}
	}

	private void getProfilingSummary() {
		List<ProfilerTargetSummary> summaries = org.eclipse.epsilon.profiling.Profiler.INSTANCE.getTargetSummaries();
		if (summaries.size() == 0) {
			return;
		}
		System.out.println("Profiling summary:");
		int maxLength = 0;
		for (ProfilerTargetSummary s : summaries) {
			if (s.getName().length() > maxLength) {
				maxLength = s.getName().length();
			}
		}
		String layout = "%-"+maxLength+"s %16s %16s %16s %16s";
		String header = String.format(layout, "Name", "Execution Count", "Aggregate Time", "Individual Time", "Average Time");
		System.out.println("	"+header);
		for (ProfilerTargetSummary s : summaries) {
			String e = String.format(layout, s.getName(), s.getExecutionCount(), s.getExecutionTime().getAggregate(), s.getExecutionTime().getIndividual(), ((double)s.getExecutionTime().getAggregate())/s.getExecutionCount());
			System.out.println("	"+e);
		}
	}

	private void executeMG2CFA(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/mg2cfa.etl";
		IEolExecutableModule module = loadModule(source);
		IModel mgModel = modelHandler.getMGModel(location, true, false);
		IModel cfaModel = modelHandler.getCFAModel(location, false, true);
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(mgModel);
		execute(module, location);
		mgModel.dispose();
		//can be stored and retained alternatively
		cfaModel.dispose();
		module.reset();
	}

	private void execute(IEolExecutableModule module, String location)
			throws Exception {
		IModel logModel = modelHandler.getLOGModel(location, true, true);
		module.getContext().getModelRepository().addModel(logModel);
		module.execute();
		logModel.dispose();
	}

	private void executeCFASTATS(String location) throws Exception, URISyntaxException,
	EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/cfastats.eol";
		IEolExecutableModule module = loadModule(source);
		IModel cfaModel = modelHandler.getCFAModel(location, true, false);
		module.getContext().getModelRepository().addModel(cfaModel);
		execute(module, location);
		//can be stored and retained alternatively
//		cfaModel.dispose();
		module.reset();
	}

	
	private void executeTRACE2CFA(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/trace2cfa.eol";
		IEolExecutableModule module = loadModule(source);
		IModel cfaModel = modelHandler.getCFAModel(location, true, true);
		IModel traceModel = modelHandler.getTRACEModel(location, true, false);
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(traceModel);
		execute(module, location);
		traceModel.dispose();
		//can be stored and retained alternatively
		cfaModel.dispose();
		module.reset();
	}

	private void executeEXTRA2CFA(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/extra2cfa.eol";
		IEolExecutableModule module = loadModule(source);
		IModel cfaModel = modelHandler.getCFAModel(location, true, true);
		module.getContext().getModelRepository().addModel(cfaModel);
		execute(module, location);
		// can be stored and retained alternatively
		cfaModel.dispose();
		module.reset();
	}

	private void executeSHARED2CFA(String location) throws Exception,
		URISyntaxException, EolModelLoadingException, EolRuntimeException {
		boolean binary = modelHandler.isUseDECENTBinary();
		if (binary) {
			modelHandler.convertDECENTModelToXMI(location);
			modelHandler.setUseDECENTBinary(false);
		}
		String source = "epsilon/transform/shared2cfa.eol";
		IEolExecutableModule module = loadModule(source);
		IModel cfaModel = modelHandler.getCFAModel(location, true, true);
		IModel decentModel = modelHandler.getDECENTModel(location, true, false);
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		// can be stored and retained alternatively
		cfaModel.dispose();
		decentModel.dispose();
		module.reset();
		if (binary) {
			modelHandler.convertDECENTModelToBinary(location);
			modelHandler.setUseDECENTBinary(binary);
		}
	}

	
	private void executeEXPERIENCE2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/experience2decent3.eol";
		executeDECENTinPalace(location, source, true, true);
	}

	private void executeDECENTinPalace(String location, String source, boolean read, boolean write) throws Exception,
			URISyntaxException, EolRuntimeException {
		IEolExecutableModule module = loadModule(source);
		IModel decentModel = modelHandler.getDECENTModel(location, read, write);
		module.getContext().getModelRepository().addModel(decentModel);
		execute(module, location);
		decentModel.dispose();
		//can be stored and retained alternatively
		module.reset();
	}
	private void executeTEMPORAL2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/temporal2decent3.eol";
		executeDECENTinPalace(location, source, true, true);
	}
	private void executeUPDATEATTRIBUTES(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/updateAttributes.eol";
		executeDECENTinPalace(location, source, true, true);
	}
	private void executeDELTA2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/delta2decent3.eol";
		executeDECENTinPalace(location, source, true, true);
	}
	private void executePATTERN2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/pattern2decent3.eol";
		executeDECENTinPalace(location, source, true, true);
}
	private void executeCOLLABORATION2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/collaboration2decent3.eol";
		executeDECENTinPalace(location, source, true, true);
	}

	private void executeHITS2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/hits2decent3.eol";
		executeDECENTinPalace(location, source, true, true);
	}

	private void executeQUERY(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/query/decent.eol";
		executeDECENTinPalace(location, source, true, false);
	}

	private void executeFIX(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/fix.eol";
		executeDECENTinPalace(location, source, true, true);
	}

	private void executeBACKUP(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		File backup = new File(location+"/backup");
		backup.mkdirs();
		File decent = new File(location+"/model.decent");
		File cfa = new File(location+"/model.cfa");
		if (decent.exists()) {
			FileUtils.copyFileToDirectory(decent, backup);
		}
		if (cfa.exists()) {
			FileUtils.copyFileToDirectory(cfa, backup);
		}
	}

	private void executeROLLBACK(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		File backup = new File(location+"/backup");
		if (backup.exists()) {
			File project = new File(location);
			File decent = new File(backup.getAbsolutePath()+"/model.decent");
			File cfa = new File(backup.getAbsolutePath()+"/model.cfa");
			if (decent.exists()) {
				FileUtils.copyFileToDirectory(decent, project);
			}
			if (cfa.exists()) {
				FileUtils.copyFileToDirectory(cfa, project);
			}
			
		}
	}

	
	
	private void executeSTATS(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/query/stats.eol";
		executeDECENTinPalace(location, source, true, false);
	}

	private void executeARFFxLIVE(final String location)  {
		final String source = "epsilon/query/arffxlive.eol";
		try {
			final IModel model = modelHandler.getARFFxModel(location, true, false);
			model.load();

			executeLIVE(location, source, model);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void executeLIVE(final String location, final String source,
			final IModel model) {
		TimerTask task = new FileWatcher(new File(source)) {
			protected void onChange(File file) {
				try {
					IEolExecutableModule module = loadModule(source);
					module.getContext().getModelRepository().addModel(model);
					execute(module, location);
					module.reset();
				} catch (URISyntaxException e) {
				} catch (EolRuntimeException e) {
					String path = e.getAst().getUri().getPath();
					System.out.println("  Epsilon Runtime Exception:" +
							"\n\tReason: "+e.getReason()+
							"\n\tWhere:  "+e.getAst().getUri()+" at "+e.getLine() + " : "+e.getColumn() +
							"\n\tLink:   ("+path.substring(path.lastIndexOf("/")+1)+":"+e.getLine()+")");

				} catch (Exception e) {
					
				}
			}
		};

		Timer timer = new Timer();
		// repeat the check every second
		timer.schedule(task, new Date(), 1000);
	}

	
	private void executeDECENTLIVE(final String location)  {
		final String source = "epsilon/query/live.eol";
		try {
			final IModel model = modelHandler.getDECENTModel(location, true, false);
			model.load();
			executeLIVE(location, source, model);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void executeCFALIVE(final String location)  {
		final String source = "epsilon/query/cfalive.eol";

		try {
			final IModel cfaModel = modelHandler.getCFAModel(location, true, false);
			final IModel decentModel = modelHandler.getDECENTModel(location, true, false);
			final IModel mgModel = modelHandler.getMGModel(location, true, false);
			decentModel.load();
			mgModel.load();
			cfaModel.load();

			TimerTask task = new FileWatcher(new File(source)) {
				protected void onChange(File file) {
					try {
						final IEolExecutableModule module = loadModule(source);
						module.getContext().getModelRepository().addModel(mgModel);
						module.getContext().getModelRepository().addModel(decentModel);
						module.getContext().getModelRepository().addModel(cfaModel);

						execute(module, location);
						module.reset();

					} catch (URISyntaxException e) {
					} catch (EolRuntimeException e) {
						String path = e.getAst().getUri().getPath();
						System.out.println("  Epsilon Runtime Exception:" +
								"\n\tReason: "+e.getReason()+
								"\n\tWhere:  "+e.getAst().getUri()+" at "+e.getLine() + " : "+e.getColumn() +
								"\n\tLink:   ("+path.substring(path.lastIndexOf("/")+1)+":"+e.getLine()+")");

					} catch (Exception e) {
						
					}
				}
			};

			Timer timer = new Timer();
			// repeat the check every second
			timer.schedule(task, new Date(), 1000);

			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private void executeARFFx2ARFF(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/query/arffx2arff.eol";
		IEolExecutableModule module = loadModule(source);
		IModel arffxModel = modelHandler.getARFFxModel(location, true, false);
		module.getContext().getModelRepository().addModel(arffxModel);
		execute(module, location);
		arffxModel.dispose();
		// can be stored and retained alternatively
		module.reset();
	}
	
	private void executeDECENT2ARFFx(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/query/decent2arffx.eol";
		IEolExecutableModule module = loadModule(source);
		IModel decentModel = modelHandler.getDECENTModel(location, true, false);
		IModel arffxModel = modelHandler.getARFFxModel(location, false, true);;
		module.getContext().getModelRepository().addModel(decentModel);
		module.getContext().getModelRepository().addModel(arffxModel);
		execute(module, location);
		arffxModel.dispose();
//		decentModel.dispose();
		// can be stored and retained alternatively
		module.reset();
	}

	private void executeMG2NORMALIZEDHUNKS(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/mg2normalized_hunks.eol";
		IEolExecutableModule module = loadModule(source);
		IModel mgModel = modelHandler.getMGModel(location, true, true);
		module.getContext().getModelRepository().addModel(mgModel);
		execute(module, location);
		mgModel.dispose();
		//can be stored and retained alternatively
		module.reset();
	}

	
	private void executeMG2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/mg2decent3.etl";
		IEolExecutableModule module = loadModule(source);
		IModel decentModel = modelHandler.getDECENTModel(location, false, true);
		IModel mgModel = modelHandler.getMGModel(location, true, false);
		module.getContext().getModelRepository().addModel(decentModel);
		module.getContext().getModelRepository().addModel(mgModel);
		execute(module, location);
		mgModel.dispose();
		//can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
	}
	
	private void executeFAMIX2DECENT(String location, String lowerBound, String upperBound) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/famix2decent3.etl";
		IEolExecutableModule module = loadModule(source);
		
		//DONE: add option "use binary"
		//CAUTION: it will overwrite any existing binary model
		boolean forceFAMIX2DECENTBinary = Boolean.parseBoolean(properties.getProperty("forceFAMIX2DECENTBinary","true"));
		boolean binary = modelHandler.isUseDECENTBinary();
		if (forceFAMIX2DECENTBinary && !binary) {
			modelHandler.setUseDECENTBinary(true);
			modelHandler.convertDECENTModelToBinary(location);
		}
		
		IModel decentModel = modelHandler.getDECENTModel(location, true, true);
		//module.getContext().getModelRepository().addModel(decentModel);
		

		String[] commits = getSortedCommits(location);
		if (commits.length>0 && 0>=(Integer.parseInt(upperBound))) {
			upperBound=commits[commits.length-1];
		}
		String storageStrategy = properties.getProperty("storageStrategy", "safe");
		ArrayList<Double> factors = new ArrayList<>();
		int windowSize = 1;

		if (storageStrategy.equals("fixed-window")) {
			windowSize = Integer.parseInt(properties.getProperty("storageWindow", "1"));
		}
		
		long storageDuration = 0;
		long totalDuration = 0;
		int remainingCount = commits.length;
		for (String c : commits) {
			if (
					Integer.parseInt(c)>=(Integer.parseInt(lowerBound)) &&
					Integer.parseInt(c)<=(Integer.parseInt(upperBound))
			) {
				long start = System.currentTimeMillis();
				double eta = (remainingCount*totalDuration)/(60*1000);
				System.out.println("Processing: "+c+"/"+upperBound + "; ETA: "+eta);

				module.parse(modelHandler.getFile(source));
				IModel famixModel = modelHandler.getFAMIXModel(location,Integer.parseInt(c));
				famixModel.load();
				module.getContext().getModelRepository().addModel(decentModel);
				module.getContext().getModelRepository().addModel(famixModel);
				
				//some spaghetti below - collecting durations
				//implementing dynamic window strategy
				long executionStart = System.currentTimeMillis();
				//execute
				execute(module, location);
				long executionEnd = System.currentTimeMillis();
				long executionDuration=executionEnd-executionStart;
				
				//TODO: check why it takes about 60s between this 
				//and the next print statement even if the next step is actually skipped 
				
				int bufferSize = factors.size();
				int lastWindowSize = windowSize;
				if (factors.size()>=windowSize) {
					long storageStart = System.currentTimeMillis();
					//store
					decentModel.store();
					long storageEnd = System.currentTimeMillis();
					storageDuration = storageEnd-storageStart;
				
					//implementing fixed and dynamic window strategies
					int mean = (int) MathTools.getMean(factors);
					factors.clear();
					if (storageStrategy.equals("dynamic-window")) {
						windowSize = mean+1;
					}
				}

				long factor = storageDuration/executionDuration;
				factors.add((double) factor);

				//stats that may be useful
				System.out.println("Execution: "+executionDuration + "; "+
						"Storage: "+storageDuration  + "; "+
						"Factor: "+factor  + "; "+
						"Window: "+bufferSize+"/"+lastWindowSize
						);
					
				famixModel.dispose();
				module.reset();
				
				long end = System.currentTimeMillis();
				totalDuration = end-start;
			}
			remainingCount--;
		}
		decentModel.dispose();
		
		if (forceFAMIX2DECENTBinary && !binary) {
			modelHandler.setUseDECENTBinary(false);
			modelHandler.convertDECENTModelToXMI(location);
		}

	}

	private IEolExecutableModule loadModule(String source) throws Exception,
			URISyntaxException {
		IEolExecutableModule module = null;
		if (source.endsWith("etl")) {
			module = new EtlModule();	
		} else if (source.endsWith("eol")) {
			module = new EolModule();
		} else {
			
		}
		
		module.parse(modelHandler.getFile(source));

		if (module.getParseProblems().size() > 0) {
			System.err.println("Parse errors occured...");
			for (ParseProblem problem : module.getParseProblems()) {
				System.err.println(problem.toString());
			}
			//System.exit(-1);
		}

		return module;
	}
	
	private String[] getSortedCommits(String location) {
		File ws = new File(location+"/famix");
		if (!ws.exists()) {
			return new String[]{};
		}
		String[] commits = ws.list();
		Arrays.sort(commits, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				if (o1.equals(o2)) {
					return 0;
				} else if (Integer.parseInt(o1)>Integer.parseInt(o2)){
					return 1;
				} else {
					return -1;
				}
			}
		});
		return commits;
	}
	
	private void sampleModel(InMemoryEmfModel emfModel) throws Exception,
			EolRuntimeException {
		String allTypes = "operation allTypes() : Collection {"
				+ "var agents = DECENT!Agent.all();"
				+ "var artifacts = DECENT!Artifact.all();"
				+ "('Agents count: '+agents.size()).println();"
				+ "('Agents URI: '+agents.get(0).eResource()).println();"
				+ "return agents;"
				+ "}";

		IEolExecutableModule module = new EolModule();
		module.parse(allTypes);
		module.getContext().getModelRepository().addModel(emfModel);
		EolOperations declaredOperations = module.getDeclaredOperations();
		EolOperation operation = declaredOperations.getOperation("allTypes");
		ArrayList<Object> parameters = new ArrayList<Object>();
		Object result = operation.execute(null, parameters, module.getContext());
		System.out.println(result);
	}
}
