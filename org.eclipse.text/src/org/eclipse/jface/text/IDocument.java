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


/**
 * An <code>IDocument</code> represents text providing support for 
 * <ul>
 * <li> text manipulation
 * <li> positions
 * <li> partitions
 * <li> line information
 * <li> search
 * <li> document change listeners
 * <li> document partition change listeners
 * </ul>
 *
 * A document allows to set its content and to manipulate it. For manipulation
 * a document provides the <code>replace</code> method which substitutes a given
 * string for a specified text range in the document. On each document change, all
 * registered document listeners are informed exactly once.
 *
 * Positions are stickers to the document's text that are updated when the
 * document is changed. Positions are updated by <code>IPositionUpdater</code>s. Position 
 * updaters are managed as a list. The list defines the sequence in which position 
 * updaters are invoked.This way, position updaters may rely on each other. 
 * Positions are grouped into categories.  A category is a ordered list of positions.
 * the document defines the order of position in a category based on the position's offset
 * based on the implementation of the method <code>computeIndexInCategory</code>.
 * Each document must support a default position category whose name is specified by this
 * interface.<p>
 *
 * A document can be considered consisting of a sequence of not overlapping partitions.
 * A partition is defined by its offset, its length, and its type. Partitions are 
 * updated on every document manipulation and ensured to be up-to-date when the document
 * listeners are informed. A document uses an <code>IDocumentPartitioner</code> to 
 * manage its partitions. A document may be unpartitioned which happens when there is no
 * partitioner. In this case, the document is considered as one single partition of a
 * default type. The default type is specified by this interface. If a document change
 * changes the document's partitioning all registered partitioning listeners are
 * informed exactly once.<p>
 * 
 * An <code>IDocument</code> provides methods to map line numbers and character 
 * positions onto each other based on the document's line delimiters. When moving text 
 * between documents using different line delimiters, the text must be converted to 
 * use the target document's line delimiters. <p>
 * 
 * <code>IDocument</code> throws <code>BadLocationException</code> if the parameters of
 * queries or manipulation requests are not inside the bounds of the document. The purpose
 * of this style of exception handling is
 * <ul>
 * <li> prepare document for multi-thread access
 * <li> allow clients to implement backtracking recovery methods
 * <li> prevent clients from upfront contract checking when dealing with documents.
 * </ul>
 * Clients may implement this interface or use the default implementation provided
 * by <code>AbstractDocument</code> and <code>Document</code>.
 *
 * @see Position
 * @see IPositionUpdater
 * @see IDocumentPartitioner
 * @see ILineTracker
 * @see IDocumentListener
 * @see IDocumentPartitioningListener
 */
public interface IDocument {
	
	
	/**
	 * The identifier of the default position category.
	 */
	final static String DEFAULT_CATEGORY= "__dflt_position_category"; //$NON-NLS-1$
	
	/**
	 * The identifier of the default partition content type.
	 */
	final static String DEFAULT_CONTENT_TYPE= "__dftl_partition_content_type"; //$NON-NLS-1$
	
	
	
	
	/* --------------- text access and manipulation --------------------------- */
	
	/**
	 * Returns the character at the given document offset in this document.
	 *
	 * @param offset a document offset
	 * @return the character at the offset
	 * @exception BadLocationException if the offset is invalid in this document
	 */
	char getChar(int offset) throws BadLocationException;
	
	/**
	 * Returns the number of characters in this document.
	 *
	 * @return the number of characters in this document
	 */
	int getLength();
	
	/**
	 * Returns this document's complete text.
	 *
	 * @return the document's complete text
	 */
	String get();
	
	/**
	 * Returns this document's text for the specified range.
	 *
	 * @param offset the document offset
	 * @param length the length of the specified range
	 * @return the document's text for the specified range
	 * @exception BadLocationException if the range is invalid in this document
	 */
	String get(int offset, int length) throws BadLocationException;
	
	/**
	 * Replaces the content of the document with the given text.
	 * Sends a <code>DocumentEvent</code> to all registered <code>IDocumentListener</code>.
	 * This method is a convenience method for <code>
	 * replace(0, getLength(), text)</code>.
	 *
	 * @param text the new content of the document
	 *
	 * @see DocumentEvent
	 * @see IDocumentListener
	 */
	void set(String text);
		
	/**
	 * Subsitutes the given text for the specified document range.
	 * Sends a <code>DocumentEvent</code> to all registered <code>IDocumentListener</code>.
	 *
	 * @param offset the document offset
	 * @param length the length of the specified range
	 * @param text the substitution text
	 * @exception BadLocationException if the offset is invalid in this document
	 *
	 * @see DocumentEvent
	 * @see IDocumentListener
	 */
	void replace(int offset, int length, String text) throws BadLocationException;
		
