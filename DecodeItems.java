package d2tools;

import d2tools.util.*;

public class DecodeItems {

    private static long parseBits(D2BitReader ibr, int firstBit, int bitsToRead, String fieldName, boolean addEOL) {
        int relOffsetFirstBit = ibr.get_pos() - firstBit;           // treat first bit in item section as offset 0
        int relOffsetLastBit = relOffsetFirstBit + bitsToRead - 1;  // need to subtract 1 for 0-based indexing
        int firstByte = (ibr.get_pos() + 8) / 8;
        long fieldVal = ibr.read(bitsToRead);
        int lastByte = (ibr.get_pos() + 7) / 8;
        
        if (addEOL) {
            System.out.println(String.format("(%02d-%02d) %03d-%03d: %-19s %-12d", firstByte, lastByte, relOffsetFirstBit, relOffsetLastBit, fieldName, fieldVal));
        }
        else {
            System.out.print(String.format("(%02d-%02d) %03d-%03d: %-19s %-12d", firstByte, lastByte, relOffsetFirstBit, relOffsetLastBit, fieldName, fieldVal));
        }

        return fieldVal;
    }

    private static void parseRareBits(D2BitReader ibr, int firstBit, Item it) {
        long fieldVal = 0;
        String prefixText = "";
        String suffixText = "";

        it.rareNameID1 = (int)parseBits(ibr, firstBit, 8, "FirstNameID", false);
        it.rareName1 = it.getRareName(it.rareNameID1);
        System.out.println(String.format("%-25s", it.rareName1));

        it.rareNameID2 = (int)parseBits(ibr, firstBit, 8, "LastNameID", false);
        it.rareName2 = it.getRareName(it.rareNameID2);
        System.out.println(String.format("%-25s", it.rareName2));

        // Rare and Crafted items have 6 affixExists placeholders.
        // If a placeholder is set to true (1) then the next 11 bits
        // contains the affix
        // Crafted items can only have 2 to 4 affixes enabled
        // Rare items can have 2 to 6 affixes enabled
        // In both Crafted and Rare items, there must be at least
        // one Prefix and one Suffix (i.e. cannot be all Prefix or all Suffix)
        for (int i = 0; i < 6; i++) {
            // Is there an affix?
            fieldVal = parseBits(ibr, firstBit, 1, "AffixExists", true);

            if (fieldVal == 1) {
                // The affix exists, now read it's ID (11 bits)
                fieldVal = parseBits(ibr, firstBit, 11, "AffixID", false);

                it.rareAffixes[i] = (int)fieldVal;

                prefixText = "(" + it.getMagicPrefix(it.rareAffixes[i]) + ")";
                suffixText = "(" + it.getMagicSuffix(it.rareAffixes[i]) + ")";

                System.out.println(String.format("%s %s %s", prefixText, "/", suffixText));
            }
        }
    }

