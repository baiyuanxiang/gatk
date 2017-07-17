package org.broadinstitute.hellbender.tools.copynumber.legacy.coverage.denoising.rsvd;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.spark.api.java.JavaSparkContext;
import org.broadinstitute.hellbender.tools.exome.ReadCountCollection;
import org.broadinstitute.hellbender.utils.SimpleInterval;

import java.util.List;

/**
 * Interface for the panel of normals (PoN) for SVD-based coverage denoising.
 *
 * @author Samuel Lee &lt;slee@broadinstitute.org&gt;
 */
public interface SVDReadCountPanelOfNormals {
    /**
     * Returns the PoN version.
     */
    double getVersion();

    /**
     * Returns the number of eigensamples.
     */
    int getNumEigensamples();

    /**
     * Returns a modifiable copy of the original matrix of integer read-counts used to build the PoN
     * (no filtering will have been applied).  This matrix has has dimensions {@code M_original x N_original},
     * where {@code M_original} is the number of original intervals and {@code N_original} is the number of
     * original samples.
     */
    RealMatrix getOriginalReadCounts();

    /**
     * Returns a modifiable copy of the list of the original intervals that were used to build this PoN
     * (no filtering will have been applied).  This list has length {@code M_original}.
     */
    List<SimpleInterval> getOriginalIntervals();

    /**
     * Returns a modifiable copy of an array containing the GC content of the original intervals
     * (in the same order as in {@link #getOriginalIntervals()}).  This array has length {@code M_original}.
     */
    double[] getOriginalIntervalGCContent();

    /**
     * Returns a modifiable copy of the list of the intervals contained in this PoN after all filtering has been applied.
     * This list has length {@code M}.
     */
    List<SimpleInterval> getPanelIntervals();

    /**
     * Returns a modifiable copy of an array containing the median (across all samples, before filtering)
     * of the fractional coverage at each panel interval (in the same order as in {@link #getPanelIntervals()}).
     * This is used to standardize samples.  This array has length {@code M}.
     */
    double[] getPanelIntervalFractionalMedians();

    /**
     * Returns a modifiable copy of an array of the singular values of the eigensamples in decreasing order.
     * This array has length {@code K}.
     */
    double[] getSingularValues();

    /**
     * Returns a modifiable copy of an array containing the matrix of left-singular vectors.
     * This matrix has has dimensions {@code M x K},
     * where {@code M} is the number of panel intervals (after filtering)
     * and {@code K} is the number of eigensamples.
     * Columns are sorted by singular value in decreasing order.
     */
    double[][] getLeftSingular();

    /**
     * Returns a modifiable copy of an array contatining the pseudoinverse of the matrix of left-singular vectors
     * returned by {@link #getLeftSingular()}.
     * This matrix has dimensions {@code K x M},
     * where {@code K} is the number of eigensamples
     * and {@code M} is the number of panel intervals (after filtering).
     */
    double[][] getLeftSingularPseudoinverse();

    default SVDDenoisedCopyRatioResult denoise(final ReadCountCollection readCounts,
                                               final int numEigensamples,
                                               final JavaSparkContext ctx) {
        return SVDDenoisingUtils.denoise(this, readCounts, numEigensamples, ctx);
    }
}