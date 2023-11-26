package pins.phase.synan;
import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.symbol.*;
import pins.phase.lexan.*;
import java.util.Vector;

import static pins.data.symbol.Token.*;

public class SynAn implements AutoCloseable {
	private LexAn lexan;
	private Symbol currSymbol;

	public SynAn(LexAn lexan) {
		this.lexan = lexan;
	}

	/**
	 * Accepts param token if it matches the current symbol token else throws an error
	 * @param t
	 * @return
	 */
	private Symbol confirmAndGo(Token t){
		if(currSymbol.token != t){
			throw new Report.Error(currSymbol.location, "Syntax Error :  Expected " + getLexeme(t) + " got: "+ currSymbol.lexeme);
		}
		Symbol return_symbol=currSymbol;
		currSymbol=lexan.lexer();

		return return_symbol;
	}

	/**
	 * Return the lexical representation of the token
	 * @param t
	 * @return
	 */
	private String getLexeme(Token t){
		return switch (t) {
			case SYM_LPARENT -> "(";
			case SYM_RPARENT -> ")";
			case SYM_LSQBRACKET -> "[";
			case SYM_RSQBRACKET -> "]";
			case SYM_LBRACKET -> "{";
			case SYM_RBRACKET -> "}";
			case SYM_COMMA -> ",";
			case SYM_COLON -> ":";
			case SYM_SEMICOLON -> ";";
			case SYM_AND -> "&";
			case SYM_OR -> "|";
			case SYM_NOT -> "!";
			case SYM_DOUBLE_EQUAL -> "==";
			case SYM_NOT_EQUAL -> "!=";
			case SYM_LOWER -> "<";
			case SYM_GREATER -> ">";
			case SYM_LOWEREQUAL-> "<=";
			case SYM_GREATEREQUAL -> ">=";
			case SYM_MUL -> "*";
			case SYM_DIV -> "/";
			case SYM_MOD -> "%";
			case SYM_SUM -> "+";
			case SYM_SUB -> "-";
			case SYM_POINT -> "^";
			case SYM_EQUAL -> "=";
			case KW_CHAR -> "char";
			case KW_DEL -> "del";
			case KW_DO -> "do";
			case KW_ELSE -> "else";
			case KW_END -> "end";
			case KW_FUN -> "fun";
			case KW_IF -> "if";
			case KW_INT -> "int";
			case KW_NEW -> "new";
			case KW_THEN -> "then";
			case KW_TYP -> "typ";
			case KW_VAR -> "var";
			case KW_VOID -> "void";
			case KW_WHERE -> "where";
			case KW_WHILE -> "while";
			case CONST_VOID -> "none";
			case CONST_NIL -> "nil";
			case CONST_CHAR -> "character constant";
			case ID -> "identifier";
			case CONST_INT -> "int constant";
			default -> null;
		};
	}

	public void close() {
		lexan.close();
	}

	/**
	 * Inits program parse
	 * @return the AST of the program
	 */
	public AST parser() {
		currSymbol = lexan.lexer();
		return parsePrg();
	}

	/**
	 * Parses the program
	 * @return the AST of the program
	 */
	private AST parsePrg(){
		Vector<AstDecl> decls = parseDecls();
		return new ASTs<>(decls.get(0).location, decls);

	}


	private Vector<AstDecl> parseDecls(){
		Vector<AstDecl> decls = new Vector<AstDecl>();
		decls.add(parseDecl());
		decls.addAll(parseDeclsRst());

		return decls;
	}


