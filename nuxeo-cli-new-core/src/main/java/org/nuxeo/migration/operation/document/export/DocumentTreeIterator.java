package org.nuxeo.migration.operation.document.export;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * An iterator over a tree of documents
 * <p>
 * The tree is traversed from top to bottom and left to right.
 * <p>
 * TODO: move this in an utility package
 *
 */
public class DocumentTreeIterator implements Iterator<DocumentModel> {

    private static final Log log = LogFactory.getLog(DocumentTreeIterator.class);

    /**
     * The document manager session.
     */
    protected final CoreSession session;

    /**
     * Root document.
     */
    protected final DocumentModel root;

    /**
     * The current sequence.
     */
    protected Iterator<DocumentModel> sequence;

    /**
     * The sequence queue.
     */
    protected final Queue<Iterator<DocumentModel>> queue = new LinkedList<Iterator<DocumentModel>>();

    private boolean readFolderChildren;

    /**
     * Creates the iterator given the tree root.
     */
    public DocumentTreeIterator(CoreSession session, DocumentModel root) {
        this(session, root, false);
    }

    public DocumentTreeIterator(CoreSession session, DocumentModel root, boolean excludeRoot) {
        this(session, root, excludeRoot, true);
    }

    public DocumentTreeIterator(CoreSession session, DocumentModel root, boolean excludeRoot,
            boolean readFolderChildren) {
        this.readFolderChildren = readFolderChildren;
        this.root = root;
        this.session = session;
        if (excludeRoot) {
            sequence = session.getChildrenIterator(root.getRef(), null, null, null);
        } else {
            sequence = new OneDocSequence(root);
        }
    }

    /**
     * Gets next non empty sequence from queue.
     * <p>
     * This will remove from the queue all traversed sequences (the empty ones
     * and the first not empty sequence found).
     *
     * @return the first non empty sequence or null if no one was found
     */
    protected Iterator<DocumentModel> getNextNonEmptySequence() {
        while (true) {
            Iterator<DocumentModel> seq = queue.poll();
            if (seq == null) {
                return null;
            } else if (seq.hasNext()) {
                return seq;
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (sequence == null || !sequence.hasNext()) {
            // no current valid sequence
            sequence = getNextNonEmptySequence();
            if (sequence == null) {
                return false;
            }
        }
        // we have a sequence to iterate over
        return true;
    }

    @Override
    public DocumentModel next() {
        // satisfy iterator contract - throw an exception if no more elements to
        // iterate
        if (!hasNext()) {
            throw new NoSuchElementException("no more documents to iterate over");
        }
        // we have a non empty sequence to iterate over
        DocumentModel doc = sequence.next();
        if (doc.isFolder() && readFolderChildren) {
            // TODO: load children after the document was traversed
            // update the sequence queue with children from this folder
            queue.add(session.getChildrenIterator(doc.getRef(), null, null, null));
        }
        return doc;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove is not yet supported");
    }

    /**
     * A sequence of a single doc.
     *
     */
    static class OneDocSequence implements Iterator<DocumentModel> {
        final DocumentModel doc;

        boolean hasNext = true;

        OneDocSequence(DocumentModel doc) {
            this.doc = doc;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public DocumentModel next() {
            if (doc == null) {
                throw new NoSuchElementException("no more documents to iterate over");
            }
            hasNext = false;
            return doc;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove is not yet supported");
        }
    }

    /**
     * Resets the iterator back to the tree root and clear any cached data.
     */
    public void reset() {
        sequence = new OneDocSequence(root);
        queue.clear();
    }

}
