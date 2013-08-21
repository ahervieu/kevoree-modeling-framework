

package org.kevoree.modeling.kotlin.generator.model

import java.io.{File, PrintWriter}
import org.kevoree.modeling.kotlin.generator.ProcessorHelper._
import scala.collection.JavaConversions._
import org.eclipse.emf.ecore._
import org.kevoree.modeling.kotlin.generator.{ProcessorHelper, GenerationContext}
import scala.Some
import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * Users: Gregory NAIN, Fouquet Francois
 * Date: 23/09/11
 * Time: 13:35
 */

trait ClassGenerator extends ClonerGenerator {

  var param_suf = "P"

  def generateKMFQLMethods(pr: PrintWriter, cls: EClass, ctx: GenerationContext, pack: String)

  def generateSelectorMethods(pr: PrintWriter, cls: EClass, ctx: GenerationContext)

  def generateEqualsMethods(pr: PrintWriter, cls: EClass, ctx: GenerationContext)

  def generateContainedElementsMethods(pr: PrintWriter, cls: EClass, ctx: GenerationContext)

  def generateDiffMethod(pr: PrintWriter, cls: EClass, ctx: GenerationContext)

  def generateCompanion(ctx: GenerationContext, currentPackageDir: String, packElement: EPackage, cls: EClass, srcCurrentDir: String) {
    val localFile = new File(currentPackageDir + "/impl/" + cls.getName + "Impl.kt")
    val userFile = new File(srcCurrentDir + "/impl/" + cls.getName + "Impl.kt")
    if (userFile.exists()) {
      return
    }

    val pr = new PrintWriter(localFile, "utf-8")
    val pack = ProcessorHelper.fqn(ctx, packElement)
    pr.println("package " + pack + ".impl")
    pr.println()
    pr.println("import " + pack + ".*")
    pr.println()
    pr.print("class " + cls.getName + "Impl(")
    pr.println(") : " + cls.getName + "Internal {")
    //test if generation of variable from Base Trait
    // if (cls.getESuperTypes.isEmpty) {
    //val formatedFactoryName: String = packElement.getName.substring(0, 1).toUpperCase + packElement.getName.substring(1) + "Container"
    pr.println("override internal var internal_eContainer : " + ctx.getKevoreeContainer.get + "? = null")
    pr.println("override internal var internal_containmentRefName : String? = null")
    pr.println("override internal var internal_unsetCmd : " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand? = null")
    pr.println("override internal var internal_readOnlyElem : Boolean = false")
    pr.println("override internal var internal_recursive_readOnlyElem : Boolean = false")
    if (ctx.generateEvents) {
      pr.println("override internal var internal_modelElementListeners : MutableList<org.kevoree.modeling.api.events.ModelElementListener>? = null")
      pr.println("override internal var internal_modelTreeListeners : MutableList<org.kevoree.modeling.api.events.ModelElementListener>? = null")
    }

    //pr.println("override internal var containedIterable : Iterable<"+ctx.getKevoreeContainer.get+">? = null")


    //  }
    val idAttributes = cls.getEAllAttributes.filter(att => att.isID && !att.getName.equals("generated_KMF_ID"))
    val alreadyGeneratedAttributes = new mutable.HashSet[String]()
    cls.getEAllAttributes.foreach {
      att => {
        if (!alreadyGeneratedAttributes.contains(att.getName)) {
          alreadyGeneratedAttributes.add(att.getName)
          var defaultValue = att.getDefaultValueLiteral
          pr.print("override internal var " + "_" + att.getName + " : ")
          var typePre = ""
          var typePost = ""
          if (att.isMany) {
            typePre = "List<"
            typePost = ">"
            defaultValue = "ArrayList<" + ProcessorHelper.convertType(att.getEAttributeType) + ">()"
          }
          ProcessorHelper.convertType(att.getEAttributeType) match {

            case "String" | "java.lang.String" => pr.println(typePre + "String" + typePost + " = " + {
              if (att.getName.equals("generated_KMF_ID") && idAttributes.size == 0) {
                "\"\"+hashCode() + java.util.Date().getTime()"
              } else {
                if (defaultValue == null) {
                  "\"\""
                } else {
                  defaultValue
                }
              }

            })
            case "Double" => pr.println(typePre + "Double" + typePost + " = " + {
              if (defaultValue == null) {
                "0.0"
              } else {
                defaultValue
              }
            })
            case "java.lang.Integer" => pr.println(typePre + "Int" + typePost + " = " + {
              if (defaultValue == null) {
                "0"
              } else {
                defaultValue
              }
            })
            case "Int" => pr.println(typePre + "Int" + typePost + " = " + {
              if (defaultValue == null) {
                "0"
              } else {
                defaultValue
              }
            })

            case "Long" => pr.println(typePre + "Long" + typePost + " = " + {
              if (defaultValue == null) {
                "0"
              } else {
                defaultValue
              }
            })

            case "Boolean" | "java.lang.Boolean" => pr.println(typePre + "Boolean" + typePost + " = " + {
              if (defaultValue == null) {
                "false"
              } else {
                defaultValue
              }
            })
            case "java.lang.Object" | "Any" => pr.println(typePre + "Any?" + typePost + " = null")
            case "java.lang.Class" | "Class" | "Class<out jet.Any?>" => pr.println(typePre + "Class<out jet.Any?>?" + typePost + " = null")
            case "null" => throw new UnsupportedOperationException("ClassGenerator:: Attribute type: " + att.getEAttributeType.getInstanceClassName + " has not been converted in a known type. Can not initialize.")
            case "float" | "Float" => pr.println(typePre + "Float" + typePost + " = " + {
              if (defaultValue == null) {
                "0"
              } else {
                defaultValue
              }
            })
            case "char" | "Char" => pr.println(typePre + "Char" + typePost + " = " + {
              if (defaultValue == null) {
                "a"
              } else {
                defaultValue
              }
            })
            case "java.math.BigInteger" => pr.println(typePre + "java.math.BigInteger" + typePost + " = " + {
              if (defaultValue == null) {
                "java.math.BigInteger.ZERO"
              } else {
                defaultValue
              }
            })
            case _@className => {
              if (att.getEAttributeType.isInstanceOf[EEnum]) {
                pr.println(ProcessorHelper.fqn(ctx, att.getEAttributeType) + "? = null")
              } else {
                pr.println(typePre)
                pr.println(className)
                pr.println(typePost)
                if (defaultValue != null) {
                  pr.println(" = " + defaultValue)
                }
              }
            }
          }
        }
      }
    }
    cls.getEAllReferences.foreach {
      ref =>
        val typeRefName = ProcessorHelper.fqn(ctx, ref.getEReferenceType)
        if (ref.getUpperBound == -1 || ref.getUpperBound > 1) {
          // multiple values
          pr.println("override internal var " + "_" + ref.getName + "_java_cache :List<" + typeRefName + ">? = null")
          if (hasID(ref.getEReferenceType)) {
            pr.println("override internal val " + "_" + ref.getName + " : java.util.HashMap<Any," + typeRefName + "> = java.util.HashMap<Any," + typeRefName + ">()")
          } else {
            pr.println("override internal val " + "_" + ref.getName + " :MutableList<" + typeRefName + "> = java.util.ArrayList<" + typeRefName + ">()")
          }
          if (ctx.generateEvents && ref.isContainment) {
            pr.println("override internal var removeAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1) + "CurrentlyProcessing : Boolean = false")
          }
        } else if (ref.getUpperBound == 1 && ref.getLowerBound == 0) {
          // optional single ref
          pr.println("override internal var " + "_" + ref.getName + " : " + typeRefName + "? = null")
        } else if (ref.getUpperBound == 1 && ref.getLowerBound == 1) {
          // mandatory single ref
          pr.println("override internal var " + "_" + ref.getName + " : " + typeRefName + "? = null")
        } else if (ref.getLowerBound > 1) {
          // else
          pr.println("override internal var " + "_" + ref.getName + "_java_cache :List<" + typeRefName + ">? = null")
          if (hasID(ref.getEReferenceType)) {
            pr.println("override internal val " + "_" + ref.getName + " : java.util.HashMap<Any," + typeRefName + "> = java.util.HashMap<Any," + typeRefName + ">()")
          } else {
            pr.println("override internal val " + "_" + ref.getName + " :MutableList<" + typeRefName + "> = java.util.ArrayList<" + typeRefName + ">()")
          }
        } else {
          throw new UnsupportedOperationException("GenDefConsRef::Not standard arrity: " + cls.getName + "->" + typeRefName + "[" + ref.getLowerBound + "," + ref.getUpperBound + "]. Not implemented yet !")
        }
    }
    pr.println("}")
    pr.flush()
    pr.close()
  }

  def hasID(cls: EClass): Boolean = {
    cls.getEAllAttributes.exists {
      att => att.isID
    }
  }

  /*
  def getIdAtt(cls: EClass) = {
    cls.getEAllAttributes.find {
      att => att.isID
    }
  }

  def generateGetIDAtt(cls: EClass) = {
    if (getIdAtt(cls).isEmpty) {
      println(cls.getName)
    }
    "get" + getIdAtt(cls).get.getName.substring(0, 1).toUpperCase + getIdAtt(cls).get.getName.substring(1)
  }
  */


  def hasFindByIDMethod(cls: EClass): Boolean = {
    cls.getEReferences.exists(ref => {
      hasID(ref.getEReferenceType) && (ref.getUpperBound == -1 || ref.getLowerBound > 1)
    })
  }

  private def getGetter(name: String): String = {
    "get" + name.charAt(0).toUpper + name.substring(1) + "()"
  }

  def generateClass(ctx: GenerationContext, currentPackageDir: String, packElement: EPackage, cls: EClass) {
    val localFile = new File(currentPackageDir + "/impl/" + cls.getName + "Internal.kt")
    val pr = new PrintWriter(localFile, "utf-8")
    val pack = ProcessorHelper.fqn(ctx, packElement)
    pr.println("package " + pack + ".impl")
    pr.println()
    pr.println(generateHeader(packElement))
    //case class name

    ctx.classFactoryMap.put(pack + "." + cls.getName, ctx.packageFactoryMap.get(pack))

    pr.print("trait " + cls.getName + "Internal")
    pr.println((generateSuperTypesPlusSuperAPI(ctx, cls, packElement) match {
      case None => "{"
      case Some(s) => s + " {"
    }))

    //Generate RecursiveReadOnly
    pr.println("override fun setRecursiveReadOnly(){")

    pr.println("if(internal_recursive_readOnlyElem == true){return}")
    pr.println("internal_recursive_readOnlyElem = true")

    cls.getEAllReferences.foreach {
      contained =>
        if (contained.getUpperBound == -1 || contained.getUpperBound > 1) {
          // multiple values
          pr.println("for(sub in this." + getGetter(contained.getName) + "){")
          pr.println("sub.setRecursiveReadOnly()")
          pr.println("}")
        } else if (contained.getUpperBound == 1 /*&& contained.getLowerBound == 0*/ ) {
          // optional single ref
          pr.println("val subsubsubsub" + contained.getName + " = this." + getGetter(contained.getName) + "")
          pr.println("if(subsubsubsub" + contained.getName + "!= null){ ")
          pr.println("subsubsubsub" + contained.getName + ".setRecursiveReadOnly()")
          pr.println("}")
        } else if (contained.getLowerBound > 1) {
          // else
          pr.println("for(sub in this." + getGetter(contained.getName) + "){")
          pr.println("\t\t\tsub.setRecursiveReadOnly()")
          pr.println("\t\t}")
        } else {
          throw new UnsupportedOperationException("ClonerGenerator::Not standard arrity: " + cls.getName + "->" + contained.getName + "[" + contained.getLowerBound + "," + contained.getUpperBound + "]. Not implemented yet !")
        }
        pr.println()
    }
    pr.println("setInternalReadOnly()")

    pr.println("}")

    generateAtts(pr, cls, ctx, pack)
    generateDeleteMethod(pr, cls, ctx, pack)

    // Getters and Setters Generation
    generateAllGetterSetterMethod(pr, cls, ctx, pack)

    //if(cls.getEAllReferences.exists{ c => !c.isContainment }) {
    generateFlatReflexiveSetters(ctx, cls, pr)
    // }

    //GENERATE CLONE METHOD
    generateCloneMethods(ctx, cls, pr, packElement)
    generateKMFQLMethods(pr, cls, ctx, pack)

    if (ctx.genSelector) {
      generateSelectorMethods(pr, cls, ctx)
    }

    generateEqualsMethods(pr, cls, ctx)
    generateContainedElementsMethods(pr, cls, ctx)

    generateMetaClassName(pr, cls, ctx)

    if (ctx.genTrace) {
      generateDiffMethod(pr, cls, ctx)
    }



    pr.println("}")
    pr.flush()
    pr.close()

  }

  def generateFlatReflexiveSetters(ctx: GenerationContext, cls: EClass, pr: PrintWriter) {
    pr.println("override fun reflexiveMutator(mutationType : Int, refName : String, value : Any?) {")
    pr.println("when(refName) {")
    cls.getEAllAttributes.foreach {
      att =>
        pr.println(ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Att_" + att.getName + " -> {") //START ATTR
        pr.println("when(mutationType) {")
        pr.println("org.kevoree.modeling.api.util.ActionType.SET -> {")
        val methodNameClean = "set" + att.getName.substring(0, 1).toUpperCase + att.getName.substring(1)

        //hu ? TODO refactoring this craps
        var valueType: String = ""
        if (att.getEAttributeType.isInstanceOf[EEnum]) {
          valueType = ProcessorHelper.fqn(ctx, att.getEAttributeType)
        } else {
          valueType = ProcessorHelper.convertType(att.getEAttributeType)
        }

        if (ctx.getJS()) {
          pr.println("this." + methodNameClean + "(value as " + valueType + ")")
        } else {
          valueType match {
            case "String" => {
              pr.println("this." + methodNameClean + "(value as " + valueType + ")")
            }
            case "Boolean" | "Double" | "Int" => {
              pr.println("this." + methodNameClean + "(value.toString().to" + valueType + "())")
            }
            case "Any" => {
              pr.println("this." + methodNameClean + "(value.toString() as " + valueType + ")")
            }
            case _ => {
              pr.println("this." + methodNameClean + "(value as " + valueType + ")")
            }
          }
        }

        pr.println("}")
        pr.println("else -> {throw Exception(" + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.UNKNOWN_MUTATION_TYPE_EXCEPTION + mutationType)}")
        pr.println("}") //END MUTATION TYPE

        pr.println("}") //END ATTR
    }

    cls.getEAllReferences.foreach {
      ref =>
        pr.println(ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + " -> {") //START REF
        pr.println("when(mutationType) {")
        val valueType = ProcessorHelper.fqn(ctx, ref.getEReferenceType)

        if (ref.isMany) {

          pr.println("org.kevoree.modeling.api.util.ActionType.ADD -> {")
          val methodNameClean = "add" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)
          pr.println("      this." + methodNameClean + "(value as " + valueType + ")")
          pr.println("}")
          pr.println("org.kevoree.modeling.api.util.ActionType.ADD_ALL -> {")
          val methodNameClean2 = "addAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)
          pr.println("      this." + methodNameClean2 + "(value as List<" + valueType + ">)")
          pr.println("}")
          pr.println("org.kevoree.modeling.api.util.ActionType.REMOVE -> {")
          val methodNameClean3 = "remove" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)
          pr.println("      this." + methodNameClean3 + "(value as " + valueType + ")")
          pr.println("}")
          pr.println("org.kevoree.modeling.api.util.ActionType.REMOVE_ALL -> {")
          val methodNameClean4 = "removeAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)
          pr.println("      this." + methodNameClean4 + "()")
          pr.println("}")

        } else {

          pr.println("org.kevoree.modeling.api.util.ActionType.SET -> {")
          val methodNameClean = "set" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)
          val valueType = ProcessorHelper.fqn(ctx, ref.getEReferenceType)
          pr.println("      this." + methodNameClean + "(value as? " + valueType + ")")
          pr.println("}")

          pr.println("org.kevoree.modeling.api.util.ActionType.REMOVE -> {")
          pr.println("      this." + methodNameClean + "(null)")
          pr.println("}")

          pr.println("org.kevoree.modeling.api.util.ActionType.ADD -> {")
          pr.println("      this." + methodNameClean + "(value as? " + valueType + ")")
          pr.println("}")

        }

        if (hasID(ref.getEReferenceType) && ref.isMany) {
          pr.println("org.kevoree.modeling.api.util.ActionType.RENEW_INDEX -> {")
          pr.println("if(" + "_" + ref.getName + ".size() != 0 && " + "_" + ref.getName + ".containsKey(value)) {")
          pr.println("val obj = _" + ref.getName + ".get(value)")
          if (ctx.getGenFlatInheritance) {
            pr.println("_" + ref.getName + ".put((obj as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Impl).internalGetKey(),obj)")
          } else {
            pr.println("_" + ref.getName + ".put((obj as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Internal).internalGetKey(),obj)")
          }
          pr.println("_" + ref.getName + ".remove(value)")

          pr.println("}")
          pr.println("}")
        }

        pr.println("else -> {throw Exception(" + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.UNKNOWN_MUTATION_TYPE_EXCEPTION + mutationType)}")
        pr.println("}") //END MUTATION TYPE
        pr.println("}") //END Ref When case
    }
    pr.println("    else -> { throw Exception(\"Can reflexively \"+mutationType+\" for \"+refName + \" on \"+ this) }")
    pr.println("}") //END REFS NAME WHEN