	/**
	 * Registers the document listener with the document. After registration
	 * the IDocumentListener is informed about each change of this document.
	 * If the listener is already registered nothing happens.<p>
	 * An <code>IDocumentListener</code> may call back to this method
	 * when being inside a document notification. 
	 *
	 * @param listener the listener to be registered
	 */
	void addDocumentListener(IDocumentListener listener);
	
	/**
	 * Removes the listener from the document's list of document listeners.
	 * If the listener is not registered with the document nothing happens.<p>
	 * An <code>IDocumentListener</code> may call back to this method
	 * when being inside a document notification. 
	 *
	 * @param listener the listener to be removed
	 */
	void removeDocumentListener(IDocumentListener listener);
	
	/**
	 * Adds the given document listener as one which is notified before
	 * those document listeners added with <code>addDocumentListener</code>
	 * are notified. If the given listener is also registered using
	 * <code>addDocumentListener</code> it will be notified twice.
	 * If the listener is already registered nothing happens.<p>
	 * 
	 * This method is not for public use, it may only be called by
	 * implementers of <code>IDocumentAdapter</code> and only if those 
	 * implementers need to implement <code>IDocumentListener</code>.
	 * 
	 * @param documentAdapter the listener to be added as prenotified document listener
	 */
	void addPrenotifiedDocumentListener(IDocumentListener documentAdapter);
	
	/**
	 * Removes the given document listener from the document's list of
	 * prenotified document listeners. If the listener is not registered
	 * with the document nothing happens. <p>
	 * 
	 * This method is not for public use, it may only be called by
	 * implementers of <code>IDocumentAdapter</code> and only if those 
	 * implementers need to implement <code>IDocumentListener</code>.
	 * 
	 * @param documentAdapter the listener to be removed
	 * 
	 * @see #addPrenotifiedDocumentListener(IDocumentListener)
	 */
	void removePrenotifiedDocumentListener(IDocumentListener documentAdapter);
	
	
	
	/* -------------------------- positions ----------------------------------- */
	
	/**
	 * Adds a new position category to the document. If the position category
	 * already exists nothing happens.
	 *
	 * @param category the category to be added
	 */
	void addPositionCategory(String category);
	
	/**
	 * Deletes the position category from the document. All positions
	 * in this category are thus deleted as well.
	 *
	 * @param category the category to be removed
	 * @exception BadPositionCategoryException if category is undefined in this document
	 */
	void removePositionCategory(String category) throws BadPositionCategoryException;
	
	/**
	 * Returns all position categories of this document. This
	 * includes the default position category.
	 *
	 * @return the document's position categories
	 */
	String[] getPositionCategories();
	
	/**
	 * Checks the presence of the specified position category.
	 *
	 * @param category the category to check
	 * @return <code>true</code> if category is defined
	 */
	boolean containsPositionCategory(String category);
	
	/**
	 * Adds the position to the document's default position category.
	 * This is a convenience method for <code>addPosition(DEFAULT_CATEGORY, position)</code>.
	 *
	 * @param position the position to be added
	 * @exception BadLocationException if position describes an invalid range in this document
	 */
	void addPosition(Position position) throws BadLocationException;
	
	/**
	 * Removes the given position from the document's default position category.
	 * This is a convenience method for <code>removePosition(DEFAULT_CATEGORY, position)</code>.
	 *
	 * @param position the position to be removed
	 */
	void removePosition(Position position);
	
	/**
	 * Adds the position to the specified position category of the document.
	 * A position that has been added to a position category is updated on each
	 * change applied to the document. Positions may be added multiple times.
	 * The order of the category is maintained.
	 *
	 * @param category the category to which to add
	 * @param position the position to be added
	 * @exception BadLocationException if position describes an invalid range in this document
	 * @exception BadPositionCategoryException if the category is undefined in this document
	 */
	void addPosition(String category, Position position) throws BadLocationException, BadPositionCategoryException;
	
	/**
	 * Removes the given position from the specified position category. 
	 * If the position is not part of the specified category nothing happens.
	 * If the position has been added multiple times, only the first occurence is deleted.
	 *
	 * @param category the category from which to delete
	 * @param position the position to be deleted
	 * @exception BadPositionCategoryException if category is undefined in this document
	 */
	void removePosition(String category, Position position) throws BadPositionCategoryException;
	
	/**
	 * Returns all positions of the given position category.
	 * The positions are ordered according to the category's order.
	 * Manipulating this list does not affect the document, but manipulating the
	 * position does affect the document.
	 *
	 * @param category the category
	 * @return the list of all positions
	 * @exception BadPositionCategoryException if category is undefined in this document
	 */
	Position[] getPositions(String category) throws BadPositionCategoryException;
	
