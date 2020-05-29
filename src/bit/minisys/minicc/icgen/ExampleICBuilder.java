package bit.minisys.minicc.icgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import bit.minisys.minicc.parser.ast.*;
public class ExampleICBuilder implements ASTVisitor{

	private Map<ASTNode, ASTNode> map;				// 使用map存储子节点的返回值，key对应子节点，value对应返回值，value目前类别包括ASTIdentifier,TemportaryValue
	private List<Quat> quats;						// 生成的四元式列表
	private Integer tmpId;							// 临时变量编号
	
	private SymbolTable globalSymbolTable = new SymbolTable();//全局符号表
	private HashMap<String, SymbolTable> funcSymbolTable= new HashMap<String,SymbolTable>();//各个函数的符号表
	String funcName = null;
	private ArrayList<String> errors = new ArrayList<String>();
	private int iterationFlag = 0;
	private boolean breakFlag = false;
	public ExampleICBuilder() {
		map = new HashMap<ASTNode, ASTNode>();
		quats = new LinkedList<Quat>();
		tmpId = 0;
	}
	public List<Quat> getQuats() {
		return quats;
	}

	private int newQuat(String op, ASTNode res, ASTNode opnd1, ASTNode opnd2) {
		int index = quats.size();
		Quat q = new Quat(index, op, res, opnd1, opnd2);
		this.quats.add(q);
		return index;
	}
	@Override
	public void visit(ASTCompilationUnit program) throws Exception {
		for (ASTNode node : program.items) {
			if(node instanceof ASTDeclaration) {
				visit((ASTDeclaration)node);
			}else if(node instanceof ASTFunctionDefine) {
				visit((ASTFunctionDefine)node);
			}
		}
	}

	@Override
	public void visit(ASTDeclaration declaration) throws Exception {
		for (ASTInitList  iterable : declaration.initLists) {
			SymbolEntry se = new SymbolEntry();
			se.name = ((ASTVariableDeclarator)iterable.declarator).identifier.value;
			se.kind = SymbolKind.SYM_VAR;
			se.tokenId = ((ASTVariableDeclarator)iterable.declarator).identifier.tokenId;
			se.type = declaration.specifiers.get(declaration.specifiers.size()-1).value;
			if (this.funcName == null) {
				if(this.globalSymbolTable.getSymbolEntryByName(se.name) != null){
					errors.add("_var_defined_again.c"+":"+se.tokenId+"\t"+se.name);
				}else{
					this.globalSymbolTable.insertSybolEnty(se);
				}
			}else {
				SymbolTable st = funcSymbolTable.get(funcName);
				if(st.getSymbolEntryByName(se.name) != null){
					errors.add("_var_defined_again.c"+":"+se.tokenId+"\t"+se.name);
				}else{
					st.insertSybolEnty(se);
					this.funcSymbolTable.put(funcName, st);
				}
			}
			if(!iterable.exprs.isEmpty()) {//带初始化的定义
				String op = "=";
				ASTNode res = ((ASTVariableDeclarator)iterable.declarator).identifier;
				ASTNode opnd1 = null;
				ASTNode opnd2 = null;
				ASTExpression expr = iterable.exprs.get(0);
				if (expr instanceof ASTIdentifier) {
					opnd1 = expr;
				}else if(expr instanceof ASTIntegerConstant) {
					opnd1 = expr;
				}else if(expr instanceof ASTBinaryExpression) {
					ASTBinaryExpression value = (ASTBinaryExpression)expr;
					op = value.op.value;
					visit(value.expr1);
					opnd1 = map.get(value.expr1);
					visit(value.expr2);
					opnd2 = map.get(value.expr2);
				}else if (expr instanceof ASTUnaryExpression) {
					ASTUnaryExpression value = (ASTUnaryExpression)expr;
					op = value.op.value;
					visit(value.expr);
					opnd1 = map.get(value.expr);
					opnd2 = null;
				}else if (expr instanceof ASTPostfixExpression) {
					ASTPostfixExpression value = (ASTPostfixExpression)expr;
					op = value.op.value;
					visit(value.expr);
					opnd1 = map.get(value.expr);
					opnd2 = null;
				}
				
				newQuat(op, res, opnd1, opnd2);
				map.put(iterable, res);
			}
		}
	}

