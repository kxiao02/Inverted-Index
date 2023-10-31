import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Util {
    static class Merge {
        public static String sortMerge(int numFiles) {
            List<String> filesToMerge = new ArrayList<>();

            /* Sort the contents of the postings file by the first field (term) alphanumerically.
             * For lines that have the same term, it will sort them by the second field (docID) numerically.*/
            List<String> sortCmd = new ArrayList<>(Arrays.asList("sort", "-k1,1", "-k2,2n"));

            for (int i = 1; i <= numFiles; i++) {
                // The posting file to be sorted using Unix sort
                String fileToSort = String.format("postingList#%d", i);
//                System.out.println("======== Start Unix Sort " + fileToSort + " ========");

                // Sorted file
                String sortedFileName = String.format("sortedPostingList#%d", i);

                // Build and execute the sorting command line
                List<String> fullSortCmd = new ArrayList<>(Arrays.asList(fileToSort, "-o", sortedFileName));
                fullSortCmd.addAll(0, sortCmd);
                executeCommand(fullSortCmd);
//                System.out.println("======== Done Unix Sort " + fileToSort + " ========");

                // Add the sorted file to merge list
                filesToMerge.add(sortedFileName);

                // Remove original posting file
                try {
                    Files.delete(Paths.get(fileToSort));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Merge sorted files to one file and return the file name.
            return merge(filesToMerge);
        }

        private static String merge(List<String> filesToMerge) {
            List<String> mergeCmd = new ArrayList<>(Arrays.asList("sort", "-m", "-k2,2n"));

//            System.out.println("======== Start merging posting files ========");

            // Build and execute the merging command line
            mergeCmd.addAll(filesToMerge);
            mergeCmd.add("-o");
            mergeCmd.add("mergedPostingList");
            executeCommand(mergeCmd);
//            System.out.println("======== Done merging posting files ========");

            // Remove all the intermediate sorted postings
            for (String fileToMerge : filesToMerge) {
                try {
                    Path path = Paths.get(fileToMerge);
                    Files.delete(path);
                } catch (IOException e) {
                    System.err.println("Failed to delete file: " + fileToMerge);
                    e.printStackTrace();
                }
            }
//            System.out.println("======== Done removing sorted files ========");

            return "mergedPostingList";
        }

        private static void executeCommand(List<String> command) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                Process process = processBuilder.start();

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.err.println("Command executed with exit code: " + exitCode);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class VarByte {

        // Encode a single number using variable byte encoding
        public static List<Byte> encodeNum(int num) {
            List<Byte> encodedBytes = new ArrayList<>();
            while (num >= 128) {
                encodedBytes.add((byte) (num & 0x7F)); // Write the 7 least significant bits
                num >>>= 7;  // Unsigned right shift by 7 bits
            }
            encodedBytes.add((byte) (num | 0x80)); // Set the most significant bit to indicate end of number
            return encodedBytes;
        }

        // Encode a list of numbers
        public static List<Byte> encode(List<Integer> numbers) {
            List<Byte> encodedArray = new ArrayList<>();
            for (int number : numbers) {
                encodedArray.addAll(encodeNum(number));
            }
            return encodedArray;
        }

        // Decode a list of bytes into numbers
        public static List<Integer> decode(List<Byte> encodedBytes) {
            List<Integer> decodedNumbers = new ArrayList<>();
            int currentNumber = 0;
            int shiftAmount = 0;
            for (byte encodedByte : encodedBytes) {
                if (encodedByte >= 0) {  // Check if the most significant bit is 0
                    currentNumber |= (encodedByte << shiftAmount);
                    decodedNumbers.add(currentNumber);
                    currentNumber = 0;
                    shiftAmount = 0;
                } else {  // Most significant bit is 1
                    currentNumber |= ((encodedByte & 0x7F) << shiftAmount);  // Mask the most significant bit
                    shiftAmount += 7;
                }
            }
            return decodedNumbers;
        }
    }

}
