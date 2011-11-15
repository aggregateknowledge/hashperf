/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A CSV parser that operates record-by-record (row-by-row). It is reusable but
 * <i>not</i> thread-safe. Any record delimiters (e.g. newline) found in a 
 * record are treated as literals. Various escaping modes are available. No
 * internal structure is compacted when reused so there is the danger that a 
 * single malformed for large record may cause a large amount of heap to be
 * used and not released.
 */
// NOTE:  naming matches that of CsvReader from csvreader.com since traditionally
//        that has been the CSV reader that has been used.
public class CsvRecordReader {
    // initial / default values
    private static final int COLUMN_COUNT = 10;
    private static final int COLUMN_BUFFER_SIZE = 25;

    // ************************************************************************
    // the strategy used for CSV parsing
    private final ParseStrategy parseStrategy;

    // ------------------------------------------------------------------------
    // input record buffers
    public char[] inputBuffer/*none until copyRecordToBuffer()*/;
    public int inputBufferPosition = 0/*default*/;
    public int inputBufferSize = 0/*default*/;

    // per-column buffers
    private StringBuilder columnBuffer = new StringBuilder(COLUMN_BUFFER_SIZE);
    private List<String> columns/*allocated on readRecord()*/;

    // ------------------------------------------------------------------------
    // between-method state
    private boolean inColumn = false;
    private boolean qualifiedColumn = false;
    private boolean lastCharWasDelimiter = false;

    // ========================================================================
    /**
     * @param  delimiter the character to use as the column delimiter. This
     *         constructor is provided for convenience.
     */
    public CsvRecordReader(final char delimiter) {
        this(new ParseStrategy()/*default strategy*/);

        this.parseStrategy.delimiter = delimiter;
    }

    /**
     * @param  strategy the <code>ParseStrategy</code> to be used. This cannot
     *         be <code>null</code>.
     */
    public CsvRecordReader(final ParseStrategy strategy) {
        this.parseStrategy = strategy;
    }

    // ========================================================================
    /**
     * @param  record the record to be read. It should not be terminated. This
     *         cannot be <code>null</code> though it may be blank.
     * @return the array of parsed columns. This will never be <code>null</code>
     *         though it may be empty
     */
    public String[] readRecord(final String record) {
        // initialize the state
        columns = new ArrayList<String>(COLUMN_COUNT);
        copyRecordToBuffer(record)/*force read (resets state, etc)*/;

        // loop over all non-column data (essentially, look for column delimiters
        // and whitespace)
        while(inputBufferPosition < inputBufferSize) {
            qualifiedColumn = false/*reset for new column*/;

            final char currentChar = inputBuffer[inputBufferPosition/*don't advance -- look ahead*/];
            lastCharWasDelimiter = false/*until determined otherwise below*/;
            if(parseStrategy.useTextQualifier && (currentChar == parseStrategy.textQualifier)) {
                inColumn = true;
                readQualified();
            } else if(currentChar == parseStrategy.delimiter) {
                // a column with no contents
                lastCharWasDelimiter = true/*by definition*/;
                inputBufferPosition++;
                endColumn();
            } else if(parseStrategy.useComments && columns.isEmpty() && (currentChar == parseStrategy.commentChar)) {
                // a comment at the beginning of the line (comments anywhere
                // else are treated as part of the contents) means that there's
                // nothing more to do
                return new String[0]/*nothing read*/;
            } else if(parseStrategy.trimWhitespace && Character.isWhitespace(currentChar)) {
                inputBufferPosition++/*advance and ignore current char*/;
                inColumn = true;
            } else {/*non-delimiter, non-whitespace, non-qualifier*/
                inColumn = true;
                readUnqualified();
            }
        }

        // end-of-record reached (while still processing a column)
        if(inColumn || lastCharWasDelimiter)
            endColumn();
        /* else -- did not end while in a column */

        return columns.toArray(new String[0]/*toArray() will allocate*/);
    }

    /**
     * Reads a qualified column (meaning a column that is defined by the text
     * qualifier (e.g. double quotes)).
     * 
     * @see #readUnqualified()
     */
    private void readQualified() {
        qualifiedColumn = true;

        boolean ignoreTrailing = false;
        boolean lastCharWasEscape = false;
        boolean lastCharWasQualifier = false;
        final char escapeChar = (parseStrategy.escapeMode == EscapeMode.BACKSLASH) ?
                                    Chars.BACKSLASH :
                                    parseStrategy.textQualifier;

        // advance past the qualifier
        inputBufferPosition++;

        do {
            final char currentChar = inputBuffer[inputBufferPosition++];

            if(ignoreTrailing) {
                // stop when the delimiter is reached
                if(currentChar == parseStrategy.delimiter)
                    endColumn();
                /* else -- not the delimiter */
            } else if(currentChar == parseStrategy.textQualifier) {
                if(lastCharWasEscape) {
                    appendChar(currentChar);
                    lastCharWasEscape = false;
                    lastCharWasQualifier = false;
                } else { /*the last char was not an escape*/
                    if(parseStrategy.escapeMode == EscapeMode.DOUBLED)
                        lastCharWasEscape = true;
                    /* else -- not 'doubled' escape mode */

                    lastCharWasQualifier = true;

                    // NOTE:  the column isn't ended until the delimiter is found
                    //        (any trailing characters will be discarded)
                }
            } else if(lastCharWasEscape && (parseStrategy.escapeMode == EscapeMode.BACKSLASH)) {
                readEscaped(currentChar);
                lastCharWasEscape = false;
            } else if(currentChar == escapeChar) {
                lastCharWasEscape = true;
            } else if(lastCharWasQualifier) {
                if(currentChar == parseStrategy.delimiter)
                    endColumn();
                else /*the qualified column has finished but the delimiter hasn't been found*/
                    ignoreTrailing = true;

                lastCharWasQualifier = false/*reset*/;
            } else /*any other character*/
                appendChar(currentChar);
        } while( (inputBufferPosition < inputBufferSize) && inColumn);
    }

