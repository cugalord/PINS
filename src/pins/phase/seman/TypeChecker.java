package pins.phase.seman;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.visitor.AstFullVisitor;
import pins.data.typ.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

enum TypeCheckerState {
    Body, Main
}

public class TypeChecker extends AstFullVisitor<SemType, TypeCheckerState> {

    HashSet<String> used = new HashSet<String>();

    public SemType visit(ASTs<?> trees, TypeCheckerState state) {
        for (AST tree : trees.asts()) {
            if (tree instanceof AstTypDecl) {
                tree.accept(this, TypeCheckerState.Main);
            }
        }

        for (AST tree : trees.asts()) {
            if (tree instanceof AstTypDecl) {
                tree.accept(this, TypeCheckerState.Body);
            }
        }

        for (AST tree : trees.asts()) {
            if (tree instanceof AstVarDecl) {
                tree.accept(this, state);
            }
        }

        for (AST tree : trees.asts()) {
            if (tree instanceof AstFunDecl) {
                tree.accept(this, TypeCheckerState.Main);
            }
        }

        for (AST tree : trees.asts()) {
            if (tree instanceof AstFunDecl) {
                tree.accept(this, TypeCheckerState.Body);
            }
        }
        return null;
    }

    @Override
    public SemType visit(AstTypDecl tree, TypeCheckerState state) {
        if (state == TypeCheckerState.Body) {
            if(tree.type instanceof AstTypeName) {
                SemType t = tree.type.accept(this, state);
                SemAn.declaresType.get(tree).define(t.actualType());
            }

        } else if (state == TypeCheckerState.Main) {
            SemAn.declaresType.put(tree, new SemName(tree.name));

            if(!(tree.type instanceof AstTypeName)) {
                SemType t = tree.type.accept(this, state);
                SemAn.declaresType.get(tree).define(t.actualType());
            }

        }
        return null;
    }

    @Override
    public SemType visit(AstVarDecl tree, TypeCheckerState state) {
        return tree.type.accept(this, state);
    }

    @Override
    public SemType visit(AstFunDecl tree, TypeCheckerState state) {
        if (state == TypeCheckerState.Body) {
            SemType aret = tree.expr.accept(this, state);
            SemType eret = tree.type.accept(this, state);

            if(!match(aret, eret, null)) {
                throw new Report.Error(tree.location, "Function return type mismatch");
            }


        } else if (state == TypeCheckerState.Main) {
            for (AstParDecl par : tree.pars.asts()) {
                par.accept(this, state);
            }
            SemType ret = tree.type.accept(this, state);
            if(ret.actualType() instanceof SemArr) {
                throw new Report.Error(tree.location,"Function return type cannot be an array");
            }
            return ret;
        }
        return null;
    }


    @Override
    public SemType visit(AstParDecl tree, TypeCheckerState state) {
        SemType par = tree.type.accept(this, state);

        if(par.actualType() instanceof SemArr || par.actualType() instanceof SemVoid) {
            throw new Report.Error(tree.location, "Parameter type cannot be an array or void");
        }
        return par;
    }

    @Override
    public SemType visit(AstAtomType tree, TypeCheckerState state) {
       switch (tree.kind) {
           case INT:
               SemAn.describesType.put(tree, new SemInt());
               return new SemInt();
           case CHAR:
               SemAn.describesType.put(tree, new SemChar());
               return new SemChar();
           case VOID:
               SemAn.describesType.put(tree, new SemVoid());
               return new SemVoid();
           default:
               return null;
       }
   }

    @Override
    public SemType visit(AstPtrType tree, TypeCheckerState state) {
        SemType t = tree.subType.accept(this, state);
        SemPtr ptr = new SemPtr(t.actualType());
        SemAn.describesType.put(tree, ptr);

        return ptr;
    }

    @Override
    public SemType visit(AstArrType tree, TypeCheckerState state) {
        SemType elem = tree.elemType.accept(this, state);
        SemType sizet = tree.size.accept(this, state);

        if(!(sizet instanceof SemInt)) {
            throw new Report.Error(tree.location, "Array size must be an integer");
        }

        long size = -1;
        try{
            if(tree.size instanceof AstPreExpr) {
                if(((AstPreExpr) tree.size).oper == AstPreExpr.Oper.ADD) {
                    size = Long.parseLong(((AstConstExpr)((AstPreExpr) tree.size).subExpr).name);
                }
                else {
                    throw new Report.Error(tree.location, "Array size must be positive");
                }
            }
            else {
                size = Long.parseLong(((AstConstExpr) tree.size).name);
            }
        }catch (ClassCastException e) {
            throw new Report.Error(tree.location, "Array size must be an integer");
        }

        SemArr arr = new SemArr(elem.actualType(), size);
        SemAn.describesType.put(tree, arr);

       return arr;

    }

