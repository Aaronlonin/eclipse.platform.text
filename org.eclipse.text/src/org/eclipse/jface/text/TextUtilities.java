/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jface.text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;


/**
 * Collection of text functions.
 */
public class TextUtilities {
	
	/**
	 * Default line delimiters used by these text functons.
	 */
	public final static String[] DELIMITERS= new String[] { "\n", "\r", "\r\n" }; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
	
	/**
	 * Default line delimiters used by these text functions.
	 * 
	 * @deprecated use DELIMITERS instead
	 */
	public final static String[] fgDelimiters= DELIMITERS;
	
	
	
	/**
	 * Determines which one of default line delimiters appears first in the list. If none of them the
	 * hint is returned.
	 * 
	 * @param text the text to be checked
	 * @param hint the line delimiter hint
	 */
	public static String determineLineDelimiter(String text, String hint) {
		try {
			int[] info= indexOf(DELIMITERS, text, 0);
			return DELIMITERS[info[1]];
		} catch (ArrayIndexOutOfBoundsException x) {
		}
		return hint;
	}
	
	/**
	 * Returns the starting position and the index of the longest matching search string
	 * in the given text that is greater than the given offset. Returns <code>[-1, -1]</code>
	 * if no match can be found.
	 * 
	 * @param searchStrings the strings to search for
	 * @param text the text to be searched
	 * @param offset the offset at which to start the search
	 * @return an <code>int[]</code> with two elements" the first is the starting offset, the second the index of the found
	 * 		search string in the given <code>searchStrings</code> array, returns <code>[-1, -1]</code> if no match exists
	 */
	public static int[] indexOf(String[] searchStrings, String text, int offset) {
		
		int[] result= { -1, -1 };
		int zeroIndex= -1;
		
		for (int i= 0; i < searchStrings.length; i++) {
			
			int length= searchStrings[i].length();
			
			if (length == 0) {
				zeroIndex= i;
				continue;
			}
			
			int index= text.indexOf(searchStrings[i], offset);
			if (index >= 0) {
				
				if (result[0] == -1) {
					result[0]= index;
					result[1]= i;
				} else if (index < result[0]) {
					result[0]= index;
					result[1]= i;
				} else if (index == result[0] && length > searchStrings[result[1]].length()) {
					result[0]= index;
					result[1]= i;
				}
			}
		}
		
		if (zeroIndex > -1 && result[0] == -1) {
			result[0]= 0;
			result[1]= zeroIndex;
		}
		
		return result;
	}
	
	/**
	 * Returns the index of the longest search string with which the given text ends or
	 * <code>-1</code> if none matches.
	 * 
	 * @param searchStrings the strings to search for
	 * @param text the text to search
	 * @return the index in <code>searchStrings</code> of the longest string with which <code>text</code> ends or <code>-1</code>
	 */
	public static int endsWith(String[] searchStrings, String text) {
		
		int index= -1;
		
		for (int i= 0; i < searchStrings.length; i++) {
			if (text.endsWith(searchStrings[i])) {
				if (index == -1 || searchStrings[i].length() > searchStrings[index].length())
					index= i;
			}
		}
		
		return index;
	}
	
	/**
	 * Returns the index of the longest search string with which the given text starts or <code>-1</code>
	 * if none matches.
	 * 
	 * @param searchStrings the strings to search for
	 * @param text the text to search
	 * @return the index in <code>searchStrings</code> of the longest string with which <code>text</code> starts or <code>-1</code>
	 */
	public static int startsWith(String[] searchStrings, String text) {
		
		int index= -1;
		
		for (int i= 0; i < searchStrings.length; i++) {
			if (text.startsWith(searchStrings[i])) {
				if (index == -1 || searchStrings[i].length() > searchStrings[index].length())
					index= i;
			}
		}
		
		return index;
	}
	
	/**
	 * Returns the index of the first compare string that equals the given text or <code>-1</code> 
	 * if none is equal.
	 * 
	 * @param compareStrings the strings to compare with
	 * @param text the text to check
	 * @return the index of the first equal compare string or <code>-1</code>
	 */
	public static int equals(String[] compareStrings, String text) {
		for (int i= 0; i < compareStrings.length; i++) {
			if (text.equals(compareStrings[i]))
				return i;
		}
		return -1;
	}
	
