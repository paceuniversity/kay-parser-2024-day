package com.scanner.project;

// ConcreteSyntax.java

// Implements the parser for KAY language

public class ConcreteSyntax {
	
	private TokenStream tokens;
	private Token token;
	
	public ConcreteSyntax(TokenStream ts) {
		tokens = ts;
		token = tokens.nextToken();
	}
	
	// Match a token and advance to the next one
	private void match(String expectedType, String expectedValue) {
		if (token.getType().equals(expectedType) && token.getValue().equals(expectedValue)) {
			token = tokens.nextToken();
		} else {
			throw new RuntimeException("Syntax error - Expecting: " + expectedValue + 
			                         " But saw: " + token.getType() + " = " + token.getValue());
		}
	}
	
	// Match just a type
	private void match(String expectedType) {
		if (token.getType().equals(expectedType)) {
			token = tokens.nextToken();
		} else {
			throw new RuntimeException("Syntax error - Expecting type: " + expectedType + 
			                         " But saw: " + token.getType() + " = " + token.getValue());
		}
	}
	
	// Program = main { Declarations Statements }
	public Program program() {
		match("Keyword", "main");
		match("Separator", "{");
		
		Program prog = new Program();
		prog.decpart = declarations();
		prog.body = statements();
		
		match("Separator", "}");
		
		return prog;
	}
	
	// Declarations = { Declaration }
	// Declaration can have multiple identifiers: Type Identifier { , Identifier } ;
	private Declarations declarations() {
		Declarations ds = new Declarations();
		
		// Keep reading declarations while we see type keywords
		while (token.getType().equals("Keyword") && 
		       (token.getValue().equals("integer") || token.getValue().equals("bool"))) {
			// Get the type first
			Type t = type();
			
			// First identifier
			Declaration d = new Declaration();
			d.t = t;
			d.v = new Variable();
			d.v.id = token.getValue();
			match("Identifier");
			ds.add(d);
			
			// Check for more identifiers separated by commas
			while (token.getType().equals("Separator") && token.getValue().equals(",")) {
				match("Separator", ",");
				Declaration d2 = new Declaration();
				d2.t = t;
				d2.v = new Variable();
				d2.v.id = token.getValue();
				match("Identifier");
				ds.add(d2);
			}
			
			match("Separator", ";");
		}
		
		return ds;
	}
	
	// Type = integer | bool
	private Type type() {
		Type t;
		if (token.getValue().equals("integer")) {
			t = new Type(Type.INTEGER);
			match("Keyword", "integer");
		} else if (token.getValue().equals("bool")) {
			t = new Type(Type.BOOLEAN);
			match("Keyword", "bool");
		} else {
			throw new RuntimeException("Syntax error - Expecting type (integer or bool) but saw: " + 
			                         token.getType() + " = " + token.getValue());
		}
		return t;
	}
	
	// Statements = { Statement }
	private Block statements() {
		Block b = new Block();
		
		// Keep reading statements while we don't see a closing brace
		while (!token.getValue().equals("}")) {
			Statement s = statement();
			b.blockmembers.add(s);
		}
		
		return b;
	}
	
	// Statement = Block | Assignment | IfStatement | WhileStatement
	private Statement statement() {
		Statement s;
		
		if (token.getValue().equals("{")) {
			s = block();
		} else if (token.getValue().equals("if")) {
			s = ifStatement();
		} else if (token.getValue().equals("while")) {
			s = whileStatement();
		} else if (token.getType().equals("Identifier")) {
			s = assignment();
		} else {
			throw new RuntimeException("Syntax error in statement - saw: " + 
			                         token.getType() + " = " + token.getValue());
		}
		
		return s;
	}
	
	// Block = { Statements }
	private Block block() {
		match("Separator", "{");
		Block b = statements();
		match("Separator", "}");
		return b;
	}
	
	// Assignment = Identifier := Expression ;
	private Assignment assignment() {
		Assignment a = new Assignment();
		a.target = new Variable();
		a.target.id = token.getValue();
		match("Identifier");
		match("Operator", ":=");
		a.source = expression();
		match("Separator", ";");
		
		return a;
	}
	
	// IfStatement = if ( Expression ) Statement [ else Statement ]
	private Conditional ifStatement() {
		Conditional c = new Conditional();
		match("Keyword", "if");
		match("Separator", "(");
		c.test = expression();
		match("Separator", ")");
		c.thenbranch = statement();
		
		// Check for optional else branch
		if (token.getType().equals("Keyword") && token.getValue().equals("else")) {
			match("Keyword", "else");
			c.elsebranch = statement();
		}
		
		return c;
	}
	
	// WhileStatement = while ( Expression ) Statement
	private Loop whileStatement() {
		Loop l = new Loop();
		match("Keyword", "while");
		match("Separator", "(");
		l.test = expression();
		match("Separator", ")");
		l.body = statement();
		
		return l;
	}
	
