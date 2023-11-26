package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.mem.*;
import pins.phase.memory.*;

public class CodeGenerator extends AstFullVisitor<Object, Stack<MemFrame>> {

    /**
     * Visit method which traverses the trees
     * @param trees
     * @param stack
     * @return
     */
    @Override
	public Object visit(ASTs<?> trees, Stack<MemFrame> stack) {
        if(stack == null) {
            stack = new Stack<MemFrame>();
        }
        for (AST tree : trees.asts()) {
            tree.accept(this, stack);
        }
        return trees;
    }

    /**
     * Visit method for functions -> prepares entry point for Expression generator
     * @param tree
     * @param stack
     * @return
     */
    @Override
    public Object visit(AstFunDecl tree, Stack<MemFrame> stack) {
        MemFrame frame = Memory.frames.get(tree);
        stack.push(frame);
        tree.expr.accept(new ExprGenerator(), stack);
        stack.pop();
        return tree;
    }
	
}