	private AstDecl parseDecl(){
		switch (currSymbol.token) {
			case KW_TYP -> {
				Symbol s1 = confirmAndGo(Token.KW_TYP);
				Symbol s2 = confirmAndGo(Token.ID);
				confirmAndGo(Token.SYM_EQUAL);

				AstType type = parseType();

				confirmAndGo(SYM_SEMICOLON);

				return new AstTypDecl(s1.location, s2.lexeme, type);
			}

			case KW_VAR -> {
				Symbol s1 = confirmAndGo(Token.KW_VAR);
				Symbol s2 = confirmAndGo(Token.ID);
				confirmAndGo(Token.SYM_COLON);

				AstType type = parseType();

				confirmAndGo(SYM_SEMICOLON);

				return new AstVarDecl(s1.location, s2.lexeme, type);
			}

			case KW_FUN -> {
				Symbol s1 = confirmAndGo(Token.KW_FUN);
				Symbol s2 = confirmAndGo(Token.ID);
				confirmAndGo(Token.SYM_LPARENT);

				ASTs<AstParDecl> params = parseFunParams();

				confirmAndGo(Token.SYM_RPARENT);
				confirmAndGo(Token.SYM_COLON);

				AstType type = parseType();
				confirmAndGo(Token.SYM_EQUAL);

				AstExpr expr = parseExpr();

				confirmAndGo(SYM_SEMICOLON);
				return new AstFunDecl(s1.location,s2.lexeme,params,type,expr);
			}
			default -> throw new Report.Error(currSymbol.location, "Error when parsing! (parsing decl)");
		}
	}


	private Vector<AstDecl> parseDeclsRst(){
		switch (currSymbol.token) {
			case EOF, SYM_RPARENT -> {
				return new Vector<AstDecl>();
			}
			case KW_TYP, KW_VAR, KW_FUN -> {
				return parseDecls();
			}
			default -> throw new Report.Error("Error when parsing DECLSRST");
		}
	}

	private ASTs<AstParDecl> parseFunParams(){
		switch (currSymbol.token) {
			case ID -> {
				Vector<AstParDecl> params = new Vector<>();
				Symbol s = confirmAndGo(Token.ID);
				confirmAndGo(Token.SYM_COLON);
				AstType type = parseType();
				params.add(new AstParDecl(s.location,s.lexeme,type));
				params.addAll(parseFunParamsRst());

				return new ASTs<>(s.location,params);
			}
			case SYM_RPARENT -> {
				return new ASTs<>(currSymbol.location, new Vector<>());
			}
			default -> throw new Report.Error("Error when parsing FUNPARAMS");
		}
	}

	private Vector<AstParDecl> parseFunParamsRst(){
		switch (currSymbol.token) {
			case SYM_COMMA -> {
				Vector<AstParDecl> params = new Vector<>();

				Symbol s1 = confirmAndGo(Token.SYM_COMMA);
				Symbol s2 = confirmAndGo(Token.ID);

				confirmAndGo(Token.SYM_COLON);
				AstType type = parseType();
				params.add(new AstParDecl(s1.location,s2.lexeme,type));
				params.addAll(parseFunParamsRst());

				return params;
			}
			case SYM_RPARENT -> {
				return new Vector<>();
			}
			default -> throw new Report.Error("Error when parsing FUNPARAMSRST");
		}

	}

	private AstType parseType(){
		switch (currSymbol.token) {
			case KW_VOID -> {
				Symbol s = confirmAndGo(Token.KW_VOID);
				return new AstAtomType(s.location, AstAtomType.Kind.VOID);

			}
			case KW_CHAR -> {
				Symbol s = confirmAndGo(Token.KW_CHAR);
				return new AstAtomType(s.location,AstAtomType.Kind.CHAR);
			}
			case KW_INT -> {
				Symbol s = confirmAndGo(Token.KW_INT);
				return new AstAtomType(s.location,AstAtomType.Kind.INT);
			}
			case ID -> {
				Symbol s = confirmAndGo(Token.ID);
				return new AstTypeName(s.location, s.lexeme);
			}
			case SYM_LSQBRACKET -> {
				Symbol s = confirmAndGo(Token.SYM_LSQBRACKET);
				AstExpr expr = parseExpr();
				confirmAndGo(Token.SYM_RSQBRACKET);
				AstType type = parseType();
				return new AstArrType(s.location,type,expr);
			}
			case SYM_POINT -> {
				Symbol s = confirmAndGo(Token.SYM_POINT);
				AstType type = parseType();
				return new AstPtrType(s.location,type);
			}
			case SYM_LPARENT -> {
				Symbol s = confirmAndGo(Token.SYM_LPARENT);
				AstType type = parseType();
				confirmAndGo(Token.SYM_RPARENT);
				return type;
			}
			default -> throw new Report.Error(currSymbol.location, "Error when parsing! (Type parsing)");
		}
	}