	@Override
	public void visit(ASTArrayDeclarator arrayDeclarator) throws Exception {
		
	}

	@Override
	public void visit(ASTVariableDeclarator variableDeclarator) throws Exception {
		
	}

	@Override
	public void visit(ASTFunctionDeclarator functionDeclarator) throws Exception {
		
	}

	@Override
	public void visit(ASTParamsDeclarator paramsDeclarator) throws Exception {
		SymbolTable st;
		st = funcSymbolTable.get(funcName);
		SymbolEntry sEntry = new SymbolEntry();
		sEntry.name = ((ASTVariableDeclarator)paramsDeclarator.declarator).identifier.value;
		sEntry.kind = SymbolKind.SYM_VAR;
		sEntry.tokenId = ((ASTVariableDeclarator)paramsDeclarator.declarator).identifier.tokenId;
		sEntry.type = paramsDeclarator.specfiers.get(paramsDeclarator.specfiers.size()-1).value;
		
		if(st.getSymbolEntryByName(sEntry.name) != null){
			errors.add("_var_defined_again.c"+":"+sEntry.tokenId+"\t"+sEntry.name);
		}else{
			st.insertSybolEnty(sEntry);
			funcSymbolTable.put(funcName, st);
		}
	}

	@Override
	public void visit(ASTArrayAccess arrayAccess) throws Exception {
		
	}

