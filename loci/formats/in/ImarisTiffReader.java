//
// ImarisTiffReader.java
//

/*
LOCI Bio-Formats package for reading and converting biological file formats.
Copyright (C) 2005-@year@ Melissa Linkert, Curtis Rueden, Chris Allan,
Eric Kjellman and Brian Loranger.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Library General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Library General Public License for more details.

You should have received a copy of the GNU Library General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.formats.in;

import java.io.*;
import java.util.*;
import loci.formats.*;
import loci.formats.meta.FilterMetadata;
import loci.formats.meta.MetadataStore;

/**
 * ImarisTiffReader is the file format reader for
 * Bitplane Imaris 3 files (TIFF variant).
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/loci/formats/in/ImarisTiffReader.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/loci/formats/in/ImarisTiffReader.java">SVN</a></dd></dl>
 *
 * @author Melissa Linkert linkert at wisc.edu
 */
public class ImarisTiffReader extends BaseTiffReader {

  // -- Constructor --

  /** Constructs a new Imaris TIFF reader. */
  public ImarisTiffReader() {
    super("Bitplane Imaris 3 (TIFF)", "ims");
    blockCheckLen = 1024;
    suffixSufficient = false;
  }

  // -- Internal FormatReader API methods --

  /* @see loci.formats.FormatReader#initFile(String) */
  protected void initFile(String id) throws FormatException, IOException {
    if (debug) debug("ImarisTiffReader.initFile(" + id + ")");
    super.initFile(id);

    in = new RandomAccessStream(id);
    if (in.readShort() == 0x4949) in.order(true);

    ifds = TiffTools.getIFDs(in);
    if (ifds == null) throw new FormatException("No IFDs found");

    // hack up the IFDs
    //
    // Imaris TIFFs store a thumbnail in the first IFD; each of the remaining
    // IFDs defines a stack of tiled planes.

    status("Verifying IFD sanity");

    Vector tmp = new Vector();

    for (int i=1; i<ifds.length; i++) {
      long[] byteCounts = TiffTools.getIFDLongArray(ifds[i],
        TiffTools.TILE_BYTE_COUNTS, false);
      long[] offsets = TiffTools.getIFDLongArray(ifds[i],
        TiffTools.TILE_OFFSETS, false);

      for (int j=0; j<byteCounts.length; j++) {
        Hashtable t = (Hashtable) ifds[i].clone();
        TiffTools.putIFDValue(t, TiffTools.TILE_BYTE_COUNTS, byteCounts[j]);
        TiffTools.putIFDValue(t, TiffTools.TILE_OFFSETS, offsets[j]);
        tmp.add(t);
      }
    }

    String comment = TiffTools.getComment(ifds[0]);

    status("Populating metadata");

    core.sizeC[0] = ifds.length - 1;
    core.sizeZ[0] = tmp.size() / core.sizeC[0];
    core.sizeT[0] = 1;
    core.sizeX[0] = (int) TiffTools.getImageWidth(ifds[1]);
    core.sizeY[0] = (int) TiffTools.getImageLength(ifds[1]);

    ifds = (Hashtable[]) tmp.toArray(new Hashtable[0]);
    core.imageCount[0] = core.sizeC[0] * core.sizeZ[0];
    core.currentOrder[0] = "XYZCT";
    core.interleaved[0] = false;
    core.rgb[0] =
      core.imageCount[0] != core.sizeZ[0] * core.sizeC[0] * core.sizeT[0];

    int bitsPerSample = TiffTools.getIFDIntValue(ifds[0],
      TiffTools.BITS_PER_SAMPLE);
    int bitFormat = TiffTools.getIFDIntValue(ifds[0], TiffTools.SAMPLE_FORMAT);

    while (bitsPerSample % 8 != 0) bitsPerSample++;
    if (bitsPerSample == 24 || bitsPerSample == 48) bitsPerSample /= 3;

    boolean signed = bitFormat == 2;

    if (bitFormat == 3) core.pixelType[0] = FormatTools.FLOAT;
    else {
      switch (bitsPerSample) {
        case 8:
          core.pixelType[0] = signed ? FormatTools.INT8 : FormatTools.UINT8;
          break;
        case 16:
          core.pixelType[0] = signed ? FormatTools.INT16 : FormatTools.UINT16;
          break;
        case 32:
          core.pixelType[0] = signed ? FormatTools.INT32: FormatTools.UINT32;
          break;
      }
    }

    status("Parsing comment");

    // likely an INI-style comment, although we can't be sure

    MetadataStore store =
      new FilterMetadata(getMetadataStore(), isMetadataFiltered());
    int[] channelIndexes = new int[3];

    if (comment != null && comment.startsWith("[")) {
      // parse key/value pairs
      StringTokenizer st = new StringTokenizer(comment, "\n");
      while (st.hasMoreTokens()) {
        String line = st.nextToken();
        int equals = line.indexOf("=");
        if (equals < 0) continue;
        String key = line.substring(0, equals).trim();
        String value = line.substring(equals + 1).trim();
        addMeta(key, value);

        if (key.equals("Description")) {
          store.setImageDescription(value, 0);
        }
        else if (key.equals("LSMEmissionWavelength") && !value.equals("0")) {
          store.setLogicalChannelEmWave(new Integer(value), 0,
            channelIndexes[1]++);
        }
        else if (key.equals("LSMExcitationWavelength") && !value.equals("0")) {
          store.setLogicalChannelExWave(new Integer(value), 0,
            channelIndexes[2]++);
        }
        else if (key.equals("Name") && !currentId.endsWith(value)) {
          store.setLogicalChannelName(value, 0, channelIndexes[0]++);
        }
        else if (key.equals("RecordingDate")) {
          value = value.replaceAll(" ", "T");
          store.setImageCreationDate(value.substring(0, value.indexOf(".")), 0);
        }
      }
      metadata.remove("Comment");
    }

    store.setImageName("", 0);
    MetadataTools.populatePixels(store, this);
  }

}
