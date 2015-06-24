package fr.ortolang.diffusion.api.template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.io.Files;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class FileIconMethod implements TemplateMethodModelEx {

	private static final Map<String, String> typeMapping;
	private static final Map<String, String> extMapping;
	private static final String DEFAULT_ICON = "default.png"; 
	
	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
		if (arguments.size() != 2) {
			throw new TemplateModelException("This method need two parameters, filename and mimetype.");
		}
		String extension = Files.getFileExtension(((SimpleScalar) arguments.get(0)).getAsString());
		String mimetype = ((SimpleScalar) arguments.get(1)).getAsString();
		if ( extension != null && extMapping.containsKey(extension) ) {
			return extMapping.get(extension);
		}
		if (mimetype != null) {
			for ( String template : typeMapping.keySet() ) {
				if ( mimetype.matches(template) ) {
					return typeMapping.get(template);
				}
			}
		}
		return DEFAULT_ICON;
	}
	
	static {
		typeMapping = new HashMap<String, String> ();
		typeMapping.put("text/.*", "text.png");
		typeMapping.put("image/.*", "image.png");
		typeMapping.put("audio/.*", "audio.png");
		typeMapping.put("video/.*", "video.png");
		typeMapping.put("application/.*", "bin.png");
		typeMapping.put("ortolang/workspace", "folder-home.png");
		typeMapping.put("ortolang/snapshot", "folder.png");
		typeMapping.put("ortolang/collection", "folder.png");
		typeMapping.put("ortolang/link", "html.png");
		extMapping = new HashMap<String, String> ();
		extMapping.put("7z", "archive.png");
		extMapping.put("bz2", "archive.png");
		extMapping.put("cab", "archive.png");
		extMapping.put("gz", "archive.png");
		extMapping.put("tar", "archive.png");
		extMapping.put("aac", "audio.png");
		extMapping.put("aif", "audio.png");
		extMapping.put("aifc", "audio.png");
		extMapping.put("aiff", "audio.png");
		extMapping.put("ape", "audio.png");
		extMapping.put("au", "audio.png");
		extMapping.put("flac", "audio.png");
		extMapping.put("iff", "audio.png");
		extMapping.put("m4a", "audio.png");
		extMapping.put("mid", "audio.png");
		extMapping.put("mp3", "audio.png");
		extMapping.put("mpa", "audio.png");
		extMapping.put("ra", "audio.png");
		extMapping.put("wav", "audio.png");
		extMapping.put("wma", "audio.png");
		extMapping.put("f4a", "audio.png");
		extMapping.put("f4b", "audio.png");
		extMapping.put("oga", "audio.png");
		extMapping.put("ogg", "audio.png");
		extMapping.put("xm", "audio.png");
		extMapping.put("it", "audio.png");
		extMapping.put("s3m", "audio.png");
		extMapping.put("mod", "audio.png");
		extMapping.put("bin", "bin.png");
		extMapping.put("hex", "bin.png");
		extMapping.put("bmp", "bmp.png");
		extMapping.put("c", "c.png");
		extMapping.put("xlsx", "calc.png");
		extMapping.put("xlsm", "calc.png");
		extMapping.put("xltx", "calc.png");
		extMapping.put("xltm", "calc.png");
		extMapping.put("xlam", "calc.png");
		extMapping.put("xlr", "calc.png");
		extMapping.put("xls", "calc.png");
		extMapping.put("csv", "calc.png");
		extMapping.put("iso", "cd.png");
		extMapping.put("cpp", "cpp.png");
		extMapping.put("css", "css.png");
		extMapping.put("sass", "css.png");
		extMapping.put("scss", "css.png");
		extMapping.put("deb", "deb.png");
		extMapping.put("doc", "doc.png");
		extMapping.put("docx", "doc.png");
		extMapping.put("docm", "doc.png");
		extMapping.put("dot", "doc.png");
		extMapping.put("dotx", "doc.png");
		extMapping.put("dotm", "doc.png");
		extMapping.put("log", "doc.png");
		extMapping.put("msg", "doc.png");
		extMapping.put("odt", "doc.png");
		extMapping.put("pages", "doc.png");
		extMapping.put("rtf", "doc.png");
		extMapping.put("tex", "doc.png");
		extMapping.put("wpd", "doc.png");
		extMapping.put("wps", "doc.png");
		extMapping.put("svg", "draw.png");
		extMapping.put("svgz", "draw.png");
		extMapping.put("ai", "eps.png");
		extMapping.put("eps", "eps.png");
		extMapping.put("exe", "exe.png");
		extMapping.put("gif", "gif.png");
		extMapping.put("h", "h.png");
		extMapping.put("html", "html.png");
		extMapping.put("xhtml", "html.png");
		extMapping.put("shtml", "html.png");
		extMapping.put("htm", "html.png");
		extMapping.put("URL", "html.png");
		extMapping.put("url", "html.png");
		extMapping.put("ico", "ico.png");
		extMapping.put("jar", "java.png");
		extMapping.put("java", "java.png");
		extMapping.put("jpg", "jpg.png");
		extMapping.put("jpeg", "jpg.png");
		extMapping.put("jpe", "jpg.png");
		extMapping.put("js", "js.png");
		extMapping.put("json", "js.png");
		extMapping.put("md", "markdown.png");
		extMapping.put("pkg", "package.png");
		extMapping.put("dmg", "package.png");
		extMapping.put("pdf", "pdf.png");
		extMapping.put("php", "php.png");
		extMapping.put("phtml", "php.png");
		extMapping.put("m3u", "playlist.png");
		extMapping.put("m3u8", "playlist.png");
		extMapping.put("pls", "playlist.png");
		extMapping.put("pls8", "playlist.png");
		extMapping.put("png", "png.png");
		extMapping.put("psd", "psd.png");
		extMapping.put("py", "py.png");
		extMapping.put("rar", "rar.png");
		extMapping.put("rb", "rb.png");
		extMapping.put("rss", "rss.png");
		extMapping.put("bat", "script.png");
		extMapping.put("cmd", "script.png");
		extMapping.put("sh", "script.png");
		extMapping.put("sql", "sql.png");
		extMapping.put("tiff", "tiff.png");
		extMapping.put("tif", "tiff.png");
		extMapping.put("txt", "text.png");
		extMapping.put("nfo", "text.png");
		extMapping.put("asf", "video.png");
		extMapping.put("asx", "video.png");
		extMapping.put("avi", "video.png");
		extMapping.put("flv", "video.png");
		extMapping.put("mkv", "video.png");
		extMapping.put("mov", "video.png");
		extMapping.put("mp4", "video.png");
		extMapping.put("mpg", "video.png");
		extMapping.put("rm", "video.png");
		extMapping.put("srt", "video.png");
		extMapping.put("swf", "video.png");
		extMapping.put("vob", "video.png");
		extMapping.put("wmv", "video.png");
		extMapping.put("m4v", "video.png");
		extMapping.put("f4v", "video.png");
		extMapping.put("f4p", "video.png");
		extMapping.put("ogv", "video.png");
		extMapping.put("webm", "video.png");
		extMapping.put("xml", "xml.png");
		extMapping.put("zip", "zip.png");
	}
}

