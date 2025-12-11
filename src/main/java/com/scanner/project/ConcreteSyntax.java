package com.scanner.project;

// parser for KAY language

public class ConcreteSyntax {

	public Token token;
	public TokenStream input;

	public ConcreteSyntax(TokenStream ts) {
		input = ts;
		token = input.nextToken();
	}

	// prints error message when something goes wrong
	private String SyntaxError(String tok) {
		String s = "Syntax error - Expecting: " + tok + " But saw: "
				+ token.getType() + " = " + token.getValue();
		System.out.println(s);
		return s;
	}

	// checks if current token matches what we expect
	private void match(String s) {
		if (token.getValue().equals(s))
			token = input.nextToken();
		else
			throw new RuntimeException(SyntaxError(s));
	}

	// entry point - parses the whole program
	public Program program() {
		String[] header = { "main", "{" };
		Program p = new Program();
		for (int i = 0; i < header.length; i++)
			match(header[i]);
		p.decpart = declarations();
		p.body = statements();
		match("}");
		return p;
	}

	// keeps reading declarations until we hit something else
	private Declarations declarations() {
		Declarations ds = new Declarations();
		while (token.getValue().equals("integer") || token.getValue().equals("bool")) {
			declaration(ds);
		}
		return ds;		
	}

	// single declaration line
	private void declaration(Declarations ds) {
		Type t = type();
		identifiers(ds, t);
		match(";");
	}

	// gets the type (integer or bool)
	private Type type() {
		Type t = null;
		if (token.getValue().equals("integer"))
			t = new Type(token.getValue());
		else if (token.getValue().equals("bool"))
			t = new Type(token.getValue());
		else
			throw new RuntimeException(SyntaxError("integer | bool"));
		token = input.nextToken();
		return t;
	}

	// handles comma separated variable names
	private void identifiers(Declarations ds, Type t) {
		Declaration d = new Declaration();
		d.t = t;
		if (token.getType().equals("Identifier")) {
			d.v = new Variable();
			d.v.id = token.getValue();
			ds.addElement(d);
			token = input.nextToken();
			while (token.getValue().equals(",")) {
				d = new Declaration();
				d.t = t;
				token = input.nextToken();
				if (token.getType().equals("Identifier")) {
					d.v = new Variable();
					d.v.id = token.getValue();
					ds.addElement(d);
					token = input.nextToken();
				} else
					throw new RuntimeException(SyntaxError("Identifier"));
			}
		} else
			throw new RuntimeException(SyntaxError("Identifier"));
	}

	// figures out what kind of statement we're looking at
	private Statement statement() {
		Statement s = new Skip();
		if (token.getValue().equals(";")) {
			token = input.nextToken();
			return s;
		} else if (token.getValue().equals("{")) {
			token = input.nextToken();
			s = statements();
			match("}");
		} else if (token.getValue().equals("if"))
			s = ifStatement();
		else if (token.getValue().equals("while")) {
			s = whileStatement();
		} else if (token.getType().equals("Identifier")) {
			s = assignment();
		} else
			throw new RuntimeException(SyntaxError("Statement"));
		return s;
	}

	// block of statements inside braces
	private Block statements() {
		Block b = new Block();
		while (!token.getValue().equals("}")) {
			b.blockmembers.addElement(statement());
		}
		return b;
	}

	// variable assignment like x := 5
	private Assignment assignment() {
		Assignment a = new Assignment();
		if (token.getType().equals("Identifier")) {
			a.target = new Variable();
			a.target.id = token.getValue();
			token = input.nextToken();
			match(":=");
			a.source = expression();
			match(";");
		} else
			throw new RuntimeException(SyntaxError("Identifier"));
		return a;
	}

	// handles || (or)
	private Expression expression() {
		Binary b;
		Expression e;
		e = conjunction();
		while (token.getValue().equals("||")) {
			b = new Binary();
			b.term1 = e;
			b.op = new Operator(token.getValue());
			token = input.nextToken();
			b.term2 = conjunction();
			e = b;
		}
		return e;
	}

	// handles && (and)
	private Expression conjunction() {
		Binary b;
		Expression e;
		e = relation();
		while (token.getValue().equals("&&")) {
			b = new Binary();
			b.term1 = e;
			b.op = new Operator(token.getValue());
			token = input.nextToken();
			b.term2 = relation();
			e = b;
		}
		return e;
	}

	// handles comparisons like < > == etc
	private Expression relation() {
		Binary b;
		Expression e;
		e = addition();
		while (token.getValue().equals("<") || token.getValue().equals("<=")
				|| token.getValue().equals(">")
				|| token.getValue().equals(">=")
				|| token.getValue().equals("==")
				|| token.getValue().equals("<>")) {
			b = new Binary();
			b.op = new Operator(token.getValue());
			token = input.nextToken();
			b.term1 = e;
			b.term2 = addition();
			e = b;
		}
		return e;
	}

	// handles + and -
	private Expression addition() {
		Binary b;
		Expression e;
		e = term();
		while (token.getValue().equals("+") || token.getValue().equals("-")) {
			b = new Binary();
			b.op = new Operator(token.getValue());
			token = input.nextToken();
			b.term1 = e;
			b.term2 = term();
			e = b;
		}
		return e;
	}

	// handles * and /
	private Expression term() {
		Binary b;
		Expression e;
		e = negation();
		while (token.getValue().equals("*") || token.getValue().equals("/")) {
			b = new Binary();
			b.op = new Operator(token.getValue());
			token = input.nextToken();
			b.term1 = e;
			b.term2 = negation();
			e = b;
		}
		return e;
	}

	// handles the ! operator
	private Expression negation() {
		Unary u;
		if (token.getValue().equals("!")) {
			u = new Unary();
			u.op = new Operator(token.getValue());
			token = input.nextToken();
			u.term = factor();
			return u;
		} else
			return factor();
	}

	// the basic building blocks - variables, numbers, or parenthesized expressions
	private Expression factor() {
		Expression e = null;
		if (token.getType().equals("Identifier")) {
			Variable v = new Variable();
			v.id = token.getValue();
			e = v;
			token = input.nextToken();
		} else if (token.getType().equals("Literal")) {
			Value v = null;
			if (isInteger(token.getValue()))
				v = new Value((new Integer(token.getValue())).intValue());
			else if (token.getValue().equals("True"))
				v = new Value(true);
			else if (token.getValue().equals("False"))
				v = new Value(false);
			else
				throw new RuntimeException(SyntaxError("Literal"));
			e = v;
			token = input.nextToken();
		} else if (token.getValue().equals("(")) {
			token = input.nextToken();
			e = expression();
			match(")");
		} else
			throw new RuntimeException(SyntaxError("Identifier | Literal | ("));
		return e;
	}

	// if statement with optional else
	private Conditional ifStatement() {
		Conditional c = new Conditional();
		match("if");
		match("(");
		c.test = expression();
		match(")");
		c.thenbranch = statement();
		c.elsebranch = null;
		if (token.getValue().equals("else")) {
			token = input.nextToken();
			c.elsebranch = statement();
		}
		return c;
	}

	// while loop
	private Loop whileStatement() {
		Loop l = new Loop();
		match("while");
		match("(");
		l.test = expression();
		match(")");
		l.body = statement();
		return l;
	}

	// helper to check if a string is a number
	private boolean isInteger(String s) {
		boolean result = true;
		for (int i = 0; i < s.length(); i++)
			if ('0' > s.charAt(i) || '9' < s.charAt(i))
				result = false;
		return result;
	}

}