    private static void parseMagicProperties(D2BitReader ibr, int firstBit) {
        Item it = new Item();
        MagicProperties prop;

        // TODO this arbitrary 50 iteration while loop is stupid -- fix it
        int idx = 0;
        while (idx++ < 50) {    // Arbitrary number to prevent infinite loop during bad decode
            long fieldVal = parseBits(ibr, firstBit, 9, "MagicPropertyID", true);

            // Termination sequence (0x1FF) -- indicates the end of an item or section
            if (fieldVal == 511) {
                break;
            }

            prop = new MagicProperties(fieldVal);

            if (prop.getPropID() == fieldVal) {
                fieldVal = parseBits(ibr, firstBit, prop.getProp1(), "MagicProperty A", true);

                if (prop.getProp2() > 0) {
                    fieldVal = parseBits(ibr, firstBit, prop.getProp2(), "MagicProperty B", true);

                    if (prop.getProp3() > 0) {
                        fieldVal = parseBits(ibr, firstBit, prop.getProp3(), "MagicProperty C", true);

                        if (prop.getProp4() > 0) {
                            fieldVal = parseBits(ibr, firstBit, prop.getProp4(), "MagicProperty D", true);
                        }
                    }
                }
            }

            it.mProps.add(prop);
        }
    }

public static void main(String[] args) throws Exception
{
if (args.length > 0) {
    String iFilename = args[0];
    boolean isD2S = iFilename.toLowerCase().endsWith(".ds");
    boolean isD2X = iFilename.toLowerCase().endsWith(".d2x");

    if (iFilename == null || !(isD2S || isD2X)) {
        throw new Exception("Incorrect Character file name");
    }

    int itemSectionFirstByte = 0;
    int itemFirstBit = 0;
    int curByte = 0;
    long fieldVal = 0;

    Item itm = new Item();

    D2BitReader iReader;
    iReader = new D2BitReader(iFilename);

    MagicProperties prop;

    iReader.set_byte_pos(0);
    itemSectionFirstByte = iReader.findNextFlag("JM", 0);
    iReader.set_byte_pos(itemSectionFirstByte);

    System.out.println("Reading first item in " + iFilename);
    System.out.println();

    if (isD2S) {
        curByte = iReader.findNextFlag("JM", itemSectionFirstByte);
        iReader.set_byte_pos(curByte);
        itemFirstBit = iReader.get_pos();
    }
    else if (isD2X) {
        curByte = itemSectionFirstByte;
        iReader.set_byte_pos(itemSectionFirstByte);
        itemFirstBit = iReader.get_pos();
    }

    System.out.println("------------------------------------------------------------");
    System.out.println(String.format("%-7s %-8s %-19s %-11s %-42s", "fBytes",
                                                                    "iBits",
                                                                    "Property",
                                                                    "Dec Value",
                                                                    "Text Value"));
    System.out.println("------------------------------------------------------------");

    //////////////////////////
    // BEGIN SIMPLE SECTION //
    //////////////////////////

    iReader.set_byte_pos(curByte);

    fieldVal = parseBits(iReader, itemFirstBit, 16, "JM", true);
    fieldVal = parseBits(iReader, itemFirstBit, 4, "<unknown>", true);
    itm.isIdentified = (short)parseBits(iReader, itemFirstBit, 1, "IsIdentified", true);
    fieldVal = parseBits(iReader, itemFirstBit, 6, "<unknown>", true);
    itm.isSocketed = (short)parseBits(iReader, itemFirstBit, 1, "IsSocketed", true);
    fieldVal = parseBits(iReader, itemFirstBit, 1, "<unknown>", true);
    itm.isNew = (short)parseBits(iReader, itemFirstBit, 1, "IsNew", true);
    fieldVal = parseBits(iReader, itemFirstBit, 2, "<unknown>", true);
    itm.isEar = (short)parseBits(iReader, itemFirstBit, 1, "IsEar", true);
    itm.isStarter = (short)parseBits(iReader, itemFirstBit, 1, "IsStarter", true);
    fieldVal = parseBits(iReader, itemFirstBit, 3, "<unknown>", true);
    itm.isSimple = (short)parseBits(iReader, itemFirstBit, 1, "IsSimple", true);
    itm.isEthereal = (short)parseBits(iReader, itemFirstBit, 1, "IsEthereal", true);
    fieldVal = parseBits(iReader, itemFirstBit, 1, "<unknown>", true);
    itm.isPersonalized = (short)parseBits(iReader, itemFirstBit, 1, "IsPersonalized", true);
    fieldVal = parseBits(iReader, itemFirstBit, 1, "<unknown>", true);
    itm.isRuneword = (short)parseBits(iReader, itemFirstBit, 1, "IsRuneword", true);
    fieldVal = parseBits(iReader, itemFirstBit, 15, "<unknown>", true);
    itm.locationID = (int)parseBits(iReader, itemFirstBit, 3, "LocationID", true);
    itm.equippedID = (int)parseBits(iReader, itemFirstBit, 4, "EquippedID", true);
    itm.positionX = (int)parseBits(iReader, itemFirstBit, 4, "PositionX", true);
    itm.positionY = (int)parseBits(iReader, itemFirstBit, 3, "PositionY", true);
    fieldVal = parseBits(iReader, itemFirstBit, 1, "<unknown>", true);
    itm.altPositionID = (int)parseBits(iReader, itemFirstBit, 3, "AltPositionID", true);

    if (itm.isEar == 0) {

        char tmp;
        // Item Type -- 4 chars, each 8 bits
        tmp = (char)parseBits(iReader, itemFirstBit, 8, "ItemTypeChar1", false);
        System.out.println(String.format("%-25c", tmp));
        itm.typeID += String.valueOf(tmp);

        tmp = (char)parseBits(iReader, itemFirstBit, 8, "ItemTypeChar2", false);
        System.out.println(String.format("%-25c", tmp));
        itm.typeID += String.valueOf(tmp);

        tmp = (char)parseBits(iReader, itemFirstBit, 8, "ItemTypeChar3", false);
        System.out.println(String.format("%-25c", tmp));
        itm.typeID += String.valueOf(tmp);

        tmp = (char)parseBits(iReader, itemFirstBit, 8, "ItemTypeChar4", false);
        System.out.println(String.format("%-25c", tmp));
        itm.typeID += String.valueOf(tmp);

        itm.typeID = itm.typeID.trim();
        System.out.println(String.format("%16s %-19s %-11s %s", "----------------", "ItemTypeID", "-", itm.typeID));
        itm.type = itm.getItemType();
        System.out.println(String.format("%16s %-19s %-11s %s", "----------------", "ItemType", "-", itm.type));

        switch (itm.type) {
            case ARMOR :
                itm.typeName = itm.getArmorName();
                break;
            case SHIELD :
                itm.typeName = itm.getShieldName();
                break;
            case WEAPON :
                itm.typeName = itm.getWeaponName();
                break;
            default :
                itm.typeName = itm.getOtherName();
                break;
        }

        // Number of sockets that are filled with items
        itm.numSocketsFilled = (int)parseBits(iReader, itemFirstBit, 3, "FilledSockets", true);
    }
    else {
        itm.earClass = (int)parseBits(iReader, itemFirstBit, 3, "EarClass", true);
        itm.earLevel = (int)parseBits(iReader, itemFirstBit, 7, "EarLevel", true);
        itm.earName = Long.toString(parseBits(iReader, itemFirstBit, 7, "EarName", false));
        System.out.println(String.format("%-25s", itm.earName));

        // If the ear is not byte-aligned, then it needs to
        // be byte-aligned before reading the next property
        // EAR BYTE-ALIGNING NOT IMPLEMENTED YET
    }

    ///////////////////////////
    // END OF SIMPLE SECTION //
    ///////////////////////////

    if (itm.isSimple == 0) {
        // Item Fingerprint is 8 chars, each 4 bits
        itm.fingerprint = Long.toHexString(parseBits(iReader, itemFirstBit, 32, "Fingerprint", false));
        System.out.println(String.format("%-25s", itm.fingerprint));

        itm.iLvl = (int)parseBits(iReader, itemFirstBit, 7, "iLvl", true);

        fieldVal = parseBits(iReader, itemFirstBit, 4, "Quality", false);
        switch ((int)fieldVal) {
            case 1 :
                itm.quality = Item.ItemQuality.LOW;
                break;
            case 3 :
                itm.quality = Item.ItemQuality.HIGH;
                break;
            case 4 :
                itm.quality = Item.ItemQuality.MAGIC;
                break;
            case 5 :
                itm.quality = Item.ItemQuality.SET;
                break;
            case 6 :
                itm.quality = Item.ItemQuality.RARE;
                break;
            case 7 :
                itm.quality = Item.ItemQuality.UNIQUE;
                break;
            case 8 :
                itm.quality = Item.ItemQuality.CRAFTED;
                break;
            default :
                itm.quality = Item.ItemQuality.NORMAL;
                break;
        }
        System.out.println(String.format("%-25s", itm.quality));

        itm.hasMultiplePics = (short)parseBits(iReader, itemFirstBit, 1, "HasMultiplePics", true);

        // The item has multiple pics, so which pic is it?
        if (itm.hasMultiplePics == 1) {
            itm.pictureID = (int)parseBits(iReader, itemFirstBit, 3, "PictureID", true);
        }

        itm.isClassSpecific = (short)parseBits(iReader, itemFirstBit, 1, "IsClassSpecific", true);

        if (itm.isClassSpecific == 1) {
            // TODO learn what these bits represent
            fieldVal = parseBits(iReader, itemFirstBit, 11, "<unknown>", true);
        }

        switch (itm.quality) {
            case LOW :
                // Low Quality
                itm.lowQualityID = (int)parseBits(iReader, itemFirstBit, 3, "LowQualityID", true);
                break;
            case NORMAL :
                // Normal Quality

                // No extra data for normal quality items
                break;
            case HIGH :
                // High Quality
                itm.highQualityID = (int)parseBits(iReader, itemFirstBit, 3, "HighQualityID", true);
                break;
            case MAGIC :
                // Magic

                itm.magicPrefixID = (int)parseBits(iReader, itemFirstBit, 11, "MagicPrefix", false);
                itm.magicPrefixName = itm.getMagicPrefix(itm.magicPrefixID);
                System.out.println(String.format("%-25s", itm.magicPrefixName));

                itm.magicSuffixID = (int)parseBits(iReader, itemFirstBit, 11, "MagicSuffix", false);
                itm.magicSuffixName = itm.getMagicSuffix(itm.magicSuffixID);
                System.out.println(String.format("%-25s", itm.magicSuffixName));
                break;
            case SET :
                // Set
                itm.setID = (int)parseBits(iReader, itemFirstBit, 12, "SetNameID", false);
                itm.setName = itm.getSetName();
                System.out.println(String.format("%-25s", itm.setName));
                break;
            case RARE :
                // Rare
                parseRareBits(iReader, itemFirstBit, itm);
                break;
            case UNIQUE :
                // Unique
                itm.uniqueID = (int)parseBits(iReader, itemFirstBit, 12, "UniqueNameID", false);
                itm.uniqueName = itm.getUniqueName();
                System.out.println(String.format("%-25s", itm.uniqueName));
                break;
            case CRAFTED :
                // Crafted
                parseRareBits(iReader, itemFirstBit, itm);
                break;
        }

        String fieldTitle = "";
        if (itm.isRuneword == 1) {
            if (itm.quality == Item.ItemQuality.NORMAL) {
                fieldTitle = "RunewordNormQual";
            }
            if (itm.quality == Item.ItemQuality.HIGH) {
                fieldTitle = "RunewordHighQual";
            }

            itm.runewordID = (int)parseBits(iReader, itemFirstBit, 16, fieldTitle, false);
            itm.runewordName = Long.toString(itm.runewordID);
            System.out.println(String.format("%-25s", itm.runewordName));
        }

        if (itm.isPersonalized == 1) {
            itm.personalizedName = Long.toString(parseBits(iReader, itemFirstBit, 7, "PersonalizedName", false));
            System.out.println(String.format("%-25s", itm.personalizedName));
        }

        // TODO set isTome when obtaining itemType
        if (itm.isTome == 1) {
            // TODO Not sure what these 5 bits represent
            itm.tomeData = (int)parseBits(iReader, itemFirstBit, 5, "TomeData", true);
        }

        itm.timestamp = (short)parseBits(iReader, itemFirstBit, 1, "Timestamp", true);

        if (itm.type == Item.ItemType.ARMOR || itm.type == Item.ItemType.SHIELD) {
            itm.defenseRating = (int)parseBits(iReader, itemFirstBit, 11, "DefenseRating", true);
        }

        if (itm.type == Item.ItemType.ARMOR || itm.type == Item.ItemType.SHIELD || itm.type == Item.ItemType.WEAPON) {
            itm.maxDurability = (int)parseBits(iReader, itemFirstBit, 8, "MaxDurability", true);

            // Some items have no max durability (e.g. phase blade)
            if (itm.maxDurability > 0) {
                itm.curDurability = (int)parseBits(iReader, itemFirstBit, 8, "CurDurability", true);

                // Unknown extra bit here (check if it refers to broken items)
                fieldVal = parseBits(iReader, itemFirstBit, 1, "<unknown>", true);
            }
        }

        // TODO set isStacked when obtaining itemType
        //itm.isStacked = 1;
        if (itm.isStacked == 1) {
            // Stacked items (e.g. javelins)
            itm.stackedQty = (int)parseBits(iReader, itemFirstBit, 9, "StackQty", true);
        }

                if (itm.isSocketed == 1) {
                    itm.socketQty = (int)parseBits(iReader, itemFirstBit, 4, "SocketQty", true);
                }

                //if (itemQuality == 5) {
                if (itm.quality == Item.ItemQuality.SET) {
                    // For set items; how many lists of magical properties
                    // follow the one regular magical property list
                    itm.setBonusesQty = (int)parseBits(iReader, itemFirstBit, 5, "SetBonusQty", true);
                }

                // Magic Properties (can be present on any item quality type)
                parseMagicProperties(iReader, itemFirstBit);

                // Additional runeword magic properties
                if (itm.isRuneword == 1) {
                    System.out.println("------------------------------------------------------------");
                    System.out.println("Runeword Properties");
                    System.out.println("------------------------------------------------------------");
                    parseMagicProperties(iReader, itemFirstBit);
                }
            }

            System.out.println("------------------------------------------------------------");
            System.out.println();
        }
        else {
            System.out.println("No args provided");
        }
    }
}
