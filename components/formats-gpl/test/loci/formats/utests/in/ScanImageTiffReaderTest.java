/*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
 * %%
 * Copyright (C) 2005 - 2016 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package loci.formats.utests.in;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import loci.common.xml.XMLTools;
import loci.formats.FormatException;
import loci.formats.in.ScanImageTiffReader;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Unit tests for {@link PrairieMetadata}.
 *
 * @author Curtis Rueden
 */
public class ScanImageTiffReaderTest {

	public static void main(String... args) throws FormatException, IOException{
		

		
		//Paths for PC and for MAC
		//String path = "C:/Users/mpinkert/Documents/SampleImages/multi-channel.ome.tif";
		String path = "C:/Users/mpinkert/Documents/SampleImages/cycletest/position_xyz_5.tif";
		//String path = "/Users/Pinkert/Documents/SampleImages/cycletest/position_xyz_5.tif"; 
		

		//Can change ScanImage to just TiffReader as a sanity check
		ScanImageTiffReader r = new ScanImageTiffReader();
		
		boolean match = 
		r.isThisType(path);
		System.out.println("Match = " + match);
		
		r.setId(path);
		
		System.out.println("sizeC = " + r.getSizeC());
		System.out.println("sizeX = " + r.getSizeX());
		System.out.println("sizeY = " + r.getSizeY());
		System.out.println("sizeZ = " + r.getSizeZ());
		System.out.println("sizeT = " + r.getSizeT());
		r.close();
	}
}