	private ASTs<AstStmt> parseStmts(){
		AstStmt stmt = parseStmt();

		Vector<AstStmt> stmts = parseStmtsRst(stmt);

		return new ASTs<>(stmt.location,stmts);
	}

	private Vector<AstStmt> parseStmtsRst(AstStmt left){
		switch (currSymbol.token) {
			case SYM_SEMICOLON -> {
				confirmAndGo(SYM_SEMICOLON);
				return parseStmtsf(left);
			}
			default -> throw new Report.Error(left.location, "Error while parsing");
		}
	}

	private Vector<AstStmt> parseStmtsf(AstStmt left){
		Vector<AstStmt> stmts = new Vector<>();
		stmts.add(left);
		switch (currSymbol.token) {
			case SYM_SEMICOLON, SYM_RBRACKET, KW_END, KW_ELSE -> {
				return stmts;
			}
			default -> {
				stmts.addAll(parseStmts().asts());
				return stmts;
			}

		}
	}

	private AstStmt parseStmt(){
		switch (currSymbol.token){
			case KW_IF ->{
				Symbol s = confirmAndGo(KW_IF);
				AstExpr expr = parseExpr();
				confirmAndGo(KW_THEN);
				ASTs<AstStmt> stmts = parseStmts();
				AstStmt ifstmt = parseStmtElse(s, expr, new AstExprStmt(stmts.asts().get(0).location, new AstStmtExpr(stmts.asts().get(0).location, stmts)));
				confirmAndGo(KW_END);

				return ifstmt;
			}

			case KW_WHILE -> {
				Symbol s = confirmAndGo(KW_WHILE);
				AstExpr expr = parseExpr();
				confirmAndGo(KW_DO);
				ASTs<AstStmt> stmtsAsts = parseStmts();
				AstExprStmt stmts = new AstExprStmt(stmtsAsts.asts().get(0).location, new AstStmtExpr(stmtsAsts.asts().get(0).location, stmtsAsts));
				confirmAndGo(KW_END);
				return new AstWhileStmt(s.location,expr, stmts);
			}
			default -> {
				AstExpr expr = parseExpr();
				return parseExprAsgn(expr);
			}
		}
	}

	private AstStmt parseStmtElse(Symbol s, AstExpr expr, AstStmt stmt){
		switch (currSymbol.token) {
			case KW_ELSE ->{
				confirmAndGo(KW_ELSE);
				ASTs<AstStmt> stmts = parseStmts();
				return new AstIfStmt(s.location, expr, stmt, new AstExprStmt(stmts.asts().get(0).location, new AstStmtExpr(stmts.asts().get(0).location, stmts)));
			}
			default -> {
				return new AstIfStmt(s.location, expr, stmt);
			}
		}
	}

	private AstStmt parseExprAsgn(AstExpr left){
		switch (currSymbol.token){
			case SYM_EQUAL -> {
				confirmAndGo(SYM_EQUAL);
				AstExpr expr = parseExpr();
				return new AstAssignStmt(left.location,left,expr);
			}
			default -> {
				return new AstExprStmt(left.location, left);
			}
		}
	}

	private AstExpr parseExpr(){
		switch (currSymbol.token) {
			case KW_NEW, KW_DEL -> {
				return parseExprAlloc();
			}
			case SYM_LBRACKET -> {
				Symbol s = confirmAndGo(SYM_LBRACKET);

				ASTs<AstStmt> stmts = parseStmts();

				confirmAndGo(SYM_RBRACKET);
				return new AstStmtExpr(s.location, stmts);
			}
			default -> {
				return parseExprDis();
			}
		}


	}

