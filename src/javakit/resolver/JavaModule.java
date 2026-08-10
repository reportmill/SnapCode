package javakit.resolver;
import snap.util.ListUtils;
import java.util.*;

/**
 * This class represents a Java Module.
 */
public class JavaModule extends JavaDecl {

    // The child packages
    protected List<JavaPackage> _packages;

    /**
     * Constructor.
     */
    public JavaModule(Resolver aResolver, String moduleName)
    {
        super(aResolver, DeclType.Module);

        // Set Name, SimpleName
        _id = _name = _simpleName = moduleName;
    }

    /**
     * Returns the child packages.
     */
    public List<JavaPackage> getPackages()
    {
        if (_packages != null) return _packages;

        String moduleName = getName();
        if (moduleName.equals("java"))
            return _packages = Collections.emptyList();

        // Lookup the module in the boot layer
        Module module = ModuleLayer.boot().findModule(moduleName).orElse(null);
        assert module != null;
        Set<String> packageNames = module.getPackages();

        // Return packages for names (some might be null due to exclusions)
        return _packages = ListUtils.mapNonNull(packageNames, _resolver::getJavaPackageForName);
    }

    /**
     * Returns the child for name.
     */
    public JavaModule getChildForName(String childName)
    {
        String moduleName = getName() + '.' + childName;
        return _resolver.isKnownModuleName(moduleName) ? _resolver.getJavaModuleForName(moduleName) : null;
    }

    /**
     * Returns a package for given simple class name.
     */
    public JavaPackage getPackageForSimpleClassName(String simpleClassName)
    {
        for (JavaPackage pkg : getPackages()) {
            if (pkg.getChildForName(simpleClassName) instanceof JavaClass)
                return pkg;
        }
        return null;
    }

    /**
     * Returns a full class name for given simple class name.
     */
    public String getClassNameForSimpleClassName(String simpleClassName)
    {
        JavaPackage pkg = getPackageForSimpleClassName(simpleClassName);
        return pkg != null ? pkg.getName() + '.' + simpleClassName : null;
    }
}
