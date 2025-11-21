package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaType;
import java.util.ArrayList;
import java.util.List;

/**
 * A type that is a union of multiple types. Used for catch clause: catch (Exception1 | Exception2 e) { ... }
 */
public class JTypeUnion extends JType {

    // The first type
    private JType _firstType;

    // All types
    private List<JType> _allTypes = new ArrayList<>();

    /**
     * Constructor.
     */
    public JTypeUnion()
    {
        super();
    }

    /**
     * Adds a type.
     */
    public void addType(JType aType)
    {
        if (_allTypes.isEmpty())
            _firstType = aType;
        _allTypes.add(aType);
        addChild(aType);
    }

    /**
     * Returns the base expression.
     */
    @Override
    public JExpr getBaseExpr()  { return _firstType.getBaseExpr(); }

    /**
     * Returns the name.
     */
    @Override
    public String getName()  { return _firstType.getName(); }

    /**
     * Returns the simple name.
     */
    @Override
    public String getSimpleName()  { return _firstType.getSimpleName(); }

    /**
     * Returns the base type.
     */
    @Override
    protected JavaType getBaseType()  { return _firstType.getBaseType(); }

    /**
     * Returns the base type.
     */
    @Override
    protected JavaClass getBaseClass()  { return _firstType.getBaseClass(); }

    /**
     * Returns the JavaType.
     */
    @Override
    public JavaType getJavaType()  { return _firstType.getJavaType(); }

    /**
     * Returns the JavaClass.
     */
    @Override
    public JavaClass getJavaClass()  { return _firstType.getJavaClass(); }

    /**
     * Adds a type to another type, returning the union type.
     */
    public static JTypeUnion addTypeToType(JType type1, JType type2)
    {
        if (type1 instanceof JTypeUnion unionType) {
            unionType.addType(type2);
            return unionType;
        }

        JTypeUnion unionType = new JTypeUnion();
        unionType.addType(type1);
        unionType.addType(type2);
        return unionType;
    }
}
