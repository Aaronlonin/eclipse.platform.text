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


import java.util.ArrayList;
import java.util.List;



/**
 * Abstract implementation of <code>ILineTracker</code>. It lets the
 * definition of line delimiters to subclasses. Assuming that '\n' is
 * the only line delimiter, this abstract implementation defines the
 * following line scheme:
 * <ul>
 * <li> ""        -> [0,0]
 * <li> "a"       -> [0,1]
 * <li> "\n"      -> [0,1], [1,0]
 * <li> "a\n"     -> [0,2], [2,0]
 * <li> "a\nb"    -> [0,2], [2,1]
 * <li> "a\nbc\n" -> [0,2], [2,3], [5,0]
 * </ul>
 * This class must be subclassed.
 */
public abstract class AbstractLineTracker implements ILineTracker {
	
	
	/**
	 * Combines the information of the occurence of a line delimiter.
	 * <code>delimiterIndex</code> is the index where a line delimiter
	 * starts, whereas <code>delimiterLength</code>, indicates the length
	 * of the delimiter. 
	 */
	protected static class DelimiterInfo {
		public int delimiterIndex;
		public int delimiterLength;
		public String delimiter;
	}
		
	
	/** The line information */
	private List fLines= new ArrayList();
	/** The length of the tracked text */
	private int fTextLength;
	
	
	/**
	 * Creates a new line tracker.
	 */
	protected AbstractLineTracker() {
	}
			
	/**
	 * Binary search for the line at a given offset.
	 *
	 * @param offset the offset whose line should be found
	 * @return the line of the offset
	 */
	private int findLine(int offset) {
		
		if (fLines.size() == 0)
			return -1;
			
		int left= 0;
		int right= fLines.size() -1;
		int mid= 0;
		Line line= null;
		
		while (left < right) {
			
			mid= (left + right) / 2;
				
			line= (Line) fLines.get(mid);
			if (offset < line.offset) {
				if (left == mid)
					right= left;
				else
					right= mid -1;
			} else if (offset > line.offset) {
				if (right == mid)
					left= right;
				else
					left= mid  +1;
			} else if (offset == line.offset) {
				left= right= mid;
			}
		}
		
		line= (Line) fLines.get(left);
		if (line.offset > offset)
			-- left;		
		return left;
	}
	
	/**
	 * Returns the number of lines covered by the specified text range.
	 *
	 * @param startLine the line where the text range starts
	 * @param offset the start offset of the text range
	 * @param length the length of the text range
	 * @return the number of lines covered by this text range
	 * @exception BadLocationException if range is undefined in this tracker
	 */
	private int getNumberOfLines(int startLine, int offset, int length) throws BadLocationException {
		
		if (length == 0)
			return 1;
			
		int target= offset + length;
		
		Line l= (Line) fLines.get(startLine);
		
		if (l.delimiter == null)
			return 1;
		
		if (l.offset + l.length > target)
			return 1;
		
		if (l.offset + l.length == target)
			return 2;
			
		return getLineNumberOfOffset(target) - startLine + 1;
	}
	
	/*
	 * @see ILineTracker#getLineLength(int)
	 */
	public int getLineLength(int line) throws BadLocationException {
		
		int lines= fLines.size();
		
		if (line < 0 || line > lines)
			throw new BadLocationException();
		
		if (lines == 0 || lines == line)
			return 0;
				
		Line l= (Line) fLines.get(line);
		return l.length;
	}
		
	/*
	 * @see ILineTracker#getLineNumberOfOffset(int)
	 */
	public int getLineNumberOfOffset(int position) throws BadLocationException {
				
		if (position > fTextLength)
			throw new BadLocationException();
			
		if (position == fTextLength) {
			
			int lastLine= fLines.size() - 1;
			if (lastLine < 0)
				return 0;
							
			Line l= (Line) fLines.get(lastLine);
			return (l.delimiter != null ? lastLine + 1 : lastLine);
		}
		
		return findLine(position);
	}
	
