/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.workbench.texteditor.tests;

import java.util.ResourceBundle;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.util.Util;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.TextViewer;

import org.eclipse.ui.PlatformUI;

/**
 * Tests the FindReplaceDialog.
 *
 * @since 3.1
 */
public class FindReplaceDialogTest extends TestCase {

	private Accessor fFindReplaceDialog;
	private TextViewer fTextViewer;

	public FindReplaceDialogTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(new FindReplaceDialogTest("testInitialButtonState"));
		suite.addTest(new FindReplaceDialogTest("testDisableWholeWordIfRegEx"));
		suite.addTest(new FindReplaceDialogTest("testDisableWholeWordIfNotWord"));
		suite.addTest(new FindReplaceDialogTest("testFocusNotChangedWhenEnterPressed"));
		

		if (org.eclipse.jface.util.Util.isWindows() /* Disabled for now, see bug 323476 || org.eclipse.jface.util.Util.isLinux() */)
//			suite.addTest(new FindReplaceDialogTest("testFocusNotChangedWhenButtonMnemonicPressed"));
		suite.addTest(new FindReplaceDialogTest("testShiftEnterReversesSearchDirection"));

		return suite;
	}

	private void runEventQueue() {
		Display display= PlatformUI.getWorkbench().getDisplay();
		for (int i= 0; i < 10; i++) { // workaround for https://bugs.eclipse.org/323272
			while (display.readAndDispatch()) {
				// do nothing
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

	private void openFindReplaceDialog() {
		Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		fFindReplaceDialog= new Accessor("org.eclipse.ui.texteditor.FindReplaceDialog", getClass().getClassLoader(), new Object[] { shell });
		fFindReplaceDialog.invoke("create", null);
	}

	private void openTextViewerAndFindReplaceDialog() {
		fTextViewer= new TextViewer(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		fTextViewer.setDocument(new Document("line\nline\nline"));
		fTextViewer.getControl().setFocus();

		Accessor fFindReplaceAction;
		fFindReplaceAction= new Accessor("org.eclipse.ui.texteditor.FindReplaceAction", getClass().getClassLoader(), new Class[] {ResourceBundle.class, String.class, Shell.class, IFindReplaceTarget.class}, new Object[] {ResourceBundle.getBundle("org.eclipse.ui.texteditor.ConstructedEditorMessages"), "Editor.FindReplace.", fTextViewer.getControl().getShell(), fTextViewer.getFindReplaceTarget()});
		fFindReplaceAction.invoke("run", null);

		Object fFindReplaceDialogStub= fFindReplaceAction.get("fgFindReplaceDialogStub");
		if (fFindReplaceDialogStub == null)
			fFindReplaceDialogStub= fFindReplaceAction.get("fgFindReplaceDialogStubShell");
		Accessor fFindReplaceDialogStubAccessor= new Accessor(fFindReplaceDialogStub, "org.eclipse.ui.texteditor.FindReplaceAction$FindReplaceDialogStub", getClass().getClassLoader());

		fFindReplaceDialog= new Accessor(fFindReplaceDialogStubAccessor.invoke("getDialog", null), "org.eclipse.ui.texteditor.FindReplaceDialog", getClass().getClassLoader());
	}

	protected void tearDown() throws Exception {
		if (fFindReplaceDialog != null) {
			fFindReplaceDialog.invoke("close", null);
			fFindReplaceDialog= null;
		}

		if (fTextViewer != null) {
			fTextViewer.getControl().dispose();
			fTextViewer= null;
		}
	}

	public void testInitialButtonState() {
		openFindReplaceDialog();

		Boolean value;
		value= (Boolean)fFindReplaceDialog.invoke("isWholeWordSearch", null);
		assertFalse(value.booleanValue());
		value= (Boolean)fFindReplaceDialog.invoke("isWholeWordSetting", null);
		assertFalse(value.booleanValue());
		value= (Boolean)fFindReplaceDialog.invoke("isWrapSearch", null);
		assertTrue(value.booleanValue());
		value= (Boolean)fFindReplaceDialog.invoke("isRegExSearch", null);
		assertFalse(value.booleanValue());
		value= (Boolean)fFindReplaceDialog.invoke("isRegExSearchAvailableAndChecked", null);
		assertFalse(value.booleanValue());
		Button checkbox= (Button)fFindReplaceDialog.get("fIsRegExCheckBox");
		assertTrue(checkbox.isEnabled());
		checkbox= (Button)fFindReplaceDialog.get("fWholeWordCheckBox");
		assertFalse(checkbox.isEnabled()); // there's no word in the Find field
	}

	public void testDisableWholeWordIfRegEx() {
		openFindReplaceDialog();

		Combo findField= (Combo)fFindReplaceDialog.get("fFindField");
		findField.setText("word");

		Button isRegExCheckBox= (Button)fFindReplaceDialog.get("fIsRegExCheckBox");
		Button wholeWordCheckbox= (Button)fFindReplaceDialog.get("fWholeWordCheckBox");

		assertTrue(isRegExCheckBox.isEnabled());
		assertTrue(wholeWordCheckbox.isEnabled());

		fFindReplaceDialog.set("fIsTargetSupportingRegEx", true);
		isRegExCheckBox.setSelection(true);
		wholeWordCheckbox.setSelection(true);
		fFindReplaceDialog.invoke("updateButtonState", null);

		assertTrue(isRegExCheckBox.isEnabled());
		assertFalse(wholeWordCheckbox.isEnabled());

		// XXX: enable once https://bugs.eclipse.org/bugs/show_bug.cgi?id=72462 has been fixed
//		assertFalse(wholeWordCheckbox.getSelection());
	}

	public void testDisableWholeWordIfNotWord() {
		openFindReplaceDialog();

		Combo findField= (Combo)fFindReplaceDialog.get("fFindField");
		Button isRegExCheckBox= (Button)fFindReplaceDialog.get("fIsRegExCheckBox");
		Button wholeWordCheckbox= (Button)fFindReplaceDialog.get("fWholeWordCheckBox");

		fFindReplaceDialog.set("fIsTargetSupportingRegEx", false);
		isRegExCheckBox.setSelection(false);
		wholeWordCheckbox.setSelection(true);
		fFindReplaceDialog.invoke("updateButtonState", null);

		findField.setText("word");
		assertTrue(isRegExCheckBox.isEnabled());
		assertTrue(wholeWordCheckbox.isEnabled());
		assertTrue(wholeWordCheckbox.getSelection());

		findField.setText("no word");
		assertTrue(isRegExCheckBox.isEnabled());
		assertFalse(wholeWordCheckbox.isEnabled());

		// XXX: enable once https://bugs.eclipse.org/bugs/show_bug.cgi?id=72462 has been fixed
//		assertFalse(wholeWordCheckbox.getSelection());
	}

	public void testFocusNotChangedWhenEnterPressed() {
		openTextViewerAndFindReplaceDialog();

		Combo findField= (Combo)fFindReplaceDialog.get("fFindField");
		findField.setFocus();
		findField.setText("line");
		final Event event= new Event();

		event.type= SWT.Traverse;
		event.detail= SWT.TRAVERSE_RETURN;
		event.character= SWT.CR;
		event.doit= true;
		findField.traverse(SWT.TRAVERSE_RETURN, event);
		runEventQueue();
		assertTrue(findField.isFocusControl());

		Button wrapSearchBox= (Button)fFindReplaceDialog.get("fWrapCheckBox");
		wrapSearchBox.setFocus();
		event.doit= true;
		findField.traverse(SWT.TRAVERSE_RETURN, event);
		runEventQueue();
		assertTrue(wrapSearchBox.isFocusControl());

		Button allScopeBox= (Button)fFindReplaceDialog.get("fGlobalRadioButton");
		allScopeBox.setFocus();
		event.doit= true;
		findField.traverse(SWT.TRAVERSE_RETURN, event);
		runEventQueue();
		assertTrue(allScopeBox.isFocusControl());
	}

	public void testFocusNotChangedWhenButtonMnemonicPressed() {
		openTextViewerAndFindReplaceDialog();

		Combo findField= (Combo)fFindReplaceDialog.get("fFindField");
		findField.setText("line");
		final Event event= new Event();

		Button wrapSearchBox= (Button)fFindReplaceDialog.get("fWrapCheckBox");
		wrapSearchBox.setFocus();
		event.detail= SWT.TRAVERSE_MNEMONIC;
		event.character= 'n';
		event.doit= false;
		wrapSearchBox.traverse(SWT.TRAVERSE_MNEMONIC, event);
		runEventQueue();
		assertTrue(wrapSearchBox.isFocusControl());

		Button allScopeBox= (Button)fFindReplaceDialog.get("fGlobalRadioButton");
		allScopeBox.setFocus();
		event.detail= SWT.TRAVERSE_MNEMONIC;
		event.doit= false;
		allScopeBox.traverse(SWT.TRAVERSE_MNEMONIC, event);
		runEventQueue();
		assertTrue(allScopeBox.isFocusControl());

		event.detail= SWT.TRAVERSE_MNEMONIC;
		event.character= 'r';
		event.doit= false;
		allScopeBox.traverse(SWT.TRAVERSE_MNEMONIC, event);
		runEventQueue();
		assertTrue(allScopeBox.isFocusControl());
	}


	public void testShiftEnterReversesSearchDirection() {
		openTextViewerAndFindReplaceDialog();

		Combo findField= (Combo)fFindReplaceDialog.get("fFindField");
		findField.setText("line");
		IFindReplaceTarget target= (IFindReplaceTarget)fFindReplaceDialog.get("fTarget");
		runEventQueue();
		Shell shell= ((Shell)fFindReplaceDialog.get("fActiveShell"));
		if (shell == null && Util.isGtk())
			fail("this test does not work on GTK unless the runtime workbench has focus");
		
		final Event event= new Event();

		event.type= SWT.TRAVERSE_RETURN;
		event.character= SWT.CR;
		shell.traverse(SWT.TRAVERSE_RETURN, event);
		runEventQueue();
		assertEquals(0, (target.getSelection()).x);
		assertEquals(4, (target.getSelection()).y);

		event.doit= true;
		shell.traverse(SWT.TRAVERSE_RETURN, event);
		runEventQueue();
		assertEquals(5, (target.getSelection()).x);
		assertEquals(4, (target.getSelection()).y);

		event.type= SWT.Selection;
		event.stateMask= SWT.SHIFT;
		event.doit= true;
		Button findNextButton= (Button)fFindReplaceDialog.get("fFindNextButton");
		findNextButton.notifyListeners(SWT.Selection, event);
		assertEquals(0, (target.getSelection()).x);
		assertEquals(4, (target.getSelection()).y);

		Button forwardRadioButton= (Button)fFindReplaceDialog.get("fForwardRadioButton");
		forwardRadioButton.setSelection(false);
		findNextButton.notifyListeners(SWT.Selection, event);
		assertEquals(5, (target.getSelection()).x);
	}
}
