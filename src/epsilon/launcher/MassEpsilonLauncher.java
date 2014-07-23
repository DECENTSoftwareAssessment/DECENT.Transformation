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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.emc.emf.EmfUtil;
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolExecutableModule;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.etl.EtlModule;

import epsilon.launcher.EpsilonStandaloneLauncher;

/**
 * This example demonstrates using the 
 * Epsilon Transformation Language, the M2M language
 * of Epsilon, in a stand-alone manner 
 * @author Dimitrios Kolovos
 */
public class MassEpsilonLauncher extends EpsilonStandaloneLauncher {
	
	public static void main(String[] args) throws Exception {
		MassEpsilonLauncher launcher = new MassEpsilonLauncher();
		launcher.registerMetaModels();
		launcher.execute(args[0]);
	}
	
	public IEolExecutableModule createModule() {
		return new EtlModule();
	}

	public void execute(String location) throws Exception {
		//TODO: check if resources are available 
//		executeMG2NORMALIZEDHUNKS(location);
//		executeMG2DECENT(location);
//		executeMG2CFA(location);
//		executeCFA2DECENT(location);
//		executeTEMPORAL2DECENT(location);
//		executeBZ2TRACE(location);
//		executeTRACE2DECENT(location);
		String lowerBound = "0";
		String upperBound = "0";
		executeFAMIX2DECENT(location, lowerBound, upperBound); //reloads only famix model instances
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
		IModel decentModel = getDECENTModel(location, true, true);
		IModel traceModel = getTRACEModel(location, true, false);
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
		IModel mgModel = getMGModel(location, true, false);
		IModel bzModel = getBZModel(location);
		IModel traceModel = getTRACEModel(location, false, true);
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

	
	private void executeCFA2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/cfa2decent3.etl";
		IEolExecutableModule module = loadModule(source);
		IModel decentModel = getDECENTModel(location, true, true);
		//TODO: consider removing reliance on MG especially if it is only needed in one line
		IModel cfaModel = getCFAModel(location, true, false);
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
		IModel mgModel = getMGModel(location, true, false);
		IModel cfaModel = getCFAModel(location, false, true);
//		mgModel.load();
		module.getContext().getModelRepository().addModel(cfaModel);
		module.getContext().getModelRepository().addModel(mgModel);
		module.execute();
		mgModel.dispose();
		//can be stored and retained alternatively
		cfaModel.dispose();
		module.reset();
	}

	private void executeTEMPORAL2DECENT(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/temporal2decent3.eol";
		IEolExecutableModule module = loadModule(source);
		IModel decentModel = getDECENTModel(location, true, true);
//		decentModel.load();
		module.getContext().getModelRepository().addModel(decentModel);
		module.execute();
		decentModel.dispose();
		//can be stored and retained alternatively
		module.reset();
	}

	
	private void executeMG2NORMALIZEDHUNKS(String location) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		String source = "epsilon/transform/mg2normalized_hunks.eol";
		IEolExecutableModule module = loadModule(source);
		IModel mgModel = getMGModel(location, true, true);
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
		IModel decentModel = getDECENTModel(location, false, true);
		IModel mgModel = getMGModel(location, true, false);
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
		
		IModel decentModel = getDECENTModel(location, true, true);
//		decentModel.load();
		//module.getContext().getModelRepository().addModel(decentModel);

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
				module.parse(getFile(getSource()));
				IModel famixModel = getFAMIXModel(location,Integer.parseInt(c));
				famixModel.load();
				module.getContext().getModelRepository().addModel(decentModel);
				module.getContext().getModelRepository().addModel(famixModel);
				preProcess();
				module.execute();
				postProcess();
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
		
		module.parse(getFile(source));

		if (module.getParseProblems().size() > 0) {
			System.err.println("Parse errors occured...");
			for (ParseProblem problem : module.getParseProblems()) {
				System.err.println(problem.toString());
			}
			System.exit(-1);
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
	
	public IModel getDECENTModel(String location, boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.decent";
		IModel model = createEmfModel("DECENT", resourceLocation, "../DECENT.Meta/model/DECENTv3.ecore", read, write);
		//In memory example -> can be used to work with DB? or even EMF-Fragments
		//new InMemoryEmfModel("DECENT", resource, "../DECENT.Meta/model/DECENTv3.ecore")
		return model;
	}

	public IModel getFAMIXModel(String location, int commitId) throws Exception {
		//TODO: make filtered optional infix
		String resourceLocation = location+"/famix/"+commitId+"/filtered/model.famix";
		IModel model = createEmfModel("FAMIX", resourceLocation, "../DECENT.Meta/model/FAMIX.ecore", true, false);
		return model;
	}
	
	public IModel getMGModel(String location,boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.mg";
		IModel model = createEmfModel("MG", resourceLocation, "../DECENT.Meta/model/MG.ecore", read, write);
		return model;
	}

	public IModel getBZModel(String location) throws Exception {
		String resourceLocation = location+"/model.bz";
		IModel model = createEmfModel("BZ", resourceLocation, "../DECENT.Meta/model/BZ.ecore", true, false);
		return model;
	}

	public IModel getCFAModel(String location, boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.cfa";
		IModel model = createEmfModel("CFA", resourceLocation, "../DECENT.Meta/model/CFA.ecore", read, write);
		return model;
	}

	public IModel getTRACEModel(String location, boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.trace";
		IModel model = createEmfModel("TRACE", resourceLocation, "../DECENT.Meta/model/Traces.ecore", read, write);
		return model;
	}

	
	@Override
	public List<IModel> getModels() throws Exception {
		List<IModel> models = new ArrayList<IModel>();
		
		return models;
	}

	//TODO: extract all parameters and configurations
	@Override
	public String getSource() throws Exception {
		//return "src/sample/famix2decent.etl";
		return "epsilon/transform/famix2decent3.etl";
	}

	@Override
	public void postProcess() {
		
	}

}