	/*
	 * @see ILineTracker#getLineInformationOfOffset(int)
	 */
	public IRegion getLineInformationOfOffset(int position) throws BadLocationException {		
		if (position > fTextLength)
			throw new BadLocationException();
			
		if (position == fTextLength) {
			int size= fLines.size();
			if (size == 0)
				return new Region(0, 0);
			Line l= (Line) fLines.get(size - 1);
			return (l.delimiter != null ? new Line(fTextLength, 0) : new Line(fTextLength - l.length, l.length));
		}	
		
		return getLineInformation(findLine(position));
	}
	
	/*
	 * @see ILineTracker#getLineInformation(int)
	 */
	public IRegion getLineInformation(int line) throws BadLocationException {
		
		int lines= fLines.size();
		
		if (line < 0 || line > lines)
			throw new BadLocationException();
			
		if (lines == 0)
			return new Line(0, 0);
			
		if (line == lines) {
			Line l= (Line) fLines.get(line - 1);
			return new Line(l.offset + l.length, 0);
		}
		
		Line l= (Line) fLines.get(line);
		return (l.delimiter != null ? new Line(l.offset, l.length - l.delimiter.length()) : l);
	}
		
	/*
	 * @see ILineTracker#getLineOffset(int)
	 */
	public int getLineOffset(int line) throws BadLocationException {
		
		int lines= fLines.size();
		
		if (line < 0 || line > lines)
			throw new BadLocationException();
		
		if (lines == 0)
			return 0;
			
		if (line == lines) {
			Line l= (Line) fLines.get(line - 1);
			return l.offset + l.length;
		}
		
		Line l= (Line) fLines.get(line);
		return l.offset;
	}
		
	/*
	 * @see ILineTracker#getNumberOfLines()
	 */
	public int getNumberOfLines() {
		
		int lines= fLines.size();
		
		if (lines == 0)
			return 1;
			
		Line l= (Line) fLines.get(lines - 1);
		return (l.delimiter != null ? lines + 1 : lines);
	}
		
	/*
	 * @see ILineTracker#getNumberOfLines(int, int)
	 */
	public int getNumberOfLines(int position, int length) throws BadLocationException {
		
		if (position < 0 || position + length > fTextLength)
			throw new BadLocationException();
			
		if (length == 0) // optimization
			return 1;
			
		return getNumberOfLines(getLineNumberOfOffset(position), position, length);
	}
	
	/*
	 * @see ILineTracker#computeNumberOfLines(String)
	 */
	public int computeNumberOfLines(String text) {
		int count= 0;
		int start= 0;
		DelimiterInfo delimiterInfo= nextDelimiterInfo(text, start);
		while (delimiterInfo != null && delimiterInfo.delimiterIndex > -1) {
			++count;			
			start= delimiterInfo.delimiterIndex + delimiterInfo.delimiterLength;
			delimiterInfo= nextDelimiterInfo(text, start);
		}
		return count;	
	}
	
	
	/* ----------------- manipulation ------------------------------ */
	
	
	/**
	 * Returns the info of the first delimiter found in the given
	 * text starting at the given offset.
	 *
	 * @param text the text to be searched
	 * @param offset the offset in the given text
	 * @return the info of the first found delimiter or <code>null</code> if 
	 *		there is no such info
	 */
	protected abstract DelimiterInfo nextDelimiterInfo(String text, int offset);
	
	
	/**
	 * Creates the line structure for the given text. Newly created lines
	 * are inserted into the line structure starting at the given
	 * position. Returns the number of newly created lines.
	 *
	 * @param text the text for which to create a line structure
	 * @param insertPosition the position at which the newly created lines are inserted
	 * 		into the tracker's line structure
	 * @param offset the offset of all newly created lines
	 * @return the number of newly created lines
	 */
	private int createLines(String text, int insertPosition, int offset) {
		
		int count= 0;
		int start= 0;
		DelimiterInfo delimiterInfo= nextDelimiterInfo(text, 0);
		
		
		while (delimiterInfo != null && delimiterInfo.delimiterIndex > -1) {
			
			int index= delimiterInfo.delimiterIndex + (delimiterInfo.delimiterLength - 1);
			
			if (insertPosition + count >= fLines.size())
				fLines.add(new Line(offset + start, offset + index, delimiterInfo.delimiter));
			else 
				fLines.add(insertPosition + count, new Line(offset + start, offset + index, delimiterInfo.delimiter));
				
			++count;
			start= index + 1;
			delimiterInfo= nextDelimiterInfo(text, start);
		}
		
		if (start < text.length()) {
			if (insertPosition + count < fLines.size()) {
				// there is a line below the current
				Line l= (Line) fLines.get(insertPosition + count);
				int delta= text.length() - start;
				l.offset -= delta;
				l.length += delta;
			} else {
				fLines.add(new Line(offset + start, offset + text.length() - 1, null));
				++count;
			}
		}
		
		return count;
	}
	
