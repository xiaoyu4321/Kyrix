package box;

import project.Canvas;

import java.util.ArrayList;
import java.util.HashMap;

public class BoxandData {
    public Box box;
    public ArrayList<ArrayList<ArrayList<String>>> data;

    public BoxandData(Box box, ArrayList<ArrayList<ArrayList<String>>> data) {

        this.box = box;
        this.data = data;
    }

    // Render data used to be stored in a three-dimenson array (layer, row, field).
    // To enable writing rendering functions using field names,
    // we convert it to an array of arrays of dictionaries (hashMap in Java)
    public static ArrayList<ArrayList<HashMap<String, String>>> getDictionaryFromData(ArrayList<ArrayList<ArrayList<String>>> data, Canvas c) {

        ArrayList<ArrayList<HashMap<String, String>>> ret = new ArrayList<>();
        int numLayers = data.size();
System.out.println("in box and data");
        for (int i = 0; i < numLayers; i ++) {
            ret.add(new ArrayList<>());
            int numRows = data.get(i).size();
            ArrayList<String> fields = c.getLayers().get(i).getTransform().getColumnNames();
            int numFields = fields.size();
            for (int j = 0; j < numRows; j ++) {
                ArrayList<String> rowArray = data.get(i).get(j);
                HashMap<String, String> rowDict = new HashMap<>();
                for (int k = 0; k < numFields; k ++)
                    rowDict.put(fields.get(k), rowArray.get(k));
                if(c.getId().equals("region") && i == 1){
                rowDict.put("cx", rowArray.get(1));
                rowDict.put("cy", rowArray.get(0));
                rowDict.put("minx", rowArray.get(1));
                rowDict.put("miny", rowArray.get(0));
                rowDict.put("maxx", rowArray.get(2));
                rowDict.put("maxy", rowArray.get(0));
                }
                else {
                rowDict.put("cx", rowArray.get(numFields-1));
                rowDict.put("cy", rowArray.get(5));
                rowDict.put("minx", rowArray.get(numFields - 1));
                rowDict.put("miny", rowArray.get(5));
                rowDict.put("maxx", rowArray.get(numFields - 1));
                rowDict.put("maxy", rowArray.get(5));
                }
                ret.get(i).add(rowDict);
            }
        }
        return ret;
    }
}
