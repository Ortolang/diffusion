package fr.ortolang.diffusion.store.handle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;

public class FormatHandleFile {
	
	public static void main(String args[]) {
		Path input = Paths.get("/home/jerome/handle.txt");
		Path output = Paths.get("/home/jerome/handle-out.txt");
		Path log = Paths.get("/home/jerome/handle-out.log");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int nbreads = 0;
		try (InputStream fis = Files.newInputStream(input)) {
			while ( (nbreads=fis.read(buf)) != -1 ) {
				baos.write(buf, 0, nbreads);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String content = baos.toString();
		StringTokenizer lines = new StringTokenizer(content, "\r\n");
		
		try ( OutputStream os = Files.newOutputStream(output); OutputStream logos = Files.newOutputStream(log) ) {
			while ( lines.hasMoreTokens() ) {
				String line = lines.nextToken();
				String[] words = line.split("\t");
				
				StringBuffer record = new StringBuffer();
				record.append("DELETE 11403").append(words[1].substring(words[1].lastIndexOf("/"))).append("\r\n\r\n");
				record.append("DELETE 11403").append(words[1].substring(27)).append("\r\n\r\n");
				record.append("CREATE 11403").append(words[1].substring(27)).append("\r\n");
				record.append("100 HS_ADMIN 86400 1110 ADMIN 300:111111111111:0.NA/11403\r\n");
				record.append("1 URL 86400 1110 UTF8 http://lrl-diffusion.univ-bpclermont.fr/11403/comere/").append(words[0]).append("\r\n\r\n");
				os.write(record.toString().getBytes());
				
				logos.write(line.getBytes());
				logos.write("\n".getBytes());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
