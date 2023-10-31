public class Run {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java Run <input_file>");
            System.exit(1);
        }

        /* ./data/<SOURCE_FILE>
         * Either .trec.gz or .trec will work*/
        String dataFilePath = args[0];
        Timer timer = new Timer();

        int numPostings = createIntermediatePostings(dataFilePath, timer);
        String mergedFile = sortAndMergePostings(numPostings, timer);
        createInvertedIndex(mergedFile, timer);

        System.out.println("Total runtime: " + timer.update(1) + " s");
    }

    private static int createIntermediatePostings(String filePath, Timer timer) {
        PostingBuilder postingBuilder = new PostingBuilder(filePath, 536870912);
        int numPostings = postingBuilder.build();
        System.out.println("Total time to create intermediate postings: " + timer.update(0) + " s");
        System.out.println("Number of Postings: " + numPostings);
        return numPostings;
    }

    private static String sortAndMergePostings(int numPostings, Timer timer) {
        String mergedFile = Util.Merge.sortMerge(numPostings);
        System.out.println("Total time to sort and merge postings: " + timer.update(0) + " s");
        return mergedFile;
    }

    private static void createInvertedIndex(String mergedFile, Timer timer) {
        InvertedIndexBuilder invertedIndexBuilder = new InvertedIndexBuilder(mergedFile);
        invertedIndexBuilder.buildInvertedIndex();
        System.out.println("Total time to create inverted index and lexicon: " + timer.update(0) + " s");
    }
}
