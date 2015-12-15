package fr.ortolang.diffusion.rendering;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;

import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import freemarker.cache.TemplateLoader;

public class BinaryStoreTemplateLoader implements TemplateLoader {
    
    private BinaryStoreService store;
    
    public BinaryStoreTemplateLoader (BinaryStoreService store) {
        this.store = store;
    }

    @Override
    public Object findTemplateSource(String hash) throws IOException {
        try {
            return store.getFile(hash);
        } catch (BinaryStoreServiceException | DataNotFoundException e) {
            throw new IOException("unable to find template for file hash: " + hash, e);
        }
    }

    @Override
    public long getLastModified(Object source) {
        return ((File)source).lastModified();
    }

    @Override
    public Reader getReader(Object source, String encoding) throws IOException {
        return Files.newBufferedReader(((File)source).toPath(), Charset.forName(encoding));
    }

    @Override
    public void closeTemplateSource(Object source) throws IOException {
        //Nothing to close.
    }
    
    

}
