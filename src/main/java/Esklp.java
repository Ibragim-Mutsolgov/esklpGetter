/*
 * Copyright (c) 2022.
 */

// This service collect medicine data from Minzdrav resource and write to database

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.Properties;
import net.lingala.zip4j.exception.ZipException;

@Slf4j
public class Esklp {

    private final static Properties config = new Properties();
    private static String fullFileListRequestURL = "https://esklp.egisz.rosminzdrav.ru/fs/public/api/esklp?exportType=full&exportFormat=XML&sorting=createTimestamp__desc&page=1&per_page=1";
    private static String downloadRequestURL = "https://esklp.egisz.rosminzdrav.ru/fs/public/api/esklp/download/";
    private static Calendar cl;

    public static void main(String[] args) {
        /*cl = GregorianCalendar.getInstance();
        try {
            log.info( cl.getTime().toString().concat(" - Service started."));
            loadConfig(); //Не работает. Не знаю нужно ли//Load configuration
        } catch (IOException e) {
            log.error(cl.getTime().toString().concat(e.getMessage()));
        }
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder()
                .url(fullFileListRequestURL)
                .method("GET", null)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();  // Request file list
        } catch (IOException e) {
            log.error(cl.getTime().toString().concat(e.getMessage()));
        }
        JSONObject jo = null;
        try {
            assert response != null;
            jo = new JSONObject(Objects.requireNonNull(response.body()).string());
        } catch (IOException e) {
            log.error(cl.getTime().toString().concat(e.getMessage()));
        }
        //Parse responsed JSON
        assert jo != null;
        String fileId = jo.getJSONArray("results").getJSONObject(0).get("fileId").toString();
        String fileName = jo.getJSONArray("results").getJSONObject(0).get("fileName").toString();
        String fileExt = jo.getJSONArray("results").getJSONObject(0).get("fileExt").toString();
        request = new Request.Builder()
                .url(downloadRequestURL.concat(fileId))
                .method("GET", null)
                .build();
        try {
            response = client.newCall(request).execute(); // Download file
        } catch (IOException e) {
            log.error(cl.getTime().toString().concat(e.getMessage()));
        }

        //Save downloaded file
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName.concat(".").concat(fileExt));
        } catch (FileNotFoundException e) {
            log.error(cl.getTime().toString().concat(e.getMessage()));
        }
        try {
            assert fos != null;
            fos.write(Objects.requireNonNull(response.body()).bytes());
            fos.close();
        } catch (IOException e) {
            log.error(cl.getTime().toString().concat(e.getMessage()));
        }
        //Unzip downloaded archived file
        try {
            new ZipFile(fileName.concat(".").concat(fileExt)).extractAll(".");
        } catch ( ZipException e) {
            log.error(cl.getTime().toString().concat(e.getMessage()));
        }*/
        //Parse data and write to database
        parseAndWriteToDB();
    }

    private static void loadConfig() throws IOException {
        FileInputStream in = new FileInputStream("config.properties");
        config.load(in);
        fullFileListRequestURL = config.getProperty("fullFileListRequestURL");
        //String incrementalFileListRequestURL = config.getProperty("incrementalFileListRequestURL");
        downloadRequestURL = config.getProperty("downloadRequestURL");
//        String dbURL = config.getProperty("dbURL");
//        String dbHost = config.getProperty("dbHost");
//        String dbPort = config.getProperty("dbPort");
//        String dbName = config.getProperty("dbName");
        //String dbUser = config.getProperty("dbUser");
        //String dbPassword = config.getProperty("dbPassword");
        in.close();
    }

