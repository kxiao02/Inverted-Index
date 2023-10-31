import java.io.*;
import java.util.*;

public class Lexicon {
    // List to store the lexicon entries
    private final List<LexiconEntry> lexicon;
    // Current start position in the inverted index file for a term
    private long currStartPos;
    // Current end position in the inverted index file for a term
    private long currEndPos;
    // Current number of postings (document-frequency) for a term
    private long currNumPosting;

    // Constructor initializing the lexicon list and variables
    public Lexicon() {
        lexicon = new ArrayList<>();
        currStartPos = 0;
        currEndPos = 0;
        currNumPosting = 0;
    }

    // Method to add a new term along with its metadata to the lexicon
    public void add(String term) {
        LexiconEntry lexiconEntry = new LexiconEntry(term, currStartPos, currEndPos, currNumPosting);
        lexicon.add(lexiconEntry);
    }

    // Method to reset variables for the next term
    public void reset() {
        currStartPos = currEndPos + 1;
        currNumPosting = 1;
    }

    // Method to write the lexicon to a file
    public void write() throws IOException {
        String output = "lexicon";

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
            for (LexiconEntry entry : lexicon) {
                dos.writeUTF(entry.term); // Writing term
                dos.writeLong(entry.startPos); // Writing start position of term in inverted index
                dos.writeLong(entry.endPos); // Writing end position of term in inverted index
                dos.writeLong(entry.numPosting); // Writing document-frequency of term
            }
        }
    }

    // Method to increment the number of postings for the current term
    public void incNumPosting() {
        currNumPosting++;
    }

    // Method to set the end position of the current term in the inverted index
    public void setEndPos(long outputSize) {
        currEndPos = outputSize - 1;
    }

    // Inner class to represent an entry in the lexicon
    private static class LexiconEntry {
        String term; // The term
        long startPos; // Start position of term in inverted index
        long endPos; // End position of term in inverted index
        long numPosting; // Document-frequency of term

        // Constructor to initialize a LexiconEntry
        private LexiconEntry(String term, long startPos, long endPos, long numPosting) {
            this.term = term;
            this.startPos = startPos;
            this.endPos = endPos;
            this.numPosting = numPosting;
        }
    }
}
