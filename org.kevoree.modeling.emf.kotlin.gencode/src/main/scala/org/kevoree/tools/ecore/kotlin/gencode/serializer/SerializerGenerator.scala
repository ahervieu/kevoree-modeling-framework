/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 * 	Fouquet Francois
 * 	Nain Gregory
 */

package org.kevoree.tools.ecore.kotlin.gencode.serializer

import scala.collection.JavaConversions._
import java.io.{File, PrintWriter}
import org.eclipse.emf.ecore.{EReference, EPackage, EClass}
import org.kevoree.tools.ecore.kotlin.gencode.{GenerationContext, ProcessorHelper}

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 02/10/11
 * Time: 20:55
 */

class SerializerGenerator(ctx: GenerationContext) {


  def generateSerializer(pack: EPackage) {

    val serializerGenBaseDir = ProcessorHelper.getPackageGenDir(ctx, pack) + "/serializer/"
    ProcessorHelper.checkOrCreateFolder(serializerGenBaseDir)

    val modelPackage = ProcessorHelper.fqn(ctx, pack)


    ctx.getRootContainerInPackage(pack) match {
      case Some(cls: EClass) => {
        val subs = generateSerializer(serializerGenBaseDir, modelPackage, pack.getName + ":" + cls.getName, cls, pack, true)
        generateDefaultSerializer(serializerGenBaseDir, modelPackage, cls, pack, subs)
      }
      case None => throw new UnsupportedOperationException("Root container not found. Returned one.")
    }
  }


  private def generateDefaultSerializer(genDir: String, packageName: String, root: EClass, rootXmiPackage: EPackage, sub: Set[String]) {

    val genFile = new File(genDir + "ModelSerializer.kt")
    val pr = new PrintWriter(genFile, "utf-8")
    pr.println("package " + packageName + ".serializer")
    pr.println("class ModelSerializer ")

    if (sub.size > 0) {
      pr.print(sub.mkString(" : ", " , ", " "))
    }

    pr.println("{")

    pr.println()
    pr.println("fun serialize(o : Any,ostream : java.io.OutputStream) {")
    pr.println()
    pr.println("when(o) {")
    pr.println("is " + ProcessorHelper.fqn(ctx, root) + " -> {")
    pr.println("val context = get" + root.getName + "XmiAddr(o,\"/\")")
    pr.println("val wt = java.io.PrintStream(java.io.BufferedOutputStream(ostream),false)")
    pr.println("" + root.getName + "toXmi(o,context,wt)")
    pr.println("wt.flush()")
    pr.println("wt.close()")
    pr.println("}")
    pr.println("else -> { }")
    pr.println("}") //END MATCH
    pr.println("}") //END serialize method
    pr.println("}") //END TRAIT
    pr.flush()
    pr.close()


  }


  private def generateSerializer(genDir: String, packageName: String, refNameInParent: String, root: EClass, rootXmiPackage: EPackage, isRoot: Boolean = false): Set[String] = {
    var subSerializer = Set[String](root.getName + "Serializer")

    // ProcessorHelper.checkOrCreateFolder(genDir + "/serializer")
    //PROCESS SELF
    //System.out.println("[DEBUG] SerializerGenerator::generateSerializer => " + root.getName)

    val file = new File(genDir + root.getName + "Serializer.kt")

    //if(!file.exists()) {
    val pr = new PrintWriter(file, "utf-8")
    pr.println("package " + packageName + ".serializer")
    generateToXmiMethod(rootXmiPackage, root, pr, rootXmiPackage.getName + ":" + root.getName, isRoot)
    pr.flush()
    pr.close()



    //PROCESS SUB
    root.getEAllContainments.foreach {
      sub =>
        val subfile = new File(genDir + sub.getEReferenceType.getName + "Serializer.kt")
        if (!subfile.exists()) {
          val subpr = new PrintWriter(subfile, "utf-8")
          subpr.println("package " + packageName + ".serializer")
          generateToXmiMethod(rootXmiPackage, sub.getEReferenceType, subpr, sub.getName)
          subpr.flush()
          subpr.close()



          //¨PROCESS ALL SUB TYPE
          ProcessorHelper.getAllConcreteSubTypes(sub.getEReferenceType).foreach {
            subsubType =>
              if (subsubType != root) {
                //avoid looping in case of self-containment
                subSerializer = subSerializer ++ generateSerializer(genDir, packageName, sub.getName, subsubType, rootXmiPackage)
              }
          }
          if (sub.getEReferenceType != root) {
            //avoid looping in case of self-containment
            subSerializer = subSerializer ++ generateSerializer(genDir, packageName, sub.getName, sub.getEReferenceType, rootXmiPackage)
          }
        }

    }
    // }

    subSerializer

  }