	// Expression = Conjunction { || Conjunction }
	private Expression expression() {
		Expression e = conjunction();
		
		while (token.getType().equals("Operator") && token.getValue().equals("||")) {
			Operator op = new Operator(token.getValue());
			match("Operator", "||");
			Expression term2 = conjunction();
			
			Binary b = new Binary();
			b.op = op;
			b.term1 = e;
			b.term2 = term2;
			e = b;
		}
		
		return e;
	}
	
	// Conjunction = Equality { && Equality }
	private Expression conjunction() {
		Expression e = equality();
		
		while (token.getType().equals("Operator") && token.getValue().equals("&&")) {
			Operator op = new Operator(token.getValue());
			match("Operator", "&&");
			Expression term2 = equality();
			
			Binary b = new Binary();
			b.op = op;
			b.term1 = e;
			b.term2 = term2;
			e = b;
		}
		
		return e;
	}
	
	// Equality = Relation [ EquOp Relation ]
	private Expression equality() {
		Expression e = relation();
		
		while (token.getType().equals("Operator") && 
		       (token.getValue().equals("==") || token.getValue().equals("!="))) {
			Operator op = new Operator(token.getValue());
			String opValue = token.getValue();
			match("Operator", opValue);
			Expression term2 = relation();
			
			Binary b = new Binary();
			b.op = op;
			b.term1 = e;
			b.term2 = term2;
			e = b;
		}
		
		return e;
	}
	
	// Relation = Addition [ RelOp Addition ]
	private Expression relation() {
		Expression e = addition();
		
		while (token.getType().equals("Operator") && 
		       (token.getValue().equals("<") || token.getValue().equals("<=") ||
		        token.getValue().equals(">") || token.getValue().equals(">=") ||
		        token.getValue().equals("<>"))) {
			Operator op = new Operator(token.getValue());
			String opValue = token.getValue();
			match("Operator", opValue);
			Expression term2 = addition();
			
			Binary b = new Binary();
			b.op = op;
			b.term1 = e;
			b.term2 = term2;
			e = b;
		}
		
		return e;
	}
	
	// Addition = Term { AddOp Term }
	private Expression addition() {
		Expression e = term();
		
		while (token.getType().equals("Operator") && 
		       (token.getValue().equals("+") || token.getValue().equals("-"))) {
			Operator op = new Operator(token.getValue());
			String opValue = token.getValue();
			match("Operator", opValue);
			Expression term2 = term();
			
			Binary b = new Binary();
			b.op = op;
			b.term1 = e;
			b.term2 = term2;
			e = b;
		}
		
		return e;
	}
	
	// Term = Factor { MulOp Factor }
	private Expression term() {
		Expression e = factor();
		
		while (token.getType().equals("Operator") && 
		       (token.getValue().equals("*") || token.getValue().equals("/"))) {
			Operator op = new Operator(token.getValue());
			String opValue = token.getValue();
			match("Operator", opValue);
			Expression term2 = factor();
			
			Binary b = new Binary();
			b.op = op;
			b.term1 = e;
			b.term2 = term2;
			e = b;
		}
		
		return e;
	}
	
	// Factor = [ UnaryOp ] Primary
	private Expression factor() {
		if (token.getType().equals("Operator") && 
		    (token.getValue().equals("!") || token.getValue().equals("-"))) {
			Operator op = new Operator(token.getValue());
			String opValue = token.getValue();
			match("Operator", opValue);
			Expression term = primary();
			
			Unary u = new Unary();
			u.op = op;
			u.term = term;
			return u;
		} else {
			return primary();
		}
	}
	
	// Primary = Identifier | Literal | ( Expression )
	private Expression primary() {
		Expression e;
		
		if (token.getType().equals("Identifier")) {
			Variable v = new Variable();
			v.id = token.getValue();
			match("Identifier");
			e = v;
		} else if (token.getType().equals("Literal")) {
			e = literal();
		} else if (token.getValue().equals("(")) {
			match("Separator", "(");
			e = expression();
			match("Separator", ")");
		} else {
			throw new RuntimeException("Syntax error in primary expression - saw: " + 
			                         token.getType() + " = " + token.getValue());
		}
		
		return e;
	}
	
	// Literal = IntegerLiteral | BooleanLiteral
	private Value literal() {
		Value v;
		
		if (token.getValue().equals("True")) {
			v = new Value(true);
			match("Literal", "True");
		} else if (token.getValue().equals("False")) {
			v = new Value(false);
			match("Literal", "False");
		} else {
			// Integer literal
			int intValue = Integer.parseInt(token.getValue());
			v = new Value(intValue);
			match("Literal");
		}
		
		return v;
	}
}
