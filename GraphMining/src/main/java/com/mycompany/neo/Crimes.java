package com.mycompany.neo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.neo4j.driver.v1.*;
import static org.neo4j.driver.v1.Values.parameters;

/**
 * @author merth
 */
public class Crimes {

    Driver driver;
    int KeyRelationNum;
    int nonKeyRelationNum;
    int KeyNum;
    int nonKeyNum; 
    int K;
    int NK;
    double supp;
    double conf;
    List list = new ArrayList();
    List listCount = new ArrayList();
    List controlList = new ArrayList();
    static double time1;
    static double time2;
    String ruleKey = "Arrested";
    String nonKey = "Not Arrested";
    String ruleFile = "CrimeNodes.csv";
    int TotalKey = 0;

    public Crimes(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    private void addNode() {
        try (Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {

                tx.run("MERGE (a:Arrest {name:'Arrested'})");
                tx.run("MERGE (a:Arrest {name:'Not Arrested'})");
                tx.run("LOAD CSV FROM \"file:///Type.csv\" AS line\n"
                        + "MERGE (n:CrimeType {name:line[0]})\n"
                        + "RETURN n");
                tx.run("LOAD CSV FROM \"file:///Case.csv\" AS line\n"
                        + "MERGE (n:Case {name:line[0]})\n"
                        + "RETURN n");
                tx.run("LOAD CSV FROM \"file:///Location.csv\" AS line\n"
                        + "MERGE (n:Location {name:line[0]})\n"
                        + "RETURN n");
                tx.run("LOAD CSV FROM \"file:///Place.csv\" AS line\n"
                        + "MERGE (n:Place {name:line[0]})\n"
                        + "RETURN n");
                tx.success();
            }
        }
    }

    private static void relation(Crimes db2) throws IOException {

        File file = new File("crimes.csv");
        File file2 = new File("places.csv");

        BufferedReader reader = new BufferedReader(new FileReader(file));
        BufferedReader reader2 = new BufferedReader(new FileReader(file2));

        reader.readLine();//Headlines

        int new_ID_counter = 0;
        String hold = "";
        String ID2 = "";
        String Type2 = "";
        String Name = "";
        String Dist = "";
        String get_near_place = "";
        String ID;
        String Type;
        String Loc;
        String Arrest;

        for (int i = 0; i < 1000; i++) {

            String[] data = reader.readLine().split(",");
            ID = data[0];
            Type = data[2];
            Loc = data[3];
            Arrest = data[4];

            String data_near2[] = new String[12];
            String data_near[] = new String[12];

            db2.addRelation(ID, Type, Loc, Arrest);

        }
        db2.add_near_place();
    }

    public void addRelation(String ID, String Type, String Loc, String Arrest) {
        String ID_to_Type;
        String ID_to_Loc;
        String ID_to_Arrest;

        try (Session session = driver.session()) {

            try (Transaction tx = session.beginTransaction()) {
                ID_to_Type = "MATCH (a:Case {name:'" + ID + "'}),(b:CrimeType {name:'" + Type + "'})\n"
                        + "MERGE(a)-[:CRIME_TYPE]->(b)";
                ID_to_Loc = "MATCH (a:Case {name:'" + ID + "'}),(b:Location {name:'" + Loc + "'})\n"
                        + "MERGE(a)-[:HAPPENED_IN]->(b)";
                if (Arrest.equals("True")) {
                    ID_to_Arrest = "MATCH (a:Case {name:'" + ID + "'}),(b:Arrest {name:'Arrested'})\n"
                            + "MERGE(a)-[:STATUS]->(b)";
                } else {
                    ID_to_Arrest = "MATCH (a:Case {name:'" + ID + "'}),(b:Arrest {name:'Not Arrested'})\n"
                            + "MERGE(a)-[:STATUS]->(b)";
                }
                tx.run(ID_to_Type);
                tx.run(ID_to_Loc);
                tx.run(ID_to_Arrest);
                tx.success();
            }
        }
    }

    private void add_near_place() throws FileNotFoundException, IOException {
        String ID_to_Building;
        try (Session session = driver.session()) {

            try (Transaction tx = session.beginTransaction()) {
                File f = new File("places.csv");
                BufferedReader br = new BufferedReader(new FileReader(f));
                String s = "";
                while ((s = br.readLine()) != null) {
                    String[] a = s.split(",");
                    ID_to_Building = "MATCH (a:Case {name:'" + a[0] + "'}),(b:Place {name:'" + a[1] + "'})\n"
                            + "MERGE(a)-[:NEAR {name:'" + a[2] + "',distance:'" + a[3] + "'}]->(b)";

                    tx.run(ID_to_Building);
                }
                tx.success();
            }
        }
    }

    public void getRelations() {
        try (Session session = driver.session()) {
            String str;
            str = "MATCH (c)-[]->({name:'" + ruleKey + "'}) MATCH (c)-[r]->() RETURN COUNT(r)";
            StatementResult result = session.run(str);
            str = "MATCH (c)-[]->({name:'" + nonKey + "'}) MATCH (c)-[r]->() RETURN COUNT(r)";
            StatementResult result2 = session.run(str);
            str = "MATCH a=(c)-[]->({name:'" + ruleKey + "'}) RETURN COUNT(a)";
            StatementResult result3 = session.run(str);
            str = "MATCH a=(c)-[]->({name:'" + nonKey + "'}) RETURN COUNT(a)";
            StatementResult result4 = session.run(str);

            while (result.hasNext()) {
                Record record = result.next();
                String Qstr = "" + record.get("COUNT(r)");
                KeyRelationNum = Integer.parseInt(Qstr);
            }

            while (result2.hasNext()) {
                Record record = result2.next();
                String Qstr = "" + record.get("COUNT(r)");
                nonKeyRelationNum = Integer.parseInt(Qstr);
            }
            while (result3.hasNext()) {
                Record record = result3.next();
                String Qstr = "" + record.get("COUNT(a)");
                KeyNum = Integer.parseInt(Qstr);
                TotalKey = KeyNum;

            }
            while (result4.hasNext()) {
                Record record = result4.next();
                String Qstr = "" + record.get("COUNT(a)");
                nonKeyNum = Integer.parseInt(Qstr);

            }
        }
    }

    private void getValues() throws IOException {
        try (Session session = driver.session()) {

            int counter = 0;
            int counterNK = 0;
            int s = 0;
            File f = new File(ruleFile);
            BufferedReader r = new BufferedReader(new FileReader(f));
            BufferedReader r2 = new BufferedReader(new FileReader(f));
            while ((r2.readLine()) != null) {
                s++;
            }
            int data[] = new int[s];
            int dataNK[] = new int[s];
            String x;
            String x2 = "";

            while ((x = r.readLine()) != null) {
                x2 = "MATCH z=(c)-[]->({name:'" + ruleKey + "'}),(c)-[]->({name:'" + x + "'}) RETURN COUNT(z)";
                StatementResult result = session.run(x2);
                x2 = "MATCH z=(c)-[]->({name:'" + nonKey + "'}),(c)-[]->({name:'" + x + "'}) RETURN COUNT(z)";
                StatementResult result2 = session.run(x2);
                while (result.hasNext()) {
                    Record record = result.next();
                    data[counter] = Integer.parseInt("" + record.get("COUNT(z)"));
                    counter++;
                }
                while (result2.hasNext()) {
                    Record record = result2.next();
                    dataNK[counterNK] = Integer.parseInt("" + record.get("COUNT(z)"));
                    counterNK++;
                }
            }

            compValue(data, dataNK);
        }
    }

    private void compValue(int data[], int dataNK[]) throws IOException {
        File f2 = new File(ruleFile);
        BufferedReader br = new BufferedReader(new FileReader(f2));
        double value;
        double hold;
        String node;
        String nodes[] = new String[data.length];
        double rates[] = new double[data.length];

        for (int i = 0; i < data.length; i++) {
            node = br.readLine();
            if (node == null) {
                break;
            }
            value = (100.0 * (KeyRelationNum - data[i])) / KeyRelationNum;
            hold = 100 - value;
            value = (100.0 * (nonKeyRelationNum - dataNK[i])) / nonKeyRelationNum;
            hold = hold / (100 - value);

            if (hold >= 1.0 && hold < 100.0) {

            } else if (hold >= 0.0 && hold < 1.0) {

            } else {
                hold = 0;
            }
            nodes[i] = node;
            rates[i] = hold;
        }
        sorting_by_rates(rates, nodes, data, dataNK);
    }

    private void sorting_by_rates(double rates[], String nodes[], int dataK[], int dataNK[]) throws IOException {
        double tmp;
        String tmp2;
        int tmp3;

        for (int i = 0; i < dataK.length; i++) {
            for (int j = 0; j < dataK.length - i - 1; j++) {
                if (rates[j] < rates[j + 1]) {
                    tmp = rates[j];
                    rates[j] = rates[j + 1];
                    rates[j + 1] = tmp;

                    tmp2 = nodes[j];
                    nodes[j] = nodes[j + 1];
                    nodes[j + 1] = tmp2;

                    tmp3 = dataK[j];
                    dataK[j] = dataK[j + 1];
                    dataK[j + 1] = tmp3;

                    tmp3 = dataNK[j];
                    dataNK[j] = dataNK[j + 1];
                    dataNK[j + 1] = tmp3;
                }
            }

        }
        File f = new File("sorted.txt");
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        File f2 = new File("sortedvalues.txt");
        BufferedWriter bw2 = new BufferedWriter(new FileWriter(f2));

        double value;
        double hold;
        for (int i = 0; i < dataK.length; i++) {
            bw2.write(nodes[i] + "\n");
            value = (100.0 * (KeyRelationNum - dataK[i])) / KeyRelationNum;
            hold = 100 - value;
            value = (100.0 * (nonKeyRelationNum - dataNK[i])) / nonKeyRelationNum;
            hold = hold / (100 - value);
            bw2.write("Rate: " + hold + "\n");
            if (hold >= 1.0 && hold < 100.0) {
                KeyRelationNum = KeyRelationNum - dataK[i];
                nonKeyRelationNum = nonKeyRelationNum - dataNK[i];
            }
        }
        for (int i = 0; i < nodes.length; i++) {
            bw.write(nodes[i] + "\n");
        }
        bw.close();
        bw2.close();
    }

    private void Calculate() throws FileNotFoundException, IOException {
        File f2 = new File("sortedvalues.txt");
        String a;

        String a2;
        String b;
        String b2;
        double rate;
        String query;
        String w;
        String w2;

        String str;
        BufferedReader br2 = new BufferedReader(new FileReader(f2));
        while ((a = br2.readLine()) != null) {
            a2 = br2.readLine();
            rate = Double.parseDouble(a2.substring(a2.indexOf(": ") + 2, a2.length()));
            if (rate >= 1.0 && rate < 100.0) {
                getSupp(a);
            }
        }
        setOdds();
    }

    private void getSupp(String a) throws FileNotFoundException, IOException {
        K = 0;
        NK = 0;

        double x;
        try (Session session = driver.session()) {
            String str = "";
            String x2 = "MATCH (x) return count(x)";
            File f2 = new File("ConfSupp.txt");
            FileWriter fw = new FileWriter(f2, true);
            BufferedWriter br2 = new BufferedWriter(fw);
            StatementResult result = session.run(x2);
            StatementResult result2 = session.run(x2);
            Record record = result.next();
            record = result2.next();

            str = "MATCH a=(c)-[]->({name:'" + ruleKey + "'}),p=(c)-[]->({name:'" + a + "'}) RETURN COUNT(a)";
            result = session.run(str);
            str = "MATCH a=(c)-[]->({name:'" + nonKey + "'}),p=(c)-[]->({name:'" + a + "'}) RETURN COUNT(a)";
            result2 = session.run(str);

            while (result.hasNext()) {
                record = result.next();
                String Qstr = "" + record.get("COUNT(a)");
                K = Integer.parseInt(Qstr);
            }

            while (result2.hasNext()) {
                record = result2.next();
                String Qstr = "" + record.get("COUNT(a)");
                NK = Integer.parseInt(Qstr);
            }

            x = K * 1.0;
            conf = (x / (x + NK));
            supp = (x / KeyNum);
            if (supp >= 0.1 && conf >= 0.4) {
                br2.write(a + "\n");
            }
            br2.close();
        }
    }

    private void setOdds() throws IOException {
        File f = new File("ConfSupp.txt");
        BufferedReader br = new BufferedReader(new FileReader(f));
        String value;
        int size = 0;
        int sizeComma = 0;
        List testlist = new ArrayList();
        list.add(br.readLine());
        while ((value = br.readLine()) != null) {
            size = list.size();
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < ("" + list.get(i)).length(); j++) {
                    if (("" + list.get(i)).charAt(j) == ',') {
                        sizeComma++;
                    }
                }
                if (sizeComma < 1) {
                    list.add(list.get(i) + "," + value);
                }
                sizeComma = 0;
            }
            list.add(value);
            testlist.add(value);

        }
        for (int i = 0; i < testlist.size(); i++) {
            list.add("" + testlist.get(i));
        }
        getNames();
    }

    private void getNames() throws FileNotFoundException, IOException {
        try (Session session = driver.session()) {
            File f2 = new File("sortedNamesK.txt");
            File f3 = new File("sortedNamesNK.txt");
            BufferedWriter bwz = new BufferedWriter(new FileWriter(f2));
            BufferedWriter bw2 = new BufferedWriter(new FileWriter(f3));
            String query = "";
            String x2 = "";
            String hold;
            StatementResult result;
            StatementResult result2;
            StatementResult result3;

            String strEast = " (c)-[]->({name:'" + ruleKey + "'}) RETURN c.name";
            String strWest = " (c)-[]->({name:'" + nonKey + "'}) RETURN c.name";
            for (int i = 0; i < list.size(); i++) {
                query = "" + list.get(i);
                if (!query.contains(",")) {
                    x2 = "MATCH x=(c)-[]->({name:'" + query + "'}),z=(c)-[]->({name:'" + ruleKey + "'}) RETURN c.name";
                    result = session.run(x2);
                    x2 = "MATCH x=(c)-[]->({name:'" + query + "'}),z=(c)-[]->({name:'" + nonKey + "'}) RETURN c.name";
                    result2 = session.run(x2);
                    x2 = "MATCH x=(c)-[]->({name:'" + query + "'}),z=(c)-[]->({name:'" + ruleKey + "'}) RETURN COUNT(c)";
                    result3 = session.run(x2);
                } else {
                    String[] query_ = query.split(",");
                    hold = "";
                    for (int j = 0; j < query_.length; j++) {
                        hold += "(c)-[]->({name:'" + query_[j] + "'}),";
                    }

                    String str = "MATCH " + hold + strEast;
                    result = session.run(str);
                    str = "MATCH " + hold + strWest;
                    result2 = session.run(str);
                    str = "MATCH " + hold + " (c)-[]->({name:'" + ruleKey + "'}) RETURN COUNT(c)";
                    result3 = session.run(str);
                }
                bwz.write(query + "\n");
                bw2.write(query + "\n");
                while (result.hasNext()) {
                    Record record = result.next();
                    bwz.write("" + record.get("c.name") + "\n");
                }
                while (result2.hasNext()) {
                    Record record = result2.next();
                    bw2.write("" + record.get("c.name") + "\n");
                }
                while (result3.hasNext()) {
                    Record record = result3.next();
                    listCount.add("" + record.get("COUNT(c)"));
                }

                bwz.write("xxx\n");
                bw2.write("xxx\n");
            }
            bwz.close();
            bw2.close();
            send();
        }
    }

    private void send() throws IOException {
        try (Session session = driver.session()) {
            File f = new File("SuppConfDegerleri.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            int K = 0;
            int NK = 0;
            double supp;
            double conf;
            double x;
            String value = "";
            String query = "";
            String queryK = "";
            String queryNK = "";
            String ctrl = "";
            String last = "";
            StatementResult result;
            StatementResult result2;
            int count_;
            for (int i = 0; i < list.size(); i++) {
                value = "" + list.get(i);
                if (value.contains(",")) {
                    String[] query_ = value.split(",");
                    String hold = "";
                    ctrl = "";
                    last = query_[query_.length - 1];
                    for (int j = 0; j < query_.length - 1; j++) {
                        hold += query_[j] + ":'true',";
                        ctrl += query_[j] + ",";
                    }
                    hold = hold.substring(0, hold.lastIndexOf(","));
                    ctrl = ctrl.substring(0, ctrl.lastIndexOf(","));
                    query = "MATCH (c {" + hold + "})-[]->({name:'" + query_[query_.length - 1] + "'})";
                    queryK = query + ",(c)-[]->({name:'" + ruleKey + "'}) RETURN COUNT(c)";
                    result = session.run(queryK);
                    queryNK = query + ",(c)-[]->({name:'" + nonKey + "'}) RETURN COUNT(c)";
                    result2 = session.run(queryNK);
                } else {
                    query = "MATCH (c)-[]->({name:'" + value + "'}),(c)-[]->({name:'" + ruleKey + "'}) WHERE Size(KEYS(c))=1 RETURN COUNT(c)";
                    result = session.run(query);
                    query = "MATCH (c)-[]->({name:'" + value + "'}),(c)-[]->({name:'" + nonKey + "'}) WHERE Size(KEYS(c))=1 RETURN COUNT(c)";
                    result2 = session.run(query);
                }
                while (result.hasNext()) {
                    Record record = result.next();
                    K = Integer.parseInt("" + record.get("COUNT(c)"));
                }
                while (result2.hasNext()) {
                    Record record = result2.next();
                    NK = Integer.parseInt("" + record.get("COUNT(c)"));
                }

                x = K * 1.0;
                conf = (x / (x + NK));
                supp = (x / KeyNum);

                int lastindex = 0;
                int datanum = 0;
                if (supp > 0.1 && conf > 0.4) {
                    bw.write(value + "\nSupp:" + supp + "\nConf:" + conf + "\n***\n");
                    if (value.contains(",")) {
                        datanum = getnum(last);
                        for (int j = 0; j < list.size(); j++) {
                            if (("" + list.get(j)).equals(last)) {
                                lastindex = j;
                                break;
                            }
                        }
                        for (int j = 0; j < list.size(); j++) {
                            if (("" + list.get(j)).equals(ctrl)) {
                                count_ = Integer.parseInt("" + listCount.get(j));
                                if (count_ - K > (KeyNum * 0.1) && datanum > (KeyNum * 0.1)) {
                                    listCount.set(lastindex, datanum);
                                    listCount.set(j, count_ - K);
                                    compress(value);
                                    break;
                                } else {
                                    keyToRelation(value);
                                    compress(value);
                                    break;
                                }
                            }
                        }
                    } else if (!value.contains(",")) {
                        compress(value);
                    }
                } else if (value.contains(",")) {
                    keyToRelation(value);
                }
            }
            bw.close();
        }
    }

    private int getnum(String value) {
        int num = 0;
        String query = "";
        try (Session session = driver.session()) {
            query = "MATCH (c)-[]->({name:'" + value + "'}),(c)-[]->({name:'" + ruleKey + "'}) WHERE Size(keys(c))=1 RETURN COUNT(c)";
            StatementResult result = session.run(query);
            while (result.hasNext()) {
                Record record = result.next();
                num = Integer.parseInt("" + record.get("COUNT(c)"));
            }
        }
        return num;
    }

    private void keyToRelation(String value) {
        try (Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                controlList.clear();
                int size = 0;
                String[] query = value.split(",");
                for (int i = 0; i < value.length(); i++) {
                    if (value.charAt(i) == ',') {
                        size++;
                    }
                }
                String key = "";
                String keyNull = "";
                String rule = "";
                for (int j = 0; j < query.length - 1; j++) {
                    rule += query[j] + ",";
                    key += query[j] + ":'true',";
                    keyNull += query[j] + ":null,";
                }
                key = key.substring(0, key.lastIndexOf(","));
                keyNull = keyNull.substring(0, keyNull.lastIndexOf(","));
                rule = rule.substring(0, rule.lastIndexOf(","));

                read(key, size);
                for (int i = 0; i < list.size(); i++) {
                    if (("" + list.get(i)).equals(rule)) {
                        listCount.set(i, (Integer.parseInt("" + listCount.get(i))) + controlList.size());
                        break;
                    }
                }

                String q = "MATCH (c {" + key + "})-[r]->() WHERE size(keys(c))=" + (size + 1) + " SET c+=({" + keyNull + "})";
                tx.run(q);
                for (int i = 0; i < controlList.size(); i++) {
                    String name = ("" + controlList.get(i)).substring(("" + controlList.get(i)).indexOf("\"") + 1, ("" + controlList.get(i)).lastIndexOf("\""));
                    for (int j = 0; j < query.length - 1; j++) {

                        String e = "MATCH (c {name:'" + name + "'}) MATCH (r {name:'" + query[j] + "'}) MERGE (c)-[:NEW]->(r)";
                        tx.run(e);
                    }

                }
                tx.success();
            }
        }
    }

    private void read(String hold, int size) {
        try (Session session = driver.session()) {
            String w = "MATCH (c {" + hold + "})  RETURN c.name";
            StatementResult result = session.run(w);
            while (result.hasNext()) {
                Record record = result.next();
                controlList.add("" + record.get("c.name"));
            }
        }
    }

    private void compress(String a) throws FileNotFoundException, IOException {
        try (Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                File f = new File("sortedNamesK.txt");
                File f2 = new File("sortedNamesNK.txt");

                BufferedReader br = new BufferedReader(new FileReader(f));
                BufferedReader br2 = new BufferedReader(new FileReader(f2));
                StatementResult result;
                String key;
                String nonkey;
                String key2;
                String nonkey2;
                String k = "xxx";
                String query = "";
                int ctrl = 0;

                while (!(key = br.readLine()).equals(a));
                while (!(nonkey = br2.readLine()).equals(a));

                if (a.contains(",")) {
                    ctrl = Control(a);
                    if (ctrl > (KeyNum * 0.1)) {
                        if (key.equals(a)) {
                            while ((key.startsWith(k)) == false) {
                                key = br.readLine();
                                if (key.startsWith(k)) {
                                    break;
                                }

                                key2 = key.substring(key.indexOf("\"") + 1, key.lastIndexOf("\""));
                                break_relation(a, key2, 1);
                            }
                        }
                        if (nonkey.equals(a)) {
                            while ((nonkey.startsWith(k)) == false) {
                                nonkey = br2.readLine();
                                if (nonkey.startsWith(k)) {
                                    break;
                                }
                                nonkey2 = nonkey.substring(nonkey.indexOf("\"") + 1, nonkey.lastIndexOf("\""));
                                break_relation(a, nonkey2, 2);

                            }
                        }
                    } else {
                    }
                } else {
                    if (key.equals(a)) {
                        while ((key.startsWith(k)) == false) {
                            key = br.readLine();
                            if (key.startsWith(k)) {
                                break;
                            }

                            key2 = key.substring(key.indexOf("\"") + 1, key.lastIndexOf("\""));
                            break_relation(a, key2, 1);
                        }
                    }
                    if (nonkey.equals(a)) {
                        while ((nonkey.startsWith(k)) == false) {
                            nonkey = br2.readLine();
                            if (nonkey.startsWith(k)) {
                                break;
                            }
                            nonkey2 = nonkey.substring(nonkey.indexOf("\"") + 1, nonkey.lastIndexOf("\""));
                            break_relation(a, nonkey2, 2);
                        }
                    }
                }

                tx.success();
            }
        }
    }

    private int Control(String a) {
        String[] value = a.split(",");
        String test = "";
        String query = "";
        int num = 0;
        int size = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) == ',') {
                size++;
            }
        }
        try (Session session = driver.session()) {
            for (int j = 0; j < value.length - 1; j++) {
                test += value[j] + ":'true',";
            }
            test = test.substring(0, test.lastIndexOf(","));
            query = "MATCH (c {" + test + "})-[]->({name:'" + value[value.length - 1] + "'}),(c)-[]->({name:'" + ruleKey + "'}) WHERE Size(keys(c))=" + (size + 1) + " RETURN COUNT(c)";
            StatementResult result = session.run(query);
            while (result.hasNext()) {
                Record record = result.next();
                num = Integer.parseInt("" + record.get("COUNT(c)"));
            }
        }
        return num;
    }

    private void break_relation(String a, String b, int i) throws IOException {
        try (Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                int counter = 0;
                int size = 0;
                String query;
//                String z = a;
//                z = z.replaceAll("\\(", "");
//                z = z.replaceAll("\\)", "");
//                z = z.replaceAll(" ", "");
//                z = z.replaceAll("/", "");
//                z = z.replaceAll("\\.", "");
//                z = z.replaceAll("-", "");
                if (!a.contains(",") && i == 1) {

                    query = "MATCH (c {name:'" + b + "'}) WHERE Size(keys(c))=1 SET c+= {" + a + ":'true'}";
                    tx.run(query);
                    query = "MATCH p=(c {name:'" + b + "'})-[r]->({name:'" + a + "'}),z=(c)-[]->({name:'" + ruleKey + "'}) WHERE size(keys(c))=2 DETACH DELETE r";
                    tx.run(query);
                } else if (!a.contains(",") && i == 2) {
                    query = "MATCH (c {name:'" + b + "'}) WHERE Size(keys(c))=1 SET c+= {" + a + ":'true'}";
                    tx.run(query);
                    query = "MATCH p=(c {name:'" + b + "'})-[r]->({name:'" + a + "'}),z=(c)-[]->({name:'" + nonKey + "'}) WHERE size(keys(c))=2 DETACH DELETE r";
                    tx.run(query);
                }
                if (a.contains(",")) {
                    for (int j = 0; j < a.length(); j++) {
                        if (a.charAt(j) == ',') {
                            counter++;
                        }
                    }

                    a = a.substring(a.lastIndexOf(",") + 1, a.length());

                    size = counter + 1;

                    if (i == 1) {
                        query = "MATCH (c {name:'" + b + "'}) WHERE Size(keys(c))=" + size + " SET c+= {" + a + ":'true'}";
                        tx.run(query);
                        query = "MATCH p=(c {name:'" + b + "'})-[r]->({name:'" + a + "'}),z=(c)-[]->({name:'" + ruleKey + "'}) WHERE Size(keys(c))=" + (size + 1) + " DETACH DELETE r";
                        tx.run(query);

                    } else {
                        query = "MATCH (c {name:'" + b + "'}) WHERE Size(keys(c))=" + size + " SET c+= {" + a + ":'true'}";
                        tx.run(query);
                        query = "MATCH p=(c {name:'" + b + "'})-[r]->({name:'" + a + "'}),z=(c)-[]->({name:'" + nonKey + "'}) WHERE Size(keys(c))=" + (size + 1) + " DETACH DELETE r";
                        tx.run(query);
                    }
                }
                tx.success();
            }
        }
    }

    public void close() {
        driver.close();
    }

    private void getRules() throws IOException {
        try (Session session = driver.session()) {
            File file = new File("Rules.txt");
            File file2 = new File("RulesUnique.txt");

            List hold = new ArrayList();
            List hold2 = new ArrayList();
            List hold3 = new ArrayList();
            HashSet<String> myHashset = new HashSet<String>();

            FileWriter printer = new FileWriter(file);
            BufferedWriter fg = new BufferedWriter(printer);

            FileWriter printer2 = new FileWriter(file2);
            BufferedWriter fg2 = new BufferedWriter(printer2);

            BufferedReader reader = null;
            reader = new BufferedReader(new FileReader(file));

            BufferedReader reader2 = null;
            reader2 = new BufferedReader(new FileReader(file2));

            String str;
            String ctr;

            str = "MATCH (c)-[]->({name:'" + ruleKey + "'}) RETURN keys(c)";
            StatementResult result = session.run(str);

            while (result.hasNext()) {
                Record record = result.next();
                String Astr = "" + record.get("keys(c)");
                if (Astr.startsWith("[\"name\"]")); else {
                    hold.add(Astr);
                }

            }
            hold.add("END");

            String a = " \"name\"";
            String b = "[\"name\"";
            String c = "\"name\"]";
            String d = "";
            String t;
            String t2;

            for (int m = 0; m < hold.size(); m++) {

                t = "" + hold.get(m);
                if (t.startsWith("END")) {
                    break;
                }
                String data = t;

                if (data.equals(a)) {
                    data.replaceAll(a, "");
                }
                if (data.equals(b)) {
                    data.replaceAll(b, "");
                }
                if (data.equals(c)) {
                    data.replaceAll(c, "");
                }
                if (data.equals(d)) {
                } else {

                    hold2.add(data);
                }

            }

            for (int k = 0; k < hold2.size(); k++) {

                t2 = "" + hold2.get(k);

                t2 = t2.replaceAll("\\[", "");
                t2 = t2.replaceAll("\"", "");
                t2 = t2.replaceAll("\\]", "");
                t2 = t2.replaceAll(" ", "");
                t2 = t2.replaceAll("name,", "");
                t2 = t2.replaceAll(",name", "");
                fg.write(t2);

                fg.write("\n");

            }
            fg.close();

            while ((ctr = reader.readLine()) != null) {
                myHashset.add(ctr);
            }

            for (String diller : myHashset) {
                fg2.write(diller + "\n");
            }
            fg2.close();
        }
    }

    private void results() throws FileNotFoundException, IOException {
        File f = new File("Rules.txt");
        File f2 = new File("RulesUnique.txt");
        File f3 = new File("Results.txt");

        BufferedReader br = new BufferedReader(new FileReader(f));
        BufferedReader br2 = new BufferedReader(new FileReader(f2));
        BufferedReader br3 = new BufferedReader(new FileReader(f2));
        BufferedWriter bw = new BufferedWriter(new FileWriter(f3));
        int size = 0;
        while (br.readLine() != null) {
            size++;
        }
        String a = "";
        int sizeComma = 0;
        int counter = 0;
        int counter2 = 0;
        while ((a = br2.readLine()) != null) {
            if (a.contains(",")) {
                for (int i = 0; i < a.length(); i++) {
                    if (a.charAt(i) == ',') {
                        sizeComma++;
                    }
                }
                counter += sizeComma + 1;
                sizeComma = 0;
            } else {
                counter++;
            }
            counter2++;
        }
        double avg = (counter * 1.0) / counter2;
        double acc = ((size * 100.0) / TotalKey);
        bw.write("Coverage: " + size + "/" + TotalKey + " Accuracy: %" + acc + "\n");
        bw.write("Time (Database): " + time1 + "\n");
        bw.write("Time (Rules): " + time2 + "\n");
        bw.write("Average: " + avg + "\n");
        bw.write("Number of scanned rules: " + list.size() + "\n");
        bw.write("\n" + counter2 + " Rules:\n---\n");
        String x = "";
        int i = 1;
        while ((x = br3.readLine()) != null) {
            bw.write(i + ". " + x + "\n");
            i++;
        }

        bw.close();
    }

    public static void main(String... args) throws FileNotFoundException, IOException {
        //Server connection
        Crimes db = new Crimes("bolt://localhost:7687", "neo4j", "123"); // Change this to your own parameters 

        long startTime = System.currentTimeMillis();
        System.out.println("Database is creating..");

        db.addNode();
        relation(db);

        long endTime = System.currentTimeMillis();
        time1 = ((endTime - startTime) / 1000.0);
        System.out.println("Database created in " + time1 + " sec");
        startTime = System.currentTimeMillis();
        System.out.println("Rules are explaining..");

        db.getRelations();
        db.getValues();
        db.Calculate();

        endTime = System.currentTimeMillis();
        time2 = (endTime - startTime) / 1000.0;
        db.getRules();

        System.out.println("Rules explained in " + time2 + " sec");

        db.results();
        File f = new File("ConfSupp.txt");
        BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));
        bw.close();
        db.close();
    }

}
