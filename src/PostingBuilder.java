import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;

public class PostingBuilder {

    // Matches words, standalone numbers, percentages, and money amounts
    private static final Pattern TERM_PATTERN = Pattern.compile("\\w+|%\\d+|\\$\\d+");
    private static final String TEXT_START = "<TEXT>";
    private static final String TEXT_END = "</TEXT>";

    private final String filePath;
    private final int bufferSize;
    private final ByteArrayOutputStream buffer;
    private int fileNum;

    public PostingBuilder(String inputFile, int size) {
        this.filePath = inputFile;
        this.bufferSize = size;
        // Automatically allocates address when full
        this.buffer = new ByteArrayOutputStream(bufferSize);
        this.fileNum = 0;
    }

    public int build() {
        try (BufferedReader reader = getBufferedReader(filePath);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize)) {

            List<Posting> postings = new ArrayList<>();  // List to store postings for the current document
            PageTable table = new PageTable();  // Page table to store document metadata (docID, URL)
            String currentDocUrl = "";  // URL of the current document
            int docID = 0;  // Document ID counter
            long docSize = 0;  // Size of the current document
            boolean insideDoc = false;  // Flag to track whether we are inside a document

            String line;
            while ((line = reader.readLine()) != null) {
                if (TEXT_START.equals(line)) {
                    if (!insideDoc) {
                        insideDoc = true;
                        docID++;
                    }
                } else if (TEXT_END.equals(line)) {
                    if (insideDoc) {
                        insideDoc = false;
                        table.addDoc(docID, currentDocUrl, docSize);  // Add an entry to the page table for the current document
                        docSize = 0;  // Reset document size for the next document
                        currentDocUrl = "";  // Reset URL for the next document

                        if (!postings.isEmpty()) {
                            processDocument(postings, docID);
                            postings.clear();
                        }
                    }
                } else if (insideDoc) {
                    docSize += line.length();
                    if (currentDocUrl.isEmpty()) {
                        currentDocUrl = line;  // Assume the first line inside a document is its URL
                    } else {
                        tokenizeAndAddPostings(line, docID, postings);  // Tokenize the line and add postings to the list
                    }
                }
            }

            if (buffer.size() != 0) flush(); // Flush the buffer if it's not empty

            table.write(); // Write the page table to disk in binary

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("An error occurred while building postings: " + e.getMessage());
        }
        return fileNum;
    }


    // Handles gzip files
    private BufferedReader getBufferedReader(String filePath) throws IOException {
        if (filePath.endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filePath)), StandardCharsets.UTF_8));
        } else {
            return new BufferedReader(new FileReader(filePath));
        }
    }

    private List<Posting> mergePostings(List<Posting> postings) {
        Map<String, Posting> merged = new LinkedHashMap<>();
        for (Posting p : postings) {
            merged.compute(p.term, (k, v) -> (v == null) ? p : new Posting(p.term, p.docID, v.frequency + p.frequency));
        }
        return new ArrayList<>(merged.values());
    }

    private void tokenizeAndAddPostings(String line, int docId, List<Posting> postings) {
        Matcher matcher = TERM_PATTERN.matcher(line);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            postings.add(new Posting(word, docId, 1));
        }
    }

    private void processDocument(List<Posting> postings, int docId) throws IOException {
        if (postings.isEmpty()) return;

        // Sort postings by term
        postings.sort(Comparator.comparing(p -> p.term));

        // Merge postings with the same term
        List<Posting> mergedPostings = mergePostings(postings);

        // Build and write posting strings to buffer
        for (Posting p : mergedPostings) {
            String postingString = p.term + ' ' + docId + ' ' + p.frequency + '\n';
            byte[] postingBytes = postingString.getBytes(StandardCharsets.UTF_8);

            // If adding the current posting would overflow the buffer, flush the buffer first
            if (buffer.size() + postingBytes.length > bufferSize) {
                flush();
            }

            // Write the current posting to the buffer
            buffer.write(postingBytes);
        }
    }


    // Write out any buffered content to an underlying stream
    // Ensures that all data that has been buffered is actually written out and not left in the buffer.
    private void flush() throws IOException {
        if (buffer.size() > 0) {
            fileNum++; // start a new file
            System.out.println("======== Flushed to file " + fileNum + " ========");

            try (BufferedWriter out = new BufferedWriter(new FileWriter("Postings" + fileNum))) {
                out.write(buffer.toString(StandardCharsets.UTF_8));
            }

            // Clear the buffer
            buffer.reset();
        }
    }

    static class Posting {
        String term;
        int docID;
        int frequency;

        public Posting(String term, int docID, int frequency) {
            this.term = term;
            this.docID = docID;
            this.frequency = frequency;
        }
    }
}
