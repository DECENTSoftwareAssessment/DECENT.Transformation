package epsilon.launcher;
/*******************************************************************************
 * Copyright (c) 2008 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.emf.common.util.URI;
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
import org.eclipse.epsilon.etl.EtlModule;

import DECENT.DECENTPackage;
import MG.MGPackage;
import resource.tools.DECENTResourceTool;
import resource.tools.MGResourceTool;
import epsilon.launcher.EpsilonStandaloneLauncher;

/**
 * This example demonstrates using the 
 * Epsilon Transformation Language, the M2M language
 * of Epsilon, in a stand-alone manner 
 * @author Dimitrios Kolovos
 */
@SuppressWarnings("unused")
public class MassEpsilonLauncher {
	
	private Properties properties = new Properties();
	private DECENTEpsilonModelHandler modelHandler = new DECENTEpsilonModelHandler();
	
	public static void main(String[] args) throws Exception {
		MassEpsilonLauncher launcher = new MassEpsilonLauncher();
		launcher.loadProperties(args);
		launcher.registerMetaModels();
		launcher.executeSteps();
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
		modelHandler.setUseDECENTBinary(Boolean.parseBoolean(properties.getProperty("useDECENTBinary")));
		modelHandler.setUseMGBinary(Boolean.parseBoolean(properties.getProperty("useMGBinary")));
		if (arguments.length == 2) {
			properties.setProperty("steps", arguments[1]);
		}

	}

	public void executeSteps() throws Exception {
		String dataLocation = properties.getProperty("dataLocation");
		String project = properties.getProperty("project");
		String location = dataLocation+project;
		for (String step : properties.getProperty("steps").split(",")) {
			executeTransformation(step.replace("steps/", ""), location);
		}
	}	
	public void executeTransformation(String step, String location) {
		//TODO: extract and generalize steps as configuration files or models
		//with description, source, required models, required steps, accepted arguments, dependencies, etc.
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
			case "CFA2DECENT":
				executeCFA2DECENT(location);
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
			case "DELTA2DECENT":
				executeDELTA2DECENT(location);
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
			case "QUERY":
				executeQUERY(location);
				break;
			case "STATS":
				executeSTATS(location);
				break;
			case "LIVE":
				executeLIVE(location);
				break;
			case "BIN2DECENT":
				//duplicates RT functionality
				executeBIN2DECENT(location);
				break;
			default:
				System.out.println("ERROR: Unknown step "+step);
				break;
			}
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
			//e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void executeBIN2DECENT(String location) {
		if (new File(location+"/model.decent"+"bin").exists()) {
			modelHandler.convertDECENTModelToXMI(location);
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
//		traceModel.load();
//		decentModel.load();
		module.getContext().getModelRepository().addModel(traceModel);
		module.getContext().getModelRepository().addModel(decentModel);
		module.execute();
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
//		mgModel.load();
//		bzModel.load();
		module.getContext().getModelRepository().addModel(mgModel);
		module.getContext().getModelRepository().addModel(bzModel);
		module.getContext().getModelRepository().addModel(traceModel);
		module.execute();
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
		// mgModel.load();
		// cfaModel.load();
		// decentModel.load();
		module.getContext().getModelRepository().addModel(dagModel);
		module.getContext().getModelRepository().addModel(decentModel);
		module.execute();
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
		// mgModel.load();
		// cfaModel.load();
		// decentModel.load();
		module.getContext().getModelRepository().addModel(dudeModel);
		module.getContext().getModelRepository().addModel(decentModel);
		module.execute();
		dudeModel.dispose();
		// can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
	}
	
