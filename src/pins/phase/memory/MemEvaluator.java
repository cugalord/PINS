package pins.phase.memory;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.mem.*;
import pins.data.typ.*;
import pins.phase.seman.*;

/**
 * Computing memory layout: frames and accesses.
 */
public class MemEvaluator extends AstFullVisitor<Object, MemEvaluator.FunContext> {

	/**
	 * Functional context, i.e., used when traversing function and building a new
	 * frame, parameter acceses and variable acceses.
	 */
	protected class FunContext {
		public int depth = 0;
		public long locsSize = 0;
		public long argsSize = 0;
		public long parsSize = new SemPtr(new SemVoid()).size();
	}

	/**
	 * Set starting depth and context.
	 * Evalate program
	 * @param trees
	 * @param fctx
	 * @return trees
	 */
	public Object visit(ASTs<?> trees, FunContext fctx) {

		if(fctx == null) {
			fctx = new FunContext();
			fctx.depth = 1;
		}

		for(AST tree : trees.asts()) {
			tree.accept(this, fctx);

			if(tree instanceof AstFunDecl) {
				fctx.parsSize = new SemPtr(new SemVoid()).size();
			}
		}

		return trees;

	}

	/**
	 * Evalate variable
	 * Check if variable is local or global
	 * @param tree
	 * @param fctx
	 * @return tree
	 */
	@Override
	public Object visit(AstVarDecl tree, FunContext fctx){

		long size = SemAn.describesType.get(tree.type).size();

		if(fctx.depth < 2){
			MemLabel label = new MemLabel(tree.name);
			MemAbsAccess access = new MemAbsAccess(size, label);

			Memory.varAccesses.put(tree, access);
		}
		else{
			int offset = (int) (-fctx.locsSize - size);
			fctx.locsSize += size;
			MemRelAccess access = new MemRelAccess(size, offset, fctx.depth);
			Memory.varAccesses.put(tree, access);
		}

		return tree;
	}

	/**
	 * Evaluate function
	 * @param tree
	 * @param fctx
	 * @return tree
	 */
	@Override
	public Object visit(AstFunDecl tree, FunContext fctx){
		fctx.depth++;

		// accept function params
		for(AstParDecl par : tree.pars.asts()) {
			par.accept(this, fctx);
		}

		// accept a new function body
		tree.expr.accept(this, fctx);

		fctx.depth--;

		// create new label and check if function is in local (wrapped) or global scope
		MemLabel label = new MemLabel();
		if(fctx.depth < 2){
			label = new MemLabel(tree.name);
		}

		// create new frame and add it to the list of frames
		MemFrame frame = new MemFrame(label, fctx.depth, fctx.locsSize, fctx.argsSize);
		Memory.frames.put(tree, frame);

		// reset context
		fctx.argsSize=0;
		fctx.locsSize=0;

		return tree;

	}

	/**
	 * Evaluate function parameter
	 * @param tree
	 * @param fctx
	 * @return tree
	 */
	@Override
	public Object visit(AstParDecl tree, FunContext fctx){
		long size = SemAn.describesType.get(tree.type).size();
		MemRelAccess access = new MemRelAccess(size, fctx.parsSize, fctx.depth);
		Memory.parAccesses.put(tree, access);
		fctx.parsSize += size;

		return tree;
	}

	/**
	 * Evaluate function call
	 * @param tree
	 * @param fctx
	 * @return tree
	 */
	@Override
	public Object visit(AstCallExpr tree, FunContext fctx){
		long maxargsSize = 0;

		for(AstExpr arg : tree.args.asts()) {
			maxargsSize += SemAn.exprOfType.get(arg).size();
			arg.accept(this, fctx);
		}

		fctx.argsSize = Math.max(fctx.argsSize, maxargsSize + new SemPtr(new SemVoid()).size());
		return tree;
	}

	/**
	 * Evaluate stmt expression
	 * @param tree
	 * @param fctx
	 * @return tree
	 */
	@Override
	public Object visit(AstStmtExpr tree, FunContext fctx){
		for(AstStmt stmt : tree.stmts.asts()) {
			stmt.accept(this, fctx);
		}

		return tree;
	}

	/**
	 * Evaluate wthere expression
	 * @param tree
	 * @param fctx
	 * @return tree
	 */
	@Override
	public Object visit(AstWhereExpr tree, FunContext fctx){
			for(AstDecl decl : tree.decls.asts()) {
				if(decl instanceof AstFunDecl) {
					FunContext nextFctx = new FunContext();
					nextFctx.depth = fctx.depth;
					decl.accept(this, nextFctx);
				}
				else {
					decl.accept(this, fctx);
				}
			}
			tree.subExpr.accept(this, fctx);

			return tree;
	}

	/**
	 * Evaluate tree
	 * @param tree
	 * @param fctx
	 * @return tree
	 */
	@Override
	public Object visit(AstExprStmt tree, FunContext fctx){
		tree.expr.accept(this, fctx);
		return tree;
	}



}
