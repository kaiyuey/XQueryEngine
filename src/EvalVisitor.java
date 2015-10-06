import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EvalVisitor extends XQueryBaseVisitor<ArrayList<Node>> {

		DocumentBuilderFactory dbf;
		Document doc;
		Document outDoc;
		DocumentBuilder db;
		ArrayList<Node> nodeList = new ArrayList<Node>();
		HashMap<String, ArrayList<Node>> varHash = new HashMap<>();

		Stack<HashMap<String, ArrayList<Node>>> stk = new Stack<HashMap<String, ArrayList<Node>>>();
		
		EvalVisitor() {
			
			dbf = DocumentBuilderFactory.newInstance();
			try {
			db = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
			doc = null;
			
		}

		/**
		 * ap
		 */
		@Override 
		public ArrayList<Node> visitXQap(@NotNull XQueryParser.XQapContext ctx) { 
			ArrayList<Node> apList = visit(ctx.ap());
			return apList;
		}

		/**
		 * Var
		 */
		@Override 
		public ArrayList<Node> visitXQVar(@NotNull XQueryParser.XQVarContext ctx) { 
			ArrayList<Node> varList = new ArrayList<Node>();
			String var = ctx.getText();
			
			varList = varHash.get(var);
			//TODO:
			nodeList = new ArrayList<Node>(varList);
			//System.out.println("var"+varList);
			return varList;
		}

		/**
		 * StringConstant
		 */
		@Override 
		public ArrayList<Node> visitXQConst(@NotNull XQueryParser.XQConstContext ctx) { 
			String constant =  ctx.getText();
			//System.out.println(constant);
			ArrayList<Node> sc = new ArrayList<Node>();
			//TODO: nullpointer 
			sc.add(makeText(constant.substring(1, constant.length()-1)));
			return sc; 
		}

		//function to make a new text node
		private Node makeText(String str){
			Node node = outDoc.createTextNode(str);
			return node;
		}
		
		/**
		 * xq ',' xq	
		 */
		@Override 
		public ArrayList<Node> visitXQComma(@NotNull XQueryParser.XQCommaContext ctx) { 
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> left = visit(ctx.xq(0));
			nodeList = temp;
			ArrayList<Node> right = visit(ctx.xq(1));
			left.addAll(right);

			nodeList = new ArrayList<Node>(left);
			return left;
		}

		/**
		 * '(' xq ')'
		 */
		@Override 
		public ArrayList<Node> visitXQParen(@NotNull XQueryParser.XQParenContext ctx) { 
			return visit(ctx.xq());  
		}

		/**
		 * xq '/' rp
		 */
		@Override 
		public ArrayList<Node> visitXQRp(@NotNull XQueryParser.XQRpContext ctx) { 
			ArrayList<Node> left = visit(ctx.xq());
			//System.out.println("XQrpleft"+left.size());
			ArrayList<Node> right = visit(ctx.rp());
			//System.out.println("XQrpright"+right.size());

			ArrayList<Node> result = removeDulp(right);
			nodeList = new ArrayList<Node>(result);
			return result;
		}

		/**
		 * xq '//' rp
		 */
		@Override 
		public ArrayList<Node> visitXQdouble(@NotNull XQueryParser.XQdoubleContext ctx) { 
			ArrayList<Node> leftxq = visit(ctx.xq());
			for(int i = 0; i<leftxq.size(); i++){
				//System.out.println("leftsize:"+ leftxq);
				Node node =leftxq.get(i);
				getallChild(node, nodeList);
				
			}
			ArrayList<Node> rightrp = visit(ctx.rp());
			nodeList = new ArrayList<Node>(rightrp);
			return rightrp;
		}

		/**
		 * '<'NAME'>''{'xq'}''<''/'NAME'>'
		 */
		@Override 
		public ArrayList<Node> visitXQtag(@NotNull XQueryParser.XQtagContext ctx) {
			ArrayList<Node> children = visit(ctx.xq());
			String tag = ctx.NAME(0).getText();
			
			//System.out.println("tag"+tag);
			//System.out.println("children"+children);
			Node item = makeElement(tag,children);
			ArrayList<Node> newNode = new ArrayList<Node>();
			newNode.add(item);
			nodeList = new ArrayList<Node>(newNode);
			return  newNode;
		}

		// fuction to make new element node
		private Node makeElement(String t, ArrayList<Node> l){
			
			Node node = outDoc.createElement(t);
			for(int i =0; i<l.size(); i++){
				node.appendChild(outDoc.importNode(l.get(i).cloneNode(true), true));
			}
			//System.out.println("new node type: "+ node.getNodeType());
			return node;
			
		}
		
		/**
		 * forClause (letClause)? (whereClause)? returnClause
		 */
		@Override 
		public ArrayList<Node> visitXQFLWR(@NotNull XQueryParser.XQFLWRContext ctx) { 
			//System.out.println("flwr init varhash: "+varHash);

			visit(ctx.forClause());
			Stack<HashMap<String, ArrayList<Node>>> tempStack = new Stack<>();
			
			//System.out.println("flwr stk size:"+stk.size());
			//stk = (Stack) tempStack.clone();
			tempStack =	(Stack) stk.clone();

			if(ctx.letClause() != null){
				System.out.println("Have letClause");
				Stack templet = new Stack<HashMap<String, ArrayList<Node>>>();
				while(!tempStack.isEmpty()){
				varHash=tempStack.pop();
				visit(ctx.letClause());
				templet.push(varHash);}
				
				tempStack = (Stack) templet.clone();
			}
			//tempStack =	(Stack) stk.clone();
			if(ctx.whereClause() != null){
				//System.out.println("Have whereClause");
				ArrayList<Node> whereList = new ArrayList<Node>();
				Stack tempstk = new Stack<HashMap<String, ArrayList<Node>>>();
				//System.out.println(stk);
				//System.out.println("temp"+tempStack);

				while(!tempStack.isEmpty()){
					varHash = tempStack.pop();
					//System.out.println("where varHash"+varHash);
					//System.out.println("varHash size:"+varHash.size());
					whereList = visit(ctx.whereClause());

					if(!whereList.isEmpty()){
						//System.out.println("exist a list");
						
						HashMap<String, ArrayList<Node>> temphash = new HashMap<String, ArrayList<Node>>(varHash);
						tempstk.push(temphash);
					}
				}

				tempStack = tempstk;
				//System.out.println("stk size:"+tempStack.size());
			}
			
			ArrayList<Node> flwrList = new ArrayList<Node>();
			while(!tempStack.isEmpty()){
				varHash = tempStack.pop();
				//System.out.println("return varHash"+varHash);
				//System.out.println("stk size"+stk.size());
				flwrList.addAll(visit(ctx.returnClause()));
			}
			stk.clear();
			//flwrList = visit(ctx.returnClause());
			nodeList = new ArrayList<Node>(flwrList);
			return flwrList; 
		}

		/**
		 * letClause xq
		 */
		@Override 
		public ArrayList<Node> visitXQlet(@NotNull XQueryParser.XQletContext ctx) { 
			visit(ctx.letClause());
			//System.out.println(stk);
			//System.out.println(ctx.xq().getText());
			varHash = stk.pop();
			ArrayList<Node> result = visit(ctx.xq());
			
			//System.out.println(result);
			nodeList = new ArrayList<Node>(result);
			return result; 
		}

		/**milestone2 **/
		
		/**
		 * 'join' '(' xq ',' xq ',' varlist ',' varlist ')' 		#Join
		 */
		@Override 
		public ArrayList<Node> visitJoin(@NotNull XQueryParser.JoinContext ctx) { 
			ArrayList<Node> JoinList = new ArrayList<Node>();
			
			ArrayList<Node> leftXQ = visit(ctx.xq(0));
			ArrayList<Node> rightXQ = visit(ctx.xq(1));
			
			//get join lists
			ArrayList<String> leftvar = new ArrayList<String>();
			ArrayList<String> rightvar = new ArrayList<String>();

			for(int i=0; i<ctx.varlist(0).NAME().size(); i++){
				leftvar.add(ctx.varlist(0).NAME(i).getText());
			}
			//System.out.println("leftvarlist"+leftvar);
			for(int i=0; i<ctx.varlist(1).NAME().size(); i++){
				rightvar.add(ctx.varlist(1).NAME(i).getText());
			}
			//System.out.println("rightvarlist"+rightvar);
			
			//build join maps for different tuples
			HashMap<String, ArrayList<Node>> leftMap = new HashMap<>();
			HashMap<String, ArrayList<Node>> rightMap = new HashMap<>();
			
			for(int i=0; i<leftXQ.size(); i++){
				NodeList leftT = leftXQ.get(i).getChildNodes();
				//System.out.println("left size:"+ leftT.item(0));
				String leftMkey = "";
				ArrayList<Node> leftMValue = new ArrayList<Node>();
				for(String leftstr: leftvar){
					//System.out.println("leftstr "+leftstr);

					for(int j=0; j<leftT.getLength(); j++){
						
						Node left = leftT.item(j);
						//System.out.println("leftName "+left.getNodeName());

						if(left.getNodeName().equals(leftstr)){
							leftMkey += left.getTextContent();
							//System.out.println("left key: "+left.getFirstChild().getTextContent());
						}
					}
				}
				leftMValue.add(leftXQ.get(i));
				if(leftMap.containsKey(leftMkey)){
					//leftMap.get(leftMkey).add(leftXQ.get(i));

					ArrayList<Node> temp = leftMap.get(leftMkey);
					temp.add(leftXQ.get(i));
					leftMap.put(leftMkey, temp);
				}
				else{
					leftMap.put(leftMkey, leftMValue);
				}
			}
			//System.out.println("leftMap"+leftMap);

			for(int i=0; i<rightXQ.size(); i++){
				NodeList rightT = rightXQ.get(i).getChildNodes();
				String rightMkey = "";
				ArrayList<Node> rightMValue = new ArrayList<Node>();

				for(String rightstr: rightvar){
					for(int j=0; j<rightT.getLength(); j++){
						Node right = rightT.item(j);
						if(right.getNodeName().equals(rightstr)){
							rightMkey += right.getTextContent();
							
							//System.out.println("right key: "+ right.getFirstChild().getTextContent());
						}
					}
				}
				rightMValue.add(rightXQ.get(i));
				//System.out.println("key"+rightMValue);

				if(rightMap.containsKey(rightMkey)){
					//rightMap.get(rightMkey).add(rightXQ.get(i));
					ArrayList<Node> temp = rightMap.get(rightMkey);
					temp.add(rightXQ.get(i));
					rightMap.put(rightMkey, temp);
				}
				else{
					rightMap.put(rightMkey, rightMValue);
				}			
			}
			//System.out.println("lMap"+leftMap.size());

			//System.out.println("rMap"+rightMap.size());

			Set<String> keys = leftMap.keySet();

			for(String key: keys){
				//System.out.println("key"+key);
				if(rightMap.containsKey(key)){
					ArrayList<Node> lTT = leftMap.get(key);
					ArrayList<Node> rTT = rightMap.get(key);

					for(Node lnode: lTT){
						for(Node rnode: rTT){
							Node lrnode = lnode.cloneNode(true);
							NodeList lchildren = lnode.getChildNodes();
							Node rr = rnode.cloneNode(true);
							NodeList rchildren = rr.getChildNodes();
							
							while(rchildren.getLength()>0){
								lrnode.appendChild(rr.getFirstChild());
							}
							JoinList.add(lrnode);
						}
					}
				}
			}
			nodeList = new ArrayList<Node>(JoinList);
			//System.out.println("nodeList size: "+JoinList.size());
			return JoinList; 
		}

		/**
		 * 'for' Var 'in' xq ( ',' Var 'in' xq)*
		 */
		@Override 
		public ArrayList<Node> visitXQfor(@NotNull XQueryParser.XQforContext ctx) {
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<String> varStr = new ArrayList<String>();

			HashMap<String, ArrayList<Node>> hash = new HashMap<String, ArrayList<Node>>(varHash);
			
			//System.out.println("for varhash"+varHash);
			for(int i=0; i<ctx.Var().size();i++){
				varStr.add(ctx.Var(i).getText());
			}
			//System.out.println(varStr);

			stk.clear();
			//System.out.println("stk1.size"+stk.size());
			forHelper(varStr,ctx,hash,0);
			//System.out.println("stk"+stk.get(0));
			//System.out.println("stk2.size"+stk.size());

			return temp; 
		}

		private void forHelper(ArrayList<String> varStr, XQueryParser.XQforContext ctx,	HashMap<String, ArrayList<Node>> hash, int step){
			if(step == varStr.size()){
				//System.out.println("hash size:"+hash.size());
				//System.out.println("for"+hash);
				HashMap<String, ArrayList<Node>> stkHash = new HashMap<>(hash);

				stk.push(stkHash);
				
				//System.out.println("inner stk"+stk.get(0));

				return;
			}

			ArrayList<Node> xqList = visit(ctx.xq(step));
			//System.out.println("xqL"+xqList);

			hash = new HashMap<String, ArrayList<Node>>(varHash);

			for(int i=0; i<xqList.size();i++){
				ArrayList<Node> item = new ArrayList<Node>();

				item.add(xqList.get(i));
				//HashMap<String, ArrayList<Node>> hash = new HashMap<>(varHash);
				hash.put(varStr.get(step), item);
				varHash = (hash);

				//stk.push(varHash);
				forHelper(varStr,ctx,hash,step+1);	
				hash.remove(varStr.get(step));
			}
		}
		
		/**
		 * 'let' Var ':=' xq (',' Var ':=' xq)*
		 */
		@Override 
		public ArrayList<Node> visitLet(@NotNull XQueryParser.LetContext ctx) { 
			//HashMap<String, ArrayList<Node>> hash = new HashMap<>(varHash);
			//System.out.println("let"+varHash);
			for(int i=0; i<ctx.Var().size(); i++){
				String var = ctx.Var(i).getText();
				//System.out.println("var"+var);
				ArrayList<Node> elements = visit(ctx.xq(i));
				//System.out.println("elements"+elements);
				varHash.put(var,elements);
			}
			stk.push(varHash);
			//System.out.println(stk);
			return null;  
		}

		/**
		 * 'where' cond	
		 */
		@Override 
		public ArrayList<Node> visitWhere(@NotNull XQueryParser.WhereContext ctx) {
			ArrayList<Node> whereList = new ArrayList<Node>();
			whereList = visit(ctx.cond());
			return whereList; 
		}

		/**
		 * 'return' xq
		 */
		@Override 
		public ArrayList<Node> visitReturn(@NotNull XQueryParser.ReturnContext ctx) {
			ArrayList<Node> returnList = new ArrayList<Node>();
			returnList = visit(ctx.xq());
			return returnList; 
		}

		/**
		 * xq '=' xq 	xq 'eq' xq 
		 */
		@Override 
		public ArrayList<Node> visitCondEqual(@NotNull XQueryParser.CondEqualContext ctx) { 
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> equal = new ArrayList<Node>();
			ArrayList<Node> left = visit(ctx.xq(0));
			nodeList = new ArrayList<Node>(temp);
			ArrayList<Node> right = visit(ctx.xq(1));
			
			//System.out.println("left"+left);
			//System.out.println("right"+right);
			
			for(int j = 0; j<left.size(); j++){
				for(int k = 0; k<right.size(); k++){
					Node lnode = left.get(j);
					Node rnode = right.get(k);
					if(lnode.isEqualNode(rnode)) {
						equal.add(lnode);
						equal.add(rnode);
					}
				}
			}
			//System.out.println("equal"+equal.size());

			return equal;
		}

		/**
		 * xq '==' xq | xq 'is' xq														# CondIsSame										
		 */
		@Override 
		public ArrayList<Node> visitCondIsSame(@NotNull XQueryParser.CondIsSameContext ctx) { 
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> IsSame = new ArrayList<Node>();
			ArrayList<Node> left = visit(ctx.xq(0));
			nodeList = new ArrayList<Node>(temp);
			ArrayList<Node> right = visit(ctx.xq(1));
			
			//System.out.println("left"+left);
			//System.out.println("right"+right);
			
			for(int j = 0; j<left.size(); j++){
				for(int k = 0; k<right.size(); k++){
					Node lnode = left.get(j);
					Node rnode = right.get(k);
					if(lnode.isSameNode(rnode)) {
						IsSame.add(lnode);
						IsSame.add(rnode);
					}
				}
			}
			//System.out.println("same"+IsSame.size());

			return IsSame;
		}

		/**
		 * 'empty' '(' xq ')'
		 */
		@Override 
		public ArrayList<Node> visitCondEmpty(@NotNull XQueryParser.CondEmptyContext ctx) {
			ArrayList<Node> condEmpty =new ArrayList<Node>();
			condEmpty = visit(ctx.xq());
			ArrayList<Node> empty = new ArrayList<Node>();
			if(condEmpty.isEmpty()){
				empty.add(null);
			}
				
			return empty; 
		}

		/**
		 * 'some' Var 'in' xq (',' Var 'in' xq) *  'satisfies' cond
		 * 	∃Var1∈[XQ1]X...∃Varn∈[XQn]XCond
		 */
		//TODO: 
		@Override 
		public ArrayList<Node> visitCondSome(@NotNull XQueryParser.CondSomeContext ctx) { 
			ArrayList<Node> some = new ArrayList<Node>();
			HashMap<String, ArrayList<Node>> hash = new HashMap<>(varHash);
			ArrayList<String> someVarStr = new ArrayList<String>();
			Stack<HashMap<String, ArrayList<Node>>> someStk = new Stack<>();
			for(int i =0; i<ctx.Var().size();i++){
				someVarStr.add(ctx.Var(i).getText());
			}
			
			stk.clear();
			someHelper(someVarStr, ctx, hash, 0);
			
			someStk = stk;
			while(!someStk.isEmpty()){
				varHash = someStk.pop();
				//System.out.println("som varHash"+varHash);
				//System.out.println("som cond "+ ctx.cond().getText());
				some.addAll(visit(ctx.cond()));
			}
			return some;  
		}

		
		private void someHelper(ArrayList<String> varStr, XQueryParser.CondSomeContext ctx,	HashMap<String, ArrayList<Node>> hash, int step){
			if(step == varStr.size()){
								//System.out.println("hash size:"+hash.size());
								//System.out.println(hash);
				HashMap<String, ArrayList<Node>> stkHash = new HashMap<>(hash);
				stk.push(stkHash);
								
								//System.out.println("inner stk"+stk.get(0));

				return;
			}
			ArrayList<Node> xqList = visit(ctx.xq(step));
			hash = new HashMap<>(varHash);
			for(int i=0; i<xqList.size();i++){
				ArrayList<Node> item = new ArrayList<Node>();
				item.add(xqList.get(i));
								//HashMap<String, ArrayList<Node>> hash = new HashMap<>(varHash);
				hash.put(varStr.get(step), item);
								//stk.push(varHash);
				varHash = (hash);

				someHelper(varStr,ctx,hash,step+1);	
				hash.remove(varStr.get(step));
			}
		}
		
		/**
		 * '(' cond ')'
		 */
		@Override 
		public ArrayList<Node> visitCondParen(@NotNull XQueryParser.CondParenContext ctx) { 
			ArrayList<Node> nodes = visit(ctx.cond());
			nodeList = new ArrayList<Node>(nodes);
			return nodes;
		}
		
		/**
		 * cond 'and' cond
		 */
		@Override 
		public ArrayList<Node> visitCondAnd(@NotNull XQueryParser.CondAndContext ctx) { 
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			HashMap<String, ArrayList<Node>> tempHash = new HashMap<>(varHash);
			
			ArrayList<Node> left = visit(ctx.cond(0));
			
			varHash = tempHash;
			nodeList = new ArrayList<Node>(temp);   //do i need deep copy
			ArrayList<Node> right = visit(ctx.cond(1));
			//System.out.println("and left.size:"+ left.size());
			//System.out.println("and right size:"+ right.size());
			ArrayList<Node> AndList = new ArrayList<Node>();
			
			if(!left.isEmpty() && !right.isEmpty()){
				AndList.add(null);
			}
			//nodeList = new ArrayList<Node>(AndList);
			return AndList; 
		}

		/**
		 * not cond
		 */
		@Override 
		public ArrayList<Node> visitCondNot(@NotNull XQueryParser.CondNotContext ctx) { 
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> condList = visit(ctx.cond());
			ArrayList<Node> NotList = new ArrayList<Node>();
			
			if(condList.isEmpty()){
				NotList.add(null);
			}
			//nodeList = new ArrayList<Node>(NotList);
			return NotList; 
		}

		/**
		 *	cond 'or' cond 													# CondOr	
		 */
		@Override 
		public ArrayList<Node> visitCondOr(@NotNull XQueryParser.CondOrContext ctx) { 
			ArrayList<Node> orList = new ArrayList<Node>();
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			HashMap<String, ArrayList<Node>> tempHash = new HashMap<>(varHash);

			ArrayList<Node> left = visit(ctx.cond(0));
			nodeList = temp;
			varHash = tempHash;

			ArrayList<Node> right = visit(ctx.cond(1));
			
			left.addAll(right);
			orList = removeDulp(left);
			nodeList = new ArrayList<Node>(orList);
			return orList;
		}

		
		
		
		/**
		 * NAME '/' rp 				# APChild
		 */
		@Override 
		public ArrayList<Node> visitAPChild(XQueryParser.APChildContext ctx) { 
			String file = ctx.StringConstant().getText();
			String fname = file.substring(1, file.length()-1);
			System.out.println(fname);
			try {
				//db = dbf.newDocumentBuilder();
				doc = db.parse(fname);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Node root = doc.getDocumentElement();
			//System.out.println(root);
			
			nodeList.add(root);
			ArrayList<Node> apChild = visit(ctx.rp());
			nodeList = new ArrayList<Node>(apChild);

			return apChild; 
			
			}			

		/**
		 * NAME '//' rp			# APSubtree
		 */
		@Override 
		public ArrayList<Node> visitAPSubtree(XQueryParser.APSubtreeContext ctx) { 
			String file = ctx.StringConstant().getText();
			String fname = file.substring(1, file.length()-1);
			ArrayList<Node> apSub = new ArrayList<Node>();
			try {
				//db = dbf.newDocumentBuilder();
				doc = db.parse(fname);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//TODO all elements' rp?
			Node root = doc.getDocumentElement();
			//ArrayList<Node> allnodes = new ArrayList<Node>();
			nodeList.add(root);

			getallChild(root, nodeList);
			//System.out.println(nodeList.size());

			apSub = visit(ctx.rp());
			nodeList= new ArrayList<Node>(apSub);
			return apSub;				
		}
				
		/**
		 * rp '//' rp	# RPSubtree
		 */
		@Override
		public ArrayList<Node> visitRPSubtree(XQueryParser.RPSubtreeContext ctx) { 
			ArrayList<Node> left = new ArrayList<Node>();
			left = visit(ctx.rp(0));
			ArrayList<Node> allchildren = new ArrayList<Node>();
			for(int i = 0; i<left.size(); i++){
				//System.out.println("leftsize:"+ allchildren.size());
				Node node =left.get(i);
				getallChild(node, nodeList);
				
			}
			allchildren = visit(ctx.rp(1));
			// complex
			nodeList = new ArrayList<Node>(allchildren);
			return allchildren ; 
		}
		
		/**get all children nodes of root*/
		public void getallChild(Node root, ArrayList<Node> allnodes){
			NodeList children = root.getChildNodes();
			for(int i = 0; i< children.getLength(); i++){
				allnodes.add(children.item(i));
				getallChild(children.item(i),allnodes);
			}
			return;
		}
		
		/**
		 * rp ',' rp	# RPComma
		 * merge
		 */
		@Override 
		public ArrayList<Node> visitRPComma(XQueryParser.RPCommaContext ctx) {
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> left = visit(ctx.rp(0));
			nodeList = temp;
			ArrayList<Node> right = visit(ctx.rp(1));
			left.addAll(right);

			nodeList = new ArrayList<Node>(left);
			return left;
		}
		
		/**
		 * NAME	 # RPTag
		 * < c | x ← [∗]R(n), tag(n) = tagName >
		 */
		@Override 
		public ArrayList<Node> visitRPTag(XQueryParser.RPTagContext ctx) { 
			//TODO: tag? Initialize
			ArrayList<Node> tag = new ArrayList<Node>();
			for (int i = 0; i < nodeList.size(); i++){
				Node node = nodeList.get(i);
				NodeList allnodes = node.getChildNodes();
				//System.out.println("taglength:"+ allnodes.getLength());

				for (int j = 0; j <allnodes.getLength(); j++){
					Node element = allnodes.item(j);
					//System.out.println("element" + element.getNodeName());
					//System.out.println(ctx.NAME().getText());

					//TODO:
					if(element.getNodeType() == Node.ELEMENT_NODE && element.getNodeName().equals(ctx.NAME().getText())){
						//System.out.println("adding node: "+element.getNodeName());
						tag.add(element);
						//System.out.println("element" + element.getNodeName());
					}
				}				
			}
			nodeList = new ArrayList<Node>(tag);

			return tag;
		}
		
		/**
		 * '.'	 # RPCurrent
		 * return current nodeList <n>
		 */
		@Override 
		public ArrayList<Node> visitRPCurrent(XQueryParser.RPCurrentContext ctx) { 
			//TODO: same?
			return nodeList; 
		}
		
		/**
		 * rp '[' filter ']' 	# RPFilter
		 */
		@Override 
		public ArrayList<Node> visitRPFilter(XQueryParser.RPFilterContext ctx) { 
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> rpList = visit(ctx.rp());
			ArrayList<Node> fList = visit(ctx.filter());
			
			nodeList = new ArrayList<Node>(fList);
			//TODO
			//TODO: filter should be boolean
			return fList;
		}
		
		/**
		 * 'text()'  # RPText
		 * txt(n) the text node associated to element node n
		 */
		@Override 
		public ArrayList<Node> visitRPText(XQueryParser.RPTextContext ctx) { 
			//TODO : do we need to update nodeList?
			ArrayList<Node> text = new ArrayList<Node>();
			for(int i =0; i < nodeList.size(); i++){
				NodeList nodes = nodeList.get(i).getChildNodes();
				for(int j = 0; j< nodes.getLength(); j++){
					Node node = nodes.item(j);
					if(node.getNodeType() == Node.TEXT_NODE){
						text.add(node);
					}
				}
			}
			//nodeList = text;
			return text;
		}
		
		/**
		 *'..'	# RPParent
		 *parent(n) a singleton list containing the parent of element node n, if n has a parent. The empty list otherwise.
		 */
		@Override 
		public ArrayList<Node> visitRPParent(XQueryParser.RPParentContext ctx) { 
			ArrayList<Node> parent = new ArrayList<Node>();
			for(int i =0; i < nodeList.size(); i++){
				Node node = nodeList.get(i);
				Node p = node.getParentNode();
				if(p != null && !parent.contains(p)){
					parent.add(p);
				}
			}			
			nodeList= new ArrayList<Node>(parent);
			return parent;
		}
		
		/**
		 *   '*'	# RPAllChild
		 *   return children(n); all children of nodes n 
		 */
		@Override 
		public ArrayList<Node> visitRPAllChild(XQueryParser.RPAllChildContext ctx) { 
			ArrayList<Node> nodes = new ArrayList<Node>();
			//TODO: children? only element nodes?
			for (int i = 0; i < nodeList.size(); i++){
				Node node = nodeList.get(i);
				NodeList allnodes = node.getChildNodes();
				for (int j = 0; j <allnodes.getLength(); j++){
					Node element = allnodes.item(j);
					if(element.getNodeType() == Node.ELEMENT_NODE){
						nodes.add(element);
					}
				}				
			}
			//System.out.println("*"+nodes.size());
			nodeList = new ArrayList<Node>(nodes);
			return nodes;
			}
		
		/**
		 * '(' rp ')'			# RPParen
		 * same as rp
		 */
		@Override 
		public ArrayList<Node> visitRPParen(XQueryParser.RPParenContext ctx) { 
			ArrayList<Node> pare = visit(ctx.rp());
			nodeList = new ArrayList<Node>(pare);
			return pare; 
		}
		
		/**
		 * '@' NAME		# RPAttr
		 * attribute is NAME
		 */
		@Override 
		public ArrayList<Node> visitRPAttr(XQueryParser.RPAttrContext ctx) { 
			//TODO: if attr is empty, can we get parent node? @ *?
			ArrayList<Node> attr = new ArrayList<Node>();
			for(int i =0; i < nodeList.size(); i++){
				NamedNodeMap nodes = nodeList.get(i).getAttributes();
				for(int j = 0; j< nodes.getLength(); j++){
					Node node = nodes.item(j);
					if(node.getTextContent().equals(ctx.NAME().getText())){
						attr.add(node);
					}
				}
			}
			return attr;
		}
		/**
		 * rp '/' rp	# RPChild
		 */
		@Override 
		public ArrayList<Node> visitRPChild(XQueryParser.RPChildContext ctx) { 
			//ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> left = visit(ctx.rp(0));
			//System.out.println("left"+left.size());
			ArrayList<Node> right = visit(ctx.rp(1));
			//System.out.println("right"+right.size());

			ArrayList<Node> result = removeDulp(right);
			nodeList = new ArrayList<Node>(result);
			return result;
		}
		
		private ArrayList<Node> removeDulp(ArrayList<Node> list) {
			// TODO not unique?
			HashSet<Node> nodeset = new HashSet<Node>();
			//System.out.println("set"+nodeset.size());
			ArrayList<Node> newList = new ArrayList<Node>();
			
			for(int i = 0; i<list.size();i++){
				 Node item = list.get(i);
					//System.out.println(item);
				   if (nodeset.add(item)){
				      newList.add(item);  
				   }
			}
			//System.out.println("newList"+nodeset.size());
			//System.out.println(newList);
			return newList; 
		}


		/**
		 * filter 'and' filter	# FAnd
		 */
		@Override 
		public ArrayList<Node> visitFAnd(XQueryParser.FAndContext ctx) { 
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> left = visit(ctx.filter(0));
			nodeList = temp;
			ArrayList<Node> right = visit(ctx.filter(1));
			
			ArrayList<Node> AndList = new ArrayList<Node>();
			
			for(int i = 0; i<left.size(); i++){
				Node lnode = left.get(i);
				if(right.contains(lnode)){
					AndList.add(lnode);
				}
			}
			nodeList = new ArrayList<Node>(AndList);
			return AndList;
		}
		
		/**
		 * rp '=' rp   =   rp 'equal' rp	 # FEquiv
		 */
		@Override 
		public ArrayList<Node> visitFEquiv(XQueryParser.FEquivContext ctx) { 
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> equal = new ArrayList<Node>();

			for(int i=0; i<temp.size(); i++){
				Node item = temp.get(i);
				//System.out.println("item" + item);

				nodeList.clear();
				nodeList.add(item);
				
				ArrayList<Node> left = visit(ctx.rp(0));

				//System.out.println("element" + ctx.rp(0).getText());

				nodeList.clear();
				nodeList.add(item);	
				
				ArrayList<Node> right = visit(ctx.rp(1));

				//System.out.println("left"+left);
				//System.out.println("right"+right);

				ArrayList<Node> exist = new ArrayList<Node>();
				for(int j = 0; j<left.size(); j++){
					for(int k = 0; k<right.size(); k++){
						Node lnode = left.get(j);
						Node rnode = right.get(k);

						if(lnode.isEqualNode(rnode)) {
							exist.add(lnode);
							exist.add(rnode);
							System.out.println("exist"+lnode);
						}
					//TODO: equal and is ?
					}
				}
				if(exist.size() != 0){
					equal.add(item);
				}
			}
			nodeList = new ArrayList<Node>(equal);
			return equal;
		}
		
		/**
		 * rp					# FRP
		 */
		@Override 
		public ArrayList<Node> visitFRP(XQueryParser.FRPContext ctx) { 
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> FRPList = new ArrayList<Node>();
			for(int i = 0; i<temp.size();i++){
				Node item = temp.get(i);
				nodeList.clear();
				nodeList.add(item);
				ArrayList<Node> rpList = visit(ctx.rp());
				if(rpList.size() != 0){
					FRPList.add(item);
				}
			}
			nodeList = new ArrayList<Node>(FRPList);
			return FRPList;
		}
		
		/**
		 * rp '==' rp	= rp'is' rp		# FIsSame
		 */
		@Override 
		public ArrayList<Node> visitFIsSame(XQueryParser.FIsSameContext ctx) { 
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> IsSame = new ArrayList<Node>();

			for(int i=0; i<temp.size(); i++){
				Node item = temp.get(i);
				nodeList.clear();
				nodeList.add(item);
				ArrayList<Node> left = visit(ctx.rp(0));
				nodeList.clear();
				nodeList.add(item);			
				ArrayList<Node> right = visit(ctx.rp(1));
				
				ArrayList<Node> exist = new ArrayList<Node>();
				for(int j = 0; j<left.size(); j++){
					for(int k = 0; k<right.size(); k++){
						Node lnode = left.get(j);
						Node rnode = right.get(k);
						if(lnode.isSameNode(rnode)) {
							exist.add(lnode);
							exist.add(rnode);
						}
					//TODO: equal and is ?
					}
				}
				if(exist.size() != 0){
					IsSame.add(item);
				}
			}
			nodeList = new ArrayList<Node>(IsSame);
			return IsSame;
		}
		
		/**
		 * filter 'or' filter	# FOr
		 */
		@Override 
		public ArrayList<Node> visitFOr(XQueryParser.FOrContext ctx) { 
			ArrayList<Node> orList = new ArrayList<Node>();
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> left = visit(ctx.filter(0));
			nodeList = temp;
			ArrayList<Node> right = visit(ctx.filter(1));
			
			left.addAll(right);
			orList = removeDulp(left);
			nodeList = new ArrayList<Node>(orList);
			return orList;
		}
		
		/**
		 *'not' filter			# FNot		 
		 */
		@Override 
		public ArrayList<Node> visitFNot(XQueryParser.FNotContext ctx) {
			ArrayList<Node> temp = new ArrayList<Node>(nodeList);
			ArrayList<Node> fList = visit(ctx.filter());
			ArrayList<Node> NotList = new ArrayList<Node>();
			
			for(int i=0; i<temp.size(); i++){
				Node item = temp.get(i);
				if(!fList.contains(item)){
					NotList.add(item);
				}
			}
			nodeList = new ArrayList<Node>(NotList);
			return NotList;
		}
		
		/**
		 * '(' filter ')'		# FParen
		 */
		@Override 
		public ArrayList<Node> visitFParen(XQueryParser.FParenContext ctx) { 
			ArrayList<Node> nodes = visit(ctx.filter());
			nodeList = new ArrayList<Node>(nodes);
			return nodes;
		}
}
