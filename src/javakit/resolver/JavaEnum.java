package javakit.resolver;

/**
 * This class emulates a Java Enum.
 */
public class JavaEnum {

    // The JavaClass
    private JavaClass _javaClass;

    // The name
    private String _name;

    /**
     * Constructor.
     */
    public JavaEnum(JavaClass aClass, String aName)
    {
        _javaClass = aClass;
        _name = aName;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()  { return _name; }
}
