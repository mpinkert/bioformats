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
	
	// -- Fields --
	
	private double physicalSizeX, physicalSizeY, physicalSizeZ;
	private Double zoom;
	
	// -- Constructor --

	public ScanImageTiffReader() {
		super("ScanImage TIFF", new String[] {"tif", "tiff"});
		suffixSufficient = false;
		domains = new String[] {FormatTools.LM_DOMAIN};
	}

	// -- IFormatReader API methods --

	/* @see loci.formats.IFormatReader#isThisType(RandomAccessInputStream) */
	@Override
	public boolean isThisType(RandomAccessInputStream stream) throws IOException {
	    TiffParser tp = new TiffParser(stream);

	
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
