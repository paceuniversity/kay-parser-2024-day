package com.scanner.project;

// KAY language parser

public class ConcreteSyntax {

    public Token token;
    public TokenStream input;

    public ConcreteSyntax(TokenStream ts) {
        input = ts;
        token = input.nextToken();
    }

    private String SyntaxError(String expected) {
        String s = "Syntax error - Expecting: " + expected + " But saw: "
                + token.getType() + " = " + token.getValue();
        System.out.println(s);
        throw new RuntimeException(s);
    }

    private void match(String expected) {
        boolean isTokenType = expected.equals("Identifier") || expected.equals("Keyword") || 
                              expected.equals("Literal") || expected.equals("Operator") || 
                              expected.equals("Separator") || expected.equals("EOF");
        
        boolean matched = false;
        if (isTokenType) {
            matched = token.getType().equals(expected);
        } else {
            matched = token.getValue().equals(expected);
        }
        
        if (matched) {
            token = input.nextToken();
        } else {
            throw new RuntimeException("Syntax error - Expecting: " + expected
                    + " But saw: " + token.getType() + " = " + token.getValue());
        }
    }

    public Program program() {
        match("main");
        match("{");
        Declarations decpart = declarations();
        Block body = statements();
        match("}");
        return new Program(decpart, body);
    }

    private Declarations declarations() {
        Declarations decs = new Declarations();

        while (token.getValue().equals("integer") || token.getValue().equals("bool")) {
            String type = token.getValue();
            match("Keyword");
            
            String id = token.getValue();
            match("Identifier");
            
            Declaration d = new Declaration();
            d.v = new Variable();
            d.v.id = id;
            d.t = new Type(type);
            decs.add(d);
            
            while (token.getValue().equals(",")) {
                match(",");
                id = token.getValue();
                match("Identifier");
                
                d = new Declaration();
                d.v = new Variable();
                d.v.id = id;
                d.t = new Type(type);
                decs.add(d);
            }
            
            match(";");
        }
        return decs;
    }

    private Block statements() {
        Block b = new Block();

        while (token.getType().equals("Identifier") ||
               token.getValue().equals("if") ||
               token.getValue().equals("while")) {
            b.blockmembers.add(statement());
        }
        return b;
    }

    private Statement statement() {
        if (token.getType().equals("Identifier")) {
            return assignment();
        } else if (token.getValue().equals("if")) {
            return ifStatement();
        } else if (token.getValue().equals("while")) {
            return whileStatement();    
        } else {
            throw new RuntimeException("Syntax error - Unexpected token in statement: "
                    + token.getType() + " = " + token.getValue());
        }
    }

    private Assignment assignment() {
        String id = token.getValue();
        match("Identifier");
        match(":=");
        Expression e = expression();
        match(";");

        Assignment a = new Assignment();
        a.target = new Variable();
        a.target.id = id;
        a.source = e;
        return a;
    }

    private Conditional ifStatement() {
        match("if"); 
        match("(");
        Expression cond = expression();
        match(")");
        match("{");
        Block thenPart = statements();
        match("}");
        Block elsePart = null;
        if (token.getValue().equals("else")) {
            match("else");
            match("{");
            elsePart = statements();
            match("}");
        }

        Conditional c = new Conditional();
        c.test = cond;
        c.thenbranch = thenPart;
        c.elsebranch = elsePart;
        return c;
    }

    private Loop whileStatement() {
        match("while");
        match("(");
        Expression cond = expression();
        match(")");
        match("{");
        Block body = statements();
        match("}");

        Loop l = new Loop();
        l.test = cond;
        l.body = body;
        return l;
    }

    private Expression expression() {
        Expression e = term();
        while (token.getValue().equals("+") || token.getValue().equals("-") ||
               token.getValue().equals("<") || token.getValue().equals(">") ||
               token.getValue().equals("<=") || token.getValue().equals(">=") ||
               token.getValue().equals("==") || token.getValue().equals("!=") ||
               token.getValue().equals("&&") || token.getValue().equals("||")) {

            String op = token.getValue();
            match(op);
            Expression e2 = term();

            Binary b = new Binary();
            b.op = new Operator(op);
            b.term1 = e;
            b.term2 = e2;
            e = b;
        }
        return e;
    }

    private Expression term() {
        Expression e = factor();
        while (token.getValue().equals("*") || token.getValue().equals("/")) {
            String op = token.getValue();
            match(op);
            Expression e2 = factor();

            Binary b = new Binary();
            b.op = new Operator(op);
            b.term1 = e;
            b.term2 = e2;
            e = b;
        }
        return e;
    }

    private Expression factor() {
        Expression e = null;

        if (token.getType().equals("Identifier")) {
            Variable v = new Variable();
            v.id = token.getValue();
            e = v;
            match("Identifier");

        } else if (token.getType().equals("Literal")) {
            e = new Value(Integer.parseInt(token.getValue()));
            match("Literal");

        } else if (token.getType().equals("Keyword") && token.getValue().equals("True")) {
            e = new Value(true);
            match("True");

        } else if (token.getType().equals("Keyword") && token.getValue().equals("False")) {
            e = new Value(false);
            match("False");

        } else if (token.getValue().equals("(")) {
            match("(");
            e = expression();
            match(")");

        } else {
            throw new RuntimeException("Syntax error in factor - saw: "
                    + token.getType() + " = " + token.getValue());
        }

        return e;
    }
}
