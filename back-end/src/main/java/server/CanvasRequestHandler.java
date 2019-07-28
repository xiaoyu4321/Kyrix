package server;

import box.BoxandData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import main.Config;
import main.DbConnector;
import main.Main;
import project.Canvas;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.RList;

/**
 * Created by wenbo on 1/8/18.
 */
public class CanvasRequestHandler implements HttpHandler {

    private final Gson gson;

    public CanvasRequestHandler() {

        gson = new GsonBuilder().create();
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        System.out.println("Serving /canvas");

        // check if this is a POST request
        if (! httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
            Server.sendResponse(httpExchange, HttpsURLConnection.HTTP_BAD_METHOD, "");
            return;
        }

        // get data of the current request
        String query = httpExchange.getRequestURI().getQuery();
        Map<String, String> queryMap = Server.queryToMap(query);
        String canvasId = queryMap.get("id");

        // get the current canvas
        Canvas c = null;
        try {
            c = Main.getProject().getCanvas(canvasId).deepCopy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // list of predicates
        ArrayList<String> predicates = new ArrayList<>();
        for (int i = 0; i < c.getLayers().size(); i ++)
            predicates.add(queryMap.get("predicate" + i));

        // calculate w or h if they are not pre-determined
        if (c.getwSql().length() > 0) {
            String predicate = queryMap.get("predicate" + c.getwLayerId());
            String sql = c.getwSql() + " and " + predicate;
            String db = c.getDbByLayerId(c.getwLayerId());
            try {
                c.setW(getWidthOrHeightBySql(sql, db));
            } catch (Exception e) {}
        }
        if (c.gethSql().length() > 0) {
            String predicate = queryMap.get("predicate" + c.gethLayerId());
            String sql = c.gethSql() + " and " + predicate;
            String db = c.getDbByLayerId(c.gethLayerId());
            try {
                c.setH(getWidthOrHeightBySql(sql, db));
            } catch (Exception e) {}
        }

        // get static data
        ArrayList<ArrayList<ArrayList<String>>> staticData = null;
        try {
            staticData = getStaticData(c, predicates);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // construct the response object
        Map<String, Object> respMap = new HashMap<>();
        respMap.put("canvas", c);
        respMap.put("staticData", BoxandData.getDictionaryFromData(staticData, c));
        String response = gson.toJson(respMap);
        // send the response back
        Server.sendResponse(httpExchange, HttpsURLConnection.HTTP_OK, response);
    }

    private int getWidthOrHeightBySql(String sql, String db) throws SQLException, ClassNotFoundException {

        return Integer.valueOf(DbConnector.getQueryResult(db, sql).get(0).get(0));
    }

    private ArrayList<ArrayList<ArrayList<String>>> getStaticData(Canvas c, ArrayList<String> predicates)
            throws SQLException, ClassNotFoundException, Exception {
        String filePath = "/home/scidb/biobank/phege/lib/data_access_helpers.R";
        RConnection rc = new RConnection();
        rc.assign("filepath", filePath);
        rc.eval("source(filepath)");
        rc.eval("namespace <- \"RIVAS\"");
        rc.eval("association_set = \"RIVAS_ASSOC\"");
        rc.eval("variants_namespace = \"UK_BIOBANK\"");
        REXP a = rc.eval("try(bb <- get_scidb_biobank_connection(username = \"scidbadmin\", password = \"Paradigm4\"),silent=TRUE)");

        long start = System.currentTimeMillis();
        // container for data
        ArrayList<ArrayList<ArrayList<String>>> data = new ArrayList<>();
        for (int k = 0; k < c.getLayers().size(); k ++) {

            // add an empty placeholder for static layers
            if (! c.getLayers().get(k).isStatic()) {
                data.add(new ArrayList<>());
                continue;
            }
        rc.eval("phenotypes <- get_phenotypes(bb,association_namespace = namespace,association_set_name = association_set)");
        if(c.getId().equals("phenotype")){
            if(predicates.get(k).length() == 0)
                rc.eval("pheno <- phenotypes[as.integer(1), ]");
            else {
                String[] preds = predicates.get(k).split("\'");
                int pheno_id = (int)(Double.parseDouble(preds[1]));
                rc.eval("pheno <- phenotypes[as.integer(" + pheno_id + "), ]");
            }
        rc.parseAndEval("REGION_TAB_ADDITIONAL_VARIANT_FIELD_NAME = c(\"genes\", \"consequence\")");
System.out.println("in get static data");
        rc.parseAndEval("result <- get_associations_for_phenotype_tab(bb, variants_namespace = variants_namespace, association_namespace = namespace, association_set = association_set, phenotypes = pheno, additional_variant_field_names = REGION_TAB_ADDITIONAL_VARIANT_FIELD_NAME)");
//transforming xpos
        rc.eval("result$xpos <- result$pos");
System.out.println("Getting associations for phenotype tab...");
        rc.eval("for (chrom in chroms_all_but_first[chroms_all_but_first %in% PHEGE_CONFIG$CHROMOSOME_SELECTION]) result$xpos[result$chrom == chrom] <- (result$xpos[result$chrom == chrom] + sum(chromosome_lengths[1:chrom - 1]))");
        }
        else if (c.getId().equals("variant")){
        if(predicates.get(k).length() == 0)
            System.out.println("Error: no predicate specified.");
        String[] preds = predicates.get(k).split("\'");
        int chromo = (int)(Double.parseDouble(preds[1]));
        Long pos = (long)(Double.parseDouble(preds[3]));
        rc.assign("chromosome", Integer.toString(chromo));
        rc.assign("position", Long.toString(pos));
  
        rc.parseAndEval("VARIANTS_TAB_ADDITIONAL_VARIANT_FIELD_NAMES = c(\"genes\", \"consequence\")");
        rc.parseAndEval("result <- get_variant_info(bb, variants_namespace = variants_namespace, association_namespace = namespace, association_set_name = association_set, chromosome = chromosome, position = position, additional_variant_field_names = VARIANTS_TAB_ADDITIONAL_VARIANT_FIELD_NAMES)");
        rc.eval("result$variant <- paste0(result$chrom,\" : \",result$pos,\" \",result$ref,\"/\",result$alt,\" \",result$rsid)");
        rc.eval("result$ref <- NULL");
        rc.eval("result$alt <- NULL");
        rc.eval("result$rsid <- NULL");
        rc.eval("var_info <- result");
        rc.eval("result <- get_associations_for_variant_tab(bb,association_namespace = namespace,association_set_name = association_set,chromosome = chromosome,position = position)");
        rc.eval("result <- merge(result, phenotypes, by = \"sub_field_id\")");
        rc.eval("result <- merge(result, var_info[, c(\"variant_id\", \"variant\")], by = \"variant_id\")");
        rc.eval("result <- result[order(result$log10pvalue, decreasing = TRUE), ]");
        rc.eval("result$value_type[is.na(result$value_type)] <- \"NA\"");
        }
System.out.println("data returned in " + (System.currentTimeMillis() - start) / 1000.0 + "s.");
        long st = System.currentTimeMillis();
        RList x = rc.eval("result").asList();
        String[] keys = x.keys();
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        double[] chrom = x.at(keys[0]).asDoubles();
        for(int i=0;i<chrom.length;i++){
            ArrayList<String> curRow = new ArrayList<>();
            for(String key : keys){
            String[] s = x.at(key).asStrings();
            curRow.add(s[i]);
            }
            result.add(curRow);
        }
rc.close();
        data.add(result);
System.out.println("transform R to Java in  " + (System.currentTimeMillis() - st) / 1000.0 + "s.");
        }
        return data;
    }
}