  private def getGetter(name: String): String = {
    "get" + name.charAt(0).toUpper + name.substring(1)
  }


  private def generateToXmiMethod(pack: EPackage, cls: EClass, buffer: PrintWriter, refNameInParent: String, isRoot: Boolean = false) = {
    val packageOfModel = ProcessorHelper.fqn(ctx, pack)

    buffer.println("import " + packageOfModel + ".*")
    buffer.println()
    buffer.println("trait " + cls.getName + "Serializer ")


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
    buffer.println("{")
    buffer.println()


    stringListSubSerializers.foreach {
      sub =>
        buffer.println("fun get" + sub._2 + "XmiAddr(o : " + sub._1 + ",previousAddr : String) : Map<Any,String>") //PRINT ABSTRACT USEFULL METHOD
        buffer.println("fun " + sub._2 + "toXmi(o :" + sub._1 + ",refNameInParent : String, addrs : Map<Any,String>, ostream : java.io.PrintStream)")
    }
    buffer.println()
    buffer.println()
    buffer.println()


    //GENERATE GET XMI ADDR                                                                              0
    //System.out.println("[DEBUG] SerializerGen::" + cls)
    buffer.println("fun get" + cls.getName + "XmiAddr(selfObject : " + ProcessorHelper.fqn(ctx, cls) + ",previousAddr : String): Map<Any,String> {")
    buffer.println("var subResult = java.util.HashMap<Any,String>()")
    buffer.println("if(previousAddr == \"/\"){ subResult.put(selfObject,\"/\") }\n")
    buffer.println("var i = 0")
    cls.getEAllContainments.foreach {
      subClass =>
        subClass.getUpperBound match {
          case 1 => {
            //if (subClass.getLowerBound == 0) {
              buffer.println("val sub"+subClass.getName+" = selfObject."+getGetter(subClass.getName)+"()")
              buffer.println("if(sub"+subClass.getName+"!= null){")
              buffer.println("subResult.put(sub"+subClass.getName+",previousAddr+\"/@" + subClass.getName + "\" )")
              buffer.println("subResult.putAll(get" + subClass.getEReferenceType.getName + "XmiAddr(sub"+subClass.getName+",previousAddr+\"/@" + subClass.getName + "\"))")
              buffer.println("}")
            //} else {
            //  buffer.println("subResult.put(selfObject." + getGetter(subClass.getName) + "(),(previousAddr+\"/@" + subClass.getName + "\" ))")
            //  buffer.println("subResult.putAll(get" + subClass.getEReferenceType.getName + "XmiAddr(selfObject." + getGetter(subClass.getName) + "(),previousAddr+\"/@" + subClass.getName + "\"))")
            //  //buffer.println("}")
            //}
          }
          case -1 => {
            buffer.println("i=0")
            buffer.println("selfObject." + getGetter(subClass.getName) + "().forEach{ sub -> ")
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
      buffer.println("else -> {}")//throw new InternalError(\""+ cls.getName +"Serializer did not match anything for selfObject class name: \" + selfObject.getClass.getName)")
      buffer.println("}")
    }
    buffer.println("return subResult")
    buffer.println("}")


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
    if(isRoot){
      buffer.println("ostream.println(\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\")")
    }
    buffer.println("ostream.print('<')")
    if (!isRoot) {
      buffer.println("ostream.print(refNameInParent)")
    } else {
      buffer.println("ostream.print(\"" + refNameInParent + "\")")
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
                  buffer.println("if(selfObject." + getGetter(att.getName) + "().toString() != \"\"){")
                  buffer.println("ostream.print((\" " + att.getName + "=\\\"\"+selfObject." + getGetter(att.getName) + "()+\"\\\"\"))")
                  buffer.println("}")
                }
              }
            }
            case -1 => println("WTF!")
          }
      }


      def generateRef(ref : EReference){

        buffer.println("val subsub"+ref.getName+" = selfObject."+getGetter(ref.getName)+"()")
        buffer.println("if(subsub"+ref.getName+" != null){")

        buffer.println("val subsubsub"+ref.getName+" = addrs.get(subsub"+ref.getName+")")
        buffer.println("if(subsubsub"+ref.getName+" != null){")
        buffer.println("ostream.print((\" " + ref.getName + "=\\\"\"+subsubsub"+ref.getName+"+\"\\\"\"))")
        buffer.println("} else {")
        buffer.println("throw Exception(\"KMF Serialization error : non contained reference "+cls.getName+"/"+ref.getName+" \")")
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

              /*
              ref.getLowerBound match {
                case 0 => {
                  buffer.println("val subsub"+ref.getName+" = selfObject."+getGetter(ref.getName)+"()")
                  buffer.println("if(subsub"+ref.getName+" != null){")
                  buffer.println("ostream.print((\" " + ref.getName + "=\\\"\"+subsub"+ref.getName+"+\"\\\"\"))")
                  buffer.println("} else {")
                  buffer.println("throw Exception(\"KMF Serialization error : non contained reference "+cls.getName+"/"+ref.getName+" \")")
                  buffer.println("}")
                }
                case 1 => {
                  if (ref.getEOpposite != null) {
                    if (ref.getEOpposite.getUpperBound != -1) {
                      buffer.println("val subsub"+ref.getName+" = addrs.get(selfObject." + getGetter(ref.getName) + "())")
                      buffer.println("if(subsub"+ref.getName+"!= null){")
                      buffer.println("ostream.print((\" " + ref.getName + "=\\\"\"+subsub"+ref.getName+"+\"\\\"\"))")
                      buffer.println("} else {")
                      buffer.println("throw Exception(\"KMF Serialization error : non contained reference "+cls.getName+"/"+ref.getName+" \")")
                      buffer.println("}")
                    } else {
                       //OPTIMISATION, WE DON'T SAVE BOTH REFERENCE
                       //WARNING ECLIPSE COMPAT VERIFICATION
                    }
                  } else {
                    buffer.println("val subsub"+ref.getName+" = addrs.get(selfObject." + getGetter(ref.getName) + "())")
                    buffer.println("if(subsub"+ref.getName+"!= null){")
                    buffer.println("ostream.print((\" " + ref.getName + "=\\\"\"+subsub"+ref.getName+"+\"\\\"\"))")
                    buffer.println("} else {")
                    buffer.println("throw Exception(\"KMF Serialization error : non contained reference "+cls.getName+"/"+ref.getName+" \")")
                    buffer.println("}")
                  }
                }
              }*/
            }
            case _ => {
              //buffer.println("var subadrs" + ref.getName + " : List[String] = List()")
              buffer.println("if(selfObject." + getGetter(ref.getName) + "().count() > 0){")
              buffer.println("ostream.print(\" " + ref.getName + "=\\\"\")")
              buffer.println("var firstItLoop = true")
              buffer.println("selfObject." + getGetter(ref.getName) + "().forEach{sub ->")
              buffer.println("if(!firstItLoop){ostream.print(\" \")}")

              buffer.println("val subsub = addrs.get(sub)")
              buffer.println("if(subsub != null){")
              buffer.println("ostream.print(subsub)")
              buffer.println("} else {")
              buffer.println("throw Exception(\"KMF Serialization error : non contained reference "+cls.getName+"/"+ref.getName+" \")")
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
              buffer.println("val sub"+subClass.getName+" = selfObject."+getGetter(subClass.getName)+"()")
              buffer.println("if(sub"+subClass.getName+"!= null){")
              buffer.println("" + subClass.getEReferenceType.getName + "toXmi(sub"+subClass.getName+",\"" + subClass.getName + "\",addrs,ostream)")
              buffer.println("}")
            } else {
              buffer.println("" + subClass.getEReferenceType.getName + "toXmi(selfObject." + getGetter(subClass.getName) + ",\"" + subClass.getName + "\",addrs,ostream)")
            }
          }
          case -1 => {
            buffer.println("selfObject." + getGetter(subClass.getName) + "().forEach { so -> ")
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
      buffer.println("ostream.print(\"" + refNameInParent + "\")")
    }
    buffer.println("ostream.print('>')")
    buffer.println("ostream.println()")

    buffer.println("}")
    buffer.println("}") //End MATCH CASE
    buffer.println("}") //END TO XMI
    buffer.println("}") //END TRAIT
  }


}