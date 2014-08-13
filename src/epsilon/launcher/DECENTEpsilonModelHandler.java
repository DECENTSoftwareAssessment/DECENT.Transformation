package epsilon.launcher;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.IModel;

import resource.tools.DECENTResourceTool;
import resource.tools.MGResourceTool;
import DECENT.DECENTPackage;
import MG.MGPackage;

public class DECENTEpsilonModelHandler {
	private HashMap<String, Object> metaModelCache = new HashMap<>();
	private boolean useDECENTBinary = true;
	private boolean useMGBinary = true;

	public IModel getDECENTModel(String location, boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.decent";
		IModel model;
		
		if (isUseDECENTBinary()) {
			unregisterMetaModels("decent");
			if (!read) {
				new File(location+"/model.decent").delete();
				new File(location+"/model.decentbin").delete();
			}
			DECENTResourceTool tool = new DECENTResourceTool();
			if (new File(location+"/model.decent").exists() && !new File(location+"/model.decent"+"bin").exists()) {
				Resource resource = tool.loadResourceFromXMI(location+"/model.decent","decent", DECENTPackage.eINSTANCE);
				tool.storeBinaryResourceContents(resource.getContents(), location+"/model.decent"+"bin", "decentbin");
			}
			
			Resource resourceBin = tool.loadResourceFromBinary(resourceLocation+"bin","decentbin", DECENTPackage.eINSTANCE);
			//NOTE: Adding the package is essential as otherwise epsilon breaks
			model = new InMemoryEmfModel("DECENT", resourceBin, DECENTPackage.eINSTANCE);
			model.setStoredOnDisposal(write);
			model.setReadOnLoad(read);
		
			restoreMetaModels();		
		} else {
			model = createEmfModel("DECENT", resourceLocation, "../DECENT.Meta/model/DECENTv3.ecore", read, write);
		}

		return model;
	}

	public IModel getMGModel(String location,boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.mg";
		IModel model;
		if (isUseMGBinary()) {
			unregisterMetaModels("");
			
			MGResourceTool tool = new MGResourceTool();
			if (!new File(location+"/model.mg"+"bin").exists()) {
				Resource resource = tool.loadResourceFromXMI(location+"/model.mg","mg", MGPackage.eINSTANCE);
				tool.storeBinaryResourceContents(resource.getContents(), location+"/model.mg"+"bin", "mgbin");
			}

			Resource resourceBin = tool.loadResourceFromBinary(resourceLocation+"bin","mgbin", MGPackage.eINSTANCE);
			//NOTE: Adding the package is essential as otherwise epsilon breaks
			model = new InMemoryEmfModel("MG", resourceBin, MGPackage.eINSTANCE);
			model.setStoredOnDisposal(write);
			model.setReadOnLoad(read);
			
			restoreMetaModels();		
		} else {
			model = createEmfModel("MG", resourceLocation, "../DECENT.Meta/model/MG.ecore", read, write);
		}
		
		return model;
	}
	
