package pins.phase.seman;
import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.visitor.*;

enum NameResolverState {
    Body, Main
}
public class NameResolver extends AstFullVisitor<Object, NameResolverState>{
    private SymbTable symTable = new SymbTable();

    @Override
    public Object visit (ASTs<?> trees, NameResolverState state) {
        for (AST tree : trees.asts()){
            if(tree instanceof AstTypDecl){
                tree.accept(this, NameResolverState.Main);
            }
        }

        for (AST tree : trees.asts()) {
            if (tree instanceof AstTypDecl) {
                tree.accept(this, NameResolverState.Body);
            }
        }

        for (AST tree : trees.asts()) {
            if (tree instanceof AstVarDecl) {
                tree.accept(this, state);
            }
        }

        for (AST tree : trees.asts()){
            if(tree instanceof AstFunDecl) {
                tree.accept(this, NameResolverState.Main);
            }
        }

        for (AST tree : trees.asts()){
            if(tree instanceof AstFunDecl) {
                tree.accept(this, NameResolverState.Body);
            }
        }
        return null;
    }

    @Override
    public Object visit (AstVarDecl tree, NameResolverState state) {
        try {
            symTable.ins(tree.name, tree);
        }catch (SymbTable.CannotInsNameException e){
            throw new Report.Error(tree.location, "Cannot redefine '" + tree.name + "' as a variable.");
        }
        tree.type.accept(this, state);
        return null;
    }

    @Override
    public Object visit(AstTypDecl tree, NameResolverState state) {
        if(state == NameResolverState.Main) {
            try {
                symTable.ins(tree.name, tree);
            } catch (SymbTable.CannotInsNameException e) {
                throw new Report.Error(tree.location, "Cannot redefine '" + tree.name + "' as a type.");
            }
        }
        else{
            tree.type.accept(this, state);
        }
        return null;
    }

    @Override
    public Object visit (AstFunDecl tree, NameResolverState state) {
        if(state == NameResolverState.Main) {
            try {
                symTable.ins(tree.name, tree);
            } catch (SymbTable.CannotInsNameException e) {
                throw new Report.Error(tree.location, "Cannot redefine '" + tree.name + "' as a function.");
            }

            tree.type.accept(this, state);
            for(AstParDecl param : tree.pars.asts()) {
                param.accept(this, state);
            }
        }
        else {
            symTable.newScope();

            for(AstParDecl param : tree.pars.asts()) {
                param.accept(this, state);
            }
            tree.expr.accept(this, state);

            symTable.oldScope();
        }
        return null;
    }

    @Override
    public Object visit (AstParDecl tree, NameResolverState state){
        if(state == NameResolverState.Main) {
            tree.type.accept(this, state);
        }
        else{
            try {
                symTable.ins(tree.name, tree);
            }
            catch (SymbTable.CannotInsNameException e) {
                throw new Report.Error(tree.location, "Cannot redefine '" + tree.name + "' as a parameter.");
            }
        }

        return null;
    }

    @Override
    public Object visit (AstStmtExpr tree, NameResolverState state) {
        for(AstStmt stmt : tree.stmts.asts()) {
            stmt.accept(this, state);
        }
        return null;
    }

    @Override
    public Object visit (AstWhereExpr tree, NameResolverState state) {
        symTable.newScope();
        tree.decls.accept(this, state);
        tree.subExpr.accept(this, state);

        symTable.oldScope();
        return null;
    }


    @Override
    public Object visit(AstTypeName tree, NameResolverState state){
        try{
            SemAn.declaredAt.put(tree, symTable.fnd(tree.name));
        }
        catch(SymbTable.CannotFndNameException e){
            throw new Report.Error(tree.location, "Type '" + tree.name + "' is not defined.");
        }
        return null;
    }

    @Override
    public Object visit(AstNameExpr tree, NameResolverState state){
        try{
            SemAn.declaredAt.put(tree, symTable.fnd(tree.name));
        }
        catch(SymbTable.CannotFndNameException e){
            throw new Report.Error(tree.location, "Variable '" + tree.name + "' is not defined.");
        }
        return null;
    }

    @Override
    public Object visit(AstCallExpr tree, NameResolverState state) {
        try{
            SemAn.declaredAt.put(tree, symTable.fnd(tree.name));
        }
        catch(SymbTable.CannotFndNameException e){
            throw new Report.Error(tree.location, "Function '" + tree.name + "' is not defined.");
        }

        for(AstExpr expr : tree.args.asts()) {
            expr.accept(this, state);
        }
        return null;
    }


}