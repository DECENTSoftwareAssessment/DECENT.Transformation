package epsilon.launcher;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.CachedModel;
import org.eclipse.epsilon.eol.models.IModel;

import resource.tools.ARFFxResourceTool;
import resource.tools.CFAResourceTool;
import resource.tools.DECENTResourceTool;
import resource.tools.MGResourceTool;
import ARFFx.ARFFxPackage;
import CFA.CFAPackage;
import DECENT.DECENTPackage;
import MG.MGPackage;

public class DECENTEpsilonModelHandler {
	private HashMap<String, Object> metaModelCache = new HashMap<>();
	private boolean useARFFxBinary = false;
	private boolean useDECENTBinary = false;
	private boolean useCFABinary = false;
	private boolean useDECENTDB = false;
	private boolean useMGBinary = false;
	private boolean useNeoDECENT = false;

	public IModel getDECENTModel(String location, boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.decent";
		EmfModel model;
		
		if (isUseDECENTBinary()) {
			unregisterMetaModels("");
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
			//alternative pattern
//			model = createInMemoryEmfModel("DECENT", resourceLocation, "../DECENT.Meta/model/DECENTv3.ecore", read, write, resourceBin, DECENTPackage.eINSTANCE);
//			restoreMetaModels();

			//NOTE: Adding the package is essential as otherwise epsilon breaks
			model = new InMemoryEmfModel("DECENT", resourceBin, DECENTPackage.eINSTANCE);
			model.setStoredOnDisposal(write);
			model.setReadOnLoad(read);
			model.setCachingEnabled(true);
			restoreMetaModels();		
		} else if (isUseDECENTDB()) {
			unregisterMetaModels("");
			if (!read) {
				new File(location+"/model.decent").delete();
			}
			
			DECENTResourceTool tool = new DECENTResourceTool();
			//TODO: export settings
			String db = "decent_test";
			tool.setDbServer("localhost");
			tool.setDbPort("7317");
			tool.setDbUser("cvsanaly");
			tool.setDbPass("vcsanaly");

			if (!tool.isInitializedDB(db, new EPackage[] { DECENTPackage.eINSTANCE })) {
				System.out.println("here?");
				tool.initializeDB(db);
				if (new File(location+"/model.decent").exists()) {
					Resource resource = tool.loadResourceFromXMI(location+"/model.decent","decent", DECENTPackage.eINSTANCE);
					tool.storeResourceInDB(resource.getContents(), db);
				} else {
					//TODO: if it is not initialised not converted it can be a problem
				}
			}
			//TODO: doesn't seem to work!
			Resource resourceBin = tool.loadResourceFromDB(db);
			restoreMetaModels();		
			//tool.storeResourceContents(resourceBin.getContents(), location+"/model-from-db.decent", "decent");

			//alternative pattern
//			model = createInMemoryEmfModel("DECENT", resourceLocation, "../DECENT.Meta/model/DECENTv3.ecore", read, write, resourceBin, DECENTPackage.eINSTANCE);
//			restoreMetaModels();

			//NOTE: Adding the package is essential as otherwise epsilon breaks
			model = new InMemoryEmfModel("DECENT", resourceBin, DECENTPackage.eINSTANCE);
			model.setStoredOnDisposal(write);
			model.setReadOnLoad(read);
			model.setCachingEnabled(true);
		
			
//		} else if (isUseNeoDECENT()) {
//			unregisterMetaModels("");
//			DECENTResourceTool tool = new DECENTResourceTool();
//			if (!read) {
//				new File(location+"/model.decent").delete();
//				new File(location+"/model.decentneo").delete();
//			}
//			if (new File(location+"/model.decent").exists() && !new File(location+"/model.decent"+"neo").exists()) {
//				Resource resource = tool.loadResourceFromXMI(location+"/model.decent","decent", DECENTPackage.eINSTANCE);
//				tool.storeResourceInNeo(resource.getContents(), location+"/model.decent"+"neo");
//			}
//			//TODO: get newer version or check why this doesn't work, perhaps a matter of URI?
//			//f.getFile(location);
//			Resource resourceBin = tool.loadResourceFromNeo(resourceLocation+"neo");
//			//alternative pattern
////			model = createInMemoryEmfModel("DECENT", resourceLocation, "../DECENT.Meta/model/DECENTv3.ecore", read, write, resourceBin, DECENTPackage.eINSTANCE);
////			restoreMetaModels();
//			restoreMetaModels();	
//			System.out.println(resourceBin.getContents().get(0).eClass());
//			
//
//			//NOTE: Adding the package is essential as otherwise epsilon breaks
//			model = new InMemoryEmfModel("DECENT", resourceBin, DECENTPackage.eINSTANCE);
//			model.setStoredOnDisposal(write);
//			model.setReadOnLoad(read);
//			model.setCachingEnabled(true);
//
		} else {
			model = createEmfModel("DECENT", resourceLocation, "../DECENT.Meta/model/DECENTv3.ecore", read, write);
		}

		return model;
	}