	/**
	 * Returns a document event which is an accumulation of a list of document events,
	 * <code>null</code> if the list of documentEvents is empty.
	 * The document of the document events are ignored.
	 * 
	 * @param unprocessedDocument the document to which the document events would be applied
	 * @param documentEvents the list of document events to merge
	 * @return returns the merged document event
	 * @throws BadLocationException might be thrown if document is not in the correct state with respect to document events
	 */
	public static DocumentEvent mergeUnprocessedDocumentEvents(IDocument unprocessedDocument, List documentEvents) throws BadLocationException {

		if (documentEvents.size() == 0)
			return null;

		final Iterator iterator= documentEvents.iterator();
		final DocumentEvent firstEvent= (DocumentEvent) iterator.next();

		// current merged event
		final IDocument document= unprocessedDocument;		
		int offset= firstEvent.getOffset();
		int length= firstEvent.getLength();
		final StringBuffer text= new StringBuffer(firstEvent.getText() == null ? "" : firstEvent.getText()); //$NON-NLS-1$

		while (iterator.hasNext()) {

			final int delta= text.length() - length;

			final DocumentEvent event= (DocumentEvent) iterator.next();
			final int eventOffset= event.getOffset();
			final int eventLength= event.getLength();
			final String eventText= event.getText() == null ? "" : event.getText(); //$NON-NLS-1$
			
			// event is right from merged event
			if (eventOffset > offset + length + delta) {
				final String string= document.get(offset + length, (eventOffset - delta) - (offset + length));
				text.append(string);
				text.append(eventText);
				
				length= (eventOffset - delta) + eventLength - offset;
				
			// event is left from merged event
			} else if (eventOffset + eventLength < offset) {
				final String string= document.get(eventOffset + eventLength, offset - (eventOffset + eventLength));
				text.insert(0, string);
				text.insert(0, eventText);
				
				length= offset + length - eventOffset;
				offset= eventOffset;
			
			// events overlap eachother				
			} else {
				final int start= Math.max(0, eventOffset - offset);
				final int end= Math.min(text.length(), eventLength + eventOffset - offset);
				text.replace(start, end, eventText);

				offset= Math.min(offset, eventOffset);
				final int totalDelta= delta + eventText.length() - eventLength;
				length= text.length() - totalDelta; 
			}
		}		

		return new DocumentEvent(document, offset, length, text.toString());
	}

	/**
	 * Returns a document event which is an accumulation of a list of document events,
	 * <code>null</code> if the list of document events is empty.
	 * The document events being merged must all refer to the same document, to which
	 * the document changes have been already applied.
	 * 
	 * @param documentEvents the list of document events to merge
	 * @return returns the merged document event
	 * @throws BadLocationException might be thrown if document is not in the correct state with respect to document events
	 */
	public static DocumentEvent mergeProcessedDocumentEvents(List documentEvents) throws BadLocationException {

		if (documentEvents.size() == 0)
			return null;

		final ListIterator iterator= documentEvents.listIterator(documentEvents.size());
		final DocumentEvent firstEvent= (DocumentEvent) iterator.previous();

		// current merged event
		final IDocument document= firstEvent.getDocument();		
		int offset= firstEvent.getOffset();
		int length= firstEvent.getLength();
		int textLength= firstEvent.getText() == null ? 0 : firstEvent.getText().length();

		while (iterator.hasPrevious()) {

			final int delta= length - textLength;

			final DocumentEvent event= (DocumentEvent) iterator.previous();
			final int eventOffset= event.getOffset();
			final int eventLength= event.getLength();
			final int eventTextLength= event.getText() == null ? 0 : event.getText().length();

			// event is right from merged event
			if (eventOffset > offset + textLength + delta) {
				length= (eventOffset - delta) - (offset + textLength) + length + eventLength;				
				textLength= (eventOffset - delta) + eventTextLength - offset; 

			// event is left from merged event
			} else if (eventOffset + eventTextLength < offset) {
				length= offset - (eventOffset + eventTextLength) + length + eventLength;
				textLength= offset + textLength - eventOffset;
				offset= eventOffset;

			// events overlap eachother				
			} else {
				final int start= Math.max(0, eventOffset - offset);
				final int end= Math.min(length, eventTextLength + eventOffset - offset);
				length += eventLength - (end - start);
				
				offset= Math.min(offset, eventOffset);
				final int totalDelta= delta + eventLength - eventTextLength;
				textLength= length - totalDelta;
			}				
		}
		
		final String text= document.get(offset, textLength);
		return new DocumentEvent(document, offset, length, text);
	}
	
