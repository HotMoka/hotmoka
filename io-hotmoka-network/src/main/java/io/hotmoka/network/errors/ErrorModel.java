package io.hotmoka.network.errors;

import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;

/**
 * The model of an exception thrown by a REST method.
 */
public class ErrorModel {

	/**
	 * The message of the exception.
	 */
	public String message;

	/**
	 * The fully-qualified name of the class of the exception.
	 */
	public String exceptionClassName;

	/**
	 * Builds the model of an exception thrown by a REST method.
	 * 
	 * @param message the message of the exception
	 * @param exceptionClass the class of the exception
	 */
    public ErrorModel(String message, Class<? extends Exception> exceptionClass) {
        this.message = message;
        this.exceptionClassName = abstractName(exceptionClass);
    }

    public ErrorModel() {}

    /**
     * Abstracts an exception class to the name of its superclass as declared in the methods
     * of the nodes. This avoids different results for nodes that throw subclasses
     * of the declared exceptions.
     * 
     * @param exceptionClass the class
     * @return the abstracted name of the exception class
     */
    private static String abstractName(Class<? extends Exception> exceptionClass) {
    	if (TransactionException.class.isAssignableFrom(exceptionClass))
    		return TransactionException.class.getName();
    	else if (TransactionRejectedException.class.isAssignableFrom(exceptionClass))
    		return TransactionRejectedException.class.getName();
    	else if (CodeExecutionException.class.isAssignableFrom(exceptionClass))
    		return CodeExecutionException.class.getName();
    	else if (NoSuchElementException.class.isAssignableFrom(exceptionClass))
    		return NoSuchElementException.class.getName();
    	else if (NoSuchAlgorithmException.class.isAssignableFrom(exceptionClass))
    		return NoSuchAlgorithmException.class.getName();
    	else if (InterruptedException.class.isAssignableFrom(exceptionClass))
    		return InterruptedException.class.getName();
    	else if (TimeoutException.class.isAssignableFrom(exceptionClass))
    		return TimeoutException.class.getName();
    	else
    		return exceptionClass.getName();
	}

	/**
     * Builds the model of an exception thrown by a REST method.
     *
     * @param e the exception
     */
    public ErrorModel(Exception e) {
        this(e.getMessage() != null ? e.getMessage() : "", e.getClass());
    }
}