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
public class EtlAdvancedStandaloneLauncher extends EpsilonStandaloneLauncher {
	
	public static void main(String[] args) throws Exception {
		new EtlAdvancedStandaloneLauncher().execute(args[0], args[1], args[2]);
	}
	
	@Override
	public IEolExecutableModule createModule() {
		return new EtlModule();
	}

	public void execute(String location, String lowerBound, String upperBound) throws Exception {
		EmfUtil.register(URI.createFileURI(new File("../DECENT.Meta/model/DECENTv2.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		EmfUtil.register(URI.createFileURI(new File("../DECENT.Meta/model/DECENTv3.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		EmfUtil.register(URI.createFileURI(new File("../DECENT.Meta/model/AbstractDECENTProvider.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		EmfUtil.register(URI.createFileURI(new File("../DECENT.Meta/model/FAMIX.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		lowerBound = "2";
		upperBound = "4";
		//executeDefault(); //default slower version (reloads decent model instance every time)
		//String location = "/Users/philip-iii/Dev/workspaces/emf/DECENT.Data/input/yakuake";
		executeSeparate(location, lowerBound, upperBound); //reloads only famix model instances
	}
	
	private void executeDefault() throws Exception {
		//NOTE: a somewhat ugly workaround for the ANT based launcher that refuses to store changes for some reason when ran for 
		//multiple revisions
		//TODO: pending refinement (this will probably evolve into being the go-to launcher as ANT is a waste of time and its limits 
		//are becoming more and more evident. It was good use to get started but ultimately with sufficient refinement a Java-based
		//launcher can be equally simplistic
		
//		for (int i = 1; i <= 50; i++) {
//			executeForCommitId(i);
//		}
		
		String famixResourceLocation = "/home/philip-iii/TEMP";
		famixResourceLocation = "/media/DATA/Backup/Results/rekonq/fmx/famix/";
		famixResourceLocation = "/Users/philip-iii/Dev/workspaces/emf/DECENT.Data/input/yakuake/famix";
		

		File ws = new File(famixResourceLocation);
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
		
		for (String c : commits) {
			if (Integer.parseInt(c)<50) {
				System.out.println("Processing: "+c);
				executeForCommitId(Integer.parseInt(c));
			}
		}
	}

	private void executeForCommitId(int commitId) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		module = createModule();
		module.parse(getFile(getSource()));
		
		if (module.getParseProblems().size() > 0) {
			System.err.println("Parse errors occured...");
			for (ParseProblem problem : module.getParseProblems()) {
				System.err.println(problem.toString());
			}
			System.exit(-1);
		}

		for (IModel model : getModelsForCommit(commitId)) {
			module.getContext().getModelRepository().addModel(model);
		}
		
		preProcess();
		result = execute(module);
		postProcess();
		//TODO: revamp to dispose only of the famix model and reload only a new famix model
		module.getContext().getModelRepository().dispose();
	}

	private void executeSeparate(String location, String lowerBound, String upperBound) throws Exception, URISyntaxException,
			EolModelLoadingException, EolRuntimeException {
		module = createModule();
		module.parse(getFile(getSource()));

		if (module.getParseProblems().size() > 0) {
			System.err.println("Parse errors occured...");
			for (ParseProblem problem : module.getParseProblems()) {
				System.err.println(problem.toString());
			}
			System.exit(-1);
		}

		module.getContext().getModelRepository().addModel(getDecentModel(location));
		
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
		
		for (String c : commits) {
			if (	Integer.parseInt(c)>=(Integer.parseInt(lowerBound)) &&
					Integer.parseInt(c)<=(Integer.parseInt(upperBound))
			) {
				System.out.println("Processing: "+c);
				IModel famixModel = getFamixModel(location,Integer.parseInt(c));
				module.getContext().getModelRepository().addModel(famixModel);
				//System.out.println("@"+module.getContext().getModelRepository().getModels().size()+" models loaded");
				preProcess();
				//module.execute();
				result = execute(module);
				postProcess();
				//module.getContext().getModelRepository().getModelByName("FAMIX").dispose();
				//module.getContext().getModelRepository().getModelByName("FAMIX").dispose();
//				module.getContext().getModelRepository().removeModel(famixModel);
				module.reset();
				module.parse(getFile(getSource()));
				module.getContext().getModelRepository().addModel(getDecentModel(location));

			}
		}
		module.getContext().getModelRepository().dispose();
	}
	
	public IModel getDecentModel(String location) throws Exception {
		String decentResourceLocation = location+"/model.decent";
		IModel model = createEmfModel("DECENT", decentResourceLocation, "../DECENT.Meta/model/DECENTv3.ecore", true, true);
		return model;
	}

	public IModel getFamixModel(String location, int commitId) throws Exception {
		//TODO: make filtered optional infix
		String famixResourceLocation = location+"/famix/"+commitId+"/filtered/model.famix";
		IModel model =  createEmfModel("FAMIX", famixResourceLocation, "../DECENT.Meta/model/FAMIX.ecore", true, false);
		return model;
	}
	
	
	public List<IModel> getModelsForCommit(int commitId) throws Exception {
		List<IModel> models = new ArrayList<IModel>();
		String decentResourceLocation = "output/MGGitWS.decent";
		decentResourceLocation = "output/rekonq.decent";
		String famixResourceLocation = "/home/philip-iii/TEMP";
		famixResourceLocation = "/media/DATA/Backup/Results/rekonq";
		models.add(createEmfModel("DECENT", decentResourceLocation, "../DECENT/model/DECENTv3.ecore", true, true));
		models.add(createEmfModel("FAMIX", famixResourceLocation + "/fmx/famix/"+commitId+"/model.famix", "../famix.m3/model/FAMIX.ecore", true, false));
		
		return models;
	}

	
	@Override
	public List<IModel> getModels() throws Exception {
		List<IModel> models = new ArrayList<IModel>();
		models.add(createEmfModel("DECENT", "output/MGGitWS.decent", "../DECENT/model/DECENTv2.ecore", true, true));
//		models.add(createEmfModel("FAMIX", "input/model.famix", "../famix.m3/model/FAMIX.ecore", true, false));
		models.add(createEmfModel("FAMIX", "/home/philip-iii/TEMP/fmx/famix/8/model.famix", "../famix.m3/model/FAMIX.ecore", true, false));
		
		return models;
	}

	//TODO: extract all parameters and configurations
	@Override
	public String getSource() throws Exception {
		//return "src/sample/famix2decent.etl";
		return "src/sample/famix2decent3.etl";
	}

	@Override
	public void postProcess() {
		
	}

}
