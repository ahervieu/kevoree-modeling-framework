package org.kevoree.modeling.api;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 05/08/13
 * Time: 12:00
 */

trait ModelTraceApplicator {

    fun setTargetModel(rootElem : KMFContainer)

    fun applyTraceOnModel(traceSeq: TraceSequence)

}