/**
 * ﻿Copyright (C) 2007 - 2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.wps.server.ilwis;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.opengis.wps.x100.ProcessDescriptionType;

import org.n52.ilwis.java.Engine;
import org.n52.ilwis.java.IlwisOperation;
import org.n52.ilwis.java.ilwisobjects;
import org.n52.ilwis.java.vectorOperation;
import org.n52.wps.PropertyDocument.Property;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.IAlgorithm;
import org.n52.wps.server.IAlgorithmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * A container, which allows the 52n WPS to recognize the Ilwis library.
 * Basic initialization is performed here.
 * 
 * Whenever a getcapabilities request comes in, the process names are extraced based on the available process description documents for Ilwis processes.
 * This should be changed in the future, when process descriptions should be generated automatically. When a execute process request comes in, a generic GenericIlwisProcessDelegator is created. 
 */

public class IlwisProcessRepository implements IAlgorithmRepository {
	private static Logger LOGGER = LoggerFactory
			.getLogger(IlwisProcessRepository.class);
	private Map<String, ProcessDescriptionType> registeredProcesses;
	private Map<String, Long> IDToIlwisID = new HashMap<String, Long>();

	@Override
	public Collection<String> getAlgorithmNames() {
		return registeredProcesses.keySet();
	}

	@Override
	public IAlgorithm getAlgorithm(String processID) {
		if (!containsAlgorithm(processID)) {
			throw new RuntimeException("Could not allocate Process: " + processID);
		}
		processID = processID.replace("org.n52.wps.server.ilwis.", "");
		if(!IDToIlwisID.containsKey(processID)) {
			throw new RuntimeException("Could not find Process ID");
		}
		return new GenericIlwisProcessDelegator(processID,
				registeredProcesses.get(processID), IDToIlwisID.get(processID));
	}

	@Override
	public ProcessDescriptionType getProcessDescription(String processID) {
		if (!registeredProcesses.containsKey(processID)) {
			registeredProcesses.put(processID, getAlgorithm(processID)
					.getDescription());
		}
		return registeredProcesses.get(processID);
	}

	public boolean containsAlgorithm(String processID) {
		if (registeredProcesses.containsKey(processID)) {
			return true;
		}
		LOGGER.warn("Could not find Ilwis Process " + processID);
		return false;
	}

	@Override
	public void shutdown() {
		// Not used
		// Ilwis shutdown is included in the ilwisobjects.initIlwisObjects()
	}

	public IlwisProcessRepository() {
		LOGGER.info("Initializing Ilwis Repository");
		registeredProcesses = new HashMap<String, ProcessDescriptionType>();

		/*
		 * get properties of Repository
		 * 
		 * check whether process is amongst them and active
		 * 
		 * if properties are empty (not initialized yet) add all valid processes
		 * to WPSConfig
		 */

		Property[] propertyProcesses = WPSConfig.getInstance()
				.getPropertiesForRepositoryClass(this.getClass().getName());

		ArrayList<String> processList = new ArrayList<String>(
				propertyProcesses.length);

		for (Property prop : propertyProcesses) {
			if(prop.getName().equals("IlwisLocation")) {
				ilwisobjects.setIlwisLocation(prop.getStringValue());
				LOGGER.info("Ilwis location set to: " + prop.getStringValue());
			}
			if(prop.getName().equals("Algorithm")){
				if (prop.getActive()) {
					processList.add(prop.getStringValue());
				} else {
					LOGGER.info("Ilwis Process : " + prop.getStringValue()
							+ " not active.");
				}
			}
		}

		// Ilwis initialize
		try {
			ilwisobjects.initIlwisObjects();
		} catch (FileNotFoundException e) {
			LOGGER.info(e.getMessage());
		}

		vectorOperation ilwisOperations = Engine.getAllOperations();
		IlwisProcessDescriptionCreator descriptionCreator = new IlwisProcessDescriptionCreator();
		
		// Indexing the identic named operations
		for(int i=0; i<ilwisOperations.size(); i++) {
			String operationName = ilwisOperations.get(i).getName();
			int index = 0;
			while(IDToIlwisID.containsKey( operationName )) {
				operationName = ilwisOperations.get(i).getName() + "_" + ++index;
			}
			IDToIlwisID.put(operationName, ilwisOperations.get(i).getId());
		}

		for(String s : IDToIlwisID.keySet()) {
			String key = "org.n52.wps.server.ilwis." + s;
			if (!processList.contains(key) ) {
				LOGGER.info("Did not add Ilwis Process : " + key
						+ ". Not in Repository properties or not active.");
				continue;
			}
			IlwisOperation ilwisProcess = Engine.getOperationById( IDToIlwisID.get(s) );
			ProcessDescriptionType processDescription;
			try {
				processDescription = descriptionCreator
						.createDescribeProcessType(ilwisProcess);
			} catch (Exception e) {
				LOGGER.warn("Could not add Ilwis Process : " + key
						+ ". Errors while creating describe Process");
				continue;
			}

			registeredProcesses.put(key, processDescription);
			LOGGER.info("Ilwis Process " + key + " added.");
		}

		LOGGER.info("Initialization of Ilwis Repository successful");
	}

}