	private AstExpr parseExprAlloc(){
		switch (currSymbol.token) {
			case KW_NEW -> {
				Symbol s = confirmAndGo(Token.KW_NEW);
				AstExpr expr = parseExpr();
				return new AstPreExpr(s.location,AstPreExpr.Oper.NEW,expr);
			}
			case KW_DEL -> {
				Symbol s = confirmAndGo(Token.KW_DEL);
				AstExpr expr = parseExpr();
				return new AstPreExpr(s.location,AstPreExpr.Oper.DEL,expr);
			}
			default -> throw new Report.Error(currSymbol.location, "Syntax error when parsing! (alloc expr)");
		}
	}

	private AstExpr parseExprDis(){

		AstExpr exprcon= parseExprCon();
		return parseExprDisRst(exprcon);
	}

	private AstExpr parseExprDisRst(AstExpr left){
		if (currSymbol.token == Token.SYM_OR) {
			Symbol s = confirmAndGo(Token.SYM_OR);
			AstExpr con = parseExprCon();
			AstExpr disrst = new AstBinExpr(s.location, AstBinExpr.Oper.OR, left, con);
			return parseExprDisRst(disrst);
		} else {
			return left;
		}
	}

	private AstExpr parseExprCon(){
		AstExpr rel = parseExprRel();
		return parseExprConRst(rel);
	}

	private AstExpr parseExprConRst(AstExpr left){
		if (currSymbol.token == Token.SYM_AND) {
			Symbol s = confirmAndGo(Token.SYM_AND);
			AstExpr rel = parseExprRel();
			AstExpr conrst = new AstBinExpr(s.location, AstBinExpr.Oper.AND, left, rel);
			return parseExprConRst(rel);
		} else {
			return left;
		}
	}

	private AstExpr parseExprRel(){
		AstExpr add = parseExprAdd();
		return parseExprRelRst(add);
	}

	private AstExpr parseExprRelRst(AstExpr left){
		switch (currSymbol.token){
			case SYM_DOUBLE_EQUAL -> {
				Symbol s = confirmAndGo(Token.SYM_DOUBLE_EQUAL);
				AstExpr add = parseExprAdd();
				AstExpr rel = new AstBinExpr(s.location,AstBinExpr.Oper.EQU, left, add);
				return parseExprRelRst(rel);
			}
			case SYM_NOT_EQUAL -> {
				Symbol s = confirmAndGo(Token.SYM_NOT_EQUAL);
				AstExpr add = parseExprAdd();
				AstExpr rel = new AstBinExpr(s.location,AstBinExpr.Oper.NEQ, left, add);
				return parseExprRelRst(rel);
			}
			case SYM_GREATEREQUAL -> {
				Symbol s = confirmAndGo(Token.SYM_GREATEREQUAL);
				AstExpr add = parseExprAdd();
				AstExpr rel = new AstBinExpr(s.location,AstBinExpr.Oper.GEQ, left, add);
				return parseExprRelRst(rel);
			}
			case SYM_LOWEREQUAL -> {
				Symbol s = confirmAndGo(Token.SYM_LOWEREQUAL);
				AstExpr add = parseExprAdd();
				AstExpr rel = new AstBinExpr(s.location,AstBinExpr.Oper.LEQ, left, add);
				return parseExprRelRst(rel);
			}
			case SYM_LOWER -> {
				Symbol s = confirmAndGo(Token.SYM_LOWER);
				AstExpr add = parseExprAdd();
				AstExpr rel = new AstBinExpr(s.location,AstBinExpr.Oper.LTH, left, add);
				return parseExprRelRst(rel);
			}
			case SYM_GREATER -> {
				Symbol s = confirmAndGo(Token.SYM_GREATER);
				AstExpr add = parseExprAdd();
				AstExpr rel = new AstBinExpr(s.location,AstBinExpr.Oper.GTH, left, add);
				return parseExprRelRst(rel);
			}
			default -> {
				return left;
			}

		}
	}

