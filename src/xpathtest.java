import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class xpathtest {
	public static void main(String[] args) throws Exception {
        String inputFile = null;
        if ( args.length>0 ) inputFile = args[0];
               
        String newFile = RewriteHelper(inputFile);

        InputStream is = System.in;
        if ( newFile!=null ) is = new FileInputStream(newFile);
        ANTLRInputStream input = new ANTLRInputStream(is);
        XQueryLexer lexer = new XQueryLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        XQueryParser parser = new XQueryParser(tokens);
        ParseTree tree = parser.xq(); // parse
        //System.out.println("parser tree"+tree.getText());
        
        
        EvalVisitor eval = new EvalVisitor();
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		eval.outDoc = docBuilder.newDocument();
		
        ArrayList<Node> finalResult = eval.visit(tree);
        //System.out.println("fR"+ finalResult);

        System.out.println("outDoc"+ eval.outDoc);

        Element mainRootElement = eval.outDoc.createElement("myResult");
        eval.outDoc.appendChild(mainRootElement);
        System.out.println("finalResult size: " + finalResult.size());
        for(Node n:finalResult){
        	mainRootElement.appendChild(eval.outDoc.importNode(n.cloneNode(true), true));
        	//System.out.println(n);
        }
        
        try{
        // write the content into xml file
     		TransformerFactory transformerFactory = TransformerFactory.newInstance();
     		Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 

     		DOMSource source = new DOMSource(eval.outDoc);
     		StreamResult result = new StreamResult(new File("output.xml"));
      
     		// Output to console for testing
     		// StreamResult result = new StreamResult(System.out);
      
     		transformer.transform(source, result);
      
     		System.out.println("File saved!");
        }catch (Exception e) {
            e.printStackTrace();
        }
        
    }
	
	public static String RewriteHelper(String inputFile) throws IOException{
		InputStream is = System.in;
        if ( inputFile!=null ) is = new FileInputStream(inputFile);
        ANTLRInputStream input = new ANTLRInputStream(is);
        XQueryLexer lexer = new XQueryLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        XQueryParser parser = new XQueryParser(tokens);
        ParseTree tree = parser.xq(); // parse
        //System.out.println("parser tree"+tree.getText());
        //System.out.println("tree child"+tree.getChildCount());
        // Save different variables
        Integer FirstTreeChilds = tree.getChildCount();
        String FirstTag = "";
        if(FirstTreeChilds != 3){
        	FirstTag = tree.getChild(1).getText();
        	tree = tree.getChild(4);
            //System.out.println("parser tree"+tree.getText());

        }

        ParseTree forChild = tree.getChild(0);
        
        int forChilds = forChild.getChildCount();
        
        ArrayList<TreeMap<String,String> > TArray = new ArrayList<>();
        HashMap<String, Integer> globalMap = new HashMap<>();
        int DocNum = 0;
        
        for(int i=0; i<forChilds; i += 4){
        	String var = forChild.getChild(i+1).getText();
        	ParseTree path = forChild.getChild(i+3);
        	if(path.getText().startsWith("document")){
        		TreeMap<String, String> localMap = new TreeMap<>();
        		localMap.put(var, path.getText());
        		globalMap.put(var, DocNum);
        		TArray.add(localMap);
        		DocNum += 1;
        	}
        	else{
        		String pVar = path.getChild(0).getText();
        		int docnum = globalMap.get(pVar);
        		globalMap.put(var, docnum);
        		TArray.get(docnum).put(var, path.getText());
        	}
        }
        //System.out.println("TArray: "+ TArray);
        //System.out.println("globalMap:"+ globalMap);

        // find relations in whereClause
        TreeMap<String, HashMap<Integer, ArrayList> > whereMap = new TreeMap<>();
        HashMap<Integer, ArrayList<String>> textMap = new HashMap<>();
        ParseTree whereChild = tree.getChild(1);
        //System.out.println("whereChild"+whereChild);
        traverseCond(whereChild.getChild(1), globalMap, whereMap, textMap);
        
        //System.out.println("whereMap" + whereMap);
        
        
        //Rewrite part
        ArrayList<Integer> joinArray = new ArrayList<>();
       
        String JoinResult = "";
        
        Object[] whereArray = whereMap.keySet().toArray();
		int length = whereArray == null ? 0 : whereArray .length;
		for(int i=0; i<length; i++){
			String key = (String) whereArray[i];
			//System.out.println("key" + key);
			String[] a = key.split("-");

			int keyLeft = Integer.parseInt(a[0]);
			int keyRight = Integer.parseInt(a[1]);

			HashMap<Integer, ArrayList> temp = whereMap.get(key);
			//System.out.println(temp);
			String lList = temp.get(keyLeft).toString();
			String rList = temp.get(keyRight).toString();
			
			if(!joinArray.contains(keyLeft) && !joinArray.contains(keyRight)){
				String left = partionHelper(TArray.get(keyLeft), textMap, keyLeft);
				String right = partionHelper(TArray.get(keyRight), textMap, keyRight);
				joinArray.add(keyLeft);
				if(keyLeft != keyRight){
					joinArray.add(keyRight);
				}
				
				JoinResult += "join(" + "\n"+ left +",\n" + right + ",\n" + lList + "," + rList +"\n )";
			}
			else if(joinArray.contains(keyLeft) && !joinArray.contains(keyRight)){
				String right = partionHelper(TArray.get(keyRight), textMap, keyLeft) ;
				joinArray.add(keyRight);
				
				JoinResult = "join(" + "\n"+ JoinResult + ",\n" + right + ",\n" + lList + "," + rList +"\n )";
			}
			else if(joinArray.contains(keyRight) && !joinArray.contains(keyLeft)){
				String left = partionHelper(TArray.get(keyLeft), textMap, keyRight);
				joinArray.add(keyLeft);
				
				JoinResult = "join(" +"\n"+ JoinResult + ",\n" + left + ",\n" + lList + "," + rList +"\n )";
			}
			//System.out.println("JoinResult: "+JoinResult);
		}
		System.out.println("joinArray: "+joinArray);
		if(joinArray.size() < 1){
			
        	System.out.println("u don't need rewrite");
        	return inputFile;
        }
		
		ParseTree returnChild = tree.getChild(2).getChild(1);
		String returnStr = returnHelper(returnChild);
		
		System.out.println(returnStr);
		
		
		String finalJoin;
		if(FirstTreeChilds == 3){
			finalJoin = "for $tuple in " + JoinResult + "\nreturn "+returnStr;
		}
		else{
			//System.out.println("Firsttag"+FirstTag);
			finalJoin = "<" + FirstTag + ">" +"{"+ "for $tuple in " + JoinResult + "\nreturn "+ returnStr +"}</" + FirstTag + ">" ;
		}
		
		String newFile = "newquery.txt" ;
	    FileWriter fstream = new FileWriter(newFile,false);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(finalJoin);
        out.close();
        
		return newFile;
	}
	
	public static void traverseCond(ParseTree cond, HashMap<String, Integer> globalMap, TreeMap<String, HashMap<Integer, ArrayList> > whereMap, HashMap<Integer, ArrayList<String>> textMap){
		//System.out.println("WHERE"+ cond.getText());
		if(cond.getChild(1).getText().equals("eq") || cond.getChild(1).getText().equals("=")){
			String lvar = cond.getChild(0).getText();
			String rvar = cond.getChild(2).getText();
			if(!lvar.startsWith("$") || !rvar.startsWith("$")){
				String textCond = lvar+" eq "+rvar;
				//System.out.println(textCond);
				String textVar;
				if(lvar.startsWith("$")){
					textVar = lvar;
				}
				else{
					textVar = rvar;
				}
				Integer part = globalMap.get(textVar);
				if(textMap.containsKey(part)){
					textMap.get(part).add(textCond);
				}
				else{
					ArrayList<String> textArr = new ArrayList<>();
					textArr.add(textCond);
					textMap.put(part, textArr);
				}
			}
			else{
				String ltag = lvar.substring(1);
				String rtag = rvar.substring(1);
				
				int leftPart = globalMap.get(lvar);
				int rightPart = globalMap.get(rvar);
				String key1 = Integer.toString(leftPart)+"-"+Integer.toString(rightPart);
				String key2 = Integer.toString(rightPart)+"-"+Integer.toString(leftPart);
				
				if(leftPart == rightPart){
					String samePartCond = lvar + " eq" +rvar + " ";
					if(textMap.containsKey(leftPart)){
						textMap.get(leftPart).add(samePartCond);
					}
					else{
						ArrayList<String> sameArr = new ArrayList<>();
						sameArr.add(samePartCond);
						textMap.put(leftPart, sameArr);
					}
				}
				else{
					if(whereMap.containsKey(key1)){
						whereMap.get(key1).get(leftPart).add(ltag);
						whereMap.get(key1).get(rightPart).add(rtag);
					}
					else if(whereMap.containsKey(key2)){
						whereMap.get(key2).get(leftPart).add(ltag);
						whereMap.get(key2).get(rightPart).add(rtag);
					}
					else{
						HashMap<Integer, ArrayList> lMap = new HashMap<>();
						ArrayList<String> leftArray = new ArrayList<>();
						ArrayList<String> rightArray = new ArrayList<>();
						
						leftArray.add(ltag);
						rightArray.add(rtag);
						lMap.put(leftPart, leftArray);
						lMap.put(rightPart, rightArray);
						
						whereMap.put(key1, lMap);
					}
				}
				
			}
		}
		else{
			traverseCond(cond.getChild(0),globalMap,whereMap,textMap);
			traverseCond(cond.getChild(2),globalMap,whereMap,textMap);
		}
	}
	
	public static String partionHelper(TreeMap<String, String> forMap, HashMap<Integer, ArrayList<String>> textMap, int part){
		String fStr = "for ";
		String wStr = " ";
		String rStr = "return <tuple> {";
		String result;
		
		Object[] tempArray = forMap.keySet().toArray();
		int length = tempArray == null ? 0 : tempArray .length;
		
		for(int i=0; i<length; i++){
			String key = (String) tempArray[i];
			String tag = key.substring(1);
			fStr += key + " in " + forMap.get(key) + ',';
			rStr += "<"+tag+">{"+key+"}</"+tag+">,";
		}
		if(textMap.containsKey(part)){
			ArrayList<String> whereText = textMap.get(part);
			wStr += "where ";
			for(int j=0; j<whereText.size(); j++){
				wStr += whereText.get(j) + "and ";
			}
			result = fStr.substring(0, fStr.length()-1) + "\n" + wStr.substring(0, wStr.length()-4) + "\n"+ rStr.substring(0, rStr.length()-1) + "} </tuple>";
		}
		
		else{
		result = fStr.substring(0, fStr.length()-1) + "\n" + rStr.substring(0, rStr.length()-1) + "} </tuple>";
		}
		
		return result;
	}
	
	public static String returnHelper(ParseTree rtree){
		String result = "";
		
		if(rtree.getChildCount() == 0){
			String leaf = rtree.getText();
			if(leaf.startsWith("$")){
				String tmp = leaf.substring(1,leaf.length());
				leaf = "$tuple/"+tmp +"/*";
			}
			return leaf;
		}
		else{
			for(int i=0; i<rtree.getChildCount(); i++){
			String aa = returnHelper(rtree.getChild(i));
			result += aa;
			}
		}
	return result;	
	}
}


