package snapcode.debug;

/**
 * A class to hold a bunch of Exceptions.
 */
public class Exceptions {

    public static class AmbiguousMethodException extends Exception {
        public AmbiguousMethodException()
        {
        }

        public AmbiguousMethodException(String s)
        {
            super(s);
        }
    }

    public static class FrameIndexOutOfBoundsException extends IndexOutOfBoundsException {
    }

    public static class LineNotFoundException extends Exception {
        public LineNotFoundException()
        {
        }

        public LineNotFoundException(String s)
        {
            super(s);
        }
    }

    public static class MalformedMemberNameException extends Exception {
        public MalformedMemberNameException()
        {
        }

        public MalformedMemberNameException(String s)
        {
            super(s);
        }
    }

//public static class MethodNotFoundException2 extends Exception { }

    public static class NoSessionException extends Exception {
    }

    public static class NoThreadException extends Exception {
    }

    public static class VMLaunchFailureException extends Exception {
    }

    public static class VMNotInterruptedException extends Exception {
    }

}