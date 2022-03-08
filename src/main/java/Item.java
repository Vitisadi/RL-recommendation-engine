import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.text.DecimalFormat;

enum DataType {
    PCToXBOX,
    XBOXToPC,
    PCToPSN,
    PSNToPC,
    PSNToXBOX,
    XBOXToPSN
};

public class Item {
    int position;
    String name;
    String color;
    boolean bp;
    transient String itemFullName;
    transient String imgURL;
    transient Integer[] PCPrice = new Integer[2];
    transient Integer[] PSNPrice = new Integer[2];
    transient Integer[] XBOXPrice = new Integer[2];
    int PCMidPrice, PSNMidPrice, XBOXMidPrice;
    int PCToXBOXRate, XBOXToPCRate, PCToPSNRate, PSNToPCRate, PSNToXBOXRate, XBOXToPSNRate;
    int PCToXBOXProfit, XBOXToPCProfit, PCToPSNProfit, PSNToPCProfit, PSNToXBOXProfit, XBOXToPSNProfit;
    int PCToXBOXProfitTotal, XBOXToPCProfitTotal, PCToPSNProfitTotal, PSNToPCProfitTotal, PSNToXBOXProfitTotal, XBOXToPSNProfitTotal;
    int demandHave = -1;
    int demandWant = -1;

    Item(String name, String color, boolean bp){
        this.name = name;
        this.color = color;
        this.bp = bp;
    }

    void findPrice(){
        Integer[] prices = new Integer[6];
        prices = findValue(generateItemURL(), bp);
        PCPrice[0] = prices[0];
        PCPrice[1] = prices[1];
        PSNPrice[0] = prices[2];
        PSNPrice[1] = prices[3];
        XBOXPrice[0] = prices[4];
        XBOXPrice[1] = prices[5];

        PCMidPrice = (PCPrice[0] + PCPrice[1])/2;
        PSNMidPrice = (PSNPrice[0] + PSNPrice[1])/2;
        XBOXMidPrice = (XBOXPrice[0] + XBOXPrice[1])/2;
        findItemFullName();
        findCrossPlatformRelations();
    }

    public int calculateDemandHave(){
        if (demandHave != -1 ) {
            return demandHave;
        }

        WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);

