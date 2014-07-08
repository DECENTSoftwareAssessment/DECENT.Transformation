package epsilon.launcher.deprecated;
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
import org.eclipse.epsilon.egl.EglTemplateFactory;
import org.eclipse.epsilon.egl.EglTemplateFactoryModuleAdapter;
import org.eclipse.epsilon.egl.internal.EglModule;
import org.eclipse.epsilon.emc.emf.EmfUtil;
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
public class EglAdvancedStandaloneLauncher extends EpsilonStandaloneLauncher {
	
	public static void main(String[] args) throws Exception {
		new EglAdvancedStandaloneLauncher().execute();
	}
	
	@Override
	public IEolExecutableModule createModule() {
		return new EglTemplateFactoryModuleAdapter(new EglTemplateFactory());
	}

	@Override
	public void execute() throws Exception {
		EmfUtil.register(URI.createFileURI(new File("../DECENT/model/DECENTv2.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		super.execute();
	}

	@Override
	public List<IModel> getModels() throws Exception {
		List<IModel> models = new ArrayList<IModel>();
		models.add(createEmfModel("DECENT", "output/MGGitWS.decent", "../DECENT/model/DECENTv2.ecore", true, false));
		return models;
	}

	@Override
	public String getSource() throws Exception {
		return "src/sample/decent.egl";
	}

	@Override
	public void postProcess() {
		System.out.println(result);
	}

}
