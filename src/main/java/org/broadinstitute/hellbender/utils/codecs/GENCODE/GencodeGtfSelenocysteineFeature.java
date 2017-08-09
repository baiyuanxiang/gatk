package org.broadinstitute.hellbender.utils.codecs.GENCODE;

import java.util.ArrayList;
import java.util.List;

/**
 * A Gencode GTF Feature representing a selenocysteine.
 *
 * A GTF Feature represents one row of a GTF File.
 * The specification of a GTF file is defined here:
 * http://mblab.wustl.edu/GTF22.html
 *
 * Created by jonn on 7/25/17.
 */
final public class GencodeGtfSelenocysteineFeature extends GencodeGtfFeature {

    private GencodeGtfSelenocysteineFeature(String[] gtfFields) {
        super(gtfFields);
    }

    public static GencodeGtfFeature create(String[] gtfFields) {
        return new GencodeGtfSelenocysteineFeature(gtfFields);
    }

    private GencodeGtfSelenocysteineFeature(final GencodeGtfFeatureBaseData baseData) {
        super(baseData);
    }

    public static GencodeGtfFeature create(final GencodeGtfFeatureBaseData baseData) {
        return new GencodeGtfSelenocysteineFeature(baseData);
    }
}
