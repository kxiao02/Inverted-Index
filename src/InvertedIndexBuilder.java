import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class InvertedIndexBuilder {

    // File to store the inverted index.
    private static final String OUTPUT_FILE = "invertedIndex";

    // Number of postings in a block.
    private static final int BLOCK_SIZE = 64;

    // Input file containing the postings.
    private final String inputFile;

    // Object to keep track of terms and their associated data.
    private final Lexicon lexicon = new Lexicon();

    // Constructor to initialize the input file.
    public InvertedIndexBuilder(String inputFile) {
        this.inputFile = inputFile;
    }

    // Method to build the inverted index.
    public void buildInvertedIndex() {
        try (
                // Reader to read from the input file.
                BufferedReader reader = new BufferedReader(new FileReader(inputFile));

                // Output stream to write the inverted index to the file.
                DataOutputStream out = new DataOutputStream(new FileOutputStream(OUTPUT_FILE))
        ) {
            // Lists to store document IDs and frequencies for each term. Refreshed for each term.
            List<Long> docIDs = new ArrayList<>();
            List<Long> freqs = new ArrayList<>();

            // Variable to keep track of the current term being processed.
            String term = null;

            // Infinite loop to keep reading lines from the file until break is called.
            while (true) {
                // Read a line from the file.
                String line = reader.readLine();

                // If the line is null (end of file) and term is also null (no term processed), break the loop.
                if (line == null && term == null) break;

                // Split the line into term, document ID, and frequency.
                String[] postingFields = line != null ? line.split(" ") : null;
                String currTerm = line != null ? postingFields[0] : null;
                long currId = line != null ? Long.parseLong(postingFields[1]) : -1;
                long currFreq = line != null ? Long.parseLong(postingFields[2]) : -1;

                // If this is the first term, initialize the term and increment numPosting.
                if (term == null) {
                    term = currTerm;
                    lexicon.incNumPosting();
                }

                // If the current term is the same as the previous, increment numPosting and add the docID and frequency to the lists.
                if (currTerm != null && currTerm.equals(term)) {
                    lexicon.incNumPosting();
                    docIDs.add(currId);
                    freqs.add(currFreq);
                } else {
                    // If the current term is different, process the previous term.
                    createInvertedListForTerm(out, docIDs, freqs);
                    lexicon.setEndPos(out.size());
                    lexicon.add(term);
                    lexicon.reset();

                    // If currTerm is null, it means we have reached the end of the file, so break the loop.
                    if (currTerm == null) break;

                    // Otherwise, start processing the new term.
                    term = currTerm;
                    docIDs.clear();
                    freqs.clear();
                    docIDs.add(currId);
                    freqs.add(currFreq);

                }
            }

            // Write the lexicon to the output file.
            lexicon.write();
        } catch (IOException e) {
            System.out.println("Error: Can Not Open merged posting file");
            e.printStackTrace();
        }
    }

    /*
			Block Layout (before compression):
		   ┌─────────────┬────────────┬───────────┐
		   │ metaData(16)│ docIDs(512)│ Freqs(512)│
		   └─────────────┴────────────┴───────────┘
	*/

    // Method to create the inverted list for a term and write it to the output file.
    private void createInvertedListForTerm(DataOutputStream out, List<Long> docIDs, List<Long> freqs) throws IOException {
        // List to store the inverted list.
        List<Byte> invertedList = new ArrayList<>();

        // Lists to store metadata.
        List<Long> blockSizeMeta = new ArrayList<>();
        List<Long> metaLastId = new ArrayList<>();

        // Compress the postings and get the blocks.
        List<Byte> blocks = compressPostings(docIDs, freqs, blockSizeMeta, metaLastId);

        // Compress and add metadata to the inverted list.
        invertedList.addAll(Util.VarByte.encode(blockSizeMeta.stream().map(Long::intValue).collect(Collectors.toList())));
        invertedList.addAll(Util.VarByte.encode(metaLastId.stream().map(Long::intValue).collect(Collectors.toList())));

        // Append blocks to the inverted list.
        invertedList.addAll(blocks);

        // Write inverted list to file.
        for (byte b : invertedList) {
            out.writeByte(b);
        }
    }

    // Method to compress postings and get blocks.
    private List<Byte> compressPostings(List<Long> docIDs, List<Long> freqs, List<Long> blockSizeMeta, List<Long> lastDocIdMeta) {
        List<Byte> blocks = new ArrayList<>();
        int numBlocks = (int) Math.ceil((double) docIDs.size() / BLOCK_SIZE);

        for (int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            int start = blockIndex * BLOCK_SIZE;
            int end = Math.min((blockIndex + 1) * BLOCK_SIZE, docIDs.size());
            List<Long> idBlock = new ArrayList<>(docIDs.subList(start, end));
            List<Long> freqBlock = new ArrayList<>(freqs.subList(start, end));

            // Convert docIDs to differences for compression.
            convertToDifferences(idBlock, lastDocIdMeta);

            // Compress the block. Uses stream to optimize memory usage
            List<Byte> compressedIdBlock = Util.VarByte.encode(idBlock.stream().map(Long::intValue).collect(Collectors.toList()));
            List<Byte> compressedFreqBlock = Util.VarByte.encode(freqBlock.stream().map(Long::intValue).collect(Collectors.toList()));

            // Update metadata.
            lastDocIdMeta.add(docIDs.get(end - 1));
            blockSizeMeta.add((long) (compressedIdBlock.size() + compressedFreqBlock.size()));

            // Add compressed data to blocks.
            blocks.addAll(compressedIdBlock);
            blocks.addAll(compressedFreqBlock);
        }

        return blocks;
    }

    // Method to convert docIDs to differences.
    private void convertToDifferences(List<Long> ids, List<Long> lastDocIdMeta) {
        for (int i = ids.size() - 1; i >= 0; i--) {
            if (i != 0) {
                ids.set(i, ids.get(i) - ids.get(i - 1));
            } else if (!lastDocIdMeta.isEmpty()) {
                ids.set(i, ids.get(i) - lastDocIdMeta.get(lastDocIdMeta.size() - 1));
            }
        }
    }
}
