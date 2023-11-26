package pins.phase.lexan;

import java.io.*;
import pins.common.report.*;
import pins.data.symbol.*;

/**
 * Lexical analyzer.
 */
public class LexAn implements AutoCloseable {

	private String srcFileName;

	private FileReader srcFile;

	/**
	 * lexeme <- current lexeme string
	 * col and row are used for Location class
	 * comment_count is used to count for comments (they can be nested)
	 */
	private String lexeme="";
	private int[] col = {1, 1};
	private int[] row = {1, 1};
	private int commment_count=0;
	private boolean char_flag=false;

	/**
	 * Constructor.
	 * @param srcFileName source file name
	 */
	public LexAn(String srcFileName) {
		this.srcFileName = srcFileName;
		try {
			srcFile = new FileReader(new File(srcFileName));
		} catch (FileNotFoundException __) {
			throw new Report.Error("Lexical phase: Cannot open source file '" + srcFileName + "'.");
		}
	}

	/**
	* Close the source file.
	*/
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file '" + srcFileName + "'.");
		}
	}

	/**
	 * Reads file char by char. Ignores comments
	 * @returns lexical symbol (Symbol class) when one can be constructed
	 */
	public Symbol lexer() {
		char curr_chr;
		try{
			while(srcFile.ready()) {
				//reads char from file
				curr_chr = (char) srcFile.read();

				// check curr_chr for ws,comments and lexeme for errors on ws
				chk_char(curr_chr);

				// append char to lexeme
				lexeme += curr_chr;

				// if lexeme matches
				if (commment_count==0 && lexeme.length() != 0 && switcher(lexeme) == null && switcher(lexeme.substring(0, lexeme.length() - 1)) != null) {
					// prepare args for return Symbol constructor
					Token return_token = switcher(lexeme.substring(0, lexeme.length() - 1));
					String return_lex = lexeme.substring(0, lexeme.length() - 1);
					Location return_location = new Location(row[0], col[0], row[1], col[1]-1);

					//set starting column on ending column
					col[0] = col[1];

					// cut and trim lexeme and handle ws for cols and rows
					lexeme = lexeme.substring(lexeme.length() - 1);
					ws_location(curr_chr);
					if(!char_flag) {
						lexeme = lexeme.trim();
					}

					// create Symbol and return it
					return new Symbol(return_token, return_lex, return_location);
				}

				// trim lexeme and handle ws for cols and rows
				ws_location(curr_chr);

				// if in comment we do not trim whitespaces (edgecase)
				if(commment_count == 0 && !char_flag) {
					lexeme = lexeme.trim();
				}
			}

			//handles last lexeme
			if (commment_count==0 && lexeme.length() != 0 && switcher(lexeme) != null) {
				// prepare args for return Symbol constructor
				Token return_token = switcher(lexeme);
				String return_lex = lexeme;
				Location return_location = new Location(row[0], col[0], row[1], col[1]-1);

				//set starting column on ending column
				col[0] = col[1];

				//empty out lexeme
				lexeme = "";

				// create Symbol and return it
				return new Symbol(return_token, return_lex, return_location);
			}

			//check for lexeme error
			checkError();

			// create EOF Symbol and return it
			return new Symbol(Token.EOF, "EOF", new Location(null));

		}catch (IOException __){
			throw new Report.Error("Lexical phase: IO error (lexer method)");
		}
	}

	/**
	 * Validates char for ws,comments and lexeme for errors on ws
	 * @param curr_chr
	 */
	private void chk_char(char curr_chr){
		//check for comments in lexeme
		chk_comments();

		if(curr_chr == '\'' && commment_count==0){
			if(lexeme.length() == 0){
				char_flag=!char_flag;
			}
			else{
				if(!lexeme.substring(lexeme.length()-1).contains("\\") || !lexeme.substring(lexeme.length()-1).contains("\\\\")){
					char_flag=!char_flag;
				}
			}
		}

		////check for errors when reading ws
		if(Character.isWhitespace(curr_chr)) {
			checkError();
		}


	}

	/**
	 * Sets class variables col and row according to ws character
	 **/
	private void ws_location(char curr_chr){
		switch (curr_chr) {
			case '\n' -> {
				row[1]++;
				row[0]++;
				col[0] = 1;
				col[1] = 1;
			}
			case '\r' -> {
				col[0] = 1;
				col[1] = 1;
			}
			case '\t' -> {
				col[0] += 8 - (col[0] % 8) + 1;
				col[1] += 8 - (col[1] % 8) + 1;
			}
			case ' ' -> {
				col[0]++;
				col[1]++;
			}
			default -> {
				if(commment_count != 0){
					col[0]++;
				}
				col[1]++;
			}
		}
	}

	/**
	 * Checks if current lexeme is invalid -> throws null
	 **/
	private void checkError(){
		if(commment_count==0 && lexeme.length() !=0 && !char_flag && switcher(lexeme) == null) {
			throw new Report.Error(new Location(row[0], col[0], row[1], col[1]), "Invalid token:" + lexeme);
		}
	}

	/**
	 * Functions checks for comments. #{, #}
	 * sets commment_count variable and empties lexeme if comment is detected
	 **/
	private void chk_comments(){
		if(lexeme.contains("#{") && !lexeme.contains("'#{")){
			commment_count++;
			lexeme="";
		}
		else if(lexeme.contains("}#") && !lexeme.contains("'}#")){
			commment_count--;
			lexeme="";
			col[0]=col[1];
		}
	}

	/**
	 * Function matches input string (lexeme) with lexical symbol
	 * @returns Token (enum). if no match is found returns null
	 */
	private Token switcher(String token){
		switch (token){
			case "(":
				return Token.SYM_LPARENT;
			case ")":
				return Token.SYM_RPARENT;
			case "{":
				return Token.SYM_LBRACKET;
			case "}":
				return Token.SYM_RBRACKET;
			case "[":
				return Token.SYM_LSQBRACKET;
			case "]":
				return Token.SYM_RSQBRACKET;
			case ",":
				return Token.SYM_COMMA;
			case ":":
				return Token.SYM_COLON;
			case ";":
				return Token.SYM_SEMICOLON;
			case "&":
				return Token.SYM_AND;
			case "|":
				return Token.SYM_OR;
			case "!":
				return Token.SYM_NOT;
			case "=":
				return Token.SYM_EQUAL;
			case "==":
				return Token.SYM_DOUBLE_EQUAL;
			case "!=":
				return Token.SYM_NOT_EQUAL;
			case "<":
				return Token.SYM_LOWER;
			case ">":
				return Token.SYM_GREATER;
			case "<=":
				return Token.SYM_LOWEREQUAL;
			case ">=":
				return Token.SYM_GREATEREQUAL;
			case "*":
				return Token.SYM_MUL;
			case "/":
				return Token.SYM_DIV;
			case "%":
				return Token.SYM_MOD;
			case "+":
				return Token.SYM_SUM;
			case "-":
				return Token.SYM_SUB;
			case "^":
				return Token.SYM_POINT;
			case "char":
				return Token.KW_CHAR;
			case "del":
				return Token.KW_DEL;
			case "do":
				return Token.KW_DO;
			case "else":
				return Token.KW_ELSE;
			case "end":
				return Token.KW_END;
			case "fun":
				return Token.KW_FUN;
			case "if":
				return Token.KW_IF;
			case "int":
				return Token.KW_INT;
			case "new":
				return Token.KW_NEW;
			case "then":
				return Token.KW_THEN;
			case "typ":
				return Token.KW_TYP;
			case "var":
				return Token.KW_VAR;
			case "void":
				return Token.KW_VOID;
			case "where":
				return Token.KW_WHERE;
			case "while":
				return Token.KW_WHILE;
			case "none":
				return Token.CONST_VOID;
			case "nil":
				return Token.CONST_NIL;
			case "'\\''":
				return Token.CONST_CHAR;
			case "'\\\\'":
				return Token.CONST_CHAR;
			default:
				if(matchID(token)){
					return Token.ID;
				}
				else if(matchINTconst(token)){
					return Token.CONST_INT;
				}
				else if(matchCHARconst(token)){
					return Token.CONST_CHAR;
				}
		}

		return null;
	}

	/**
	 * Function CHAR for regex match
	 * @returns true if lexeme matches
	 **/
	private boolean matchCHARconst(String token) {
		return token.matches("['][\\x20-\\x26\\x28-\\x5B\\x5D-\\x7E][']");
	}

	/**
	 * Function ID for regex match
	 * @param token
	 * @return
	 */
	private boolean matchID(String token) {
		return token.matches("[A-Za-z_][A-Za-z_0-9]*");
	}

	/**
	 * Function INT for regex match
	 * @param token
	 * @return
	 */
	private boolean matchINTconst(String token) {
		return token.matches("[0-9]+");
	}
}


