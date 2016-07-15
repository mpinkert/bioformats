package loci.formats.in;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import loci.common.DateTools;
import loci.common.Location;
import loci.common.RandomAccessInputStream;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.meta.MetadataStore;
import loci.formats.tiff.TiffParser;


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
	
	/** The list of tif files that should be present based on the metadata */
	private String[] tifList;
	
	/** Optional xml file */
	private Location xmlFile;
	
	/** Flag indicating that the reader is operating in a mode where grouping of files is
	 * disallowed.  This happens when there is no associated Z-stack acquisition
	 * for the selected file or associated xml file*/
	private boolean singleTiffMode;
	//TODO Ask Bioformats whether singleTiffMode is necessary or if we should be relying upon CAN_GROUP.
	
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
		if (singleTiffMode && xmlFile == null) return true;
		else return false;
	}	  
//	  
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
	/*@Override
	public String[] getSeriesUsedFiles(boolean noPixels) {
		FormatTools.assertId(currentId, true, 1);
		if (singleTiffMode) return tiff.getSeriesUsedFiles(noPixels);
		
		//Holds all file names
		final ArrayList<String> usedFiles = new ArrayList<String>();

		//Add the optional metadata file to the used files list
		if (xmlFile != null) usedFiles.add(xmlFile.getAbsolutePath());
		
		
	}*/

	// -- Internal BaseTiffReader API methods --

	/* @see BaseTiffReader#initStandardMetadata() */
	@Override
	protected void initStandardMetadata() throws FormatException, IOException {
		super.initStandardMetadata();
		
		// parse key/value pairs in the comment
	    String comment = ifds.get(0).getComment();
	    
	    //Dimension values
	    String tz = null, tc = null, tt = null;
	    
	    
	    if (comment != null){
	    	String[] lines = comment.split("\n");
	    	for (String line : lines){
	    		int equals = line.indexOf("=");
	            if (equals < 0) continue;
	            String key = line.substring(0, equals-1);
	            String value = line.substring(equals + 2);
	            addGlobalMeta(key, value);
	            
	            
	            
	            //find the Z, C, and T dimension info
	            if (key.equals("scanimage.SI.hChannels.channelDisplay")){
	            	int count = 1;
	            	for (int it = 0; it < value.length(); it++)
	            	{
	            		if (value.charAt(it) == ';') count++;
	            	}
	            	tc = Integer.toString(count);
	            }
	            else if (key.equals("scanimage.SI.acqsPerLoop")) tt = value;
	            else if(key.equals("scanimage.SI.hStackManager.numSlices")) tz = value;
	            
	    	}
	    }
	    CoreMetadata m = core.get(0);

	    m.sizeT = 1;
	    if (getSizeZ() == 0) m.sizeZ = 1;
	    if (getSizeC() == 0) m.sizeC = 1;

	    
	    if (tz != null) m.sizeZ = Integer.parseInt(tz);
	    if (tt != null) m.sizeT = Integer.parseInt(tt);
	    if (tc != null) m.sizeC = Integer.parseInt(tc);
	    
	    
	    //Calculate the number of images for this data set based on those parameters
	    m.imageCount = getSizeZ() * getSizeT();
	    
	    //Judge whether this is a single image, or a series of images
	    if (getImageCount() == 1) singleTiffMode = true;
	    else singleTiffMode = false;
	    
	    
	    //look for other TIFF files that belong to this dataset
	    if (!singleTiffMode){
	    	Location file = new Location(currentId);
	    	String fileName = file.getName();
	    	//Separation between the user named prefix and the program named suffix
	    	int fixSeparator = fileName.lastIndexOf('_');
	    	if (!(fixSeparator > 0))
	    	{
	    		warnFileName();
	    	}
	    	else{
	    		String prefix = fileName.substring(0, fileName.lastIndexOf('_'));
	    	
	    		String suffix = fileName.substring(fileName.lastIndexOf('_')+1, fileName.lastIndexOf('.'));
	    	
	    		//check that the suffix is of the appropriate format
	    		
	    		//Checking that the suffix is correct according to the metadata
	    		int xyIdx = Integer.parseInt(getGlobalMeta("scanimage.SI.hCycleManager.cycleIterIdxDone").toString());
	    		int slices = Integer.parseInt(getGlobalMeta("scanimage.SI.hStackManager.numSlices").toString());
	    		int acqNum = Integer.parseInt(getGlobalMeta("acquisitionNumbers").toString());

	    		int expectedSuffix = xyIdx*slices + acqNum;
	    		if (!(Integer.parseInt(suffix) == expectedSuffix)){
	    			warnSuffix(suffix, expectedSuffix);
	    			System.out.println("The file suffix, #{}, does not match the expected suffix, #{}", suffix, expectedSuffix");
	    			//We can still read the file as a single tiff
	    			singleTiffMode = true;
	    			
	    		}
	    		else{
	    			String [] list = getSeriesUsedFiles();
	    		}
	    	}
	    }
	    
	}

	/* @see BaseTiffReader#initMetadataStore() */
	@Override
	protected void initMetadataStore() throws FormatException {
		super.initMetadataStore();
	    MetadataStore store = makeFilterMetadata();
	    MetadataTools.populatePixels(store, this);
	}

	// -- Internal FormatReader API methods --

	@Override
	protected void initFile(String id) throws FormatException, IOException {
		super.initFile(id);

		//Check for a metadata file.
		findMetadataFile();

		//Extract the metadata and any additional file locations
		initStandardMetadata(); //Original metadata, all key value pairs
		
		//Put the metadata into the bioformats store
		initMetadataStore(); //Core metadata
	}

	
	// -- Helper methods --
	/**Finds the optional XML metadata file*/
	private void findMetadataFile() {
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

	/** Gets the sequence associated with the file*/
	//private String[] sequence()
	

	/** Emits a warning about a missing {@code <File>}. */
	private void warnFile(String missingFile){
		LOGGER.warn("The file ", missingFile, " does not exist.");
	}
	
	/** Emits a warning about the file having an incorrect file name */
	private void warnFileName(){
		LOGGER.warn("The file name is not of the appropriate format.  The file should be named (Prefix)_(Suffix).tif,  Where the suffix is an integer determined by ScanImage parameters.");
	}
	
	/** Emits a warning about the file suffix not matching the expected value */
	private void warnSuffix(String suffix, int expectedSuffix){
		LOGGER.warn("The file suffix, #{}, does not match the expected suffix, #{}", suffix, expectedSuffix);
	}
	
	/** Emits a warning about a mismatch between the number of channels in the metadata and the file*/
	private void warnChannels(int metaChannels, int actualChannels){
		LOGGER.warn("The metadata claims the file should have #{} channels, but only #{} are present", metaChannels, actualChannels);
	}
	
	
	/** Emits a warning */
	
	
	public static void main(String... args) throws FormatException, IOException{
		

		
		//Paths for PC and for MAC
		String path = "C:/Users/mpinkert/Documents/SampleImages/cycletest/test.tif";
		//String path = "/Users/Pinkert/Documents/SampleImages/cycletest/test.tif"; 
		

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