	/**
	 * Determines whether a position described by the parameters is managed by this document.
	 *
	 * @param category the category to check
	 * @param offset the offset of the position to find
	 * @param length the length of the position to find
	 * @return <code>true</code> if position is found
	 */
	boolean containsPosition(String category, int offset, int length);
		
	/**
	 * Computes the index at which a <code>Position</code> with the
	 * specified offset would be inserted into the given category. As the
	 * ordering inside a category only depends on the offset, the index must be
	 * choosen to be the first of all positions with the same offset.
	 *
	 * @param category the category in which would be added
	 * @param offset the position offset to be considered
	 * @return the index into the category
	 * @exception BadLocationException if offset is invalid in this document
	 * @exception BadPositionCategoryException if category is undefined in this document
	 */
	int computeIndexInCategory(String category, int offset) throws BadLocationException, BadPositionCategoryException;
		
	/**
	 * Appends a new position updater to the document's list of position updaters.
	 * Position updaters may be added multiple times.<p>
	 * An <code>IPositionUpdater</code> may call back to this method
	 * when being inside a document notification. 
	 *
	 * @param updater the updater to be added
	 */
	void addPositionUpdater(IPositionUpdater updater);
	
	/**
	 * Removes the position updater from the document's list of position updaters.
	 * If the position updater has multiple occurences only the first occurence is
	 * removed. If the position updater is not registered with this document, nothing
	 * happens.<p>
	 * An <code>IPositionUpdater</code> may call back to this method
	 * when being inside a document notification. 
	 *
	 * @param updater the updater to be removed
	 */
	void removePositionUpdater(IPositionUpdater updater);
	
	/**
	 * Inserts the position updater at the specified index in the document's 
	 * list of position updaters. Positions updaters may be inserted multiple times.<p>
	 * An <code>IPositionUpdater</code> may call back to this method
	 * when being inside a document notification. 
	 *
	 * @param updater the updater to be inserted
	 * @param index the index in the document's updater list
	 */
	void insertPositionUpdater(IPositionUpdater updater, int index);
	
	/**
	 * Returns the list of position updaters attached to the document.
	 *
	 * @return the list of position updaters
	 */
	IPositionUpdater[] getPositionUpdaters();
	
	
	
	
	/* -------------------------- partitions ---------------------------------- */
	
	/**
	 * Returns the set of legal content types of document partitions.
	 * This set can be empty. The set can contain more content types than 
	 * contained by the result of <code>getPartitioning(0, getLength())</code>.
	 *
	 * @return the set of legal content types
	 */
	String[] getLegalContentTypes();
	
	/**
	 * Returns the type of the document partition containing the given offset.
	 * This is a convenience method for <code>getPartition(offset).getType()</code>.
	 *
	 * @param offset the document offset
	 * @return the partition type
	 * @exception BadLocationException if offset is invalid in this document
	 */
	String getContentType(int offset) throws BadLocationException;
	
	/**
	 * Returns the document partition in which the position is located.
	 *
	 * @param offset the document offset
	 * @return a specification of the partition
	 * @exception BadLocationException if offset is invalid in this document
	 */
	ITypedRegion getPartition(int offset) throws BadLocationException;
	
	/**
	 * Computes the partitioning of the given document range using the 
	 * document's partitioner.
	 *
	 * @param offset the document offset at which the range starts
	 * @param length the length of the document range
	 * @return a specification of the range's partitioning
	 * @exception BadLocationException if the range is invalid in this document
	 */
	ITypedRegion[] computePartitioning(int offset, int length) throws BadLocationException;
	
	/**
	 * Registers the document partitioning listener with the document. After registration
	 * the document partitioning listener is informed about each partition change
	 * cause by a document manipulation or by changing the document's partitioner. 
	 * If a document partitioning listener is also
	 * a document listener, the following notification sequence is guaranteed if a
	 * document manipulation changes the document partitioning:
	 * <ul>
	 * <li>listener.documentAboutToBeChanged(DocumentEvent);
	 * <li>listener.documentPartitioningChanged();
	 * <li>listener.documentChanged(DocumentEvent);
	 * </ul>
	 * If the listener is already registered nothing happens.<p>
	 * An <code>IDocumentPartitioningListener</code> may call back to this method
	 * when being inside a document notification. 
	 *
	 * @param listener the listener to be added
	 */
	void addDocumentPartitioningListener(IDocumentPartitioningListener listener);
	