	public IModel getARFFxModel(String location, boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.arffx";
		IModel model = createEmfModel("ARFFx", resourceLocation, "../DECENT.Meta/model/ARFFx.ecore", read, write);
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

	public IModel getBZModel(String location) throws Exception {
		String resourceLocation = location+"/model.bz";
		IModel model = createEmfModel("BZ", resourceLocation, "../DECENT.Meta/model/BZ.ecore", true, false);
		return model;
	}

	public IModel getDAGModel(String location) throws Exception {
		String resourceLocation = location+"/model.dag";
		IModel model = createEmfModel("DAG", resourceLocation, "../DECENT.Meta/model/DAG.ecore", true, false);
		return model;
	}
	
	public IModel getDUDEModel(String location) throws Exception {
		String resourceLocation = location+"/model.dude";
		IModel model = createEmfModel("DUDE", resourceLocation, "../DECENT.Meta/model/DuDe.ecore", true, false);
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

	public void convertDECENTModelToBinary(String location) {
		unregisterMetaModels("decent");
		DECENTResourceTool tool = new DECENTResourceTool();
		Resource resource = tool.loadResourceFromXMI(location+"/model.decent","decent", DECENTPackage.eINSTANCE);
		tool.storeBinaryResourceContents(resource.getContents(), location+"/model.decent"+"bin", "decentbin");
		restoreMetaModels();		
	}

	public void convertDECENTModelToXMI(String location) {
		unregisterMetaModels("decent");
		DECENTResourceTool tool = new DECENTResourceTool(); 
		Resource resource = tool.loadResourceFromBinary(location+"/model.decentbin","decentbin", DECENTPackage.eINSTANCE);
		tool.storeResourceContents(resource.getContents(), location+"/model.decent", "decent");
		restoreMetaModels();		
	}

	public IModel getBinaryDECENTModel(String location, boolean readOnLoad, boolean storedOnDisposal) throws Exception {
		String resourceLocation = location+"/model.decent";
		unregisterMetaModels("decent");

		DECENTResourceTool tool = new DECENTResourceTool();
		if (!new File(location+"/model.decent"+"bin").exists()) {
			Resource resource = tool.loadResourceFromXMI(location+"/model.decent","decent", DECENTPackage.eINSTANCE);
			tool.storeBinaryResourceContents(resource.getContents(), location+"/model.decent"+"bin", "decentbin");
		}

		Resource resourceBin = tool.loadResourceFromBinary(resourceLocation+"bin","decentbin", DECENTPackage.eINSTANCE);
		//NOTE: Adding the package is essential as otherwise epsilon breaks
		InMemoryEmfModel emfModel = new InMemoryEmfModel("DECENT", resourceBin, DECENTPackage.eINSTANCE);
		emfModel.setStoredOnDisposal(storedOnDisposal);
		emfModel.setReadOnLoad(readOnLoad);
		restoreMetaModels();		

//		sampleModel(emfModel);
		return emfModel;
	}

	protected EmfModel createEmfModel(String name, String model, 
			String metamodel, boolean readOnLoad, boolean storeOnDisposal) 
					throws EolModelLoadingException, URISyntaxException {
		EmfModel emfModel = new EmfModel();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_NAME, name);
		properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, 
				"file:/" + getFile(metamodel).getAbsolutePath());
		properties.put(EmfModel.PROPERTY_MODEL_URI, 
				"file:/" + getFile(model).getAbsolutePath());
		properties.put(EmfModel.PROPERTY_IS_METAMODEL_FILE_BASED, "true");
		properties.put(EmfModel.PROPERTY_READONLOAD, readOnLoad + "");
		properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, 
				storeOnDisposal + "");
		emfModel.load(properties, null);
		return emfModel;
	}

	protected EmfModel createEmfModelByURI(String name, String model, 
			String metamodel, boolean readOnLoad, boolean storeOnDisposal) 
					throws EolModelLoadingException, URISyntaxException {
		EmfModel emfModel = new EmfModel();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_NAME, name);
		properties.put(EmfModel.PROPERTY_METAMODEL_URI, metamodel);
		properties.put(EmfModel.PROPERTY_MODEL_URI, 
				"file:/" + getFile(model).getAbsolutePath());
		properties.put(EmfModel.PROPERTY_IS_METAMODEL_FILE_BASED, "false");
		properties.put(EmfModel.PROPERTY_READONLOAD, readOnLoad + "");
		properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, 
				storeOnDisposal + "");
		emfModel.load(properties, null);
		return emfModel;
	}

	protected File getFile(String fileName) throws URISyntaxException {
		return new File(fileName);
	}

	private void restoreMetaModels() {
		for (String key : metaModelCache .keySet()) {
			EPackage.Registry.INSTANCE.put(key, metaModelCache.get(key));
		};
	}

	private void unregisterMetaModels(String filter) {
		for (String key : EPackage.Registry.INSTANCE.keySet()) {
			if (key.contains(filter)) {
				metaModelCache.put(key, EPackage.Registry.INSTANCE.get(key));
			}
		};
		for (String key : metaModelCache .keySet()) {
			EPackage.Registry.INSTANCE.remove(key);
		};
	}

	public boolean isUseDECENTBinary() {
		return useDECENTBinary;
	}

	public void setUseDECENTBinary(boolean useDECENTBinary) {
		this.useDECENTBinary = useDECENTBinary;
	}

	public boolean isUseMGBinary() {
		return useMGBinary;
	}

	public void setUseMGBinary(boolean useMGBinary) {
		this.useMGBinary = useMGBinary;
	}

}
