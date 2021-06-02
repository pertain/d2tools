package d2tools;

import d2tools.util.*;

import java.util.*;

public class UpdateChecksum {

	private static long calculateCheckSum(D2BitReader iRead, String filetype) {
		iRead.set_byte_pos(0);
		long lCheckSum = 0; // unsigned integer checksum

        int bitmin = 0;
        int bitmax = 0;

        switch (filetype) {
            case "d2s" :
                bitmin = 12;
                bitmax = 15;
                break;
            case "d2x" :
                bitmin = 7;
                bitmax = 10;
                break;
            default :
                System.out.println("Invalid file type. Checksum not calculated.");
                return 0;
        }

		for (int i = 0; i < iRead.get_length(); i++) {
			long lByte = iRead.read(8);

			if (i >= bitmin && i <= bitmax) {
                lByte = 0;
            }

			long upshift = lCheckSum << 33 >>> 32;
			long add = lByte + ((lCheckSum >>> 31) == 1 ? 1 : 0);
			lCheckSum = upshift + add;
		}

		return lCheckSum;
	}

	public static void main(String[] args) throws Exception
	{
        if (args.length > 0) {
            String iFilename = args[0];
            boolean isD2S = iFilename.toLowerCase().endsWith(".d2s");
            boolean isD2X = iFilename.toLowerCase().endsWith(".d2x");

            if (iFilename == null || !(isD2S || isD2X)) {
                throw new Exception("Incorrect Character file name");
            }

            D2BitReader iReader;
            iReader = new D2BitReader(iFilename);

            // Determine the correct checksum offset
            long lCheckSum = 0;
            int checksumOffset = 0;
            String filetype;

            if (isD2S) {
                checksumOffset = 12;
                filetype = "d2s";
            }
            else if (isD2X) {
                checksumOffset = 7;
                filetype = "d2x";
            }
            else {
                throw new Exception("Something is wrong with the file extension");
            }

            // TODO: Clean up D2S section so it resembles the D2X section
            // i.e.     1. Read/Print current checksum in decimal and hex
            //          2. Calculate/Print new checksum in decimal and hex
            //          3. Prompt user before modifying the file
            //          4. Update file
            //          5. Read/Print updated checksum in decimal and hex
            if (isD2S) {
                System.out.println("Calculating .d2s checksum for " + iFilename);

                iReader.set_byte_pos(0);
                byte[] data = iReader.get_bytes(iReader.get_length());

                byte[] existingChecksumBytes = { data[checksumOffset],
                                                 data[checksumOffset + 1],
                                                 data[checksumOffset + 2],
                                                 data[checksumOffset + 3] };
                System.out.println(String.format("0x%02X 0x%02X 0x%02X 0x%02X", existingChecksumBytes[3],
                                                                                existingChecksumBytes[2],
                                                                                existingChecksumBytes[1],
                                                                                existingChecksumBytes[0]));

                // Clear the current checksum
                byte[] calculatedChecksumBytes = { 0, 0, 0, 0 };
                iReader.setBytes(checksumOffset, calculatedChecksumBytes);

                byte[] length = new byte[4];
                length[3] = (byte) ((0xff000000 & data.length) >>> 24);
                length[2] = (byte) ((0x00ff0000 & data.length) >>> 16);
                length[1] = (byte) ((0x0000ff00 & data.length) >>> 8);
                length[0] = (byte) (0x000000ff & data.length);
                iReader.setBytes(8, length);

                iReader.set_byte_pos(0);
                //long lCheckSum = calculateCheckSum(iReader, filetype);
                lCheckSum = calculateCheckSum(iReader, "d2s");
                System.out.println("Calculated checksum: " + lCheckSum);

                if (lCheckSum == 0) {
                    throw new Exception("Unable to calculate checksum");
                }

                calculatedChecksumBytes[3] = (byte) ((0xff000000 & lCheckSum) >>> 24);
                calculatedChecksumBytes[2] = (byte) ((0x00ff0000 & lCheckSum) >>> 16);
                calculatedChecksumBytes[1] = (byte) ((0x0000ff00 & lCheckSum) >>> 8);
                calculatedChecksumBytes[0] = (byte) (0x000000ff & lCheckSum);

                System.out.println(String.format("0x%02X 0x%02X 0x%02X 0x%02X", calculatedChecksumBytes[3],
                                                                                calculatedChecksumBytes[2],
                                                                                calculatedChecksumBytes[1],
                                                                                calculatedChecksumBytes[0]));

                //System.out.println("Writing checksum to file");
                // Write the checksum to d2s file
                //iReader.setBytes(checksumOffset, calculatedChecksumBytes);
                //iReader.save();
                //setModified(false);
            }
            else if (isD2X) {
                System.out.println("Calculating .d2x checksum for " + iFilename);

                iReader.set_byte_pos(checksumOffset);
                byte[] existingChecksumBytes = iReader.get_bytes(4);


                iReader.set_byte_pos(checksumOffset);
                long existingChecksum = iReader.read(32);


                System.out.println(String.format("%-20s: [0x%02X 0x%02X 0x%02X 0x%02X] (%d)",
                                                 "Existing Checksum",
                                                 existingChecksumBytes[3],
                                                 existingChecksumBytes[2],
                                                 existingChecksumBytes[1],
                                                 existingChecksumBytes[0],
                                                 existingChecksum));


                // Calculate checksum without clearing the existing checksum (unlike d2s)
                long calculatedChecksum = calculateCheckSum(iReader, "d2x");

                if (calculatedChecksum == 0) {
                    throw new Exception("Unable to calculate checksum");
                }

                // Clear the current checksum (not sure if clearing is necessary, but it doesn't hurt)
                byte[] calculatedChecksumBytes = { 0, 0, 0, 0 };
                iReader.set_byte_pos(checksumOffset);
                iReader.setBytes(checksumOffset, calculatedChecksumBytes);

                calculatedChecksumBytes[3] = (byte) ((0xff000000 & calculatedChecksum) >>> 24);
                calculatedChecksumBytes[2] = (byte) ((0x00ff0000 & calculatedChecksum) >>> 16);
                calculatedChecksumBytes[1] = (byte) ((0x0000ff00 & calculatedChecksum) >>> 8);
                calculatedChecksumBytes[0] = (byte) (0x000000ff & calculatedChecksum);


                System.out.println(String.format("%-20s: [0x%02X 0x%02X 0x%02X 0x%02X] (%d)",
                                                 "Calculated Checksum",
                                                 calculatedChecksumBytes[3],
                                                 calculatedChecksumBytes[2],
                                                 calculatedChecksumBytes[1],
                                                 calculatedChecksumBytes[0],
                                                 calculatedChecksum));


                // Now that you know the calculated checksum, do you want to update the file with it?
                Scanner uInput = new Scanner(System.in);
                System.out.print("Write checksum to file? (N/y): ");
                String uResponse = uInput.nextLine();

                if (uResponse.toLowerCase().charAt(0) == 'y') {
                    System.out.println("Updating checksum in " + iFilename);

                    // Update file with the new checksum
                    iReader.setBytes(checksumOffset, calculatedChecksumBytes);
                    iReader.save();


                    iReader.set_byte_pos(checksumOffset);
                    byte[] updatedChecksumBytes = iReader.get_bytes(4);

                    iReader.set_byte_pos(checksumOffset);
                    long updatedChecksum = iReader.read(32);

                    System.out.println(String.format("%-20s: [0x%02X 0x%02X 0x%02X 0x%02X] (%d)",
                                                     "Updated Checksum",
                                                     updatedChecksumBytes[3],
                                                     updatedChecksumBytes[2],
                                                     updatedChecksumBytes[1],
                                                     updatedChecksumBytes[0],
                                                     updatedChecksum));

                }
                else {
                    System.out.println("File was not modified (" + iFilename + ")");
                }
            }
        }
        else {
            System.out.println("No args provided");
        }
	}
}