    private static void parseAndWriteToDB() {
        //String url = dbURL.concat(dbHost).concat(":").concat(dbPort).concat("/").concat(dbName);
        try {
//          Connection con = DriverManager.getConnection(url, dbUser, dbPassword);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new FileInputStream("esklp_20220322_full_21.5_00002_0001.xml"));

            doc.getDocumentElement().normalize();
            Node rootElement = doc.getDocumentElement();

            // Read attributes
            int attributes = rootElement.getAttributes().getLength();
            if (attributes > 0) {
                for (int i = 0; i < attributes; i++) {
                    System.out.print(rootElement.getAttributes().item(i).getNodeName());
                    System.out.print(" - ");
                    System.out.println(rootElement.getAttributes().item(i).getNodeValue());
                }
            }

            System.out.println(doc.getChildNodes().item(0).getChildNodes());//ESKLP
            System.out.println(doc.getElementsByTagName("ns2:group_list").getLength());

            for(int i=0; i<doc.getElementsByTagName("ns2:group_list").getLength(); i++) {//Первый тег//Список всех group_list
                //System.out.println("Номер " + i);
                //System.out.println(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes());
                String mnn = "";
                String mnn_name = "";
                String form = "";
                String dosage_user_name = "";
                String dosage_num = "";
                String dosage_unit = "";
                String pack1_name = "";
                String pack1_num = "";
                String pack2_name = "";
                String pack2_num = "";
                for (int j = 0; j < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().getLength(); j++) {//Список всех дочерних элементов
                    //System.out.println(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes() + " - " + j);
                    //System.out.println("    Аттрибуты");
                    for (int k = 0; k < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getAttributes().getLength(); k++) {//Список всех аттрибутов
                        //Вводим значение, если есть
                        //System.out.print("      "+doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getAttributes().item(k).getNodeName());
                        //System.out.print(" - ");
                        //System.out.println(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getAttributes().item(k).getNodeValue());
                    }
                    for (int t = 0; t < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().getLength(); t++) {//Список всех тегов
                        if ("ns2:group_list".equals(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getNodeName())) {
                            break;
                        }
                        //System.out.println("["+doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getNodeName()+"]");
                        //System.out.println("    Значения");
                        //Выводится тег date_changed и smnn
                        //У smnn тега есть вложенные теги, соответственно, необходимо добавить следующий вложенный цикл
                        if(!doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(0).toString().startsWith("[#text")) {//проверка есть ли вложенный тип
                            for (int h = 0; h < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().getLength(); h++) {
                                if (doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getAttributes() != null) {
                                    //System.out.println("      " + doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes());
                                    //System.out.println("        ->Аттрибуты");
                                    for (int a = 0; a < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getAttributes().getLength(); a++) {
                                        //System.out.print("          " + doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getAttributes().item(a).getNodeName());
                                        //System.out.print(" - ");
                                        //System.out.println(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getAttributes().item(a).getNodeValue());

                                    }
                                }
                                //System.out.println("        ->Значения");
                                for (int a = 0; a < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().getLength(); a++) {
                                    //Тут отслеживаем mnn
                                    if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getNodeName().equals("ns2:mnn")){
                                        //System.out.println("Отслеживаем mnn");
                                        mnn = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getTextContent();
                                    }
                                    //Тут отслеживаем form
                                    if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getNodeName().equals("ns2:form")){
                                        //System.out.println("Отслеживаем form");
                                        form = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getTextContent();
                                    }
                                    //System.out.print("          "+doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getNodeName());
                                    //System.out.print(" - ");
                                    //System.out.println(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getTextContent());
                                    for (int y = 0; y < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().getLength(); y++) {//Вложенный цикл для ns2:klp_list
                                        if (doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getAttributes() != null) {
                                            //System.out.println("            ->" + doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getNodeName());
                                            for (int y1 = 0; y1 < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getAttributes().getLength(); y1++) {
                                                if(y1==0){
                                                    //System.out.println("              ->->Аттрибуты");
                                                }
                                                //System.out.print("                " + doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getAttributes().item(y1).getNodeName());
                                                //System.out.print(" - ");
                                                //System.out.println(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getAttributes().item(y1).getNodeValue());
                                            }
                                        }
                                        for (int y1 = 0; y1 < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().getLength(); y1++) {
                                            if(y1==0){
                                                //System.out.println("              ->->Значения");
                                            }
                                            if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getNodeName().startsWith("#text")){
                                                //System.out.print("                "+doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getNodeName());//Тут отслеживаем mnn_name
                                                if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getNodeName().equals("ns2:mnn_name")){
                                                    //System.out.println("Отслеживаем mnn_name");
                                                    mnn_name = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getTextContent();
                                                }
                                                //Тут отслеживаем dosage_unit -> name
                                                if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getNodeName().equals("ns2:name") &
                                                        doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getNodeName().equals("ns2:dosage_unit")){
                                                    //System.out.println("Отслеживаем dosage_unit -> name");
                                                    dosage_unit = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getTextContent();
                                                }
                                                //Тут отслеживаем dosage_num
                                                if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getNodeName().equals("ns2:dosage_num")){
                                                    //System.out.println("Отслеживаем dosage_num");
                                                    dosage_num = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getTextContent();
                                                }
                                                //Тут отслеживаем dosage_user -> name
                                                if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getNodeName().equals("ns2:dosage_num") &
                                                        doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getNodeName().equals("ns2:dosage_user")){
                                                    //System.out.println("Отслеживаем dosage_user -> name");
                                                    dosage_user_name = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getTextContent();
                                                }
                                            }else{
                                                //System.out.print("                "+doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getNodeName());
                                                //Тут отслеживаем mnn_name
                                                if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getNodeName().equals("ns2:mnn_name")){
                                                    //System.out.println("Отслеживаем mnn_name");
                                                    mnn_name = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getTextContent();
                                                }
                                                //Тут отслеживаем dosage_unit -> name
                                                if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getNodeName().equals("ns2:name") &
                                                        doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getNodeName().equals("ns2:dosage_unit")){
                                                    //System.out.println("Отслеживаем dosage_unit -> name");
                                                    dosage_unit = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getTextContent();
                                                }
                                                //Тут отслеживаем dosage_num
                                                if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getNodeName().equals("ns2:dosage_num")){
                                                    //System.out.println("Отслеживаем dosage_num");
                                                    dosage_num = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getTextContent();
                                                }
                                                //Тут отслеживаем dosage_user -> name
                                                if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getNodeName().equals("ns2:name") &
                                                        doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getNodeName().equals("ns2:dosage_user")){
                                                    //System.out.println("Отслеживаем dosage_user -> name");
                                                    dosage_user_name = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getTextContent();
                                                }
                                            }
                                            //System.out.print(" - ");
                                            //System.out.println(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getTextContent());
                                            for(int u=0; u < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().getLength(); u++){
                                                if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getAttributes() != null) {
                                                    for (int u1 = 0; u1 < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getAttributes().getLength(); u1++) {//Аттрибуты
                                                        if(u1==0){
                                                            //System.out.println("                ->->->Аттрибуты");
                                                        }
                                                        //System.out.print("                  " + doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getAttributes().item(u1).getNodeName());
                                                        //System.out.print(" - ");
                                                        //System.out.println(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getAttributes().item(u1).getNodeValue());
                                                    }
                                                }
                                                for(int u1=0; u1 < doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getChildNodes().getLength(); u1++){//Значения
                                                    if(u1==0){
                                                        //System.out.println("                ->->->Значения");
                                                    }
                                                    //Тут отслеживаем ns2:name - pack1
                                                    if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getNodeName().equals("ns2:name") &
                                                            doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getNodeName().equals("ns2:pack_1")){
                                                        //System.out.println("Отслеживаем pack1_name");
                                                        pack1_name = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getChildNodes().item(u1).getTextContent();
                                                    }
                                                    //Тут отслеживаем ns2:num - pack1
                                                    if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getNodeName().equals("ns2:num") &
                                                            doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getNodeName().equals("ns2:pack_1")){
                                                        //System.out.println("Отслеживаем pack1_num");
                                                        pack1_num = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getChildNodes().item(u1).getTextContent();
                                                    }
                                                    //Тут отслеживаем ns2:name - pack2
                                                    if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getNodeName().equals("ns2:name") &
                                                            doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getNodeName().equals("ns2:pack_2")){
                                                        //System.out.println("Отслеживаем pack2_name");
                                                        pack2_name = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getChildNodes().item(u1).getTextContent();
                                                    }
                                                    //Тут отслеживаем ns2:num - pack2
                                                    if(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getNodeName().equals("ns2:num") &
                                                            doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getNodeName().equals("ns2:pack_2")){
                                                        //System.out.println("Отслеживаем pack2_num");
                                                        pack2_num = doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getChildNodes().item(u1).getTextContent();
                                                    }
                                                    //System.out.print("                  "+doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getNodeName());
                                                    //System.out.print(" - ");
                                                    //System.out.println(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getChildNodes().item(h).getChildNodes().item(a).getChildNodes().item(y).getChildNodes().item(y1).getChildNodes().item(u).getChildNodes().item(u1).getTextContent());
                                                }

                                                Connection con = DriverManager.getConnection("jdbc:postgresql://172.16.3.0:5432/postgres", "postgres", "SSDbuyitCheap");
                                                Statement statement = con.createStatement();

                                                //Проверяем есть в базе наименование
                                                ResultSet set = statement.executeQuery(
                                                        "select id from ClientDrugs where grls='" + mnn + "'"
                                                );
                                                int id = 0;
                                                while(set.next()){
                                                    id = Integer.parseInt(set.getString("id"));
                                                }
                                                if(id == 0){//данных нет
                                                    statement.execute(
                                                            "insert into ClientDrugs (grls) values ('"+mnn+"')"
                                                    );
                                                    ResultSet set2 = statement.executeQuery(
                                                            "select id from ClientDrugs where grls='" + mnn + "'"
                                                    );
                                                    int id2 = 0;
                                                    while(set2.next()){
                                                        id2 = Integer.parseInt(set2.getString("id"));
                                                    }
                                                    if(id2 == 0) {
                                                        statement.execute(
                                                                "insert into Settings (id, mnn, form, dosage_user, dosage_value, dosage_unit, pack1_name, pack1_num, pack2_name, pack2_num) values " +
                                                                        "('" + id2 + "', '" + mnn_name + "', '" + form + "', '" + dosage_user_name + "', '" + dosage_num + "', '" + dosage_unit + "'," +
                                                                        "'" + pack1_name + "', '" + pack1_num + "', '" + pack2_name + "', '" + pack2_num + "')"
                                                        );
                                                    }
                                                    set2.close();
                                                }else{//Данные есть
                                                    //Проверка есть ли похожие значения
                                                    ResultSet set3 = statement.executeQuery(
                                                            "select id from Settings where mnn='"+mnn_name+"' and form='"+form+"'and dosage_user='"+dosage_user_name+"'and" +
                                                                    " dosage_value='"+dosage_num+"'and dosage_unit='"+dosage_unit+"'and pack1_name='"+pack1_name+"' and " +
                                                                    "pack1_num='"+pack1_num+"'and pack2_name='"+pack2_name+"'and pack2_num='"+pack2_num+"'"
                                                    );
                                                    int id3 = 0;
                                                    while(set3.next()){
                                                        id3 = Integer.parseInt(set3.getString("id"));
                                                    }
                                                    if(id3 == 0){
                                                        statement.execute(
                                                                "insert into Settings (id, mnn, form, dosage_user, dosage_value, dosage_unit, pack1_name, pack1_num, pack2_name, pack2_num) values " +
                                                                        "('"+id+"', '"+mnn_name+"', '"+form+"', '"+dosage_user_name+"', '"+dosage_num+"', '"+dosage_unit+"'," +
                                                                        "'"+pack1_name+"', '"+pack1_num+"', '"+pack2_name+"', '"+pack2_num+"')"
                                                        );
                                                    }
                                                    set3.close();
                                                }
                                                set.close();
                                                statement.close();
                                                con.close();
                                            }
                                        }
                                    }
                                }
                            }
                        }else{
                            //System.out.print("      "+doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getNodeName());
                            //System.out.print(" - ");
                            //System.out.println(doc.getElementsByTagName("ns2:group_list").item(i).getChildNodes().item(j).getChildNodes().item(t).getTextContent());
                        }
                    }
                }
                //System.out.println();
            }
            for(int i=0; i<doc.getElementsByTagName("ns2:group").getLength(); i++){
                System.out.print(doc.getElementsByTagName("ns2:group").item(i).getAttributes().item(0).getNodeName());
                System.out.print(" - ");
                System.out.println(doc.getElementsByTagName("ns2:group").item(i).getAttributes().item(0).getNodeValue());
            }
        } catch ( ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
          log.error(cl.getTime().toString().concat(e.getMessage()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
