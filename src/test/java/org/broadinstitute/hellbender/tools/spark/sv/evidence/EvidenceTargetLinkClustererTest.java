package org.broadinstitute.hellbender.tools.spark.sv.evidence;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.tools.spark.sv.utils.SVInterval;
import org.broadinstitute.hellbender.tools.spark.utils.IntHistogram;
import org.broadinstitute.hellbender.utils.IntHistogramTest;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class EvidenceTargetLinkClustererTest {
    private static final SAMFileHeader artificialSamHeader =
            ArtificialReadUtils.createArtificialSamHeaderWithGroups(2, 1, 1000000, 1);
    private static final ReadMetadata readMetadata = initMetadata();


    // todo: simplify; this was copied from BreakpointDensityFilterTest and we don't need all this prob
    private static ReadMetadata initMetadata() {
        final ReadMetadata.PartitionBounds[] partitionBounds = new ReadMetadata.PartitionBounds[3];
        partitionBounds[0] = new ReadMetadata.PartitionBounds(0, 1, 0, 10000);
        partitionBounds[1] = new ReadMetadata.PartitionBounds(0, 10001, 0, 20000);
        partitionBounds[2] = new ReadMetadata.PartitionBounds(0, 20001, 0, 30000);
        return new ReadMetadata(Collections.emptySet(), artificialSamHeader,
                new LibraryStatistics(new IntHistogram.CDF(IntHistogramTest.genLogNormalSample(350, 40, 10000)),
                        60000000000L, 600000000L, 3000000000L),
                partitionBounds, 100, 10, 30);
    }

    @DataProvider(name = "evidence")
    public Object[][] createTestData() {

        final List<BreakpointEvidence> evidenceList = new ArrayList<>();
        final List<GATKRead> pair1 = ArtificialReadUtils.createPair(artificialSamHeader, "pair1", 151, 1250, 500000, true, false);
        evidenceList.add(new BreakpointEvidence.WeirdTemplateSize(pair1.get(0), readMetadata));

        final List<GATKRead> pair2 = ArtificialReadUtils.createPair(artificialSamHeader, "pair2", 151, 1275, 600000, true, false);
        evidenceList.add(new BreakpointEvidence.WeirdTemplateSize(pair2.get(0), readMetadata));

        final List<GATKRead> pair3 = ArtificialReadUtils.createPair(artificialSamHeader, "pair3", 151, 1300, 500025, true, false);
        evidenceList.add(new BreakpointEvidence.WeirdTemplateSize(pair3.get(0), readMetadata));

        final List<EvidenceTargetLink> linkList = new ArrayList<>();
        linkList.add(new EvidenceTargetLink(
                new SVInterval(readMetadata.getContigID(pair1.get(0).getContig()),
                        1275 + pair2.get(0).getLength(),
                        1275 + readMetadata.getFragmentLengthStatistics(pair2.get(0).getReadGroup()).getMaxNonOutlierFragmentSize()),
                true,
                new SVInterval(readMetadata.getContigID(pair1.get(1).getContig()),
                        600000 - readMetadata.getFragmentLengthStatistics(pair1.get(0).getReadGroup()).getMaxNonOutlierFragmentSize() + 151,
                        600000 + BreakpointEvidence.DiscordantReadPairEvidence.MATE_ALIGNMENT_LENGTH_UNCERTAINTY),
                false, 0, 1));

        linkList.add(new EvidenceTargetLink(
                new SVInterval(readMetadata.getContigID(pair1.get(0).getContig()),
                        1300 + pair3.get(0).getLength(),
                        1250 + readMetadata.getFragmentLengthStatistics(pair2.get(0).getReadGroup()).getMaxNonOutlierFragmentSize()),
                true,
                new SVInterval(readMetadata.getContigID(pair1.get(1).getContig()),
                        500025 - readMetadata.getFragmentLengthStatistics(pair2.get(0).getReadGroup()).getMaxNonOutlierFragmentSize() + 151,
                        500000 + BreakpointEvidence.DiscordantReadPairEvidence.MATE_ALIGNMENT_LENGTH_UNCERTAINTY),
                false, 0, 2));

        List<Object[]> tests = new ArrayList<>();

        tests.add(new Object[] {evidenceList.iterator(), linkList.iterator()});

        return tests.toArray(new Object[][]{});
    }


    @Test(dataProvider = "evidence")
    public void testClusterEvidence(Iterator<BreakpointEvidence> evidenceIterator, Iterator<EvidenceTargetLink> expectedResults) throws Exception {
        EvidenceTargetLinkClusterer clusterer = new EvidenceTargetLinkClusterer(readMetadata, 0);
        final Iterator<EvidenceTargetLink> results = clusterer.cluster(evidenceIterator);

        while (expectedResults.hasNext()) {
            EvidenceTargetLink nextExpected =  expectedResults.next();
            assertTrue(results.hasNext());
            EvidenceTargetLink nextActual =  results.next();
            assertEquals(nextActual, nextExpected);
        }
        assertTrue(!results.hasNext());
    }

    @Test
    public void testDeduplicateTargetLinks() throws Exception {
        final ArrayList<EvidenceTargetLink> evidenceTargetLinks = new ArrayList<>();
        evidenceTargetLinks.add(new EvidenceTargetLink(new SVInterval(0, 100, 200), true, new SVInterval(0, 500, 600), false, 3, 1));
        evidenceTargetLinks.add(new EvidenceTargetLink(new SVInterval(0, 125, 400), false, new SVInterval(0, 500, 600), false, 5, 5));
        evidenceTargetLinks.add(new EvidenceTargetLink(new SVInterval(0, 550, 650), false, new SVInterval(0, 150, 250), true, 1, 2));

        List<EvidenceTargetLink> entries = EvidenceTargetLinkClusterer.deduplicateTargetLinks(evidenceTargetLinks);
        Assert.assertEquals(entries.size(), 2);
        Iterator<EvidenceTargetLink> iterator = entries.iterator();
        EvidenceTargetLink next = iterator.next();
        Assert.assertEquals(next, new EvidenceTargetLink(new SVInterval(0, 125, 400), false, new SVInterval(0, 500, 600), false, 5, 5));

        next = iterator.next();
        Assert.assertEquals(next, new EvidenceTargetLink(new SVInterval(0, 150, 200), true, new SVInterval(0, 550, 600), false, 4, 2));

        evidenceTargetLinks.add(new EvidenceTargetLink(new SVInterval(0, 100, 200), false, new SVInterval(0, 800, 900), true, 7, 8));

        entries = EvidenceTargetLinkClusterer.deduplicateTargetLinks(evidenceTargetLinks);
        Assert.assertEquals(entries.size(), 3);
        iterator = entries.iterator();
        next = iterator.next();
        Assert.assertEquals(next, new EvidenceTargetLink(new SVInterval(0, 100, 200), false, new SVInterval(0, 800, 900), true, 7, 8));
        next = iterator.next();
        Assert.assertEquals(next, new EvidenceTargetLink(new SVInterval(0, 125, 400), false, new SVInterval(0, 500, 600), false, 5, 5));
        next = iterator.next();
        Assert.assertEquals(next, new EvidenceTargetLink(new SVInterval(0, 150, 200), true, new SVInterval(0, 550, 600), false, 4, 2));


        evidenceTargetLinks.add(new EvidenceTargetLink(new SVInterval(0, 525, 560), false, new SVInterval(0, 100, 175), true, 2, 3));
        entries = EvidenceTargetLinkClusterer.deduplicateTargetLinks(evidenceTargetLinks);

        Assert.assertEquals(entries.size(), 3);
        iterator = entries.iterator();
        next = iterator.next();
        Assert.assertEquals(next, new EvidenceTargetLink(new SVInterval(0, 100, 200), false, new SVInterval(0, 800, 900), true, 7, 8));
        next = iterator.next();
        Assert.assertEquals(next, new EvidenceTargetLink(new SVInterval(0, 125, 400), false, new SVInterval(0, 500, 600), false, 5, 5));
        next = iterator.next();
        Assert.assertEquals(next, new EvidenceTargetLink(new SVInterval(0, 150, 175), true, new SVInterval(0, 550, 560), false, 6, 3));

    }
}