	/**
	 * Removes all connected document partitioners from the given document and stores them
	 * under their partitioning name in a map. This map is returned. After this method has been called
	 * the given document is no longer connected to any document partitioner.
	 * 
	 * @param document the document
	 * @return the map containing the removed partitioners
	 */
	public static Map removeDocumentPartitioners(IDocument document) {
		Map partitioners= new HashMap();
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) document;
			String[] partitionings= extension3.getPartitionings();
			for (int i= 0; i < partitionings.length; i++) {
				IDocumentPartitioner partitioner= extension3.getDocumentPartitioner(partitionings[i]);
				if (partitioner != null) {
					extension3.setDocumentPartitioner(partitionings[i], null);
					partitioner.disconnect();
					partitioners.put(partitionings[i], partitioner);
				}
			}
		} else {
			IDocumentPartitioner partitioner= document.getDocumentPartitioner();
			if (partitioner != null) {
				document.setDocumentPartitioner(null);
				partitioner.disconnect();
				partitioners.put(IDocumentExtension3.DEFAULT_PARTITIONING, partitioner);
			}
		}
		return partitioners;
	}
	
	/**
	 * Connects the given document with all document partitioners stored in the given map under
	 * their partitioning name. This method cleans the given map.
	 * 
	 * @param document the document
	 * @param partitioners the map containing the partitioners to be connected
	 * @since 3.0
	 */
	public static void addDocumentPartitioners(IDocument document, Map partitioners) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) document;
			Iterator e= partitioners.keySet().iterator();
			while (e.hasNext()) {
				String partitioning= (String) e.next();
				IDocumentPartitioner partitioner= (IDocumentPartitioner) partitioners.get(partitioning);
				partitioner.connect(document);
				extension3.setDocumentPartitioner(partitioning, partitioner);
			}
			partitioners.clear();
		} else {
			IDocumentPartitioner partitioner= (IDocumentPartitioner) partitioners.get(IDocumentExtension3.DEFAULT_PARTITIONING);
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
	}
	
	/**
	 * Returns the content type at the given offset of the given document.
	 * 
	 * @param document the document
	 * @param partitioning the partitioning to be used
	 * @param offset the offset
	 * @return the content type at the given offset of the document
	 * @throws BadLocationException if offset is invalid in the document
	 * @since 3.0
	 */
	public static String getContentType(IDocument document, String partitioning, int offset) throws BadLocationException {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) document;
			try {
				return extension3.getContentType(partitioning, offset);
			} catch (BadPartitioningException x) {
				return IDocument.DEFAULT_CONTENT_TYPE;
			}
		} else {
			return document.getContentType(offset);
		}		
	}
	
	/**
	 * Returns the partition of the given offset of the given document.
	 * 
	 * @param document the document
	 * @param partitioning the partitioning to be used
	 * @param offset the offset
	 * @return the content type at the given offset of this viewer's input document
	 * @throws BadLocationException if offset is invalid in the given document
	 * @since 3.0
	 */
	public static ITypedRegion getPartition(IDocument document, String partitioning, int offset) throws BadLocationException {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) document;
			try {
				return extension3.getPartition(partitioning, offset);
			} catch (BadPartitioningException x) {
				return new TypedRegion(0, document.getLength(), IDocument.DEFAULT_CONTENT_TYPE);
			}
		} else {
			return document.getPartition(offset);
		}	
	}

	/**
	 * Computes and returns the partitioning for the given region of the given document for the given partitioning name.
	 * 
	 * @param document the document
	 * @param partitioning the partitioning name
	 * @param offset the region offset
	 * @param length the region length
	 * @return  the partitioning for the given region of the given document for the given partitioning name
	 * @throws BadLocationException if the given region is invalid for the given document
	 * @since 3.0
	 */
	public static ITypedRegion[] computePartitioning(IDocument document, String partitioning, int offset, int length) throws BadLocationException {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) document;
			try {
				return extension3.computePartitioning(partitioning, offset, length);
			} catch (BadPartitioningException x) {
				return new ITypedRegion[0];
			}
		} else {
			return document.computePartitioning(offset, length);
		}
	}
	
	/**
	 * Computes and returns the partition managing position categories for the
	 * given document or <code>null</code> if this was impossible.
	 * 
	 * @param document the document
	 * @return the partition managing position categories
	 * @since 3.0
	 */
	public static String[] computePartitionManagingCategories(IDocument document) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) document;
			String[] partitionings= extension3.getPartitionings();
			if (partitionings != null) {
				Set categories= new HashSet(); 
				for (int i= 0; i < partitionings.length; i++) {
					IDocumentPartitioner p= extension3.getDocumentPartitioner(partitionings[i]);
					if (p instanceof IDocumentPartitionerExtension2) {
						IDocumentPartitionerExtension2 extension2= (IDocumentPartitionerExtension2) p;
						String[] c= extension2.getManagingPositionCategories();
						if (c != null) {
							for (int j= 0; j < c.length; j++) 
								categories.add(c[j]);
						}
					}
				}
				String[] result= new String[categories.size()];
				categories.toArray(result);
				return result;
			}
		}
		return null;
	}
	
	/**
	 * Returns the default line delimiter for the given document. This is either the delimiter of the first line, or the platform line delimiter if it is
	 * a legal line delimiter or the first one of the legal line delimiters. The default line delimiter should be used when performing document
	 * manipulations that span multiple lines.
	 * 
	 * @param document the document
	 * @return the document's default line delimiter
	 * @since 3.0
	 */
	public static String getDefaultLineDelimiter(IDocument document) {
		
		String lineDelimiter= null;
		
		try {
			lineDelimiter= document.getLineDelimiter(0);
		} catch (BadLocationException x) {
		}
			
		if (lineDelimiter == null) {
			String sysLineDelimiter= System.getProperty("line.separator"); //$NON-NLS-1$
			String[] delimiters= document.getLegalLineDelimiters();
			Assert.isTrue(delimiters.length > 0);
			for (int i= 0; i < delimiters.length; i++) {
				if (delimiters[i].equals(sysLineDelimiter)) {
					lineDelimiter= sysLineDelimiter;
					break;
				}
			}
				
			if (lineDelimiter == null)
				lineDelimiter= delimiters[0];
		}
		
		return lineDelimiter;
	}
}
