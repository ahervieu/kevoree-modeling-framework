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

import org.eclipse.emf.ecore.xmi.XMIResource
import scala.collection.JavaConversions._
import java.io.{File, PrintWriter}
import org.eclipse.emf.ecore.{EEnum, EReference, EPackage, EClass}
import org.kevoree.modeling.kotlin.generator.{GenerationContext, ProcessorHelper}

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 02/10/11
 * Time: 20:55
 */

class SerializerGenerator(ctx: GenerationContext) {

  def generateSerializer(model: XMIResource) {

    val serializerGenBaseDir = ctx.getBaseLocationForUtilitiesGeneration.getAbsolutePath + File.separator + "serializer" + File.separator
    ProcessorHelper.checkOrCreateFolder(serializerGenBaseDir)
    val genFile = new File(serializerGenBaseDir + "ModelSerializer.kt")
    val pr = new PrintWriter(genFile, "utf-8")
    beginSerializer(pr)

    var rootClass : EClass = null
    model.getContents.foreach{ pack =>
      if(pack.isInstanceOf[EPackage]) {
        ctx.getRootContainerInPackage(pack.asInstanceOf[EPackage]) match {
          case Some(cls: EClass) => {
            rootClass = cls
            generateDefaultSerializer(pr, rootClass)
          }
          case None => {}
        }
      } else {
        println("Don't know what to do for serializing element " + pack.getClass + " at the root of the metamodel.")
      }
    }

    model.getContents.foreach{ pack =>
      if(pack.isInstanceOf[EPackage]) {
        generateSerializer(rootClass, pack.asInstanceOf[EPackage], pr)
      }
    }

    endSerializer(pr)
    pr.flush()
    pr.close()
  }

  private def beginSerializer(pr:PrintWriter) {

    pr.println("package " + ProcessorHelper.fqn(ctx,ctx.getBasePackageForUtilitiesGeneration) + ".serializer")
    pr.println("class ModelSerializer")
    pr.println("{")
    pr.println()

  }

  private def endSerializer(pr:PrintWriter) {
    pr.println("}") //END TRAIT
  }

  private def generateDefaultSerializer(pr : PrintWriter, root: EClass) {

    pr.println("fun serialize(oMS : Any,ostream : java.io.OutputStream) {")
    pr.println()
    pr.println("when(oMS) {")
    pr.println("is " + ProcessorHelper.fqn(ctx, root) + " -> {")
    pr.println("val context = get" + root.getName + "XmiAddr(oMS,\"/\")")
    pr.println("val wt = java.io.PrintStream(java.io.BufferedOutputStream(ostream),false)")
    pr.println("" + root.getName + "toXmi(oMS,context,wt)")
    pr.println("wt.flush()")
    pr.println("wt.close()")
    pr.println("}")
    pr.println("else -> { }")
    pr.println("}") //END MATCH
    pr.println("}") //END serialize method

  }

  private def generateSerializer(root: EClass, rootXmiPackage: EPackage, pr: PrintWriter) {
    rootXmiPackage.getEClassifiers.foreach {
      eClass =>
        if(eClass.isInstanceOf[EClass]){
          //generateSerializationMethods(rootXmiPackage, eClass.asInstanceOf[EClass], pr, eClass.getName,eClass == root)
          generateSerializationMethods(rootXmiPackage, eClass.asInstanceOf[EClass], pr, eClass == root)
        }
    }
    rootXmiPackage.getESubpackages.foreach{subpackage =>
      generateSerializer(root, subpackage, pr)
    }
  }

  private def getGetter(name: String): String = {
    "get" + name.charAt(0).toUpper + name.substring(1)
  }

  private def generateRef(buffer:PrintWriter, cls:EClass, ref: EReference) {
    buffer.println("val subsub" + ref.getName + " = selfObject." + getGetter(ref.getName) + "()")
    buffer.println("if(subsub" + ref.getName + " != null){")
    buffer.println("val subsubsub" + ref.getName + " = addrs.get(subsub" + ref.getName + ")")
    buffer.println("if(subsubsub" + ref.getName + " != null){")
    buffer.println("ostream.print(\" " + ref.getName + "=\\\"\"+subsubsub" + ref.getName + "+\"\\\"\")")
    buffer.println("} else {")
    buffer.println("throw Exception(\"KMF " + cls.getName + " Serialization error : No address found for reference " + ref.getName + "(id:\"+subsub" + ref.getName + "+\" container:\"+subsub" + ref.getName + ".eContainer()+\")\")")
    buffer.println("}")
    buffer.println("}")
  }

