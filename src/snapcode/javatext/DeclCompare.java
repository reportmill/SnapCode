package snapcode.javatext;
import javakit.resolver.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * A Comparator to sort JavaDecls.
 */
class DeclCompare implements Comparator<JavaDecl> {

    // The DeclMatcher match string
    private String _prefix;

    // Whether DeclMatcher prefix is literal pattern
    private boolean _literal;

    // The receiving class for completions
    private JavaClass _receivingClass;

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
        String fullName1 = decl1.getFullNameWithParameterTypes();
        String fullName2 = decl2.getFullNameWithParameterTypes();
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

        // If literal prefix match (case-sensitive), add bonus
        if (_literal && declName.startsWith(_prefix))
            rating += 5;

        // Handle Vars, Fields, Methods and Constructors: Add bonus
        if (aDecl instanceof JavaMember || aDecl instanceof JavaLocalVar) {

            // Members and local vars are probably most likely
            rating += 8;

            // If matching receiving class, add bonus
            if (_receivingClass != null) {
                JavaClass evalClass = aDecl.getEvalClass();
                if (_receivingClass.isAssignableFrom(evalClass))
                    rating += 5;
            }

            // If constructor for inner class, add penalty
            if (aDecl instanceof JavaConstructor) {
                JavaConstructor constructor = (JavaConstructor) aDecl;
                JavaClass evalClass = constructor.getEvalClass();
                int classRating = getMatchRatingForClass(evalClass);
                rating += classRating;
            }
        }

        // Handle Class: Add bonus for preferred packages or receiving class match
        else if (aDecl instanceof JavaClass) {
            int classRating = getMatchRatingForClass((JavaClass) aDecl);
            rating += classRating;
        }

        // Handle word: Add bonus
        else if (aDecl instanceof JavaWord && _receivingClass == null) {
            rating += 5;
        }

        // Additional chars make us less happy
        int additionalCharsCount = declName.length() - _prefix.length();
        rating -= additionalCharsCount;

        // Return
        return rating;
    }

    /**
     * Returns the match rating for a class.
     */
    private int getMatchRatingForClass(JavaClass javaClass)
    {
        // Get package
        JavaPackage pkg = javaClass.getPackage();
        String packageName = pkg != null ? pkg.getName() : "";

        // If root or common package, add bonus
        int rating = getPackageBonus(packageName);

        // If class is ReceivingClass, user might want cast
        if (javaClass == _receivingClass)
            rating += 5;

        // Inner class makes us less happy
        if (javaClass.isMemberClass())
            rating -= 2;

        // Return
        return rating;
    }

    /**
     * Returns a bonus for common packages.
     */
    private static int getPackageBonus(String packageName)
    {
        Integer packageBonus = _packageBonusMap.get(packageName);
        if (packageBonus != null)
            return packageBonus;

        // Return no bonus
        return 0;
    }

    /**
     * A map of package bonuses.
     */
    private static Map<String,Integer> _packageBonusMap = getPackageBonusMapImpl();
    private static Map<String,Integer> getPackageBonusMapImpl()
    {
        // "java.util", "java.lang", "java.io", "snap.view", "snap.gfx", "snap.geom", "snap.util"
        Map<String,Integer> map = new HashMap<>();
        map.put("", 6);
        map.put("java.lang", 6);
        map.put("java.util", 5);
        map.put("java.util.function", 4);
        map.put("java.util.stream", 5);
        map.put("java.io", 4);
        map.put("snap.view", 2);
        map.put("snap.gfx", 2);
        map.put("snap.geom", 2);
        map.put("snap.util", 2);
        return map;
    }
}