    /**
     * Reads an unqualified column (meaning a column that is not defined by the 
     * text qualifier (e.g. double quotes)).
     * 
     * @see #readQualified()
     */
    private void readUnqualified() {
        boolean lastCharWasEscape = false;
        do {
            final char currentChar = inputBuffer[inputBufferPosition++];

            if(!parseStrategy.useTextQualifier && (parseStrategy.escapeMode == EscapeMode.BACKSLASH) && (currentChar == Chars.BACKSLASH)) {
                if(lastCharWasEscape) {
                    appendChar(currentChar);
                    lastCharWasEscape = false;
                } else /*the last char was not an escape (backslash)*/
                    lastCharWasEscape = true;
            } else if(lastCharWasEscape && (parseStrategy.escapeMode == EscapeMode.BACKSLASH)) {
                readEscaped(currentChar);
                lastCharWasEscape = false;
            } else if(currentChar == parseStrategy.delimiter) {
                lastCharWasDelimiter = true/*by definition*/;
                endColumn();
            } else /*any other character*/
                appendChar(currentChar);
        } while( (inputBufferPosition < inputBufferSize) && inColumn);
    }

    /**
     * @param character the character to be appended to the column buffer
     */
    private void appendChar(final char character) {
        columnBuffer.append(character);
    }

    // ------------------------------------------------------------------------
    /**
     * @param  record the record to be read. It will not be terminated. This
     *         cannot be <code>null</code> though it may be blank.
     */
    private void copyRecordToBuffer(final String record) {
        inputBufferSize = record.length();
        inputBuffer = new char[inputBufferSize];
        record.getChars(0, inputBufferSize, inputBuffer, 0);

        inputBufferPosition = 0;
    }

    // ------------------------------------------------------------------------
    /**
     * Creates a new column by copying the text from the column buffer.
     */
    private void endColumn() {
        if(inColumn) {
            // whitespace trim if configured to do so (and not in a qualified column) 
            int lastCharIndex = columnBuffer.length() - 1;
            if(parseStrategy.trimWhitespace && !qualifiedColumn) {
                while((lastCharIndex >= 0) && Character.isWhitespace(columnBuffer.charAt(lastCharIndex)))
                    lastCharIndex--;
            } /* else -- don't trim or in a qualified column */

            columns.add(columnBuffer.substring(0, (lastCharIndex + 1)));
        } else/*wasn't in a column / empty column*/
            columns.add(""/*empty*/);

        // reset for next column
        columnBuffer.setLength(0/*reset*/);
        inColumn = false;
    }

    // ------------------------------------------------------------------------
    /**
     * Reads the current char assuming it is escaped. More chars are read based
     * on the escape sequence (e.g. octal, hex, etc).
     * 
     * @param  currentChar the current escaped char that is to be read
     */
    private void readEscaped(final char currentChar) {
        switch(currentChar) {
            case 'a':
                appendChar(Chars.ALERT);
                break;
            case 'b':
                appendChar(Chars.BACKSPACE);
                break;
            case 'e':
                appendChar(Chars.ESCAPE);
                break;
            case 'f':
                appendChar(Chars.FORM_FEED);
                break;
            case 'n':
                appendChar(Chars.LF);
                break;
            case 'r':
                appendChar(Chars.CR);
                break;
            case 't':
                appendChar(Chars.TAB);
                break;
            case 'v':
                appendChar(Chars.VERTICAL_TAB);
                break;

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                readOctal(currentChar);
                break;
            case 'd':
            case 'D':
                readDecimal();
                break;
            case 'o':
            case 'O':
                readOctal();
                break;
            case 'u':
            case 'U':
                readUnicode();
                break;
            case 'x':
            case 'X':
                readHex();
                break;

            default:
                // treat as a literal (even though it's escaped)
                appendChar(currentChar);
                break;
        }
    }

    // NOTE:  none of these methods ensure that the escaped character is in 
    //        range. This is primarily so as to not force the caller to catch
    //        exceptions. This can be revisited as need be.