	private AstExpr parseExprAdd(){
		AstExpr mul = parseExprMul();
		return parseExprAddRst(mul);
	}

	private AstExpr parseExprAddRst(AstExpr left){
		switch (currSymbol.token) {
			case SYM_SUM -> {
				Symbol s = confirmAndGo(Token.SYM_SUM);
				AstExpr mul = parseExprMul();
				AstExpr addrst = new AstBinExpr(s.location,AstBinExpr.Oper.ADD, left, mul);

				return parseExprAddRst(addrst);
			}
			case SYM_SUB -> {
				Symbol s = confirmAndGo(Token.SYM_SUB);
				AstExpr mul = parseExprMul();
				AstExpr addrst = new AstBinExpr(s.location,AstBinExpr.Oper.SUB, left, mul);

				return parseExprAddRst(addrst);
			}

			default -> {
				return left;
			}
		}
	}

	private AstExpr parseExprMul() {
		AstExpr pref = parseExprPref();
		return parseExprMulRst(pref);
	}

	private AstExpr parseExprMulRst(AstExpr left){
		switch (currSymbol.token) {
			case SYM_MUL -> {
				Symbol s = confirmAndGo(Token.SYM_MUL);
				AstExpr pref = parseExprPref();
				AstExpr mulrst = new AstBinExpr(s.location,AstBinExpr.Oper.MUL, left, pref);

				return parseExprMulRst(mulrst);
			}
			case SYM_DIV -> {
				Symbol s = confirmAndGo(Token.SYM_DIV);
				AstExpr pref = parseExprPref();
				AstExpr mulrst = new AstBinExpr(s.location,AstBinExpr.Oper.DIV, left, pref);

				return parseExprMulRst(mulrst);
			}
			case SYM_MOD -> {
				Symbol s = confirmAndGo(Token.SYM_MOD);

				AstExpr pref = parseExprPref();
				AstExpr mulrst = new AstBinExpr(s.location,AstBinExpr.Oper.MOD, left, pref);

				return parseExprMulRst(mulrst);
			}
			default -> {
				return left;
			}
		}
	}

	private AstExpr parseExprPref(){
		switch (currSymbol.token){
			case SYM_NOT -> {
				Symbol s = confirmAndGo(Token.SYM_NOT);
				AstExpr pref = parseExprPref();
				return new AstPreExpr(s.location,AstPreExpr.Oper.NOT, pref);
			}
			case SYM_SUM -> {
				Symbol s = confirmAndGo(Token.SYM_SUM);
				AstExpr pref = parseExprPref();
				return new AstPreExpr(s.location,AstPreExpr.Oper.ADD, pref);
			}
			case SYM_SUB -> {
				Symbol s = confirmAndGo(Token.SYM_SUB);
				AstExpr pref = parseExprPref();
				return new AstPreExpr(s.location,AstPreExpr.Oper.SUB, pref);
			}
			case SYM_POINT -> {
				Symbol s = confirmAndGo(Token.SYM_POINT);
				AstExpr pref = parseExprPref();
				return new AstPreExpr(s.location,AstPreExpr.Oper.PTR, pref);
			}
			default ->  {
				return parseExprPost();
			}
		}
	}

	private AstExpr parseExprPost(){
		AstExpr atom = parseExprAtom();
		return parseExprPostRst(atom);
	}

	private AstExpr parseExprPostRst(AstExpr left){
		switch (currSymbol.token) {
			case SYM_LSQBRACKET -> {
				Symbol s = confirmAndGo(Token.SYM_LSQBRACKET);
				AstExpr expr = parseExpr();
				confirmAndGo(Token.SYM_RSQBRACKET);
				AstExpr postrst = new AstBinExpr(s.location, AstBinExpr.Oper.ARR, left, expr);
				return parseExprPostRst(postrst);
			}
			case SYM_POINT -> {
				Symbol s = confirmAndGo(Token.SYM_POINT);
				AstExpr ptrrst = new AstPstExpr(s.location, AstPstExpr.Oper.PTR, left);
				return parseExprPostRst(ptrrst);
			}
			default -> {
				return left;
			}
		}
	}

