package org.apache.james.experimental.imapserver;

import org.apache.avalon.framework.logger.Logger;

public class MockLogger implements Logger {

	public void debug(String arg0) {}

	public void debug(String arg0, Throwable arg1) {
	}

	public void error(String arg0) {
	}

	public void error(String arg0, Throwable arg1) {
	}

	public void fatalError(String arg0) {
	}

	public void fatalError(String arg0, Throwable arg1) {
	}

	public Logger getChildLogger(String arg0) {
		return this;
	}

	public void info(String arg0) {
	}

	public void info(String arg0, Throwable arg1) {
	}

	public boolean isDebugEnabled() {
		return false;
	}

	public boolean isErrorEnabled() {
		return false;
	}

	public boolean isFatalErrorEnabled() {
		return false;
	}

	public boolean isInfoEnabled() {
		return false;
	}

	public boolean isWarnEnabled() {
		return false;
	}

	public void warn(String arg0) {
	}

	public void warn(String arg0, Throwable arg1) {
	}

}
