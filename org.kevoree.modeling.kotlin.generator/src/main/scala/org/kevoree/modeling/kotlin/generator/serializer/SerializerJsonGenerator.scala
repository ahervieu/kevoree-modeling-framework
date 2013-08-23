/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 * Fouquet Francois
 * Nain Gregory
 */
/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 * Fouquet Francois
 * Nain Gregory
 */

package org.kevoree.modeling.kotlin.generator.serializer

import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import org.apache.velocity.VelocityContext
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.xmi.XMIResource
import scala.collection.JavaConversions._
import java.io.{File, PrintWriter}
import org.eclipse.emf.ecore._
import org.kevoree.modeling.kotlin.generator.{GenerationContext, ProcessorHelper}
import java.util
import scala.Tuple2

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 02/10/11
 * Time: 20:55
 */

class SerializerJsonGenerator(ctx: GenerationContext) {

  def generateJsonSerializer(pack: EPackage, model: ResourceSet) {

    if (ctx.getJS()) {
      generateStaticJavaClass()
    }

    val serializerGenBaseDir = ctx.getBaseLocationForUtilitiesGeneration.getAbsolutePath + File.separator + "serializer" + File.separator
    ProcessorHelper.checkOrCreateFolder(serializerGenBaseDir)
    generateDefaultSerializer(serializerGenBaseDir, model)
  }


  private def generateStaticJavaClass() {
    val basePath = ctx.getRootGenerationDirectory + File.separator + "java" + File.separator + "io"
    ProcessorHelper.checkOrCreateFolder(basePath)

    val genOutputStreamFile = new File(basePath + File.separator + "OutputStream.kt")
    val OutputStream = new PrintWriter(genOutputStreamFile, "utf-8")
    val genPrintStreamFile = new File(basePath + File.separator + "PrintStream.kt")
    val PrintStream = new PrintWriter(genPrintStreamFile, "utf-8")

    val ve = new VelocityEngine()
    ve.setProperty("file.resource.loader.class", classOf[ClasspathResourceLoader].getName())
    ve.init()
    val template1 = ve.getTemplate("templates/jsIO/java.io.OutputStream.vm")
    val ctxV = new VelocityContext()
    template1.merge(ctxV, OutputStream)
    OutputStream.flush()
    OutputStream.close()

    val template2 = ve.getTemplate("templates/jsIO/java.io.PrintStream.vm")
    template2.merge(ctxV, PrintStream)
    PrintStream.flush()
    PrintStream.close()

  }

  private def generateEscapeMethod(pr: PrintWriter) {
    val ve = new VelocityEngine()
    ve.setProperty("file.resource.loader.class", classOf[ClasspathResourceLoader].getName())
    ve.init()
    val template = ve.getTemplate("templates/SerializerEscapeJSON.vm")
    val ctxV = new VelocityContext()
    template.merge(ctxV, pr)
  }


  private def generateDefaultSerializer(genDir: String, model: ResourceSet) {
    val genFile = new File(genDir + "JSONModelSerializer.kt")
    val pr = new PrintWriter(genFile, "utf-8")
    pr.println("package " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".serializer")
    pr.println("class JSONModelSerializer : org.kevoree.modeling.api.ModelSerializer ")
    pr.println("{")
    pr.println()

    pr.println("override fun serialize(oMS : Any) : String? {")

    if(ctx.getJS()){
      pr.println("val oo = java.io.OutputStream()")
    } else {
      pr.println("val oo = java.io.ByteArrayOutputStream()")
    }
    pr.println("serialize(oMS,oo)")
    pr.println("oo.flush()")
    if(ctx.getJS()){
      pr.println("return oo.result")
    } else {
      pr.println("return oo.toString()")
    }
    pr.println("}")


    pr.println("override fun serialize(oMS : Any,ostream : java.io.OutputStream) {")
    pr.println()
    if (ctx.getJS()) {
      pr.println("val wt = java.io.PrintStream(ostream)")
    } else {
      pr.println("val wt = java.io.PrintStream(java.io.BufferedOutputStream(ostream),false)")
    }
    pr.println("when(oMS) {")
    ProcessorHelper.collectAllClassifiersInModel(model).filter(proot => !proot.isInstanceOf[EEnum]).foreach {
      root =>
        if (ctx.getJS()) {
          //KotLin bug Workaround : http://youtrack.jetbrains.com/issue/KT-3519
          pr.println("is " + ProcessorHelper.fqn(ctx, root) + ", is " + ProcessorHelper.fqn(ctx, root.getEPackage) + ".impl." + root.getName + "Impl -> {")
        } else {
          pr.println("is " + ProcessorHelper.fqn(ctx, root) + " -> {")
        }
        pr.println("val context = get" + root.getName + "JsonAddr(oMS as " + ProcessorHelper.fqn(ctx, root) + ")")
        pr.println("" + root.getName + "toJson(oMS,context,wt)")
        pr.println("}")
    }
    pr.println("else -> { }")
    pr.println("}") //END MATCH
    pr.println("wt.flush()")
    //pr.println("wt.close()")
    pr.println("}") //END serialize method
    generateEscapeMethod(pr)
    getEAllEclass(model,ctx).foreach {
      eClass => {
          generateToJsonMethod(eClass, pr)
      }
    }
    pr.println("}") //END TRAIT
    pr.flush()
    pr.close()
  }