    @Override
    public SemType visit(AstTypeName tree, TypeCheckerState state) {
        AstTypDecl typ = (AstTypDecl) SemAn.declaredAt.get(tree);
        SemType type = SemAn.declaresType.get(typ);

        if(used.contains(tree.name)){
            throw new Report.Error(tree.location,"Type " + tree.name + " is already used");
        }
        used.add(tree.name);

        if(typ.type instanceof AstTypeName){
            type = typ.type.accept(this, state);
        }

        SemAn.describesType.put(tree, type.actualType());
        used.clear();

        return type.actualType();
    }


    @Override
    public SemType visit(AstConstExpr tree, TypeCheckerState state) {
       switch (tree.kind) {
           case INT:
               SemAn.exprOfType.put(tree, new SemInt());
               return new SemInt();
           case CHAR:
               SemAn.exprOfType.put(tree, new SemChar());
               return new SemChar();
           case VOID:
               SemAn.exprOfType.put(tree, new SemVoid());
               return new SemVoid();
           case PTR:
               SemAn.exprOfType.put(tree, new SemPtr(new SemVoid()));
               return new SemPtr(new SemVoid());
       }

       return null;
    }

    @Override
    public SemType visit(AstPreExpr tree, TypeCheckerState state) {
        switch (tree.oper) {
            case ADD,SUB,NOT:
                SemType t = tree.subExpr.accept(this, state);
                if(!(t.actualType() instanceof SemInt)) {
                    throw new Report.Error(tree.location, "Operator " + tree.oper + " can only be applied to integer");
                }
                SemAn.exprOfType.put(tree, t.actualType());
                return t;

            case PTR:
                SemType type = tree.subExpr.accept(this, state);
                SemPtr ptr = new SemPtr(type.actualType());

                SemAn.exprOfType.put(tree, ptr);
                return ptr;

            case NEW:
                SemType type2 = tree.subExpr.accept(this, state);
                if(!(type2.actualType() instanceof SemInt)) {
                    throw new Report.Error(tree.location, "Operator " + tree.oper + " can only be applied to integer");
                }
                SemPtr ptr1 = new SemPtr(new SemVoid());
                SemAn.exprOfType.put(tree, ptr1);
                return ptr1;

            case DEL:
                SemType type3 = tree.subExpr.accept(this, state);
                if(!(type3.actualType() instanceof SemPtr)) {
                    throw new Report.Error(tree.location, "Operator " + tree.oper + " can only be applied to pointer");
                }
                SemAn.exprOfType.put(tree, new SemVoid());
                return new SemVoid();
        }
        return null;
    }

    @Override
    public SemType visit(AstPstExpr tree, TypeCheckerState state) {
        SemType t = tree.subExpr.accept(this, state);

        if(!(t.actualType() instanceof SemPtr)) {
            throw new Report.Error(tree.location, "Operator " + tree.oper + " can only be applied to pointer");
        }

        SemType base = ((SemPtr) t).baseType;
        SemAn.exprOfType.put(tree, base);
        return base;
    }


    @Override
    public SemType visit(AstBinExpr tree, TypeCheckerState state) {
       switch (tree.oper) {
           case ADD,SUB,MUL,DIV,MOD,AND,OR:
               SemType t1 = tree.fstSubExpr.accept(this, state);
               SemType t2 = tree.sndSubExpr.accept(this, state);

               if(!(t1.actualType() instanceof SemInt) || !(t2.actualType() instanceof SemInt)) {
                   throw new Report.Error(tree.location, "Operator " + tree.oper + " can only be applied to integer");
               }

               SemAn.exprOfType.put(tree, new SemInt());
               return new SemInt();

           case EQU, NEQ, LTH, GTH, LEQ, GEQ:
               SemType t3 = tree.fstSubExpr.accept(this, state);
               SemType t4 = tree.sndSubExpr.accept(this, state);

               if((t3.actualType() instanceof SemVoid) || (t4.actualType() instanceof SemVoid) || t3.actualType() instanceof SemArr || t4.actualType() instanceof SemArr) {
                   throw new Report.Error(tree.location, "Operator " + tree.oper + " cannot be applied to void or array");
               }

               if(!match(t3, t4, null)) {
                   throw new Report.Error(tree.location, "Operator " + tree.oper + " can only be applied to same type");
               }

               SemAn.exprOfType.put(tree, new SemInt());
               return new SemInt();

           case ARR:
               SemType t5 = tree.fstSubExpr.accept(this, state);
               SemType t6 = tree.sndSubExpr.accept(this, state);

               if(!(t5.actualType() instanceof SemArr)){
                   throw new Report.Error(tree.location, "Operator " + tree.oper + " can only be applied to array");
               }

               if(t6.actualType() instanceof SemVoid) {
                   throw new Report.Error(tree.location, "Operator " + tree.oper + " can only be applied to array");
               }
               SemAn.exprOfType.put(tree, ((SemArr) t5).elemType.actualType());
               return ((SemArr) t5).elemType.actualType();
       }
       return null;
    }

