package javakit.resolver;
import snap.util.ArrayUtils;

/**
 * This JavaDecl subclass represents reserved words
 */
public class JavaWord extends JavaDecl {

    // The WordType
    private WordType  _wordType;

    // Constant for word type
    public enum WordType { Modifier, Declaration, Statement, Unknown }

    // Modifiers
    public static final JavaWord Public = new JavaWord("public", WordType.Modifier);
    public static final JavaWord Private = new JavaWord("private", WordType.Modifier);
    public static final JavaWord Protected = new JavaWord("protected", WordType.Modifier);
    public static final JavaWord Abstract = new JavaWord("abstract", WordType.Modifier);
    public static final JavaWord Default = new JavaWord("default", WordType.Modifier);
    public static final JavaWord Final = new JavaWord("final", WordType.Modifier);
    public static final JavaWord Static = new JavaWord("static", WordType.Modifier);

    // Declarations
    public static final JavaWord Class = new JavaWord("class", WordType.Declaration);
    public static final JavaWord Interface = new JavaWord("interface", WordType.Declaration);
    public static final JavaWord Enum = new JavaWord("enum", WordType.Declaration);
    public static final JavaWord Extends = new JavaWord("extends", WordType.Declaration);
    public static final JavaWord Implements = new JavaWord("implements", WordType.Declaration);
    public static final JavaWord Import = new JavaWord("import", WordType.Declaration);
    public static final JavaWord Package = new JavaWord("package", WordType.Declaration);

    // Statement words
    public static final JavaWord Assert = new JavaWord("assert", WordType.Statement);
    public static final JavaWord Break = new JavaWord("break", WordType.Statement);
    public static final JavaWord Case = new JavaWord("case", WordType.Statement);
    public static final JavaWord Catch = new JavaWord("catch", WordType.Statement);
    public static final JavaWord Continue = new JavaWord("continue", WordType.Statement);
    public static final JavaWord Do = new JavaWord("do", WordType.Statement);
    public static final JavaWord Else = new JavaWord("else", WordType.Statement);
    public static final JavaWord Finally = new JavaWord("finally", WordType.Statement);
    public static final JavaWord For = new JavaWord("for", WordType.Statement);
    public static final JavaWord If = new JavaWord("if", WordType.Statement);
    public static final JavaWord Instanceof = new JavaWord("instanceof", WordType.Statement);
    public static final JavaWord New = new JavaWord("new", WordType.Statement);
    public static final JavaWord Return = new JavaWord("return", WordType.Statement);
    public static final JavaWord Switch = new JavaWord("switch", WordType.Statement);
    public static final JavaWord Synchronized = new JavaWord("synchronized", WordType.Statement);
    public static final JavaWord Throw = new JavaWord("throw", WordType.Statement);
    public static final JavaWord Throws = new JavaWord("throws", WordType.Statement);
    public static final JavaWord Try = new JavaWord("try", WordType.Statement);
    public static final JavaWord While = new JavaWord("while", WordType.Statement);

    // All
    public static final JavaWord[] ALL = {
            Public, Private, Protected, Abstract, Default, Final, Static,
            Class, Interface, Enum, Extends, Implements, Import, Package,
            Assert, Break, Case, Catch, Continue, Do, Else, Finally, For, If, Instanceof, New, Return,
            Switch, Synchronized, Throw, Throws, Try, While
    };

    /**
     * Constructor.
     */
    public JavaWord(String aName, WordType aWordType)
    {
        super(null, DeclType.Word);
        _id = _name = _simpleName = aName;
        _wordType = aWordType;
    }

    /**
     * Override.
     */
    @Override
    public String getSuggestionString()
    {
        // Get full name
        String fullNameStr = getFullName();
        String suggestionStr = fullNameStr;

        // Get descriptor string
        switch (_wordType) {
            case Modifier: suggestionStr += " - modifier"; break;
            case Declaration: suggestionStr += " - declaration word"; break;
            case Statement: suggestionStr += " - statement word"; break;
        }

        // Return
        return suggestionStr;
    }

    /**
     * Override to add parens to some statement words.
     */
    @Override
    public String getReplaceString()
    {
        String superStr = super.getReplaceString();
        if (wantsParens())
            superStr += " ()";
        return superStr;
    }

    /**
     * Returns whether word wants parens.
     */
    private boolean wantsParens()
    {
        return _wordType == WordType.Statement && ArrayUtils.containsId(WANTS_PARENS, this);
    }

    // Array of JavaWords that want parens
    private static final JavaWord[] WANTS_PARENS = { If, For, While, Assert, Switch };
}