	/**
	 * Keeps track of the line information when text is inserted.
	 * Returns the number of inserted lines.
	 *
	 * @param lineNumber the line at which the insert happens
	 * @param offset at which the insert happens
	 * @param text the inserted text
	 * @return the number of inserted lines
	 * @exception BadLocationException if offset is invalid in this tracker
	 */
	private int insert(int lineNumber, int offset, String text) throws BadLocationException {
		
		if (text == null || text.length() == 0)
			return 0;
			
		fTextLength += text.length();
		
		int size= fLines.size();
		
		if (size == 0 || lineNumber >= size)
			return createLines(text, size, offset);
					
		Line line= (Line) fLines.get(lineNumber);
		DelimiterInfo delimiterInfo= nextDelimiterInfo(text, 0);
		if (delimiterInfo == null || delimiterInfo.delimiterIndex == -1) {
			line.length += text.length();
			return 0;
		}
		
		
		// as there is a line break, split line but do so only if rest of line is not of length 0
		int restLength= line.offset + line.length - offset;
		if (restLength > 0) {
			// determine start and end of the second half of the splitted line
			Line lineRest= new Line(offset, restLength);
			lineRest.delimiter= line.delimiter;
			// shift it by the inserted text
			lineRest.offset += text.length();
			//  and insert in line structure
			fLines.add(lineNumber + 1, lineRest);
		}
		
		// adapt the beginning of the splitted line
		line.delimiter= delimiterInfo.delimiter;
		int nextStart= offset + delimiterInfo.delimiterIndex + delimiterInfo.delimiterLength;
		line.length= nextStart - line.offset;
		
		// insert lines for the remaining text
		text= text.substring(delimiterInfo.delimiterIndex + delimiterInfo.delimiterLength);
		return createLines(text, lineNumber + 1, nextStart) + 1;
	}
		
	/**
	 * Keeps track of the line information when text is removed. Returns 
	 * whether the line at which the deletion start will thereby be deleted.
	 *
	 * @param lineNumber the lineNumber at which the deletion starts
	 * @param offset the offset of the first deleted character
	 * @param length the number of deleted characters 
	 * @return whethere the start line of the deletion has been deleted
	 * @exception BadLocationException if position is unkown to the tracker
	 */
	private boolean remove(int lineNumber, int offset, int length) throws BadLocationException {
		
		if (length == 0)
			return false;		
		
		int removedLineEnds= getNumberOfLines(lineNumber, offset, length) - 1;
		Line line= (Line) fLines.get(lineNumber);
		
		if ((lineNumber == fLines.size() - 1) && removedLineEnds > 0) {
			line.length -= length; 
			line.delimiter= null;
		} else {
			
			++ lineNumber;
			for (int i= 1; i <= removedLineEnds; i++) {
				
				if (lineNumber == fLines.size()) {
					line.delimiter= null;
					break;
				}
				
				Line line2= (Line) fLines.get(lineNumber);
				line.length += line2.length;
				line.delimiter= line2.delimiter;
				fLines.remove(lineNumber);
			}
			line.length -= length;
		}
		
		fTextLength -= length;
		
		if (line.length == 0) {
			fLines.remove(line);
			return true;
		}
		
		return false;	
	}
	
