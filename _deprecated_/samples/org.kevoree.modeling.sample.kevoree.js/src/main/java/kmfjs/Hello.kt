package kevoree

import kotlin.browser.document
import org.kevoree.impl.DefaultKevoreeFactory
import java.io.OutputStream
import org.kevoree.serializer.JSONModelSerializer
import org.kevoree.loader.JSONModelLoader
import org.kevoree.ContainerRoot
import java.io.ByteArrayInputStream
import org.kevoree.cloner.ModelCloner

/**
* Created with IntelliJ IDEA.
* User: duke
* Date: 02/04/13
* Time: 15:07
*/
fun myApp() : ContainerRoot? {
    val element = document.getElementById("foo")
    if (element != null) {
        val factory = DefaultKevoreeFactory()
        val root = factory.createContainerRoot()
        val node0 = factory.createContainerNode()
        node0.setName("node0")
        root.addNodes(node0)

        var lookupNode = root.findNodesByID("node0")
        element.appendChild(document.createElement("br")!!)
        element.appendChild(document.createTextNode("lookupNode")!!)
        element.appendChild(document.createTextNode(lookupNode!!.getName())!!)
        element.appendChild(document.createElement("br")!!)

        val oo = OutputStream()
        val saver = JSONModelSerializer()
        saver.serialize(root,oo)
        element.appendChild(document.createTextNode("Direct Creation Saved")!!)
        element.appendChild(document.createElement("br")!!)
        element.appendChild(document.createTextNode(oo.result)!!)
        element.appendChild(document.createElement("br")!!)

        val loader = JSONModelLoader()
        val modelLoaded = loader.loadModelFromString(oo.result)!!.get(0) as ContainerRoot
        element.appendChild(document.createTextNode("Node Size")!!)
        element.appendChild(document.createElement("br")!!)
        element.appendChild(document.createTextNode(",size="+modelLoaded.getNodes().size)!!)
        element.appendChild(document.createElement("br")!!)

        val oo2 = OutputStream()
        saver.serialize(modelLoaded,oo2)
        element.appendChild(document.createTextNode("After reload in browser")!!)
        element.appendChild(document.createElement("br")!!)
        element.appendChild(document.createTextNode(oo2.result)!!)
        element.appendChild(document.createElement("br")!!)


        val cloner = ModelCloner();
        val clonedRoot = cloner.clone(root);
        val oo3 = OutputStream()
        saver.serialize(clonedRoot!!,oo3)
        element.appendChild(document.createTextNode("After cloned in browser")!!)
        element.appendChild(document.createElement("br")!!)
        element.appendChild(document.createTextNode(oo3.result)!!)
        element.appendChild(document.createElement("br")!!)

        return root;

    }
    return null;
}

    /*

fun tesIt(){

}

class It : ListIterator<String> {
    public override fun hasNext(): Boolean {
        throw UnsupportedOperationException()
    }
    public override fun next(): String {
        throw UnsupportedOperationException()
    }
    public override fun hasPrevious(): Boolean {
        throw UnsupportedOperationException()
    }
    public override fun previous(): String {
        throw UnsupportedOperationException()
    }
    public override fun nextIndex(): Int {
        throw UnsupportedOperationException()
    }
    public override fun previousIndex(): Int {
        throw UnsupportedOperationException()
    }

} */

/*
class Mut : MutableIterator<String> {
    public override fun remove() {
        throw UnsupportedOperationException()
    }
    public override fun next(): String {
        throw UnsupportedOperationException()
    }
    public override fun hasNext(): Boolean {
        throw UnsupportedOperationException()
    }

}   */