package pins.phase.imcgen;

import java.util.*;
import java.util.concurrent.Semaphore;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.*;
import pins.data.imc.code.stmt.*;
import pins.data.mem.*;
import pins.data.typ.*;
import pins.phase.memory.*;
import pins.phase.seman.*;

public class ExprGenerator implements AstVisitor<ImcExpr, Stack<MemFrame>> {

	@Override
	public ImcExpr visit(AstWhereExpr whereExpr, Stack<MemFrame> frames) {
		whereExpr.decls.accept(new CodeGenerator(), frames);
		ImcExpr code = whereExpr.subExpr.accept(this, frames);
		ImcGen.exprImc.put(whereExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstConstExpr constExpr, Stack<MemFrame> frames) {
		switch (constExpr.kind) {
			case INT -> {
				ImcCONST c_int = new ImcCONST(Integer.parseInt(constExpr.name));
				ImcGen.exprImc.put(constExpr, c_int);
				return c_int;
			}
			case CHAR -> {
				ImcCONST c_char = new ImcCONST(constExpr.name.charAt(2));
				if(constExpr.name.length() == 3){
					c_char = new ImcCONST(constExpr.name.charAt(1));
				}
				ImcGen.exprImc.put(constExpr, c_char);
				return c_char;
			}
			case VOID -> {
				ImcCONST c_void = new ImcCONST(0);
				ImcGen.exprImc.put(constExpr, c_void);
				return c_void;
			}
			case PTR -> {
				ImcCONST c_ptr = new ImcCONST(0);
				ImcGen.exprImc.put(constExpr, c_ptr);
				return c_ptr;
			}
		}
		return null;
	}
	@Override
	public ImcExpr visit(AstBinExpr binExpr, Stack<MemFrame> frames) {
		ImcExpr left = binExpr.fstSubExpr.accept(this, frames);
		ImcExpr right = binExpr.sndSubExpr.accept(this, frames);
		switch (binExpr.oper) {
			case ADD -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.ADD, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case SUB -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.SUB, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case MUL -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.MUL, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case DIV -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.DIV, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case MOD -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.MOD, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case AND -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.AND, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case OR -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.OR, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case EQU -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.EQU, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case NEQ -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.NEQ, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case LTH -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.LTH, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case GTH -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.GTH, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case LEQ -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.LEQ, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case GEQ -> {
				ImcBINOP c_oper = new ImcBINOP(ImcBINOP.Oper.GEQ, left, right);
				ImcGen.exprImc.put(binExpr, c_oper);
				return c_oper;
			}
			case ARR -> {
				long elem_size = SemAn.exprOfType.get(binExpr).size();

				ImcBINOP mul = new ImcBINOP(ImcBINOP.Oper.MUL, right, new ImcCONST(elem_size));
				ImcBINOP add = new ImcBINOP(ImcBINOP.Oper.ADD, ((ImcMEM) left).addr, mul);

				ImcMEM real_addr = new ImcMEM(add);
				ImcGen.exprImc.put(binExpr,real_addr);

				return real_addr;
			}

		}
		return null;
	}

	@Override
	public ImcExpr visit(AstPreExpr preExpr, Stack<MemFrame> frames){
		ImcExpr sub = preExpr.subExpr.accept(this, frames);
		switch (preExpr.oper){
			case NEW -> {
				Vector<ImcExpr> args = new Vector<>();
				Vector<Long> ofsts = new Vector<>();

				args.add(new ImcTEMP(frames.peek().FP));
				ofsts.add(0L);

				args.add(sub);
				ofsts.add(8L);

				ImcCALL call = new ImcCALL(new MemLabel("new"), ofsts, args);
				ImcGen.exprImc.put(preExpr, call);
				return call;
			}
			case DEL -> {
				Vector<ImcExpr> args = new Vector<>();
				Vector<Long> ofsts = new Vector<>();

				args.add(new ImcTEMP(frames.peek().FP));
				ofsts.add(0L);

				args.add(sub);
				ofsts.add(8L);

				ImcCALL call = new ImcCALL(new MemLabel("del"), ofsts, args);
				ImcGen.exprImc.put(preExpr, call);
				return call;
			}
			case NOT -> {
				ImcUNOP op = new ImcUNOP(ImcUNOP.Oper.NOT, sub);
				ImcGen.exprImc.put(preExpr,op);
				return op;
			}
			case ADD -> {
				ImcGen.exprImc.put(preExpr,sub);
				return sub;
			}
			case SUB -> {
				ImcUNOP op = new ImcUNOP(ImcUNOP.Oper.NEG, sub);
				ImcGen.exprImc.put(preExpr,op);
				return op;

			}
			case PTR -> {
				ImcMEM adr = new ImcMEM(sub);
				if(sub instanceof ImcMEM){
					ImcGen.exprImc.put(preExpr,((ImcMEM) sub).addr);
					return ((ImcMEM) sub).addr;
				}

				ImcGen.exprImc.put(preExpr,adr);
				return adr;
			}
		}
		return sub;
	}

	@Override
	public ImcExpr visit(AstPstExpr pstExpr, Stack<MemFrame> frames){
		ImcExpr sub = pstExpr.subExpr.accept(this, frames);
		ImcMEM mem = new ImcMEM(sub);
		ImcGen.exprImc.put(pstExpr, mem);
		return mem;
	}

	@Override
	public ImcExpr visit(AstCastExpr castExpr, Stack<MemFrame> frames){
		ImcExpr expr = castExpr.subExpr.accept(this, frames);
		ImcGen.exprImc.put(castExpr, expr);
		return expr;
	}

	@Override
	public ImcExpr visit(AstNameExpr nameExpr, Stack<MemFrame> frames){
		ImcCONST offset = null;
		long diff = 0;
		if(SemAn.declaredAt.get(nameExpr) instanceof AstVarDecl){
			if(Memory.varAccesses.get((AstVarDecl) SemAn.declaredAt.get(nameExpr)) instanceof MemRelAccess){
				MemRelAccess access = (MemRelAccess) Memory.varAccesses.get((AstVarDecl) SemAn.declaredAt.get(nameExpr));
				offset = new ImcCONST(access.offset);
				diff=Math.abs(access.depth-frames.peek().depth-1);
			}
			else {
				MemAbsAccess abs = (MemAbsAccess) Memory.varAccesses.get((AstVarDecl) SemAn.declaredAt.get(nameExpr));
				ImcMEM mem = new ImcMEM(new ImcNAME(abs.label));
				ImcGen.exprImc.put(nameExpr, mem);

				return mem;
			}
		}
		else if(SemAn.declaredAt.get(nameExpr) instanceof AstParDecl){
			MemRelAccess access = Memory.parAccesses.get((AstParDecl) SemAn.declaredAt.get(nameExpr));
			offset = new ImcCONST(access.offset);
			diff=Math.abs(access.depth-frames.peek().depth-1);
		}

		ImcTEMP temp = new ImcTEMP(frames.peek().FP);
		ImcMEM mem = null;
		if(diff > 0){
			mem= new ImcMEM(temp);
			for(int i=1; i<diff; i++){
				mem = new ImcMEM(mem);
			}

			ImcMEM finalmem = new ImcMEM(new ImcBINOP(ImcBINOP.Oper.ADD, mem, offset));
			ImcGen.exprImc.put(nameExpr, finalmem);

			return finalmem;
		}

		ImcMEM finalmem = new ImcMEM(new ImcBINOP(ImcBINOP.Oper.ADD, temp, offset));
		ImcGen.exprImc.put(nameExpr, finalmem);

		return finalmem;

	}

	@Override
	public ImcExpr visit(AstCallExpr callExpr, Stack<MemFrame> frames){
		Vector<ImcExpr> args = new Vector<>();
		Vector<Long> ofsts = new Vector<>();

		MemFrame funcframe = Memory.frames.get(((AstFunDecl) SemAn.declaredAt.get(callExpr)));
		ImcTEMP temp = new ImcTEMP(frames.peek().FP);

		long diff = Math.abs(funcframe.depth-frames.peek().depth-1);

		ImcMEM mem = null;
		if(diff > 0){
			mem = new ImcMEM(temp);
			for(int i=1; i<diff; i++){
				mem = new ImcMEM(mem);
			}
			args.add(mem);
			ofsts.add(0L);
		}
		else {
			args.add(temp);
			ofsts.add(0L);
		}

		long offset = 8L;
		for(AstExpr arg : callExpr.args.asts()){
			args.add(arg.accept(this, frames));
			ofsts.add(offset);
			offset += SemAn.exprOfType.get(arg).size();
		}

		ImcCALL call = new ImcCALL(funcframe.label, ofsts, args);
		ImcGen.exprImc.put(callExpr, call);
		return call;
	}

	@Override
	public ImcExpr visit(AstStmtExpr stmtExpr, Stack<MemFrame> frames){
		ImcSTMTS stmts = new ImcSTMTS(new Vector<>());
		for(AstStmt stmt : stmtExpr.stmts.asts()){
			stmts.stmts.add(stmt.accept(new StmtGenerator(), frames));
		}

		if(stmts.stmts.get(stmts.stmts.size()-1) instanceof ImcESTMT){
			ImcStmt temp = stmts.stmts.get(stmts.stmts.size()-1);
			stmts.stmts.remove(stmts.stmts.size()-1);
			ImcSEXPR sex = new ImcSEXPR(stmts, ((ImcESTMT) temp).expr);
			ImcGen.exprImc.put(stmtExpr, sex);
			return sex;
		}
		else {
			ImcSEXPR sex = new ImcSEXPR(stmts, new ImcCONST(0));
			ImcGen.exprImc.put(stmtExpr,sex);
			return sex;
		}

	}

}
