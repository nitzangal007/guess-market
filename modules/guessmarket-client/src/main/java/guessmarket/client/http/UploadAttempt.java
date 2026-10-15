package guessmarket.client.http;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
/** Captured bytes and metadata survive uncertain outcomes, independently of later file edits. */
public final class UploadAttempt {
    private final String requestId,filename,digest;
    private final byte[] bytes;
    public UploadAttempt(String requestId,String filename,byte[] bytes){
        if(bytes.length>5242880)throw new IllegalArgumentException("XML limit is 5 MiB.");this.requestId=Objects.requireNonNull(requestId);this.filename=Objects.requireNonNull(filename);this.bytes=bytes.clone();
        try{digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(this.bytes));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static UploadAttempt capture(Path path,String id)throws IOException{try(var input=Files.newInputStream(path)){var bytes=input.readNBytes(5242881);if(bytes.length>5242880)throw new IOException("XML limit is 5 MiB.");return new UploadAttempt(id,path.getFileName().toString(),bytes);}}
    public String requestId(){return requestId;}public String filename(){return filename;}public String digest(){return digest;}public byte[] bytes(){return bytes.clone();}
}