        try {
            HtmlPage page = client.getPage(generateItemURL());
            DomElement tr;
            tr = page.getElementById("sellerOffersContainer");
            DomNode tdHover = tr.getFirstChild();
            // Find next table
            DomNode nodeTable = tdHover.getNextSibling();
            while (!nodeTable.getLocalName().equalsIgnoreCase("table")) {
                nodeTable = nodeTable.getNextSibling();
            }
            DomNode nodeTableBody = nodeTable.getFirstChild();
            DomNode nodeHeaderRow = nodeTableBody.getFirstChild();
            // Scan rows
            DomNode element = nodeHeaderRow.getNextSibling();
            demandHave = 0;
            while (element != null) {
                int credit = Integer.parseInt(element.getFirstChild().getTextContent());
                int amount = Integer.parseInt(element.getFirstChild().getNextSibling().getTextContent());
                demandHave += amount;

                element = element.getNextSibling();
            }
            //System.out.println("Total amount want: " + demandHave);
        }catch(Exception e){
            e.printStackTrace();
        }
        return demandHave;
    }

    public int calculateDemandWant(){
        if (demandWant != -1 ) {
            return demandWant;
        }

        WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);

        try {
            HtmlPage page = client.getPage(generateItemURL());
            DomElement tr;
            tr = page.getElementById("buyerOffersContainer");
            DomNode tdHover = tr.getFirstChild();
            // Find next table
            DomNode nodeTable = tdHover.getNextSibling();
            while (!nodeTable.getLocalName().equalsIgnoreCase("table")) {
                nodeTable = nodeTable.getNextSibling();
            }
            DomNode nodeTableBody = nodeTable.getFirstChild();
            DomNode nodeHeaderRow = nodeTableBody.getFirstChild();
            // Scan rows
            DomNode element = nodeHeaderRow.getNextSibling();
            demandWant = 0;
            while (element != null) {
                int credit = Integer.parseInt(element.getFirstChild().getTextContent());
                int amount = Integer.parseInt(element.getFirstChild().getNextSibling().getTextContent());
                demandWant += amount;

                element = element.getNextSibling();
            }
            //System.out.println("Total amount have: " + demandWant);
        }catch(Exception e){
            e.printStackTrace();
        }
        return demandWant;
    }

    Integer[] findValue(String searchUrl, boolean blueprint){
        WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        Integer[] intPrices = new Integer[6]; // 0-1 PC 2-3 PSN 4-5 Xbox  / Even - Low / Odd - High
        try {
            HtmlPage page = client.getPage(searchUrl);
            DomElement tr;
            if(!blueprint) { //item
                tr = page.getElementById("matrixRow0");
            } else { //blueprint
                tr = page.getElementById("matrixRow3");
            }
            DomNode tdHover = tr.getFirstChild();
            DomNode tdPrice1 = tdHover.getNextSibling();

            intPrices[0] = valueToInt(tdPrice1.getFirstChild().getTextContent())[0];
            intPrices[1] = valueToInt(tdPrice1.getFirstChild().getTextContent())[1];
            intPrices[2] = valueToInt(tdPrice1.getNextSibling().getFirstChild().getTextContent())[0];
            intPrices[3] = valueToInt(tdPrice1.getNextSibling().getFirstChild().getTextContent())[1];
            intPrices[4] = valueToInt(tdPrice1.getNextSibling().getNextSibling().getFirstChild().getTextContent())[0];
            intPrices[5] = valueToInt(tdPrice1.getNextSibling().getNextSibling().getFirstChild().getTextContent())[1];
        }catch(Exception e){
            e.printStackTrace();
        }
        return intPrices;
    }

    String generateItemURL(){
        String link;
        if(color.equalsIgnoreCase("none")){
            link = "https://rl.insider.gg/en/pc/" + name;
        } else {
            link = "https://rl.insider.gg/en/pc/" + name + "/" + color;
        }
        return link;
    }

    Integer[] valueToInt(String stringPrice){
        String[] prices = new String[2];
        Integer[] intPrices = new Integer[2];
        if(stringPrice.contains("k")){
            int location = stringPrice.indexOf(" k");
            stringPrice = stringPrice.substring(0, location);
            prices = stringPrice.split(" - ");
            if(prices[0].contains(".")){
                String start = prices[0].substring(0, prices[0].indexOf("."));
                String end = prices[0].substring(prices[0].indexOf(".") + 1);
                prices[0] = start + end;
                prices[0] += "00";
            } else {
                prices[0] += "000";
            }
            if(prices[1].contains(".")){
                String start = prices[1].substring(0, prices[1].indexOf("."));
                String end = prices[1].substring(prices[1].indexOf(".") + 1);
                prices[1] = start + end;
                prices[1] += "00";
            } else {
                prices[1] += "000";
            }
        } else {
            prices = stringPrice.split(" - ");
        }
        try {
            intPrices[0] = Integer.parseInt(prices[0]);
            intPrices[1] = Integer.parseInt(prices[1]);
        } catch(NumberFormatException e) {
            System.out.println("Error Converting String to Int: " + prices[0] + " or " + prices[1]);
        }

        return intPrices;

//        String[] prices = new String[2];
//        Integer[] intPrices = new Integer[2];
//        if(stringPrice.contains("k")){
//            int location = stringPrice.indexOf(" k");
//            stringPrice = stringPrice.substring(0, location);
//            if(stringPrice.contains(".")){
//                while(stringPrice.contains(".")) {
//                    String start = stringPrice.substring(0, stringPrice.indexOf("."));
//                    String end = stringPrice.substring(stringPrice.indexOf(".") + 1);
//                    stringPrice = start + end;
//                }
//                prices = stringPrice.split(" - ");
//                prices[0] += "00";
//                prices[1] += "00";
//            } else {
//                prices = stringPrice.split(" - ");
//                prices[0] += "000";
//                prices[1] += "000";
//            }
//        } else {
//            prices = stringPrice.split(" - ");
//        }
//        try {
//            intPrices[0] = Integer.parseInt(prices[0]);
//            intPrices[1] = Integer.parseInt(prices[1]);
//        } catch(NumberFormatException e) {
//            System.out.println("Error Converting String to Int: " + prices[0] + " or " + prices[1]);
//        }
//
//        return intPrices;
    }

    public String getImageUrl(){
        if (imgURL != null) {
            return imgURL;
        }

        WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        String imageLink;
        try {
            HtmlPage page = client.getPage(generateItemURL());
            DomElement tr;
            tr = page.getElementById("itemSummaryImage");
            DomNode tdHover = tr.getFirstChild();
            DomNode tdURL = tdHover.getNextSibling();
            String[] iURL = tdURL.toString().split("\"");
            imgURL = iURL[1];
        }catch(Exception e){
            e.printStackTrace();
        }
        return imgURL;
    }

    void findCrossPlatformRelations(){
        DecimalFormat df = new DecimalFormat("0.00");
        PCToXBOXRate = (int)Math.round(XBOXMidPrice * 100.0/ PCMidPrice);
        XBOXToPCRate = (int)Math.round(PCMidPrice * 100.0  / XBOXMidPrice);
        PCToPSNRate = (int)Math.round(PSNMidPrice * 100.0 / PCMidPrice);
        PSNToPCRate = (int)Math.round(PCMidPrice * 100.0 / PSNMidPrice);
        PSNToXBOXRate = (int)Math.round(XBOXMidPrice * 100.0 / PSNMidPrice);
        XBOXToPSNRate = (int)Math.round(PSNMidPrice * 100.0 / XBOXMidPrice);
//        PCToXBOXRate = Float.parseFloat(df.format((float) XBOXMidPrice / PCMidPrice));
//        XBOXToPCRate = Float.parseFloat(df.format((float) PCMidPrice / XBOXMidPrice));
//        PCToPSNRate = Float.parseFloat(df.format((float) PSNMidPrice / PCMidPrice));
//        PSNToPCRate = Float.parseFloat(df.format((float) PCMidPrice / PSNMidPrice));
//        PSNToXBOXRate = Float.parseFloat(df.format((float) XBOXMidPrice / PSNMidPrice));
//        XBOXToPSNRate = Float.parseFloat(df.format((float) PSNMidPrice / XBOXMidPrice));

        PCToXBOXProfit = XBOXMidPrice - PCMidPrice;
        XBOXToPCProfit = PCMidPrice - XBOXMidPrice;
        PCToPSNProfit = PSNMidPrice - PCMidPrice;
        PSNToPCProfit = PCMidPrice - PSNMidPrice;
        PSNToXBOXProfit = XBOXMidPrice - PSNMidPrice;
        XBOXToPSNProfit = PSNMidPrice - XBOXMidPrice;
    }

    void findItemFullName(){
        String blueprint;
        if(bp){
            blueprint = "Blueprint";
        } else {
            blueprint = "";
        }
        itemFullName = color + " " + name + " " + blueprint;
    }

    public Integer getRateByType(DataType dataType) {
        switch (dataType) {
            case PCToXBOX:
                return PCToXBOXRate;
            case XBOXToPC:
                return XBOXToPCRate;
            case PCToPSN:
                return PCToPSNRate;
            case PSNToPC:
                return PSNToPCRate;
            case PSNToXBOX:
                return PSNToXBOXRate;
            case XBOXToPSN:
                return XBOXToPSNRate;
            default:
                System.out.println("Error Finding rate type :( - " + dataType);
        }
        return -1;
    }

    public int getProfitByType(DataType dataType) {
        switch (dataType) {
            case PCToXBOX:
                return PCToXBOXProfit;
            case XBOXToPC:
                return XBOXToPCProfit;
            case PCToPSN:
                return PCToPSNProfit;
            case PSNToPC:
                return PSNToPCProfit;
            case PSNToXBOX:
                return PSNToXBOXProfit;
            case XBOXToPSN:
                return XBOXToPSNProfit;
            default:
                System.out.println("Error Finding profit type - " + dataType);
        }
        return -1;
    }

    public int getProfitTotalByType(DataType dataType) {
        switch (dataType) {
            case PCToXBOX:
                return PCToXBOXProfitTotal;
            case XBOXToPC:
                return XBOXToPCProfitTotal;
            case PCToPSN:
                return PCToPSNProfitTotal;
            case PSNToPC:
                return PSNToPCProfitTotal;
            case PSNToXBOX:
                return PSNToXBOXProfitTotal;
            case XBOXToPSN:
                return XBOXToPSNProfitTotal;
            default:
                System.out.println("Error Finding profit type (get) - " + dataType);
        }
        return -1;
    }

    public void setProfitTotalByType(DataType dataType, int profit) {
        switch (dataType) {
            case PCToXBOX:
                PCToXBOXProfitTotal = profit;
                break;
            case XBOXToPC:
                XBOXToPCProfitTotal = profit;
                break;
            case PCToPSN:
                PCToPSNProfitTotal = profit;
                break;
            case PSNToPC:
                PSNToPCProfitTotal = profit;
                break;
            case PSNToXBOX:
                PSNToXBOXProfitTotal = profit;
                break;
            case XBOXToPSN:
                XBOXToPSNProfitTotal = profit;
                break;
            default:
                System.out.println("Error Finding profit type (set) - " + dataType);
        }
    }

    public String getPlatform(DataType typePlatform){
        if(typePlatform == DataType.PCToXBOX){
            return "PCToXBOX";
        } else if(typePlatform == DataType.XBOXToPC){
            return "XBOXToPC";
        } else if(typePlatform == DataType.PCToPSN){
            return "PCToPSN";
        } else if(typePlatform == DataType.PSNToPC){
            return "PSNToPC";
        } else if(typePlatform == DataType.PSNToXBOX){
            return "PSNToXBOX";
        } else if(typePlatform == DataType.XBOXToPSN){
            return "XBOXToPSN";
        } else {
            System.out.println("Error Finding platform type (get) - " + typePlatform);
        }
        return "";
    }

    void print(){
        System.out.println("Item: " + color + " " + name);
        System.out.println("PC: " + PCMidPrice);
        System.out.println("PSN: " + PSNMidPrice);
        System.out.println("XBOX: " + XBOXMidPrice);

        System.out.println(" ");

        System.out.println("PC to XBOX Rate: " + PCToXBOXRate);
        System.out.println("XBOX to PC Rate: " + XBOXToPCRate);

        System.out.println(" ");

        System.out.println("PC to PSN Rate: " + PCToPSNRate);
        System.out.println("PSN to PC Rate: " + PSNToPCRate);

        System.out.println(" ");

        System.out.println("PSN to XBOX Rate: " + PSNToXBOXRate);
        System.out.println("XBOX to PSN Rate: " + XBOXToPSNRate);
    }
}
