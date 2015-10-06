/**
 * Define a grammar called Xquery
 */
grammar XQuery;

xq: Var														# XQVar					
	| StringConstant										# XQConst
	| ap													# XQap
	| '(' xq ')'											# XQParen
	| xq ',' xq												# XQComma
	| xq '/' rp												# XQRp
	| xq '//' rp											# XQdouble
	| '<'NAME'>''{'xq'}''<''/'NAME'>'						# XQtag
	| forClause (letClause)? (whereClause)? returnClause   		# XQFLWR
	| letClause xq											# XQlet
	| 'join' '(' xq ',' xq ',' varlist ',' varlist ')' 		#Join	
	;	

varlist: '[' NAME ( ',' NAME)* ']'							
			;
			
forClause:	'for' Var 'in' xq ( ',' Var 'in' xq)*			# XQfor
			;						

letClause:	'let' Var ':=' xq (',' Var ':=' xq)*			# let
			;						

whereClause: 'where' cond									# where
			;								

returnClause: 'return' xq								# return
				;										
	
cond: xq '=' xq 														# CondEqual							
	  | xq 'eq' xq 														# CondEqual
	  | xq '==' xq 														# CondIsSame										
	  | xq 'is' xq 														# CondIsSame			
	  | 'empty' '(' xq ')' 												# CondEmpty					
	  | 'some' Var 'in' xq (',' Var 'in' xq) *  'satisfies' cond   		# CondSome
	  | '(' cond ')'  													# CondParen				
	  | cond 'and' cond 												# CondAnd				
	  | cond 'or' cond 													# CondOr			
	  | 'not' cond 														# CondNot			
	  ;	
	  
	
ap: DOC '(' StringConstant ')' '/' rp 				# APChild
	| DOC '(' StringConstant ')' '//' rp			# APSubtree
	;


	
rp: NAME					# RPTag
	| '*'					# RPAllChild
	| '.'	 				# RPCurrent
	| '..'					# RPParent
	| 'text()'				# RPText
	| '(' rp ')'			# RPParen
	| rp '/' rp				# RPChild
	| rp '//' rp			# RPSubtree
	| rp '[' filter ']' 	# RPFilter
	| rp ',' rp				# RPComma
	| '@' NAME				# RPAttr
	;
		
filter:	rp					# FRP
	| rp '=' rp				# FEquiv
	| rp 'equal' rp			# FEquiv
	| rp '==' rp			# FIsSame
	| rp 'is' rp			# FIsSame
	| '(' filter ')'		# FParen
	| filter 'and' filter	# FAnd
	| filter 'or' filter	# FOr
	| 'not' filter			# FNot
	;
		 	
Var: '$' NAME;	
StringConstant: '"' [ \t\r\na-zA-Z0-9.!,_]+ '"';
DOC:	'document';
NAME:	[a-zA-Z0-9._]+;	

WS : [ \t\r\n]+ -> skip ;
