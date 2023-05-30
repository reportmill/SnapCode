package snapcode.javatext;
import javakit.resolver.*;
import snap.util.ArrayUtils;

import java.util.Comparator;

/**
 * A Comparator to sort JavaDecls.
 */
class DeclCompare implements Comparator<JavaDecl> {

    // The DeclMatcher match string
    private String  _prefix;

    // Whether DeclMatcher prefix is literal pattern
    private boolean  _literal;

    // The receiving class for completions
    private JavaClass  _receivingClass;

    /**
     * Creates a DeclCompare.
     */
    DeclCompare(DeclMatcher aDeclMatcher, JavaClass aRecClass)
    {
        _receivingClass = aRecClass;
        _prefix = aDeclMatcher.getPrefix();
        _literal = _prefix.length() == aDeclMatcher.getMatcher().pattern().pattern().length();
    }

    /**
     * Standard compare to method.
     */
    public int compare(JavaDecl decl1, JavaDecl decl2)
    {
        // Get ratings
        int rating1 = getMatchRating(decl1);
        int rating2 = getMatchRating(decl2);
        if (rating1 != rating2)
            return rating1 > rating2 ? -1 : 1;

        // If simple names are unique, return order
        String simpleName1 = decl1.getSimpleName();
        String simpleName2 = decl2.getSimpleName();
        int simpleNameComp = simpleName1.compareToIgnoreCase(simpleName2);
        if (simpleNameComp != 0)
            return simpleNameComp;

        // Otherwise use full name
        String fullName1 = decl1.getFullName();
        String fullName2 = decl2.getFullName();
        return fullName1.compareToIgnoreCase(fullName2);
    }

    /**
     * Returns the match rating for a decl.
     */
    public int getMatchRating(JavaDecl aDecl)
    {
        // Get name
        String declName = aDecl.getSimpleName();
        int rating = 0;

        // Literal prefix match (case sensitive) makes us more happy
        if (_literal && declName.startsWith(_prefix))
            rating += 2;

        // Vars, Fields, Methods and Constructors make us more happy
        if (aDecl instanceof JavaExecutable || aDecl instanceof JavaLocalVar) {

            // Increase rating
            rating += 5;

            // Matching receiving class makes us more happy
            if (_receivingClass != null) {
                JavaType evalType = aDecl.getEvalType();
                if (_receivingClass.isAssignable(evalType))
                    rating += 5;
            }

            // Constructor for inner class makes us less happy
            if (aDecl instanceof JavaConstructor) {
                JavaConstructor constructor = (JavaConstructor) aDecl;
                JavaClass evalClass = constructor.getEvalClass();
                if (evalClass.isMemberClass())
                    rating -= 2;
            }
        }

        // Class in pref package makes us more happy
        else if (aDecl instanceof JavaClass) {

            // Get package
            JavaClass javaClass = (JavaClass) aDecl;
            JavaPackage pkg = javaClass.getPackage();

            // Handle root package (primitive classes mostly)
            if (pkg == null)
                rating += 5;

                // Handle preferred packages
            else {
                String pkgName = pkg.getName();
                if (ArrayUtils.contains(PREF_PACKAGES, pkgName))
                    rating += 5;
                else if (pkgName.startsWith("java.util.") || pkgName.startsWith("java.lang."))
                    rating += 2;
            }

            // If class is ReceivingClass, user might want cast
            if (javaClass == _receivingClass)
                rating += 5;

            // Inner class makes us less happy
            if (javaClass.isMemberClass())
                rating -= 2;
        }

        // Additional chars make us less happy
        int additionalCharsCount = declName.length() - _prefix.length();
        rating -= additionalCharsCount;

        // Return
        return rating;
    }

    // Constant for above
    private static String PREF_PACKAGES[] = { "java.lang", "java.util", "snap.view", "snapcode.app" };
}
