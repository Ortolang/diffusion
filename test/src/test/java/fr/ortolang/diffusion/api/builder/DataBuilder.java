package fr.ortolang.diffusion.api.builder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;

public class DataBuilder {

	private static final String VERSION = "Workspace 10000 V2.0 part2";
	private static final String SOURCE_FOLDER = "/media/space/jerome/Data/dataset/frantext";
	private static final String DESTINATION_FOLDER = "/media/space/jerome/Data/dataset/10000-part2";
	private static final int LIMIT = 5000;

	private static Logger logger = Logger.getLogger(DataBuilder.class.getName());
	
	
	@Test
	public void build() throws IOException {
		Path objects = Paths.get(SOURCE_FOLDER, "objects");
				
		CorpusObjectsVisitor builder = new CorpusObjectsVisitor(SOURCE_FOLDER, DESTINATION_FOLDER, LIMIT);
		Files.walkFileTree(objects, builder);
	}

	class CorpusObjectsVisitor implements FileVisitor<Path> {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Path origin;
		Path destination;
		int limit;
		int cpt = 0;

		public CorpusObjectsVisitor(String origin, String destination, int limit) {
			this.origin = Paths.get(origin);
			this.destination = Paths.get(destination);
			this.limit = limit;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			logger.log(Level.INFO, "Folder found at path : " + dir);
			Path newdir = destination.resolve(origin.relativize(dir));
			if ( !newdir.toFile().exists() ) {
				logger.log(Level.INFO, "Creating new folder : " + newdir);
				newdir.toFile().mkdirs();
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			logger.log(Level.INFO, "Data object found at path : " + file);
			cpt++;
			if ( limit == -1 || cpt <= limit ) {
				Path newfile = destination.resolve(origin.relativize(file));
				logger.log(Level.INFO, "Copying file into new path : " + newfile);
				try ( OutputStream os = Files.newOutputStream(newfile) ) {
					Files.copy(file, os);
					os.write("<!-- a simple comment to avoid binary store use the same stream ".getBytes());
					os.write(VERSION.getBytes());
					os.write(" -->\r\n".getBytes());
				}
				
				return FileVisitResult.CONTINUE;
			} else {
				return FileVisitResult.TERMINATE;
			}
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			logger.log(Level.WARNING, "Unable to visit data object at path : " + file, exc);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

	}

}
