package org.eclipse.ui.examples.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.RGB;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.RuleBasedDamagerRepairer;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.examples.javaeditor.java.JavaAutoIndentStrategy;
import org.eclipse.ui.examples.javaeditor.java.JavaCompletionProcessor;
import org.eclipse.ui.examples.javaeditor.java.JavaDoubleClickSelector;
import org.eclipse.ui.examples.javaeditor.java.JavaPartitionScanner;
import org.eclipse.ui.examples.javaeditor.javadoc.JavaDocCompletionProcessor;
import org.eclipse.ui.examples.javaeditor.util.JavaColorProvider;

/**
 * Example configuration for an <code>SourceViewer</code> which shows Java code.
 */
public class JavaSourceViewerConfiguration extends SourceViewerConfiguration {

	/**
	 * Default constructor.
	 */
	public JavaSourceViewerConfiguration() {
	}
	
	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new JavaAnnotationHover();
	}
	
	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	public IAutoIndentStrategy getAutoIndentStrategy(ISourceViewer sourceViewer, String contentType) {
		return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? new JavaAutoIndentStrategy() : new DefaultAutoIndentStrategy());
	}
	
	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, JavaPartitionScanner.JAVA_DOC, JavaPartitionScanner.JAVA_MULTILINE_COMMENT };
	}
	
	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {

		ContentAssistant assistant= new ContentAssistant();
		assistant.setContentAssistProcessor(new JavaCompletionProcessor(), IDocument.DEFAULT_CONTENT_TYPE);
		assistant.setContentAssistProcessor(new JavaDocCompletionProcessor(), JavaPartitionScanner.JAVA_DOC);

		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);
		assistant.setProposalPopupOrientation(assistant.PROPOSAL_OVERLAY);
		assistant.setContextInformationPopupOrientation(assistant.CONTEXT_INFO_ABOVE);
		assistant.setContextInformationPopupBackground(JavaEditorEnvironment.getJavaColorProvider().getColor(new RGB(150, 150, 0)));

		return assistant;
	}
	
	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	public String getDefaultPrefix(ISourceViewer sourceViewer, String contentType) {
		return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? "//" : null); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new JavaDoubleClickSelector();
	}
	
	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "\t", "    " }; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		JavaColorProvider provider= JavaEditorEnvironment.getJavaColorProvider();
		PresentationReconciler reconciler= new PresentationReconciler();

		RuleBasedDamagerRepairer dr= new RuleBasedDamagerRepairer(JavaEditorEnvironment.getJavaCodeScanner(), new TextAttribute(provider.getColor(provider.DEFAULT)));
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr= new RuleBasedDamagerRepairer(JavaEditorEnvironment.getJavaDocScanner(), new TextAttribute(provider.getColor(provider.JAVADOC_DEFAULT)));
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_DOC);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_DOC);

		dr= new RuleBasedDamagerRepairer(null, new TextAttribute(provider.getColor(provider.MULTI_LINE_COMMENT)));
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_MULTILINE_COMMENT);

		return reconciler;
	}
	
	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	public int getTabWidth(ISourceViewer sourceViewer) {
		return 4;
	}
	
	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return new JavaTextHover();
	}
}
