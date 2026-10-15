package guessmarket.server;
import guessmarket.engine.*;
import guessmarket.dto.api.ApiError;
public final class ApiExceptionMapper {
    private ApiExceptionMapper() {}
    public static ApiFailure map(WorldCommandException problem,String id,String instance,long revision){
        int status=switch(problem.getCode()){case NOT_OWNER->403;case USER_NOT_FOUND,EVENT_NOT_FOUND->404;case INVALID_OPTION,INVALID_QUANTITY,INVALID_ORDER->400;default->409;};
        return new ApiFailure(status,new ApiError(problem.getCode().name(),problem.getMessage(),null,"Refresh the market or correct the input.",id,instance,revision));
    }
}
