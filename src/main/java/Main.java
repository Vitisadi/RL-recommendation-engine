import com.thoughtworks.xstream.XStream;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class Main extends ListenerAdapter {
    public static void main(String[] args) throws LoginException, FileNotFoundException {
        long start = System.nanoTime();
        JDABuilder builder = new JDABuilder(AccountType.BOT);
        String token = "";
        JDA jda = JDABuilder.createDefault(token).build();
        jda.addEventListener(new Main());

        DataType typePlatform = DataType.PCToXBOX;
        String sortBy = "All"; //Rate / Profit / DemandHave / DemandWant / All

        ArrayList<Item> itemsArrayList = new ArrayList<>();
        //itemsArrayList = loadFromXml(typePlatform, sortBy);

        start = System.nanoTime();
        scrapeItems(itemsArrayList, start);
        System.out.println("Scraped in: " + ((System.nanoTime() - start)/1000000000) + "s");

        start = System.nanoTime();
        //orderItems(itemsArrayList, typePlatform, sortBy);
        orderItems(itemsArrayList, DataType.PCToXBOX, sortBy);
        orderItems(itemsArrayList, DataType.XBOXToPC, sortBy);
        orderItems(itemsArrayList, DataType.PCToPSN, sortBy);
        orderItems(itemsArrayList, DataType.PSNToPC, sortBy);
        orderItems(itemsArrayList, DataType.PSNToXBOX, sortBy);
        orderItems(itemsArrayList, DataType.XBOXToPSN, sortBy);
        System.out.println("Sorted in: " + ((System.nanoTime() - start)/10000000) + "s");
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (event.getAuthor().isBot()) {
            return;
        }


        System.out.println("We received a message from " +
                event.getAuthor().getName() + ": " +
                event.getMessage().getContentDisplay()
        );

        if (!event.getMessage().getContentRaw().startsWith("!")) {
            return;
        }
        String[] args = event.getMessage().getContentRaw().split(" ");

        if (event.getMessage().getContentRaw().startsWith("!price")) {
            priceCommand(event, args);
        }
    }

    void priceCommand(MessageReceivedEvent event, String[] args) {
        if (args.length != 4) {
            event.getChannel().sendMessage("Making sure you have written in all 3 parameters").queue();
            event.getChannel().sendMessage("Example: !price Octane White Item").queue();
        } else {
            String name = args[1];
            String color = args[2];
            boolean bp = !args[3].equalsIgnoreCase("item");
            Item item = new Item(name, color, bp);
            item.findPrice();
            //item.print();
            String type;
            if (bp) {
                type = "Blueprint";
            } else {
                type = "Item";
            }
            embedMsg(event,
                    item.itemFullName,
                    String.valueOf(item.PCMidPrice),
                    String.valueOf(item.PSNMidPrice),
                    String.valueOf(item.XBOXMidPrice),
                    item.getImageUrl(),
                    item.calculateDemandHave());
        }
    }

    public static void embedMsg(MessageReceivedEvent event, String itemFullName, String PCPrice, String PSNPrice, String XBOXPRice, String imgURL, int demand) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(itemFullName.toUpperCase(), null);
        eb.setColor(new Color(68, 168, 50));

        eb.addField("PC", PCPrice, true);
        eb.addField("PSN", PSNPrice, true);
        eb.addField("XBOX", XBOXPRice, true);
        eb.addField("demand", String.valueOf(demand), true);

        eb.setFooter(time(), null);
        eb.setImage(imgURL);

        event.getChannel().sendMessage(eb.build()).queue();
    }

    public static void scrapeItems(ArrayList<Item> itemsArrayList, long start) throws FileNotFoundException {
        Scanner itemFile = new Scanner(new File("src/main/resources/items.txt"));
        //Scanner itemFile = new Scanner(new File("src/main/resources/testItems.txt"));
        while (itemFile.hasNextLine()) {
            String rawLine = itemFile.nextLine();
            if (rawLine.contains("#")) {
                continue;
            }
            String[] lineArgs = rawLine.split(", ");

            if (lineArgs[1].equalsIgnoreCase("All")) {
                String[] allPossibleItemColors = {"None", "Black", "White", "Grey", "Crimson", "Pink", "Cobalt", "sBlue", "Sienna", "Saffron", "Lime", "fGreen", "Orange", "Purple"};
                ArrayList<String> colorExceptions = new ArrayList<>();
                if (lineArgs.length > 2) {
                    colorExceptions.addAll(Arrays.asList(lineArgs).subList(3, lineArgs.length));
                }
                boolean addItem = true;
                for (String color : allPossibleItemColors) {
                    for (String exceptions : colorExceptions) {
                        if (exceptions.equalsIgnoreCase(color)) {
                            addItem = false;
                            break;
                        } else {
                            addItem = true;
                        }
                    }
                    if (addItem) {
                        itemsArrayList.add(new Item(lineArgs[0], color, Boolean.parseBoolean(lineArgs[2])));
                    }
                }
            } else {
                itemsArrayList.add(new Item(lineArgs[0], lineArgs[1], Boolean.parseBoolean(lineArgs[2])));
            }
        }
        System.out.println("Loaded list: " + ((System.nanoTime() - start) / 1000000) + "s");

        start = System.nanoTime();
        for (Item item : itemsArrayList) {
            item.findPrice();
            item.calculateDemandHave();
            item.calculateDemandWant();
            //System.out.println(item.itemFullName + " complete");
        }
    }

    public static void orderItems(final ArrayList<Item> itemsArrayList, final DataType typePlatform, String sortBy) {
        int profitMinimum;
        int rateMinimum;
        int demandPreferable;
        int demandMinimum;
        if (sortBy.equalsIgnoreCase("All")) {
            if(itemsArrayList.get(0).getPlatform(typePlatform).equals("PCToXBOX") || itemsArrayList.get(0).getPlatform(typePlatform).equals("PCToPSN")) {
                profitMinimum = 1000;
                rateMinimum = 130;
                demandPreferable = 30;
                demandMinimum = 10;
            } else if(itemsArrayList.get(0).getPlatform(typePlatform).equals("XBOXToPC") || itemsArrayList.get(0).getPlatform(typePlatform).equals("PSNToPC")){
                profitMinimum = -10000;
                rateMinimum = 90;
                demandPreferable = 30;
                demandMinimum = 10;
            } else if(itemsArrayList.get(0).getPlatform(typePlatform).equals("PSNToXBOX") || itemsArrayList.get(0).getPlatform(typePlatform).equals("XBOXToPSN")){
                profitMinimum = 1000;
                rateMinimum = 130;
                demandPreferable = 30;
                demandMinimum = 10;
            } else {
                profitMinimum = -1;
                rateMinimum = -1;
                demandPreferable = -1;
                demandMinimum = -1;
                System.out.println("Error setting order amounts" + itemsArrayList.get(0).getPlatform(typePlatform));
            }
            int profitTotal = 0;
            int profitRate = 0;
            int profitDemandAmount = 0;
            int profitDemandDifference = 0;
            for (Item item : itemsArrayList) {
                if (item.getProfitByType(typePlatform) >= profitMinimum) {
                    profitTotal = item.getProfitByType(typePlatform);
                } else {
                    profitTotal = -100000;
                }
                if (item.getRateByType(typePlatform) >= rateMinimum) {
                    profitRate = profitTotal;
                   } else {
                    profitRate = -100000;
                }
                if (item.demandHave >= demandPreferable && item.demandWant >= demandPreferable) {
                    profitDemandAmount = profitRate * 3 / 2;
                } else if (item.demandHave >= demandMinimum && item.demandWant >= demandMinimum) {
                    profitDemandAmount = profitRate;
                } else {
                    profitDemandAmount = -100000;
                }
                if (item.demandHave * 2.00 >= item.demandWant && item.demandWant * 2.00 >= item.demandHave) {
                    profitDemandDifference = profitDemandAmount;
                } else {
                    profitDemandDifference = -100000;
                }
                item.setProfitTotalByType(typePlatform, profitDemandDifference);
            }
            long start = System.nanoTime();
            sortItems(itemsArrayList, typePlatform, "profitTotal");
            System.out.println("Sorted index : " + ((System.nanoTime() - start) / 1000) + "ms");
            } else {
                sortItems(itemsArrayList, typePlatform, sortBy);
            }

        for(int i = 0; i < itemsArrayList.size(); i++){
            itemsArrayList.get(i).position = i;
        }

        saveAsXml(typePlatform, sortBy, itemsArrayList);
    }

    public static void saveAsXml(DataType typePlatform, String sortBy, ArrayList<Item> itemsArrayList) {
        XStream xstream = new XStream();
        xstream.alias("item", Item.class);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("src/main/resources/itemTracking/" + typePlatform + "_" + sortBy + ".xml");
            xstream.toXML(itemsArrayList, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Item> loadFromXml(DataType typePlatform, String sortBy) {
        XStream xstream = new XStream();
        xstream.alias("item", Item.class);
        FileReader fileReader = null;
        try {
            fileReader = new FileReader("src/main/resources/itemTracking/" + typePlatform + "_" + sortBy + ".xml");
            Object result = xstream.fromXML(fileReader);
            return (ArrayList<Item>)result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean compareToMore(Item item1, Item item2, DataType typePlatform, String typeMethod) {
        if(typeMethod.equalsIgnoreCase("profitTotal")){
            return item1.getProfitTotalByType(typePlatform) >= item2.getProfitTotalByType(typePlatform);
        } else if (typeMethod.equalsIgnoreCase("Rate")) {
            return item1.getRateByType(typePlatform) >= item2.getRateByType(typePlatform);
        } else if (typeMethod.equalsIgnoreCase("Profit")) {
            return item1.getProfitByType(typePlatform) >= item2.getProfitByType(typePlatform);
        } else if(typeMethod.equalsIgnoreCase("demandHave")){
            return item1.demandHave >= item2.demandHave;
        } else if(typeMethod.equalsIgnoreCase("demandWant")){
            return item1.demandWant >= item2.demandWant;
        } else {
            System.out.println("Method sorting for not found");
        }
        return false;
    }

    public static void sortItems(ArrayList<Item> items, DataType typePlatform, String sortBy) {
        for (int i = 0; i < items.size() - 1; i++) {
            int highestIndex = i;
            for (int j = i + 1; j < items.size(); j++) {
                if (compareToMore(items.get(j), items.get(highestIndex), typePlatform, sortBy)) {
                    highestIndex = j;
                }
            }

            if (highestIndex != i) {
                Item temp = items.get(i);
                items.set(i, items.get(highestIndex));
                items.set(highestIndex, temp);
            }
        }
    }

    public static String time() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return (dtf.format(now));
    }
}