  def generateGetXmiAddrMethod(buffer:PrintWriter, cls:EClass, subTypes : List[EClass]) {
    buffer.println("fun get" + cls.getName + "XmiAddr(selfObject : " + ProcessorHelper.fqn(ctx, cls) + ",previousAddr : String): Map<Any,String> {")
    buffer.println("var subResult = java.util.HashMap<Any,String>()")
    buffer.println("if(previousAddr == \"/\"){ subResult.put(selfObject,\"/\") }\n")
    if (cls.getEAllContainments.filter(subClass => subClass.getUpperBound == -1).size > 0) {
      buffer.println("var i = 0")
    }
    var firstUsed = true
    cls.getEAllContainments.foreach {
      subClass =>
        subClass.getUpperBound match {
          case 1 => {
            //if (subClass.getLowerBound == 0) {
            buffer.println("val sub" + subClass.getName + " = selfObject." + getGetter(subClass.getName) + "()")
            buffer.println("if(sub" + subClass.getName + "!= null){")
            buffer.println("subResult.put(sub" + subClass.getName + ",previousAddr+\"/@" + subClass.getName + "\" )")
            buffer.println("subResult.putAll(get" + subClass.getEReferenceType.getName + "XmiAddr(sub" + subClass.getName + ",previousAddr+\"/@" + subClass.getName + "\"))")
            buffer.println("}")
          }
          case -1 => {
            if (!firstUsed) {
              buffer.println("i=0")
            }
            firstUsed = false
            buffer.println("for(sub in selfObject." + getGetter(subClass.getName) + "()){")
            buffer.println("subResult.put(sub,(previousAddr+\"/@" + subClass.getName + ".\"+i))")
            buffer.println("subResult.putAll(get" + subClass.getEReferenceType.getName + "XmiAddr(sub,previousAddr+\"/@" + subClass.getName + ".\"+i))")
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
        subType =>
          buffer.println("is " + ProcessorHelper.fqn(ctx, subType) + " -> { subResult.putAll(get" + subType.getName + "XmiAddr(selfObject as " + ProcessorHelper.fqn(ctx, subType) + ",previousAddr)) }")
      }
      buffer.println("else -> {}") //throw new InternalError(\""+ cls.getName +"Serializer did not match anything for selfObject class name: \" + selfObject.getClass.getName)")
      buffer.println("}")
    }
    buffer.println("return subResult")
    buffer.println("}")
  }


  private def generateToXmiMethod(buffer:PrintWriter, cls:EClass, isRoot : Boolean = false) {
    if (isRoot) {
      buffer.println("fun " + cls.getName + "toXmi(selfObject : " + ProcessorHelper.fqn(ctx, cls) + ", addrs : Map<Any,String>, ostream : java.io.PrintStream) {")
    } else {
      buffer.println("fun " + cls.getName + "toXmi(selfObject : " + ProcessorHelper.fqn(ctx, cls) + ",refNameInParent : String, addrs : Map<Any,String>, ostream : java.io.PrintStream) {")
    }
    buffer.println("when(selfObject) {")
    val subtypesList = ProcessorHelper.getDirectConcreteSubTypes(cls)
    subtypesList.foreach {
      subType =>
        buffer.println("is " + ProcessorHelper.fqn(ctx, subType) + " -> {" + subType.getName + "toXmi(selfObject as " + ProcessorHelper.fqn(ctx, subType) + ",refNameInParent,addrs,ostream) }")
    }
    buffer.println("else -> {")
    if (isRoot) {
      buffer.println("ostream.println(\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\")")
    }
    buffer.println("ostream.print('<')")
    if (!isRoot) {
      buffer.println("ostream.print(refNameInParent)")
    } else {
      buffer.println("ostream.print(\"" + cls.getName + "\")")
    }
    if (isRoot || cls.getEAllAttributes.size() > 0 || cls.getEAllReferences.filter(eref => !cls.getEAllContainments.contains(eref)).size > 0) {
      if (isRoot) {
        buffer.println("ostream.print(\" xmlns:" + cls.getEPackage.getNsPrefix + "=\\\"" + cls.getEPackage.getNsURI + "\\\"\")")
        buffer.println("ostream.print(\" xmlns:xsi=\\\"http://wwww.w3.org/2001/XMLSchema-instance\\\"\")")
        buffer.println("ostream.print(\" xmi:version=\\\"2.0\\\"\")")
        buffer.println("ostream.print(\" xmlns:xmi=\\\"http://www.omg.org/XMI\\\"\")")
      }
      buffer.println("ostream.print(\" xsi:type=\\\"" + cls.getEPackage.getName + ":" + cls.getName + "\\\"\")")
      cls.getEAllAttributes.foreach {
        att =>
          att.getUpperBound match {
            case 1 => {
              att.getLowerBound match {
                case _ => {
                  if (att.getEAttributeType.isInstanceOf[EEnum]) {
                    buffer.println("if(selfObject." + getGetter(att.getName) + "() != null){")
                    buffer.println("ostream.print(\" " + att.getName + "=\\\"\"+selfObject." + getGetter(att.getName) + "()!!.name()+\"\\\"\")")
                    buffer.println("}")
                  } else {
                    buffer.println("if(selfObject." + getGetter(att.getName) + "().toString() != \"\"){")
                    buffer.println("ostream.print(\" " + att.getName + "=\\\"\"+selfObject." + getGetter(att.getName) + "()+\"\\\"\")")
                    buffer.println("}")
                  }
                }
              }
            }
            case -1 => println("WTF! " + att.getName)
          }
      }

      cls.getEAllReferences.filter(eref => !cls.getEAllContainments.contains(eref)).foreach {
        ref =>
          ref.getUpperBound match {
            case 1 => {
              ref.getLowerBound match {
                case 0 => {
                  generateRef(buffer, cls, ref)
                }
                case 1 => {
                  if (ref.getEOpposite != null) {
                    if (ref.getEOpposite.getUpperBound != -1) {
                      generateRef(buffer, cls, ref)
                    } else {
                      //OPTIMISATION, WE DON'T SAVE BOTH REFERENCE
                      //WARNING ECLIPSE COMPAT VERIFICATION
                    }
                  } else {
                    generateRef(buffer, cls, ref)
                  }
                }
              }
            }
            case _ => {
              //buffer.println("var subadrs" + ref.getName + " : List[String] = List()")
              buffer.println("if(selfObject." + getGetter(ref.getName) + "().size() > 0){")
              buffer.println("ostream.print(\" " + ref.getName + "=\\\"\")")
              buffer.println("var firstItLoop = true")
              buffer.println("for(sub in selfObject." + getGetter(ref.getName) + "()){")
              buffer.println("if(!firstItLoop){ostream.print(\" \")}")
              buffer.println("val subsub = addrs.get(sub)")
              buffer.println("if(subsub != null){")
              buffer.println("ostream.print(subsub)")
              buffer.println("} else {")
              buffer.println("throw Exception(\"KMF Serialization error : non contained reference " + cls.getName + "/" + ref.getName + " \")")
              buffer.println("}")
              buffer.println("firstItLoop=false")
              buffer.println("}")
              buffer.println("ostream.print(\"\\\"\")")
              buffer.println("}")
            }
          }
      }
    }
    buffer.println("ostream.print('>')")
    buffer.println("ostream.println()")
    cls.getEAllContainments.foreach {
      subClass =>
        subClass.getUpperBound match {
          case 1 => {
            if (subClass.getLowerBound == 0) {
              buffer.println("val sub" + subClass.getName + " = selfObject." + getGetter(subClass.getName) + "()")
              buffer.println("if(sub" + subClass.getName + "!= null){")
              buffer.println("" + subClass.getEReferenceType.getName + "toXmi(sub" + subClass.getName + ",\"" + subClass.getName + "\",addrs,ostream)")
              buffer.println("}")
            } else {
              buffer.println("" + subClass.getEReferenceType.getName + "toXmi(selfObject." + getGetter(subClass.getName) + "()!!,\"" + subClass.getName + "\",addrs,ostream)")
            }
          }
          case -1 => {
            buffer.println("for(so in selfObject." + getGetter(subClass.getName) + "()){")
            buffer.println("" + subClass.getEReferenceType.getName + "toXmi(so,\"" + subClass.getName + "\",addrs,ostream)")
            buffer.println("}")
          }
        }
    }
    //Close Tag
    buffer.println("ostream.print(\"</\")")
    if (!isRoot) {
      buffer.println("ostream.print(refNameInParent)")
    } else {
      buffer.println("ostream.print(\"" + cls.getName + "\")")
    }
    buffer.println("ostream.print('>')")
    buffer.println("ostream.println()")

    buffer.println("}")
    buffer.println("}") //End MATCH CASE
    buffer.println("}") //END TO XMI
  }

  private def generateSerializationMethods(pack: EPackage, cls: EClass, buffer: PrintWriter, isRoot: Boolean = false) = {
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

    //GENERATE GET XMI ADDR
    //System.out.println("[DEBUG] SerializerGen::" + cls)
    generateGetXmiAddrMethod(buffer, cls, subTypes)

    generateToXmiMethod(buffer, cls, isRoot)
    if(isRoot) {
      generateToXmiMethod(buffer, cls, false)
    }


  }
}