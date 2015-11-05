/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itextpdf.basics.font.otf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * LookupType 4: Ligature Substitution Subtable
 * @author psoares
 */
public class GsubLookupFormat4 extends OpenTableLookup {
    /**
     * The key is the first character. The first element in the int array is the
     * output ligature
     */
    private HashMap<Integer,List<int[]>> ligatures;
    
    public GsubLookupFormat4(OpenTypeFontTableReader openReader, int lookupFlag, int[] subTableLocations) throws IOException {
        super(openReader, lookupFlag, subTableLocations);
        ligatures = new HashMap<>();
        readSubTables();
    }
    
    @Override
    public boolean transformOne(GlyphLine line) {
        //TODO >
        if (line.idx >= line.end)
            return false;
        boolean changed = false;
        Glyph g = line.glyphs.get(line.idx);
        boolean match = false;
        if (ligatures.containsKey(g.index) && !openReader.IsSkip(g.index, lookupFlag)) {
            GlyphIndexer gidx = new GlyphIndexer();
            gidx.line = line;
            List<int[]> ligs = ligatures.get(g.index);
            for (int[] lig : ligs) {
                match = true;
                gidx.idx = line.idx;
                for (int j = 1; j < lig.length; ++j) {
                    NextGlyph(gidx);
                    if (gidx.glyph == null || gidx.glyph.index != lig[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    String composed = g.chars;
                    gidx.idx = line.idx;
                    boolean isMark = true;
                    for (int j = 1; j < lig.length; ++j) {
                        NextGlyph(gidx);
                        composed += gidx.glyph.chars;
                        isMark &= gidx.glyph.IsMark;
                        line.glyphs.remove(gidx.idx--);
                    }
                    Integer c = openReader.getGlyphToCharacter(lig[0]);
                    Glyph glyph = new Glyph(lig[0], openReader.getGlyphWidth(lig[0]), c, c == null ? composed : String.valueOf((char) (int) c), isMark);
                    line.glyphs.set(line.idx, glyph);
                    line.end -= lig.length - 1;
                    break;
                }
            }
        }
        if (match) {
            changed = true;
        }
        line.idx++;
        return changed;
    }

    @Override
    protected void readSubTable(int subTableLocation) throws IOException {
        openReader.rf.seek(subTableLocation);
        openReader.rf.readShort(); //subformat - always 1
        int coverage = openReader.rf.readUnsignedShort() + subTableLocation;
        int ligSetCount = openReader.rf.readUnsignedShort();
        int[] ligatureSet = new int[ligSetCount];
        for (int k = 0; k < ligSetCount; ++k) {
            ligatureSet[k] = openReader.rf.readUnsignedShort() + subTableLocation;
        }
        List<Integer> coverageGlyphIds = openReader.readCoverageFormat(coverage);
        for (int k = 0; k < ligSetCount; ++k) {
            openReader.rf.seek(ligatureSet[k]);
            int ligatureCount = openReader.rf.readUnsignedShort();
            int[] ligature = new int[ligatureCount];
            for (int j = 0; j < ligatureCount; ++j) {
                ligature[j] = openReader.rf.readUnsignedShort() + ligatureSet[k];
            }
            ArrayList<int[]> components = new ArrayList<>(ligatureCount);
            for (int j = 0; j < ligatureCount; ++j) {
                openReader.rf.seek(ligature[j]);
                int ligGlyph = openReader.rf.readUnsignedShort();
                int compCount = openReader.rf.readUnsignedShort();
                int[] component = new int[compCount];
                component[0] = ligGlyph;
                for (int i = 1; i < compCount; ++i) {
                    component[i] = openReader.rf.readUnsignedShort();
                }
                components.add(component);
            }
            ligatures.put(coverageGlyphIds.get(k), components);
        }
    }    
}