    /**
     * Checks if name is a valid identifier
     * @param tree
     * @param state
     * @return
     */
    @Override
    public SemType visit(AstNameExpr tree, TypeCheckerState state) {
       AstType aType = SemAn.declaredAt.get(tree).type;
       SemType t = aType.accept(this, state);

       SemAn.exprOfType.put(tree, t.actualType());
       return t.actualType();
    }

    /**
     * Checks if the function is declared checks if the arguments are correct, and if the return type is correct
     * @param tree
     * @param state
     * @return the return type of the function
     */
    @Override
    public SemType visit(AstCallExpr tree, TypeCheckerState state) {
        //check if the function is declared
        AstFunDecl fDecl = (AstFunDecl) SemAn.declaredAt.get(tree);

        //check the number of arguments
        if(tree.args.asts().size() != fDecl.pars.asts().size()) {
            throw new Report.Error(tree.location, "Function " + fDecl.name + " expects " + fDecl.pars.asts().size() + " arguments, but " + tree.args.asts().size() + " are given");
        }
        //check the type of arguments
        for (int i=0; i < tree.args.asts().size(); i++) {
            SemType argType = tree.args.asts().get(i).accept(this, state);
            SemType paramType = fDecl.pars.asts().get(i).accept(this, state);
            //check the type of arguments
            if(!match(argType, paramType, null)) {
                throw new Report.Error(tree.location, "Argument " + i + " of function call does not match parameter type");
            }
        }
        //check the return type
        SemType type = fDecl.type.accept(this, state);
        SemAn.exprOfType.put(tree, type.actualType());

        return type.actualType();
    }

    /**
     * Check if two cast types are compatible
     * @param tree
     * @param state
     * @return actual type of the cast expression
     */
    @Override
    public SemType visit(AstCastExpr tree, TypeCheckerState state) {
        SemType t1 = tree.subExpr.accept(this, state);
        SemType t2 = tree.type.accept(this, state);

        if(t1.actualType() instanceof SemVoid || t1.actualType() instanceof SemArr) {
            throw new Report.Error(tree.location, "Cannot cast to void or array");
        }
        if(t2.actualType() instanceof SemVoid || t2.actualType() instanceof SemArr) {
            throw new Report.Error(tree.location, "Cannot cast to void or array");
        }

        SemAn.exprOfType.put(tree, t2.actualType());
        return t2.actualType();
    }

    /**
     * Checker for AstWhereExpr node.
     * @param tree
     * @param state
     * @return actual type of the expression
     */
    @Override
    public SemType visit(AstWhereExpr tree, TypeCheckerState state) {
        for(AstDecl d : tree.decls.asts()) {
            d.accept(this, state);
        }

        SemType t = tree.subExpr.accept(this, state);
        SemAn.exprOfType.put(tree, t.actualType());
        return t.actualType();
    }

    /**
     * Typechecker for AstStmtExpr node.
     * @param tree
     * @param state
     * @return actual type of the expression
     */
    @Override
    public SemType visit(AstStmtExpr tree, TypeCheckerState state) {
        SemType t = null;

        for(AstStmt s : tree.stmts.asts()) {
            t = s.accept(this, state);
        }

        SemAn.exprOfType.put(tree, t.actualType());
        return t.actualType();
    }

    /**
     * Typechecker for AstExprStmt tree node.
     * @param tree
     * @param state
     * @return actual type of the expression
     */
    @Override
    public SemType visit(AstExprStmt tree, TypeCheckerState state) {
        SemType t = tree.expr.accept(this, state);

        SemAn.stmtOfType.put(tree, t.actualType());
        return t.actualType();
    }

