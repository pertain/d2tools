package d2tools.util;

import java.io.*;
import java.util.*;

public class MagicProperties {
    String filename;
    Scanner s;
    long propID;    // Property ID
    int prop1;      // Length of property field 1
    int prop2;      // Length of property field 2
    int prop3;      // Length of property field 3
    int prop4;      // Length of property field 4

    public MagicProperties(long itemID) {
        filename = "util/magicproperties.txt";
        s = null;
        propID = itemID;
        prop1 = 0;
        prop2 = 0;
        prop3 = 0;
        prop4 = 0;

        lookupID(itemID);
    }

    private void reset() {
        s = null;
        propID = -1;
        prop1 = 0;
        prop2 = 0;
        prop3 = 0;
        prop4 = 0;
    }

    private void lookupID(long id) {
        reset();

        try {
            s = new Scanner(new BufferedReader(new FileReader(filename)));
            s.useDelimiter(",");

            int cur = 0;

            while (s.hasNextLine()) {
                String line = s.nextLine();

                String sItems[] = line.split(",");

                cur = Integer.parseInt(sItems[0]);

                if (cur == 9999) {  // End of parsable data
                    break;
                }

                if (cur == id) {
                    propID = id;

                    for (int i = 1; i < sItems.length; i++) {
                        if (i == 1) {
                            prop1 = Integer.parseInt(sItems[i]);
                        }
                        if (i == 2) {
                            prop2 = Integer.parseInt(sItems[i]);
                        }
                        if (i == 3) {
                            prop3 = Integer.parseInt(sItems[i]);
                        }
                        if (i == 4) {
                            prop4 = Integer.parseInt(sItems[i]);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            reset();
            ex.printStackTrace();
            return;
        }
        finally {
            s.close();
        }
    }

    public long getPropID() {
        return propID;
    }

    public int getProp1() {
        return prop1;
    }

    public int getProp2() {
        return prop2;
    }

    public int getProp3() {
        return prop3;
    }

    public int getProp4() {
        return prop4;
    }
}
