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
 * ScanImageTiffReader is the file format reader 
 * for the ScanImage TIFF variant
 * @author Michael Pinkert
 *
 */

public class ScanImageTiffReader extends BaseTiffReader {
	// -- Constants --
	private static final String SCANIMAGE_MAGIC_STRING = "scanimage";
	private static final String METADATA_STRING = "Metadata.xml";
	private static final String[] XML_SUFFIX = {"xml"};
	
	// -- Fields --
	/**Helper reader for opening images */
	private TiffReader tiff;
	
	/** Pixel size*/
	private double physicalSizeX, physicalSizeY, physicalSizeZ;
	
	/** Microscope Zoom*/
	private Double zoom;
	
	/** Optional xml file */
	private Location xmlFile;
	
	/** Flag indicating that the reader is operating in a mode where grouping of files is
	 * disallowed.  This happens when there is no associated Z-stack acquisition
	 * for the selected file*/
	private boolean singleTiffMode;
	
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
		return false;
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
		return FormatTools.MUST_GROUP;
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
		if (singleTiffMode) return tiff.getSeriesUsedFiles(noPixels);
		
		//Holds all file names
		final ArrayList<String> usedFiles = new ArrayList<String>();

		//Add the optional metadata file to the used files list
		if (xmlFile != null) usedFiles.add(xmlFile.getAbsolutePath());
		
		if (!noPixels){
		}
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
	/**Finds the optional XML metadata file*/
	  private void findMetadataFiles() {
		    LOGGER.info("Finding metadata files");
		    if (xmlFile == null) xmlFile = find(XML_SUFFIX);
		  }

	  /** Finds the first file with one of the given suffixes. */
	  private Location find(final String[] suffix) {
	    final Location file = new Location(currentId).getAbsoluteFile();
	    final Location parent = file.getParentFile();
	    final String[] listing = parent.list();
	    for (final String name : listing) {
	      if (checkSuffix(name, suffix)) {
	        return new Location(parent, name);
	      }
	    }
	    return null;
	  }
	   
}
