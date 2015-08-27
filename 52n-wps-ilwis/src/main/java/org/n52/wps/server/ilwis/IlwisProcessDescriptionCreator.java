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

import java.math.BigInteger;

import net.opengis.ows.x11.DomainMetadataType;
import net.opengis.ows.x11.RangeType;
import net.opengis.wps.x100.ComplexDataCombinationsType;
import net.opengis.wps.x100.ComplexDataDescriptionType;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.LiteralInputType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType.DataInputs;
import net.opengis.wps.x100.ProcessDescriptionType.ProcessOutputs;
import net.opengis.wps.x100.SupportedComplexDataInputType;
import net.opengis.wps.x100.SupportedComplexDataType;

import org.n52.ilwis.java.IlwisOperation;
import org.n52.wps.io.IOHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IlwisProcessDescriptionCreator {
	
	private static Logger LOGGER = LoggerFactory
			.getLogger(GenericIlwisProcessDelegator.class);

	public ProcessDescriptionType createDescribeProcessType(IlwisOperation algorithm) throws UnsupportedGeoAlgorithmException{

			ProcessDescriptionType pdt = ProcessDescriptionType.Factory.newInstance();
			pdt.setStatusSupported(true);
			pdt.setStoreSupported(true);
			
			pdt.addNewAbstract().setStringValue(algorithm.getName());
			pdt.addNewTitle().setStringValue(algorithm.getLongname());
			pdt.addNewIdentifier().setStringValue("org.n52.wps.server.ilwis." + algorithm.getName());
			pdt.setProcessVersion("1.0.0");
			
			// Inputs
			DataInputs inputs = pdt.addNewDataInputs();
			for (int i = 1; algorithm.getPinType(i) != 0; i++) {
				addParameter(inputs, algorithm, i);
			}

			// Outputs
			ProcessOutputs outputs = pdt.addNewProcessOutputs();
			for (int i = 1; algorithm.getPoutType(i) != 0; i++) {
				addOutput(outputs, algorithm, i);
			}

			return pdt;
	}

	private void addOutput(ProcessOutputs outputs, IlwisOperation ilwisOperaton, int index) {
		OutputDescriptionType output = outputs.addNewOutput();
		output.addNewAbstract().setStringValue(ilwisOperaton.getPoutDesc(index));
		output.addNewIdentifier().setStringValue(ilwisOperaton.getPoutName(index));
		output.addNewTitle().setStringValue(ilwisOperaton.getPoutName(index));
		
		long type = ilwisOperaton.getPoutType(index);
		LOGGER.info("output type -> " + type);
		
		if ((type & 4L)!= 0) { // Shp
			SupportedComplexDataType complexOutput = output.addNewComplexOutput();
			ComplexDataCombinationsType supported = complexOutput.addNewSupported();
			ComplexDataDescriptionType format = supported.addNewFormat();
			format.setMimeType("application/x-zipped-shp");
			
			ComplexDataDescriptionType defaultFormat = complexOutput.addNewDefault().addNewFormat();
			defaultFormat.setMimeType("application/x-zipped-shp");
		} else if (true){ // Raster
			SupportedComplexDataType complexOutput = output.addNewComplexOutput();
			ComplexDataCombinationsType supported = complexOutput.addNewSupported();
			ComplexDataDescriptionType format = supported.addNewFormat();
			format.setMimeType("image/tiff");
			
			
//			ComplexDataDescriptionType format2 = supported.addNewFormat();
//			format2.setMimeType("image/jpeg");	
//			ComplexDataDescriptionType format3 = supported.addNewFormat();
//			format3.setMimeType("image/png");
			
			ComplexDataDescriptionType defaultFormat = complexOutput.addNewDefault().addNewFormat();
			defaultFormat.setMimeType("image/tiff");
		}
	}

	private void addParameter(DataInputs inputs, IlwisOperation ilwisOperaton, int index) throws UnsupportedGeoAlgorithmException {
		InputDescriptionType input = inputs.addNewInput();
		input.addNewAbstract().setStringValue(ilwisOperaton.getPinDesc(index));
		input.addNewTitle().setStringValue(ilwisOperaton.getPinName(index));
		input.addNewIdentifier().setStringValue( ilwisOperaton.getPinName(index).replace(",", "") );
		
		long type = ilwisOperaton.getPinType(index);
		
		if ((type & 17179869184L) != 0){ // Double
			LiteralInputType literal = input.addNewLiteralData();
			DomainMetadataType dataType = literal.addNewDataType();
			dataType.setReference("xs:double");
			literal.setDataType(dataType);
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			RangeType range = literal.addNewAllowedValues().addNewRange();
			range.addNewMaximumValue().setStringValue("" + Double.POSITIVE_INFINITY);
			range.addNewMinimumValue().setStringValue("" + Double.NEGATIVE_INFINITY);
//			literal.setDefaultValue(Double.toString(ai.getDefaultValue()));
		}
		if ((type & 4294967296L) != 0){ // Long, int64
			LiteralInputType literal = input.addNewLiteralData();
			DomainMetadataType dataType = literal.addNewDataType();
			dataType.setReference("xs:long");
			literal.setDataType(dataType);
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			RangeType range = literal.addNewAllowedValues().addNewRange();
			range.addNewMaximumValue().setStringValue("" + Long.MAX_VALUE);
			range.addNewMinimumValue().setStringValue("" + Long.MIN_VALUE);
		}
		if ((type & 1073741824L) != 0){ // Int, int32
			LiteralInputType literal = input.addNewLiteralData();
			DomainMetadataType dataType = literal.addNewDataType();
			dataType.setReference("xs:int");
			literal.setDataType(dataType);
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			RangeType range = literal.addNewAllowedValues().addNewRange();
			range.addNewMaximumValue().setStringValue("" + Integer.MAX_VALUE);
			range.addNewMinimumValue().setStringValue("" + Integer.MIN_VALUE);
		}
		if ((type & 268435456L) != 0){ // Short, int16
			LiteralInputType literal = input.addNewLiteralData();
			DomainMetadataType dataType = literal.addNewDataType();
			dataType.setReference("xs:short");
			literal.setDataType(dataType);
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			RangeType range = literal.addNewAllowedValues().addNewRange();
			range.addNewMaximumValue().setStringValue("" + Short.MAX_VALUE);
			range.addNewMinimumValue().setStringValue("" + Short.MIN_VALUE);
		}
		if ((type & 536870912L) != 0){ // Long, uint32
			LiteralInputType literal = input.addNewLiteralData();
			DomainMetadataType dataType = literal.addNewDataType();
			dataType.setReference("xs:long");
			literal.setDataType(dataType);
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			RangeType range = literal.addNewAllowedValues().addNewRange();
			range.addNewMaximumValue().setStringValue("" + 4294967295L);
			range.addNewMinimumValue().setStringValue("" + 0);
		}
		if ((type & 134217728L) != 0){ // Int, uint16
			LiteralInputType literal = input.addNewLiteralData();
			DomainMetadataType dataType = literal.addNewDataType();
			dataType.setReference("xs:int");
			literal.setDataType(dataType);
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			RangeType range = literal.addNewAllowedValues().addNewRange();
			range.addNewMaximumValue().setStringValue("" + 65535);
			range.addNewMinimumValue().setStringValue("" + 0);
		}
		if ((type & 16777216L) != 0){ // Boolean
			LiteralInputType literal = input.addNewLiteralData();
			input.setMinOccurs(BigInteger.valueOf(1));
			DomainMetadataType dataType = literal.addNewDataType();
			dataType.setReference("xs:boolean");
			literal.setDataType(dataType);
			literal.addNewAnyValue();
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			literal.setDefaultValue("false");
		}
		if ((type & 68719476736L) != 0){ // String
			LiteralInputType literal = input.addNewLiteralData();
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			literal.addNewAnyValue();
			DomainMetadataType dataType = literal.addNewDataType();
			dataType.setReference("xs:string");
			literal.setDataType(dataType);
		}
		if ((type & 8) != 0) { // Raster from tif
			SupportedComplexDataInputType complex = input.addNewComplexData();
			ComplexDataCombinationsType supported = complex.addNewSupported();
			ComplexDataDescriptionType format = supported.addNewFormat();
			format.setMimeType("image/tiff");
			format.setEncoding(IOHandler.ENCODING_BASE64);
			ComplexDataDescriptionType defaultFormat = complex.addNewDefault().addNewFormat();
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			
			defaultFormat.setMimeType("image/tiff");
			defaultFormat.setEncoding(IOHandler.ENCODING_BASE64);
		}
		if ((type & 131072L) != 0) { // Georef from .grf
			SupportedComplexDataInputType complex = input.addNewComplexData();
			ComplexDataCombinationsType supported = complex.addNewSupported();
			ComplexDataDescriptionType format = supported.addNewFormat();
			format.setMimeType("application/vnd.ilwis.grf");
			ComplexDataDescriptionType defaultFormat = complex.addNewDefault().addNewFormat();
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			
			defaultFormat.setMimeType("application/vnd.ilwis.grf");
		}
		if ((type & 549755813888L) != 0) { // Coordinate
			LiteralInputType literal = input.addNewLiteralData();
			DomainMetadataType dataType = literal.addNewDataType();
			dataType.setReference("xs:double");
			literal.setDataType(dataType);
			input.setMinOccurs(BigInteger.valueOf(2));
			input.setMaxOccurs(BigInteger.valueOf(2));
			RangeType range = literal.addNewAllowedValues().addNewRange();
			range.addNewMaximumValue().setStringValue("" + Double.POSITIVE_INFINITY);
			range.addNewMinimumValue().setStringValue("" + Double.NEGATIVE_INFINITY);
		}
		if ((type & 512L) != 0) { // (conventional)coordinatesystem
			LiteralInputType literal = input.addNewLiteralData();
			input.setMinOccurs(BigInteger.valueOf(1));
			input.setMaxOccurs(BigInteger.valueOf(1));
			literal.addNewAnyValue();
			DomainMetadataType dataType = literal.addNewDataType();
			dataType.setReference("xs:string");
			literal.setDataType(dataType);
		}

	}

		
	//This class is thrown when there is any problem creating the XML
	//WPS file from a geoalgorithm, due to some yet unsupported feature
	//or parameter
	public class UnsupportedGeoAlgorithmException extends Exception{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1017100163300095362L;

	}

}