    /**
     * Typechecks if the two types  in assign stmt are compatible.
     * @param tree
     * @param state
     * @return SemVoid
     */
    @Override
    public SemType visit(AstAssignStmt tree, TypeCheckerState state) {
        SemType t1 = tree.fstSubExpr.accept(this, state);
        SemType t2 = tree.sndSubExpr.accept(this, state);

        if((t1.actualType() instanceof SemVoid) || (t1.actualType() instanceof SemArr)) {
            throw new Report.Error(tree.location, "Cannot assign to void or array");
        }

        if((t2.actualType() instanceof SemVoid) || (t2.actualType() instanceof SemArr)) {
            throw new Report.Error(tree.location, "Cannot assign to void or array");
        }

        if(!match(t1, t2, null)) {
            throw new Report.Error(tree.location, "Cannot assign " + t1 + " to " + t2);
        }
        SemAn.stmtOfType.put(tree, new SemVoid());
        return new SemVoid();
    }

    /**
     * TypeChecker for the AstIfStmt tree node.
     * @param tree
     * @param state
     * @return SemVoid
     */
    @Override
    public SemType visit(AstIfStmt tree, TypeCheckerState state) {
        SemType exprType = tree.condExpr.accept(this, state);

        if(!(exprType instanceof SemInt)) {
            throw new Report.Error(tree.location, "Condition of if statement must be boolean");
        }

        SemType thenType = tree.thenBodyStmt.accept(this, state);
        SemType elseType = null;

        if(tree.elseBodyStmt != null) {
            elseType = tree.elseBodyStmt.accept(this, state);
        }

        if(!(thenType instanceof SemVoid)) {
            throw new Report.Error(tree.location, "Then body of if statement must be void");
        }
        if(elseType != null && !(elseType instanceof SemVoid)) {
            throw new Report.Error(tree.location, "Else body of if statement must be void");
        }
        SemAn.stmtOfType.put(tree, new SemVoid());
        return new SemVoid();
    }

    /**
     * Typechecker for the AstWhileStmt tree node.
     * @param tree
     * @param state
     * @return SemVoid
     */
    @Override
    public SemType visit(AstWhileStmt tree, TypeCheckerState state) {
        SemType exprType = tree.condExpr.accept(this, state);

        if(!(exprType instanceof SemInt)) {
            throw new Report.Error(tree.location, "Condition of while statement must be boolean");
        }

        SemType bodyType = tree.bodyStmt.accept(this, state);

        if(!(bodyType instanceof SemVoid)) {
            throw new Report.Error(tree.location, "Body of while statement must be void");
        }
        SemAn.stmtOfType.put(tree, new SemVoid());
        return new SemVoid();
    }

    /**
     * Checks if two types are compatible.
     * If they match we link the two types in typeMap.
     * @param t1
     * @param t2
     * @param typeMap
     * @return boolean
     */
    private boolean match(SemType t1, SemType t2, HashMap<SemType, HashSet<SemType>> typeMap) {
        if ((t1 instanceof SemName) && (t2 instanceof SemName)) {
            if (typeMap == null) {
                typeMap = new HashMap<>();
            }
            if (typeMap.isEmpty()) {
                if (typeMap.get(t1).contains(t2) && typeMap.get(t2).contains(t1)) {
                    return true;
                } else {
                    HashSet<SemType> s1 = typeMap.get(t1);
                    s1.add(t2);
                    typeMap.put(t1, s1);

                    HashSet<SemType> s2 = typeMap.get(t2);
                    s2.add(t1);
                    typeMap.put(t2, s2);
                }
            }
        }

        t1 = t1.actualType();
        t2 = t2.actualType();

        if (t1 instanceof SemVoid) {
            return (t2 instanceof SemVoid);
        }

        if (t1 instanceof SemInt) {
            return (t2 instanceof SemInt);
        }

        if (t1 instanceof SemChar) {
            return (t2 instanceof SemChar);
        }

        if (t1 instanceof SemArr) {
            if (!(t2 instanceof SemArr)) {
                return false;
            }
            SemArr arr1 = (SemArr) t1;
            SemArr arr2 = (SemArr) t2;

            if (arr1.numElems != arr2.numElems) {
                return false;
            }
            return match(arr1.elemType, arr2.elemType, typeMap);
        }

        if(t1 instanceof SemPtr) {
            if(!(t2 instanceof SemPtr)) {
                return false;
            }

            SemPtr ptr1 = (SemPtr) t1;
            SemPtr ptr2 = (SemPtr) t2;

            if(ptr1.baseType.actualType() instanceof SemVoid || ptr2.baseType.actualType() instanceof SemVoid) {
                return true;
            }

            return match(ptr1.baseType, ptr2.baseType, typeMap);
        }

        throw new Report.InternalError();
    }
}
