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
		//String location = "/Users/philip-iii/Dev/workspaces/emf/DECENT.Data/input/yakuake";
		executeSeparate(location, lowerBound, upperBound); //reloads only famix model instances
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

		module.reset();
		
		IModel decentModel = getDecentModel(location);
		decentModel.load();
		//module.getContext().getModelRepository().addModel(decentModel);

		String[] commits = getSortedCommits(location);
		
		//TODO: add option "semi-safe" which stores intermediate decent model but does not reload it (optionally with a frequency - every 1/2/5/10 revisions)
		//TODO: add option "safe" which stores each intermediate decent model (and/or reloads it)
		for (String c : commits) {
			if (	Integer.parseInt(c)>=(Integer.parseInt(lowerBound)) &&
					Integer.parseInt(c)<=(Integer.parseInt(upperBound))
			) {
				System.out.println("Processing: "+c);
				module.parse(getFile(getSource()));
				IModel famixModel = getFamixModel(location,Integer.parseInt(c));
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
	
	public IModel getDecentModel(String location) throws Exception {
		String decentResourceLocation = location+"/model.decent";
		IModel model = createEmfModel("DECENT", decentResourceLocation, "../DECENT.Meta/model/DECENTv3.ecore", true, true);
		return model;
	}

	public IModel getFamixModel(String location, int commitId) throws Exception {
		//TODO: make filtered optional infix
		String famixResourceLocation = location+"/famix/"+commitId+"/filtered/model.famix";
		IModel model = createEmfModel("FAMIX", famixResourceLocation, "../DECENT.Meta/model/FAMIX.ecore", true, false);
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
		return "src/sample/famix2decent3.etl";
	}

	@Override
	public void postProcess() {
		
	}

}
