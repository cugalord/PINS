package pins.phase.imcgen;

import java.util.*;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.ImcCALL;
import pins.data.imc.code.expr.ImcExpr;
import pins.data.imc.code.stmt.*;
import pins.data.mem.*;
import pins.phase.seman.SemAn;

public class StmtGenerator implements AstVisitor<ImcStmt, Stack<MemFrame>> {

	/**
	 * Generate ImcMOVE for assignment statement.
	 * Append to stmtImc map.
	 * @param assignStmt
	 * @param frames
	 * @return ImcMOVE
	 */
	@Override
	public ImcStmt visit(AstAssignStmt assignStmt, Stack<MemFrame> frames) {
		ImcStmt code = new ImcMOVE(assignStmt.fstSubExpr.accept(new ExprGenerator(), frames),
				assignStmt.sndSubExpr.accept(new ExprGenerator(), frames));
		ImcGen.stmtImc.put(assignStmt, code);
		return code;
	}

	/**
	 * Generate ImcESTMT for expression statement.
	 * Append to stmtImc map.
	 * @param ExprStmt
	 * @param frames
	 * @return ImcESTMT
	 */
	@Override
	public ImcStmt visit(AstExprStmt ExprStmt, Stack<MemFrame> frames) {
		ImcExpr expr = ExprStmt.expr.accept(new ExprGenerator(), frames);
		ImcESTMT code = new ImcESTMT(expr);
		ImcGen.stmtImc.put(ExprStmt, code);
		return code;
	}

	/**
	 * Generate ImcStmts with multiple ImcInstructions for if statement.
	 * Handle if-else and if-else-if conditions.
	 * Generate jumps for conditionals.
	 * Append to stmtImc map.
	 * @param ifStmt
	 * @param frames
	 * @return ImcSTMTS
	 */
	@Override
	public ImcStmt visit(AstIfStmt ifStmt, Stack<MemFrame> frames) {
		ImcSTMTS stmts = new ImcSTMTS(new Vector<>());
		ImcExpr expr = ifStmt.condExpr.accept(new ExprGenerator(), frames);
		if(ifStmt.elseBodyStmt != null) {
			MemLabel l1 = new MemLabel();
			MemLabel l2 = new MemLabel();
			MemLabel l3 = new MemLabel();

			ImcCJUMP cjump = new ImcCJUMP(expr, l1, l2);
			ImcStmt elseStmt = ifStmt.elseBodyStmt.accept(this, frames);
			ImcStmt thenStmt = ifStmt.thenBodyStmt.accept(this, frames);
			stmts.stmts.add(cjump);
			stmts.stmts.add(new ImcLABEL(l1));
			stmts.stmts.add(thenStmt);
			stmts.stmts.add(new ImcJUMP(l3));
			stmts.stmts.add(new ImcLABEL(l2));
			stmts.stmts.add(elseStmt);
			stmts.stmts.add(new ImcLABEL(l3));
		}
		else {
			MemLabel l1 = new MemLabel();
			MemLabel l2 = new MemLabel();
			ImcCJUMP cjump = new ImcCJUMP(expr, l1, l2);
			ImcStmt thenStmt = ifStmt.thenBodyStmt.accept(this, frames);
			stmts.stmts.add(cjump);
			stmts.stmts.add(new ImcLABEL(l1));
			stmts.stmts.add(thenStmt);
			stmts.stmts.add(new ImcLABEL(l2));
		}

		ImcGen.stmtImc.put(ifStmt, stmts);
		return stmts;
	}

	/**
	 * Generate Generate ImcStmts with multiple ImcInstructions for while statement.
	 * Handle while-do and create labels for jumps.
	 * Generate jumps for while loop / condition.
	 * Append to stmtImc map.
	 * @param whileStmt
	 * @param frames
	 * @return ImcCALL
	 */
	@Override
	public ImcStmt visit(AstWhileStmt whileStmt, Stack<MemFrame> frames) {
		ImcSTMTS stmts = new ImcSTMTS(new Vector<>());
		ImcExpr expr = whileStmt.condExpr.accept(new ExprGenerator(), frames);
		MemLabel l1 = new MemLabel();
		MemLabel l2 = new MemLabel();
		MemLabel l3 = new MemLabel();

		ImcCJUMP cjump = new ImcCJUMP(expr, l2, l3);
		ImcStmt bodyStmt = whileStmt.bodyStmt.accept(this, frames);
		ImcJUMP jump = new ImcJUMP(l1);
		stmts.stmts.add(new ImcLABEL(l1));
		stmts.stmts.add(cjump);
		stmts.stmts.add(new ImcLABEL(l2));
		stmts.stmts.add(bodyStmt);
		stmts.stmts.add(jump);
		stmts.stmts.add(new ImcLABEL(l3));

		ImcGen.stmtImc.put(whileStmt, stmts);
		return stmts;
	}





}
