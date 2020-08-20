package org.jboss.tools.quarkus.lsp4e.internal;

import java.util.function.Function;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jboss.tools.quarkus.lsp4e.MicroProfileLSPPlugin;

/**
 * This class is a copy/paste of JDT LS
 * https://github.com/eclipse/eclipse.jdt.ls/blob/master/org.eclipse.jdt.ls.core/src/org/eclipse/jdt/ls/core/internal/handlers/JsonRpcHelpers.java
 * with only required method for Quarkus LSP4E.
 *
 */
public class JsonRpcHelpers {

	/**
	 * Convert line, column to a document offset.
	 *
	 * @param openable
	 * @param line
	 * @param column
	 * @return
	 */
	public static int toOffset(IOpenable openable, int line, int column) {
		if (openable != null) {
			try {
				return convert(openable, (IDocument document) -> toOffset(document, line, column));
			} catch (JavaModelException e) {
//				JavaLanguageServerPlugin.log(e);
			}
		}

		return -1;
	}

	/**
	 * Convert line, column to a document offset.
	 *
	 * @param buffer
	 * @param line
	 * @param column
	 * @return
	 */
	public static int toOffset(IBuffer buffer, int line, int column) {
		if (buffer != null) {
			return toOffset(toDocument(buffer), line, column);
		}
		return -1;
	}

	/**
	 * Convert line, column to a document offset.
	 *
	 * @param document
	 * @param line
	 * @param column
	 * @return
	 */
	public static int toOffset(IDocument document, int line, int column) {
		if (document != null) {
			try {
				return document.getLineOffset(line) + column;
			} catch (BadLocationException e) {
				// JavaLanguageServerPlugin.logException(e.getMessage(), e);
			}
		}
		return -1;
	}

	/**
	 * Convert offset to line number and column.
	 * 
	 * @param buffer
	 * @param line
	 * @param column
	 * @return
	 */
	public static int[] toLine(IBuffer buffer, int offset) {
		return toLine(toDocument(buffer), offset);
	}

	/**
	 * Convert the document offset to line number and column.
	 *
	 * @param document
	 * @param line
	 * @return
	 */
	public static int[] toLine(IDocument document, int offset) {
		try {
			int line = document.getLineOfOffset(offset);
			int column = offset - document.getLineOffset(line);
			return new int[] { line, column };
		} catch (BadLocationException e) {
			MicroProfileLSPPlugin.logException(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Returns an {@link IDocument} for the given buffer. The implementation tries
	 * to avoid copying the buffer unless required. The returned document may or may
	 * not be connected to the buffer.
	 *
	 * @param buffer a buffer
	 * @return a document with the same contents as the buffer or <code>null</code>
	 *         is the buffer is <code>null</code>
	 */
	public static IDocument toDocument(IBuffer buffer) {
		if (buffer == null) {
			return null;
		}
		if (buffer instanceof IDocument) {
			return (IDocument) buffer;
		} else if (buffer instanceof DocumentAdapter) {
			IDocument document = ((DocumentAdapter) buffer).getDocument();
			if (document != null) {
				return document;
			}
		}
		return new org.eclipse.jdt.internal.core.DocumentAdapter(buffer);
	}

	private static <T> T convert(IOpenable openable, Function<IDocument, T> consumer) throws JavaModelException {
		Assert.isNotNull(openable, "openable");
		boolean mustClose = false;
		try {
			if (!openable.isOpen()) {
				openable.open(new NullProgressMonitor());
				mustClose = openable.isOpen();
			}
			IBuffer buffer = openable.getBuffer();
			return consumer.apply(toDocument(buffer));
		} finally {
			if (mustClose) {
				try {
					openable.close();
				} catch (JavaModelException e) {
//					JavaLanguageServerPlugin.logException("Error when closing openable: " + openable, e);
				}
			}
		}
	}

}