	private void executeCFA2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/cfa2decent3.etl";
		IEolExecutableModule module = loadModule(source);
		IModel decentModel = modelHandler.getDECENTModel(location, true, true);
		//TODO: consider removing reliance on MG especially if it is only needed in one line
		IModel cfaModel = modelHandler.getCFAModel(location, true, false);
//		mgModel.load();
//		cfaModel.load();
//		decentModel.load();
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(decentModel);
		module.execute();
		cfaModel.dispose();
		//can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
	}

	private void executeMG2CFA(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/mg2cfa.etl";
		IEolExecutableModule module = loadModule(source);
		IModel mgModel = modelHandler.getMGModel(location, true, false);
		IModel cfaModel = modelHandler.getCFAModel(location, false, true);
//		mgModel.load();
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(mgModel);
		module.execute();
		mgModel.dispose();
		//can be stored and retained alternatively
		cfaModel.dispose();
		module.reset();
	}

	private void executeTRACE2CFA(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/trace2cfa.eol";
		IEolExecutableModule module = loadModule(source);
		IModel cfaModel = modelHandler.getCFAModel(location, true, true);
		IModel traceModel = modelHandler.getTRACEModel(location, true, false);
		//mgModel.load();
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(traceModel);
		module.execute();
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
		// mgModel.load();
		module.getContext().getModelRepository().addModel(cfaModel);
		module.execute();
		// can be stored and retained alternatively
		cfaModel.dispose();
		module.reset();
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
//		decentModel.load();
		module.getContext().getModelRepository().addModel(decentModel);
		module.execute();
		decentModel.dispose();
		//can be stored and retained alternatively
		module.reset();
	}
	private void executeTEMPORAL2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/temporal2decent3.eol";
		executeDECENTinPalace(location, source, true, true);
	}
	private void executeDELTA2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/delta2decent3.eol";
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

	private void executeSTATS(String location) throws Exception,
			URISyntaxException, EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/query/stats.eol";
		executeDECENTinPalace(location, source, true, false);
	}

	private void executeLIVE(String location)  {
		final String source = "epsilon/query/live.eol";
		try {
			final IModel decentModel = modelHandler.getDECENTModel(location, true, false);
			decentModel.load();

			TimerTask task = new FileWatcher(new File(source)) {
				protected void onChange(File file) {
					try {
						IEolExecutableModule module = loadModule(source);
						module.getContext().getModelRepository().addModel(decentModel);
						module.execute();
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
		module.execute();
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
		// decentModel.load();
		module.getContext().getModelRepository().addModel(decentModel);
		module.getContext().getModelRepository().addModel(arffxModel);
		module.execute();
		decentModel.dispose();
		arffxModel.dispose();
		// can be stored and retained alternatively
		module.reset();
	}

	private void executeMG2NORMALIZEDHUNKS(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/mg2normalized_hunks.eol";
		IEolExecutableModule module = loadModule(source);
		IModel mgModel = modelHandler.getMGModel(location, true, true);
//		mgModel.load();
		module.getContext().getModelRepository().addModel(mgModel);
		module.execute();
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
//		mgModel.load();
		module.getContext().getModelRepository().addModel(decentModel);
		module.getContext().getModelRepository().addModel(mgModel);
		module.execute();
		mgModel.dispose();
		//can be stored and retained alternatively
		decentModel.dispose();
		module.reset();
	}
	
	private void executeFAMIX2DECENT(String location, String lowerBound, String upperBound) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/famix2decent3.etl";
		IEolExecutableModule module = loadModule(source);
		
		//TODO: add option "use binary"
		IModel decentModel = modelHandler.getDECENTModel(location, true, true);
		//module.getContext().getModelRepository().addModel(decentModel);
		//decentModel.load();
		

		String[] commits = getSortedCommits(location);
		if (0<=(Integer.parseInt(upperBound))) {
			upperBound=commits[commits.length-1];
		}
		//TODO: add option "semi-safe" which stores intermediate decent model but does not reload it (optionally with a frequency - every 1/2/5/10 revisions)
		//TODO: add option "safe" which stores each intermediate decent model (and/or reloads it)
		for (String c : commits) {
			if (
					Integer.parseInt(c)>=(Integer.parseInt(lowerBound)) &&
					Integer.parseInt(c)<=(Integer.parseInt(upperBound))
			) {
				System.out.println("Processing: "+c);
				module.parse(modelHandler.getFile(source));
				IModel famixModel = modelHandler.getFAMIXModel(location,Integer.parseInt(c));
				famixModel.load();
				module.getContext().getModelRepository().addModel(decentModel);
				module.getContext().getModelRepository().addModel(famixModel);
				module.execute();
				famixModel.dispose();
				decentModel.store();
				module.reset();
			}
		}
		decentModel.dispose();
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