  private def getGetter(name: String): String = {
    "get" + name.charAt(0).toUpper + name.substring(1)
  }

  /*
  private def generateEENUMToJsonMethod(cls: EEnum, buffer: PrintWriter) {
    buffer.println("fun get" + cls.getName + "JsonAddr(selfObject : " + ProcessorHelper.fqn(ctx, cls) + ",previousAddr : String): Map<Any,String> {")
    buffer.println("ostream.print('{')")
    buffer.println("ostream.print(\" \\\"eClass\\\":\\\"" + ProcessorHelper.fqn(ctx, cls.getEPackage) + ":" + cls.getName + "\\\" \")")
    buffer.println("ostream.print('}')") // end JSON element
    buffer.println("}")
  } */

  private def generateToJsonMethod(cls: EClass, buffer: PrintWriter) {
    var stringListSubSerializers = Set[Tuple2[String, String]]()
    if (cls.getEAllContainments.size > 0) {
      cls.getEAllContainments.foreach {
        contained =>
          if (contained.getEReferenceType != cls) {
            stringListSubSerializers = stringListSubSerializers ++ Set(Tuple2(ProcessorHelper.fqn(ctx, contained.getEReferenceType), contained.getEReferenceType.getName)) // + "Serializer")
          }
      }
    }
    val subTypes = ProcessorHelper.getDirectConcreteSubTypes(cls)
    if (subTypes.size > 0) {
      subTypes.foreach {
        sub =>
          stringListSubSerializers = stringListSubSerializers ++ Set(Tuple2(ProcessorHelper.fqn(ctx, sub), sub.getName)) // + "Serializer")
      }
    }
    //GENERATE GET Json ADDR                                                                              0
    buffer.println("private fun get" + cls.getName + "JsonAddr(selfObject : " + ProcessorHelper.fqn(ctx, cls) + "): Map<Any,String> {")
    buffer.println("var subResult = java.util.HashMap<Any,String>()")
    //buffer.println("if(previousAddr == \"/\"){ subResult.put(selfObject,\"//\") }\n")

    if (cls.getEAllContainments.filter(subClass => subClass.getUpperBound == -1).size > 0) {
      buffer.println("var i = 0")
    }

    var firstUsed = true

    cls.getEAllContainments.foreach {
      subClass =>
        subClass.getUpperBound match {
          case 1 => {
            buffer.println("val sub" + subClass.getName + " = selfObject." + getGetter(subClass.getName) + "()")
            buffer.println("if(sub" + subClass.getName + "!= null){")

            //buffer.println("val subPath_" + subClass.getName + "=sub" + subClass.getName + ".path()")
            //buffer.println("if(subPath_" + subClass.getName + "!=null){")
            buffer.println("subResult.put(sub" + subClass.getName + ",sub" + subClass.getName + ".path()!!)")
            //buffer.println("} else {")
            //buffer.println("subResult.put(sub" + subClass.getName + ",previousAddr+\"/@" + subClass.getName + "\" )")
            //buffer.println("}")

            buffer.println("subResult.putAll(get" + subClass.getEReferenceType.getName + "JsonAddr(sub" + subClass.getName + "))")
            buffer.println("}")
          }
          case _ if(subClass.getUpperBound == -1 || subClass.getUpperBound > 1) => {
            if (!firstUsed) {
              buffer.println("i=0")
            }
            firstUsed = false
            buffer.println("for(sub in selfObject." + getGetter(subClass.getName) + "()){")

            buffer.println("val subPath_" + subClass.getName + "=sub.path()")
            //buffer.println("if(subPath_" + subClass.getName + "!=null){")
            buffer.println("subResult.put(sub,sub.path()!!)")
            //buffer.println("} else {")
            //buffer.println("subResult.put(sub,(previousAddr+\"/@" + subClass.getName + ".\"+i))")
            //buffer.println("}")

            buffer.println("subResult.putAll(get" + subClass.getEReferenceType.getName + "JsonAddr(sub))")
            buffer.println("i=i+1")
            buffer.println("}")
            buffer.println()
          }
        }
    }

    buffer.println()
    if (subTypes.size > 0) {
      buffer.println("when(selfObject) {")
      subTypes.foreach {
        subType => {

          if (ctx.getJS()) {
            //KotLin bug workaround : http://youtrack.jetbrains.com/issue/KT-3519
            buffer.println("is " + ProcessorHelper.fqn(ctx, subType) + ", is " + ProcessorHelper.fqn(ctx, subType.getEPackage) + ".impl." + subType.getName + "Impl -> {")
          } else {
            buffer.println("is " + ProcessorHelper.fqn(ctx, subType) + " -> {")
          }

          buffer.println("subResult.putAll(get" + subType.getName + "JsonAddr(selfObject as " + ProcessorHelper.fqn(ctx, subType) + "))")
          buffer.println("}")
        }
      }
      buffer.println("else -> {}") //throw new InternalError(\""+ cls.getName +"Serializer did not match anything for selfObject class name: \" + selfObject.getClass.getName)")
      buffer.println("}")
    }
    buffer.println("return subResult")
    buffer.println("}")

    buffer.println("fun " + cls.getName + "toJson(selfObject : " + ProcessorHelper.fqn(ctx, cls) + ", addrs : Map<Any,String>, ostream : java.io.PrintStream) {")

    buffer.println("when(selfObject) {")
    val subtypesList = ProcessorHelper.getDirectConcreteSubTypes(cls)
    subtypesList.foreach {
      subType =>
        if (ctx.getJS()) {
          buffer.println("is " + ProcessorHelper.fqn(ctx, subType) + ", is " + ProcessorHelper.fqn(ctx, subType.getEPackage) + ".impl." + subType.getName + "Impl -> {" + subType.getName + "toJson(selfObject as " + ProcessorHelper.fqn(ctx, subType) + ",addrs,ostream) }")
        } else {
          buffer.println("is " + ProcessorHelper.fqn(ctx, subType) + " -> {" + subType.getName + "toJson(selfObject as " + ProcessorHelper.fqn(ctx, subType) + ",addrs,ostream) }")
        }
    }
    buffer.println("else -> {")
    buffer.println("ostream.print('{')")
    buffer.println("ostream.print(\" \\\"eClass\\\":\\\"" + ProcessorHelper.fqn(ctx, cls.getEPackage) + ":" + cls.getName + "\\\" \")")
    if (cls.getEAllAttributes.size() > 0 || cls.getEAllReferences.filter(eref => !cls.getEAllContainments.contains(eref)).size > 0) {
      cls.getEAllAttributes.foreach {
        att =>
          att.getUpperBound match {
            case 1 => {
              att.getLowerBound match {
                case _ => {
                  if (att.getEAttributeType.isInstanceOf[EEnum]) {
                    buffer.println("if(selfObject." + getGetter(att.getName) + "() != null){")
                    buffer.println("ostream.println(',')")
                    buffer.println("ostream.print(\" \\\"" + att.getName + "\\\":\\\"\"+selfObject." + getGetter(att.getName) + "()!!.name()+\"\\\"\")")
                    buffer.println("}")
                  } else {
                    buffer.println("if(selfObject." + getGetter(att.getName) + "().toString() != \"\"){")
                    buffer.println("ostream.println(',')")

                    buffer.println("ostream.print(\" \\\"" + att.getName + "\\\":\")")
                    if (!ProcessorHelper.convertType(att.getEAttributeType,ctx).equals("Boolean")) {
                      buffer.println("ostream.print(\"\\\"\")")
                    }
                    if (ProcessorHelper.convertType(att.getEAttributeType,ctx) == "String") {
                      buffer.println("escapeJson(ostream, selfObject." + getGetter(att.getName) + "())")
                    } else {
                      buffer.println("ostream.print(selfObject." + getGetter(att.getName) + "())")
                    }
                    if (!ProcessorHelper.convertType(att.getEAttributeType,ctx).equals("Boolean")) {
                      buffer.println("ostream.print('\"')")
                    }
                    buffer.println("}")
                  }
                }
              }
            }
            case -1 => println("WTF! " + att.getName)
          }
      }


      def generateRef(ref: EReference) {
        buffer.println("val subsub" + ref.getName + " = selfObject." + getGetter(ref.getName) + "()")
        buffer.println("if(subsub" + ref.getName + " != null){")
        buffer.println("val subsubsub" + ref.getName + " = addrs.get(subsub" + ref.getName + ")")
        buffer.println("if(subsubsub" + ref.getName + " != null){")
        buffer.println("ostream.println(',')")
        buffer.println("ostream.print(\" \\\"" + ref.getName + "\\\":\\\"\"+subsubsub" + ref.getName + "+\"\\\"\")")
        buffer.println("} else {")
        buffer.println("throw Exception(\"KMF " + cls.getName + " Serialization error : No address found for reference " + ref.getName + "(id:\"+subsub" + ref.getName + "+\" container:\"+subsub" + ref.getName + ".eContainer()+\")\")")
        buffer.println("}")
        buffer.println("}")
      }

      cls.getEAllReferences.filter(eref => !cls.getEAllContainments.contains(eref)).foreach {
        ref =>
          ref.getUpperBound match {
            case 1 => {
              ref.getLowerBound match {
                case 0 => {
                  generateRef(ref)
                }
                case 1 => {
                  if (ref.getEOpposite != null) {
                    if (ref.getEOpposite.getUpperBound != -1) {
                      generateRef(ref)
                    } else {
                      //OPTIMISATION, WE DON'T SAVE BOTH REFERENCE
                      //WARNING ECLIPSE COMPAT VERIFICATION
                    }
                  } else {
                    generateRef(ref)
                  }
                }
              }
            }
            case _ => {
              buffer.println("if(selfObject." + getGetter(ref.getName) + "().size() > 0){")
              buffer.println("ostream.println(',')")
              buffer.println("ostream.print(\" \\\"" + ref.getName + "\\\": [\")")
              buffer.println("var firstItLoop = true")
              buffer.println("for(sub in selfObject." + getGetter(ref.getName) + "()){")
              buffer.println("if(!firstItLoop){ostream.println(\",\")}")
              buffer.println("val subsub = addrs.get(sub)")
              buffer.println("if(subsub != null){")
              buffer.println("ostream.print('\"')")
              buffer.println("ostream.print(subsub)")
              buffer.println("ostream.print('\"')")
              buffer.println("} else {")
              buffer.println("throw Exception(\"KMF Serialization error : non contained reference " + cls.getName + "/" + ref.getName + " \")")
              buffer.println("}")
              buffer.println("firstItLoop=false")
              buffer.println("}")
              buffer.println("ostream.print(\"]\")")
              buffer.println("}")
            }
          }
      }
    }
    cls.getEAllContainments.foreach {
      subClass =>
        subClass.getUpperBound match {
          case 1 => {
            if (subClass.getLowerBound == 0) {
              buffer.println("val sub" + subClass.getName + " = selfObject." + getGetter(subClass.getName) + "()")
              buffer.println("if(sub" + subClass.getName + "!= null){")
              buffer.println("ostream.println(',')")
              buffer.println("ostream.print(\"\\\"" + subClass.getName + "\\\":\")")
              buffer.println("" + subClass.getEReferenceType.getName + "toJson(sub" + subClass.getName + ",addrs,ostream)")
              buffer.println("}")
            } else {
              buffer.println("ostream.println(',')")
              buffer.println("ostream.println(\"\\\"" + subClass.getName + "\\\":\")")
              buffer.println("" + subClass.getEReferenceType.getName + "toJson(selfObject." + getGetter(subClass.getName) + "()!!,addrs,ostream)")
            }
          }
          case _ if(subClass.getUpperBound == -1 || subClass.getUpperBound > 1) => {
            buffer.println("if(selfObject." + getGetter(subClass.getName) + "().size() > 0){")
            buffer.println("ostream.println(',')")
            buffer.println("ostream.println(\"\\\"" + subClass.getName + "\\\": [\")")
            buffer.println("var iloop_first_" + subClass.getName + " = true")
            buffer.println("for(so in selfObject." + getGetter(subClass.getName) + "()){")
            buffer.println("if(!iloop_first_" + subClass.getName + "){ostream.println(',')}")
            buffer.println("" + subClass.getEReferenceType.getName + "toJson(so,addrs,ostream)")
            buffer.println("iloop_first_" + subClass.getName + " = false")
            buffer.println("}")
            buffer.println("ostream.println(']')")
            buffer.println("}")
          }
        }
    }
    //Close Tag
    buffer.println("ostream.println('}')")
    buffer.println("}")
    buffer.println("}") //End MATCH CASE
    buffer.println("}") //END TO Json
  }

  def getEAllEclass(pack: ResourceSet, ctx: GenerationContext): util.Collection[EClass] = {
    val result = new util.HashMap[String,EClass]()
    pack.getAllContents.foreach {
      eclass =>

        if (eclass.isInstanceOf[EClass] && !result.containsKey(ProcessorHelper.fqn(ctx,eclass.asInstanceOf[EClass]))) {
          result.put(ProcessorHelper.fqn(ctx,eclass.asInstanceOf[EClass]),eclass.asInstanceOf[EClass])
        }
    }
    return result.values()
  }


}