package hudson.plugins.scm_sync_configuration.exceptions;

/**
 * @author fcamblor
 *         Exception which will be easily loggable, by providing both class and method called, causing the exception
 */
public class LoggableException extends RuntimeException {
    private final Class clazz;
    private final String methodName;

    public LoggableException(final String message, final Class clazz, final String methodName, final Throwable cause) {
        super(message, cause);
        this.clazz = clazz;
        this.methodName = methodName;
    }

    public LoggableException(final String message, final Class clazz, final String methodName) {
        super(message);
        this.clazz = clazz;
        this.methodName = methodName;
    }

    public Class getClazz() {
        return clazz;
    }

    public String getMethodName() {
        return methodName;
    }
}
