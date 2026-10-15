package guessmarket.server;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import guessmarket.engine.*;
import guessmarket.engine.xml.ex3.*;
import java.util.*;
import java.io.*;
import java.security.MessageDigest;
@WebServlet("/api/v1/events/upload")
@MultipartConfig(fileSizeThreshold=6291456,maxFileSize=5242880,maxRequestSize=6291456)
public final class UploadServlet extends ApiServlet {
    private static final long serialVersionUID=1L;
    @Override void dispatch(HttpServletRequest request,HttpServletResponse response)throws Exception {
        method(request,"POST");String session=session(request);app().actor(session);String id=request.getHeader("X-Request-Id");RequestValidator.uuid(id,app());
        if(request.getContentType()==null||!request.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/form-data;"))throw app().failure(415,"UNSUPPORTED_CONTENT_TYPE","Send one XML file as multipart/form-data.",id);
        Collection<Part> parts;
        try{parts=request.getParts();}catch(IllegalStateException problem){throw app().failure(413,"UPLOAD_TOO_LARGE","XML limit is 5 MiB; request limit is 6 MiB.",id);}catch(ServletException problem){throw app().failure(400,"INVALID_MULTIPART","The multipart file could not be read.",id);}
        try{
            if(parts.size()!=1)throw app().failure(400,"INVALID_UPLOAD","Upload exactly one part named file.",id);
            var part=parts.iterator().next();if(!part.getName().equals("file")||part.getSubmittedFileName()==null)throw app().failure(400,"INVALID_UPLOAD","Select one XML file.",id);
            String filename=part.getSubmittedFileName().replace('\\','/');filename=filename.substring(filename.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","_");
            if(filename.isEmpty()||filename.length()>255||!filename.toLowerCase(Locale.ROOT).endsWith(".xml"))throw app().failure(400,"INVALID_XML_FILENAME","Choose a file ending in .xml.",id);
            byte[] bytes;try(var input=part.getInputStream()){bytes=input.readNBytes(5242881);}if(bytes.length>5242880)throw app().failure(413,"UPLOAD_TOO_LARGE","XML limit is 5 MiB.",id);
            String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));ParsedEventBatch batch;
            try(var input=new ByteArrayInputStream(bytes)){batch=new Ex3XmlEventLoader().parse(input,filename);}catch(EngineOperationException problem){throw app().failure(422,problem.getCode().name(),problem.getMessage(),id);}
            send(response,201,app().upload(session,id,digest,filename,batch));
        }finally{for(var part:parts)part.delete();}
    }
}
