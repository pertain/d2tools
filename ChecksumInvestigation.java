package d2tools;

import d2tools.util.*;

public class ChecksumInvestigation {

	private static long calculateCheckSum(D2BitReader iRead) {
		iRead.set_byte_pos(0);
		long lCheckSum = 0; // unsigned integer checksum
        long test = 0;
        long test2 = 0;
        long test3 = 0;

		for (int i = 0; i < iRead.get_length(); i++) {
			long lByte = iRead.read(8);

			//if (i >= 7 && i <= 10) {
                //lByte = 0;
            //}

            test = lCheckSum;
            test2 = test << 33;
            test3 = test2 >>> 32;
            System.out.println(String.format("(%s: %d) (%s: %d) (%s %d)", "test", test, "test2", test2, "test3", test));


			long upshift = lCheckSum << 33 >>> 32;
            System.out.println(String.format("(%d) %s: %d", i, "upshift", upshift));
			long add = lByte + ((lCheckSum >>> 31) == 1 ? 1 : 0);
            System.out.println(String.format("(%d) %s: %d", i, "add", add));
			lCheckSum = upshift + add;
            System.out.println(String.format("(%d) %s: %d", i, "lCheckSum", lCheckSum));

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
            String filetype;

            if (isD2S) {
                filetype = "d2s";
            }
            else if (isD2X) {
                filetype = "d2x";
            }
            else {
                throw new Exception("Something is wrong with the file extension");
            }

            if (isD2S) {
                System.out.println("Can only calculate checksums for .d2x files.");
            }
            else if (isD2X) {
                ///*
                System.out.println("Calculating .d2x checksum for " + iFilename + " (first 3 bytes)");

                iReader.set_byte_pos(0);
                byte[] data = iReader.get_bytes(3);


                // Calculate checksum without clearing the existing checksum (unlike d2s)
                lCheckSum = calculateCheckSum(iReader);
                System.out.println(String.format("(%s) %s: %d", "F", "lCheckSum", lCheckSum));

                if (lCheckSum == 0) {
                    throw new Exception("Unable to calculate checksum");
                }

                System.out.println("Calculated checksum: " + lCheckSum);
            }
        }
        else {
            System.out.println("No args provided");
        }
	}
}
