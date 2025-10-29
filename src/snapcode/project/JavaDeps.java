package snapcode.project;
import snap.util.ArrayUtils;
import snap.util.ActivityMonitor;
import snap.web.WebFile;

/**
 * This class handles //DEPS and //REPO directives in Java files.
 */
public class JavaDeps {

    /**
     * Resolve '//DEPS' dependencies for given java file.
     */
    public static void resolveDependenciesForFile(ActivityMonitor activityMonitor, WebFile javaFile)
    {
        // Get dependencies for file (just return if none)
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(javaFile);
        BuildDependency[] dependencies = findDependenciesForJavaString(javaAgent.getJavaTextString());
        if (dependencies.length == 0)
            return;

        Project project = javaAgent.getProject();
        BuildFile buildFile = project.getBuildFile();

        // Add dependencies to project build file
        for (BuildDependency dependency : dependencies) {

            // If already in list, skip
            if (buildFile.getDependencies().contains(dependency))
                continue;

            // If maven dependency, load first to see if valid
            if (dependency instanceof MavenDependency mavenDependency) {
                if (!mavenDependency.isLoaded()) {
                    if (activityMonitor != null)
                        activityMonitor.beginTask("Loading dependency: " + mavenDependency.getName(), 1);
                    mavenDependency.waitForLoad();
                    if (activityMonitor != null)
                        activityMonitor.endTask();
                    if (!mavenDependency.isLoaded())
                        continue;
                }
            }

            // Add dependency
            buildFile.addDependency(dependency);
            System.out.println("JavaDeps: Added build dependency: " + dependency.getId());
        }
    }

    /**
     * Returns the //DEPS dependencies for given string.
     */
    public static BuildDependency[] findDependenciesForJavaString(String javaText)
    {
        // Get lines that start with '//DEPS '
        String javaStr = javaText.length() > 500 ? javaText.substring(0, 500) : javaText;
        String[] lines = javaStr.split("\\r?\\n");
        String[] depLines = ArrayUtils.filter(lines, line -> line.startsWith("//DEPS "));
        return ArrayUtils.mapNonNull(depLines, depLine -> createDependencyForDepLine(depLine), BuildDependency.class);
    }

    /**
     * Creates a BuildDependency for given //DEPS string.
     */
    private static BuildDependency createDependencyForDepLine(String depLine)
    {
        String depStr = depLine.substring("//DEPS ".length());

        // If dep string is maven dependency, return it
        MavenDependency mavenDependency = new MavenDependency(depStr);
        if (mavenDependency.isResolved())
            return mavenDependency;

        // If dep string is jar file dependency, return it
        if (depStr.endsWith(".jar")) {
            BuildDependency.JarFileDependency jarDep = new BuildDependency.JarFileDependency();
            jarDep.setJarPath(depStr);
            if (jarDep.isResolved())
                return jarDep;
        }

        // Return invalid
        return null;
    }
}