	@Override
	public void visit(ASTBinaryExpression binaryExpression) throws Exception {
		String op = binaryExpression.op.value;
		ASTNode res = null;
		ASTNode opnd1 = null;
		ASTNode opnd2 = null;
		
		if (op.equals("=")||op.equals("-=")||op.equals("+=")) {
			// 赋值操作,获取被赋值的对象res
			visit(binaryExpression.expr1);
			res = map.get(binaryExpression.expr1);
			if (binaryExpression.expr2 instanceof ASTIdentifier) {
				opnd1 = binaryExpression.expr2;
			}else if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
				opnd1 = binaryExpression.expr2;
			}else if(binaryExpression.expr2 instanceof ASTBinaryExpression) {
				visit(binaryExpression.expr2);
				opnd1 = map.get(binaryExpression.expr2);
			}else if (binaryExpression.expr2 instanceof ASTUnaryExpression) {
				visit(binaryExpression.expr2);
				opnd1 = map.get(binaryExpression.expr2);
			}else if (binaryExpression.expr2 instanceof ASTPostfixExpression) {
				visit(binaryExpression.expr2);
				opnd1 = map.get(binaryExpression.expr2);
			}
		}else if (op.equals("+")||op.equals("-")||op.equals("*")||op.equals("/")||op.equals("%")||op.equals("<<")||op.equals(">>")||op.equals("<")||op.equals(">")||op.equals(">=")||op.equals("<=")||op.equals("==")||op.equals("!=")) {
			//各种二元运算操作，结果存储到中间变量
			res = new TemporaryValue(++tmpId);
			//临时变量储存到符号表
			SymbolEntry sEntry = new SymbolEntry();
			sEntry.kind = SymbolKind.SYM_TEMP;
			sEntry.name = ((TemporaryValue)res).name();
			if (funcName == null) {
				globalSymbolTable.insertSybolEnty(sEntry);
			}else {
				funcSymbolTable.get(funcName).insertSybolEnty(sEntry);
			}
			
			visit(binaryExpression.expr1);
			opnd1 = map.get(binaryExpression.expr1);
			visit(binaryExpression.expr2);
			opnd2 = map.get(binaryExpression.expr2);
		}else {
		}
		// build quat
		newQuat(op, res, opnd1, opnd2);
		map.put(binaryExpression, res);
	}

	@Override
	public void visit(ASTBreakStatement breakStat) throws Exception {
		if (this.iterationFlag > 0) {
			this.iterationFlag--;
			this.breakFlag = true;
		}
		else {
			errors.add("_break_not_in_loop.c");
		}
	}

	@Override
	public void visit(ASTContinueStatement continueStatement) throws Exception {
		
	}

	@Override
	public void visit(ASTCastExpression castExpression) throws Exception {
		
	}

	@Override
	public void visit(ASTCharConstant charConst) throws Exception {
		
	}

	@Override
	public void visit(ASTCompoundStatement compoundStat) throws Exception {
		for (ASTNode node : compoundStat.blockItems) {
			if(node instanceof ASTDeclaration) {
				visit((ASTDeclaration)node);
			}else if (node instanceof ASTStatement) {
				visit((ASTStatement)node);
			}
		}
	}

	@Override
	public void visit(ASTConditionExpression conditionExpression) throws Exception {
		
	}

	@Override
	public void visit(ASTExpression expression) throws Exception {
		if(expression instanceof ASTArrayAccess) {
			visit((ASTArrayAccess)expression);
		}else if(expression instanceof ASTBinaryExpression) {
			visit((ASTBinaryExpression)expression);
		}else if(expression instanceof ASTCastExpression) {
			visit((ASTCastExpression)expression);
		}else if(expression instanceof ASTCharConstant) {
			visit((ASTCharConstant)expression);
		}else if(expression instanceof ASTConditionExpression) {
			visit((ASTConditionExpression)expression);
		}else if(expression instanceof ASTFloatConstant) {
			visit((ASTFloatConstant)expression);
		}else if(expression instanceof ASTFunctionCall) {
			visit((ASTFunctionCall)expression);
		}else if(expression instanceof ASTIdentifier) {
			visit((ASTIdentifier)expression);
		}else if(expression instanceof ASTIntegerConstant) {
			visit((ASTIntegerConstant)expression);
		}else if(expression instanceof ASTMemberAccess) {
			visit((ASTMemberAccess)expression);
		}else if(expression instanceof ASTPostfixExpression) {
			visit((ASTPostfixExpression)expression);
		}else if(expression instanceof ASTStringConstant) {
			visit((ASTStringConstant)expression);
		}else if(expression instanceof ASTUnaryExpression) {
			visit((ASTUnaryExpression)expression);
		}else if(expression instanceof ASTUnaryTypename){
			visit((ASTUnaryTypename)expression);
		}
	}

	@Override
	public void visit(ASTExpressionStatement expressionStat) throws Exception {
		for (ASTExpression node : expressionStat.exprs) {
			visit((ASTExpression)node);
		}
	}

	@Override
	public void visit(ASTFloatConstant floatConst) throws Exception {
		
	}

	@Override
	public void visit(ASTFunctionCall funcCall) throws Exception {
		if (this.globalSymbolTable.getSymbolEntryByName(((ASTIdentifier)funcCall.funcname).value) == null) {
			errors.add("_var_not_defined.c"+":"+((ASTIdentifier)funcCall.funcname).tokenId.toString()+"\t"+((ASTIdentifier)funcCall.funcname).value);
		}
		for (ASTExpression iterable : funcCall.argList) {
			visit(iterable);
		}
	}

	@Override
	public void visit(ASTGotoStatement gotoStat) throws Exception {
		
	}

	@Override
	public void visit(ASTIdentifier identifier) throws Exception {
		map.put(identifier, identifier);
		//语义检查
		SymbolTable st;
		if (funcName == null) {
			st = this.globalSymbolTable;
		}else {
			st = funcSymbolTable.get(funcName);
		}
		if (st.getSymbolEntryByName(identifier.value) == null && this.globalSymbolTable.getSymbolEntryByName(identifier.value)==null) {
			errors.add("_var_not_defined.c"+":"+identifier.tokenId+"\t"+identifier.value);
		}
	}

	@Override
	public void visit(ASTInitList initList) throws Exception {
		
	}

	@Override
	public void visit(ASTIntegerConstant intConst) throws Exception {
		map.put(intConst, intConst);
	}

	@Override
	public void visit(ASTIterationDeclaredStatement iterationDeclaredStat) throws Exception {
		
	}

	@Override
	public void visit(ASTIterationStatement iterationStat) throws Exception {
		for (ASTExpression iterable : iterationStat.init) {
			visit(iterable);
		}
		int iterationBeginIndex = this.quats.size();
		for (ASTExpression iterable : iterationStat.cond) {
			visit(iterable);
		}
		newQuat("jt", new JumpIndex(this.quats.size()-1+3), null, null);
		int jfIndex = newQuat("jf", null, null, null);
		visit((ASTCompoundStatement)iterationStat.stat);

		for (ASTExpression iterable : iterationStat.step) {
			visit(iterable);
		}
		newQuat("j", new JumpIndex(iterationBeginIndex), null, null);
		this.quats.get(jfIndex).setRes(new JumpIndex(this.quats.size()-1+1));
	}

	@Override
	public void visit(ASTLabeledStatement labeledStat) throws Exception {
		
	}

	@Override
	public void visit(ASTMemberAccess memberAccess) throws Exception {
		
	}

	@Override
	public void visit(ASTPostfixExpression postfixExpression) throws Exception {
		String op = postfixExpression.op.value;
		ASTNode res = null;
		ASTNode opnd1 = null;
		ASTNode opnd2 = null;
		
		if (op.equals("++")||op.equals("--")) {
			visit(postfixExpression.expr);
			res = map.get(postfixExpression.expr);
			if (postfixExpression.expr instanceof ASTIdentifier) {
				opnd1 = postfixExpression.expr;
			}else if (postfixExpression.expr instanceof ASTUnaryExpression) {
				ASTUnaryExpression value = (ASTUnaryExpression)postfixExpression.expr;
				op = value.op.value;
				visit(value.expr);
				opnd1 = map.get(value.expr);
				opnd2 = null;
			}else if (postfixExpression.expr instanceof ASTPostfixExpression) {
				ASTPostfixExpression value = (ASTPostfixExpression)postfixExpression.expr;
				op = value.op.value;
				visit(value.expr);
				opnd1 = map.get(value.expr);
				opnd2 = null;
			}
		}else {
		}

		newQuat(op, res, opnd1, opnd2);
		map.put(postfixExpression, res);
	}

	@Override
	public void visit(ASTReturnStatement returnStat) throws Exception {
		if (returnStat.expr != null) {
			for (ASTExpression iterable : returnStat.expr) {
				visit(iterable);
			}
		}
	}

	@Override
	public void visit(ASTSelectionStatement selectionStat) throws Exception {
		visit(selectionStat.cond.get(0));
		newQuat("jt",new JumpIndex(this.quats.size()-1+3) ,null ,null );
		int jfIndex = newQuat("jf", null, null, null);
		visit(selectionStat.then);
		int jIndex = newQuat("j", null, null, null);
		this.quats.get(jfIndex).setRes(new JumpIndex(this.quats.size()-1+1));
		visit(selectionStat.otherwise);
		this.quats.get(jIndex).setRes(new JumpIndex(this.quats.size()-1+1));
	}

	@Override
	public void visit(ASTStringConstant stringConst) throws Exception {
		
	}

	@Override
	public void visit(ASTTypename typename) throws Exception {
		
	}

	@Override
	public void visit(ASTUnaryExpression unaryExpression) throws Exception {
		String op = unaryExpression.op.value;
		ASTNode res = null;
		ASTNode opnd1 = null;
		ASTNode opnd2 = null;
		if (op.equals("++")||op.equals("--")||op.equals("!")||op.equals("~")) {
			visit(unaryExpression.expr);
			res = map.get(unaryExpression.expr);
			if (unaryExpression.expr instanceof ASTIdentifier) {
				opnd1 = unaryExpression.expr;
			}else if (unaryExpression.expr instanceof ASTUnaryExpression) {
				ASTUnaryExpression value = (ASTUnaryExpression)unaryExpression.expr;
				op = value.op.value;
				visit(value.expr);
				opnd1 = map.get(value.expr);
				opnd2 = null;
			}else if (unaryExpression.expr instanceof ASTPostfixExpression) {
				ASTPostfixExpression value = (ASTPostfixExpression)unaryExpression.expr;
				op = value.op.value;
				visit(value.expr);
				opnd1 = map.get(value.expr);
				opnd2 = null;
			}
		}else {
		}
		newQuat(op, res, opnd1, opnd2);
		map.put(unaryExpression, res);
	}

	@Override
	public void visit(ASTUnaryTypename unaryTypename) throws Exception {
		
	}

	@Override
	public void visit(ASTFunctionDefine functionDefine) throws Exception {
		SymbolEntry symbolEntry = new SymbolEntry();
		this.funcName = ((ASTVariableDeclarator)((ASTFunctionDeclarator)functionDefine.declarator).declarator).identifier.value;
		symbolEntry.name = funcName;
		symbolEntry.kind = SymbolKind.SYM_FUNC;
		symbolEntry.tokenId = ((ASTVariableDeclarator)((ASTFunctionDeclarator)functionDefine.declarator).declarator).identifier.tokenId;
		symbolEntry.type = functionDefine.specifiers.get(functionDefine.specifiers.size()-1).value;
		if(this.globalSymbolTable.getSymbolEntryByName(symbolEntry.name) != null){
			errors.add("_var_defined_again.c"+":"+symbolEntry.tokenId+"\t"+symbolEntry.name);
		}else{
			this.globalSymbolTable.insertSybolEnty(symbolEntry);
		}
		
		SymbolTable st = new SymbolTable();
		this.funcSymbolTable.put(this.funcName, st);//为函数创建符号表
		for (ASTParamsDeclarator e : (((ASTFunctionDeclarator)functionDefine.declarator).params)) {//遍历函数的参数
			visit(e);
		}

		visit(functionDefine.body);//函数体
		this.funcName = null;
	}

	@Override
	public void visit(ASTDeclarator declarator) throws Exception {
		
	}

	@Override
	public void visit(ASTStatement statement) throws Exception {
		if(statement instanceof ASTIterationDeclaredStatement) {
			visit((ASTIterationDeclaredStatement)statement);
		}else if(statement instanceof ASTIterationStatement) {
			this.iterationFlag++;
			visit((ASTIterationStatement)statement);
			if (this.iterationFlag > 0  ) {
				if (this.breakFlag == false) {
					this.iterationFlag--;
				}else {
					
				}
			}else {
				if (this.breakFlag == true) {
					
				}else {
					this.errors.add("_break_not_in_loop.c");
				}
			}
		}else if(statement instanceof ASTCompoundStatement) {
			visit((ASTCompoundStatement)statement);
		}else if(statement instanceof ASTSelectionStatement) {
			visit((ASTSelectionStatement)statement);
		}else if(statement instanceof ASTExpressionStatement) {
			visit((ASTExpressionStatement)statement);
		}else if(statement instanceof ASTBreakStatement) {
			visit((ASTBreakStatement)statement);
		}else if(statement instanceof ASTContinueStatement) {
			visit((ASTContinueStatement)statement);
		}else if(statement instanceof ASTReturnStatement) {
			visit((ASTReturnStatement)statement);
		}else if(statement instanceof ASTGotoStatement) {
			visit((ASTGotoStatement)statement);
		}else if(statement instanceof ASTLabeledStatement) {
			visit((ASTLabeledStatement)statement);
		}
	}

	@Override
	public void visit(ASTToken token) throws Exception {
	}
}