	/**
	 * Adapts the offset of all lines with line numbers greater than the specified
	 * one to the given delta.
	 *
	 * @param lineNumber the line number after which to start
	 * @param delta the offset delta to be applied
	 */
	private void adaptLineOffsets(int lineNumber, int delta) {
		int size= fLines.size();
		for (int i= lineNumber + 1; i < size; i++) {
			Line l= (Line) fLines.get(i);
			l.offset += delta;
		}
	}
	
	/*
	 * @see ILineTracker#replace(int, int, String)
	 */
	public void replace(int position, int length, String text) throws BadLocationException {
		
		int firstLine= getLineNumberOfOffset(position);
		int insertLineNumber= firstLine;
		
		if (remove(firstLine, position, length))
			-- firstLine;
			
		int lastLine= firstLine + insert(insertLineNumber, position, text);
		
//		int lines= fLines.size();
//		if (lines > 0) {
//			
//			// try to collapse the first and the second line if second line is empty
//			if (0 <= firstLine && firstLine + 1 < lines) {
//				Line l2= (Line) fLines.get(firstLine + 1);
//				if (l2.delimiter != null && l2.length == l2.delimiter.length()) {
//					// line is empty
//					
//					// append empty line to precessor
//					Line l1= (Line) fLines.get(firstLine);
//					StringBuffer buffer= new StringBuffer();
//					buffer.append(l1.delimiter);
//					buffer.append(l2.delimiter);
//					
//					// test whether this yields just one line rather then two
//					DelimiterInfo info= nextDelimiterInfo(buffer.toString(), 0);
//					if (info != null && info.delimiterIndex == 0 && info.delimiterLength == buffer.length()) {
//						l1.length += l2.length;
//						l1.delimiter += l2.delimiter;
//						fLines.remove(firstLine + 1);
//						-- lastLine;
//					}
//				}
//			}
//			
//			// try to collapse the last inserted line with the following line
//			if (lastLine < lines) {
//				Line l2= (Line) fLines.get(lastLine);
//				if (l2.delimiter != null && l2.length == l2.delimiter.length()) {
//					// line is empty
//					
//					// append empty line to precessor
//					Line l1= (Line) fLines.get(lastLine -1);
//					StringBuffer buffer= new StringBuffer();
//					buffer.append(l1.delimiter);
//					buffer.append(l2.delimiter);
//					
//					// test whether this yields just one line rather then two
//					DelimiterInfo info= nextDelimiterInfo(buffer.toString(), 0);
//					if (info != null && info.delimiterIndex == 0 && info.delimiterLength == buffer.length()) {
//						l1.length += l2.length;
//						l1.delimiter += l2.delimiter;
//						fLines.remove(lastLine);
//					}
//				}
//			}
//		}
		
		int delta= -length;
		if (text != null)
			delta= text.length() + delta;
		
		if (delta != 0)
			adaptLineOffsets(lastLine, delta);
	}
	
	/*
	 * @see ILineTracker#set(String)
	 */
	public void set(String text) {
		fLines.clear();
		if (text != null) {
			fTextLength= text.length();
			createLines(text, 0, 0);
		}
	}
	
	/*
	 * @see ILineTracker#getLineDelimiter(int)
	 */
	public String getLineDelimiter(int line) throws BadLocationException {
		
		int lines= fLines.size();
		
		if (line < 0 || line > lines)
			throw new BadLocationException();
			
		if (lines == 0)
			return null;
			
		if (line == lines)
			return null;
					
		Line l= (Line) fLines.get(line);
		return l.delimiter;
	}
}