	/**
	 * Removes the listener from this document's list of document partitioning
	 * listeners. If the listener is not registered with the document nothing
	 * happens.<p>
	 * An <code>IDocumentPartitioningListener</code> may call back to this method
	 * when being inside a document notification. 
	 *
	 * @param listener the listener to be removed
	 */
	void removeDocumentPartitioningListener(IDocumentPartitioningListener listener);
	
	/**
	 * Sets this document's partitioner. The caller of this method is responsible for
	 * disconnecting the document's old partitioner from the document and to
	 * connect the new partitioner to the document. Informs all document partitioning
	 * listeners about this change.
	 *
	 * @param the document's new partitioner
	 *
	 * @see IDocumentPartitioningListener
	 */
	void setDocumentPartitioner(IDocumentPartitioner partitioner);	
	
	/**
	 * Returns this document's partitioner.
	 *
	 * @return this document's partitioner
	 */
	IDocumentPartitioner getDocumentPartitioner();
	
	
	
	/* ---------------------- line information -------------------------------- */
	
	/**
	 * Returns the length of the given line including the line's delimiter.
	 *
	 * @param line the line of interest
	 * @return the length of the line
	 * @exception BadLocationException if the line number is invalid in this document
	 */
	int getLineLength(int line) throws BadLocationException;
	
	/**
	 * Returns the number of the line at which the character of the specified position is located. 
	 * The first line has the line number 0. A new line starts directly after a line
	 * delimiter. <code>(pos == document length)</code> is a valid argument also there is no
	 * corresponding character.
	 *
	 * @param offset the document offset
	 * @return the number of the line
	 * @exception BadLocationException if the offset is invalid in this document
	 */
	int getLineOfOffset(int offset) throws BadLocationException;
	
	/**
	 * Determines the offset of the first character of the given line.
	 *
	 * @param line the line of interest
	 * @return the document offset
	 * @exception BadLocationException if the line number is invalid in this document
	 */
	int getLineOffset(int line) throws BadLocationException;
	
	/**
	 * Returns a description of the specified line. The line is described by its
	 * offset and its length excluding the line's delimiter.
	 *
	 * @param line the line of interest
	 * @return a line description
	 * @exception BadLocationException if the line number is invalid in this document
	 */
	IRegion getLineInformation(int line) throws BadLocationException;
	
	/**
	 * Returns a description of the line at the given offset.
	 * The description contains the offset and the length of the line
	 * excluding the line's delimiter.
	 *
	 * @param offset the offset whose line should be described
	 * @return a region describing the line
	 * @exception BadLocationException if offset is invalid in this document
	 */
	IRegion getLineInformationOfOffset(int offset) throws BadLocationException;
	
	/**
	 * Returns the number of lines in this document
	 *
	 * @return the number of lines in this document
	 */
	int getNumberOfLines();
	
	/**
	 * Returns the number of lines which are occupied by a given text range.
	 *
	 * @param offset the offset of the specified text range
	 * @param length the length of the specified text range
	 * @return the number of lines occupied by the specified range
	 * @exception BadLocationException if specified range is invalid in this tracker
	 */
	int getNumberOfLines(int offset, int length) throws BadLocationException;
	
	/**
	 * Computes the number of lines in the given text. For a given
	 * implementer of this interface this method returns the same
	 * result as <code>set(text); getNumberOfLines()</code>.
	 * 
	 * @param text the text whose number of lines should be computed
	 * @return the number of lines in the given text
	 */
	int computeNumberOfLines(String text);
	
	
	/* ------------------ line delimiter conversion --------------------------- */
	
	/**
	 * Returns the document's legal line delimiters.
	 *
	 * @return the document's legal line delimiters
	 */
	String[] getLegalLineDelimiters();

	/**
	 * Returns the line delimiter of that line or <code>null</code> if the
     * line is not closed with a line delimiter.
	 *
	 * @param line the line of interest
     * @return the line's delimiter or <code>null</code> if line does not have a delimiter
	 * @exception BadLocationException if the line number is invalid in this document
	 */
	String getLineDelimiter(int line) throws BadLocationException;
	

	/* ---------------------------- search ------------------------------------ */
	
	/**
	 * Returns the offset of a given search string in the document based on a set of search criteria.
	 *
	 * @param startOffset document offset at which search starts
	 * @param findString the string to find
	 * @param forwardSearch the search direction
	 * @param caseSensitive indicates whether lower and upper case should be distinguished
	 * @param wholeWord indicates whether the findString should be limited by white spaces as 
	 * 		defined by Character.isWhiteSpace
	 * @return the offset of the first occurence of findString based on the parameters
	 * @exception BadLocationException if startOffset is an invalid document offset
	 */
	int search(int startOffset, String findString, boolean forwardSearch, boolean caseSensitive, boolean wholeWord) throws BadLocationException;
}