	public IModel getMGModel(String location,boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.mg";
		if (!new File(resourceLocation).exists()) {
			System.out.println("ERROR: Missing Resource: " + resourceLocation);
			System.exit(1);
		}
		EmfModel model;
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
			model.setCachingEnabled(true);
			
			restoreMetaModels();		
		} else {
			model = createEmfModel("MG", resourceLocation, "../DECENT.Meta/model/MG.ecore", read, write);
		}
		
		return model;
	}
	
	public IModel getARFFxModel(String location, boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.arffx";
		EmfModel model;
		
		if (isUseARFFxBinary()) {
			unregisterMetaModels("");
			if (!read) {
				new File(location+"/model.arffx").delete();
				new File(location+"/model.arffxbin").delete();
			}
			ARFFxResourceTool tool = new ARFFxResourceTool();
			if (new File(location+"/model.arffx").exists() && !new File(location+"/model.arffx"+"bin").exists()) {
				Resource resource = tool.loadResourceFromXMI(location+"/model.arffx","arffx", ARFFxPackage.eINSTANCE);
				tool.storeBinaryResourceContents(resource.getContents(), location+"/model.arffx"+"bin", "arffxbin");
			}
			
			Resource resourceBin = tool.loadResourceFromBinary(resourceLocation+"bin","arffxbin", ARFFxPackage.eINSTANCE);
			//alternative pattern
//			model = createInMemoryEmfModel("DECENT", resourceLocation, "../DECENT.Meta/model/DECENTv3.ecore", read, write, resourceBin, DECENTPackage.eINSTANCE);
//			restoreMetaModels();

			//NOTE: Adding the package is essential as otherwise epsilon breaks
			model = new InMemoryEmfModel("ARFFx", resourceBin, ARFFxPackage.eINSTANCE);
//			model.getModelImpl().getURI().toFileString()
			model.setStoredOnDisposal(write);
			model.setReadOnLoad(read);
			model.setCachingEnabled(true);
			restoreMetaModels();		
		} else {
			model = createEmfModel("ARFFx", resourceLocation, "../DECENT.Meta/model/ARFFx.ecore", read, write);
		}
		
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
		EmfModel model;
		
		if (isUseCFABinary()) {
			unregisterMetaModels("");
			if (!read) {
				new File(location+"/model.cfa").delete();
				new File(location+"/model.cfabin").delete();
			}
			CFAResourceTool tool = new CFAResourceTool();
			if (new File(location+"/model.cfa").exists() && !new File(location+"/model.cfa"+"bin").exists()) {
				Resource resource = tool.loadResourceFromXMI(location+"/model.cfa","cfa", CFAPackage.eINSTANCE);
				tool.storeBinaryResourceContents(resource.getContents(), location+"/model.decent"+"bin", "decentbin");
			}
			
			Resource resourceBin = tool.loadResourceFromBinary(resourceLocation+"bin","cfabin", CFAPackage.eINSTANCE);
			//alternative pattern
//			model = createInMemoryEmfModel("DECENT", resourceLocation, "../DECENT.Meta/model/DECENTv3.ecore", read, write, resourceBin, DECENTPackage.eINSTANCE);
//			restoreMetaModels();

			//NOTE: Adding the package is essential as otherwise epsilon breaks
			model = new InMemoryEmfModel("CFA", resourceBin, CFAPackage.eINSTANCE);
			model.setStoredOnDisposal(write);
			model.setReadOnLoad(read);
			model.setCachingEnabled(true);
			restoreMetaModels();		
		} else {
			model = createEmfModel("CFA", resourceLocation, "../DECENT.Meta/model/CFA.ecore", read, write);
		}
		return model;
	}

	public IModel getTRACEModel(String location, boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.trace";
		IModel model = createEmfModel("TRACE", resourceLocation, "../DECENT.Meta/model/Traces.ecore", read, write);
		return model;
	}

	public IModel getLOGModel(String location, boolean read, boolean write) throws Exception {
		String resourceLocation = location+"/model.log";
		if (!new File(resourceLocation).exists()) {
			read = false;
		}
		IModel model = createEmfModel("LOG", resourceLocation, "../DECENT.Meta/model/LOG.ecore", read, write);
		System.setProperty("epsilon.logFileAvailable", "true");
		return model;
	}
	
	public void convertDECENTModelToBinary(String location) {
		unregisterMetaModels("");
		DECENTResourceTool tool = new DECENTResourceTool();
		Resource resource = tool.loadResourceFromXMI(location+"/model.decent","decent", DECENTPackage.eINSTANCE);
		tool.storeBinaryResourceContents(resource.getContents(), location+"/model.decent"+"bin", "decentbin");
		restoreMetaModels();		
	}

	public void convertDECENTModelToDB(String location) {
		unregisterMetaModels("");
		DECENTResourceTool tool = new DECENTResourceTool();
		String db = "decent_test";
		tool.setDbServer("localhost");
		tool.setDbPort("7317");
		tool.setDbUser("cvsanaly");
		tool.setDbPass("vcsanaly");
		tool.initializeDB(db);
		Resource resource = tool.loadResourceFromXMI(location+"/model.decent","decent", DECENTPackage.eINSTANCE);
		tool.storeResourceInDB(resource.getContents(), db );
		restoreMetaModels();		
	}

	public void convertDECENTModelToXMI(String location) {
		unregisterMetaModels("");
		DECENTResourceTool tool = new DECENTResourceTool(); 
		Resource resource = tool.loadResourceFromBinary(location+"/model.decentbin","decentbin", DECENTPackage.eINSTANCE);
		restoreMetaModels();		
		tool.storeResourceContents(resource.getContents(), location+"/model.decent", "decent");
	}


	public void convertARFFxModelToBinary(String location) {
		unregisterMetaModels("");
		ARFFxResourceTool tool = new ARFFxResourceTool();
		Resource resource = tool.loadResourceFromXMI(location+"/model.arffx","arffx", ARFFxPackage.eINSTANCE);
		tool.storeBinaryResourceContents(resource.getContents(), location+"/model.arffx"+"bin", "arffxbin");
		restoreMetaModels();		
	}

	public void convertARFFxModelToXMI(String location) {
		unregisterMetaModels("");
		ARFFxResourceTool tool = new ARFFxResourceTool(); 
		Resource resource = tool.loadResourceFromBinary(location+"/model.arffxbin","arffxbin", DECENTPackage.eINSTANCE);
		restoreMetaModels();		
		tool.storeResourceContents(resource.getContents(), location+"/model.arffx", "arffx");
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
		emfModel.setCachingEnabled(true);
		restoreMetaModels();		

//		sampleModel(emfModel);
		return emfModel;
	}

	protected EmfModel createInMemoryEmfModel(String name, String model, 
			String metamodel, boolean readOnLoad, boolean storeOnDisposal, Resource resource, EPackage einstance) 
					throws EolModelLoadingException, URISyntaxException {
		EmfModel emfModel = new InMemoryEmfModel(name, resource, einstance);
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_NAME, name);
		properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, 
				"file:/" + getFile(metamodel).getAbsolutePath());
		properties.put(EmfModel.PROPERTY_MODEL_URI, 
				"file:/" + getFile(model).getAbsolutePath());
		properties.put(EmfModel.PROPERTY_IS_METAMODEL_FILE_BASED, "true");
		properties.put(EmfModel.PROPERTY_READONLOAD, readOnLoad + "");
		properties.put(EmfModel.PROPERTY_CACHED, "true");
		properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, 
				storeOnDisposal + "");
		emfModel.load(properties, null);
		return emfModel;
	}

	
	protected EmfModel createEmfModel(String name, String model, 
			String metamodel, boolean readOnLoad, boolean storeOnDisposal) 
					throws EolModelLoadingException, URISyntaxException {
		EmfModel emfModel = new EmfModel();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_NAME, name);
		properties.put(EmfModel.PROPERTY_ALIASES, name);
		properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, 
				"file:/" + getFile(metamodel).getAbsolutePath());
		properties.put(EmfModel.PROPERTY_MODEL_URI, 
				"file:/" + getFile(model).getAbsolutePath());
		properties.put(EmfModel.PROPERTY_IS_METAMODEL_FILE_BASED, "true");
		properties.put(EmfModel.PROPERTY_READONLOAD, readOnLoad + "");
		properties.put(EmfModel.PROPERTY_CACHED, "true");
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
		properties.put(EmfModel.PROPERTY_CACHED, "true");
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

	public boolean isUseDECENTDB() {
		return useDECENTDB;
	}

	public void setUseDECENTDB(boolean useDECENTDB) {
		this.useDECENTDB = useDECENTDB;
	}

	public boolean isUseARFFxBinary() {
		return useARFFxBinary;
	}

	public void setUseARFFxBinary(boolean useARFFxBinary) {
		this.useARFFxBinary = useARFFxBinary;
	}

	public boolean isUseNeoDECENT() {
		return useNeoDECENT;
	}

	public void setUseNeoDECENT(boolean useNeoDECENT) {
		this.useNeoDECENT = useNeoDECENT;
	}

	public boolean isUseCFABinary() {
		return useCFABinary;
	}

	public void setUseCFABinary(boolean useCFABinary) {
		this.useCFABinary = useCFABinary;
	}

}