    /**
     * Reads and appends the unicode escaped text.
     */
    private void readUnicode() {
        // NOTE:  this *may* exit early if there are no enough characters. That
        //        is allowed and the result is that the value will be wrong

        int charsRemaining = 4/*4 chars in a unicode representation*/;
        char value = (char)0;
        while( (inputBufferPosition < inputBufferSize) && (charsRemaining-- > 0) ) {
            final char currentChar = inputBuffer[inputBufferPosition++];

            value <<= 4;
            value += fromHex(currentChar);
        }
        appendChar(value);
    }

    /**
     * Reads and appends the octal escaped text where the first character is
     * specified.
     * 
     * @param  currentChar the first character of the octal representation.
     * @see #readOctal()  
     */
    private void readOctal(char currentChar) {
        // NOTE:  this *may* exit early if there are no enough characters. That
        //        is allowed and the result is that the value will be wrong
        
        int charsRemaining = 2/*3 chars total in an octal representation (1 was already read)*/;
        char value = (char)(currentChar - '0');
        while( (inputBufferPosition < inputBufferSize) && (charsRemaining-- > 0) ) {
            currentChar = inputBuffer[inputBufferPosition++];

            value <<= 3;
            value += (char)(currentChar - '0');
        }
        appendChar(value);
    }

    /**
     * Reads and appends the octal escaped text.
     * 
     * @see #readOctal(char)
     */
    private void readOctal() {
        // NOTE:  this *may* exit early if there are no enough characters. That
        //        is allowed and the result is that the value will be wrong

        int charsRemaining = 3/*3 chars in an octal representation*/;
        char value = (char)0;
        while( (inputBufferPosition < inputBufferSize) && (charsRemaining-- > 0) ) {
            final char currentChar = inputBuffer[inputBufferPosition++];

            value <<= 3;
            value += (char)(currentChar - '0');
        }
        appendChar(value);
    }

    /**
     * Reads and appends the decimal escaped text.
     */
    private void readDecimal() {
        // NOTE:  this *may* exit early if there are no enough characters. That
        //        is allowed and the result is that the value will be wrong

        int charsRemaining = 3/*3 chars in a decimal representation*/;
        char value = (char)0;
        while( (inputBufferPosition < inputBufferSize) && (charsRemaining-- > 0) ) {
            final char currentChar = inputBuffer[inputBufferPosition++];

            value *= (char)10;
            value += (char)(currentChar - '0');
        }
        appendChar(value);
    }

    /**
     * Reads and appends the hex escaped text.
     */
    private void readHex() {
        // NOTE:  this *may* exit early if there are no enough characters. That
        //        is allowed and the result is that the value will be wrong

        int charsRemaining = 2/*2 chars in a hex representation*/;
        char value = (char)0;
        while( (inputBufferPosition < inputBufferSize) && (charsRemaining-- > 0) ) {
            final char currentChar = inputBuffer[inputBufferPosition++];

            value <<= 4;
            value += fromHex(currentChar);
        }
        appendChar(value);
    }

    // ========================================================================
    /**
     * @param  the the hex character (i.e. [a-fA-F0-9] to be converted into a
     *         Java character.
     * @return the Java ("decimal") character for the specified hex character.
     */
    private static char fromHex(final char hex) {
        if(hex >= 'a')
            return (char)(hex - 'a' + 10);
        else if(hex >= 'A')
            return (char)(hex - 'A' + 10);
        else
            return (char)(hex - '0');
    }

    // ************************************************************************
    // escaped characters for convenience 
    public static class Chars {
        public static final char DOUBLE_QUOTE = '"';
        public static final char SINGLE_QUOTE = '\'';

        public static final char COMMA = ',';
        public static final char POUND = '#';

        public static final char BACKSLASH = '\\';

        public static final char ALERT = '\u0007'/*'\a'*/;
        public static final char BACKSPACE = '\b';
        public static final char ESCAPE = '\u001B'/*'\e'*/;
        public static final char FORM_FEED = '\f';
        public static final char LF = '\n';
        public static final char CR = '\r';
        public static final char TAB = '\t';
        public static final char VERTICAL_TAB = '\u000B'/*'\v'*/;
    }

    // ========================================================================
    /**
     * The mode that defines how escaping occurs for the text qualifier.
     */
    public static enum EscapeMode {
        /**
         * Escape by doubling-up the text qualifier.
         */
        DOUBLED,

        /**
         * Escape using a backslash.
         */
        BACKSLASH
    }

    // ------------------------------------------------------------------------
    /** 
     * The strategy used for parsing.
     */
    public static class ParseStrategy {
        // a 'text qualifier' is how a column is denoted (e.g. "surrounded by quotes")
        public char textQualifier = Chars.DOUBLE_QUOTE;
        public boolean useTextQualifier = true/*default allow text qualifiers*/;
        public EscapeMode escapeMode = EscapeMode.DOUBLED;

        public boolean trimWhitespace = true/*default trim leading whitespace*/;

        // field delimiter
        public char delimiter = Chars.COMMA;

        public boolean useComments = false/*treat comments as text*/;
        public char commentChar = Chars.POUND;
    }
}