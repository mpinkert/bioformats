package loci.formats.in;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import loci.common.DataTools;
import loci.common.Location;
import loci.common.RandomAccessInputStream;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.meta.MetadataStore;
import loci.formats.tiff.TiffParser;

import ome.xml.model.primitives.PositiveFloat;

import ome.units.quantity.Length;
import ome.units.quantity.Time;
import ome.units.UNITS;

/**
 * ScanImageTiffReader is the file format reader for ScanImage tiff files
 * @author Pinkert
 *
 */

public class ScanImageTiffReader extends BaseTiffReader {
	// -- Constants --
	private static final String SCANIMAGE_MAGIC_STRING = "scanimage";
	private static final String METADATA_STRING = "Metadata.xml";
	
	// -- Fields --
	
	private double physicalSizeY, physicalSizeZ;
	private double physicalSizeX;

	private Double zoom;
	
	// -- Constructor --

	public ScanImageTiffReader() {
		super("ScanImage", new String[] {"tif", "tiff", "xml"});
		suffixSufficient = false;
		domains = new String[] {FormatTools.LM_DOMAIN};
		hasCompanionFiles = true;
		datasetDescription = "One or multiple .tiff files corresponding to a Z-stack and possibly one .txt to acquire pixel size data";
	}

	// -- IFormatReader API methods --
	/* @see loci.formats.IFormatReader#isSingleFile(String) */
	@Override
	public boolean isSingleFile(String id) throws FormatException, IOException {
		/* Check if there is an additional metadata file
		This file is NOT a part of the native scan image, but must be added to provide pixel size
	    This checks for a file name that includes Metadata.xml or that is based on the name of the
	    acquisition
	    */
		Location file = new Location(id).getAbsoluteFile();
	    String name = file.getName();
	    Location parent = file.getParentFile(); //Parent directory
	    String acquisition = name.substring(0, name.lastIndexOf("_")-1);
	    
	    String[] list = parent.list();
	    for (int i=0; i<list.length; i++)
	    {
	    	/*Checking for the generic metadata files*/
	    	if (list[i].contains(METADATA_STRING)) return true;
	    	
	    	
	    	/*Checking for acquisition specific metadata file*/
	    	if(list[i] == acquisition.concat(".xml")) return true;
	    }
	    
	}
	  
	  
	/* @see loci.formats.IFormatReader#isThisType(RandomAccessInputStream) */
	@Override
	public boolean isThisType(RandomAccessInputStream stream) throws IOException {
	    TiffParser tp = new TiffParser(stream);
	    String comment = tp.getComment();
	    if (comment==null) return false;
	    return comment.indexOf(SCANIMAGE_MAGIC_STRING) >= 0;
	}
	

	  /* @see loci.formats.IFormatReader#fileGroupOption(String) */
	  @Override
	  public int fileGroupOption(String id) throws FormatException, IOException {
	    return FormatTools.CAN_GROUP;
	  }
	  
	/* @see loci.formats.IFormatReader#close(boolean) */
	@Override
	public void close(boolean fileOnly) throws IOException {		  
		super.close(fileOnly);
	    if (!fileOnly) {
	      physicalSizeX = physicalSizeY = physicalSizeZ = 0;
	      zoom = null;
	    }
	}

	  /* @see loci.formats.IFormatReader#getSeriesUsedFiles(boolean) */
	  @Override
	  public String[] getSeriesUsedFiles(boolean noPixels) {
		    FormatTools.assertId(currentId, true, 1);
		    
	  }
	  
	// -- Internal BaseTiffReader API methods --

	/* @see BaseTiffReader#initStandardMetadata() */
	@Override
	protected void initStandardMetadata() throws FormatException, IOException {
	}

	/* @see BaseTiffReader#initMetadataStore() */
	@Override
	protected void initMetadataStore() throws FormatException {
	}

	// -- Helper methods --

}
