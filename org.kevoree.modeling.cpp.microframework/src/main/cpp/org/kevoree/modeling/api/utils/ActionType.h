

#include <string>






enum  actions {SET=0,ADD=1,REMOVE=2,ADD_ALL=3,REMOVE_ALL=4,RENEW_INDEX=5};
static std::string ActionType(actions e)
{
	  switch(e)
	  {
		  case SET: return "0";
		  case ADD: return "1";
		  case REMOVE: return "2";
		  case ADD_ALL: return "3";
		  case REMOVE_ALL: return "4";
		  case RENEW_INDEX: return "5";
		  default: return "-1";
	  }
}