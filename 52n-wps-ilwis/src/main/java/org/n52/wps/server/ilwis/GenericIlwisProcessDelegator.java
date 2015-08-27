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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.opengis.wps.x100.ProcessDescriptionType;

import org.n52.ilwis.java.CoordinateSystem;
import org.n52.ilwis.java.Engine;
import org.n52.ilwis.java.FeatureCoverage;
import org.n52.ilwis.java.IObject;
import org.n52.ilwis.java.IlwisOperation;
import org.n52.ilwis.java.RasterCoverage;
import org.n52.ilwis.java.ilwisobjects;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralIntBinding;
import org.n52.wps.io.data.binding.literal.LiteralLongBinding;
import org.n52.wps.io.data.binding.literal.LiteralShortBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.IAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericIlwisProcessDelegator implements IAlgorithm {

	private static Logger LOGGER = LoggerFactory
			.getLogger(GenericIlwisProcessDelegator.class);

	private String processID;
	private ProcessDescriptionType processDescription;
	private List<String> errors = new ArrayList<String>();
	private IlwisOperation ilwisProcess;
	private Map<String, Integer> inputId = new HashMap<String, Integer>();
	private Map<String, Integer> outputId = new HashMap<String, Integer>();

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData) {
		Map<String, IData> result = new HashMap<String, IData>();
		String[] ilwisInputList = new String[inputData.size()];
		String ilwisWorkingDir = System.getProperty("java.io.tmpdir");

		try {

			// Convert the inputs to string
			for (String inID : inputData.keySet()) {
				int i = inputId.get(inID) - 1;
				long ilwisType = ilwisProcess.getPinType(i + 1);

				// Checks if this input is required
				if (inputData.get(inID) == null) {
					throw new RuntimeException(inID + " is not required by "
							+ processID);
				}

				if (inputData.get(inID).get(0) instanceof GenericFileDataBinding) { // File
					GenericFileDataBinding fileDataBinding = (GenericFileDataBinding) inputData
							.get(inID).get(0);
					File file = fileDataBinding.getPayload().getBaseFile(true);
					ilwisInputList[i] = file.getName();
					ilwisWorkingDir = file.getParent();
				} else if (inputData.get(inID).size() == 2
						&& inputData.get(inID).get(0) instanceof LiteralDoubleBinding) { // Coordinate
					ilwisInputList[i] = new org.n52.ilwis.java.Coordinate(
							(Double) inputData.get(inID).get(0).getPayload(),
							(Double) inputData.get(inID).get(1).getPayload())
							.toString();
				} else if ((ilwisType & 512L) != 0
						&& inputData.get(inID).get(0) instanceof LiteralStringBinding) { // Coordinatesystem
																							// from
																							// String
					ilwisInputList[i] = new CoordinateSystem(inputData
							.get(inID).get(0).getPayload().toString())
							.toString();
				} else if (true) { // Literals
					ilwisInputList[i] = inputData.get(inID).get(0).getPayload()
							.toString();
				}
			}
			
			// Checks missing parameters
			for (String s : ilwisInputList) {
				if (s == null) {
					throw new RuntimeException("Missing parameter.");
				}
			}

			// Set the Ilwis working directory
			if (ilwisWorkingDir != null) {
				ilwisobjects.disconnectIssueLogger();
				Engine.setWorkingCatalog(ilwisWorkingDir);
				ilwisobjects.connectIssueLogger();
				LOGGER.info("Ilwis working dir: " + ilwisWorkingDir);
			} else {
				LOGGER.info("Ilwis working dir: none");
			}

			// Ilwis do function
			IObject ilwisResult = null;
			LOGGER.info("Engine do " + ilwisInputList.length);
			switch (ilwisInputList.length) {
			case 0:
				LOGGER.info("Ilwis do(" + "outputname" + ","
						+ ilwisProcess.getName() + ")");
				ilwisResult = Engine._do("outputname", ilwisProcess.getName());
				break;
			case 1:
				LOGGER.info("Ilwis do(" + "outputname" + ","
						+ ilwisProcess.getName() + "," + ilwisInputList[0]
						+ ")");
				ilwisResult = Engine._do("outputname", ilwisProcess.getName(),
						ilwisInputList[0]);
				break;
			case 2:
				LOGGER.info("Ilwis do(" + "outputname" + ","
						+ ilwisProcess.getName() + "," + ilwisInputList[0]
						+ "," + ilwisInputList[1] + ")");
				ilwisResult = Engine._do("outputname", ilwisProcess.getName(),
						ilwisInputList[0], ilwisInputList[1]);
				break;
			case 3:
				LOGGER.info("Ilwis do(" + "outputname" + ","
						+ ilwisProcess.getName() + "," + ilwisInputList[0]
						+ "," + ilwisInputList[1] + "," + ilwisInputList[2]
						+ ")");
				ilwisResult = Engine
						._do("outputname", ilwisProcess.getName(),
								ilwisInputList[0], ilwisInputList[1],
								ilwisInputList[2]);
				break;

			case 4:
				LOGGER.info("Ilwis do(" + "outputname" + ","
						+ ilwisProcess.getName() + "," + ilwisInputList[0]
						+ "," + ilwisInputList[1] + "," + ilwisInputList[2]
						+ "," + ilwisInputList[3] + ")");
				ilwisResult = Engine._do("outputname", ilwisProcess.getName(),
						ilwisInputList[0], ilwisInputList[1],
						ilwisInputList[2], ilwisInputList[3]);
				break;
			case 5:
				LOGGER.info("Ilwis do(" + "outputname" + ","
						+ ilwisProcess.getName() + "," + ilwisInputList[0]
						+ "," + ilwisInputList[1] + "," + ilwisInputList[2]
						+ "," + ilwisInputList[3] + "," + ilwisInputList[4]
						+ ")");
				ilwisResult = Engine
						._do("outputname", ilwisProcess.getName(),
								ilwisInputList[0], ilwisInputList[1],
								ilwisInputList[2], ilwisInputList[3],
								ilwisInputList[4]);
				break;
			case 6:
				LOGGER.info("Ilwis do(" + "outputname" + ","
						+ ilwisProcess.getName() + "," + ilwisInputList[0]
						+ "," + ilwisInputList[1] + "," + ilwisInputList[2]
						+ "," + ilwisInputList[3] + "," + ilwisInputList[4]
						+ "," + ilwisInputList[5] + ")");
				ilwisResult = Engine._do("outputname", ilwisProcess.getName(),
						ilwisInputList[0], ilwisInputList[1],
						ilwisInputList[2], ilwisInputList[3],
						ilwisInputList[4], ilwisInputList[5]);
				break;
			case 7:
				LOGGER.info("Ilwis do(" + "outputname" + ","
						+ ilwisProcess.getName() + "," + ilwisInputList[0]
						+ "," + ilwisInputList[1] + "," + ilwisInputList[2]
						+ "," + ilwisInputList[3] + "," + ilwisInputList[4]
						+ "," + ilwisInputList[5] + "," + ilwisInputList[6]
						+ ")");
				ilwisResult = Engine
						._do("outputname", ilwisProcess.getName(),
								ilwisInputList[0], ilwisInputList[1],
								ilwisInputList[2], ilwisInputList[3],
								ilwisInputList[4], ilwisInputList[5],
								ilwisInputList[6]);
				break;
			}
			LOGGER.info("Successful engine._do");

			// Store output
			long outtype = ilwisProcess.getPoutType(1);
			LOGGER.info("Output type: " + outtype);

			if ((outtype & 8) != 0) { // Raster
				RasterCoverage rasterResult = RasterCoverage
						.toRasterCoverage(ilwisResult);
				rasterResult.store("raster", "GTiff", "gdal");
				LOGGER.info("Storing file: " + ilwisWorkingDir + File.separator
						+ "raster.tif");
				File file = new File(ilwisWorkingDir + File.separator
						+ "raster.tif");

				GenericFileDataBinding result1 = new GenericFileDataBinding(
						new GenericFileData(file, "image/tiff"));
				LOGGER.info("Storing " + ilwisProcess.getPoutName(1) + " "
						+ result1.toString());
				result.put(ilwisProcess.getPoutName(1), result1);

				LOGGER.error("File storing error");

			} else if ((outtype & 4) != 0) { // Polygon, Shp
				FeatureCoverage featureResult = FeatureCoverage
						.toFeatureCoverage(ilwisResult);
				featureResult.store("polygon.shp", "ESRI Shapefile", "gdal");
				LOGGER.info("Storing file: " + ilwisWorkingDir + File.separator
						+ "polygon.shp");
				File file = new File(ilwisWorkingDir + File.separator
						+ "polygon.shp");

				GenericFileDataBinding result1 = new GenericFileDataBinding(
						new GenericFileData(file, "application/x-zipped-shp"));

				LOGGER.info("Storing " + ilwisProcess.getPoutName(1) + " "
						+ result1.toString());
				result.put(ilwisProcess.getPoutName(1), result1);
			}

		} catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException("Error while executing process " + processID + " : " + e.getMessage() );
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException("Error while executing process " + processID + " : " + e.getMessage() );
		}

		return result;
	}

	@Override
	public List<String> getErrors() {
		return errors;
	}

	@Override
	public ProcessDescriptionType getDescription() {
		return processDescription;
	}

	@Override
	public String getWellKnownName() {
		return "org.n52.wps.server.ilwis." + processID;
	}

	@Override
	public boolean processDescriptionIsValid() {
		return ilwisProcess.isValid();
	}

	@Override
	public Class<?> getInputDataType(String id) {
		int paramId = inputId.get(id);
		long type = ilwisProcess.getPinType(paramId);
		return getDataType(type);
	}

	@Override
	public Class<?> getOutputDataType(String id) {
		int paramId = outputId.get(id);
		long type = ilwisProcess.getPoutType(paramId);
		return getDataType(type);
	}

	private Class<?> getDataType(long type) {
		if ((type & 4) != 0)
			return GenericFileDataBinding.class; // Polygon, shp

		if ((type & 8) != 0)
			return GenericFileDataBinding.class; // Raster

		if ((type & 131072L) != 0)
			return GenericFileDataBinding.class; // Georef

		if ((type & 68719476736L) != 0)
			return LiteralStringBinding.class; // String

		if ((type & 17179869184L) != 0)
			return LiteralDoubleBinding.class; // Double

		if ((type & 4294967296L) != 0)
			return LiteralLongBinding.class; // Int64

		if ((type & 1073741824L) != 0)
			return LiteralIntBinding.class; // Int32

		if ((type & 268435456L) != 0)
			return LiteralShortBinding.class; // Int16

		if ((type & 549755813888L) != 0)
			return LiteralDoubleBinding.class; // Coordinate

		if ((type & 512L) != 0)
			return LiteralStringBinding.class; // (conventional)Coordinatesystem

		LOGGER.warn("Invalid Ilwis Datatype: " + type);
		return null; // Default
	}

	public GenericIlwisProcessDelegator(String processID,
			ProcessDescriptionType processDescriptionType, long ilwisID) {
		this.processID = processID.replace("org.n52.wps.server.ilwis.", "");

		this.processDescription = processDescriptionType;
		ilwisProcess = Engine.getOperationById(ilwisID);

		// Mapping inputs
		for (int i = 1; ilwisProcess.getPinType(i) != 0; i++) {
			inputId.put(ilwisProcess.getPinName(i).replace(",", ""), i);
		}

		// Mapping outputs
		for (int i = 1; ilwisProcess.getPoutType(i) != 0; i++) {
			outputId.put(ilwisProcess.getPoutName(i).replace(",", ""), i);
		}

	}
}