	private AstExpr parseExprAtom() {
		switch (currSymbol.token) {
			case CONST_CHAR -> {
				Symbol s = confirmAndGo(Token.CONST_CHAR);
				return new AstConstExpr(s.location,AstConstExpr.Kind.CHAR, s.lexeme);
			}
			case CONST_INT -> {
				Symbol s = confirmAndGo(Token.CONST_INT);
				return new AstConstExpr(s.location,AstConstExpr.Kind.INT, s.lexeme);
			}
			case CONST_VOID -> {
				Symbol s = confirmAndGo(Token.CONST_VOID);
				return new AstConstExpr(s.location,AstConstExpr.Kind.VOID, s.lexeme);
			}
			case CONST_NIL -> {
				Symbol s = confirmAndGo(Token.CONST_NIL);
				return new AstConstExpr(s.location,AstConstExpr.Kind.PTR, s.lexeme);
			}
			case SYM_LPARENT -> {
				confirmAndGo(Token.SYM_LPARENT);

				AstExpr expr = parseExpr();
				AstExpr exprRst = parseExprRst(expr);

				confirmAndGo(Token.SYM_RPARENT);

				return exprRst;
			}
			case ID -> {
				Symbol s = confirmAndGo(Token.ID);
				AstNameExpr id = new AstNameExpr(s.location, s.lexeme);
				AstExpr IdentifierRst = parseIdentifierRst(id);

				return IdentifierRst;
			}
			default -> throw new Report.Error(currSymbol.location, "Error when parsing! (expr atom)");
		}
	}

	private AstExpr parseIdentifierRst(AstNameExpr left){

		if(currSymbol.token==Token.SYM_LPARENT){
			Symbol s = confirmAndGo(Token.SYM_LPARENT);

			Vector<AstExpr> params = new Vector<>();
			if(currSymbol.token!= SYM_RPARENT){
				params.addAll(parseIdentParams());
			}

			ASTs<AstExpr> right = new ASTs<>(s.location, params);

			confirmAndGo(Token.SYM_RPARENT);
			return new AstCallExpr(left.location, left.name, right);
		}
		else{
			return left;
		}
	}

	private Vector<AstExpr> parseIdentParams(){
		Vector<AstExpr> params = new Vector<>();
		params.add(parseExpr());
		params.addAll(parseIdentParamsRst());

		return params;
	}

	private Vector<AstExpr> parseIdentParamsRst(){
		if(currSymbol.token==Token.SYM_COMMA){
			confirmAndGo(Token.SYM_COMMA);
			Vector<AstExpr> params = new Vector<>();

			params.add(parseExpr());
			params.addAll(parseIdentParamsRst());
			return params;
		}
		return new Vector<AstExpr>();
	}

	private AstExpr parseExprRst(AstExpr left){
		switch (currSymbol.token){
			case SYM_COLON -> {
				return parseTypeCast(left);
			}
			case KW_WHERE -> {
				return parseExprWhere(left);

			}
			default -> {
				return left;
			}
		}
	}

	private AstWhereExpr parseExprWhere(AstExpr left){
		Symbol s = confirmAndGo(Token.KW_WHERE);
		Vector<AstDecl> params = parseDecls();
		ASTs<AstDecl> ret = new ASTs<>(s.location, params);

		return new AstWhereExpr(s.location, ret, left);
	}

	private AstCastExpr parseTypeCast(AstExpr left){
		Symbol s = confirmAndGo(Token.SYM_COLON);
		AstType type = parseType();
		return new AstCastExpr(s.location, left, type);
	}
}
