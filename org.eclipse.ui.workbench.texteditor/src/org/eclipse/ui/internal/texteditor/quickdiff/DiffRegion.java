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
package org.eclipse.ui.internal.texteditor.quickdiff;

import java.util.List;
import java.util.ListIterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ILineDiffInfo;

import org.eclipse.ui.internal.texteditor.quickdiff.compare.rangedifferencer.RangeDifference;


/**
 * The local implementation of <code>ILineDiffInfo</code>. As instances are
 * also <code>Annotation</code>s, they can be used in
 * <code>DocumentLineDiffer</code>s <code>IAnnotationModel</code> protocol.
 * 
 * @since 3.0
 */
public final class DiffRegion extends Annotation implements ILineDiffInfo {
	private final RangeDifference fDifference;

	private final int fOffset;

	private final List fList;

	private final IDocument fDocument;

	/**
	 * Creates a new diff region.
	 * 
	 * @param difference
	 * @param offset
	 * @param differences
	 * @param source
	 */
	public DiffRegion(RangeDifference difference, int offset, List differences, IDocument source) {
		super("org.eclipse.ui.workbench.texteditor.quickdiffChange", false, null); //$NON-NLS-1$
		fOffset= offset;
		fDifference= difference;
		fList= differences;
		fDocument= source;
	}

	/*
	 * @see org.eclipse.jface.text.source.Annotation#getType()
	 */
	public String getType() {
		// we return unknown for unchanged regions to avoid
		// them getting displayed.
		switch (getChangeType()) {
			case CHANGED:
				return "org.eclipse.ui.workbench.texteditor.quickdiffChange"; //$NON-NLS-1$
			case ADDED:
				return "org.eclipse.ui.workbench.texteditor.quickdiffAddition"; //$NON-NLS-1$
			case UNCHANGED:
			default:
				return TYPE_UNKNOWN;
		}
	}

	/*
	 * @see org.eclipse.jface.text.source.ILineDiffInfo#getRemovedLinesBelow()
	 */
	public int getRemovedLinesBelow() {
		if (fOffset == fDifference.rightLength() - 1) {
			if (getChangeType() != UNCHANGED) {
				return Math.max(fDifference.leftLength() - fDifference.rightLength(), 0);
			} else {
				for (ListIterator it= fList.listIterator(); it.hasNext();) {
					if (fDifference.equals(it.next())) {
						if (it.hasNext()) {
							RangeDifference next= (RangeDifference) it.next();
							if (next.rightLength() == 0)
								return Math.max(next.leftLength() - next.rightLength(), 0);
						}
						return 0;
					}
				}
				return 0;
			}
		} else
			return 0;
	}

	/*
	 * @see org.eclipse.jface.text.source.ILineDiffInfo#getChangeType()
	 */
	public int getChangeType() {
		if (fDifference.kind() == RangeDifference.NOCHANGE)
			return UNCHANGED;
		else {
			if (fOffset >= fDifference.leftLength())
				return ADDED;
			else
				return CHANGED;
		}
	}

	/*
	 * @see org.eclipse.jface.text.source.ILineDiffInfo#getRemovedLinesAbove()
	 */
	public int getRemovedLinesAbove() {
		if (getChangeType() != UNCHANGED || fOffset != 0)
			return 0;
		else {
			for (ListIterator it= fList.listIterator(fList.size()); it.hasPrevious();) {
				if (fDifference.equals(it.previous())) {
					if (it.hasPrevious()) {
						RangeDifference previous= (RangeDifference) it.previous();
						return Math.max(previous.leftLength() - previous.rightLength(), 0);
					} else
						return 0;
				}
			}
			return 0;
		}
	}

	/*
	 * @see org.eclipse.jface.text.source.ILineDiffInfo#hasChanges()
	 */
	public boolean hasChanges() {
		return getChangeType() != UNCHANGED || getRemovedLinesAbove() > 0 || getRemovedLinesBelow() > 0;
	}

	/*
	 * @see org.eclipse.jface.text.source.ILineDiffInfo#getOriginalText()
	 */
	public String[] getOriginalText() {
		IDocument doc= fDocument;
		if (doc != null) {
			int startLine= fDifference.leftStart() + fOffset;
			if (startLine >= fDifference.leftEnd())
				return new String[0]; // original text of an added line is
									  // empty

			int endLine= startLine + getRemovedLinesBelow();
			if (getChangeType() == UNCHANGED)
				startLine++;
			String[] ret= new String[endLine - startLine + 1];
			for (int i= 0; i < ret.length; i++) {
				try {
					ret[i]= doc.get(doc.getLineOffset(startLine + i), doc.getLineLength(startLine + i));
				} catch (BadLocationException e) {
					ret[i]= new String();
				}
			}
			return ret;
		}

		// in initialization phase?
		return new String[0];
	}

	/*
	 * @see org.eclipse.jface.text.source.Annotation#getText()
	 */
	public String getText() {
		int r= fDifference.rightLength();
		int l= fDifference.leftLength();
		int c= Math.min(r, l);
		int a= r - l;
		String changed= c > 0 ? QuickDiffMessages.getFormattedString("quickdiff.annotation.changed", new Integer(c)) : null; //$NON-NLS-1$
		String added;
		if (a > 0)
			added= QuickDiffMessages.getFormattedString("quickdiff.annotation.added", new Integer(a)); //$NON-NLS-1$
		else if (a < 0)
			added= QuickDiffMessages.getFormattedString("quickdiff.annotation.deleted", new Integer(-a)); //$NON-NLS-1$
		else
			added= null;
		String line= c > 1 || c == 0 && Math.abs(a) > 1 ? QuickDiffMessages.getString("quickdiff.annotation.line_plural") : QuickDiffMessages.getString("quickdiff.annotation.line_singular"); //$NON-NLS-1$//$NON-NLS-2$

		String ret= (changed != null ? changed : "") + (changed != null ? " " + line : "")   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				+ (changed != null && added != null ? ", " : " ") + (added != null ? added : "")  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				+ (added != null && changed == null ? " " + line : ""); //$NON-NLS-1$//$NON-NLS-2$
		return ret;
	}

	/**
	 * @return Returns the difference.
	 */
	public RangeDifference getDifference() {
		return fDifference;
	}

	/**
	 * @return Returns the offset.
	 */
	public int getOffset() {
		return fOffset;
	}
}