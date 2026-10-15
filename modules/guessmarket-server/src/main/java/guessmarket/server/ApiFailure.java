package guessmarket.server;
import guessmarket.dto.api.ApiError;
public final class ApiFailure extends RuntimeException {
    private static final long serialVersionUID=1L;
    private final int status;
    private final transient ApiError error;
    public ApiFailure(int status,ApiError error){super(error.message());this.status=status;this.error=error;}
    public int status(){return status;}
    public ApiError error(){return error;}
}
