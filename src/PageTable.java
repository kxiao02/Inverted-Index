import java.io.*;
import java.util.*;

public class PageTable {
    private final List<Doc> table;

    public PageTable() {
        this.table = new ArrayList<>();
    }

    public void addDoc(int docID, String link, long size) {
        Doc e = new Doc(docID, link, size);
        table.add(e);
    }

    public void write() {
        String output = "pageTable";

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
            for (Doc entry : table) {
                dos.writeInt(entry.docId);
                dos.writeUTF(entry.url);
                dos.writeLong(entry.size);
                dos.flush(); // Ensure that the data is written to the output stream
            }
        } catch (IOException e) {
            System.out.println("Failed to Open " + output);
            e.printStackTrace();
        }
    }

    private static class Doc {
        private final int docId;
        private final String url;
        private final long size;

        private Doc(int docId, String url, long size) {
            this.docId = docId;
            this.url = url;
            this.size = size;
        }
    }
}
