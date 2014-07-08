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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.epsilon.emc.emf.EmfUtil;
import org.eclipse.epsilon.eol.IEolExecutableModule;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.etl.EtlModule;

import epsilon.launcher.EpsilonStandaloneLauncher;

/**
 * This example demonstrates using the 
 * Epsilon Transformation Language, the M2M language
 * of Epsilon, in a stand-alone manner 
 * @author Dimitrios Kolovos
 */
public class EtlStandaloneLauncher extends EpsilonStandaloneLauncher {
	
	public static void main(String[] args) throws Exception {
		new EtlStandaloneLauncher().execute();
	}
	
	@Override
	public IEolExecutableModule createModule() {
		return new EtlModule();
	}

	@Override
	public List<IModel> getModels() throws Exception {
		EmfUtil.register(URI.createFileURI(new File("../DECENT.One/model/DECENTv3.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		EmfUtil.register(URI.createFileURI(new File("../DECENT.One/model/AbstractDECENTProvider.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		EmfUtil.register(URI.createFileURI(new File("../DECENT.One/model/FAMIX.ecore").getAbsolutePath()), EPackage.Registry.INSTANCE);
		
		List<IModel> models = new ArrayList<IModel>();
		models.add(createEmfModel("DECENT", "output/MGGitWS.decent", "../DECENT.One/model/DECENTv3.ecore", true, true));
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
