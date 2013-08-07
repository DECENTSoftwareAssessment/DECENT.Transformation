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
		new EtlAdvancedStandaloneLauncher().execute();
	}
	
	@Override
	public IEolExecutableModule createModule() {
		return new EtlModule();
	}

	@Override
	public void execute() throws Exception {
		//NOTE: a somewhat ugly workaround for the ANT based launcher that refuses to store changes for some reason when ran for 
		//multiple revisions
		//TODO: pending refinement (this will probably evolve into being the go-to launcher as ANT is a waste of time and its limits 
		//are becoming more and more evident. It was good use to get started but ultimately with sufficient refinement a Java-based
		//launcher can be equally simplistic
		EmfUtil.register(URI.createFileURI(new File("../DECENT/model/DECENTv2.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		EmfUtil.register(URI.createFileURI(new File("../famix.m3/model/AbstractDECENTProvider.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		EmfUtil.register(URI.createFileURI(new File("../famix.m3/model/FAMIX.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		
		for (int i = 1; i <= 2; i++) {
			executeForCommitId(i);
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

		for (IModel model : getModels(commitId)) {
			module.getContext().getModelRepository().addModel(model);
		}
		
		preProcess();
		result = execute(module);
		postProcess();
		
		module.getContext().getModelRepository().dispose();
	}

	public List<IModel> getModels(int commitId) throws Exception {
		List<IModel> models = new ArrayList<IModel>();
		models.add(createEmfModel("DECENT", "output/MGGitWS.decent", "../DECENT/model/DECENTv2.ecore", true, true));
		models.add(createEmfModel("FAMIX", "/home/philip-iii/TEMP/fmx/famix/"+commitId+"/model.famix", "../famix.m3/model/FAMIX.ecore", true, false));
		
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

	@Override
	public String getSource() throws Exception {
		return "src/sample/famix2decent.etl";
	}

	@Override
	public void postProcess() {
		
	}

}
