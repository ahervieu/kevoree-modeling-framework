#ifndef KMFContainer_H
#define KMFContainer_H

#include <string>
#include "events/ModelElementListener.h"
#include "trace/ModelTrace.h"
#include <list>
#include "utils/any.h"
#include "utils/ModelVisitor.h"
#include "utils/ModelAttributeVisitor.h"

using std::string;
using std::list;

class KMFContainer 
{

public:

    virtual KMFContainer eContainer(){}
    virtual bool isReadOnly(){}
    virtual bool isRecursiveReadOnly(){}
    virtual void setInternalReadOnly(){}
	virtual void deleteContainer(){} // can't use delete reserve c++ 
	virtual bool modelEquals(KMFContainer similarObj){}
    virtual bool deepModelEquals(KMFContainer similarObj){}
    virtual string getRefInParent(){}
    virtual KMFContainer findByPath(string query){}
    virtual KMFContainer findByID(string relationName,string idP){}
	virtual string path(){}
    virtual string metaClassName(){}
    virtual void reflexiveMutator(int mutatorType,string refName, void *value, bool setOpposite,bool fireEvent ){}
    virtual list<any>  selectByQuery(string query){}
    virtual void addModelElementListener(ModelElementListener lst){}
    virtual void removeModelElementListener(ModelElementListener lst){}
    virtual void removeAllModelElementListeners(){}
    virtual void addModelTreeListener(ModelElementListener lst){}
    virtual void removeModelTreeListener(ModelElementListener lst){}
    virtual void removeAllModelTreeListeners(){}
    
    template <class A> // http://www.cplusplus.com/doc/tutorial/templates
	A* findByPath(string query,A clazz);
 
    virtual void visit(ModelVisitor visitor,bool recursive,bool containedReference ,bool nonContainedReference){}
    virtual void visitAttributes(ModelAttributeVisitor visitor ){}

    virtual list<ModelTrace*> createTraces(KMFContainer similarObj ,bool isInter ,bool isMerge ,bool onlyReferences,bool onlyAttributes ) {}

};

#endif