    pr.println("}") //END METHOD
  }

  def generateFlatClass(ctx: GenerationContext, currentPackageDir: String, packElement: EPackage, cls: EClass) {

    val localFile = new File(currentPackageDir + "/impl/" + cls.getName + "Impl.kt")
    val pr = new PrintWriter(localFile, "utf-8")
    val pack = ProcessorHelper.fqn(ctx, packElement)
    pr.println("package " + pack + ".impl")
    pr.println()
    pr.println(generateHeader(packElement))
    //case class name
    ctx.classFactoryMap.put(pack + "." + cls.getName, ctx.packageFactoryMap.get(pack))
    pr.print("class " + cls.getName + "Impl")
    pr.println(" : " + ctx.getKevoreeContainerImplFQN + ", " + fqn(ctx, packElement) + "." + cls.getName + " { ")

    pr.println("override internal var internal_eContainer : " + ctx.getKevoreeContainer.get + "? = null")
    pr.println("override internal var internal_containmentRefName : String? = null")
    pr.println("override internal var internal_unsetCmd : " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand? = null")
    pr.println("override internal var internal_readOnlyElem : Boolean = false")
    pr.println("override internal var internal_recursive_readOnlyElem : Boolean = false")
    if (ctx.generateEvents) {
      pr.println("override internal var internal_modelElementListeners : MutableList<org.kevoree.modeling.api.events.ModelElementListener>? = null")
      pr.println("override internal var internal_modelTreeListeners : MutableList<org.kevoree.modeling.api.events.ModelElementListener>? = null")
    }
    //generate init

    val idAttributes = cls.getEAllAttributes.filter(att => att.isID && !att.getName.equals("generated_KMF_ID"))
    val alreadyGeneratedAttributes = new mutable.HashSet[String]()
    cls.getEAllAttributes.foreach {
      att => {
        if (!alreadyGeneratedAttributes.contains(att.getName)) {
          alreadyGeneratedAttributes.add(att.getName)
          var defaultValue = att.getDefaultValueLiteral
          pr.print("internal var " + "_" + att.getName + " : ")
          var typePre = ""
          var typePost = ""
          if (att.isMany) {
            typePre = "List<"
            typePost = ">"
            defaultValue = "ArrayList<" + ProcessorHelper.convertType(att.getEAttributeType) + ">()"
          }
          ProcessorHelper.convertType(att.getEAttributeType) match {
            case "String" | "java.lang.String" => pr.println(typePre + "String" + typePost + " = " + {
              if (att.getName.equals("generated_KMF_ID") && idAttributes.size == 0) {
                if(ctx.getJS()){
                  "\"\"+Math.random() + java.util.Date().getTime()"
                } else {
                  "\"\"+hashCode() + java.util.Date().getTime()"
                }
              } else {
                if (defaultValue == null) {
                  "\"\""
                } else {
                  defaultValue
                }
              }
            })
            case "Double" => pr.println("Double = " + {
              if (defaultValue == null) {
                "0.0"
              } else {
                defaultValue
              }
            })
            case "java.lang.Integer" => pr.println(typePre + "Int" + typePost + " = " + {
              if (defaultValue == null) {
                "0"
              } else {
                defaultValue
              }
            })
            case "Int" => pr.println(typePre + "Int" + typePost + " = " + {
              if (defaultValue == null) {
                "0"
              } else {
                defaultValue
              }
            })
            case "Long" => pr.println(typePre + "Long" + typePost + " = " + {
              if (defaultValue == null) {
                "0"
              } else {
                defaultValue
              }
            })
            case "Boolean" | "java.lang.Boolean" => pr.println(typePre + "Boolean" + typePost + " = " + {
              if (defaultValue == null) {
                "false"
              } else {
                defaultValue
              }
            })
            case "java.lang.Object" | "Any" => pr.println(typePre + "Any?" + typePost + " = null")
            case "java.lang.Class" | "Class" | "Class<out jet.Any?>" => pr.println(typePre + "Class<out jet.Any?>?" + typePost + " = null")
            case "null" => throw new UnsupportedOperationException("ClassGenerator:: Attribute type: " + att.getEAttributeType.getInstanceClassName + " has not been converted in a known type. Can not initialize.")
            case "float" | "Float" => pr.println(typePre + "Float" + typePost + " = " + {
              if (defaultValue == null) {
                "0"
              } else {
                defaultValue
              }
            })
            case "char" | "Char" => pr.println(typePre + "Char" + typePost + " = " + {
              if (defaultValue == null) {
                "a"
              } else {
                defaultValue
              }
            })
            case "java.math.BigInteger" => pr.println(typePre + "java.math.BigInteger" + typePost + " = " + {
              if (defaultValue == null) {
                "java.math.BigInteger.ZERO"
              } else {
                defaultValue
              }
            })
            case _@className => {
              if (att.getEAttributeType.isInstanceOf[EEnum]) {
                pr.println(ProcessorHelper.fqn(ctx, att.getEAttributeType) + "? = null")
              } else {
                pr.println(typePre)
                pr.println(className)
                pr.println(typePost)
                if (defaultValue != null) {
                  pr.println(" = " + defaultValue)
                }
              }
            }
          }
        }
      }
    }


    cls.getEAllReferences.foreach {
      ref =>
        val typeRefName = ProcessorHelper.fqn(ctx, ref.getEReferenceType)
        if (ref.getUpperBound == -1) {
          // multiple values
          pr.println("internal var " + "_" + ref.getName + "_java_cache :List<" + typeRefName + ">? = null")
          if (hasID(ref.getEReferenceType)) {
            pr.println("internal val " + "_" + ref.getName + " : java.util.HashMap<Any," + typeRefName + "> = java.util.HashMap<Any," + typeRefName + ">()")
          } else {
            pr.println("internal val " + "_" + ref.getName + " :MutableList<" + typeRefName + "> = java.util.ArrayList<" + typeRefName + ">()")
          }
        } else if (ref.getUpperBound == 1 && ref.getLowerBound == 0) {
          // optional single ref
          pr.println("internal var " + "_" + ref.getName + " : " + typeRefName + "? = null")
        } else if (ref.getUpperBound == 1 && ref.getLowerBound == 1) {
          // mandatory single ref
          pr.println("internal var " + "_" + ref.getName + " : " + typeRefName + "? = null")
        } else if (ref.getLowerBound > 1) {
          // else
          pr.println("internal var " + "_" + ref.getName + "_java_cache :List<" + typeRefName + ">? = null")
          if (hasID(ref.getEReferenceType)) {
            pr.println("internal val " + "_" + ref.getName + " : java.util.HashMap<Any," + typeRefName + "> = java.util.HashMap<Any," + typeRefName + ">()")
          } else {
            pr.println("internal val " + "_" + ref.getName + " :MutableList<" + typeRefName + "> = java.util.ArrayList<" + typeRefName + ">()")
          }
        } else {
          throw new UnsupportedOperationException("GenDefConsRef::Not standard arrity: " + cls.getName + "->" + typeRefName + "[" + ref.getLowerBound + "," + ref.getUpperBound + "]. Not implemented yet !")
        }
    }

    //Generate RecursiveReadOnly
    pr.println("override fun setRecursiveReadOnly(){")

    pr.println("if(internal_recursive_readOnlyElem == true){return}")
    pr.println("internal_recursive_readOnlyElem = true")

    cls.getEAllReferences.foreach {
      contained =>
        if (contained.getUpperBound == -1) {
          // multiple values
          pr.println("for(sub in this." + getGetter(contained.getName) + "){")
          pr.println("sub.setRecursiveReadOnly()")
          pr.println("}")
        } else if (contained.getUpperBound == 1 /*&& contained.getLowerBound == 0*/ ) {
          // optional single ref
          pr.println("val subsubsubsub" + contained.getName + " = this." + getGetter(contained.getName) + "")
          pr.println("if(subsubsubsub" + contained.getName + "!= null){ ")
          pr.println("subsubsubsub" + contained.getName + ".setRecursiveReadOnly()")
          pr.println("}")
        } else if (contained.getLowerBound > 1) {
          // else
          pr.println("for(sub in this." + getGetter(contained.getName) + "){")
          pr.println("\t\t\tsub.setRecursiveReadOnly()")
          pr.println("\t\t}")
        } else {
          throw new UnsupportedOperationException("ClonerGenerator::Not standard arity: " + cls.getName + "->" + contained.getName + "[" + contained.getLowerBound + "," + contained.getUpperBound + "]. Not implemented yet !")
        }
        pr.println()
    }
    pr.println("setInternalReadOnly()")
    pr.println("}")
    generateDeleteMethod(pr, cls, ctx, pack)
    // Getters and Setters Generation
    generateAllGetterSetterMethod(pr, cls, ctx, pack)
    //GENERATE CLONE METHOD
    generateCloneMethods(ctx, cls, pr, packElement)
    generateFlatReflexiveSetters(ctx, cls, pr)
    generateKMFQLMethods(pr, cls, ctx, pack)


    if (ctx.genSelector) {
      generateSelectorMethods(pr, cls, ctx)
    }
    generateEqualsMethods(pr, cls, ctx)
    generateContainedElementsMethods(pr, cls, ctx)

    generateMetaClassName(pr, cls, ctx)

    if (ctx.genTrace) {
      generateDiffMethod(pr, cls, ctx)
    }

    pr.println("}")
    pr.flush()
    pr.close()
  }


  private def generateMetaClassName(pr: PrintWriter, cls: EClass, ctx: GenerationContext) {
    pr.println("override fun metaClassName() : String {")
    pr.println("return \"" + ProcessorHelper.fqn(ctx, cls) + "\";")
    pr.println("}")
  }

  private def generateDeleteMethod(pr: PrintWriter, cls: EClass, ctx: GenerationContext, pack: String) {
    //TODO unreference object
    pr.println("override fun delete(){")
    if (!ctx.getJS()) {
      pr.println("for(sub in containedElements()){")
      pr.println("sub.delete()")
      pr.println("}")
    } else {
      cls.getEAllContainments.foreach {
        c =>
          if (c.isMany()) {
            pr.println("for(el in " + "_" + c.getName + "){")
            if (c.getEReferenceType.getEIDAttribute != null) {
              pr.println("el.value.delete()")
            } else {
              pr.println("el.delete()")
            }
            pr.println("}")
          } else {
            pr.println("_" + c.getName + "?.delete()")
          }
      }
    }

    cls.getEReferences.foreach {
      ref =>
        if (ref.isMany) {
          pr.println("_" + ref.getName + "?.clear()")
          pr.println("_" + ref.getName + "_java_cache = null")
        } else {
          pr.println("_" + ref.getName + " = null")
        }
    }
    pr.println("}")
  }


  private def generateAtts(pr: PrintWriter, cls: EClass, ctx: GenerationContext, pack: String) {

    cls.getEAttributes.foreach {
      att =>
        if (cls.getEAllAttributes.exists(att2 => att2.getName.equals(att.getName) && att2.getEContainingClass != cls)) {} else {
          pr.print("internal var " + "_" + att.getName + " : ")
          if (att.isMany) {
            pr.print("List<")
          }
          ProcessorHelper.convertType(att.getEAttributeType) match {
            case "java.lang.String" => pr.print("String")
            case "java.lang.Integer" => pr.print("Int")
            case "java.lang.Boolean" => pr.print("Boolean")
            case "java.lang.Object" | "Any" => pr.print("Any?")
            case "java.lang.Class" | "Class" | "Class<out jet.Any?>" => pr.print("Class<out jet.Any?>?")
            case "null" => throw new UnsupportedOperationException("ClassGenerator:: Attribute type: " + att.getEAttributeType.getInstanceClassName + " has not been converted in a known type. Can not initialize.")
            case _@className => {
              if (att.getEAttributeType.isInstanceOf[EEnum]) {
                pr.print(ProcessorHelper.fqn(ctx, att.getEAttributeType) + "?")
              } else {
                //println("--->" + className)
                pr.print(className)
              }
            }
          }
          if (att.isMany) {
            pr.print(">")
          }
          pr.println()
        }
    }

    cls.getEReferences.foreach {
      ref =>

        if (ref.getEReferenceType == null) {
          throw new Exception("Null EReferenceType for " + ref)
        }



        val typeRefName = ProcessorHelper.fqn(ctx, ref.getEReferenceType)

        if (ref.getUpperBound == -1 || ref.getUpperBound > 1) {
          // multiple values
          pr.println("internal var " + "_" + ref.getName + "_java_cache : List<" + typeRefName + ">?")
          if (hasID(ref.getEReferenceType)) {
            pr.println("internal val " + "_" + ref.getName + " : java.util.HashMap<Any," + typeRefName + ">")
          } else {
            pr.println("internal val " + "_" + ref.getName + " :MutableList<" + typeRefName + ">")
          }
        } else if (ref.getUpperBound == 1 && ref.getLowerBound == 0) {
          // optional single ref
          pr.println("internal var " + "_" + ref.getName + " : " + typeRefName + "?")
        } else if (ref.getUpperBound == 1 && ref.getLowerBound == 1) {
          // mandatory single ref
          pr.println("internal var " + "_" + ref.getName + " : " + typeRefName + "?")
        } else if (ref.getLowerBound > 1) {
          // else
          pr.println("internal var " + "_" + ref.getName + "_java_cache : List<" + typeRefName + ">?")
          if (hasID(ref.getEReferenceType)) {
            pr.println("internal val " + "_" + ref.getName + " : java.util.HashMap<Any," + typeRefName + ">")
          } else {
            pr.println("internal val " + "_" + ref.getName + " :MutableList<" + typeRefName + ">")
          }
        } else {
          throw new UnsupportedOperationException("GenDefConsRef::Not standard arrity: " + cls.getName + "->" + typeRefName + "[" + ref.getLowerBound + "," + ref.getUpperBound + "]. Not implemented yet !")
        }
    }
  }


  private def generateAllGetterSetterMethod(pr: PrintWriter, cls: EClass, ctx: GenerationContext, pack: String) {

    val atts = if (ctx.getGenFlatInheritance) {
      cls.getEAllAttributes
    } else {
      cls.getEAttributes
    }
    val alreadyGeneratedAttributes = new mutable.HashSet[String]()
    atts.foreach {
      att =>
        if (!alreadyGeneratedAttributes.contains(att.getName)) {
          alreadyGeneratedAttributes.add(att.getName)

          if (!ctx.getGenFlatInheritance && cls.getEAllAttributes.exists(att2 => att2.getName.equals(att.getName) && att2.getEContainingClass != cls)) {} else {
            //Generate getter

            if (att.getEAttributeType.isInstanceOf[EEnum]) {
              pr.print("override fun get" + att.getName.substring(0, 1).toUpperCase + att.getName.substring(1) + "() : " + ProcessorHelper.fqn(ctx, att.getEAttributeType) + "? {\n")
            } else if (ProcessorHelper.convertType(att.getEAttributeType) == "Any") {
              pr.print("override fun get" + att.getName.substring(0, 1).toUpperCase + att.getName.substring(1) + "() : Any? {\n")
            } else {

              if (ProcessorHelper.convertType(att.getEAttributeType).contains("Class")) {
                pr.print("override fun get" + att.getName.substring(0, 1).toUpperCase + att.getName.substring(1) + "() : " + ProcessorHelper.convertType(att.getEAttributeType) + "? {\n")
              } else {
                pr.print("override fun get" + att.getName.substring(0, 1).toUpperCase + att.getName.substring(1) + "() : " + ProcessorHelper.convertType(att.getEAttributeType) + " {\n")
              }

            }
            pr.println(" return " + "_" + att.getName + "\n}")
            //generate setter
            pr.print("\n override fun set" + att.getName.substring(0, 1).toUpperCase + att.getName.substring(1))
            if (att.getEAttributeType.isInstanceOf[EEnum]) {
              pr.print("(" + att.getName + param_suf + " : " + ProcessorHelper.fqn(ctx, att.getEAttributeType) + ") {\n")
            } else {
              pr.print("(" + att.getName + param_suf + " : " + ProcessorHelper.convertType(att.getEAttributeType) + ") {\n")
            }
            pr.println("if(isReadOnly()){throw Exception(" + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}")

            pr.println("val oldPath = path()")

            if (att.isID()) {
              pr.println("val oldId = internalGetKey()")
              pr.println("val previousParent = eContainer();")
              pr.println("val previousRefNameInParent = getRefInParent();")
            }

            pr.println("_" + att.getName + " = " + att.getName + param_suf)
            if (ctx.generateEvents) {
              pr.println("fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(oldPath, org.kevoree.modeling.api.util.ActionType.SET, org.kevoree.modeling.api.util.ElementAttributeType.ATTRIBUTE, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Att_" + att.getName + ", " + att.getName + param_suf + "))")
            }


            if (att.isID()) {
              pr.println("if(previousParent!=null){")
              pr.println("previousParent.reflexiveMutator(org.kevoree.modeling.api.util.ActionType.RENEW_INDEX, previousRefNameInParent!!, oldId);")
              pr.println("}")
              if (ctx.generateEvents) {
                pr.println("fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(oldPath, org.kevoree.modeling.api.util.ActionType.RENEW_INDEX, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Att_" + att.getName + ", path()))")
              }
            }


            pr.println("}")
          }
        }
    }

    val refs = if (ctx.getGenFlatInheritance) {
      cls.getEAllReferences
    } else {
      cls.getEReferences
    }
    refs.foreach {
      ref =>
        val typeRefName = ProcessorHelper.fqn(ctx, ref.getEReferenceType)
        if (ref.getUpperBound == -1 || ref.getUpperBound >1) {
          // multiple values
          pr.println(generateGetter(ctx, ref, typeRefName, false, false))
          pr.println(generateSetter(ctx, cls, ref, typeRefName, false, false))
          pr.println(generateAddMethod(cls, ref, typeRefName, ctx))
          pr.println(generateRemoveMethod(cls, ref, typeRefName, true, ctx))
        } else if (ref.getUpperBound == 1 && ref.getLowerBound == 0) {
          // optional single ref
          pr.println(generateGetter(ctx, ref, typeRefName, true, true))
          pr.println(generateSetter(ctx, cls, ref, typeRefName, true, true))
        } else if (ref.getUpperBound == 1 && ref.getLowerBound == 1) {
          // mandatory single ref
          pr.println(generateGetter(ctx, ref, typeRefName, false, true))
          pr.println(generateSetter(ctx, cls, ref, typeRefName, false, true))
        } else if (ref.getLowerBound > 1) {
          pr.println(generateGetter(ctx, ref, typeRefName, false, false))
          pr.println(generateSetter(ctx, cls, ref, typeRefName, false, false))
          pr.println(generateAddMethod(cls, ref, typeRefName, ctx))
          pr.println(generateRemoveMethod(cls, ref, typeRefName, false, ctx))
        } else {
          throw new UnsupportedOperationException("GenDefConsRef::Not a standard arity: " + cls.getName + "->" + typeRefName + "[" + ref.getLowerBound + "," + ref.getUpperBound + "]. Not implemented yet !")
        }
    }
  }

  private def generateGetter(ctx: GenerationContext, ref: EReference, typeRefName: String, isOptional: Boolean, isSingleRef: Boolean): String = {
    //Generate getter
    val methName = "get" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)
    var res = ""
    res += "\noverride fun " + methName + "() : "
    //Set return type
    res += {
      if (isOptional) {
        ""
      } else {
        ""
      }
    }



    res += {
      if (!isSingleRef) {
        if (ctx.getJS()) {
          "List<"
        } else {
          "MutableList<"
        }
      } else {
        ""
      }
    }
    res += typeRefName
    res += {
      if (!isSingleRef) {
        ">"
      } else {
        "?"
      }
    }
    res += " {\n"
    //Method core
    if (isSingleRef) {
      res += "return "
      res += "_" + ref.getName
      res += "\n"
    } else {
      if (ctx.getJS()) {
        if (hasID(ref.getEReferenceType)) {
          res += "return _" + ref.getName + ".values().toList()"
        } else {
          res += "return _" + ref.getName //TODO protection for JS
        }
      } else {
        res += "if(" + "_" + ref.getName + "_java_cache != null){\n"
        res += "return _" + ref.getName + "_java_cache as MutableList<" + typeRefName + ">\n"
        res += "} else {\n"
        if (hasID(ref.getEReferenceType)) {
          res += "_" + ref.getName + "_java_cache = java.util.Collections.unmodifiableList(_" + ref.getName + ".values().toList())\n"
          res += "return _" + ref.getName + "_java_cache as MutableList<" + typeRefName + ">\n"
        } else {
          res += "val tempL = java.util.ArrayList<" + typeRefName + ">()\n"
          res += "tempL.addAll(" + "_" + ref.getName + ")\n"
          res += "_" + ref.getName + "_java_cache = java.util.Collections.unmodifiableList(tempL)\n"
          res += "return tempL\n"
        }
        res += "}\n"
      }
    }
    res += "}"
    res
  }


  private def generateSetter(ctx: GenerationContext, cls: EClass, ref: EReference, typeRefName: String, isOptional: Boolean, isSingleRef: Boolean): String = {
    val oppositRef = ref.getEOpposite
    (if (oppositRef != null && !ref.isMany) {
      //Generates the NoOpposite_Set method only the local reference is a single ref. (opposite managed on the * side)
      generateSetterOp(ctx, cls, ref, typeRefName, isOptional, isSingleRef, true)
    } else {
      ""
    }) + generateSetterOp(ctx, cls, ref, typeRefName, isOptional, isSingleRef, false)
  }

  private def generateSetterOp(ctx: GenerationContext, cls: EClass, ref: EReference, typeRefName: String, isOptional: Boolean, isSingleRef: Boolean, noOpposite: Boolean): String = {
    //generate setter
    var res = ""
    val formatedLocalRefName = ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)


    val implExt = if (ctx.getGenFlatInheritance) {
      "Impl"
    } else {
      "Internal"
    }
    val refInternalClassFqn = ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + implExt

    if (noOpposite) {
      res += "\nfun noOpposite_set" + formatedLocalRefName
    } else {
      res += "\noverride fun set" + formatedLocalRefName
    }
    res += "(" + ref.getName + param_suf + " : "
    res += {
      if (!isSingleRef) {
        "List<" + typeRefName + ">"
      } else {
        typeRefName + "?"
      }
    }

    res += " ) {\n"

    //Read only protection
    res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"
    if (ref.isMany) {
      res += "if(" + ref.getName + param_suf + " == null){ throw IllegalArgumentException(" + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.LIST_PARAMETER_OF_SET_IS_NULL_EXCEPTION) }\n"
    }
    if (!isSingleRef) {
      //Clear cache
      res += ("_" + ref.getName + "_java_cache=null\n")
    }

    res += "if(" + "_" + ref.getName + "!= " + ref.getName + param_suf + "){\n"
    val oppositRef = ref.getEOpposite

    if (!ref.isMany) {
      // -> Single ref : 0,1 or 1
      if (!noOpposite && (oppositRef != null)) {
        //Management of opposite relation in regular setter
        val formatedOpositName = oppositRef.getName.substring(0, 1).toUpperCase + oppositRef.getName.substring(1)

        if (oppositRef.isMany) {
          // 0,1 or 1  -- *

          if (ref.isRequired) {
            // Single Ref  1
            res += "if(" + "_" + ref.getName + " != null){\n"
            res += "(" + "_" + ref.getName + " as " + refInternalClassFqn + ")!!.noOpposite_remove" + formatedOpositName + "(this)\n"
            res += "}\n"
            res += "if(" + ref.getName + param_suf + " != null){\n"
            res += "(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_add" + formatedOpositName + "(this)\n"
            res += "}\n"
          } else {
            // Single Ref  0,1
            res += "if(" + "_" + ref.getName + " != null) { (" + "_" + ref.getName + " as " + refInternalClassFqn + ")!!.noOpposite_remove" + formatedOpositName + "(this) }\n"
            res += "if(" + ref.getName + param_suf + "!=null) {(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_add" + formatedOpositName + "(this)}\n"
          }

        } else {
          // -> // 0,1 or 1  --  0,1 or 1

          if (ref.isRequired) {
            // 1 -- 0,1 or 1

            res += "if(" + "_" + ref.getName + " != null){\n"
            res += "(" + "_" + ref.getName + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(null)\n"

            if (ref.isContainment) {
              res += "(" + "_" + ref.getName + "!! as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(null,null,null)\n"
            }
            res += "}\n"

            res += "if(" + ref.getName + param_suf + " != null){\n"

            res += "(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(this)\n"

            if (ref.isContainment) {
              res += "(" + ref.getName + param_suf + "!! as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(this,null,\"" + ref.getName + "\")\n"
            }

            res += "}\n"

          } else {
            // 0,1 -- 0,1 or 1
            if (oppositRef.isRequired) {
              // 0,1 -- 1
              if (!ref.isContainment) {
                res += "if(" + "_" + ref.getName + "!=null){(" + "_" + ref.getName + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(null) }\n"
                res += "if(" + ref.getName + param_suf + "!=null){(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(this)}\n"
              } else {
                res += "if(" + "_" + ref.getName + "!=null) {\n"
                res += "(" + "_" + ref.getName + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(null)\n"
                res += "(" + "_" + ref.getName + " as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(null,null,null)\n"
                res += "}\n"
                res += "if(" + ref.getName + param_suf + "!= null) {\n"
                res += "(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(this)\n"
                res += "(" + ref.getName + param_suf + " as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(this,null,\"" + ref.getName + "\")\n"
                res += "}\n"
              }
            } else {
              // 0,1 -- 0,1
              if (!ref.isContainment) {
                res += "if(" + "_" + ref.getName + "!=null) {(" + "_" + ref.getName + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(null) }\n"
                res += "if(" + ref.getName + param_suf + "!=null) {(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(this)}\n"
              } else {
                res += "if(" + "_" + ref.getName + "!=null) {\n"
                res += "(" + "_" + ref.getName + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(null)\n"
                res += "_" + ref.getName + ".setEContainer(null,null,null)\n"
                res += "}\n"
                res += "if(" + ref.getName + param_suf + "!= null) {\n"
                res += "(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(this)\n"
                res += "(" + ref.getName + param_suf + "!! as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(this,null,\"" + ref.getName + "\")\n"
                res += "}\n"
              }

            }

          }
        }
      }

      if (noOpposite && ref.isContainment) {
        // containment relation in noOpposite Method
        if (!ref.isRequired) {
          res += "if(" + "_" + ref.getName + "!=null){\n"
          res += "(" + "_" + ref.getName + " as " + ctx.getKevoreeContainerImplFQN + " ).setEContainer(null,null,null)\n"
          res += "}\n"
          res += "if(" + ref.getName + param_suf + "!=null) {\n"
          res += "(" + ref.getName + param_suf + " as " + ctx.getKevoreeContainerImplFQN + " ).setEContainer(this,null,\"" + ref.getName + "\")\n"
          res += "}\n"
        } else {
          res += "if(" + "_" + ref.getName + " != null){\n"
          res += "(" + "_" + ref.getName + "!! as " + ctx.getKevoreeContainerImplFQN + " ).setEContainer(null, null,null)\n"
          res += "}\n"
          res += "if(" + ref.getName + param_suf + " != null){\n"
          res += "(" + ref.getName + param_suf + " as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(this, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.SET, \"" + ref.getName + "\", Any),\"" + ref.getName + "\" )\n"
          res += "}\n"
        }
      } else {
        // containment with no opposite relation
        if (ref.isContainment && (ref.getEOpposite == null)) {
          if (ref.isMany) {
            res += "(" + ref.getName + param_suf + " as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(this, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.REMOVE, \"" + ref.getName + "\", " + ref.getName + param_suf + "),\"" + ref.getName + "\" )\n"
          } else {
            res += "if(" + "_" + ref.getName + "!=null){\n"
            res += "(" + "_" + ref.getName + "!! as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(null, null,null)\n"
            res += "}\n"
            res += "if(" + ref.getName + param_suf + "!=null){\n"
            res += "(" + ref.getName + param_suf + " as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(this,  " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.SET, \"" + ref.getName + "\", null),\"" + ref.getName + "\")\n"
            res += "}\n"
          }
        }
      }
      //Setting of local reference
      res += "_" + ref.getName + " = " + ref.getName + param_suf + "\n"
      if (ctx.generateEvents) {
        if (ref.isContainment) {
          res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.SET, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + "))\n"
        } else {
          res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.SET, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + "))\n"
        }
      }

    } else {
      // -> Collection ref : * or +
      res += "_" + ref.getName + ".clear()\n"

      if (hasID(ref.getEReferenceType)) {
        res += "for(el in " + ref.getName + param_suf + "){\n"
        if (ctx.getGenFlatInheritance) {
          res += "_" + ref.getName + ".put((el as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Impl).internalGetKey(),el)\n"
        } else {
          res += "_" + ref.getName + ".put((el as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Internal).internalGetKey(),el)\n"
        }

        res += "}\n"
      } else {
        res += "_" + ref.getName + ".addAll(" + ref.getName + param_suf + ")\n"
      }

      if (ref.isContainment) {
        if (oppositRef != null) {
          res += "for(elem in " + ref.getName + param_suf + "){\n"
          res += "(elem as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(this," + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.REMOVE, \"" + ref.getName + "\", elem),\"" + ref.getName + "\")\n"
          val formatedOpositName = oppositRef.getName.substring(0, 1).toUpperCase + oppositRef.getName.substring(1)
          if (oppositRef.isMany) {
            res += "(elem as " + refInternalClassFqn + ").noOpposite_add" + formatedOpositName + "(this)\n"
          } else {
            res += "(elem as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(this)\n"
          }
          res += "}\n"
        } else {
          res += "for(elem in " + ref.getName + param_suf + "){\n"
          res += "(elem as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(this," + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.REMOVE, \"" + ref.getName + "\", elem),\"" + ref.getName + "\")\n"
          res += "}\n"

        }
      } else {
        if (oppositRef != null) {
          val formatedOpositName = oppositRef.getName.substring(0, 1).toUpperCase + oppositRef.getName.substring(1)
          if (oppositRef.isMany) {
            res += "for(elem in " + ref.getName + param_suf + "){(elem as " + refInternalClassFqn + ").noOpposite_add" + formatedOpositName + "(this)}\n"
          } else {

            val callParam = "this"

            res += "for(elem in " + ref.getName + param_suf + "){(elem as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(" + callParam + ")}\n"
          }
        }
      }
      if (ctx.generateEvents) {
        if (ref.isContainment) {
          res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.SET, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + "))"
        } else {
          res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.SET, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + "))"
        }

      }


    }
    res += "}\n" //END IF newRef != localRef

    if (noOpposite && oppositRef != null && oppositRef.isMany && !hasID(ref.getEReferenceType)) {
      res += "else {\n"
      //DUPLICATE CASE OF SET / ONLY IN LOADER RUN
      val formatedOpositName = oppositRef.getName.substring(0, 1).toUpperCase + oppositRef.getName.substring(1)
      // 0,1 or 1  -- *
      if (ref.isRequired) {
        // Single Ref  1
        res += "if(" + "_" + ref.getName + " != null){\n"
        res += "(" + "_" + ref.getName + " as " + refInternalClassFqn + ")!!.noOpposite_remove" + formatedOpositName + "(this)\n"
        res += "}\n"
      } else {
        // Single Ref  0,1
        res += "if(" + "_" + ref.getName + "!=null){ (" + "_" + ref.getName + " as " + refInternalClassFqn + ")!!.noOpposite_remove" + formatedOpositName + "(this) }\n"
      }
      res += "}\n"
    }
    res += "\n}" //END Method
    res
  }


  private def generateAddMethod(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    generateAddMethodOp(cls, ref, typeRefName, false, ctx) + generateAddAllMethodOp(cls, ref, typeRefName, false, ctx) +
      (if (ref.getEOpposite != null) {
        generateAddMethodOp(cls, ref, typeRefName, true, ctx) + generateAddAllMethodOp(cls, ref, typeRefName, true, ctx)
      } else {
        ""
      })
  }

  private def generateAddAllMethodOp(cls: EClass, ref: EReference, typeRefName: String, noOpposite: Boolean, ctx: GenerationContext): String = {
    var res = ""
    res += "\n"
    if (noOpposite) {
      res += "\nfun noOpposite_addAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)
    } else {
      res += "\noverride fun addAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)
    }
    res += "(" + ref.getName + param_suf + " :List<" + typeRefName + ">) {\n"
    res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"
    //Clear cache
    res += ("_" + ref.getName + "_java_cache=null\n")
    if (hasID(ref.getEReferenceType)) {
      res += "for(el in " + ref.getName + param_suf + "){\n"

      if (ctx.getGenFlatInheritance) {
        res += "val _key_ = (el as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Impl).internalGetKey()\n"
      } else {
        res += "val _key_ = (el as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Internal).internalGetKey()\n"
      }
      res += "if(_key_ == \"\" || _key_ == null){ throw Exception(\"Key empty : set the attribute key before adding the object\") }\n"

      res += "_" + ref.getName + ".put(_key_,el)\n"

      res += "}\n"
    } else {
      res += "_" + ref.getName + ".addAll(" + ref.getName + param_suf + ")\n"
    }

    if ((!noOpposite && ref.getEOpposite != null) || ref.isContainment) {
      res += "for(el in " + ref.getName + param_suf + "){\n"
      if (ref.isContainment) {
        res += "(el as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(this," + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.REMOVE, \"" + ref.getName + "\", el),\"" + ref.getName + "\")\n"
      }
      if (ref.getEOpposite != null && !noOpposite) {
        val opposite = ref.getEOpposite
        val formatedOpositName = opposite.getName.substring(0, 1).toUpperCase + opposite.getName.substring(1)

        val implExt = if (ctx.getGenFlatInheritance) {
          "Impl"
        } else {
          "Internal"
        }
        val refInternalClassFqn = ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + implExt
        if (!opposite.isMany) {
          res += "(el as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(this)"
        } else {
          res += "(el as " + refInternalClassFqn + ").noOpposite_add" + formatedOpositName + "(this)"
        }
      }
      res += "}\n"
    }
    if (ctx.generateEvents) {
      if (ref.isContainment) {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.ADD_ALL, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + "))\n"
      } else {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.ADD_ALL, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + "))\n"
      }
    }


    res += "}\n"
    res
  }


  private def generateAddMethodOp(cls: EClass, ref: EReference, typeRefName: String, noOpposite: Boolean, ctx: GenerationContext): String = {
    //generate add
    var res = ""
    val formatedAddMethodName = ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)
    if (noOpposite) {
      res += "\nfun noOpposite_add" + formatedAddMethodName
    } else {
      res += "\noverride fun add" + formatedAddMethodName
    }
    res += "(" + ref.getName + param_suf + " : " + typeRefName + ") {\n"
    res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"

    //Clear cache
    res += ("_" + ref.getName + "_java_cache=null\n")

    if (ref.isContainment) {
      res += "(" + ref.getName + param_suf + " as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(this," + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.REMOVE, \"" + ref.getName + "\", " + ref.getName + param_suf + "),\"" + ref.getName + "\")\n"
    }

    if (hasID(ref.getEReferenceType)) {
      if (ctx.getGenFlatInheritance) {
        res += "val _key_ = (" + ref.getName + param_suf + " as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Impl).internalGetKey()\n"
      } else {
        res += "val _key_ = (" + ref.getName + param_suf + " as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Internal).internalGetKey()\n"
      }
      res += "if(_key_ == \"\" || _key_ == null){ throw Exception(\"Key empty : set the attribute key before adding the object\") }\n"

      if (ctx.getGenFlatInheritance) {
        res += "_" + ref.getName + ".put(_key_," + ref.getName + param_suf + ")\n"
      } else {
        res += "_" + ref.getName + ".put(_key_," + ref.getName + param_suf + ")\n"
      }
    } else {
      res += "_" + ref.getName + ".add(" + ref.getName + param_suf + ")\n"
    }

    if (ctx.generateEvents) {
      if (ref.isContainment) {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.ADD, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + "))\n"
      } else {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.ADD, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + "))\n"
      }

    }



    if (ref.getEOpposite != null && !noOpposite) {
      val opposite = ref.getEOpposite
      val formatedOpositName = opposite.getName.substring(0, 1).toUpperCase + opposite.getName.substring(1)

      val implExt = if (ctx.getGenFlatInheritance) {
        "Impl"
      } else {
        "Internal"
      }
      val refInternalClassFqn = ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + implExt

      if (!opposite.isMany) {
        res += "(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(this)"
      } else {
        res += "(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_add" + formatedOpositName + "(this)"
      }
    }
    res += "}"
    res
  }


  private def generateRemoveMethod(cls: EClass, ref: EReference, typeRefName: String, isOptional: Boolean, ctx: GenerationContext): String = {
    generateRemoveMethodOp(cls, ref, typeRefName, isOptional, false, ctx) + generateRemoveAllMethod(cls, ref, typeRefName, isOptional, false, ctx) +
      (if (ref.getEOpposite != null) {
        generateRemoveMethodOp(cls, ref, typeRefName, isOptional, true, ctx) + generateRemoveAllMethod(cls, ref, typeRefName, isOptional, true, ctx)
      } else {
        ""
      })
  }

  private def generateRemoveMethodOp(cls: EClass, ref: EReference, typeRefName: String, isOptional: Boolean, noOpposite: Boolean, ctx: GenerationContext): String = {
    //generate remove
    var res = ""
    val formatedMethodName = ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)

    if (ctx.generateEvents && ref.isContainment && !noOpposite) {
      // only once in the class, only for contained references
      if (!ctx.js) {
        // initialized in the Impl
        res += "\nvar removeAll" + formatedMethodName + "CurrentlyProcessing : Boolean\n"
      } else {
        res += "\nvar removeAll" + formatedMethodName + "CurrentlyProcessing : Boolean = false\n"
      }

    }

    if (noOpposite) {
      res += "\nfun noOpposite_remove" + formatedMethodName
    } else {
      res += "\noverride fun remove" + formatedMethodName
    }


    res += "(" + ref.getName + param_suf + " : " + typeRefName + ") {\n"

    res += ("if(isReadOnly()){throw Exception(" + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n")
    //Clear cache
    res += ("_" + ref.getName + "_java_cache=null\n")



    if (isOptional) {
      if (hasID(ref.getEReferenceType)) {
        if (ctx.getGenFlatInheritance) {
          res += "if(" + "_" + ref.getName + ".size() != 0 && " + "_" + ref.getName + ".containsKey((" + ref.getName + param_suf + " as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Impl).internalGetKey())) {\n"
        } else {
          res += "if(" + "_" + ref.getName + ".size() != 0 && " + "_" + ref.getName + ".containsKey((" + ref.getName + param_suf + " as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Internal).internalGetKey())) {\n"
        }
      } else {
        res += "if(" + "_" + ref.getName + ".size() != 0 && " + "_" + ref.getName + ".indexOf(" + ref.getName + param_suf + ") != -1 ) {\n"
      }

    } else {

      if (hasID(ref.getEReferenceType)) {
        if (ctx.getGenFlatInheritance) {
          res += "if(" + "_" + ref.getName + ".size == " + ref.getLowerBound + "&& " + "_" + ref.getName + ".containsKey((" + ref.getName + param_suf + " as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Impl).internalGetKey()) ) {\n"
        } else {
          res += "if(" + "_" + ref.getName + ".size == " + ref.getLowerBound + "&& " + "_" + ref.getName + ".containsKey((" + ref.getName + param_suf + " as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Internal).internalGetKey()) ) {\n"
        }
      } else {
        res += "if(" + "_" + ref.getName + ".size == " + ref.getLowerBound + "&& " + "_" + ref.getName + ".indexOf(" + ref.getName + param_suf + ") != -1 ) {\n"
      }

      res += "throw UnsupportedOperationException(\"The list of " + ref.getName + param_suf + " must contain at least " + ref.getLowerBound + " element. Can not remove sizeof(" + ref.getName + param_suf + ")=\"+" + "_" + ref.getName + ".size)\n"
      res += "} else {\n"
    }

    if (hasID(ref.getEReferenceType)) {
      if (ctx.getGenFlatInheritance) {
        res += "_" + ref.getName + ".remove((" + ref.getName + param_suf + " as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Impl).internalGetKey())\n"
      } else {
        res += "_" + ref.getName + ".remove((" + ref.getName + param_suf + " as " + ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + "Internal).internalGetKey())\n"
      }
    } else {
      if (ctx.getJS()) {
        // Kotlin JS fix: arrayList.remove(arrayList.indexOf(elem)) does not work in Javascript, but arrayList.remove(elem) does
        res += "_" + ref.getName + ".remove(" + ref.getName + param_suf + ")\n"
      } else {
        // keep the O(1) complexity in Java
        res += "_" + ref.getName + ".remove(" + "_" + ref.getName + ".indexOf(" + ref.getName + param_suf + "))\n"
      }
    }

    if (ref.isContainment) {
      //TODO
      res += "(" + ref.getName + param_suf + "!! as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(null,null,null)\n"
    }

    if (ctx.generateEvents) {
      if (ref.isContainment) {
        res += "if(!removeAll" + formatedMethodName + "CurrentlyProcessing) {\n"
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.REMOVE, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + "))\n"
        res += "}\n"
      } else {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.REMOVE, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + "))\n"
      }
    }

    val oppositRef = ref.getEOpposite
    if (!noOpposite && oppositRef != null) {
      val formatedOpositName = oppositRef.getName.substring(0, 1).toUpperCase + oppositRef.getName.substring(1)
      val implExt = if (ctx.getGenFlatInheritance) {
        "Impl"
      } else {
        "Internal"
      }
      val refInternalClassFqn = ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + implExt

      if (oppositRef.isMany) {
        res += "(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_remove" + formatedOpositName + "(this)\n"
      } else {
        res += "(" + ref.getName + param_suf + " as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(null)\n"
      }
    }

    res += "}\n"
    res += "}\n"

    res
  }

  private def generateRemoveAllMethod(cls: EClass, ref: EReference, typeRefName: String, isOptional: Boolean, noOpposite: Boolean, ctx: GenerationContext): String = {
    var res = ""
    if (noOpposite) {
      res += "\nfun noOpposite_removeAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1) + "() {\n"
    } else {
      res += "\noverride fun removeAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1) + "() {\n"
    }

    res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"
    if (ctx.generateEvents && ref.isContainment) {
      res += "\nremoveAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1) + "CurrentlyProcessing=true\n"
    }
    val getterCall = "get" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1) + "()"
    if (hasID(ref.getEReferenceType)) {
      res += "val temp_els = " + getterCall + "!!\n"
    } else {
      var collectionHelper = "java.util.Collections.unmodifiableList"
      if (ctx.getJS()) {
        collectionHelper = ""
      }
      res += "val temp_els = " + collectionHelper + "(" + getterCall + ")\n"
    }


    if ((!noOpposite && ref.getEOpposite != null) || ref.isContainment) {

      res += "for(el in temp_els!!){\n"

      if (ref.isContainment) {
        res += "(el as " + ctx.getKevoreeContainerImplFQN + ").setEContainer(null,null,null)\n"
      }
      if (ref.getEOpposite != null && !noOpposite) {
        val opposite = ref.getEOpposite
        val formatedOpositName = opposite.getName.substring(0, 1).toUpperCase + opposite.getName.substring(1)


        val implExt = if (ctx.getGenFlatInheritance) {
          "Impl"
        } else {
          "Internal"
        }
        val refInternalClassFqn = ProcessorHelper.fqn(ctx, ref.getEReferenceType.getEPackage) + ".impl." + ref.getEReferenceType.getName + implExt
        if (!opposite.isMany) {
          res += "(el as " + refInternalClassFqn + ").noOpposite_set" + formatedOpositName + "(null)\n"
        } else {
          res += "(el as " + refInternalClassFqn + ").noOpposite_remove" + formatedOpositName + "(this)\n"
        }
      }
      res += "}\n"
    }
    res += "_" + ref.getName + "_java_cache=null\n"
    res += "_" + ref.getName + ".clear()\n"

    if (ctx.generateEvents) {
      if (ref.isContainment) {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.REMOVE_ALL, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", temp_els))\n"
        res += "\nremoveAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1) + "CurrentlyProcessing=false\n"

      } else {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.REMOVE_ALL, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", temp_els))\n"
      }
    }

    res += "}\n"
    res
  }


}
